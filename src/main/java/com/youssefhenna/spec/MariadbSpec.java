package com.youssefhenna.spec;

import io.fabric8.generator.annotation.Required;

public class MariadbSpec extends CommonDependantSpec {
    @Required
    private String storageSize;

    public String getStorageSize() {
        return storageSize;
    }

    public void setStorageSize(String storageSize) {
        this.storageSize = storageSize;
    }

}
