package com.ruoyi.visitor.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 被访人对象（来源于若依 sys_user / sys_dept，无独立表）
 *
 * @author isuzu
 */
public class VisitorHost
{
    private static final long serialVersionUID = 1L;

    /** 被访人ID(sys_user.user_id) */
    private Long userId;

    /** 姓名(sys_user.nick_name) */
    private String name;

    /** 部门(sys_dept.dept_name) */
    private String deptName;

    /** 邮箱(sys_user.email) */
    private String email;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("name", getName())
            .append("deptName", getDeptName())
            .append("email", getEmail())
            .toString();
    }
}
