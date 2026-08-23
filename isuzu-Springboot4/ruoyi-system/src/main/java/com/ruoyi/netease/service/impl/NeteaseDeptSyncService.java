package com.ruoyi.netease.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.netease.domain.NeteaseUnitItem;
import com.ruoyi.netease.domain.ThirdSyncMapping;
import com.ruoyi.netease.service.IThirdSyncLogService;
import com.ruoyi.netease.service.IThirdSyncMappingService;
import com.ruoyi.netease.service.NeteaseApiClient;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 网易企业邮箱部门同步服务
 *
 * @author isuzu
 */
@Service
public class NeteaseDeptSyncService
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseDeptSyncService.class);
    private static final String THIRD_TYPE = "netease_mail";
    private static final String RUOYI_TABLE = "sys_dept";
    private static final String SYNC_USER = "netease-sync";
    private static final Long DEFAULT_ROOT_DEPT_ID = 100L;
    private static final String CONFIG_RECONCILE_ENABLED = "netease.dept.reconcile.enabled";

    @Autowired
    private NeteaseApiClient apiClient;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private IThirdSyncMappingService mappingService;

    @Autowired
    private IThirdSyncLogService logService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 获取并构建排序后的部门树（父在前，子在后）
     * 注意：网易的父子关联键是 unitId（数字ID），unitParentId 指向父部门的 unitId，
     * 而非 unitOpenId。因此排序判断必须用 unitId 集合。
     */
    public List<NeteaseUnitItem> loadAndSortDepts()
    {
        List<NeteaseUnitItem> allUnits = apiClient.getAllUnits();
        log.info("从网易获取到 {} 个部门", allUnits.size());

        // 过滤 unitOpenId 为空的虚拟节点（如网易"默认部门"虚拟节点，unitOpenId=null, unitId=xxx_default）
        // 这类节点无法用映射表唯一标识，且非真实组织架构，跳过避免每次全量同步重复创建
        allUnits = allUnits.stream()
                .filter(u -> u.getUnitOpenId() != null && !u.getUnitOpenId().isEmpty())
                .collect(Collectors.toList());
        log.info("过滤虚拟节点后剩余 {} 个部门", allUnits.size());

        // 收集所有部门 unitId（数字ID），用于判断父级是否存在/已处理
        Set<String> unitIds = allUnits.stream()
                .map(NeteaseUnitItem::getUnitId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<NeteaseUnitItem> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<NeteaseUnitItem> remaining = new ArrayList<>(allUnits);

        // 逐层处理：先处理根节点（父级为空/不存在），再处理子节点
        while (!remaining.isEmpty())
        {
            List<NeteaseUnitItem> currentLevel = new ArrayList<>();
            List<NeteaseUnitItem> nextRemaining = new ArrayList<>();

            for (NeteaseUnitItem unit : remaining)
            {
                String parentId = unit.getUnitParentId();
                // 根级：父ID为空/0，或父ID不在任何部门的unitId集合中，或父部门已处理
                if (parentId == null || parentId.isEmpty() || "0".equals(parentId)
                        || !unitIds.contains(parentId)
                        || sorted.stream().anyMatch(s -> parentId.equals(s.getUnitId())))
                {
                    if (!visited.contains(unit.getUnitOpenId()))
                    {
                        currentLevel.add(unit);
                        visited.add(unit.getUnitOpenId());
                    }
                }
                else
                {
                    nextRemaining.add(unit);
                }
            }

            if (currentLevel.isEmpty() && !nextRemaining.isEmpty())
            {
                // 检测到循环引用，跳过剩余
                log.error("部门树存在循环引用，跳过 {} 个部门", nextRemaining.size());
                break;
            }

            sorted.addAll(currentLevel);
            remaining = nextRemaining;
        }

        return sorted;
    }

    /**
     * 全量同步部门
     *
     * @param rootDeptId 网易顶级部门挂载的根锚点部门ID（若依 dept_id=100）
     * @param revision 当前版本号
     * @return 同步的部门数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncFullDepts(Long rootDeptId, Long revision)
    {
        List<NeteaseUnitItem> sortedUnits = loadAndSortDepts();
        int syncCount = 0;

        for (NeteaseUnitItem unit : sortedUnits)
        {
            try
            {
                syncOneDept(unit, rootDeptId, revision);
                syncCount++;
            }
            catch (Exception e)
            {
                log.error("同步部门失败: {} ({})", unit.getUnitName(), unit.getUnitOpenId(), e);
                logService.record("dept", "SKIP", unit.getUnitOpenId(), null,
                        null, null, false, e.getMessage(), revision);
            }
        }

        // 标记网易侧已删除的部门为停用
        markDeletedDepts(sortedUnits, revision);

        log.info("部门全量同步完成，共同步 {} 个部门", syncCount);
        return syncCount;
    }

    private void syncOneDept(NeteaseUnitItem unit, Long rootDeptId, Long revision)
    {
        String unitOpenId = unit.getUnitOpenId();
        ThirdSyncMapping existing = mappingService.getMapping(THIRD_TYPE, RUOYI_TABLE, unitOpenId);

        // 确定父部门在若依中的 ID
        Long parentRuoyiId = resolveParentDeptId(unit.getUnitParentId(), rootDeptId);

        String unitJson = JSON.toJSONString(unit);

        if (existing == null)
        {
            // 新建部门
            SysDept dept = new SysDept();
            dept.setParentId(parentRuoyiId);
            dept.setDeptName(unit.getUnitName());
            dept.setOrderNum(unit.getRank() != null ? unit.getRank() : 0);
            dept.setStatus(UserConstants.DEPT_NORMAL);
            dept.setCreateBy(SYNC_USER);
            deptService.insertDept(dept);

            // 插入映射表
            ThirdSyncMapping mapping = new ThirdSyncMapping();
            mapping.setThirdType(THIRD_TYPE);
            mapping.setRuoyiTable(RUOYI_TABLE);
            mapping.setRuoyiId(dept.getDeptId());
            mapping.setThirdId(unitOpenId);
            mapping.setThirdAltId(unit.getUnitId());
            mapping.setThirdParentId(unit.getUnitParentId());
            mapping.setThirdJson(unitJson);
            mapping.setSyncStatus(1);
            mapping.setSyncVersion(revision);
            mappingService.saveMapping(mapping);

            logService.record("dept", "INSERT", unitOpenId, dept.getDeptId(),
                    null, unitJson, true, null, revision);
        }
        else
        {
            // 更新现有部门
            SysDept dept = deptMapper.selectDeptById(existing.getRuoyiId());
            if (dept != null)
            {
                String beforeJson = existing.getThirdJson();
                dept.setDeptName(unit.getUnitName());
                dept.setOrderNum(unit.getRank() != null ? unit.getRank() : 0);
                dept.setParentId(parentRuoyiId);
                dept.setUpdateBy(SYNC_USER);
                deptService.updateDept(dept);

                existing.setRuoyiId(dept.getDeptId());
                existing.setThirdAltId(unit.getUnitId());
                existing.setThirdParentId(unit.getUnitParentId());
                existing.setThirdJson(unitJson);
                existing.setSyncStatus(1);
                existing.setSyncVersion(revision);
                existing.setUpdateBy(SYNC_USER);
                mappingService.saveMapping(existing);

                logService.record("dept", "UPDATE", unitOpenId, dept.getDeptId(),
                        beforeJson, unitJson, true, null, revision);
            }
        }
    }

    /**
     * 解析父部门在若依中的ID。
     * 网易 unitParentId 指向父部门的 unitId（数字ID），需通过映射表 third_alt_id 关联。
     */
    private Long resolveParentDeptId(String unitParentId, Long rootDeptId)
    {
        if (unitParentId == null || unitParentId.isEmpty() || "0".equals(unitParentId))
        {
            return rootDeptId;
        }
        Long parentId = mappingService.getRuoyiIdByAltId(THIRD_TYPE, RUOYI_TABLE, unitParentId);
        return parentId != null ? parentId : rootDeptId;
    }

    private void markDeletedDepts(List<NeteaseUnitItem> remoteUnits, Long revision)
    {
        Set<String> remoteIds = remoteUnits.stream()
                .map(NeteaseUnitItem::getUnitOpenId)
                .collect(Collectors.toSet());

        List<ThirdSyncMapping> localMappings = mappingService.getAllByTable(THIRD_TYPE, RUOYI_TABLE);
        for (ThirdSyncMapping mapping : localMappings)
        {
            if (mapping.getSyncStatus() == 1 && !remoteIds.contains(mapping.getThirdId()))
            {
                softDisableDept(mapping, revision);
            }
        }
    }

    /**
     * 查询若依部门树的根锚点（parent_id=0）。
     * 找不到时回退默认 100 并 warn（对账/巡检不应因初始化缺失而反复报错，与协调器抛异常策略不同）。
     */
    public Long getRootDeptId()
    {
        List<SysDept> roots = deptMapper.selectDeptList(null)
                .stream()
                .filter(d -> d.getParentId() != null && d.getParentId() == 0L)
                .collect(Collectors.toList());
        if (roots.isEmpty())
        {
            log.warn("若依部门树根锚点缺失（parent_id=0），回退默认 100");
            return DEFAULT_ROOT_DEPT_ID;
        }
        return roots.get(0).getDeptId();
    }

    /**
     * 软停用部门：sys_dept.status 置停用（mapper 直改，不触发 ancestors 级联）+ 映射表标记已停用 + 记 DELETE 日志。
     */
    public void softDisableDept(ThirdSyncMapping mapping, Long revision)
    {
        SysDept dept = deptMapper.selectDeptById(mapping.getRuoyiId());
        if (dept != null)
        {
            dept.setStatus(UserConstants.DEPT_DISABLE);
            dept.setUpdateBy(SYNC_USER);
            deptMapper.updateDept(dept);
        }
        mappingService.markDisabled(THIRD_TYPE, RUOYI_TABLE, mapping.getThirdId());
        logService.record("dept", "DELETE", mapping.getThirdId(), mapping.getRuoyiId(),
                mapping.getThirdJson(), null, true, null, revision);
        log.info("标记部门为停用: {} (ruoyi_id={})", mapping.getThirdId(), mapping.getRuoyiId());
    }

    /**
     * 修复漂移部门：名称/排序/父级对齐远端后经 deptService.updateDept 落库。
     * 若依 updateDept 会自动重算 ancestors 并级联更新所有子部门，无需手算。
     */
    public void fixDriftedDept(ThirdSyncMapping mapping, NeteaseUnitItem remoteUnit, Long rootDeptId)
    {
        SysDept dept = deptMapper.selectDeptById(mapping.getRuoyiId());
        if (dept == null)
        {
            return;
        }
        String beforeJson = mapping.getThirdJson();

        dept.setDeptName(remoteUnit.getUnitName());
        dept.setOrderNum(remoteUnit.getRank() != null ? remoteUnit.getRank() : 0);
        dept.setParentId(resolveParentDeptId(remoteUnit.getUnitParentId(), rootDeptId));
        dept.setUpdateBy(SYNC_USER);

        deptService.updateDept(dept);

        String afterJson = JSON.toJSONString(remoteUnit);
        mapping.setThirdAltId(remoteUnit.getUnitId());
        mapping.setThirdParentId(remoteUnit.getUnitParentId());
        mapping.setThirdJson(afterJson);
        mapping.setUpdateBy(SYNC_USER);
        mappingService.saveMapping(mapping);
        logService.record("dept", "UPDATE", mapping.getThirdId(), mapping.getRuoyiId(),
                beforeJson, afterJson, true, null, 0L);
    }

    /**
     * 部门对账：轮询网易 getUnitList 与本地映射表差异比对。
     * 网易 revision 流仅承载账号变更、不含部门事件，部门的新建/改名/移动/删除由此任务收敛（默认每30分钟）。
     *
     * @return 本次产生实际变更的条数
     */
    public int reconcileDepts()
    {
        String enabled = configService.selectConfigByKey(CONFIG_RECONCILE_ENABLED);
        if (!"true".equals(enabled))
        {
            log.info("部门对账开关未开启（{}={}），跳过", CONFIG_RECONCILE_ENABLED, enabled);
            return 0;
        }

        long startTime = System.currentTimeMillis();
        List<NeteaseUnitItem> remoteUnits = apiClient.getAllUnits().stream()
                .filter(u -> u.getUnitOpenId() != null && !u.getUnitOpenId().isEmpty())
                .collect(Collectors.toList());

        Map<String, ThirdSyncMapping> localByThirdId = mappingService.getAllByTable(THIRD_TYPE, RUOYI_TABLE)
                .stream()
                .filter(m -> m.getThirdId() != null && !m.getThirdId().isEmpty())
                .collect(Collectors.toMap(ThirdSyncMapping::getThirdId, m -> m, (a, b) -> a));

        Set<String> remoteIds = remoteUnits.stream()
                .map(NeteaseUnitItem::getUnitOpenId)
                .collect(Collectors.toSet());
        Long rootDeptId = getRootDeptId();

        int changeCount = 0;

        // 分支一：远程有、本地无映射 → 拓扑排序后补建
        List<NeteaseUnitItem> toCreate = remoteUnits.stream()
                .filter(u -> !localByThirdId.containsKey(u.getUnitOpenId()))
                .collect(Collectors.toList());
        for (NeteaseUnitItem unit : sortForCreate(toCreate, remoteIds))
        {
            try
            {
                syncOneDept(unit, rootDeptId, 0L);
                changeCount++;
            }
            catch (Exception e)
            {
                log.error("部门对账补建失败: {} ({})", unit.getUnitName(), unit.getUnitOpenId(), e);
                logService.record("dept", "SKIP", unit.getUnitOpenId(), null,
                        null, JSON.toJSONString(unit), false, e.getMessage(), 0L);
            }
        }

        // 分支二 + 三：两边都有 → 快照比对漂移；远程无、本地活跃 → 软停用
        for (ThirdSyncMapping local : localByThirdId.values())
        {
            try
            {
                if (remoteIds.contains(local.getThirdId()))
                {
                    NeteaseUnitItem remoteUnit = remoteUnits.stream()
                            .filter(u -> local.getThirdId().equals(u.getUnitOpenId()))
                            .findFirst().orElse(null);
                    if (remoteUnit != null && !snapshotEquals(remoteUnit, local.getThirdJson()))
                    {
                        fixDriftedDept(local, remoteUnit, rootDeptId);
                        changeCount++;
                    }
                }
                else if (local.getSyncStatus() != null && local.getSyncStatus() == 1)
                {
                    softDisableDept(local, 0L);
                    changeCount++;
                }
            }
            catch (Exception e)
            {
                log.error("部门对账处理失败: {}", local.getThirdId(), e);
                logService.record("dept", "SKIP", local.getThirdId(), local.getRuoyiId(),
                        local.getThirdJson(), null, false, e.getMessage(), 0L);
            }
        }

        if (changeCount > 0)
        {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("部门对账完成: 变更{}条, 耗时{}ms", changeCount, elapsed);
        }
        return changeCount;
    }

    /**
     * 快照比对：反序列化映射表 third_json，比较 unitName/unitParentId/rank 三字段（与巡检哈希口径一致）。
     * 解析失败返回 false，走修复路径重建快照。
     */
    private boolean snapshotEquals(NeteaseUnitItem remoteUnit, String thirdJson)
    {
        if (thirdJson == null || thirdJson.isEmpty())
        {
            return false;
        }
        try
        {
            NeteaseUnitItem snapshot = JSON.parseObject(thirdJson, NeteaseUnitItem.class);
            return java.util.Objects.equals(snapshot.getUnitName(), remoteUnit.getUnitName())
                    && java.util.Objects.equals(snapshot.getUnitParentId(), remoteUnit.getUnitParentId())
                    && java.util.Objects.equals(
                            snapshot.getRank() != null ? snapshot.getRank() : 0,
                            remoteUnit.getRank() != null ? remoteUnit.getRank() : 0);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 补建排序：父在前子在后（按 unitParentId→unitId 关系拓扑排序）。
     * 环引用剩余节点直接输出兜底——syncOneDept 解析不到父级时回退根锚点，下轮收敛。
     */
    private List<NeteaseUnitItem> sortForCreate(List<NeteaseUnitItem> units, Set<String> allKnownUnitIds)
    {
        List<NeteaseUnitItem> sorted = new ArrayList<>();
        Set<String> sortedIds = new HashSet<>();
        List<NeteaseUnitItem> remaining = new ArrayList<>(units);

        while (!remaining.isEmpty())
        {
            List<NeteaseUnitItem> currentLevel = new ArrayList<>();
            for (NeteaseUnitItem unit : remaining)
            {
                String parentId = unit.getUnitParentId();
                boolean parentReady = parentId == null || parentId.isEmpty() || "0".equals(parentId)
                        || !allKnownUnitIds.contains(parentId)
                        || sortedIds.contains(parentId);
                if (parentReady)
                {
                    currentLevel.add(unit);
                }
            }
            if (currentLevel.isEmpty())
            {
                // 剩余节点互相引用成环，直接输出由 resolveParentDeptId 兜底挂根锚点
                log.warn("对账补建存在环引用 {} 个部门，本轮直接输出", remaining.size());
                sorted.addAll(remaining);
                break;
            }
            sorted.addAll(currentLevel);
            sortedIds.addAll(currentLevel.stream().map(NeteaseUnitItem::getUnitId).collect(Collectors.toSet()));
            remaining.removeAll(currentLevel);
        }
        return sorted;
    }
}