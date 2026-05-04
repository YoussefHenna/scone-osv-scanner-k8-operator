package com.youssefhenna.spec.scanner;

import com.youssefhenna.model.RegistryCredentials;
import io.fabric8.generator.annotation.Required;

public class ScannerSpec {
    @Required
    private String registryUrl;

    private RegistryCredentials registryCredentials;

    @Required
    private DbManagerSpec dbManager;

    @Required
    private FrontAppSpec frontApp;

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

    public DbManagerSpec getDbManager() {
        return dbManager;
    }

    public void setDbManager(DbManagerSpec dbManager) {
        this.dbManager = dbManager;
    }

    public FrontAppSpec getFrontApp() {
        return frontApp;
    }

    public void setFrontApp(FrontAppSpec frontApp) {
        this.frontApp = frontApp;
    }


}
