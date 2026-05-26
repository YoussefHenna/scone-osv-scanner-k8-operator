package com.youssefhenna.integration;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.integration.utils.IntegrationTestCrBuilder;
import com.youssefhenna.integration.utils.IntegrationTestProfile;
import com.youssefhenna.integration.utils.TestRegistryImageVersionReaderFactory;
import com.youssefhenna.model.PollConfig;
import com.youssefhenna.status.SconeOsvScannerStatus;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.youssefhenna.integration.utils.IntegrationTestCrBuilder.CR_NAME;
import static com.youssefhenna.integration.utils.IntegrationTestCrBuilder.NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
class AutoUpdateTest {

    private static final int POLL_SECONDS = 30;
    private static final String UPDATED_VERSION = "2.0.0";
    private static final String INITIAL_VERSION = "1.0.0";

    @Inject
    KubernetesClient client;

    @Inject
    TestRegistryImageVersionReaderFactory readerFactory;

    @AfterEach
    void resetFactory() {
        readerFactory.reset();
    }

    @Test
    void when_pollElapses_autoUpdateIsReRun() {
        readerFactory.setReader((spec, imageName) -> List.of(INITIAL_VERSION));

        SconeOsvScanner cr = new IntegrationTestCrBuilder()
            .withDbManagerAutoUpdate()
            .withAutoUpdatePoll(POLL_SECONDS, PollConfig.Unit.SECONDS)
            .build();
        try {
            client.resource(cr).create();

            await().atMost(60, TimeUnit.SECONDS).until(() -> {
                SconeOsvScannerStatus status = getStatus(cr);
                return status != null && status.getLastAutoUpdateCheckTime() != null;
            });
            String firstCheckTime = getStatus(cr).getLastAutoUpdateCheckTime();

            // wait for a second check to occur
            await().atMost(90, TimeUnit.SECONDS).until(() -> {
                SconeOsvScannerStatus status = getStatus(cr);
                return status != null && !firstCheckTime.equals(status.getLastAutoUpdateCheckTime());
            });
        } finally {
            client.resource(cr).delete();
        }
    }

    @Test
    void when_newVersionAvailable_deploymentImageAndStatusTargetVersionUpdated() {
        readerFactory.setReader((spec, imageName) -> List.of(UPDATED_VERSION, INITIAL_VERSION));

        SconeOsvScanner cr = new IntegrationTestCrBuilder()
            .withDbManagerAutoUpdate()
            .withAutoUpdatePoll(POLL_SECONDS, PollConfig.Unit.SECONDS)
            .build();
        try {
            client.resource(cr).create();

            String expectedImage = "registry-1.docker.io/youssefhenna/sos-dbmanager:" + UPDATED_VERSION;
            String dbManagerDeploymentName = Constants.getDbManagerDeploymentName(CR_NAME);

            await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
                Deployment deployment = client.apps().deployments()
                    .inNamespace(NAMESPACE)
                    .withName(dbManagerDeploymentName)
                    .get();
                assertNotNull(deployment, "Deployment not found: " + dbManagerDeploymentName);
                assertEquals(
                    expectedImage,
                    deployment.getSpec().getTemplate().getSpec().getContainers().getFirst().getImage(),
                    "Deployment image was not updated"
                );
            });

            await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
                SconeOsvScanner updated = client.resource(cr).get();
                assertNotNull(updated.getStatus(), "Status not yet set");
                assertNotNull(updated.getStatus().getDbManagerStatus(), "DbManager status not yet set");
                assertEquals(UPDATED_VERSION, updated.getStatus().getDbManagerStatus().getTargetVersion(),
                    "Status targetVersion was not updated");
            });
        } finally {
            client.resource(cr).delete();
        }
    }

    private SconeOsvScannerStatus getStatus(SconeOsvScanner cr) {
        SconeOsvScanner updated = client.resource(cr).get();
        return updated != null ? updated.getStatus() : null;
    }
}