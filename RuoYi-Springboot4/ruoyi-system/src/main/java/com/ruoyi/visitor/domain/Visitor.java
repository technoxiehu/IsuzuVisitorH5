package com.ruoyi.visitor.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 访客对象 visitor
 *
 * @author isuzu
 */
public class Visitor
{
    private static final long serialVersionUID = 1L;

    /** 访客ID(前端生成UUID) */
    private String visitorId;

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 单位 */
    private String company;

    /** 头像URL */
    private String avatar;

    /** 注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @NotBlank(message = "访客ID不能为空")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$", message = "访客ID格式不正确")
    public String getVisitorId()
    {
        return visitorId;
    }

    public void setVisitorId(String visitorId)
    {
        this.visitorId = visitorId;
    }

    @NotBlank(message = "姓名不能为空")
    @Size(min = 0, max = 20, message = "姓名长度不能超过20个字符")
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    @NotBlank(message = "单位不能为空")
    public String getCompany()
    {
        return company;
    }

    public void setCompany(String company)
    {
        this.company = company;
    }

    @NotBlank(message = "头像不能为空")
    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("visitorId", getVisitorId())
            .append("name", getName())
            .append("phone", getPhone())
            .append("company", getCompany())
            .append("avatar", getAvatar())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
