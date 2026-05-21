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

    public String getHashForConfigId(String configId) {
        if (policyUpdateStatuses == null) return null;
        PolicyUploadStatusItem item = policyUpdateStatuses.get(configId);
        if (item != null) return item.getLastHash();

        // configId may be pointing to an internal service of a policy. ex: scone-osv-scan/dbmanager where the policy name is scone-osv-scan.
        // There would be no 1:1 matching from policy name to config id in that case.
        // To tolerate that, find the longest match in the known policies to the config id
        String longestMatch = null;
        for (String key : policyUpdateStatuses.keySet()) {
            if (configId.startsWith(key) && (longestMatch == null || key.length() > longestMatch.length())) {
                longestMatch = key;
            }
        }
        if (longestMatch == null) return null;
        return policyUpdateStatuses.get(longestMatch).getLastHash();
    }

    public boolean hashesChanged(PolicyUploadStatus previous) {
        if (previous == null || previous.getPolicyUpdateStatuses() == null) return false;
        if (policyUpdateStatuses == null) return false;
        for (Map.Entry<String, PolicyUploadStatusItem> entry : policyUpdateStatuses.entrySet()) {
            PolicyUploadStatusItem prev = previous.getPolicyUpdateStatuses().get(entry.getKey());
            String newHash = entry.getValue().getLastHash();
            String oldHash = prev != null ? prev.getLastHash() : null;
            if (newHash != null && !newHash.equals(oldHash)) return true;
        }
        return false;
    }
}
