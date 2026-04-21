package com.youssefhenna.utils;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeploymentCommon {

    public static List<EnvVar> buildSconeEnvVars(String heap, String casAddress, String configId, String version) {
        List<EnvVar> envVars = new ArrayList<>();
        if (version != null) {
            envVars.add(new EnvVarBuilder().withName("SCONE_VERSION").withValue(version).build());
        }
        envVars.add(new EnvVarBuilder().withName("SCONE_HEAP").withValue(heap).build());
        envVars.add(new EnvVarBuilder().withName("SCONE_CAS_ADDR").withValue(casAddress).build());
        envVars.add(new EnvVarBuilder().withName("SCONE_CONFIG_ID").withValue(configId).build());
        envVars.add(new EnvVarBuilder()
            .withName("SCONE_LAS_ADDR")
            .withValueFrom(new EnvVarSourceBuilder()
                .withNewFieldRef().withFieldPath("status.hostIP").endFieldRef()
                .build())
            .build());
        return envVars;
    }

    public static ResourceRequirements buildSgxResources(String memory) {
        return new ResourceRequirementsBuilder()
            .withLimits(Map.of("sgx.intel.com/enclave", new Quantity("1")))
            .withRequests(Map.of(
                "memory", new Quantity(memory),
                "sgx.intel.com/enclave", new Quantity("1")
            ))
            .build();
    }

    public static Deployment buildDeployment(String name, String namespace, String imagePullSecretName, Container container) {
        LocalObjectReference imagePullSecret = new LocalObjectReferenceBuilder()
            .withName(imagePullSecretName)
            .build();

        PodTemplateSpec podTemplate = new PodTemplateSpecBuilder()
            .withNewMetadata()
            .addToLabels("app", name)
            .endMetadata()
            .withNewSpec()
            .withImagePullSecrets(imagePullSecret)
            .withContainers(container)
            .endSpec()
            .build();

        ObjectMeta metadata = new ObjectMetaBuilder()
            .withName(name)
            .withNamespace(namespace)
            .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
            .build();

        LabelSelector selector = new LabelSelectorBuilder()
            .addToMatchLabels("app", name)
            .build();

        DeploymentSpec deploymentSpec = new DeploymentSpecBuilder()
            .withReplicas(1)
            .withSelector(selector)
            .withTemplate(podTemplate)
            .build();

        return new DeploymentBuilder()
            .withMetadata(metadata)
            .withSpec(deploymentSpec)
            .build();
    }
}