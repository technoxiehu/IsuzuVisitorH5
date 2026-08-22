package com.ruoyi.netease.domain;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 网易企业邮箱版本变更数据
 *
 * @author isuzu
 */
public class NeteaseRevisionData
{
    private String domain;

    private Long revision;

    private String op;

    @JSONField(name = "orgOpenId")
    private String orgOpenId;

    private NeteaseAccountResp account;

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public Long getRevision() { return revision; }
    public void setRevision(Long revision) { this.revision = revision; }
    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
    public String getOrgOpenId() { return orgOpenId; }
    public void setOrgOpenId(String orgOpenId) { this.orgOpenId = orgOpenId; }
    public NeteaseAccountResp getAccount() { return account; }
    public void setAccount(NeteaseAccountResp account) { this.account = account; }
}