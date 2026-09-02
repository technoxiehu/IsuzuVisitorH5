package com.ruoyi.visitor.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.visitor.domain.GuardCardVo;
import com.ruoyi.visitor.domain.GuardEntryVo;
import com.ruoyi.visitor.domain.VisitorEntry;

/**
 * 入场/放行记录 数据层（门卫电脑端 v1.10，见 docs/03_接口契约.md §3.13~§3.15）
 *
 * @author isuzu
 */
public interface VisitorEntryMapper
{
    /**
     * 分页查询门卫有效单据卡片：审批通过、未撤销且访问窗口覆盖指定日期（当天缺省）
     *
     * @param keyword 姓名/手机号/身份证号/车牌号模糊（可空）
     * @param dateStart 当日零点（日期粒度窗口起点）
     * @param nextDayStart 次日零点（日期粒度窗口终点，开区间）
     * @return 卡片集合（按申请单提交时间倒序）
     */
    public List<GuardCardVo> selectGuardCardList(@Param("keyword") String keyword, @Param("dateStart") Date dateStart,
            @Param("nextDayStart") Date nextDayStart);

    /**
     * 新增入场记录（门卫进场/离厂事件，无二次确认、不限次数、不改申请单状态）
     *
     * @param entry 入场记录（含 entryType：0 进场 1 离厂）
     * @return 影响行数
     */
    public int insertEntry(VisitorEntry entry);

    /**
     * 查询申请单全历史最新一条进出事件（v1.12：推导访客在厂状态，不限当日，跨天未闭合仍可见）
     *
     * @param applicationId 申请单号
     * @return 最新事件（无任何事件时 null）
     */
    public VisitorEntry selectLatestEvent(@Param("applicationId") String applicationId);

    /**
     * 分页查询入场记录（JOIN 申请单/访客表；申请单已撤销的记录仍展示，历史事实审计留存）
     *
     * @param keyword 访客姓名/手机号/门卫姓名模糊（可空）
     * @param beginTime 放行时间起（可空）
     * @param endTime 放行时间止（可空）
     * @return 入场记录集合（按放行时间倒序）
     */
    public List<GuardEntryVo> selectEntryList(@Param("keyword") String keyword, @Param("beginTime") Date beginTime,
            @Param("endTime") Date endTime);
}
