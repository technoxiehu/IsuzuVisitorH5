package com.ruoyi.netease.service;

import com.ruoyi.netease.domain.ThirdSyncLog;
import java.util.List;

/**
 * 第三方同步日志表 Service
 *
 * @author isuzu
 */
public interface IThirdSyncLogService
{
    /**
     * 记录同步日志
     */
    void record(String syncType, String syncAction, String thirdId, Long ruoyiId,
                String beforeJson, String afterJson, boolean success, String errorMsg, Long syncVersion);

    /**
     * 记录巡检修复日志
     */
    void recordInspectFix(String thirdId, Long ruoyiId, String beforeJson, String afterJson, Long syncVersion);

    /**
     * 查询最近日志
     */
    List<ThirdSyncLog> getRecentLogs(String syncType, int limit);

    /**
     * 统计指定时间以来的失败次数
     */
    int countFailuresSince(String syncType, String sinceTime);
}