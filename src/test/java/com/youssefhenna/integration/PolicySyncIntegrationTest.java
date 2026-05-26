package com.youssefhenna.integration;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.integration.utils.IntegrationTestCrBuilder;
import com.youssefhenna.integration.utils.IntegrationTestProfile;
import com.youssefhenna.integration.utils.TestCASClientFactory;
import com.youssefhenna.model.PollConfig;
import com.youssefhenna.policy.cas.CASClient;
import com.youssefhenna.policy.model.SPOLDefinition;
import com.youssefhenna.policy.model.http.ReadSessionResponse;
import com.youssefhenna.policy.model.http.UploadSessionResponse;
import com.youssefhenna.status.SconeOsvScannerStatus;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.bouncycastle.openpgp.api.OpenPGPKey;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pgpainless.PGPainless;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.youssefhenna.TestUtils.*;
import static com.youssefhenna.integration.utils.IntegrationTestCrBuilder.CR_NAME;
import static com.youssefhenna.integration.utils.IntegrationTestCrBuilder.NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
class PolicySyncIntegrationTest {

    private static final int POLL_SECONDS = 30;
    private static final String GIT_BRANCH = "master";
    // Matches the prefix of dbManager sconeConfigId "osv-scanner-sample2/scone-osv-scan/dbmanager"
    private static final String SPOL_SESSION_NAME = "osv-scanner-sample2/scone-osv-scan";

    @Inject
    KubernetesClient client;

    @Inject
    TestCASClientFactory casClientFactory;

    private OpenPGPKey rootKey;
    private OpenPGPKey spolSignerKey;
    private String rootKeyArmored;
    private String spolSignerKeyArmored;
    private Path repoDir;
    private Git git;

    @BeforeEach
    void setUp() throws Exception {
        PGPainless pgp = PGPainless.getInstance();
        rootKey = pgp.generateKey().modernKeyRing("Root <root@test.com>");
        spolSignerKey = pgp.generateKey().modernKeyRing("Policy Signer <delegate@test.com>");
        rootKeyArmored = rootKey.toCertificate().toAsciiArmoredString();
        spolSignerKeyArmored = spolSignerKey.toCertificate().toAsciiArmoredString();

        repoDir = Files.createTempDirectory("test-policies-repo");
        git = Git.init().setDirectory(repoDir.toFile()).setInitialBranch(GIT_BRANCH).call();
        git.commit().setSign(false).setMessage("init").setAllowEmpty(true).call();
    }

    @AfterEach
    void tearDown() throws IOException {
        casClientFactory.reset();
        if (git != null) git.close();
        deleteDirectory(repoDir);
    }

    @Test
    void when_pollElapses_policySyncIsReRun() throws Exception {
        addAuthFileAndPolicy();

        casClientFactory.setClient(new FixedHashCASClient("hash-v1"));

        SconeOsvScanner cr = new IntegrationTestCrBuilder()
            .withPolicyUpstream(
                repoDir.toAbsolutePath().toString(),
                GIT_BRANCH,
                new ArrayList<>(List.of(rootKeyArmored)),
                new PollConfig(POLL_SECONDS, PollConfig.Unit.SECONDS)
            )
            .withAutoUpdatePoll(POLL_SECONDS, PollConfig.Unit.SECONDS)
            .build();
        try {
            client.resource(cr).create();

            await().atMost(60, TimeUnit.SECONDS).until(() -> {
                SconeOsvScannerStatus status = getStatus(cr);
                return status != null
                    && status.getPolicyUploadStatus() != null
                    && status.getPolicyUploadStatus().getLastSyncTime() != null;
            });
            String firstSyncTime = getStatus(cr).getPolicyUploadStatus().getLastSyncTime();

            await().atMost(90, TimeUnit.SECONDS).until(() -> {
                SconeOsvScannerStatus status = getStatus(cr);
                return status != null
                    && status.getPolicyUploadStatus() != null
                    && !firstSyncTime.equals(status.getPolicyUploadStatus().getLastSyncTime());
            });
        } finally {
            client.resource(cr).delete();
        }
    }

    @Test
    void when_policyHashChanges_deploymentPodTemplateAnnotationUpdated() throws Exception {
        addAuthFileAndPolicy();

        ConfigurableMockCASClient mockClient = new ConfigurableMockCASClient("hash-v1");
        casClientFactory.setClient(mockClient);

        SconeOsvScanner cr = new IntegrationTestCrBuilder()
            .withPolicyUpstream(
                repoDir.toAbsolutePath().toString(),
                GIT_BRANCH,
                new ArrayList<>(List.of(rootKeyArmored)),
                new PollConfig(POLL_SECONDS, PollConfig.Unit.SECONDS)
            )
            .withAutoUpdatePoll(POLL_SECONDS, PollConfig.Unit.SECONDS)
            .build();

        String dbManagerDeploymentName = Constants.getDbManagerDeploymentName(CR_NAME);
        try {
            client.resource(cr).create();

            await().atMost(90, TimeUnit.SECONDS).untilAsserted(() -> {
                Deployment deployment = client.apps().deployments()
                    .inNamespace(NAMESPACE)
                    .withName(dbManagerDeploymentName)
                    .get();
                assertNotNull(deployment, "Deployment not found: " + dbManagerDeploymentName);
                Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
                assertNotNull(annotations, "No annotations on pod template");
                assertEquals("hash-v1", annotations.get(Constants.POLICY_HASH_ANNOTATION));
            });

            mockClient.setHash("hash-v2");

            await().atMost(90, TimeUnit.SECONDS).untilAsserted(() -> {
                Deployment deployment = client.apps().deployments()
                    .inNamespace(NAMESPACE)
                    .withName(dbManagerDeploymentName)
                    .get();
                assertNotNull(deployment, "Deployment not found: " + dbManagerDeploymentName);
                Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
                assertNotNull(annotations, "No annotations on pod template");
                assertEquals("hash-v2", annotations.get(Constants.POLICY_HASH_ANNOTATION));
            });
        } finally {
            client.resource(cr).delete();
        }
    }

    private void addAuthFileAndPolicy() throws Exception {
        Files.write(repoDir.resolve("gpg-key-authorization-0.yml.asc"),
            signInline(rootKey, buildGpgAuthYaml(repoDir.toAbsolutePath().toString(), pastDate(), futureDate(), List.of(spolSignerKeyArmored))));

        byte[] spolJson = buildSpolJson(SPOL_SESSION_NAME);
        String spolVersionedFilename = "0-osv-policy.json";
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256(spolJson), repoDir.toAbsolutePath().toString())));

        git.add().addFilepattern(".").call();
        git.commit().setSign(false).setMessage("add auth and policy").call();
    }

    private SconeOsvScannerStatus getStatus(SconeOsvScanner cr) {
        SconeOsvScanner updated = client.resource(cr).get();
        return updated != null ? updated.getStatus() : null;
    }

    private static class FixedHashCASClient implements CASClient {
        private final String hash;

        private FixedHashCASClient(String hash) {
            this.hash = hash;
        }

        @Override
        public ReadSessionResponse readSession(String name) throws CASClientException {
            throw new CASClientException(404, "not found");
        }

        @Override
        public UploadSessionResponse uploadSession(SPOLDefinition body) {
            UploadSessionResponse resp = new UploadSessionResponse();
            resp.setHash(hash);
            return resp;
        }
    }

    private static class ConfigurableMockCASClient implements CASClient {
        private volatile String hash;

        private ConfigurableMockCASClient(String initialHash) {
            this.hash = initialHash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        @Override
        public ReadSessionResponse readSession(String name) throws CASClientException {
            throw new CASClientException(404, "not found");
        }

        @Override
        public UploadSessionResponse uploadSession(SPOLDefinition body) {
            UploadSessionResponse resp = new UploadSessionResponse();
            resp.setHash(hash);
            return resp;
        }
    }
}