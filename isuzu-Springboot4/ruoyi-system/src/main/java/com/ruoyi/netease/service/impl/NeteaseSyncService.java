package com.ruoyi.netease.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.netease.domain.NeteaseRevisionData;
import com.ruoyi.netease.service.IThirdSyncLogService;
import com.ruoyi.netease.service.NeteaseApiClient;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 网易企业邮箱同步协调器
 * 负责全量同步、增量同步的流程编排
 *
 * @author isuzu
 */
@Service
public class NeteaseSyncService
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseSyncService.class);

    private static final String CONFIG_REVISION = "netease.sync.revision";
    private static final String CONFIG_MODE = "netease.sync.mode";
    private static final String CONFIG_DOMAIN = "netease.domain";
    private static final String SYNC_USER = "netease-sync";
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    /** 网易业务错误码提取（如「网易 API 返回错误: code=-3, ...」） */
    private static final Pattern NETEASE_CODE_PATTERN = Pattern.compile("code=(-?\\d+)");

    @Autowired
    private NeteaseApiClient apiClient;

    @Autowired
    private NeteaseDeptSyncService deptSyncService;

    @Autowired
    private NeteaseUserSyncService userSyncService;

    @Autowired
    private NeteaseSyncInspectService inspectService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private IThirdSyncLogService logService;

    private int consecutiveFailures = 0;

    /**
     * 根据配置模式执行同步（由定时任务调用）
     */
    public void sync()
    {
        String mode = configService.selectConfigByKey(CONFIG_MODE);
        if ("full".equals(mode))
        {
            log.info("执行全量同步");
            syncFull();
        }
        else
        {
            log.info("执行增量同步");
            syncIncremental();
        }
    }

    /**
     * 全量同步
     */
    public void syncFull()
    {
        long startTime = System.currentTimeMillis();
        try
        {
            String domain = configService.selectConfigByKey(CONFIG_DOMAIN);
            if (domain == null || domain.isEmpty())
            {
                throw new ServiceException("netease.domain 未配置");
            }

            // 获取最新版本号
            Long revision = apiClient.getMaxRevision();
            log.info("网易最新版本号: {}", revision);

            // 同步部门（网易顶级部门直接挂到根锚点 100 下）
            int deptCount = deptSyncService.syncFullDepts(getRootDeptId(), revision);

            // 同步用户
            int userCount = userSyncService.syncFullUsers(domain, getRootDeptId(), revision);

            // 更新本地版本号和模式
            if (revision != null)
            {
                updateSysConfig(CONFIG_REVISION, String.valueOf(revision));
            }
            updateSysConfig(CONFIG_MODE, "increment");

            consecutiveFailures = 0;
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("全量同步完成: 部门{}个, 用户{}个, 版本号{}, 耗时{}ms",
                    deptCount, userCount, revision, elapsed);
        }
        catch (Exception e)
        {
            consecutiveFailures++;
            log.error("全量同步失败 (连续失败{}次)", consecutiveFailures, e);
            throw new ServiceException("全量同步失败: " + e.getMessage());
        }
    }

    /**
     * 增量同步
     */
    public void syncIncremental()
    {
        long startTime = System.currentTimeMillis();
        try
        {
            String domain = configService.selectConfigByKey(CONFIG_DOMAIN);

            // 获取本地版本号
            String localRevStr = configService.selectConfigByKey(CONFIG_REVISION);
            long localRevision = (localRevStr != null && !localRevStr.isEmpty()) ? Long.parseLong(localRevStr) : 0;

            // 获取网易最新版本号
            Long latestRevision = apiClient.getMaxRevision();
            if (latestRevision == null)
            {
                log.warn("获取最新版本号失败，降级为全量同步");
                syncFull();
                return;
            }

            log.info("版本号比对: 本地={}, 网易最新={}", localRevision, latestRevision);

            if (latestRevision == localRevision)
            {
                log.info("版本号一致，无变更");
                return;
            }

            if (latestRevision < localRevision)
            {
                log.warn("本地版本号({})大于网易版本号({})，数据异常，降级为全量同步", localRevision, latestRevision);
                updateSysConfig(CONFIG_REVISION, "0");
                syncFull();
                return;
            }

            // 逐版本拉取变更
            int changeCount = 0;
            for (long rev = localRevision + 1; rev <= latestRevision; rev++)
            {
                try
                {
                    NeteaseRevisionData revData = apiClient.getRevisionData(rev);
                    if (revData == null || revData.getAccount() == null)
                    {
                        log.debug("版本 {} 无变更数据", rev);
                        continue;
                    }

                    String op = revData.getOp();
                    log.info("处理版本 {} 变更: op={}, account={}", rev, op, revData.getAccount().getAccountName());

                    switch (op != null ? op.toUpperCase() : "UPDATE")
                    {
                        case "ADD":
                            userSyncService.syncOneUser(revData.getAccount(), domain, getRootDeptId(), rev);
                            break;
                        case "UPDATE":
                            userSyncService.syncOneUser(revData.getAccount(), domain, getRootDeptId(), rev);
                            break;
                        case "DELETE":
                            userSyncService.softDisableUser(null, revData.getAccount().getAccountOpenId(), rev);
                            break;
                        default:
                            // 未知操作码，按 UPDATE 处理
                            userSyncService.syncOneUser(revData.getAccount(), domain, getRootDeptId(), rev);
                            break;
                    }
                    changeCount++;

                    // 更新本地版本号
                    updateSysConfig(CONFIG_REVISION, String.valueOf(rev));
                }
                catch (Exception e)
                {
                    log.error("处理版本 {} 变更失败", rev, e);
                    // 解析网易业务错误码，判定该版本是否可读
                    Integer neteaseCode = extractNeteaseCode(e);
                    // 版本不存在(-4)或版本数据不可读(-3，网易服务端组装失败)：无法通过增量通道获取，
                    // 降级为全量同步绕过毒版本，避免本地版本号永久卡死、同版本夜夜重试
                    if (neteaseCode != null && (neteaseCode == -3 || neteaseCode == -4))
                    {
                        log.warn("版本 {} 无法读取 (code={})，降级为全量同步", rev, neteaseCode);
                        updateSysConfig(CONFIG_REVISION, "0");
                        syncFull();
                        return;
                    }
                    // 其他错误：跳过该版本，继续下一个
                }
            }

            consecutiveFailures = 0;
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("增量同步完成: 处理{}个变更, 版本号{}->{}, 耗时{}ms",
                    changeCount, localRevision, latestRevision, elapsed);
        }
        catch (Exception e)
        {
            consecutiveFailures++;
            log.error("增量同步失败 (连续失败{}次)", consecutiveFailures, e);
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES)
            {
                log.warn("连续{}次增量同步失败，自动降级为全量同步", MAX_CONSECUTIVE_FAILURES);
                updateSysConfig(CONFIG_MODE, "full");
                consecutiveFailures = 0;
            }
            throw new ServiceException("增量同步失败: " + e.getMessage());
        }
    }

    /**
     * 执行全量巡检
     */
    public void inspect()
    {
        inspectService.inspect();
    }

    /**
     * 查询若依部门树的根节点（parent_id=0, ancestors='0'），
     * 网易顶级部门直接挂到该根节点下。
     */
    private Long getRootDeptId()
    {
        List<SysDept> roots = deptMapper.selectDeptList(null)
                .stream()
                .filter(d -> d.getParentId() != null && d.getParentId() == 0L)
                .collect(Collectors.toList());
        if (roots.isEmpty())
        {
            throw new ServiceException("若依部门树根节点缺失（parent_id=0），请先初始化 sys_dept");
        }
        return roots.get(0).getDeptId();
    }

    /**
     * 从异常信息中解析网易业务错误码（如「网易 API 返回错误: code=-3, ...」），
     * 解析不到返回 null
     */
    private Integer extractNeteaseCode(Exception e)
    {
        String message = e.getMessage();
        if (message == null)
        {
            return null;
        }
        Matcher matcher = NETEASE_CODE_PATTERN.matcher(message);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /**
     * 更新 sys_config 配置值
     */
    private void updateSysConfig(String key, String value)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        SysConfig config = configMapper.selectConfig(query);
        if (config != null)
        {
            config.setConfigValue(value);
            config.setUpdateBy(SYNC_USER);
            configService.updateConfig(config);
        }
    }
}