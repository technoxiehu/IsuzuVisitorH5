package com.ruoyi.visitor.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 随行人员对象 visitor_companion（申请单附属名单，0~5 人，不注册）
 *
 * 身份证号明文存储，对外展示一律脱敏（IdCardUtils.mask），toString 不输出身份证号。
 *
 * @author isuzu
 */
public class VisitorCompanion
{
    private static final long serialVersionUID = 1L;

    /** 随行人员ID(后端生成UUID) */
    private String companionId;

    /** 所属申请单号 */
    private String applicationId;

    /** 姓名 */
    private String name;

    /** 身份证号(明文存储,展示脱敏) */
    private String idCard;

    /** 序号(提交顺序,列表/详情按此排序) */
    private Integer sortNo;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @NotBlank(message = "随行人员ID不能为空")
    public String getCompanionId()
    {
        return companionId;
    }

    public void setCompanionId(String companionId)
    {
        this.companionId = companionId;
    }

    @NotBlank(message = "申请单号不能为空")
    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    @NotBlank(message = "随行人员姓名不能为空")
    @Size(min = 0, max = 20, message = "随行人员姓名长度不能超过20个字符")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @NotBlank(message = "随行人员身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "随行人员身份证号格式不正确")
    public String getIdCard()
    {
        return idCard;
    }

    public void setIdCard(String idCard)
    {
        this.idCard = idCard;
    }

    public Integer getSortNo()
    {
        return sortNo;
    }

    public void setSortNo(Integer sortNo)
    {
        this.sortNo = sortNo;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("companionId", getCompanionId())
            .append("applicationId", getApplicationId())
            .append("name", getName())
            .append("sortNo", getSortNo())
            .append("createTime", getCreateTime())
            // 注意：不输出 idCard，防止日志泄漏身份证号明文
            .toString();
    }
}
