package com.youssefhenna.status;

public class DependantStatus {
    private DependantState state;
    private String currentVersion;

    public DependantStatus() {}

    public DependantStatus(DependantState state, String currentVersion) {
        this.state = state;
        this.currentVersion = currentVersion;
    }

    public DependantState getState() {
        return state;
    }

    public void setState(DependantState state) {
        this.state = state;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }
}
