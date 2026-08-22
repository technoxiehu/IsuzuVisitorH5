package com.ruoyi.netease.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 第三方系统同步映射表 sys_third_sync_mapping
 *
 * @author isuzu
 */
public class ThirdSyncMapping
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String thirdType;
    private String ruoyiTable;
    private Long ruoyiId;
    private String thirdId;
    private String thirdAltId;
    private String thirdParentId;
    private String thirdJson;
    private Integer syncStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncTime;
    private Long syncVersion;
    private String createBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getThirdType() { return thirdType; }
    public void setThirdType(String thirdType) { this.thirdType = thirdType; }
    public String getRuoyiTable() { return ruoyiTable; }
    public void setRuoyiTable(String ruoyiTable) { this.ruoyiTable = ruoyiTable; }
    public Long getRuoyiId() { return ruoyiId; }
    public void setRuoyiId(Long ruoyiId) { this.ruoyiId = ruoyiId; }
    public String getThirdId() { return thirdId; }
    public void setThirdId(String thirdId) { this.thirdId = thirdId; }
    public String getThirdAltId() { return thirdAltId; }
    public void setThirdAltId(String thirdAltId) { this.thirdAltId = thirdAltId; }
    public String getThirdParentId() { return thirdParentId; }
    public void setThirdParentId(String thirdParentId) { this.thirdParentId = thirdParentId; }
    public String getThirdJson() { return thirdJson; }
    public void setThirdJson(String thirdJson) { this.thirdJson = thirdJson; }
    public Integer getSyncStatus() { return syncStatus; }
    public void setSyncStatus(Integer syncStatus) { this.syncStatus = syncStatus; }
    public Date getSyncTime() { return syncTime; }
    public void setSyncTime(Date syncTime) { this.syncTime = syncTime; }
    public Long getSyncVersion() { return syncVersion; }
    public void setSyncVersion(Long syncVersion) { this.syncVersion = syncVersion; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("thirdType", getThirdType())
            .append("ruoyiTable", getRuoyiTable())
            .append("ruoyiId", getRuoyiId())
            .append("thirdId", getThirdId())
            .append("syncStatus", getSyncStatus())
            .append("syncVersion", getSyncVersion())
            .toString();
    }
}