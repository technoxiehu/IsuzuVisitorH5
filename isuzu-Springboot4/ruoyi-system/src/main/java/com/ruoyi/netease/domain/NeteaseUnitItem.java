package com.ruoyi.netease.domain;

import com.alibaba.fastjson2.annotation.JSONField;

/**
 * 网易企业邮箱部门信息
 *
 * @author isuzu
 */
public class NeteaseUnitItem
{
    @JSONField(name = "unitOpenId")
    private String unitOpenId;

    @JSONField(name = "unitId")
    private String unitId;

    @JSONField(name = "unitParentId")
    private String unitParentId;

    @JSONField(name = "unitName")
    private String unitName;

    @JSONField(name = "rank")
    private Integer rank;

    @JSONField(name = "unitDesc")
    private String unitDesc;

    public String getUnitOpenId() { return unitOpenId; }
    public void setUnitOpenId(String unitOpenId) { this.unitOpenId = unitOpenId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getUnitParentId() { return unitParentId; }
    public void setUnitParentId(String unitParentId) { this.unitParentId = unitParentId; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public String getUnitDesc() { return unitDesc; }
    public void setUnitDesc(String unitDesc) { this.unitDesc = unitDesc; }
}