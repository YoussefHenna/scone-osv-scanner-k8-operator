package com.youssefhenna.policy_manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.youssefhenna.policy_manager.model.FileWithSignature;
import com.youssefhenna.policy_manager.model.SPOLDefinition;
import com.youssefhenna.policy_manager.model.SessionContents;
import com.youssefhenna.policy_manager.model.http.ReadSessionResponse;
import com.youssefhenna.status.PolicyUpdateState;
import com.youssefhenna.status.PolicyUploadStatusItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


public class SPOLUpload {

    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    public static ArrayList<PolicyUploadStatusItem> uploadAll(CASClient casClient, ArrayList<FileWithSignature> spols) {
        ArrayList<PolicyUploadStatusItem> statuses = new ArrayList<>();

        // Runs several impure methods that can affect status, aborts early once last state set by one of these methods
        for (FileWithSignature spol : spols) {
            if (spol.filePath() == null) {
                continue;
            }

            PolicyUploadStatusItem statusItem = new PolicyUploadStatusItem(spol.baseName(), spol.filePath().toString());
            statuses.add(statusItem);

            SPOLDefinition spolDefinition = parseSPOLFile(spol, statusItem);
            if (spolDefinition == null || statusItem.getLastState() != null) continue;

            String casSessionContents = readCASSessionContents(casClient, spolDefinition, statusItem);
            if (statusItem.getLastState() != null) continue;

            boolean sessionsEqual = areSessionsEqual(spolDefinition.getSession(), casSessionContents, statusItem);
            if (sessionsEqual || statusItem.getLastState() != null) continue;

            uploadSession(casClient, spolDefinition, statusItem);
        }

        return statuses;
    }

    private static SPOLDefinition parseSPOLFile(FileWithSignature spol, PolicyUploadStatusItem statusItem) {
        try {
            SPOLDefinition spolDefinition = jsonMapper.readValue(spol.filePath().toFile(), SPOLDefinition.class);
            SessionContents parsedSessionContents = yamlMapper.readValue(spolDefinition.getSession(), SessionContents.class);
            statusItem.setName(parsedSessionContents.getName());
            return spolDefinition;
        } catch (IOException e) {
            statusItem.setLastState(PolicyUpdateState.FAILED_INVALID_SPOL);
            return null;
        }
    }


    private static String readCASSessionContents(CASClient casClient, SPOLDefinition spolDefinition, PolicyUploadStatusItem statusItem) {
        try {
            SessionContents parsedSessionContents = yamlMapper.readValue(spolDefinition.getSession(), SessionContents.class);
            ReadSessionResponse response = casClient.readSession(parsedSessionContents.getName());
            return response.getSession();
        } catch (CASClient.CASClientException e) {
            if (e.getStatusCode() == 404) {
                return null;
            } else if (e.getStatusCode() == 403) {
                statusItem.setLastState(PolicyUpdateState.FAILED_READ_EXISTING_FORBIDDEN);
            } else if (e.getStatusCode() >= 500) {
                statusItem.setLastState(PolicyUpdateState.FAILED_UNKNOWN_CAS_ERROR);
            } else {
                statusItem.setLastState(PolicyUpdateState.FAILED_UNKNOWN_READ_EXISTING_FAILURE);
            }
            return null;
        } catch (Exception e) {
            statusItem.setLastState(PolicyUpdateState.FAILED_UNKNOWN_READ_EXISTING_FAILURE);
            return null;
        }
    }

    private static boolean areSessionsEqual(String sessionContents, String casSessionContents, PolicyUploadStatusItem statusItem) {
        if (casSessionContents == null || sessionContents == null) {
            return false;
        }
        try {
            if (yamlDeepEquals(sessionContents, casSessionContents, Set.of("creator"))) {
                statusItem.setLastState(PolicyUpdateState.SKIPPED_CAS_ALREADY_UPTODATE);
                return true;
            }
            return false;
        } catch (IOException e) {
            statusItem.setLastState(PolicyUpdateState.FAILED_SESSION_COMPARISON_FAILURE);
            return false;
        }
    }

    private static boolean yamlDeepEquals(String a, String b, Set<String> ignoreKeys) throws IOException {
        if (a == null || b == null) {
            return false;
        }
        TypeReference<Map<String, Object>> mapType = new TypeReference<>() {};
        Map<String, Object> mapA = yamlMapper.readValue(a, mapType);
        Map<String, Object> mapB = yamlMapper.readValue(b, mapType);
        ignoreKeys.forEach(mapA::remove);
        ignoreKeys.forEach(mapB::remove);
        return mapA.equals(mapB);
    }

    private static void uploadSession(CASClient casClient, SPOLDefinition spolDefinition, PolicyUploadStatusItem statusItem) {
        try {
            casClient.uploadSession(spolDefinition);
            statusItem.setLastState(PolicyUpdateState.SUCCESS_UPLOADED);
        } catch (CASClient.CASClientException e) {
            if (e.getStatusCode() == 404) {
                statusItem.setLastState(PolicyUpdateState.FAILED_NAMESPACE_NOT_FOUND);
            } else if (e.getStatusCode() == 403) {
                statusItem.setLastState(PolicyUpdateState.FAILED_SIGNER_NOT_AUTHORIZED);
            } else if (e.getStatusCode() == 400) {
                statusItem.setLastState(PolicyUpdateState.FAILED_INVALID_SPOL);
            } else if (e.getStatusCode() == 409) {
                statusItem.setLastState(PolicyUpdateState.FAILED_PREDECESSOR_CONFLICT);
            } else if (e.getStatusCode() >= 500) {
                statusItem.setLastState(PolicyUpdateState.FAILED_UNKNOWN_CAS_ERROR);
            } else {
                statusItem.setLastState(PolicyUpdateState.FAILED_UNKNOWN_UPLOAD_SESSION_FAILURE);
            }
        } catch (Exception e) {
            statusItem.setLastState(PolicyUpdateState.FAILED_UNKNOWN_UPLOAD_SESSION_FAILURE);
        }
    }


}