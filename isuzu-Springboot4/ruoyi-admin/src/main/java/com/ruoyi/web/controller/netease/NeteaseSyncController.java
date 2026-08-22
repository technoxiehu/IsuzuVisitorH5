package com.ruoyi.web.controller.netease;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.netease.job.NeteaseSyncJob;
import com.ruoyi.netease.service.IThirdSyncLogService;
import com.ruoyi.netease.service.NeteaseTokenService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 网易企业邮箱同步管理接口
 * 仅管理员可访问
 *
 * @author isuzu
 */
@RestController
@RequestMapping("/system/neteaseSync")
public class NeteaseSyncController
{
    @Autowired
    private NeteaseSyncJob syncJob;

    @Autowired
    private NeteaseTokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private IThirdSyncLogService logService;

    /**
     * 获取同步状态
     */
    @GetMapping("/status")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult status()
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("revision", configService.selectConfigByKey("netease.sync.revision"));
        ajax.put("mode", configService.selectConfigByKey("netease.sync.mode"));
        ajax.put("domain", configService.selectConfigByKey("netease.domain"));
        ajax.put("recentLogs", logService.getRecentLogs("sync", 10));
        return ajax;
    }

    /**
     * 触发全量同步（管理员手动触发）
     */
    @PostMapping("/full")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult triggerFullSync()
    {
        syncJob.syncFull();
        return AjaxResult.success("全量同步已触发，请查看日志确认结果");
    }

    /**
     * 触发增量同步
     */
    @PostMapping("/increment")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult triggerIncrement()
    {
        syncJob.syncDaily();
        return AjaxResult.success("增量同步已触发");
    }

    /**
     * 触发全量巡检
     */
    @PostMapping("/inspect")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult triggerInspect()
    {
        syncJob.inspect();
        return AjaxResult.success("全量巡检已触发");
    }

    /**
     * 强制刷新 Token
     */
    @PostMapping("/refreshToken")
    @PreAuthorize("@ss.hasRole('admin')")
    public AjaxResult refreshToken()
    {
        tokenService.forceRefresh();
        return AjaxResult.success("Token 已刷新");
    }
}