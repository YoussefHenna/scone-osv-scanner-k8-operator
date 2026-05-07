package com.youssefhenna.status;

import java.util.Map;

public class PolicyUploadStatus {
    private Map<String, PolicyUploadStatusItem> policyUpdateStatuses;
    private PolicyUpdateRunStatus lastRunStatus;

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
}
