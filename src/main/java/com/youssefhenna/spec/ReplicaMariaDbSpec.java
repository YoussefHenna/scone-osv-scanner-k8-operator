package com.youssefhenna.spec;

public class ReplicaMariaDbSpec extends MariadbSpec {
    private int replicas = 1;

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }
}
