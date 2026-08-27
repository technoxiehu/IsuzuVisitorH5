package com.ruoyi.visitor.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 入场/放行记录对象 visitor_entry（门卫电脑端 v1.10，见 docs/03_接口契约.md §2.6）
 *
 * 同一申请单不限放行次数；放行=新增一条记录，不改动申请单状态；只存申请单 ID，不冗余访客信息。
 *
 * @author isuzu
 */
public class VisitorEntry
{
    /** 入场记录ID(后端生成UUID) */
    private String entryId;

    /** 申请单号 */
    private String applicationId;

    /** 放行门卫(sys_user.user_id) */
    private Long operatorId;

    /** 放行门卫姓名(冗余) */
    private String operatorName;

    /** 放行时间(应用服务器时间) */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

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

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
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
