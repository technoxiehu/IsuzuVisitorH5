package com.ruoyi.visitor.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.visitor.domain.Visitor;
import com.ruoyi.visitor.mapper.VisitorMapper;
import com.ruoyi.visitor.service.IVisitorService;
import com.ruoyi.visitor.utils.IdCardUtils;
import java.util.regex.Pattern;

/**
 * 访客信息 业务层处理
 *
 * @author isuzu
 */
@Service
public class VisitorServiceImpl implements IVisitorService
{
    /** 车牌号宽松校验：非必填，填写仅限汉字/字母/数字且 ≤10 位（兼容新能源等特殊车牌） */
    private static final Pattern PLATE_NO_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9]{0,10}$");

    @Autowired
    private VisitorMapper visitorMapper;

    /** 车牌号校验（非必填）：空/空白/合法值通过，非法字符抛 400 */
    private void validatePlateNo(String plateNo)
    {
        if (plateNo != null && !plateNo.isBlank() && !PLATE_NO_PATTERN.matcher(plateNo).matches())
        {
            throw new ServiceException("车牌号仅支持汉字、字母、数字且不超过10个字符", 400);
        }
    }

    /**
     * 根据访客ID查询访客信息
     *
     * @param visitorId 访客ID
     * @return 访客信息
     */
    @Override
    public Visitor selectVisitor(String visitorId)
    {
        // 返回全量身份证号：我的信息页本人回显用；门卫列表页由前端 maskIdCard 脱敏展示
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
        // 身份证号必填：格式 + 校验位（GB 11643-1999），末位小写 x 统一转大写
        String idCard = visitor.getIdCard();
        if (StringUtils.isEmpty(idCard) || !IdCardUtils.isValid(idCard.toUpperCase()))
        {
            throw new ServiceException("身份证号不能为空或格式不正确", 400);
        }
        visitor.setIdCard(idCard.toUpperCase());
        validatePlateNo(visitor.getPlateNo());
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
        // 脱敏值防覆盖守卫：空/脱敏值(含*)置 null → mapper 跳过该列保留原值；全量合法值才更新
        String idCard = visitor.getIdCard();
        if (idCard == null || !IdCardUtils.isValid(idCard.toUpperCase()))
        {
            visitor.setIdCard(null);
        }
        else
        {
            visitor.setIdCard(idCard.toUpperCase());
        }
        validatePlateNo(visitor.getPlateNo());
        visitorMapper.updateVisitor(visitor);
    }
}
