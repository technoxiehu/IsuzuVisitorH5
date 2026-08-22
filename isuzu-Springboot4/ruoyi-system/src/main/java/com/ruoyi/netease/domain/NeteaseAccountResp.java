package com.ruoyi.netease.domain;

import java.util.List;
import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 网易企业邮箱账号信息
 *
 * @author isuzu
 */
public class NeteaseAccountResp
{
    @JSONField(name = "accountOpenId")
    private String accountOpenId;

    @JSONField(name = "accountName")
    private String accountName;

    @JSONField(name = "name", alternateNames = {"nickname"})
    private String name;

    @JSONField(name = "mobile")
    private String mobile;

    @JSONField(name = "jobNumber")
    private String jobNumber;

    private String job;

    private Integer gender;

    private String remark;

    @JSONField(name = "unitList")
    private List<String> unitList;

    @JSONField(name = "unitId")
    private String unitId;

    @JSONField(name = "unitName")
    private String unitName;

    private Integer status;

    private String domain;

    public String getAccountOpenId() { return accountOpenId; }
    public void setAccountOpenId(String accountOpenId) { this.accountOpenId = accountOpenId; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getJobNumber() { return jobNumber; }
    public void setJobNumber(String jobNumber) { this.jobNumber = jobNumber; }
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<String> getUnitList() { return unitList; }
    public void setUnitList(List<String> unitList) { this.unitList = unitList; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
}