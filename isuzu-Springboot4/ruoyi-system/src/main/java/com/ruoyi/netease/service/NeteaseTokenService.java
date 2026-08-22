package com.ruoyi.netease.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.netease.domain.NeteaseTokenResp;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 网易企业邮箱 Token 管理服务
 * 使用 volatile + synchronized 双重检查锁缓存 token，自动刷新
 *
 * @author isuzu
 */
@Service
public class NeteaseTokenService
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseTokenService.class);

    private static final String CONFIG_APP_ID = "netease.app.id";
    private static final String CONFIG_AUTH_CODE = "netease.auth.code";
    private static final String CONFIG_ORG_OPEN_ID = "netease.org.open.id";
    private static final String REDIS_TOKEN_KEY = "netease:token:refresh";
    private static final String DEFAULT_SERVER_URL = "https://api.qiye.163.com";

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile LocalDateTime accessExpireTime;
    private final Object lock = new Object();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取有效的 accessToken，过期自动刷新
     */
    public String getAccessToken()
    {
        if (accessToken == null || isExpired())
        {
            synchronized (lock)
            {
                if (accessToken == null || isExpired())
                {
                    refreshToken();
                }
            }
        }
        return accessToken;
    }

    /**
     * 强制刷新 token（token 过期或 API 返回 -301 时调用）
     */
    public void forceRefresh()
    {
        synchronized (lock)
        {
            refreshToken();
        }
    }

    public String getOrgOpenId() { return configService.selectConfigByKey(CONFIG_ORG_OPEN_ID); }
    public String getAppId() { return configService.selectConfigByKey(CONFIG_APP_ID); }
    public String getAuthCode() { return configService.selectConfigByKey(CONFIG_AUTH_CODE); }

    private boolean isExpired()
    {
        return accessExpireTime == null || LocalDateTime.now().isAfter(accessExpireTime.minusMinutes(5));
    }

    private void refreshToken()
    {
        // 尝试从 Redis 恢复 refreshToken
        if (refreshToken == null)
        {
            String cached = redisCache.getCacheObject(REDIS_TOKEN_KEY);
            if (cached != null && !cached.isEmpty())
            {
                refreshToken = cached;
            }
        }

        // 优先用 refreshToken 刷新
        if (refreshToken != null && !refreshToken.isEmpty())
        {
            try
            {
                String serverUrl = getServerUrl();
                String url = serverUrl + "/api/pub/token/refresh?refreshToken=" + refreshToken;
                String respBody = doPost(url, null);
                JSONObject resp = JSON.parseObject(respBody);
                if (resp.getInteger("code") == 0)
                {
                    NeteaseTokenResp tokenResp = resp.getObject("data", NeteaseTokenResp.class);
                    applyToken(tokenResp);
                    return;
                }
                log.warn("refreshToken 刷新失败, code={}, message={}", resp.getInteger("code"), resp.getString("message"));
            }
            catch (Exception e)
            {
                log.warn("refreshToken 刷新失败，回退到 appLogin: {}", e.getMessage());
            }
        }

        // 回退到 appLogin
        String authCode = getAuthCode();
        if (authCode == null || authCode.isEmpty())
        {
            throw new ServiceException("netease.auth.code 未配置，无法获取 token");
        }
        try
        {
            String serverUrl = getServerUrl();
            Map<String, String> body = new HashMap<>();
            body.put("appId", getAppId());
            body.put("orgOpenId", getOrgOpenId());
            body.put("authCode", authCode);

            String respBody = doPost(serverUrl + "/api/pub/token/acquireToken", JSON.toJSONString(body));
            JSONObject resp = JSON.parseObject(respBody);
            if (resp.getInteger("code") != 0)
            {
                throw new ServiceException("获取 token 失败: code=" + resp.getInteger("code") + ", message=" + resp.getString("message"));
            }
            NeteaseTokenResp tokenResp = resp.getObject("data", NeteaseTokenResp.class);
            applyToken(tokenResp);
        }
        catch (ServiceException e) { throw e; }
        catch (Exception e)
        {
            throw new ServiceException("获取网易企业邮箱 token 失败: " + e.getMessage());
        }
    }

    private void applyToken(NeteaseTokenResp resp)
    {
        this.accessToken = resp.getAccessToken();
        this.refreshToken = resp.getRefreshToken();
        if (resp.getAccessTokenExpiredTime() != null)
        {
            try
            {
                this.accessExpireTime = LocalDateTime.parse(
                        resp.getAccessTokenExpiredTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            catch (Exception e)
            {
                log.warn("解析 token 过期时间失败: {}", resp.getAccessTokenExpiredTime());
            }
        }
        if (this.refreshToken != null)
        {
            redisCache.setCacheObject(REDIS_TOKEN_KEY, this.refreshToken);
        }
        log.info("网易 token 已更新, accessToken 过期时间: {}", accessExpireTime);
    }

    private String doPost(String url, String jsonBody) throws Exception
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=UTF-8");
        if (jsonBody != null)
        {
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        else
        {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public String getServerUrl()
    {
        String url = configService.selectConfigByKey("netease.server.url");
        return (url != null && !url.isEmpty()) ? url : DEFAULT_SERVER_URL;
    }
}