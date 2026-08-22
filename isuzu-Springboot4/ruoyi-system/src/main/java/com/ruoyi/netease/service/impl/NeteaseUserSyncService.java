package com.ruoyi.netease.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.netease.domain.NeteaseAccountResp;
import com.ruoyi.netease.domain.ThirdSyncMapping;
import com.ruoyi.netease.service.IThirdSyncLogService;
import com.ruoyi.netease.service.IThirdSyncMappingService;
import com.ruoyi.netease.service.NeteaseApiClient;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 网易企业邮箱用户同步服务
 *
 * @author isuzu
 */
@Service
public class NeteaseUserSyncService
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseUserSyncService.class);
    private static final String THIRD_TYPE = "netease_mail";
    private static final String RUOYI_TABLE = "sys_user";
    private static final String DEPT_TABLE = "sys_dept";
    private static final String SYNC_USER = "netease-sync";

    @Autowired
    private NeteaseApiClient apiClient;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private IThirdSyncMappingService mappingService;

    @Autowired
    private IThirdSyncLogService logService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 全量同步用户
     *
     * @param domain 邮箱域名
     * @param rootDeptId 默认部门ID（映射不到时使用）
     * @param revision 当前版本号
     * @return 同步的用户数量
     */
    public int syncFullUsers(String domain, Long rootDeptId, Long revision)
    {
        List<NeteaseAccountResp> allAccounts = apiClient.getAllAccounts();
        int syncCount = 0;

        for (NeteaseAccountResp account : allAccounts)
        {
            try
            {
                syncOneUser(account, domain, rootDeptId, revision);
                syncCount++;
            }
            catch (Exception e)
            {
                log.error("同步用户失败: {} ({})", account.getAccountName(), account.getAccountOpenId(), e);
                logService.record("user", "SKIP", account.getAccountOpenId(), null,
                        null, null, false, e.getMessage(), revision);
            }
        }

        // 标记网易侧已删除的用户为停用
        markDeletedUsers(allAccounts, revision);

        log.info("用户全量同步完成，共同步 {} 个用户", syncCount);
        return syncCount;
    }

    /**
     * 同步单个用户（增量同步使用）
     */
    public void syncOneUser(NeteaseAccountResp account, String domain, Long rootDeptId, Long revision)
    {
        String accountOpenId = account.getAccountOpenId();
        ThirdSyncMapping existing = mappingService.getMapping(THIRD_TYPE, RUOYI_TABLE, accountOpenId);

        // 确定部门
        Long deptId = resolveDeptId(account, rootDeptId);

        // 确定用户状态
        String userStatus = mapStatus(account.getStatus());

        String accountJson = JSON.toJSONString(account);

        if (existing == null)
        {
            // 新建用户
            String userName = account.getAccountName();
            // 检查用户名唯一性
            SysUser existingUser = userMapper.selectUserByUserName(userName);
            if (existingUser != null)
            {
                log.warn("用户名 {} 已存在，标记为冲突", userName);
                ThirdSyncMapping conflictMapping = new ThirdSyncMapping();
                conflictMapping.setThirdType(THIRD_TYPE);
                conflictMapping.setRuoyiTable(RUOYI_TABLE);
                conflictMapping.setRuoyiId(existingUser.getUserId());
                conflictMapping.setThirdId(accountOpenId);
                conflictMapping.setThirdJson(accountJson);
                conflictMapping.setSyncStatus(4); // 冲突
                conflictMapping.setSyncVersion(revision);
                mappingService.saveMapping(conflictMapping);
                logService.record("user", "SKIP", accountOpenId, existingUser.getUserId(),
                        null, accountJson, false, "用户名冲突: " + userName, revision);
                return;
            }

            SysUser user = new SysUser();
            user.setUserName(userName);
            user.setNickName(account.getName() != null ? account.getName() : userName);
            user.setEmail(account.getAccountName() + "@" + domain);
            user.setPhonenumber(truncatePhone(account.getMobile()));
            user.setDeptId(deptId);
            user.setStatus(userStatus);
            user.setCreateBy(SYNC_USER);
            // 设置默认密码
            String initPassword = configService.selectConfigByKey("sys.user.initPassword");
            user.setPassword(SecurityUtils.encryptPassword(initPassword != null ? initPassword : "123456"));

            userService.insertUser(user);

            // 插入映射表
            ThirdSyncMapping mapping = new ThirdSyncMapping();
            mapping.setThirdType(THIRD_TYPE);
            mapping.setRuoyiTable(RUOYI_TABLE);
            mapping.setRuoyiId(user.getUserId());
            mapping.setThirdId(accountOpenId);
            mapping.setThirdJson(accountJson);
            mapping.setSyncStatus(1);
            mapping.setSyncVersion(revision);
            mappingService.saveMapping(mapping);

            logService.record("user", "INSERT", accountOpenId, user.getUserId(),
                    null, accountJson, true, null, revision);
        }
        else
        {
            // 更新现有用户
            String beforeJson = existing.getThirdJson();
            SysUser user = new SysUser();
            user.setUserId(existing.getRuoyiId());
            user.setNickName(account.getName());
            user.setEmail(account.getAccountName() + "@" + domain);
            user.setPhonenumber(truncatePhone(account.getMobile()));
            user.setDeptId(deptId);
            user.setStatus(userStatus);
            user.setUpdateBy(SYNC_USER);
            userService.updateUser(user);

            existing.setThirdJson(accountJson);
            existing.setSyncStatus(1);
            existing.setSyncVersion(revision);
            existing.setUpdateBy(SYNC_USER);
            mappingService.saveMapping(existing);

            logService.record("user", "UPDATE", accountOpenId, existing.getRuoyiId(),
                    beforeJson, accountJson, true, null, revision);
        }
    }

    /**
     * 根据网易状态映射到若依状态
     * 0=正常, 1=禁用, 2=已删除, 4=离职, 5=交接中, 6=交接完成
     */
    private String mapStatus(Integer status)
    {
        if (status == null || status == 0) return "0"; // 正常
        return "1"; // 其他状态一律停用
    }

    /**
     * 解析用户所属部门在若依中的ID。
     * 注意：网易账号 unitList 存的是部门 unitId（数字ID），需通过映射表 third_alt_id 关联。
     */
    private Long resolveDeptId(NeteaseAccountResp account, Long rootDeptId)
    {
        List<String> unitList = account.getUnitList();
        if (unitList != null && !unitList.isEmpty())
        {
            Long deptId = mappingService.getRuoyiIdByAltId(THIRD_TYPE, DEPT_TABLE, unitList.get(0));
            if (deptId != null) return deptId;
        }
        // 用 unitId 字段尝试
        if (account.getUnitId() != null && !account.getUnitId().isEmpty())
        {
            Long deptId = mappingService.getRuoyiIdByAltId(THIRD_TYPE, DEPT_TABLE, account.getUnitId());
            if (deptId != null) return deptId;
        }
        log.warn("用户 {} 的部门映射不存在，使用默认部门 {}", account.getAccountName(), rootDeptId);
        return rootDeptId;
    }

    private String truncatePhone(String mobile)
    {
        if (mobile == null) return null;
        return mobile.length() > 11 ? mobile.substring(0, 11) : mobile;
    }

    /**
     * 软停用用户（不物理删除）
     */
    public void softDisableUser(Long ruoyiId, String accountOpenId, Long revision)
    {
        userMapper.updateUserStatus(ruoyiId, "1");
        mappingService.markDisabled(THIRD_TYPE, RUOYI_TABLE, accountOpenId);
        logService.record("user", "DELETE", accountOpenId, ruoyiId,
                null, null, true, null, revision);
        log.info("软停用用户: {} (ruoyi_id={})", accountOpenId, ruoyiId);
    }

    private void markDeletedUsers(List<NeteaseAccountResp> remoteAccounts, Long revision)
    {
        Set<String> remoteIds = remoteAccounts.stream()
                .map(NeteaseAccountResp::getAccountOpenId)
                .collect(Collectors.toSet());

        List<ThirdSyncMapping> localMappings = mappingService.getAllByTable(THIRD_TYPE, RUOYI_TABLE);
        for (ThirdSyncMapping mapping : localMappings)
        {
            if (mapping.getSyncStatus() == 1 && !remoteIds.contains(mapping.getThirdId()))
            {
                softDisableUser(mapping.getRuoyiId(), mapping.getThirdId(), revision);
            }
        }
    }
}