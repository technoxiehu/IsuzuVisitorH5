package com.ruoyi.visitor.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.visitor.domain.ApproveDetailVo;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.domain.VisitorHost;

/**
 * 访客申请单 业务层
 *
 * @author isuzu
 */
public interface IVisitorApplicationService
{
    /**
     * 按姓名关键字查询被访人
     *
     * @param keyword 姓名关键字
     * @return 被访人集合
     */
    public List<VisitorHost> selectHostList(String keyword);

    /**
     * 生成一次性提交令牌（防重复提交，Redis 存储，2 小时有效）
     *
     * @return 提交令牌
     */
    public String createSubmitToken();

    /**
     * 提交访问申请（校验通过后落库并发送审批邮件）
     * 防重复提交：待审批兜底查重 + 一次性令牌原子消费，见 docs/03_接口契约.md §3.6
     *
     * @param application 申请单（携带 submitToken）
     * @return 申请单号
     */
    public String submitApplication(VisitorApplication application);

    /**
     * 查询访客的有效审批记录及当日拒绝数
     *
     * @param visitorId 访客ID
     * @return { todayRejectCount, records }
     */
    public Map<String, Object> selectValidList(String visitorId);

    /**
     * 审批详情（token 鉴权；已审批时抛出业务异常 code=601，token 无效时抛出 code=401）
     *
     * @param token 审批 token
     * @return 审批详情
     */
    public ApproveDetailVo getApproveDetail(String token);

    /**
     * 审批结果回写（仅未审批的申请单可审批一次，条件更新兜底）
     *
     * @param token 审批 token
     * @param action 审批动作（approve 批准 / reject 拒绝）
     */
    public void approve(String token, String action);

    /**
     * 删除待审批申请单（逻辑删除 del_flag='1'，仅本人可删；已审批/已撤销/不存在时报错）
     *
     * @param visitorId 访客ID（归属校验）
     * @param applicationId 申请单号
     */
    public void deleteApplication(String visitorId, String applicationId);
}
