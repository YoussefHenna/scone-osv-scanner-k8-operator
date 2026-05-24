package com.youssefhenna.spec;

import com.youssefhenna.model.PollConfig;
import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import io.fabric8.generator.annotation.Required;

public class SconeOsvScannerSpec extends CommonRegistrySpec {
    @Required
    private String casAddress;

    private int casPort = 8081;

    @Required
    private ScannerSpec scanner;

    @Required
    private DatabaseSpec database;

    private PolicyUpstreamSpec policyUpstream;

    private PollConfig autoUpdatePoll = new PollConfig(10, PollConfig.Unit.MINUTES);

    public String getCasAddress() {
        return casAddress;
    }

    public void setCasAddress(String casAddress) {
        this.casAddress = casAddress;
    }

    public int getCasPort() {
        return casPort;
    }

    public void setCasPort(int casPort) {
        this.casPort = casPort;
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

    public PollConfig getAutoUpdatePoll() {
        return autoUpdatePoll;
    }

    public void setAutoUpdatePoll(PollConfig autoUpdatePoll) {
        this.autoUpdatePoll = autoUpdatePoll;
    }
}