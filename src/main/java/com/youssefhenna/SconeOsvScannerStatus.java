package com.youssefhenna;

import com.youssefhenna.model.DeploymentStatus;

public class SconeOsvScannerStatus {

    private DeploymentStatus dbManagerDeploymentStatus;
    private DeploymentStatus frontAppDeploymentStatus;

    public DeploymentStatus getDbManagerDeploymentStatus() {
        return dbManagerDeploymentStatus;
    }

    public void setDbManagerDeploymentStatus(DeploymentStatus dbManagerDeploymentStatus) {
        this.dbManagerDeploymentStatus = dbManagerDeploymentStatus;
    }

    public DeploymentStatus getFrontAppDeploymentStatus() {
        return frontAppDeploymentStatus;
    }

    public void setFrontAppDeploymentStatus(DeploymentStatus frontAppDeploymentStatus) {
        this.frontAppDeploymentStatus = frontAppDeploymentStatus;
    }
}