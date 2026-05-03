package com.youssefhenna.spec;

import com.youssefhenna.model.RegistryCredentials;
import io.fabric8.generator.annotation.Required;

public class SconeOsvScannerSpec {
    @Required
    private String casAddress;

    @Required
    private ScannerSpec scanner;

    @Required
    private DatabaseSpec database;

    public String getCasAddress() {
        return casAddress;
    }

    public void setCasAddress(String casAddress) {
        this.casAddress = casAddress;
    }

    public ScannerSpec getScanner() {
        return scanner;
    }

    public void setScanner(ScannerSpec scanner) {
        this.scanner = scanner;
    }

    public DatabaseSpec getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseSpec database) {
        this.database = database;
    }


}