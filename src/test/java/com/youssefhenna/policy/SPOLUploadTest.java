package com.youssefhenna.policy;

import com.youssefhenna.policy.cas.CASClient;
import com.youssefhenna.policy.model.FileWithSignature;
import com.youssefhenna.policy.model.SPOLDefinition;
import com.youssefhenna.policy.model.http.ReadSessionResponse;
import com.youssefhenna.policy.model.http.UploadSessionResponse;
import com.youssefhenna.status.PolicyUpdateState;
import com.youssefhenna.status.PolicyUploadStatusItem;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.youssefhenna.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SPOLUploadTest {

    private Path workDir;
    private StubCASClient casClient;

    @BeforeEach
    void setUp() throws Exception {
        workDir = Files.createTempDirectory("spol-upload-test");
        casClient = new StubCASClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteDirectory(workDir);
    }


    @Test
    void when_casReturns404ForSession_sessionIsUploaded() throws Exception {
        casClient.readException = new CASClient.CASClientException(404, "not found");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(1, statuses.size());
        assertEquals(PolicyUpdateState.SUCCESS_UPLOADED, statuses.getFirst().getLastState());
        assertEquals(1, casClient.uploaded.size());
    }

    @Test
    void when_casSessionMatchesSpolContents_uploadIsSkipped() throws Exception {
        String sessionYaml = "name: session-a\nversion: \"0.3\"\n";
        casClient.readException = null;
        casClient.readSessionContent = sessionYaml;

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(1, statuses.size());
        assertEquals(PolicyUpdateState.SKIPPED_CAS_ALREADY_UPTODATE, statuses.getFirst().getLastState());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_casSessionDiffersFromSpolContents_sessionIsUploaded() throws Exception {
        casClient.readException = null;
        casClient.readSessionContent = "name: session-a\nversion: \"0.2\"\n";

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(1, statuses.size());
        assertEquals(PolicyUpdateState.SUCCESS_UPLOADED, statuses.getFirst().getLastState());
        assertEquals(1, casClient.uploaded.size());
    }

    @Test
    void when_casSessionMatchesExceptCreatorField_uploadIsSkipped() throws Exception {
        casClient.readException = null;
        casClient.readSessionContent = "name: session-a\nversion: \"0.3\"\ncreator: some-other-party\n";

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(1, statuses.size());
        assertEquals(PolicyUpdateState.SKIPPED_CAS_ALREADY_UPTODATE, statuses.getFirst().getLastState());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_casReadReturns403_statusIsReadForbidden() throws Exception {
        casClient.readException = new CASClient.CASClientException(403, "forbidden");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_READ_EXISTING_FORBIDDEN, statuses.getFirst().getLastState());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_casReadReturns500_statusIsCasError() throws Exception {
        casClient.readException = new CASClient.CASClientException(500, "server error");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_UNKNOWN_CAS_ERROR, statuses.getFirst().getLastState());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_casReadReturnsUnrecognizedError_statusIsUnknownReadFailure() throws Exception {
        casClient.readException = new CASClient.CASClientException(418, "teapot");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_UNKNOWN_READ_EXISTING_FAILURE, statuses.getFirst().getLastState());
        assertEquals(0, casClient.uploaded.size());
    }


    @Test
    void when_casUploadReturns403_statusIsSignerNotAuthorized() throws Exception {
        casClient.uploadException = new CASClient.CASClientException(403, "forbidden");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_SIGNER_NOT_AUTHORIZED, statuses.getFirst().getLastState());
    }

    @Test
    void when_casUploadReturns404_statusIsNamespaceNotFound() throws Exception {
        casClient.uploadException = new CASClient.CASClientException(404, "not found");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_NAMESPACE_NOT_FOUND, statuses.getFirst().getLastState());
    }

    @Test
    void when_casUploadReturns400_statusIsInvalidSpol() throws Exception {
        casClient.uploadException = new CASClient.CASClientException(400, "bad request");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_INVALID_SPOL, statuses.getFirst().getLastState());
    }

    @Test
    void when_casUploadReturns409_statusIsPredecessorConflict() throws Exception {
        casClient.uploadException = new CASClient.CASClientException(409, "conflict");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_PREDECESSOR_CONFLICT, statuses.getFirst().getLastState());
    }

    @Test
    void when_casUploadReturns500_statusIsCasError() throws Exception {
        casClient.uploadException = new CASClient.CASClientException(500, "server error");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_UNKNOWN_CAS_ERROR, statuses.getFirst().getLastState());
    }

    @Test
    void when_casUploadReturnsUnrecognizedError_statusIsUnknownUploadFailure() throws Exception {
        casClient.uploadException = new CASClient.CASClientException(418, "teapot");

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a"));

        assertEquals(PolicyUpdateState.FAILED_UNKNOWN_UPLOAD_SESSION_FAILURE, statuses.getFirst().getLastState());
    }

    @Test
    void when_spolFileContainsInvalidJson_statusIsInvalidSpol() throws Exception {
        Path invalidFile = workDir.resolve("bad.json");
        Files.writeString(invalidFile, "not-valid-json{{{");
        FileWithSignature spol = new FileWithSignature("bad.json", invalidFile, workDir.resolve("bad.json.asc"));

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, new ArrayList<>(List.of(spol)));

        assertEquals(PolicyUpdateState.FAILED_INVALID_SPOL, statuses.getFirst().getLastState());
        assertEquals(0, casClient.uploaded.size());
    }

    @Test
    void when_fileWithNullPath_entryIsSkipped() throws Exception {
        FileWithSignature nullPathSpol = new FileWithSignature("no-file.json", null, workDir.resolve("no-file.json.asc"));

        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, new ArrayList<>(List.of(nullPathSpol)));

        assertEquals(0, statuses.size());
    }

    @Test
    void when_multipleSpolsProvided_eachIsProcessedIndependently() throws Exception {
        List<PolicyUploadStatusItem> statuses = SPOLUpload.uploadAll(casClient, spols("session-a", "session-b", "session-c"));

        assertEquals(3, statuses.size());
        assertEquals(3, casClient.uploaded.size());
        assertTrue(statuses.stream().allMatch(s -> s.getLastState() == PolicyUpdateState.SUCCESS_UPLOADED));
    }


    private ArrayList<FileWithSignature> spols(String... sessionNames) throws Exception {
        ArrayList<FileWithSignature> result = new ArrayList<>();
        for (String name : sessionNames) {
            String filename = name + ".json";
            Path filePath = workDir.resolve(filename);
            Files.write(filePath, buildSpolJson(name));
            result.add(new FileWithSignature(filename, filePath, workDir.resolve(filename + ".asc")));
        }
        return result;
    }

    /**
     * Configurable CASClient stub for SPOLUpload tests.
     * Set readException / uploadException to simulate error responses,
     * or set readSessionContent to simulate an existing CAS session.
     * Defaults to 404 on readSession (new session) and success on uploadSession.
     */
    public class StubCASClient implements CASClient {
        public final List<SPOLDefinition> uploaded = new ArrayList<>();

        public CASClientException readException = new CASClientException(404, "not found");
        public String readSessionContent = null;
        public CASClientException uploadException = null;

        @Override
        public ReadSessionResponse readSession(String name) throws CASClientException {
            if (readException != null) throw readException;
            ReadSessionResponse resp = new ReadSessionResponse();
            resp.setSession(readSessionContent);
            return resp;
        }

        @Override
        public UploadSessionResponse uploadSession(SPOLDefinition body) throws CASClientException {
            if (uploadException != null) throw uploadException;
            uploaded.add(body);
            UploadSessionResponse resp = new UploadSessionResponse();
            resp.setHash("fake-hash");
            return resp;
        }
    }
}
