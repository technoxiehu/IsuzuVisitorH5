package com.ruoyi.netease.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.netease.domain.NeteaseAccountResp;
import com.ruoyi.netease.domain.NeteaseRevisionData;
import com.ruoyi.netease.domain.NeteaseUnitItem;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 网易企业邮箱 API 客户端
 * 封装所有网易开放平台 API 调用，自动处理 token、分页、重试
 *
 * @author isuzu
 */
@Service
public class NeteaseApiClient
{
    private static final Logger log = LoggerFactory.getLogger(NeteaseApiClient.class);

    private static final String CONFIG_DOMAIN = "netease.domain";
    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Autowired
    private NeteaseTokenService tokenService;

    @Autowired
    private ISysConfigService configService;

    /**
     * 获取全量部门列表
     */
    public List<NeteaseUnitItem> getAllUnits()
    {
        String domain = getDomain();
        Map<String, Object> body = new HashMap<>();
        body.put("domain", domain);
        String respBody = post("/api/open/unit/getUnitList", body);
        JSONObject resp = JSON.parseObject(respBody);
        checkCode(resp);
        return resp.getJSONArray("data").toList(NeteaseUnitItem.class);
    }

    /**
     * 分页获取全量账号列表
     */
    public List<NeteaseAccountResp> getAllAccounts()
    {
        String domain = getDomain();
        List<NeteaseAccountResp> allAccounts = new ArrayList<>();
        int pageNum = 1;
        int pageSize = 50;
        boolean hasMore = true;

        while (hasMore)
        {
            Map<String, Object> body = new HashMap<>();
            body.put("domain", domain);
            body.put("pageNum", pageNum);
            body.put("pageSize", pageSize);
            body.put("recursion", 1);

            String respBody = post("/api/open/unit/getAccountList", body);
            JSONObject resp = JSON.parseObject(respBody);
            checkCode(resp);
            JSONObject data = resp.getJSONObject("data");
            if (data == null) {
                break;
            }
            List<NeteaseAccountResp> pageList = null;
            if (data.getJSONArray("list") != null) {
                pageList = data.getJSONArray("list").toList(NeteaseAccountResp.class);
            }

            if (pageList == null || pageList.isEmpty())
            {
                hasMore = false;
            }
            else
            {
                allAccounts.addAll(pageList);
                pageNum++;
                // 分页间隔，避免频率限制
                sleep(RETRY_DELAY_MS);
            }
        }
        log.info("获取全量账号完成，共 {} 条", allAccounts.size());
        return allAccounts;
    }

    /**
     * 获取最新版本号
     */
    public Long getMaxRevision()
    {
        String orgOpenId = tokenService.getOrgOpenId();
        Map<String, Object> body = new HashMap<>();
        body.put("orgOpenId", orgOpenId);
        String respBody = post("/api/open/address/getMaxRevision", body);
        JSONObject resp = JSON.parseObject(respBody);
        checkCode(resp);
        JSONObject data = resp.getJSONObject("data");
        return data != null ? data.getLong("revision") : null;
    }

    /**
     * 获取版本变更数据
     */
    public NeteaseRevisionData getRevisionData(long revision)
    {
        String orgOpenId = tokenService.getOrgOpenId();
        Map<String, Object> body = new HashMap<>();
        body.put("orgOpenId", orgOpenId);
        body.put("revision", revision);
        String respBody = post("/api/open/address/getRevisionData", body);
        JSONObject resp = JSON.parseObject(respBody);
        checkCode(resp);
        JSONObject data = resp.getJSONObject("data");
        return data != null ? data.toJavaObject(NeteaseRevisionData.class) : null;
    }

    // ---- 内部方法 ----

    private String post(String apiPath, Map<String, Object> body)
    {
        return postWithRetry(apiPath, body, 0);
    }

    private String postWithRetry(String apiPath, Map<String, Object> body, int retryCount)
    {
        try
        {
            String serverUrl = tokenService.getServerUrl();
            String url = serverUrl + apiPath;
            String jsonBody = JSON.toJSONString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("qiye-access-token", tokenService.getAccessToken())
                    .header("qiye-app-id", tokenService.getAppId())
                    .header("qiye-org-open-id", tokenService.getOrgOpenId())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String respBody = response.body();

            // 检查 token 是否过期
            JSONObject resp = JSON.parseObject(respBody);
            Integer code = resp.getInteger("code");
            if (code != null && (code == -301 || code == -300))
            {
                // token 过期，刷新后重试一次
                if (retryCount < 1)
                {
                    log.info("Token 过期 (code={})，刷新后重试", code);
                    tokenService.forceRefresh();
                    return postWithRetry(apiPath, body, retryCount + 1);
                }
            }
            // 频率限制，指数退避
            if (code != null && (code == -422 || code == -423))
            {
                if (retryCount < MAX_RETRY)
                {
                    long delay = RETRY_DELAY_MS * (long) Math.pow(2, retryCount);
                    log.warn("频率限制 (code={})，{}ms 后重试 (第{}次)", code, delay, retryCount + 1);
                    sleep(delay);
                    return postWithRetry(apiPath, body, retryCount + 1);
                }
                throw new ServiceException("网易 API 频率限制，已达最大重试次数");
            }

            return respBody;
        }
        catch (ServiceException e) { throw e; }
        catch (Exception e)
        {
            throw new ServiceException("网易 API 调用失败: " + apiPath + " - " + e.getMessage());
        }
    }

    private void checkCode(JSONObject resp)
    {
        Integer code = resp.getInteger("code");
        if (code == null || code != 0)
        {
            throw new ServiceException("网易 API 返回错误: code=" + code + ", message=" + resp.getString("message"));
        }
    }

    private String getDomain()
    {
        return configService.selectConfigByKey(CONFIG_DOMAIN);
    }

    private void sleep(long ms)
    {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}