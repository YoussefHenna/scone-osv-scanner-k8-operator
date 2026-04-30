package com.youssefhenna.spec;

import com.youssefhenna.model.RegistryCredentials;
import io.fabric8.generator.annotation.Required;

public class DatabaseSpec {
    @Required
    private String registryUrl;

    @Required
    private String registryRepository;

    private RegistryCredentials registryCredentials;

    @Required
    private MaxscaleSpec maxscaleSpec;

    @Required
    private MariadbSpec mariadbPrimarySpec;

    @Required
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
