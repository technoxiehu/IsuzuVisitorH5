package com.ruoyi.netease.domain;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 网易企业邮箱 Token 响应
 *
 * @author isuzu
 */
public class NeteaseTokenResp
{
    @JSONField(name = "accessToken")
    private String accessToken;

    @JSONField(name = "refreshToken")
    private String refreshToken;

    @JSONField(name = "accessTokenExpiredTime")
    private String accessTokenExpiredTime;

    @JSONField(name = "refreshTokenExpiredTime")
    private String refreshTokenExpiredTime;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public String getAccessTokenExpiredTime() { return accessTokenExpiredTime; }
    public void setAccessTokenExpiredTime(String accessTokenExpiredTime) { this.accessTokenExpiredTime = accessTokenExpiredTime; }
    public String getRefreshTokenExpiredTime() { return refreshTokenExpiredTime; }
    public void setRefreshTokenExpiredTime(String refreshTokenExpiredTime) { this.refreshTokenExpiredTime = refreshTokenExpiredTime; }
}