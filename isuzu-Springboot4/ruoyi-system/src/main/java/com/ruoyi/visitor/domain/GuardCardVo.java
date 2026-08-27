package com.ruoyi.visitor.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 门卫有效单据卡片视图对象（门卫电脑端核验页，见 docs/03_接口契约.md §3.13，v1.10）
 *
 * 手机号/身份证号返回前已脱敏；plateNo 无则 null；companions 无则空数组（恒存在）。
 *
 * @author isuzu
 */
public class GuardCardVo
{
    /** 申请单号 */
    private String applicationId;

    /** 访客ID */
    private String visitorId;

    /** 访客姓名 */
    private String visitorName;

    /** 访客手机号(脱敏 138****0000) */
    private String visitorPhone;

    /** 访客身份证号(脱敏 110***********1234) */
    private String visitorIdCard;

    /** 访客单位 */
    private String visitorCompany;

    /** 访客头像(相对路径 /profile/...，同源解析) */
    private String visitorAvatar;

    /** 车牌号(无则 null) */
    private String plateNo;

    /** 被访人姓名 */
    private String hostName;

    /** 访问事由 */
    private String reason;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 状态(1通过) */
    private String status;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 随行人员名单(身份证号脱敏) */
    private List<VisitorCompanion> companions;

    /** 当日放行次数 */
    private Integer entryCount;

    /** 当日最近一次放行时间(无则 null) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastEntryTime;

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public String getVisitorId()
    {
        return visitorId;
    }

    public void setVisitorId(String visitorId)
    {
        this.visitorId = visitorId;
    }

    public String getVisitorName()
    {
        return visitorName;
    }

    public void setVisitorName(String visitorName)
    {
        this.visitorName = visitorName;
    }

    public String getVisitorPhone()
    {
        return visitorPhone;
    }

    public void setVisitorPhone(String visitorPhone)
    {
        this.visitorPhone = visitorPhone;
    }

    public String getVisitorIdCard()
    {
        return visitorIdCard;
    }

    public void setVisitorIdCard(String visitorIdCard)
    {
        this.visitorIdCard = visitorIdCard;
    }

    public String getVisitorCompany()
    {
        return visitorCompany;
    }

    public void setVisitorCompany(String visitorCompany)
    {
        this.visitorCompany = visitorCompany;
    }

    public String getVisitorAvatar()
    {
        return visitorAvatar;
    }

    public void setVisitorAvatar(String visitorAvatar)
    {
        this.visitorAvatar = visitorAvatar;
    }

    public String getPlateNo()
    {
        return plateNo;
    }

    public void setPlateNo(String plateNo)
    {
        this.plateNo = plateNo;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getApproveTime()
    {
        return approveTime;
    }

    public void setApproveTime(Date approveTime)
    {
        this.approveTime = approveTime;
    }

    public List<VisitorCompanion> getCompanions()
    {
        return companions;
    }

    public void setCompanions(List<VisitorCompanion> companions)
    {
        this.companions = companions;
    }

    public Integer getEntryCount()
    {
        return entryCount;
    }

    public void setEntryCount(Integer entryCount)
    {
        this.entryCount = entryCount;
    }

    public Date getLastEntryTime()
    {
        return lastEntryTime;
    }

    public void setLastEntryTime(Date lastEntryTime)
    {
        this.lastEntryTime = lastEntryTime;
    }
}
