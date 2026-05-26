package com.youssefhenna.integration;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.dependent.kyverno.NamespacedImageValidatingPolicy;
import com.youssefhenna.integration.utils.IntegrationTestCrBuilder;
import com.youssefhenna.integration.utils.IntegrationTestProfile;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.youssefhenna.integration.utils.IntegrationTestCrBuilder.NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
class DependentResourceCreationTest {

    @Inject
    KubernetesClient client;

    @Test
    void when_crCreated_allCoreResourcesExist() {
        SconeOsvScanner cr = new IntegrationTestCrBuilder().build();
        try {
            client.resource(cr).create();
            await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
                assertDeploymentExists(Constants.getDbManagerDeploymentName(IntegrationTestCrBuilder.CR_NAME));
                assertDeploymentExists(Constants.getFrontAppDeploymentName(IntegrationTestCrBuilder.CR_NAME));
                assertDeploymentExists(Constants.getMaxscaleDeploymentName(IntegrationTestCrBuilder.CR_NAME));

                assertServiceExists(Constants.getFrontAppServiceName(IntegrationTestCrBuilder.CR_NAME));
                assertServiceExists(Constants.getMaxscaleServiceName(IntegrationTestCrBuilder.CR_NAME));
                assertServiceExists(Constants.getMaxscaleAdminServiceName(IntegrationTestCrBuilder.CR_NAME));
                assertServiceExists(Constants.getMariadbPrimaryServiceName(IntegrationTestCrBuilder.CR_NAME));
                assertServiceExists(Constants.getMariadbReplicaServiceName(IntegrationTestCrBuilder.CR_NAME));

                assertStatefulSetExists(Constants.getMariadbPrimaryName(IntegrationTestCrBuilder.CR_NAME));
                assertStatefulSetExists(Constants.getMariadbReplicaName(IntegrationTestCrBuilder.CR_NAME));

                assertConfigMapExists(Constants.getMariadbInitScriptsConfigMapName(IntegrationTestCrBuilder.CR_NAME));

                assertPdbExists(Constants.getMariadbPrimaryName(IntegrationTestCrBuilder.CR_NAME) + "-pdb");
                assertPdbExists(Constants.getMariadbReplicaName(IntegrationTestCrBuilder.CR_NAME) + "-pdb");
            });
        } finally {
            client.resource(cr).delete();
        }
    }

    @Test
    void when_cosignKeyDefined_imageValidatingPoliciesCreated() {
        SconeOsvScanner cr = new IntegrationTestCrBuilder().withCosignKey().build();
        try {
            client.resource(cr).create();
            await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
                assertImageValidatingPolicyExists(Constants.getDbManagerImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
                assertImageValidatingPolicyExists(Constants.getFrontAppImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
                assertImageValidatingPolicyExists(Constants.getMaxscaleImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
                assertImageValidatingPolicyExists(Constants.getMariadbPrimaryImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
                assertImageValidatingPolicyExists(Constants.getMariadbReplicaImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
            });
        } finally {
            client.resource(cr).delete();
        }
    }

    @Test
    void when_noCosignKeyDefined_imageValidatingPoliciesNotCreated() {
        SconeOsvScanner cr = new IntegrationTestCrBuilder().build();
        try {
            client.resource(cr).create();
            // wait for reconciliation to have run (proven by a core resource existing)
            await().atMost(60, TimeUnit.SECONDS).untilAsserted(() ->
                assertDeploymentExists(Constants.getDbManagerDeploymentName(IntegrationTestCrBuilder.CR_NAME))
            );
            assertImageValidatingPolicyAbsent(Constants.getDbManagerImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
            assertImageValidatingPolicyAbsent(Constants.getFrontAppImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
            assertImageValidatingPolicyAbsent(Constants.getMaxscaleImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
            assertImageValidatingPolicyAbsent(Constants.getMariadbPrimaryImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
            assertImageValidatingPolicyAbsent(Constants.getMariadbReplicaImageValidatingPolicyName(IntegrationTestCrBuilder.CR_NAME));
        } finally {
            client.resource(cr).delete();
        }
    }

    private void assertDeploymentExists(String name) {
        assertNotNull(
            client.apps().deployments().inNamespace(NAMESPACE).withName(name).get(),
            "Deployment not found: " + name
        );
    }

    private void assertServiceExists(String name) {
        assertNotNull(
            client.services().inNamespace(NAMESPACE).withName(name).get(),
            "Service not found: " + name
        );
    }

    private void assertStatefulSetExists(String name) {
        assertNotNull(
            client.apps().statefulSets().inNamespace(NAMESPACE).withName(name).get(),
            "StatefulSet not found: " + name
        );
    }

    private void assertConfigMapExists(String name) {
        assertNotNull(
            client.configMaps().inNamespace(NAMESPACE).withName(name).get(),
            "ConfigMap not found: " + name
        );
    }

    private void assertPdbExists(String name) {
        assertNotNull(
            client.policy().v1().podDisruptionBudget().inNamespace(NAMESPACE).withName(name).get(),
            "PodDisruptionBudget not found: " + name
        );
    }

    private void assertImageValidatingPolicyExists(String name) {
        assertNotNull(
            client.resources(NamespacedImageValidatingPolicy.class).inNamespace(NAMESPACE).withName(name).get(),
            "ImageValidatingPolicy not found: " + name
        );
    }

    private void assertImageValidatingPolicyAbsent(String name) {
        assertNull(
            client.resources(NamespacedImageValidatingPolicy.class).inNamespace(NAMESPACE).withName(name).get(),
            "ImageValidatingPolicy should not exist: " + name
        );
    }
}