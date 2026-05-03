package com.youssefhenna.spec;

import io.fabric8.generator.annotation.Required;

public class CommonDependantSpec {

    @Required
    private String imageName;

    @Required
    private String imageVersion;

    @Required
    private String memory;

    @Required
    private String sconeConfigId;

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageVersion() {
        return imageVersion;
    }

    public void setImageVersion(String imageVersion) {
        this.imageVersion = imageVersion;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getSconeConfigId() {
        return sconeConfigId;
    }

    public void setSconeConfigId(String sconeConfigId) {
        this.sconeConfigId = sconeConfigId;
    }
}
