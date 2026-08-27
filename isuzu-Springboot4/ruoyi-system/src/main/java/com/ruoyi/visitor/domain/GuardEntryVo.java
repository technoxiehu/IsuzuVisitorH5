package com.ruoyi.visitor.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 入场记录视图对象（门卫电脑端入场记录查询页，见 docs/03_接口契约.md §3.15，v1.10）
 *
 * 手机号/身份证号返回前已脱敏；申请单已撤销/删除的记录仍展示（历史事实，审计留存）。
 *
 * @author isuzu
 */
public class GuardEntryVo
{
    /** 入场记录ID */
    private String entryId;

    /** 申请单号 */
    private String applicationId;

    /** 访客姓名 */
    private String visitorName;

    /** 访客单位 */
    private String visitorCompany;

    /** 访客手机号(脱敏) */
    private String visitorPhone;

    /** 访客身份证号(脱敏) */
    private String visitorIdCard;

    /** 车牌号(无则 null) */
    private String plateNo;

    /** 被访人姓名 */
    private String hostName;

    /** 访问开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 访问结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 放行门卫姓名 */
    private String operatorName;

    /** 放行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date entryTime;

    public String getEntryId()
    {
        return entryId;
    }

    public void setEntryId(String entryId)
    {
        this.entryId = entryId;
    }

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public String getVisitorName()
    {
        return visitorName;
    }

    public void setVisitorName(String visitorName)
    {
        this.visitorName = visitorName;
    }

    public String getVisitorCompany()
    {
        return visitorCompany;
    }

    public void setVisitorCompany(String visitorCompany)
    {
        this.visitorCompany = visitorCompany;
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

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public Date getEntryTime()
    {
        return entryTime;
    }

    public void setEntryTime(Date entryTime)
    {
        this.entryTime = entryTime;
    }
}
