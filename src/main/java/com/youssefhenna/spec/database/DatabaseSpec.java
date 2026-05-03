package com.youssefhenna.spec.database;

import com.youssefhenna.model.RegistryCredentials;
import io.fabric8.generator.annotation.Required;

public class DatabaseSpec {
    @Required
    private String registryUrl;

    @Required
    private String registryRepository;

    private RegistryCredentials registryCredentials;

    @Required
    private MaxscaleSpec maxscale;

    @Required
    private PrimaryMariaDbSpec mariadbPrimary;

    @Required
    private ReplicaMariaDbSpec mariadbReplica;

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

    public MaxscaleSpec getMaxscale() {
        return maxscale;
    }

    public void setMaxscale(MaxscaleSpec maxscale) {
        this.maxscale = maxscale;
    }

    public PrimaryMariaDbSpec getMariadbPrimary() {
        return mariadbPrimary;
    }

    public void setMariadbPrimary(PrimaryMariaDbSpec mariadbPrimary) {
        this.mariadbPrimary = mariadbPrimary;
    }

    public ReplicaMariaDbSpec getMariadbReplica() {
        return mariadbReplica;
    }

    public void setMariadbReplica(ReplicaMariaDbSpec mariadbReplica) {
        this.mariadbReplica = mariadbReplica;
    }
}
