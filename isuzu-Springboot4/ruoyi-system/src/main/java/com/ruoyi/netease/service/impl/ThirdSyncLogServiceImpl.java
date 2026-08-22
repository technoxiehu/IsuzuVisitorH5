package com.ruoyi.netease.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.netease.domain.ThirdSyncLog;
import com.ruoyi.netease.mapper.ThirdSyncLogMapper;
import com.ruoyi.netease.service.IThirdSyncLogService;

/**
 * 第三方同步日志表 Service 实现
 *
 * @author isuzu
 */
@Service
public class ThirdSyncLogServiceImpl implements IThirdSyncLogService
{
    @Autowired
    private ThirdSyncLogMapper logMapper;

    @Override
    public void record(String syncType, String syncAction, String thirdId, Long ruoyiId,
                       String beforeJson, String afterJson, boolean success, String errorMsg, Long syncVersion)
    {
        ThirdSyncLog log = new ThirdSyncLog();
        log.setSyncType(syncType);
        log.setSyncAction(syncAction);
        log.setThirdId(thirdId);
        log.setRuoyiId(ruoyiId);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setSyncStatus(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        log.setSyncVersion(syncVersion);
        logMapper.insertLog(log);
    }

    @Override
    public void recordInspectFix(String thirdId, Long ruoyiId, String beforeJson, String afterJson, Long syncVersion)
    {
        record("inspect", "INSPECT_FIX", thirdId, ruoyiId, beforeJson, afterJson, true, null, syncVersion);
    }

    @Override
    public List<ThirdSyncLog> getRecentLogs(String syncType, int limit)
    {
        return logMapper.selectRecentLogs(syncType, limit);
    }

    @Override
    public int countFailuresSince(String syncType, String sinceTime)
    {
        return logMapper.countFailuresSince(syncType, sinceTime);
    }
}