package com.youssefhenna.integration.utils;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;
import java.util.Map;

public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.operator-sdk.start-operator", "true",
            "quarkus.operator-sdk.crd.apply", "true",
            // prevent quarkus from starting its own testing cluster, already setup through k3 test resource
            "quarkus.kubernetes-client.devservices.enabled", "false"
        );
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(K3sTestResource.class));
    }
}