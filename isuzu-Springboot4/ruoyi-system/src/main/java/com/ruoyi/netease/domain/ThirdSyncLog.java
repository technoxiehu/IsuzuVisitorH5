package com.ruoyi.netease.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 第三方同步日志表 sys_third_sync_log
 *
 * @author isuzu
 */
public class ThirdSyncLog
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String syncType;
    private String syncAction;
    private String thirdId;
    private Long ruoyiId;
    private String beforeJson;
    private String afterJson;
    private Integer syncStatus;
    private String errorMsg;
    private Long syncVersion;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSyncType() { return syncType; }
    public void setSyncType(String syncType) { this.syncType = syncType; }
    public String getSyncAction() { return syncAction; }
    public void setSyncAction(String syncAction) { this.syncAction = syncAction; }
    public String getThirdId() { return thirdId; }
    public void setThirdId(String thirdId) { this.thirdId = thirdId; }
    public Long getRuoyiId() { return ruoyiId; }
    public void setRuoyiId(Long ruoyiId) { this.ruoyiId = ruoyiId; }
    public String getBeforeJson() { return beforeJson; }
    public void setBeforeJson(String beforeJson) { this.beforeJson = beforeJson; }
    public String getAfterJson() { return afterJson; }
    public void setAfterJson(String afterJson) { this.afterJson = afterJson; }
    public Integer getSyncStatus() { return syncStatus; }
    public void setSyncStatus(Integer syncStatus) { this.syncStatus = syncStatus; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Long getSyncVersion() { return syncVersion; }
    public void setSyncVersion(Long syncVersion) { this.syncVersion = syncVersion; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("syncType", getSyncType())
            .append("syncAction", getSyncAction())
            .append("thirdId", getThirdId())
            .append("ruoyiId", getRuoyiId())
            .append("syncStatus", getSyncStatus())
            .append("syncVersion", getSyncVersion())
            .toString();
    }
}