package com.youssefhenna.policy.cas;

import com.youssefhenna.policy.model.SPOLDefinition;
import com.youssefhenna.policy.model.http.ReadSessionResponse;
import com.youssefhenna.policy.model.http.UploadSessionResponse;

import java.io.IOException;

public interface CASClient {

    ReadSessionResponse readSession(String name) throws IOException, InterruptedException, CASClientException;

    UploadSessionResponse uploadSession(SPOLDefinition body) throws IOException, InterruptedException, CASClientException;

    class CASClientException extends Exception {
        private final int statusCode;

        public CASClientException(int statusCode, String body) {
            super("CAS request failed with status " + statusCode + ": " + body);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
