package com.youssefhenna.integration.utils;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.model.PollConfig;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.spec.database.MariadbSpec;
import com.youssefhenna.spec.database.MaxscaleSpec;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.spec.scanner.DbManagerSpec;
import com.youssefhenna.spec.scanner.FrontAppSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.util.ArrayList;

public class IntegrationTestCrBuilder {

    public static final String CR_NAME = "osv-scanner-sample";
    public static final String NAMESPACE = "default";
    static final String TEST_COSIGN_KEY = "-----BEGIN PUBLIC KEY-----\ntestkey\n-----END PUBLIC KEY-----";

    private boolean cosignKey = false;
    private boolean dbManagerAutoUpdate = false;
    private PollConfig autoUpdatePoll = null;
    private PolicyUpstreamSpec policyUpstream = null;

    public IntegrationTestCrBuilder withCosignKey() {
        this.cosignKey = true;
        return this;
    }

    public IntegrationTestCrBuilder withDbManagerAutoUpdate() {
        this.dbManagerAutoUpdate = true;
        return this;
    }

    public IntegrationTestCrBuilder withAutoUpdatePoll(int every, PollConfig.Unit unit) {
        this.autoUpdatePoll = new PollConfig(every, unit);
        return this;
    }

    public IntegrationTestCrBuilder withPolicyUpstream(String gitUrl, String branch, ArrayList<String> gpgKeys, PollConfig poll) {
        PolicyUpstreamSpec upstream = new PolicyUpstreamSpec();
        upstream.setGitUrl(gitUrl);
        upstream.setBranch(branch);
        upstream.setGpgKeys(gpgKeys);
        upstream.setPoll(poll);
        this.policyUpstream = upstream;
        return this;
    }

    public SconeOsvScanner build() {
        SconeOsvScanner cr = new SconeOsvScanner();
        cr.setMetadata(new ObjectMetaBuilder()
            .withName(CR_NAME)
            .withNamespace(NAMESPACE)
            .build());
        cr.setSpec(buildSpec());
        return cr;
    }

    private SconeOsvScannerSpec buildSpec() {
        SconeOsvScannerSpec spec = new SconeOsvScannerSpec();
        spec.setCasAddress("scone-cas.cf");
        spec.setRegistryUrl("registry-1.docker.io");
        spec.setScanner(buildScannerSpec());
        spec.setDatabase(buildDatabaseSpec());
        if (autoUpdatePoll != null) {
            spec.setAutoUpdatePoll(autoUpdatePoll);
        }
        if (policyUpstream != null) {
            spec.setPolicyUpstream(policyUpstream);
        }
        return spec;
    }

    private ScannerSpec buildScannerSpec() {
        ScannerSpec scanner = new ScannerSpec();
        scanner.setDbManager(buildDbManagerSpec());
        scanner.setFrontApp(buildFrontAppSpec());
        return scanner;
    }

    private DbManagerSpec buildDbManagerSpec() {
        DbManagerSpec dbManager = new DbManagerSpec();
        dbManager.setImageName("youssefhenna/sos-dbmanager");
        dbManager.setImageVersion("1.0.0");
        dbManager.setMemory("12G");
        dbManager.setSconeConfigId("osv-scanner-sample2/scone-osv-scan/dbmanager");
        dbManager.setAutoUpdate(dbManagerAutoUpdate);
        if (cosignKey) dbManager.setCosignPublicKey(TEST_COSIGN_KEY);
        return dbManager;
    }

    private FrontAppSpec buildFrontAppSpec() {
        FrontAppSpec frontApp = new FrontAppSpec();
        frontApp.setImageName("youssefhenna/sos-osvscan");
        frontApp.setImageVersion("1.0.0");
        frontApp.setReplicas(1);
        frontApp.setMemory("5G");
        frontApp.setSconeConfigId("osv-scanner-sample2/scone-osv-scan/osvscan");
        if (cosignKey) frontApp.setCosignPublicKey(TEST_COSIGN_KEY);
        return frontApp;
    }

    private DatabaseSpec buildDatabaseSpec() {
        DatabaseSpec database = new DatabaseSpec();
        database.setRegistryUrl("registry.scontain.com");
        database.setMaxscale(buildMaxscaleSpec());
        database.setMariadb(buildMariadbSpec());
        return database;
    }

    private MaxscaleSpec buildMaxscaleSpec() {
        MaxscaleSpec maxscale = new MaxscaleSpec();
        maxscale.setImageName("sconecuratedimages/apps");
        maxscale.setImageVersion("maxscale-2.5-alpine");
        maxscale.setReplicas(1);
        maxscale.setMemory("2G");
        maxscale.setSconeConfigId("osv-scanner-sample2/mariadb-maxscale/maxscale");
        if (cosignKey) maxscale.setCosignPublicKey(TEST_COSIGN_KEY);
        return maxscale;
    }

    private MariadbSpec buildMariadbSpec() {
        MariadbSpec mariadb = new MariadbSpec();
        mariadb.setImageName("scone.cloud/mariadb-11-alpine");
        mariadb.setImageVersion("6.0.6");
        mariadb.setMemory("4G");
        mariadb.setSconeConfigId("osv-scanner-sample2/mariadb-primary");
        mariadb.setReplicaSconeConfigId("osv-scanner-sample2/mariadb-replica");
        mariadb.setReplicas(1);
        mariadb.setStorageSize("10Gi");
        mariadb.setStorageClassName("default");
        mariadb.setDisablePersistence(true);
        if (cosignKey) mariadb.setCosignPublicKey(TEST_COSIGN_KEY);
        return mariadb;
    }
}