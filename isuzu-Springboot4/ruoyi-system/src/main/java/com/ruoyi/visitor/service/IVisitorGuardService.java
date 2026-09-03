package com.ruoyi.visitor.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.visitor.domain.GuardCardVo;
import com.ruoyi.visitor.domain.GuardEntryVo;

/**
 * 门卫电脑端核验 业务层（v1.10，需若依登录态，见 docs/03_接口契约.md §3.13~§3.16）
 *
 * @author isuzu
 */
public interface IVisitorGuardService
{
    /**
     * 查询门卫有效单据卡片（审批通过、未撤销且访问窗口覆盖指定日期，日期缺省取应用服务器当天）
     *
     * v1.12.3 起：手机号/身份证号（含随行人员）**明文返回**（门卫对照实体证件口径）；
     * 新增在厂状态筛选 onSiteStatus（'0' 在厂内 / '1' 已离厂 / 'none' 待进场，缺省全部）
     *
     * @param keyword 姓名/手机号/身份证号/车牌号模糊（可空）
     * @param date 日期 yyyy-MM-dd（可空）
     * @param onSiteStatus 在厂状态筛选（可空）
     * @return 卡片集合（带随行人员名单与当日进场次数）
     */
    public List<GuardCardVo> selectGuardCardList(String keyword, String date, String onSiteStatus);

    /**
     * 门卫进出登记：新增进场/离厂事件记录（无二次确认、不限次数、不改申请单状态；校验单据有效且当前时刻在访问窗口内）
     *
     * v1.12 起严格交替校验：在厂内（全历史最新事件为进场）不允许再进场，厂外（无事件或最新为离厂）不允许离厂
     *
     * @param applicationId 申请单号
     * @param entryType 事件类型（'0' 进场 / '1' 离厂）
     * @param operatorId 操作门卫(sys_user.user_id)
     * @param operatorName 操作门卫姓名(冗余)
     * @return 入场记录ID
     */
    public String createEntry(String applicationId, String entryType, Long operatorId, String operatorName);

    /**
     * 查询入场记录（门卫可见全部，按事件时间倒序；展示字段脱敏）
     *
     * @param visitorName 访客姓名模糊（可空）
     * @param hostName 被访人姓名模糊（可空）
     * @param plateNo 车牌号模糊（可空）
     * @param entryType 事件类型：'0' 进场 / '1' 离厂（可空）
     * @param beginTime 事件时间起（可空）
     * @param endTime 事件时间止（可空）
     * @return 入场记录集合
     */
    public List<GuardEntryVo> selectEntryList(String visitorName, String hostName, String plateNo, String entryType,
            Date beginTime, Date endTime);

    /**
     * 入场记录导出（§3.16）：与列表查询同条件、同排序，但手机号/身份证不脱敏（全量明文，审计口径）
     *
     * @param visitorName 访客姓名模糊（可空）
     * @param hostName 被访人姓名模糊（可空）
     * @param plateNo 车牌号模糊（可空）
     * @param entryType 事件类型：'0' 进场 / '1' 离厂（可空）
     * @param beginTime 事件时间起（可空）
     * @param endTime 事件时间止（可空）
     * @return 入场记录集合（明文）
     */
    public List<GuardEntryVo> selectEntryExportList(String visitorName, String hostName, String plateNo, String entryType,
            Date beginTime, Date endTime);
}
