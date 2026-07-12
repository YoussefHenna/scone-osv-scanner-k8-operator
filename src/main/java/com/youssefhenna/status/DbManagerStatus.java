package com.youssefhenna.status;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// DB Manager's internally exposed status (GET /status), merged onto the operator-derived dependant status.
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbManagerStatus extends DependantStatus {

    private String status;
    private String dbLastUpdate;
    private int dbVulnerabilityCount;
    private int cacheSbomCount;
    private long uptimeSeconds;
    private UpdateStatus currentUpdate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDbLastUpdate() {
        return dbLastUpdate;
    }

    public void setDbLastUpdate(String dbLastUpdate) {
        this.dbLastUpdate = dbLastUpdate;
    }

    public int getDbVulnerabilityCount() {
        return dbVulnerabilityCount;
    }

    public void setDbVulnerabilityCount(int dbVulnerabilityCount) {
        this.dbVulnerabilityCount = dbVulnerabilityCount;
    }

    public int getCacheSbomCount() {
        return cacheSbomCount;
    }

    public void setCacheSbomCount(int cacheSbomCount) {
        this.cacheSbomCount = cacheSbomCount;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public UpdateStatus getCurrentUpdate() {
        return currentUpdate;
    }

    public void setCurrentUpdate(UpdateStatus currentUpdate) {
        this.currentUpdate = currentUpdate;
    }

    // Progress of in-progress database update, present only while an update is running.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateStatus {

        private String status;
        private String startTime;
        private String lastUpdateTime;
        private String currentEcosystem;
        private String ecosystemProgress;
        private String processingFiles;
        private double progressPercent;
        private String message;

        @JsonProperty("error")
        private String errorMessage;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(String lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getCurrentEcosystem() {
            return currentEcosystem;
        }

        public void setCurrentEcosystem(String currentEcosystem) {
            this.currentEcosystem = currentEcosystem;
        }

        public String getEcosystemProgress() {
            return ecosystemProgress;
        }

        public void setEcosystemProgress(String ecosystemProgress) {
            this.ecosystemProgress = ecosystemProgress;
        }

        public String getProcessingFiles() {
            return processingFiles;
        }

        public void setProcessingFiles(String processingFiles) {
            this.processingFiles = processingFiles;
        }

        public double getProgressPercent() {
            return progressPercent;
        }

        public void setProgressPercent(double progressPercent) {
            this.progressPercent = progressPercent;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
