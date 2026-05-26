package com.youssefhenna.integration.utils;

import com.youssefhenna.policy.cas.CASClient;
import com.youssefhenna.policy.cas.CASClientFactory;
import com.youssefhenna.policy.model.SPOLDefinition;
import com.youssefhenna.policy.model.http.ReadSessionResponse;
import com.youssefhenna.policy.model.http.UploadSessionResponse;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestCASClientFactory implements CASClientFactory {

    private volatile CASClient client = new NoOpCASClient();

    public void setClient(CASClient client) {
        this.client = client;
    }

    public void reset() {
        this.client = new NoOpCASClient();
    }

    @Override
    public CASClient create(String casAddress, int casPort) {
        return client;
    }

    private static class NoOpCASClient implements CASClient {
        @Override
        public ReadSessionResponse readSession(String name) throws CASClientException {
            throw new CASClientException(404, "not found");
        }

        @Override
        public UploadSessionResponse uploadSession(SPOLDefinition body) {
            UploadSessionResponse resp = new UploadSessionResponse();
            resp.setHash("noop-hash");
            return resp;
        }
    }
}