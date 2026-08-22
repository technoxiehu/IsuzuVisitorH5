package com.ruoyi.netease.service;

import com.ruoyi.netease.domain.ThirdSyncMapping;
import java.util.List;

/**
 * 第三方同步映射表 Service
 *
 * @author isuzu
 */
public interface IThirdSyncMappingService
{
    /**
     * 根据第三方ID查询若依表主键ID
     */
    Long getRuoyiId(String thirdType, String ruoyiTable, String thirdId);

    /**
     * 根据第三方数字ID（unitId）查询若依表主键ID
     */
    Long getRuoyiIdByAltId(String thirdType, String ruoyiTable, String thirdAltId);

    /**
     * 根据第三方ID查询映射记录
     */
    ThirdSyncMapping getMapping(String thirdType, String ruoyiTable, String thirdId);

    /**
     * 查询所有活跃映射记录
     */
    List<ThirdSyncMapping> getActiveMappings(String thirdType);

    /**
     * 查询指定表的所有映射记录
     */
    List<ThirdSyncMapping> getAllByTable(String thirdType, String ruoyiTable);

    /**
     * 插入或更新映射记录
     */
    void saveMapping(ThirdSyncMapping mapping);

    /**
     * 标记为已停用
     */
    void markDisabled(String thirdType, String ruoyiTable, String thirdId);

    /**
     * 标记为冲突待处理
     */
    void markConflict(String thirdType, String ruoyiTable, String thirdId);

    /**
     * 更新版本号
     */
    void updateVersion(String thirdType, String ruoyiTable, String thirdId, Long version);
}