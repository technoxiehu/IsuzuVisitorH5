package com.ruoyi.visitor.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.visitor.domain.ApproveDetailVo;
import com.ruoyi.visitor.domain.Visitor;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.domain.VisitorCompanion;
import com.ruoyi.visitor.domain.VisitorHost;
import com.ruoyi.visitor.mapper.VisitorApplicationMapper;
import com.ruoyi.visitor.mapper.VisitorCompanionMapper;
import com.ruoyi.visitor.mapper.VisitorMapper;
import com.ruoyi.visitor.service.ApproveTokenService;
import com.ruoyi.visitor.service.IVisitorApplicationService;
import com.ruoyi.visitor.service.VisitorMailService;
import com.ruoyi.visitor.utils.IdCardUtils;

/**
 * 访客申请单 业务层处理
 *
 * @author isuzu
 */
@Service
public class VisitorApplicationServiceImpl implements IVisitorApplicationService
{
    /** UUID 格式校验 */
    private static final Pattern UUID_PATTERN = Pattern
            .compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** 当日允许的最大拒绝次数 */
    private static final int MAX_DAILY_REJECT = 3;

    /** 随行人员最大数量 */
    private static final int MAX_COMPANIONS = 5;

    @Autowired
    private VisitorApplicationMapper visitorApplicationMapper;

    @Autowired
    private VisitorCompanionMapper visitorCompanionMapper;

    @Autowired
    private VisitorMapper visitorMapper;

    @Autowired
    private ApproveTokenService approveTokenService;

    @Autowired
    private VisitorMailService visitorMailService;

    /**
     * 按姓名关键字查询被访人
     */
    @Override
    public List<VisitorHost> selectHostList(String keyword)
    {
        return visitorApplicationMapper.selectHostList(keyword);
    }

    /**
     * 提交访问申请（申请单与随行人员同事务写入）
     */
    @Override
    @Transactional
    public String submitApplication(VisitorApplication application)
    {
        checkSubmit(application);

        // 当日拒绝数达到上限，禁止再提交（入口拦截的后端兜底；当日区间按应用服务器时间统计）
        Date todayStart = DateUtils.parseDate(DateUtils.getDate());
        Date nextDayStart = DateUtils.addDays(todayStart, 1);
        int todayReject = visitorApplicationMapper.countTodayReject(application.getVisitorId(), todayStart, nextDayStart);
        if (todayReject >= MAX_DAILY_REJECT)
        {
            throw new ServiceException("审批人拒绝近期访问，谢谢。", 601);
        }

        application.setApplicationId(IdUtils.fastUUID());
        application.setStatus("0");
        application.setCreateTime(new Date());
        visitorApplicationMapper.insertApplication(application);

        // 随行人员名单随申请单快照入库
        insertCompanions(application);

        // 发送审批邮件（发送失败不影响申请单提交成功，VisitorMailService 内部已兜底）
        Visitor visitor = visitorMapper.selectVisitorById(application.getVisitorId());
        VisitorHost host = visitorApplicationMapper.selectHostById(application.getHostId());
        String hostEmail = host != null ? host.getEmail() : null;
        String token = approveTokenService.createToken(application.getApplicationId());
        visitorMailService.sendApproveMail(application, visitor, hostEmail, token);

        return application.getApplicationId();
    }

    /**
     * 批量写入随行人员（sort_no 按提交顺序），撞唯一键 uk_application_id_card 时转参数错误
     */
    private void insertCompanions(VisitorApplication application)
    {
        List<VisitorCompanion> companions = application.getCompanions();
        if (companions == null || companions.isEmpty())
        {
            return;
        }
        Date now = new Date();
        for (int i = 0; i < companions.size(); i++)
        {
            VisitorCompanion c = companions.get(i);
            c.setCompanionId(IdUtils.fastUUID());
            c.setApplicationId(application.getApplicationId());
            c.setSortNo(i);
            c.setCreateTime(now);
        }
        try
        {
            visitorCompanionMapper.insertBatch(companions);
        }
        catch (DuplicateKeyException e)
        {
            // 应用层已去重，此处兜底并发提交同身份证的场景
            throw new ServiceException("随行人员身份证号不能重复", 400);
        }
    }

    /**
     * 申请单提交校验
     */
    private void checkSubmit(VisitorApplication application)
    {
        if (StringUtils.isEmpty(application.getVisitorId()) || !UUID_PATTERN.matcher(application.getVisitorId()).matches())
        {
            throw new ServiceException("访客ID格式不正确", 400);
        }
        Visitor visitor = visitorMapper.selectVisitorById(application.getVisitorId());
        if (visitor == null)
        {
            throw new ServiceException("访客不存在，请先注册", 400);
        }
        if (application.getHostId() == null)
        {
            throw new ServiceException("被访人不能为空", 400);
        }
        VisitorHost host = visitorApplicationMapper.selectHostById(application.getHostId());
        if (host == null)
        {
            throw new ServiceException("被访人不存在或已停用", 400);
        }
        application.setHostName(host.getName());
        if (application.getStartTime() == null)
        {
            throw new ServiceException("开始时间不能为空", 400);
        }
        if (application.getEndTime() == null)
        {
            throw new ServiceException("结束时间不能为空", 400);
        }
        // 开始时间不能早于当前日期（当天零点）
        Date todayStart = DateUtils.parseDate(DateUtils.getDate());
        if (application.getStartTime().before(todayStart))
        {
            throw new ServiceException("开始时间不能早于当前日期", 400);
        }
        // 结束时间必须晚于开始时间
        if (!application.getEndTime().after(application.getStartTime()))
        {
            throw new ServiceException("结束时间必须晚于开始时间", 400);
        }
        if (StringUtils.isEmpty(application.getReason()))
        {
            throw new ServiceException("访问事由不能为空", 400);
        }
        if (application.getReason().length() > 200)
        {
            throw new ServiceException("访问事由长度不能超过200个字符", 400);
        }
        // 随行人员（可选）：数量/姓名/身份证号校验
        checkCompanions(application.getCompanions());
    }

    /**
     * 随行人员校验（docs/03_接口契约.md §3.6）：
     * 可选，最多 5 人；姓名非空 ≤20；身份证 18 位（末位可 X）且校验位合法（GB 11643-1999）；申请单内不重复。
     */
    private void checkCompanions(List<VisitorCompanion> companions)
    {
        if (companions == null || companions.isEmpty())
        {
            return;
        }
        if (companions.size() > MAX_COMPANIONS)
        {
            throw new ServiceException("随行人员最多5人", 400);
        }
        Set<String> idCardSet = new HashSet<>();
        for (VisitorCompanion c : companions)
        {
            if (StringUtils.isEmpty(c.getName()))
            {
                throw new ServiceException("随行人员姓名不能为空", 400);
            }
            if (c.getName().length() > 20)
            {
                throw new ServiceException("随行人员姓名长度不能超过20个字符", 400);
            }
            if (StringUtils.isEmpty(c.getIdCard()))
            {
                throw new ServiceException("随行人员身份证号不能为空", 400);
            }
            String idCard = c.getIdCard().trim().toUpperCase();
            if (!IdCardUtils.isValid(idCard))
            {
                throw new ServiceException("随行人员身份证号格式不正确", 400);
            }
            if (!idCardSet.add(idCard))
            {
                throw new ServiceException("随行人员身份证号不能重复", 400);
            }
            c.setIdCard(idCard); // 归一化大写后入库
        }
    }

    /**
     * 查询访客的全部申请记录及当日拒绝数（PRD v1.5：列表页展示全部记录，
     * 每条按应用服务器时间计算 effective 窗口标记；记录带随行人员名单，身份证号脱敏）
     */
    @Override
    public Map<String, Object> selectValidList(String visitorId)
    {
        Map<String, Object> result = new HashMap<>();
        Date todayStart = DateUtils.parseDate(DateUtils.getDate());
        Date nextDayStart = DateUtils.addDays(todayStart, 1);
        result.put("todayRejectCount", visitorApplicationMapper.countTodayReject(visitorId, todayStart, nextDayStart));
        List<VisitorApplication> records = visitorApplicationMapper.selectValidList(visitorId);
        fillEffective(records);
        fillCompanions(records);
        result.put("records", records);
        return result;
    }

    /**
     * 计算每条记录的 effective（当前时刻是否落在开始~结束时间窗口内，应用服务器时间）
     */
    private void fillEffective(List<VisitorApplication> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        Date now = new Date();
        for (VisitorApplication record : records)
        {
            boolean effective = record.getStartTime() != null && record.getEndTime() != null
                    && !record.getStartTime().after(now) && !record.getEndTime().before(now);
            record.setEffective(effective);
        }
    }

    /**
     * 为记录批量填充随行人员名单（身份证号脱敏；无名单的记录补空数组）
     */
    private void fillCompanions(List<VisitorApplication> records)
    {
        if (records == null || records.isEmpty())
        {
            return;
        }
        List<String> applicationIds = new ArrayList<>();
        for (VisitorApplication record : records)
        {
            applicationIds.add(record.getApplicationId());
        }
        List<VisitorCompanion> companions = visitorCompanionMapper.selectListByApplicationIds(applicationIds);
        Map<String, List<VisitorCompanion>> group = new HashMap<>();
        for (VisitorCompanion c : companions)
        {
            group.computeIfAbsent(c.getApplicationId(), k -> new ArrayList<>()).add(c);
        }
        for (VisitorApplication record : records)
        {
            List<VisitorCompanion> list = group.getOrDefault(record.getApplicationId(), new ArrayList<>());
            maskCompanions(list);
            record.setCompanions(list);
        }
    }

    /**
     * 身份证号脱敏（查询结果仅用于返回，直接覆盖为脱敏值，不入库）
     */
    private void maskCompanions(List<VisitorCompanion> companions)
    {
        for (VisitorCompanion c : companions)
        {
            c.setIdCard(IdCardUtils.mask(c.getIdCard()));
        }
    }

    /**
     * 审批详情（token 鉴权）
     */
    @Override
    public ApproveDetailVo getApproveDetail(String token)
    {
        VisitorApplication application = getApplicationByToken(token);

        // 只能审批一次：已审批的申请单直接拦截
        if (!"0".equals(application.getStatus()))
        {
            throw new ServiceException("该申请单已完成审批", 601);
        }

        ApproveDetailVo detail = new ApproveDetailVo();
        detail.setApplicationId(application.getApplicationId());
        detail.setHostName(application.getHostName());
        detail.setStartTime(application.getStartTime());
        detail.setEndTime(application.getEndTime());
        detail.setReason(application.getReason());
        detail.setStatus(application.getStatus());
        detail.setCreateTime(application.getCreateTime());

        Visitor visitor = visitorMapper.selectVisitorById(application.getVisitorId());
        if (visitor != null)
        {
            detail.setVisitorName(visitor.getName());
            detail.setVisitorCompany(visitor.getCompany());
            detail.setVisitorAvatar(visitor.getAvatar());
        }

        // 随行人员名单（脱敏后返回）
        List<VisitorCompanion> companions = visitorCompanionMapper
                .selectListByApplicationIds(Collections.singletonList(application.getApplicationId()));
        maskCompanions(companions);
        detail.setCompanions(companions);
        return detail;
    }

    /**
     * 审批结果回写
     */
    @Override
    public void approve(String token, String action)
    {
        VisitorApplication application = getApplicationByToken(token);

        // 只能审批一次：条件更新兜底，已审批的申请单影响行数为 0
        String status;
        if ("approve".equals(action))
        {
            status = "1";
        }
        else if ("reject".equals(action))
        {
            status = "2";
        }
        else
        {
            throw new ServiceException("审批动作不合法", 400);
        }
        Date now = new Date();
        int rows = visitorApplicationMapper.updateApproveStatus(application.getApplicationId(), status, now, now);
        if (rows == 0)
        {
            throw new ServiceException("该申请单已完成审批", 601);
        }
    }

    /**
     * 解析 token 并查询申请单
     */
    private VisitorApplication getApplicationByToken(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            throw new ServiceException("链接无效或已过期", 401);
        }
        String applicationId = approveTokenService.parseApplicationId(token);
        if (applicationId == null)
        {
            throw new ServiceException("链接无效或已过期", 401);
        }
        VisitorApplication application = visitorApplicationMapper.selectApplicationById(applicationId);
        if (application == null)
        {
            throw new ServiceException("链接无效或已过期", 401);
        }
        return application;
    }
}
