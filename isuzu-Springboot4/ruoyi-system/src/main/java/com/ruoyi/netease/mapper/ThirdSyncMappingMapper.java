package com.ruoyi.netease.mapper;

import java.util.List;
import com.ruoyi.netease.domain.ThirdSyncMapping;

/**
 * 第三方同步映射表 Mapper
 *
 * @author isuzu
 */
public interface ThirdSyncMappingMapper
{
    ThirdSyncMapping selectByThirdId(String thirdType, String ruoyiTable, String thirdId);

    ThirdSyncMapping selectByAltId(String thirdType, String ruoyiTable, String thirdAltId);

    ThirdSyncMapping selectByRuoyiId(String thirdType, String ruoyiTable, Long ruoyiId);

    List<ThirdSyncMapping> selectActiveMappings(String thirdType);

    List<ThirdSyncMapping> selectAllByTable(String thirdType, String ruoyiTable);

    int insertMapping(ThirdSyncMapping mapping);

    int updateMapping(ThirdSyncMapping mapping);

    int updateSyncStatus(String thirdType, String ruoyiTable, String thirdId, Integer syncStatus);

    int updateSyncVersion(String thirdType, String ruoyiTable, String thirdId, Long syncVersion);
}