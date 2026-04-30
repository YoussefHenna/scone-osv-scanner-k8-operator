package com.youssefhenna.spec;

public class CommonDependantSpec {
    private String imageName;
    private String imageVersion;
    private int replicas;
    private String memory;
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

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
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
