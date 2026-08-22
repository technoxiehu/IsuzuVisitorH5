package com.ruoyi.netease.mapper;

import java.util.List;
import com.ruoyi.netease.domain.ThirdSyncLog;

/**
 * 第三方同步日志表 Mapper
 *
 * @author isuzu
 */
public interface ThirdSyncLogMapper
{
    int insertLog(ThirdSyncLog log);

    List<ThirdSyncLog> selectRecentLogs(String syncType, int limit);

    int countFailuresSince(String syncType, String sinceTime);
}