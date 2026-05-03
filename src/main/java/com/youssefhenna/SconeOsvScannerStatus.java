package com.youssefhenna;

import com.youssefhenna.model.DependantStatus;

public class SconeOsvScannerStatus {

    private DependantStatus dbManagerStatus;
    private DependantStatus frontAppStatus;
    private DependantStatus maxscaleStatus;
    private DependantStatus mariadbPrimaryStatus;
    private DependantStatus mariadbReplicaStatus;

    public DependantStatus getDbManagerStatus() {
        return dbManagerStatus;
    }

    public void setDbManagerStatus(DependantStatus dbManagerStatus) {
        this.dbManagerStatus = dbManagerStatus;
    }

    public DependantStatus getFrontAppStatus() {
        return frontAppStatus;
    }

    public void setFrontAppStatus(DependantStatus frontAppStatus) {
        this.frontAppStatus = frontAppStatus;
    }

    public DependantStatus getMaxscaleStatus() {
        return maxscaleStatus;
    }

    public void setMaxscaleStatus(DependantStatus maxscaleStatus) {
        this.maxscaleStatus = maxscaleStatus;
    }

    public DependantStatus getMariadbPrimaryStatus() {
        return mariadbPrimaryStatus;
    }

    public void setMariadbPrimaryStatus(DependantStatus mariadbPrimaryStatus) {
        this.mariadbPrimaryStatus = mariadbPrimaryStatus;
    }

    public DependantStatus getMariadbReplicaStatus() {
        return mariadbReplicaStatus;
    }

    public void setMariadbReplicaStatus(DependantStatus mariadbReplicaStatus) {
        this.mariadbReplicaStatus = mariadbReplicaStatus;
    }
}