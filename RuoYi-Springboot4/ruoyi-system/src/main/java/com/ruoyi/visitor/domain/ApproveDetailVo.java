package com.ruoyi.visitor.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 审批详情视图对象（申请单 + 访客信息，见 docs/03_接口契约.md §3.8）
 *
 * @author isuzu
 */
public class ApproveDetailVo
{
    /** 申请单号 */
    private String applicationId;

    /** 申请人姓名 */
    private String visitorName;

    /** 申请人单位 */
    private String visitorCompany;

    /** 被访问人 */
    private String hostName;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 访问事由 */
    private String reason;

    /** 状态(0未审批 1通过 2拒绝) */
    private String status;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
