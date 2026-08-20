package com.ruoyi.visitor.service;

import com.ruoyi.visitor.domain.Visitor;

/**
 * 访客信息 业务层
 *
 * @author isuzu
 */
public interface IVisitorService
{
    /**
     * 根据访客ID查询访客信息
     *
     * @param visitorId 访客ID
     * @return 访客信息，查无返回 null
     */
    public Visitor selectVisitor(String visitorId);

    /**
     * 新用户注册（已注册时抛出业务异常 code=601）
     *
     * @param visitor 访客信息
     */
    public void registerVisitor(Visitor visitor);

    /**
     * 修改访客信息（访客不存在时抛出业务异常 code=601）
     *
     * @param visitor 访客信息
     */
    public void updateVisitor(Visitor visitor);
}
