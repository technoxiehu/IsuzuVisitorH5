package com.ruoyi.visitor.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.visitor.domain.VisitorCompanion;

/**
 * 随行人员 数据层
 *
 * @author isuzu
 */
public interface VisitorCompanionMapper
{
    /**
     * 批量新增随行人员（申请单提交时，与申请单同事务）
     *
     * @param companions 随行人员集合
     * @return 影响行数
     */
    public int insertBatch(@Param("list") List<VisitorCompanion> companions);

    /**
     * 按申请单号集合批量查询（列表页一次带出多单名单），按 application_id, sort_no 排序
     *
     * @param applicationIds 申请单号集合
     * @return 随行人员集合
     */
    public List<VisitorCompanion> selectListByApplicationIds(@Param("applicationIds") List<String> applicationIds);
}
