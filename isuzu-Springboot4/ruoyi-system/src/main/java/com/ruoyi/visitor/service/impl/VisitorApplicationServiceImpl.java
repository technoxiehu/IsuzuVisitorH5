package com.ruoyi.visitor.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.core.redis.RedisCache;
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

    /** 提交令牌 redis 主键前缀（存在 = 可用） */
    private static final String SUBMIT_TOKEN_KEY = "visitor:submit_token:";

    /** 已消费提交令牌墓碑前缀（存在 = 已使用过，用于区分「重复提交」与「令牌过期」） */
    private static final String SUBMIT_TOKEN_USED_KEY = "visitor:submit_token_used:";

    /** 提交令牌有效期（小时），覆盖填表时长 */
    private static final int SUBMIT_TOKEN_TTL_HOURS = 2;

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

    @Autowired
    private RedisCache redisCache;

    /** 审批邮件异步发送线程池（Spring Boot 自动配置的 applicationTaskExecutor） */
    @Autowired
    private TaskExecutor taskExecutor;

    /**
     * 按姓名关键字查询被访人
     */
    @Override
    public List<VisitorHost> selectHostList(String keyword)
    {
        return visitorApplicationMapper.selectHostList(keyword);
    }

    /**
     * 生成一次性提交令牌（防重复提交）
     */
    @Override
    public String createSubmitToken()
    {
        String token = IdUtils.fastUUID();
        redisCache.setCacheObject(SUBMIT_TOKEN_KEY + token, "1", SUBMIT_TOKEN_TTL_HOURS, TimeUnit.HOURS);
        return token;
    }

    /**
     * 提交访问申请（申请单与随行人员同事务写入）
     * 防重复提交链路：参数校验 → 待审批兜底查重 → 原子消费令牌 → 落库 → 邮件
     */
    @Override
    @Transactional
    public String submitApplication(VisitorApplication application)
    {
        checkSubmit(application);

        // 修改链路（v1.9）：携带 replaceApplicationId 时先撤销原待审批单（逻辑删除），再走新建流程；
        // 须先于待审批兜底查重执行，否则原单仍落在访问窗口内会误拦截；与审批并发时由 status='0' 条件更新兜底
        if (StringUtils.isNotEmpty(application.getReplaceApplicationId()))
        {
            replacePending(application);
        }

        // 当日拒绝数达到上限，禁止再提交（入口拦截的后端兜底；当日区间按应用服务器时间统计）
        Date todayStart = DateUtils.parseDate(DateUtils.getDate());
        Date nextDayStart = DateUtils.addDays(todayStart, 1);
        int todayReject = visitorApplicationMapper.countTodayReject(application.getVisitorId(), todayStart, nextDayStart);
        if (todayReject >= MAX_DAILY_REJECT)
        {
            throw new ServiceException("审批人拒绝近期访问，谢谢。", 601);
        }

        // 待审批兜底查重：当前时刻落在访问窗口内的待审批单（与 PRD §5.1.5 分流规则同构；拦截刷新/多标签页重进场景）
        if (visitorApplicationMapper.countPendingByVisitor(application.getVisitorId(), new Date()) > 0)
        {
            throw new ServiceException("您已有一份待审批的申请，请勿重复提交", 601);
        }

        // 一次性令牌原子消费（先于落库，同 token 重放无论何时到达均被拒）
        consumeSubmitToken(application.getSubmitToken());

        application.setApplicationId(IdUtils.fastUUID());
        application.setStatus("0");
        application.setCreateTime(new Date());
        visitorApplicationMapper.insertApplication(application);

        // 随行人员名单随申请单快照入库
        insertCompanions(application);

        // 发送审批邮件：挪至事务提交后执行，SMTP 耗时不再拉长事务持锁窗口（发送失败不影响申请单提交成功，VisitorMailService 内部已兜底）
        sendMailAfterCommit(application);

        return application.getApplicationId();
    }

    /**
     * 撤销原待审批申请单（修改链路的替换前置：逻辑删除，仅本人且待审批且未删除可删；
     * 影响行数为 0 时区分「已审批」（601）与「已撤销/不存在」（400），给出明确反馈）
     */
    private void replacePending(VisitorApplication application)
    {
        String oldId = application.getReplaceApplicationId();
        if (!UUID_PATTERN.matcher(oldId).matches())
        {
            throw new ServiceException("申请单ID格式不正确", 400);
        }
        int rows = visitorApplicationMapper.deleteApplicationById(oldId, application.getVisitorId(), new Date());
        if (rows > 0)
        {
            return;
        }
        VisitorApplication exist = visitorApplicationMapper.selectApplicationById(oldId);
        if (exist == null || "1".equals(exist.getDelFlag()))
        {
            throw new ServiceException("申请单不存在或已撤销", 400);
        }
        throw new ServiceException("该申请单已审批，无法修改", 601);
    }

    /**
     * 原子消费一次性提交令牌：DEL 返回 true 即抢占成功；
     * 未抢到时查墓碑区分「已使用」（601 重复提交）与「过期/非法」（400 请重新获取）
     */
    private void consumeSubmitToken(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            throw new ServiceException("缺少提交令牌，请刷新页面后重试", 400);
        }
        Boolean consumed = redisCache.deleteObject(SUBMIT_TOKEN_KEY + token);
        if (Boolean.TRUE.equals(consumed))
        {
            // 写墓碑供并发重放请求识别「已使用过」
            redisCache.setCacheObject(SUBMIT_TOKEN_USED_KEY + token, "1", SUBMIT_TOKEN_TTL_HOURS, TimeUnit.HOURS);
            return;
        }
        Object tombstone = redisCache.getCacheObject(SUBMIT_TOKEN_USED_KEY + token);
        if (tombstone != null)
        {
            throw new ServiceException("请勿重复提交", 601);
        }
        throw new ServiceException("页面已过期，请刷新页面后重试", 400);
    }

    /**
     * 事务提交后异步发送审批邮件（提交线程不再等待 SMTP 往返，接口即时返回；
     * 发送失败不影响申请单提交，VisitorMailService 内部已兜底；无事务同步时降级为直接异步提交）
     */
    private void sendMailAfterCommit(VisitorApplication application)
    {
        Runnable mailTask = () -> doSendApproveMail(application);
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    taskExecutor.execute(mailTask);
                }
            });
        }
        else
        {
            taskExecutor.execute(mailTask);
        }
    }

    /**
     * 查询申请单与被访人并实际发送审批邮件
     */
    private void doSendApproveMail(VisitorApplication application)
    {
        Visitor visitor = visitorMapper.selectVisitorById(application.getVisitorId());
        VisitorHost host = visitorApplicationMapper.selectHostById(application.getHostId());
        String hostEmail = host != null ? host.getEmail() : null;
        String token = approveTokenService.createToken(application.getApplicationId());
        visitorMailService.sendApproveMail(application, visitor, hostEmail, token);
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
        // 访问跨度不超过 7 天（含首尾，日期粒度；结束日期最晚 = 开始日期 + 6 天，v1.10）
        if (DateUtils.differentDaysByMillisecond(application.getStartTime(), application.getEndTime()) > 6)
        {
            throw new ServiceException("访问时间不能超过7天", 400);
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
     * 申请单详情（本人回显修改用；仅未审批且未撤销的申请单可查，
     * 随行人员身份证号完整返回——本人设备回显，与列表/审批页的脱敏口径区分）
     */
    @Override
    public VisitorApplication getApplicationDetail(String visitorId, String applicationId)
    {
        if (StringUtils.isEmpty(visitorId) || !UUID_PATTERN.matcher(visitorId).matches())
        {
            throw new ServiceException("访客ID格式不正确", 400);
        }
        if (StringUtils.isEmpty(applicationId) || !UUID_PATTERN.matcher(applicationId).matches())
        {
            throw new ServiceException("申请单ID格式不正确", 400);
        }
        VisitorApplication application = visitorApplicationMapper.selectApplicationById(applicationId);
        if (application == null || "1".equals(application.getDelFlag()))
        {
            throw new ServiceException("申请单不存在或已撤销", 400);
        }
        if (!visitorId.equals(application.getVisitorId()))
        {
            throw new ServiceException("无权查看该申请单", 400);
        }
        // 仅待审批可进入修改回显；已审批时拦截（前端列表可能未刷新，点到已审批单需兜底）
        if (!"0".equals(application.getStatus()))
        {
            throw new ServiceException("该申请单已完成审批", 601);
        }
        List<VisitorCompanion> companions = visitorCompanionMapper
                .selectListByApplicationIds(Collections.singletonList(applicationId));
        application.setCompanions(companions);
        return application;
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
     * 删除待审批申请单（逻辑删除 del_flag='1'；仅本人可删，与审批并发时由 status='0' 条件更新兜底）
     */
    @Override
    public void deleteApplication(String visitorId, String applicationId)
    {
        if (StringUtils.isEmpty(visitorId) || !UUID_PATTERN.matcher(visitorId).matches())
        {
            throw new ServiceException("访客ID格式不正确", 400);
        }
        if (StringUtils.isEmpty(applicationId) || !UUID_PATTERN.matcher(applicationId).matches())
        {
            throw new ServiceException("申请单ID格式不正确", 400);
        }
        int rows = visitorApplicationMapper.deleteApplicationById(applicationId, visitorId, new Date());
        if (rows > 0)
        {
            return;
        }
        // 影响行数为 0：区分「已审批」与「已撤销/不存在」，给访客明确反馈
        VisitorApplication exist = visitorApplicationMapper.selectApplicationById(applicationId);
        if (exist == null || "1".equals(exist.getDelFlag()))
        {
            throw new ServiceException("申请单不存在或已撤销", 400);
        }
        throw new ServiceException("该申请单已审批，无法删除", 400);
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
        // 访客已撤销该申请单（逻辑删除）：区分提示，避免与「链接无效」混淆
        if ("1".equals(application.getDelFlag()))
        {
            throw new ServiceException("该申请单已撤销", 602);
        }
        return application;
    }
}
