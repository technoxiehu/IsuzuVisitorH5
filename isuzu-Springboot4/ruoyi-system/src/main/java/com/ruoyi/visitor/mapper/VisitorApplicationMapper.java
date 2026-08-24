package com.ruoyi.visitor.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.visitor.domain.VisitorApplication;
import com.ruoyi.visitor.domain.VisitorHost;

/**
 * 访客申请单 数据层
 *
 * @author isuzu
 */
public interface VisitorApplicationMapper
{
    /**
     * 按姓名关键字查询被访人（仅状态正常、未删除的系统用户）
     *
     * @param keyword 姓名关键字
     * @return 被访人集合
     */
    public List<VisitorHost> selectHostList(String keyword);

    /**
     * 根据用户ID查询被访人（提交申请时校验被访人有效性）
     *
     * @param userId 系统用户ID
     * @return 被访人信息
     */
    public VisitorHost selectHostById(Long userId);

    /**
     * 新增申请单
     *
     * @param application 申请单
     * @return 结果
     */
    public int insertApplication(VisitorApplication application);

    /**
     * 根据申请单号查询申请单
     *
     * @param applicationId 申请单号
     * @return 申请单
     */
    public VisitorApplication selectApplicationById(String applicationId);

    /**
     * 查询访客的全部申请记录（列表页/入口分流，PRD v1.5），按提交时间倒序
     * 是否在访问时间窗口内由应用层按 effective 计算（不依赖数据库时钟，见 docs/03_接口契约.md §3.7）
     *
     * @param visitorId 访客ID
     * @return 申请单集合
     */
    public List<VisitorApplication> selectValidList(String visitorId);

    /**
     * 统计访客当日（自然日，GMT+8）被拒绝的审批记录数
     * 当日区间由应用层计算传入（不依赖数据库 curdate()，见 docs/03_接口契约.md §3.7）
     *
     * @param visitorId 访客ID
     * @param todayStart 当日零点
     * @param nextDayStart 次日零点
     * @return 当日拒绝数
     */
    public int countTodayReject(@Param("visitorId") String visitorId, @Param("todayStart") Date todayStart,
            @Param("nextDayStart") Date nextDayStart);

    /**
     * 统计访客待审批（status='0'）申请单数（防重复提交兜底，docs/03_接口契约.md §3.6）
     *
     * @param visitorId 访客ID
     * @return 待审批申请单数
     */
    public int countPendingByVisitor(String visitorId);

    /**
     * 审批回写（条件更新：仅状态为未审批时可更新，防重复审批）
     *
     * @param applicationId 申请单号
     * @param status 审批后状态(1通过 2拒绝)
     * @param approveTime 审批时间
     * @return 影响行数
     */
    public int updateApproveStatus(@Param("applicationId") String applicationId, @Param("status") String status,
            @Param("approveTime") Date approveTime, @Param("updateTime") Date updateTime);
}
