package com.youssefhenna.dependent.database;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.DatabaseSpec;
import com.youssefhenna.spec.MaxscaleSpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.ArrayList;
import java.util.List;


@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MaxscaleDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, SconeOsvScanner> {

    public MaxscaleDeploymentDependentResource() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        DatabaseSpec dbSpec = primarySpec.getDatabase();
        MaxscaleSpec spec = dbSpec.getMaxscale();

        String name = Constants.getMaxscaleDeploymentName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();

        String image = Common.buildImage(dbSpec.getRegistryUrl(), dbSpec.getRegistryRepository(), spec.getImageName(), spec.getImageVersion());
        String imagePullSecretName = dbSpec.getRegistryCredentials().getSecretRef().getName();

        String memory = spec.getMemory();
        List<EnvVar> envVars = new ArrayList<>(Common.buildSconeEnvVars(memory, primarySpec.getCasAddress(), spec.getSconeConfigId()));
        ResourceRequirements resources = Common.buildSgxResources(memory);

        Container container = new ContainerBuilder()
            .withName(name + "-container")
            .withImage(image)
            .withImagePullPolicy("Always")
            .withCommand("maxscale")
            .withEnv(envVars)
            .withResources(resources)
            .withPorts(
                new ContainerPortBuilder().withName(Constants.MAXSCALE_PORT_NAME).withContainerPort(Constants.MAXSCALE_PORT).withProtocol("TCP").build(),
                new ContainerPortBuilder().withName(Constants.MAXSCALE_ADMIN_PORT_NAME).withContainerPort(Constants.MAXSCALE_ADMIN_PORT).withProtocol("TCP").build()
            )
            .build();

        return Common.buildDeployment(name, namespace, imagePullSecretName, container, spec.getReplicas());
    }
}