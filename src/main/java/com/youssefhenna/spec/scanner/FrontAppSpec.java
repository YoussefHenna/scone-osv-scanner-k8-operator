package com.youssefhenna.spec.scanner;

import com.youssefhenna.spec.CommonDependantSpec;

public class FrontAppSpec extends CommonDependantSpec {
    private int replicas = 1;

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }
}
