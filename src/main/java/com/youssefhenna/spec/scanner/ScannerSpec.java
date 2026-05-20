package com.youssefhenna.spec.scanner;

import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.spec.CommonRegistrySpec;
import io.fabric8.generator.annotation.Required;

public class ScannerSpec extends CommonRegistrySpec {
    @Required
    private DbManagerSpec dbManager;

    @Required
    private FrontAppSpec frontApp;

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
