package com.ruoyi.visitor.mapper;

import com.ruoyi.visitor.domain.Visitor;

/**
 * 访客信息 数据层
 *
 * @author isuzu
 */
public interface VisitorMapper
{
    /**
     * 根据访客ID查询访客信息
     *
     * @param visitorId 访客ID
     * @return 访客信息
     */
    public Visitor selectVisitorById(String visitorId);

    /**
     * 新增访客信息
     *
     * @param visitor 访客信息
     * @return 结果
     */
    public int insertVisitor(Visitor visitor);

    /**
     * 修改访客信息
     *
     * @param visitor 访客信息
     * @return 结果
     */
    public int updateVisitor(Visitor visitor);
}
