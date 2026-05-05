package com.youssefhenna.spec.database;

import com.youssefhenna.spec.CommonDependantSpec;
import io.fabric8.generator.annotation.Required;

public class MariadbSpec extends CommonDependantSpec {
    @Required
    private String storageSize;

    @Required
    private String storageClassName;

    private boolean disablePersistence;

    private int replicas = 1;

    @Required
    private String replicaSconeConfigId;

    public String getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(String storageSize) {
        this.storageSize = storageSize;
    }

    public String getStorageClassName() {
        return storageClassName;
    }

    public void setStorageClassName(String storageClassName) {
        this.storageClassName = storageClassName;
    }

    public boolean isDisablePersistence() {
        return disablePersistence;
    }

    public void setDisablePersistence(boolean disablePersistence) {
        this.disablePersistence = disablePersistence;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public String getReplicaSconeConfigId() {
        return replicaSconeConfigId;
    }

    public void setReplicaSconeConfigId(String replicaSconeConfigId) {
        this.replicaSconeConfigId = replicaSconeConfigId;
    }
}
