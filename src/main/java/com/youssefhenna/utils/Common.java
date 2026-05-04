package com.youssefhenna.utils;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Common {

    public static List<EnvVar> buildSconeEnvVars(String heap, String casAddress, String configId) {
        List<EnvVar> envVars = new ArrayList<>();

        Collections.addAll(envVars,
            new EnvVarBuilder().withName("SCONE_VERSION").withValue("1").build(),
            new EnvVarBuilder().withName("SCONE_HEAP").withValue(heap).build(),
            new EnvVarBuilder().withName("SCONE_CAS_ADDR").withValue(casAddress).build(),
            new EnvVarBuilder().withName("SCONE_CONFIG_ID").withValue(configId).build(),
            new EnvVarBuilder().withName("BASE_SCONE_CONFIG_ID").withValue(configId).build(),
            new EnvVarBuilder()
                .withName("SCONE_LAS_ADDR")
                .withValueFrom(new EnvVarSourceBuilder()
                    .withNewFieldRef().withFieldPath("status.hostIP").endFieldRef()
                    .build())
                .build(),
            new EnvVarBuilder().withName("SCONE_ETHREAD_SLEEP_TIME_MSEC").withValue("5ms").build(),
            new EnvVarBuilder().withName("SCONE_ALLOW_DLOPEN").withValue("1").build(),
            new EnvVarBuilder().withName("SCONE_MODE").withValue("hw").build(),
            new EnvVarBuilder().withName("SCONE_SYSLIBS").withValue("1").build(),
            new EnvVarBuilder().withName("SCONE_LOG").withValue("error").build()
        );


        return envVars;
    }

    public static String buildImage(String registryUrl, String imageName, String imageVersion) {
        return registryUrl + "/" + imageName + ":" + imageVersion;
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

    public static Deployment buildDeployment(String name, String namespace, String imagePullSecretName, Container container, int replicas) {
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
            .withReplicas(replicas)
            .withSelector(selector)
            .withTemplate(podTemplate)
            .build();

        return new DeploymentBuilder()
            .withMetadata(metadata)
            .withSpec(deploymentSpec)
            .build();
    }
}