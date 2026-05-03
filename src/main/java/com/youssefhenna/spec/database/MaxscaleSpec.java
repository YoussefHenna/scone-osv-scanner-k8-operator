package com.youssefhenna.spec.database;

import com.youssefhenna.spec.CommonDependantSpec;

public class MaxscaleSpec extends CommonDependantSpec {
    private int replicas = 1;

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }
}
