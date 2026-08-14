package com.ruoyi.visitor.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.visitor.domain.ApproveDetailVo;
import com.ruoyi.visitor.domain.Visitor;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.domain.VisitorHost;
import com.ruoyi.visitor.mapper.VisitorApplicationMapper;
import com.ruoyi.visitor.mapper.VisitorMapper;
import com.ruoyi.visitor.service.ApproveTokenService;
import com.ruoyi.visitor.service.IVisitorApplicationService;
import com.ruoyi.visitor.service.VisitorMailService;

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

    @Autowired
    private VisitorApplicationMapper visitorApplicationMapper;

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
     * 提交访问申请
     */
    @Override
    public String submitApplication(VisitorApplication application)
    {
        checkSubmit(application);

        // 当日拒绝数达到上限，禁止再提交（入口拦截的后端兜底）
        int todayReject = visitorApplicationMapper.countTodayReject(application.getVisitorId());
        if (todayReject >= MAX_DAILY_REJECT)
        {
            throw new ServiceException("审批人拒绝近期访问，谢谢。", 601);
        }

        application.setApplicationId(IdUtils.fastUUID());
        application.setStatus("0");
        visitorApplicationMapper.insertApplication(application);

        // 发送审批邮件（发送失败不影响申请单提交成功）
        Visitor visitor = visitorMapper.selectVisitorById(application.getVisitorId());
        VisitorHost host = visitorApplicationMapper.selectHostById(application.getHostId());
        String hostEmail = host != null ? host.getEmail() : null;
        String token = approveTokenService.createToken(application.getApplicationId());
        visitorMailService.sendApproveMail(application, visitor, hostEmail, token);

        return application.getApplicationId();
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
    }

    /**
     * 查询访客的有效审批记录及当日拒绝数
     */
    @Override
    public Map<String, Object> selectValidList(String visitorId)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("todayRejectCount", visitorApplicationMapper.countTodayReject(visitorId));
        result.put("records", visitorApplicationMapper.selectValidList(visitorId));
        return result;
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
        int rows = visitorApplicationMapper.updateApproveStatus(application.getApplicationId(), status, new Date());
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
