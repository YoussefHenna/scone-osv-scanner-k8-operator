package com.youssefhenna.dependent.database.mariadb;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.spec.database.MariadbSpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractMariadbStatefulSetDependentResource extends CRUDKubernetesDependentResource<StatefulSet, SconeOsvScanner> {

    private static final String DATADIR_VOLUME = "mariadb-datadir";
    private static final String INIT_SCRIPTS_VOLUME = "custom-init-scripts";

    protected AbstractMariadbStatefulSetDependentResource() {
        super(StatefulSet.class);
    }

    protected abstract int getReplicas(DatabaseSpec dbSpec);
    protected abstract String getConfigId(MariadbSpec spec);
    protected abstract String getStatefulSetName(String primaryName);
    protected abstract String getEntrypointScript();
    protected abstract String getPortName();
    protected abstract int getPort();
    protected abstract List<Container> getInitContainers(String image, List<EnvVar> envVars, ResourceRequirements resources);

    @Override
    protected StatefulSet desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        DatabaseSpec dbSpec = primarySpec.getDatabase();
        MariadbSpec spec = dbSpec.getMariadb();

        String primaryName = primary.getMetadata().getName();
        String name = getStatefulSetName(primaryName);
        String namespace = primary.getMetadata().getNamespace();
        String configMapName = Constants.getMariadbInitScriptsConfigMapName(primaryName);

        String image = Common.buildImage(dbSpec.getRegistryUrl(), spec.getImageName(), spec.getImageVersion());
        String imagePullSecretName = dbSpec.getRegistryCredentials().getSecretRef().getName();

        String memory = spec.getMemory();
        List<EnvVar> envVars = Common.buildSconeEnvVars(memory, primarySpec.getCasAddress(), getConfigId(spec));
        ResourceRequirements resources = Common.buildSgxResources(memory);

        Probe probe = new ProbeBuilder()
            .withTimeoutSeconds(30)
            .withPeriodSeconds(60)
            .withNewExec()
            .withCommand("/bin/bash", "-ec",
                "export SCONE_HEAP=32M\nexport SCONE_CONFIG_ID=\"$BASE_SCONE_CONFIG_ID/probe\"\nmysqladmin")
            .endExec()
            .build();

        Container container = new ContainerBuilder()
            .withName(name + "-container")
            .withImage(image)
            .withImagePullPolicy("Always")
            .withCommand("bash", "-c", getEntrypointScript())
            .withEnv(envVars)
            .withResources(resources)
            .withSecurityContext(new SecurityContextBuilder().withRunAsUser(0L).build())
            .withPorts(new ContainerPortBuilder().withName(getPortName()).withContainerPort(getPort()).withProtocol("TCP").build())
            .withReadinessProbe(probe)
            .withStartupProbe(new ProbeBuilder(probe).withInitialDelaySeconds(10).withFailureThreshold(10).build())
            .withLivenessProbe(new ProbeBuilder(probe).withFailureThreshold(3).build())
            .withVolumeMounts(
                new VolumeMountBuilder().withName(DATADIR_VOLUME).withMountPath("/var/lib/mysql").build(),
                new VolumeMountBuilder().withName(INIT_SCRIPTS_VOLUME).withMountPath("/start").build()
            )
            .build();

        List<Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeBuilder()
            .withName(INIT_SCRIPTS_VOLUME)
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(configMapName)
                .withDefaultMode(0511)
                .build())
            .build());

        StatefulSetSpecBuilder statefulSetSpecBuilder = new StatefulSetSpecBuilder()
            .withReplicas(getReplicas(dbSpec))
            .withServiceName(name)
            .withSelector(new LabelSelectorBuilder().addToMatchLabels("app", name).build());

        if (spec.isDisablePersistence()) {
            volumes.add(new VolumeBuilder()
                .withName(DATADIR_VOLUME)
                .withEmptyDir(new EmptyDirVolumeSourceBuilder().build())
                .build());
        } else {
            statefulSetSpecBuilder.withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
                .withMetadata(new ObjectMetaBuilder()
                    .withName(DATADIR_VOLUME)
                    .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                    .build())
                .withSpec(new PersistentVolumeClaimSpecBuilder()
                    .withAccessModes("ReadWriteOnce")
                    .withStorageClassName(spec.getStorageClassName())
                    .withResources(new VolumeResourceRequirementsBuilder()
                        .withRequests(Map.of("storage", new Quantity(spec.getStorageSize())))
                        .build())
                    .build())
                .build());
        }

        statefulSetSpecBuilder.withTemplate(new PodTemplateSpecBuilder()
            .withNewMetadata()
            .addToLabels("app", name)
            .endMetadata()
            .withNewSpec()
            .withImagePullSecrets(new LocalObjectReferenceBuilder().withName(imagePullSecretName).build())
            .withInitContainers(getInitContainers(image, envVars, resources))
            .withContainers(container)
            .withVolumes(volumes)
            .endSpec()
            .build());

        return new StatefulSetBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withSpec(statefulSetSpecBuilder.build())
            .build();
    }
}