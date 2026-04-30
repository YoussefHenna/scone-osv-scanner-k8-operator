package com.youssefhenna.dependent.database;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.DatabaseSpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.MariadbSpec;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;
import java.util.Map;


@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbPrimaryStatefulSetDependentResource extends CRUDKubernetesDependentResource<StatefulSet, SconeOsvScanner> {

    private static final String DATADIR_VOLUME = "mariadb-datadir";
    private static final String INIT_SCRIPTS_VOLUME = "custom-init-scripts";

    public MariadbPrimaryStatefulSetDependentResource() {
        super(StatefulSet.class);
    }

    @Override
    protected StatefulSet desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        DatabaseSpec dbSpec = primarySpec.getDatabaseSpec();
        MariadbSpec spec = dbSpec.getMariadbPrimarySpec();

        String primaryName = primary.getMetadata().getName();
        String name = Constants.getMariadbPrimaryName(primaryName);
        String namespace = primary.getMetadata().getNamespace();
        String configMapName = Constants.getMariadbInitScriptsConfigMapName(primaryName);

        String image = Common.buildImage(dbSpec.getRegistryUrl(), dbSpec.getRegistryRepository(), spec.getImageName(), spec.getImageVersion());
        String imagePullSecretName = dbSpec.getRegistryCredentials().getSecretRef().getName();

        String memory = spec.getMemory();
        List<EnvVar> envVars = Common.buildSconeEnvVars(memory, primarySpec.getCasAddress(), spec.getSconeConfigId());
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
            .withCommand("bash", "-c", "/start/primary-entrypoint.sh")
            .withEnv(envVars)
            .withResources(resources)
            .withPorts(new ContainerPortBuilder().withName(Constants.MARIADB_PRIMARY_PORT_NAME).withContainerPort(Constants.MARIADB_PRIMARY_PORT).withProtocol("TCP").build())
            .withReadinessProbe(probe)
            .withStartupProbe(new ProbeBuilder(probe).withInitialDelaySeconds(10).withFailureThreshold(10).build())
            .withLivenessProbe(new ProbeBuilder(probe).withFailureThreshold(3).build())
            .withVolumeMounts(
                new VolumeMountBuilder().withName(DATADIR_VOLUME).withMountPath("/var/lib/mysql").build(),
                new VolumeMountBuilder().withName(INIT_SCRIPTS_VOLUME).withMountPath("/start").build()
            )
            .build();

        PersistentVolumeClaim datadirClaim = new PersistentVolumeClaimBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(DATADIR_VOLUME)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withSpec(new PersistentVolumeClaimSpecBuilder()
                .withAccessModes("ReadWriteOnce")
                .withStorageClassName("default")
                .withResources(new VolumeResourceRequirementsBuilder()
                    .withRequests(Map.of("storage", new Quantity(spec.getStorageSize())))
                    .build())
                .build())
            .build();

        return new StatefulSetBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withSpec(new StatefulSetSpecBuilder()
                .withReplicas(spec.getReplicas())
                .withServiceName(name)
                .withSelector(new LabelSelectorBuilder().addToMatchLabels("app", name).build())
                .withTemplate(new PodTemplateSpecBuilder()
                    .withNewMetadata()
                    .addToLabels("app", name)
                    .endMetadata()
                    .withNewSpec()
                    .withImagePullSecrets(new LocalObjectReferenceBuilder().withName(imagePullSecretName).build())
                    .withContainers(container)
                    .withVolumes(new VolumeBuilder()
                        .withName(INIT_SCRIPTS_VOLUME)
                        .withConfigMap(new ConfigMapVolumeSourceBuilder()
                            .withName(configMapName)
                            .withDefaultMode(0511)
                            .build())
                        .build())
                    .endSpec()
                    .build())
                .withVolumeClaimTemplates(datadirClaim)
                .build())
            .build();
    }
}