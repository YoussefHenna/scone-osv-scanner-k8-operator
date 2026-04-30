package com.youssefhenna.spec;

import com.youssefhenna.model.RegistryCredentials;

public class DatabaseSpec {
    private String registryUrl;
    private String registryRepository;
    private RegistryCredentials registryCredentials;
    private MaxscaleSpec maxscaleSpec;
    private MariadbSpec mariadbPrimarySpec;
    private MariadbSpec mariadbReplicaSpec;

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

    public MaxscaleSpec getMaxscaleSpec() {
        return maxscaleSpec;
    }

    public void setMaxscaleSpec(MaxscaleSpec maxscaleSpec) {
        this.maxscaleSpec = maxscaleSpec;
    }

    public MariadbSpec getMariadbPrimarySpec() {
        return mariadbPrimarySpec;
    }

    public void setMariadbPrimarySpec(MariadbSpec mariadbPrimarySpec) {
        this.mariadbPrimarySpec = mariadbPrimarySpec;
    }

    public MariadbSpec getMariadbReplicaSpec() {
        return mariadbReplicaSpec;
    }

    public void setMariadbReplicaSpec(MariadbSpec mariadbReplicaSpec) {
        this.mariadbReplicaSpec = mariadbReplicaSpec;
    }
}
