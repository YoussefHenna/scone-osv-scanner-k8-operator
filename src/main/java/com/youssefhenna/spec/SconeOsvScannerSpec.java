package com.youssefhenna.spec;

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


}