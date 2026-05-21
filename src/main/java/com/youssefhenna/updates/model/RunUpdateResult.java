package com.youssefhenna.updates.model;

import com.youssefhenna.status.UpdateStatus;

public class RunUpdateResult {
    private String currentVersion;
    private UpdateStatus lastUpdateStatus;

    public RunUpdateResult(String currentVersion, UpdateStatus lastUpdateStatus) {
        this.currentVersion = currentVersion;
        this.lastUpdateStatus = lastUpdateStatus;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public UpdateStatus getLastUpdateStatus() {
        return lastUpdateStatus;
    }

    public void setLastUpdateStatus(UpdateStatus lastUpdateStatus) {
        this.lastUpdateStatus = lastUpdateStatus;
    }
}
