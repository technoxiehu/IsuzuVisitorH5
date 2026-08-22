package com.ruoyi.netease.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.netease.domain.ThirdSyncMapping;
import com.ruoyi.netease.mapper.ThirdSyncMappingMapper;
import com.ruoyi.netease.service.IThirdSyncMappingService;

/**
 * 第三方同步映射表 Service 实现
 *
 * @author isuzu
 */
@Service
public class ThirdSyncMappingServiceImpl implements IThirdSyncMappingService
{
    private static final String THIRD_TYPE = "netease_mail";

    @Autowired
    private ThirdSyncMappingMapper mappingMapper;

    @Override
    public Long getRuoyiId(String thirdType, String ruoyiTable, String thirdId)
    {
        ThirdSyncMapping mapping = mappingMapper.selectByThirdId(thirdType, ruoyiTable, thirdId);
        return mapping != null ? mapping.getRuoyiId() : null;
    }

    @Override
    public Long getRuoyiIdByAltId(String thirdType, String ruoyiTable, String thirdAltId)
    {
        ThirdSyncMapping mapping = mappingMapper.selectByAltId(thirdType, ruoyiTable, thirdAltId);
        return mapping != null ? mapping.getRuoyiId() : null;
    }

    @Override
    public ThirdSyncMapping getMapping(String thirdType, String ruoyiTable, String thirdId)
    {
        return mappingMapper.selectByThirdId(thirdType, ruoyiTable, thirdId);
    }

    @Override
    public List<ThirdSyncMapping> getActiveMappings(String thirdType)
    {
        return mappingMapper.selectActiveMappings(thirdType);
    }

    @Override
    public List<ThirdSyncMapping> getAllByTable(String thirdType, String ruoyiTable)
    {
        return mappingMapper.selectAllByTable(thirdType, ruoyiTable);
    }

    @Override
    public void saveMapping(ThirdSyncMapping mapping)
    {
        ThirdSyncMapping existing = mappingMapper.selectByThirdId(
                mapping.getThirdType(), mapping.getRuoyiTable(), mapping.getThirdId());
        if (existing != null)
        {
            mapping.setUpdateBy("netease-sync");
            mappingMapper.updateMapping(mapping);
        }
        else
        {
            mapping.setCreateBy("netease-sync");
            mapping.setSyncTime(new Date());
            mappingMapper.insertMapping(mapping);
        }
    }

    @Override
    public void markDisabled(String thirdType, String ruoyiTable, String thirdId)
    {
        mappingMapper.updateSyncStatus(thirdType, ruoyiTable, thirdId, 3);
    }

    @Override
    public void markConflict(String thirdType, String ruoyiTable, String thirdId)
    {
        mappingMapper.updateSyncStatus(thirdType, ruoyiTable, thirdId, 4);
    }

    @Override
    public void updateVersion(String thirdType, String ruoyiTable, String thirdId, Long version)
    {
        mappingMapper.updateSyncVersion(thirdType, ruoyiTable, thirdId, version);
    }
}