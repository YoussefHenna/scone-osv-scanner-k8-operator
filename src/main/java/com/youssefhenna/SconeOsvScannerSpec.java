package com.youssefhenna;

import com.youssefhenna.model.RegistryCredentials;

public class SconeOsvScannerSpec {
    private String registryUrl;
    private String registryRepository;
    private RegistryCredentials registryCredentials;
    private String casAddress;
    private DbManagerSpec dbManagerSpec;
    private FrontAppSpec frontAppSpec;
    private DatabaseSpec databaseSpec;


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

    public DatabaseSpec getDatabaseSpec() {
        return databaseSpec;
    }

    public void setDatabaseSpec(DatabaseSpec databaseSpec) {
        this.databaseSpec = databaseSpec;
    }

    static class CommonDependantDeploymentSpec {
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

    public static class DbManagerSpec extends CommonDependantDeploymentSpec {
    }

    public static class FrontAppSpec extends CommonDependantDeploymentSpec {
    }

    public static class DatabaseSpec {
        private String registryRepository;
        private MaxscaleSpec maxscale;
        private MariadbSpec mariadbPrimary;
        private MariadbSpec mariadbReplica;

        public String getRegistryRepository() {
            return registryRepository;
        }

        public void setRegistryRepository(String registryRepository) {
            this.registryRepository = registryRepository;
        }

        public MaxscaleSpec getMaxscale() {
            return maxscale;
        }

        public void setMaxscale(MaxscaleSpec maxscale) {
            this.maxscale = maxscale;
        }

        public MariadbSpec getMariadbPrimary() {
            return mariadbPrimary;
        }

        public void setMariadbPrimary(MariadbSpec mariadbPrimary) {
            this.mariadbPrimary = mariadbPrimary;
        }

        public MariadbSpec getMariadbReplica() {
            return mariadbReplica;
        }

        public void setMariadbReplica(MariadbSpec mariadbReplica) {
            this.mariadbReplica = mariadbReplica;
        }
    }

    public static class MaxscaleSpec extends CommonDependantDeploymentSpec {
    }

    public static class MariadbSpec extends CommonDependantDeploymentSpec {
        private String storageSize;
        private String storageClassName;

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
    }

}