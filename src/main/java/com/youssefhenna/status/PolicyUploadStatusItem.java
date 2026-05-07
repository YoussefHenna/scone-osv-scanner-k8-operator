package com.youssefhenna.status;

import com.youssefhenna.utils.Common;

import java.util.Date;

public class PolicyUploadStatusItem {
    private String name;
    private String lastStateUpdate;
    private PolicyUpdateState lastState;
    private String lastFile;

    public PolicyUploadStatusItem(String name, String lastFile) {
        this.name = name;
        this.lastFile = lastFile;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastStateUpdate() {
        return lastStateUpdate;
    }

    public PolicyUpdateState getLastState() {
        return lastState;
    }

    public void setLastState(PolicyUpdateState lastState) {
        this.lastState = lastState;
        this.lastStateUpdate = Common.dateToString(new Date());
    }

    public String getLastFile() {
        return lastFile;
    }

    public void setLastFile(String lastFile) {
        this.lastFile = lastFile;
    }
}
