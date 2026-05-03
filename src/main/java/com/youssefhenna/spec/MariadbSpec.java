package com.youssefhenna.spec;

import io.fabric8.generator.annotation.Required;

public class MariadbSpec extends CommonDependantSpec {
    @Required
    private String storageSize;

    private boolean disablePersistence;

    public String getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(String storageSize) {
        this.storageSize = storageSize;
    }

    public boolean isDisablePersistence() {
        return disablePersistence;
    }

    public void setDisablePersistence(boolean disablePersistence) {
        this.disablePersistence = disablePersistence;
    }
}
