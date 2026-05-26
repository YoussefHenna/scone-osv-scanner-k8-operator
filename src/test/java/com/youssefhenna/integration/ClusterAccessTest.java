package com.youssefhenna.integration;

import com.youssefhenna.integration.utils.IntegrationTestProfile;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
class ClusterAccessTest {

    @Inject
    KubernetesClient client;

    // smoke test to make sure test cluster is setup properly
    @Test
    void clusterIsReachable() {
        var namespaceList = client.namespaces().list();
        assertNotNull(namespaceList);
        assertFalse(namespaceList.getItems().isEmpty());
    }
}
