package com.youssefhenna.policy;

import com.youssefhenna.policy.cas.CASClient;
import com.youssefhenna.policy.model.SPOLDefinition;
import com.youssefhenna.policy.model.http.ReadSessionResponse;
import com.youssefhenna.policy.model.http.UploadSessionResponse;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.status.PolicyUpdateRunStatus;
import com.youssefhenna.status.PolicyUpdateState;
import io.quarkus.test.junit.QuarkusTest;
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

import static com.youssefhenna.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PolicySyncTest {

    private static final String GIT_BRANCH = "master";

    private OpenPGPKey rootKey;
    private OpenPGPKey spolSignerKey;
    private String rootKeyArmored;
    private String spolSignerKeyArmored;
    private Path repoDir;
    private Git git;
    private MockCASClient casClient;

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

        casClient = new MockCASClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (git != null) git.close();
        deleteDirectory(repoDir);
    }

    @Test
    void when_validAuthFileAndSpol_spolIsUploadedToCAS() throws Exception {
        String sessionName = "test-session";
        byte[] spolJson = buildSpolJson(sessionName);
        String spolVersionedFilename = "0-osv-policy.json";
        String sha256 = sha256(spolJson);

        Files.write(repoDir.resolve("gpg-key-authorization-0.yml.asc"),
            signInline(rootKey, buildGpgAuthYaml(repoDir.toAbsolutePath().toString(), pastDate(), futureDate(), List.of(spolSignerKeyArmored))));
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256, repoDir.toAbsolutePath().toString())));
        commitAll("add policies");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(1, casClient.uploaded.size());
        assertEquals(PolicyUpdateState.SUCCESS_UPLOADED, result.statuses().getFirst().getLastState());
    }


    @Test
    void when_noAuthFilesInRepo_nothingIsUploaded() throws Exception {
        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";

        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256(spolJson), repoDir.toAbsolutePath().toString())));
        commitAll("add spol without auth");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_authFileValidityIsExpired_nothingIsUploaded() throws Exception {
        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";

        Files.write(repoDir.resolve("gpg-key-authorization-0.yml.asc"),
            signInline(rootKey, buildGpgAuthYaml(repoDir.toAbsolutePath().toString(), pastDate(30), pastDate(1), List.of(spolSignerKeyArmored))));
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256(spolJson), repoDir.toAbsolutePath().toString())));
        commitAll("add expired auth + spol");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_authFileRepoDoesNotMatchUpstreamUrl_nothingIsUploaded() throws Exception {
        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";

        Files.write(repoDir.resolve("gpg-key-authorization-0.yml.asc"),
            signInline(rootKey, buildGpgAuthYaml("https://other-repo.com/policies.git", pastDate(), futureDate(), List.of(spolSignerKeyArmored))));
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256(spolJson), repoDir.toAbsolutePath().toString())));
        commitAll("add wrong-repo auth");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_authFileSignedByKeyNotInRootGpgKeys_nothingIsUploaded() throws Exception {
        OpenPGPKey untrustedKey = PGPainless.getInstance().generateKey().modernKeyRing("Untrusted <u@test.com>");
        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";

        Files.write(repoDir.resolve("gpg-key-authorization-0.yml.asc"),
            signInline(untrustedKey, buildGpgAuthYaml(repoDir.toAbsolutePath().toString(), pastDate(), futureDate(), List.of(spolSignerKeyArmored))));
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256(spolJson), repoDir.toAbsolutePath().toString())));
        commitAll("untrusted auth signer");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }


    @Test
    void when_spolSignatureRepoDoesNotMatchUpstreamUrl_nothingIsUploaded() throws Exception {
        addValidAuthFile();

        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, sha256(spolJson), "https://wrong-repo.com/policies.git")));
        commitAll("spol wrong repo");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_spolSignatureHashDoesNotMatchFileContents_nothingIsUploaded() throws Exception {
        addValidAuthFile();

        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";
        String wrongHash = "0000000000000000000000000000000000000000000000000000000000000000";
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml(spolVersionedFilename, wrongHash, repoDir.toAbsolutePath().toString())));
        commitAll("spol wrong hash");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_spolSignatureFilenameDoesNotMatchActualFilename_nothingIsUploaded() throws Exception {
        addValidAuthFile();

        byte[] spolJson = buildSpolJson("test-session");
        String spolVersionedFilename = "0-osv-policy.json";
        Files.write(repoDir.resolve(spolVersionedFilename), spolJson);
        Files.write(repoDir.resolve(spolVersionedFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml("0-other-name.json", sha256(spolJson), repoDir.toAbsolutePath().toString())));
        commitAll("spol filename mismatch");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_spolHasNoAscSignatureFile_spolIsSkipped() throws Exception {
        addValidAuthFile();

        Files.write(repoDir.resolve("0-osv-policy.json"), buildSpolJson("test-session"));
        commitAll("spol without signature");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(0, casClient.uploaded.size());
    }


    @Test
    void when_multipleSpolVersionsExist_onlyHighestVersionIsUploaded() throws Exception {
        addValidAuthFile();

        String spolFilename = "osv-policy.json";

        byte[] spol0Json = buildSpolJson("session-v0");
        Files.write(repoDir.resolve("0-" + spolFilename), spol0Json);
        Files.write(repoDir.resolve("0-" + spolFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml("0-" + spolFilename, sha256(spol0Json), repoDir.toAbsolutePath().toString())));

        byte[] spol2Json = buildSpolJson("session-v2");
        Files.write(repoDir.resolve("2-" + spolFilename), spol2Json);
        Files.write(repoDir.resolve("2-" + spolFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml("2-" + spolFilename, sha256(spol2Json), repoDir.toAbsolutePath().toString())));

        byte[] spol1Json = buildSpolJson("session-v1");
        Files.write(repoDir.resolve("1-" + spolFilename), spol1Json);
        Files.write(repoDir.resolve("1-" + spolFilename + ".asc"),
            signInline(spolSignerKey, buildSpolSignedYaml("1-" + spolFilename, sha256(spol1Json), repoDir.toAbsolutePath().toString())));

        commitAll("multiple spol versions");

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(buildUpstream(), casClient);

        assertEquals(PolicyUpdateRunStatus.SUCCESSFUL, result.overallStatus());
        assertEquals(1, casClient.uploaded.size());
        assertTrue(casClient.uploaded.getFirst().getSession().contains("session-v2"));
    }


    private PolicyUpstreamSpec buildUpstream() {
        PolicyUpstreamSpec spec = new PolicyUpstreamSpec();
        spec.setGitUrl(repoDir.toAbsolutePath().toString());
        spec.setBranch(GIT_BRANCH);
        spec.setGpgKeys(new ArrayList<>(List.of(rootKeyArmored)));
        return spec;
    }

    private void addValidAuthFile() throws Exception {
        Files.write(repoDir.resolve("gpg-key-authorization-0.yml.asc"),
            signInline(rootKey, buildGpgAuthYaml(repoDir.toAbsolutePath().toString(), pastDate(), futureDate(), List.of(spolSignerKeyArmored))));
        commitAll("add auth file");
    }

    private void commitAll(String message) throws Exception {
        git.add().addFilepattern(".").call();
        git.commit().setSign(false).setMessage(message).call();
    }


    public class MockCASClient implements CASClient {
        public final List<SPOLDefinition> uploaded = new ArrayList<>();

        @Override
        public ReadSessionResponse readSession(String name) throws CASClientException {
            throw new CASClientException(404, "not found");
        }

        @Override
        public UploadSessionResponse uploadSession(SPOLDefinition body) {
            uploaded.add(body);
            UploadSessionResponse resp = new UploadSessionResponse();
            resp.setHash("fake-hash");
            return resp;
        }
    }
}

