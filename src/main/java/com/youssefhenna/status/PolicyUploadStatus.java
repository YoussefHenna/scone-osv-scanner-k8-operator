package com.youssefhenna.status;

import com.youssefhenna.spec.policy.PolicyUpstreamSpec;

import java.util.Map;

public class PolicyUploadStatus {
    private Map<String, PolicyUploadStatusItem> policyUpdateStatuses;
    private PolicyUpdateRunStatus lastRunStatus;
    private String lastSyncTime;
    private PolicyUpstreamSpec lastSyncedUpstream;

    public Map<String, PolicyUploadStatusItem> getPolicyUpdateStatuses() {
        return policyUpdateStatuses;
    }

    public void setPolicyUpdateStatuses(Map<String, PolicyUploadStatusItem> policyUpdateStatuses) {
        this.policyUpdateStatuses = policyUpdateStatuses;
    }

    public PolicyUpdateRunStatus getLastRunStatus() {
        return lastRunStatus;
    }

    public void setLastRunStatus(PolicyUpdateRunStatus lastRunStatus) {
        this.lastRunStatus = lastRunStatus;
    }

    public String getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(String lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public PolicyUpstreamSpec getLastSyncedUpstream() {
        return lastSyncedUpstream;
    }

    public void setLastSyncedUpstream(PolicyUpstreamSpec lastSyncedUpstream) {
        this.lastSyncedUpstream = lastSyncedUpstream;
    }
}
