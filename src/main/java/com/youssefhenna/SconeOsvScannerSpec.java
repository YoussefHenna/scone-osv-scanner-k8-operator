package com.youssefhenna;

import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.model.ResourceRef;

public class SconeOsvScannerSpec {
    private String registryUrl;
    private String registryRepository;
    private RegistryCredentials registryCredentials;
    private String casAddress;
    private DbManagerSpec dbManagerSpec;
    private FrontAppSpec frontAppSpec;


    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
    }

    public String getRegistryRepository() {
        return registryRepository;
    }

    public void setRegistryRepository(String registryRepository) {
        this.registryRepository = registryRepository;
    }

    public RegistryCredentials getRegistryCredentials() {
        return registryCredentials;
    }

    public void setRegistryCredentials(RegistryCredentials registryCredentials) {
        this.registryCredentials = registryCredentials;
    }

    public String getCasAddress() {
        return casAddress;
    }

    public void setCasAddress(String casAddress) {
        this.casAddress = casAddress;
    }

    public DbManagerSpec getDbManagerSpec() {
        return dbManagerSpec;
    }

    public void setDbManagerSpec(DbManagerSpec dbManagerSpec) {
        this.dbManagerSpec = dbManagerSpec;
    }

    public FrontAppSpec getFrontAppSpec() {
        return frontAppSpec;
    }

    public void setFrontAppSpec(FrontAppSpec frontAppSpec) {
        this.frontAppSpec = frontAppSpec;
    }

    static class CommonDependantDeploymentSpec {
        private String imageName;
        private int replicas;
        private String memory;
        private String sconeConfigId;

        public String getImageName() {
            return imageName;
        }

        public void setImageName(String imageName) {
            this.imageName = imageName;
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

    public static class DbManagerSpec extends CommonDependantDeploymentSpec { }

    public static class FrontAppSpec extends CommonDependantDeploymentSpec { }

}