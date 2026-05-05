package com.youssefhenna.spec.database;

import com.youssefhenna.model.RegistryCredentials;
import io.fabric8.generator.annotation.Required;

public class DatabaseSpec {
    @Required
    private String registryUrl;

    private RegistryCredentials registryCredentials;

    @Required
    private MaxscaleSpec maxscale;

    @Required
    private MariadbSpec mariadb;

    public String getRegistryUrl() {
        return registryUrl;
    }

    public void setRegistryUrl(String registryUrl) {
        this.registryUrl = registryUrl;
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

    public MariadbSpec getMariadb() {
        return mariadb;
    }

    public void setMariadb(MariadbSpec mariadb) {
        this.mariadb = mariadb;
    }
}
