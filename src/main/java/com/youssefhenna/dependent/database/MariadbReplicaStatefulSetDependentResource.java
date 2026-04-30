package com.youssefhenna.dependent.database;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.SconeOsvScannerSpec;
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
public class MariadbReplicaStatefulSetDependentResource extends CRUDKubernetesDependentResource<StatefulSet, SconeOsvScanner> {

    private static final String DATADIR_VOLUME = "mariadb-datadir";
    private static final String INIT_SCRIPTS_VOLUME = "custom-init-scripts";

    public MariadbReplicaStatefulSetDependentResource() {
        super(StatefulSet.class);
    }

    @Override
    protected StatefulSet desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        SconeOsvScannerSpec.DatabaseSpec dbSpec = primarySpec.getDatabaseSpec();
        SconeOsvScannerSpec.MariadbSpec spec = dbSpec.getMariadbReplica();

        String primaryName = primary.getMetadata().getName();
        String name = Constants.getMariadbReplicaName(primaryName);
        String namespace = primary.getMetadata().getNamespace();
        String configMapName = Constants.getMariadbInitScriptsConfigMapName(primaryName);

        String registryRepository = dbSpec.getRegistryRepository() != null
            ? dbSpec.getRegistryRepository()
            : primarySpec.getRegistryRepository();
        String image = Common.buildImage(primarySpec.getRegistryUrl(), registryRepository, spec.getImageName(), spec.getImageVersion());
        String imagePullSecretName = primarySpec.getRegistryCredentials().getSecretRef().getName();

        String memory = spec.getMemory();
        List<EnvVar> envVars = Common.buildSconeEnvVars(memory, primarySpec.getCasAddress(), spec.getSconeConfigId(), "1",
            new EnvVarBuilder().withName("BASE_SCONE_CONFIG_ID").withValue(spec.getSconeConfigId()).build(),
            new EnvVarBuilder().withName("SCONE_ETHREAD_SLEEP_TIME_MSEC").withValue("5ms").build(),
            new EnvVarBuilder().withName("SCONE_ALLOW_DLOPEN").withValue("1").build(),
            new EnvVarBuilder().withName("SCONE_MODE").withValue("hw").build(),
            new EnvVarBuilder().withName("SCONE_SYSLIBS").withValue("1").build(),
            new EnvVarBuilder().withName("SCONE_LOG").withValue("error").build()
        );
        ResourceRequirements resources = Common.buildSgxResources(memory);

        Probe probe = new ProbeBuilder()
            .withTimeoutSeconds(30)
            .withPeriodSeconds(60)
            .withNewExec()
            .withCommand("/bin/bash", "-ec",
                "export SCONE_HEAP=32M\nexport SCONE_CONFIG_ID=\"$BASE_SCONE_CONFIG_ID/probe\"\nmysqladmin")
            .endExec()
            .build();

        Container waitForPrimary = new ContainerBuilder()
            .withName("wait-for-primary")
            .withImage(image)
            .withCommand("/bin/bash", "-ec",
                "export SCONE_HEAP=32M\nexport SCONE_CONFIG_ID=\"$BASE_SCONE_CONFIG_ID/wait-for-primary\"\n\nuntil mysqladmin; do\n  echo \"waiting for primary...\"\n  sleep 10\ndone")
            .withEnv(envVars)
            .withResources(resources)
            .build();

        Container container = new ContainerBuilder()
            .withName(name + "-container")
            .withImage(image)
            .withImagePullPolicy("Always")
            .withCommand("bash", "-c", "/start/replica-entrypoint.sh")
            .withEnv(envVars)
            .withResources(resources)
            .withPorts(new ContainerPortBuilder().withName("mariadb").withContainerPort(Constants.MARIADB_REPLICA_PORT).withProtocol("TCP").build())
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
                .withStorageClassName(spec.getStorageClassName())
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
                    .withInitContainers(waitForPrimary)
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
