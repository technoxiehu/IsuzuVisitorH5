package com.ruoyi.visitor.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.visitor.domain.Visitor;
import com.ruoyi.visitor.mapper.VisitorMapper;
import com.ruoyi.visitor.service.IVisitorService;

/**
 * 访客信息 业务层处理
 *
 * @author isuzu
 */
@Service
public class VisitorServiceImpl implements IVisitorService
{
    @Autowired
    private VisitorMapper visitorMapper;

    /**
     * 根据访客ID查询访客信息
     *
     * @param visitorId 访客ID
     * @return 访客信息
     */
    @Override
    public Visitor selectVisitor(String visitorId)
    {
        return visitorMapper.selectVisitorById(visitorId);
    }

    /**
     * 新用户注册
     *
     * @param visitor 访客信息
     */
    @Override
    public void registerVisitor(Visitor visitor)
    {
        Visitor exist = visitorMapper.selectVisitorById(visitor.getVisitorId());
        if (exist != null)
        {
            throw new ServiceException("该用户已注册", 601);
        }
        visitorMapper.insertVisitor(visitor);
    }

    /**
     * 修改访客信息
     *
     * @param visitor 访客信息
     */
    @Override
    public void updateVisitor(Visitor visitor)
    {
        Visitor exist = visitorMapper.selectVisitorById(visitor.getVisitorId());
        if (exist == null)
        {
            throw new ServiceException("用户不存在", 601);
        }
        visitorMapper.updateVisitor(visitor);
    }
}
