package com.youssefhenna.utils;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.status.PolicyUploadStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
            new EnvVarBuilder().withName("SCONE_ALLOW_DLOPEN").withValue("2").build(),
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

    public static Map<String, String> getPolicyHashAnnotation(SconeOsvScanner primary, String sconeConfigId) {
        if (primary.getStatus() == null) return null;
        PolicyUploadStatus uploadStatus = primary.getStatus().getPolicyUploadStatus();
        if (uploadStatus == null) return null;
        String hash = uploadStatus.getHashForConfigId(sconeConfigId);
        return hash != null ? Map.of(Constants.POLICY_HASH_ANNOTATION, hash) : null;
    }

    public static Deployment buildDeployment(String name, String namespace, String imagePullSecretName, Container container, int replicas, Map<String, String> podAnnotations) {
        return buildDeployment(name, namespace, imagePullSecretName, container, replicas, List.of(), podAnnotations);
    }

    public static String getImagePullSecretName(RegistryCredentials credentials) {
        if (credentials == null || credentials.getSecretRef() == null) return null;
        return credentials.getSecretRef().getName();
    }

    public static Deployment buildDeployment(String name, String namespace, String imagePullSecretName, Container container, int replicas, List<Volume> volumes, Map<String, String> podAnnotations) {
        ObjectMetaBuilder podMetaBuilder = new ObjectMetaBuilder().addToLabels("app", name);
        if (podAnnotations != null) {
            podMetaBuilder.addToAnnotations(podAnnotations);
        }

        List<LocalObjectReference> imagePullSecrets = imagePullSecretName != null
            ? List.of(new LocalObjectReferenceBuilder().withName(imagePullSecretName).build())
            : List.of();

        PodTemplateSpec podTemplate = new PodTemplateSpecBuilder()
            .withMetadata(podMetaBuilder.build())
            .withNewSpec()
            .withImagePullSecrets(imagePullSecrets)
            .withContainers(container)
            .withVolumes(volumes)
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


    public static String extractImageVersion(String image) {
        if (image == null) return null;
        int colon = image.lastIndexOf(':');
        return colon >= 0 ? image.substring(colon + 1) : null;
    }

    public static Date parseDate(String dateString) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(dateString);
    }

    public static String dateToString(Date date)  {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}