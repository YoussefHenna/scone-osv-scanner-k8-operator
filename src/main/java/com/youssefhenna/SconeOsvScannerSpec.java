package com.youssefhenna;

import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.model.ResourceRef;

public class SconeOsvScannerSpec {
    private String registryUrl;
    private String registryRepository;
    private RegistryCredentials registryCredentials;
    private String casAddress;
    private ResourceRef casPolicy;

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

    public ResourceRef getCasPolicy() {
        return casPolicy;
    }

    public void setCasPolicy(ResourceRef casPolicy) {
        this.casPolicy = casPolicy;
    }

}