package com.youssefhenna.spec;

import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import io.fabric8.generator.annotation.Required;

public class SconeOsvScannerSpec {
    @Required
    private String casAddress;

    @Required
    private ScannerSpec scanner;

    @Required
    private DatabaseSpec database;

    private PolicyUpstreamSpec policyUpstream;

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

    public PolicyUpstreamSpec getPolicyUpstream() {
        return policyUpstream;
    }

    public void setPolicyUpstream(PolicyUpstreamSpec policyUpstream) {
        this.policyUpstream = policyUpstream;
    }
}