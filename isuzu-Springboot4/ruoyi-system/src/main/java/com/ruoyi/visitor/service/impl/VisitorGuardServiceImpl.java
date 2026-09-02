package com.ruoyi.visitor.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.visitor.domain.GuardCardVo;
import com.ruoyi.visitor.domain.GuardEntryVo;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.domain.VisitorCompanion;
import com.ruoyi.visitor.domain.VisitorEntry;
import com.ruoyi.visitor.mapper.VisitorApplicationMapper;
import com.ruoyi.visitor.mapper.VisitorCompanionMapper;
import com.ruoyi.visitor.mapper.VisitorEntryMapper;
import com.ruoyi.visitor.service.IVisitorGuardService;
import com.ruoyi.visitor.utils.IdCardUtils;

/**
 * 门卫电脑端核验 业务层处理
 *
 * @author isuzu
 */
@Service
public class VisitorGuardServiceImpl implements IVisitorGuardService
{
    @Autowired
    private VisitorEntryMapper visitorEntryMapper;

    @Autowired
    private VisitorApplicationMapper visitorApplicationMapper;

    @Autowired
    private VisitorCompanionMapper visitorCompanionMapper;

    /**
     * 查询门卫有效单据卡片（§3.13）：
     * 审批通过(status='1')且未撤销(del_flag='0')且访问窗口覆盖指定日期（日期粒度，当天缺省）；
     * 手机号/身份证号脱敏；随行人员名单批量带出（身份证脱敏）；entryCount/lastEntryTime 为当日放行统计
     */
    @Override
    public List<GuardCardVo> selectGuardCardList(String keyword, String date)
    {
        // 日期粒度窗口：date 缺省取应用服务器当天；区间 [date 00:00:00, 次日 00:00:00)
        String day = StringUtils.isEmpty(date) ? DateUtils.getDate() : date;
        Date dateStart = DateUtils.parseDate(day);
        Date nextDayStart = DateUtils.addDays(dateStart, 1);

        List<GuardCardVo> cards = visitorEntryMapper.selectGuardCardList(keyword, dateStart, nextDayStart);
        if (cards == null || cards.isEmpty())
        {
            return cards;
        }
        // 主访客手机号/身份证号脱敏（接口返回口径，见 docs/03_接口契约.md §3.13）
        for (GuardCardVo c : cards)
        {
            c.setVisitorPhone(maskPhone(c.getVisitorPhone()));
            c.setVisitorIdCard(IdCardUtils.mask(c.getVisitorIdCard()));
        }
        // 随行人员名单批量带出并按申请单分组（身份证脱敏；无名单置空数组，字段恒存在）
        List<String> applicationIds = cards.stream().map(GuardCardVo::getApplicationId).collect(Collectors.toList());
        Map<String, List<VisitorCompanion>> companionMap = new HashMap<>();
        List<VisitorCompanion> companions = visitorCompanionMapper.selectListByApplicationIds(applicationIds);
        if (companions != null)
        {
            for (VisitorCompanion c : companions)
            {
                c.setIdCard(IdCardUtils.mask(c.getIdCard()));
                companionMap.computeIfAbsent(c.getApplicationId(), k -> new ArrayList<>()).add(c);
            }
        }
        for (GuardCardVo c : cards)
        {
            c.setCompanions(companionMap.getOrDefault(c.getApplicationId(), new ArrayList<>()));
        }
        return cards;
    }

    /**
     * 门卫进出登记（§3.14）：校验申请单存在/未撤销/审批通过/当前时刻在访问窗口内，
     * 再按全历史最新事件做严格交替校验（在厂内禁再进场、厂外禁离厂），新增事件记录；
     * 无二次确认、不限次数、不改申请单状态；操作人（门卫）由 controller 从当前登录态传入
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createEntry(String applicationId, String entryType, Long operatorId, String operatorName)
    {
        if (StringUtils.isEmpty(applicationId))
        {
            throw new ServiceException("申请单ID不能为空", 400);
        }
        // 事件类型：缺省按进场处理（兼容旧前端不传 entryType 的调用），仅接受 '0'/'1'
        String type = StringUtils.isEmpty(entryType) ? "0" : entryType;
        if (!"0".equals(type) && !"1".equals(type))
        {
            throw new ServiceException("事件类型非法", 400);
        }
        VisitorApplication application = visitorApplicationMapper.selectApplicationById(applicationId);
        if (application == null)
        {
            throw new ServiceException("申请单不存在", 400);
        }
        if ("1".equals(application.getDelFlag()))
        {
            throw new ServiceException("该单据已撤销", 601);
        }
        if (!"1".equals(application.getStatus()))
        {
            throw new ServiceException("该单据未审批通过", 601);
        }
        Date now = new Date();
        if (now.before(application.getStartTime()) || now.after(application.getEndTime()))
        {
            throw new ServiceException("该单据不在有效期内", 601);
        }
        // 严格交替校验（v1.12）：在厂状态 = 全历史最新事件（不限当日，昨天进场未离厂今天仍在厂内）
        VisitorEntry latest = visitorEntryMapper.selectLatestEvent(applicationId);
        boolean onSite = latest != null && "0".equals(latest.getEntryType());
        if ("0".equals(type) && onSite)
        {
            throw new ServiceException("访客已在厂内，请先离厂", 601);
        }
        if ("1".equals(type) && !onSite)
        {
            throw new ServiceException("访客不在厂内，无法离厂", 601);
        }
        VisitorEntry entry = new VisitorEntry();
        entry.setEntryId(IdUtils.fastUUID());
        entry.setApplicationId(applicationId);
        entry.setEntryType(type);
        entry.setOperatorId(operatorId);
        entry.setOperatorName(operatorName);
        entry.setCreateTime(now);
        visitorEntryMapper.insertEntry(entry);
        return entry.getEntryId();
    }

    /**
     * 入场记录查询（§3.15）：门卫可见全部，按放行时间倒序；展示字段脱敏
     */
    @Override
    public List<GuardEntryVo> selectEntryList(String keyword, Date beginTime, Date endTime)
    {
        List<GuardEntryVo> list = visitorEntryMapper.selectEntryList(keyword, beginTime, endTime);
        if (list != null)
        {
            for (GuardEntryVo e : list)
            {
                e.setVisitorPhone(maskPhone(e.getVisitorPhone()));
                e.setVisitorIdCard(IdCardUtils.mask(e.getVisitorIdCard()));
            }
        }
        return list;
    }

    /**
     * 入场记录导出（§3.16）：与列表查询同条件、同排序，手机号/身份证不脱敏（全量明文，审计口径）
     */
    @Override
    public List<GuardEntryVo> selectEntryExportList(String keyword, Date beginTime, Date endTime)
    {
        return visitorEntryMapper.selectEntryList(keyword, beginTime, endTime);
    }

    /** 手机号脱敏：前 3 + **** + 后 4（幂等，含 * 或非 11 位原样返回） */
    private String maskPhone(String phone)
    {
        if (phone == null || phone.length() != 11 || phone.contains("*"))
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
