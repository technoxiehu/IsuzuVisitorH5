package com.ruoyi.netease.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.netease.domain.NeteaseAccountResp;
import com.ruoyi.netease.domain.NeteaseUnitItem;
import com.ruoyi.netease.domain.ThirdSyncMapping;
import com.ruoyi.netease.service.IThirdSyncLogService;
import com.ruoyi.netease.service.IThirdSyncMappingService;
import com.ruoyi.netease.service.NeteaseApiClient;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 网易企业邮箱全量巡检服务
 * 每周执行，通过哈希比对检测数据漂移并自动修复
 *
 * @author isuzu
 */
@Service
public class NeteaseSyncInspectService
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseSyncInspectService.class);
    private static final String THIRD_TYPE = "netease_mail";
    private static final int ALERT_THRESHOLD = 10;

    @Autowired
    private NeteaseApiClient apiClient;

    @Autowired
    private NeteaseDeptSyncService deptSyncService;

    @Autowired
    private NeteaseUserSyncService userSyncService;

    @Autowired
    private IThirdSyncMappingService mappingService;

    @Autowired
    private IThirdSyncLogService logService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysDeptMapper deptMapper;

    /**
     * 执行全量巡检
     */
    public void inspect()
    {
        log.info("开始全量巡检...");
        long startTime = System.currentTimeMillis();
        int fixCount = 0;

        try
        {
            String domain = configService.selectConfigByKey("netease.domain");

            // 拉取远程全量数据
            List<NeteaseUnitItem> remoteDepts = apiClient.getAllUnits();
            List<NeteaseAccountResp> remoteUsers = apiClient.getAllAccounts();

            // 生成远程哈希指纹
            Map<String, String> remoteDeptHash = buildDeptHash(remoteDepts);
            Map<String, String> remoteUserHash = buildUserHash(remoteUsers);

            // 获取本地映射
            List<ThirdSyncMapping> deptMappings = mappingService.getActiveMappings(THIRD_TYPE)
                    .stream().filter(m -> "sys_dept".equals(m.getRuoyiTable()))
                    .collect(Collectors.toList());
            List<ThirdSyncMapping> userMappings = mappingService.getActiveMappings(THIRD_TYPE)
                    .stream().filter(m -> "sys_user".equals(m.getRuoyiTable()))
                    .collect(Collectors.toList());

            // 比对部门
            Long rootDeptId = getRootDeptId();
            fixCount += inspectDepts(deptMappings, remoteDeptHash, remoteDepts, rootDeptId);
            // 比对用户
            fixCount += inspectUsers(userMappings, remoteUserHash, remoteUsers, domain, rootDeptId);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("全量巡检完成: 修复{}条, 耗时{}ms", fixCount, elapsed);

            if (fixCount > ALERT_THRESHOLD)
            {
                log.warn("巡检修复数量({})超过阈值({})，建议人工检查", fixCount, ALERT_THRESHOLD);
            }
        }
        catch (Exception e)
        {
            log.error("全量巡检失败", e);
        }
    }

    private int inspectDepts(List<ThirdSyncMapping> localMappings, Map<String, String> remoteHash,
                              List<NeteaseUnitItem> remoteDepts, Long rootDeptId)
    {
        int fixCount = 0;
        Set<String> remoteIds = remoteDepts.stream().map(NeteaseUnitItem::getUnitOpenId).collect(Collectors.toSet());

        for (ThirdSyncMapping local : localMappings)
        {
            // 跳过第三方ID为空的记录（如网易"默认部门"虚拟节点 unitOpenId 为 null）
            if (local.getThirdId() == null || local.getThirdId().isEmpty())
            {
                log.debug("跳过 thirdId 为空的映射记录 ruoyi_id={}", local.getRuoyiId());
                continue;
            }

            String remoteH = remoteHash.get(local.getThirdId());
            if (remoteH == null)
            {
                // 远程已删除，本地还在 → 软停用
                softDisableDept(local);
                fixCount++;
                continue;
            }

            // 本地快照用与远程完全相同的算法计算哈希（反序列化后按字段拼接）
            String localH = computeDeptHash(local.getThirdJson());
            if (!remoteH.equals(localH))
            {
                // 数据漂移 → 修复
                NeteaseUnitItem remoteUnit = remoteDepts.stream()
                        .filter(u -> local.getThirdId().equals(u.getUnitOpenId()))
                        .findFirst().orElse(null);
                if (remoteUnit != null)
                {
                    fixDriftedDept(local, remoteUnit, rootDeptId);
                    fixCount++;
                }
            }
            remoteIds.remove(local.getThirdId());
        }

        // 远程新增但本地无映射
        for (String newId : remoteIds)
        {
            if (newId == null) continue; // 跳过 unitOpenId 为空的虚拟节点
            NeteaseUnitItem remoteUnit = remoteDepts.stream()
                    .filter(u -> newId.equals(u.getUnitOpenId()))
                    .findFirst().orElse(null);
            if (remoteUnit != null)
            {
                logService.recordInspectFix(newId, null, null, JSON.toJSONString(remoteUnit), 0L);
                fixCount++;
            }
        }

        return fixCount;
    }

    private int inspectUsers(List<ThirdSyncMapping> localMappings, Map<String, String> remoteHash,
                              List<NeteaseAccountResp> remoteUsers, String domain, Long rootDeptId)
    {
        int fixCount = 0;
        Set<String> remoteIds = remoteUsers.stream().map(NeteaseAccountResp::getAccountOpenId).collect(Collectors.toSet());

        for (ThirdSyncMapping local : localMappings)
        {
            // 跳过第三方ID为空的记录
            if (local.getThirdId() == null || local.getThirdId().isEmpty())
            {
                continue;
            }

            String remoteH = remoteHash.get(local.getThirdId());
            if (remoteH == null)
            {
                userSyncService.softDisableUser(local.getRuoyiId(), local.getThirdId(), 0L);
                fixCount++;
                continue;
            }

            String localH = computeUserHash(local.getThirdJson());
            if (!remoteH.equals(localH))
            {
                NeteaseAccountResp remoteAccount = remoteUsers.stream()
                        .filter(a -> local.getThirdId().equals(a.getAccountOpenId()))
                        .findFirst().orElse(null);
                if (remoteAccount != null)
                {
                    userSyncService.syncOneUser(remoteAccount, domain, rootDeptId, 0L);
                    logService.recordInspectFix(local.getThirdId(), local.getRuoyiId(),
                            local.getThirdJson(), JSON.toJSONString(remoteAccount), 0L);
                    fixCount++;
                }
            }
            remoteIds.remove(local.getThirdId());
        }

        for (String newId : remoteIds)
        {
            if (newId == null) continue;
            NeteaseAccountResp remoteAccount = remoteUsers.stream()
                    .filter(a -> newId.equals(a.getAccountOpenId()))
                    .findFirst().orElse(null);
            if (remoteAccount != null)
            {
                logService.recordInspectFix(newId, null, null, JSON.toJSONString(remoteAccount), 0L);
                fixCount++;
            }
        }

        return fixCount;
    }

    /**
     * 生成远程部门哈希指纹：md5(unitName + unitParentId + rank)
     */
    private Map<String, String> buildDeptHash(List<NeteaseUnitItem> depts)
    {
        Map<String, String> hash = new HashMap<>();
        for (NeteaseUnitItem d : depts)
        {
            if (d.getUnitOpenId() == null) continue; // 跳过 unitOpenId 为空的虚拟节点
            hash.put(d.getUnitOpenId(), computeDeptHash(d));
        }
        return hash;
    }

    /**
     * 生成远程用户哈希指纹：md5(name + mobile + unitListJson + status)
     */
    private Map<String, String> buildUserHash(List<NeteaseAccountResp> users)
    {
        Map<String, String> hash = new HashMap<>();
        for (NeteaseAccountResp u : users)
        {
            if (u.getAccountOpenId() == null) continue;
            hash.put(u.getAccountOpenId(), computeUserHash(u));
        }
        return hash;
    }

    /**
     * 计算部门哈希（远程与本地快照统一用此算法）
     */
    private String computeDeptHash(NeteaseUnitItem d)
    {
        return md5(d.getUnitName() + d.getUnitParentId() + d.getRank());
    }

    /**
     * 计算用户哈希（远程与本地快照统一用此算法）
     */
    private String computeUserHash(NeteaseAccountResp u)
    {
        return md5(u.getName() + u.getMobile() + JSON.toJSONString(u.getUnitList()) + u.getStatus());
    }

    /**
     * 从本地 JSON 快照反序列化为 DTO 后按统一算法计算部门哈希
     */
    private String computeDeptHash(String thirdJson)
    {
        if (thirdJson == null || thirdJson.isEmpty()) return "";
        try
        {
            NeteaseUnitItem d = JSON.parseObject(thirdJson, NeteaseUnitItem.class);
            return computeDeptHash(d);
        }
        catch (Exception e)
        {
            return md5(thirdJson); // 解析失败兜底
        }
    }

    /**
     * 从本地 JSON 快照反序列化为 DTO 后按统一算法计算用户哈希
     */
    private String computeUserHash(String thirdJson)
    {
        if (thirdJson == null || thirdJson.isEmpty()) return "";
        try
        {
            NeteaseAccountResp u = JSON.parseObject(thirdJson, NeteaseAccountResp.class);
            return computeUserHash(u);
        }
        catch (Exception e)
        {
            return md5(thirdJson); // 解析失败兜底
        }
    }

    private void softDisableDept(ThirdSyncMapping mapping)
    {
        SysDept dept = deptMapper.selectDeptById(mapping.getRuoyiId());
        if (dept != null)
        {
            dept.setStatus(UserConstants.DEPT_DISABLE);
            dept.setUpdateBy("netease-sync");
            deptMapper.updateDept(dept);
        }
        mappingService.markDisabled(THIRD_TYPE, "sys_dept", mapping.getThirdId());
        logService.recordInspectFix(mapping.getThirdId(), mapping.getRuoyiId(),
                mapping.getThirdJson(), null, 0L);
    }

    private void fixDriftedDept(ThirdSyncMapping mapping, NeteaseUnitItem remoteUnit, Long rootDeptId)
    {
        SysDept dept = deptMapper.selectDeptById(mapping.getRuoyiId());
        if (dept != null)
        {
            String beforeJson = mapping.getThirdJson();
            dept.setDeptName(remoteUnit.getUnitName());
            dept.setOrderNum(remoteUnit.getRank() != null ? remoteUnit.getRank() : 0);
            dept.setUpdateBy("netease-sync");

            // 父级修正：解析 unitParentId → 若依 dept_id，并重算 ancestors
            String unitParentId = remoteUnit.getUnitParentId();
            Long parentRuoyiId = rootDeptId;
            if (unitParentId != null && !unitParentId.isEmpty() && !"0".equals(unitParentId))
            {
                Long resolved = mappingService.getRuoyiIdByAltId(THIRD_TYPE, "sys_dept", unitParentId);
                if (resolved != null)
                {
                    parentRuoyiId = resolved;
                }
            }
            if (parentRuoyiId != null && !parentRuoyiId.equals(dept.getParentId()))
            {
                // 父级变化：重算 ancestors = 父部门.ancestors + "," + 父部门.dept_id
                SysDept parent = deptMapper.selectDeptById(parentRuoyiId);
                String ancestors;
                if (parent != null)
                {
                    ancestors = ("0".equals(parent.getAncestors()) || parent.getAncestors() == null)
                            ? parent.getDeptId().toString()
                            : parent.getAncestors() + "," + parent.getDeptId();
                }
                else
                {
                    ancestors = "0," + parentRuoyiId;
                }
                dept.setParentId(parentRuoyiId);
                dept.setAncestors(ancestors);
            }
            deptMapper.updateDept(dept);

            String afterJson = JSON.toJSONString(remoteUnit);
            mapping.setThirdJson(afterJson);
            mapping.setUpdateBy("netease-sync");
            mappingService.saveMapping(mapping);
            logService.recordInspectFix(mapping.getThirdId(), mapping.getRuoyiId(), beforeJson, afterJson, 0L);
        }
    }

    /**
     * 查询若依部门树的根节点（parent_id=0）
     */
    private Long getRootDeptId()
    {
        List<SysDept> roots = deptMapper.selectDeptList(null)
                .stream()
                .filter(d -> d.getParentId() != null && d.getParentId() == 0L)
                .collect(java.util.stream.Collectors.toList());
        if (roots.isEmpty())
        {
            return 100L; // 若依默认根部门 ID
        }
        return roots.get(0).getDeptId();
    }

    private static String md5(String input)
    {
        if (input == null) return "";
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) { sb.append(String.format("%02x", b)); }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            return Integer.toHexString(input.hashCode());
        }
    }
}