package com.youssefhenna.status;

import java.util.Map;

public class PolicyUploadStatus {
    private Map<String, PolicyUpdateState> policyUpdateStates;

    public Map<String, PolicyUpdateState> getPolicyUpdateStates() {
        return policyUpdateStates;
    }

    public void setPolicyUpdateStates(Map<String, PolicyUpdateState> policyUpdateStates) {
        this.policyUpdateStates = policyUpdateStates;
    }
}
