package com.ruoyi.netease.job;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.netease.service.impl.NeteaseDeptSyncService;
import com.ruoyi.netease.service.impl.NeteaseSyncService;

/**
 * 网易企业邮箱同步定时任务
 * 通过 sys_job 表注册，由 Quartz 调度执行
 *
 * @author isuzu
 */
@Component("neteaseSyncJob")
public class NeteaseSyncJob
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseSyncJob.class);
    private static final String LOCK_KEY = "netease:sync:lock";
    private static final String DEPT_LOCK_KEY = "netease:dept-reconcile:lock";
    private static final long LOCK_TTL_MINUTES = 30;

    @Autowired
    private NeteaseSyncService syncService;

    @Autowired
    private NeteaseDeptSyncService deptSyncService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 无参入口 — 由 Quartz 通过 JobInvokeUtil 反射调用
     */
    public void syncDaily()
    {
        if (!tryLock())
        {
            log.info("同步任务正在执行中，跳过本次调度");
            return;
        }
        try
        {
            log.info("开始执行网易邮箱日常同步任务");
            syncService.sync();
            log.info("网易邮箱日常同步任务完成");
        }
        catch (Exception e)
        {
            log.error("网易邮箱日常同步任务失败", e);
        }
        finally
        {
            unlock();
        }
    }

    /**
     * 手动触发全量同步
     */
    public void syncFull()
    {
        if (!tryLock())
        {
            log.info("同步任务正在执行中，跳过本次触发");
            return;
        }
        try
        {
            log.info("开始执行网易邮箱全量同步");
            syncService.syncFull();
            log.info("网易邮箱全量同步完成");
        }
        catch (Exception e)
        {
            log.error("网易邮箱全量同步失败", e);
        }
        finally
        {
            unlock();
        }
    }

    /**
     * 全量巡检
     */
    public void inspect()
    {
        if (!tryLock())
        {
            log.info("巡检任务正在执行中，跳过本次调度");
            return;
        }
        try
        {
            log.info("开始执行网易邮箱全量巡检");
            syncService.inspect();
            log.info("网易邮箱全量巡检完成");
        }
        catch (Exception e)
        {
            log.error("网易邮箱全量巡检失败", e);
        }
        finally
        {
            unlock();
        }
    }

    /**
     * 部门对账（独立锁，不与日常同步互斥）
     */
    public void syncDeptReconcile()
    {
        if (!tryLock(DEPT_LOCK_KEY))
        {
            log.info("部门对账任务正在执行中，跳过本次调度");
            return;
        }
        try
        {
            log.info("开始执行网易邮箱部门对账任务");
            int changeCount = deptSyncService.reconcileDepts();
            log.info("网易邮箱部门对账任务完成，变更{}条", changeCount);
        }
        catch (Exception e)
        {
            log.error("网易邮箱部门对账任务失败", e);
        }
        finally
        {
            unlock(DEPT_LOCK_KEY);
        }
    }

    private boolean tryLock()
    {
        return tryLock(LOCK_KEY);
    }

    private void unlock()
    {
        unlock(LOCK_KEY);
    }

    private boolean tryLock(String lockKey)
    {
        try
        {
            RedisTemplate redisTemplate = redisCache.redisTemplate;
            Boolean locked = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, UUID.randomUUID().toString(), LOCK_TTL_MINUTES, TimeUnit.MINUTES);
            return Boolean.TRUE.equals(locked);
        }
        catch (Exception e)
        {
            log.warn("获取 Redis 锁失败，继续执行: {}", e.getMessage());
            return true; // Redis 不可用时允许执行
        }
    }

    private void unlock(String lockKey)
    {
        try
        {
            redisCache.deleteObject(lockKey);
        }
        catch (Exception e)
        {
            log.warn("释放 Redis 锁失败: {}", e.getMessage());
        }
    }
}