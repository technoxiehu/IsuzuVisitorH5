package com.ruoyi.visitor.domain;

import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 访客申请单对象 visitor_application
 *
 * @author isuzu
 */
public class VisitorApplication
{
    private static final long serialVersionUID = 1L;

    /** 申请单号(后端生成UUID) */
    private String applicationId;

    /** 访客ID */
    private String visitorId;

    /** 被访人(sys_user.user_id) */
    private Long hostId;

    /** 被访人姓名 */
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

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 删除标志(0存在 1已撤销/删除，逻辑删除) */
    private String delFlag;

    /** 随行人员名单（提交入参 + 列表/详情返回，见 docs/03_接口契约.md §3.6） */
    private List<VisitorCompanion> companions;

    /** 是否在访问时间窗口内（列表返回用，应用服务器时间计算，非表字段） */
    private Boolean effective;

    /** 一次性提交令牌（防重复提交，提交时由前端携带，非表字段） */
    private String submitToken;

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    @NotBlank(message = "访客ID不能为空")
    public String getVisitorId()
    {
        return visitorId;
    }

    public void setVisitorId(String visitorId)
    {
        this.visitorId = visitorId;
    }

    @NotNull(message = "被访人不能为空")
    public Long getHostId()
    {
        return hostId;
    }

    public void setHostId(Long hostId)
    {
        this.hostId = hostId;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    @NotNull(message = "开始时间不能为空")
    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    @NotNull(message = "结束时间不能为空")
    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    @NotBlank(message = "访问事由不能为空")
    @Size(min = 0, max = 200, message = "访问事由长度不能超过200个字符")
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

    public Date getApproveTime()
    {
        return approveTime;
    }

    public void setApproveTime(Date approveTime)
    {
        this.approveTime = approveTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public List<VisitorCompanion> getCompanions()
    {
        return companions;
    }

    public void setCompanions(List<VisitorCompanion> companions)
    {
        this.companions = companions;
    }

    public Boolean getEffective()
    {
        return effective;
    }

    public void setEffective(Boolean effective)
    {
        this.effective = effective;
    }

    public String getSubmitToken()
    {
        return submitToken;
    }

    public void setSubmitToken(String submitToken)
    {
        this.submitToken = submitToken;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("applicationId", getApplicationId())
            .append("visitorId", getVisitorId())
            .append("hostId", getHostId())
            .append("hostName", getHostName())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("reason", getReason())
            .append("status", getStatus())
            .append("approveTime", getApproveTime())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
