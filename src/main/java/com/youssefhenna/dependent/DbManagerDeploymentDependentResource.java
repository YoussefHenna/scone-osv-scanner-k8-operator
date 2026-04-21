package com.youssefhenna.dependent;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.SconeOsvScannerSpec;
import com.youssefhenna.utils.Constants;
import com.youssefhenna.utils.DeploymentCommon;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;


@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class DbManagerDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, SconeOsvScanner> {

    public DbManagerDeploymentDependentResource() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        SconeOsvScannerSpec spec = primary.getSpec();

        String name = Constants.getDbManagerDeploymentName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();

        String image = spec.getRegistryRepository() + "/" + spec.getDbManagerImageName() + ":latest";
        String imagePullSecretName = spec.getRegistryCredentials().getSecretRef().getName();

        String memory = "12G";
        List<EnvVar> envVars = DeploymentCommon.buildSconeEnvVars(memory, spec.getCasAddress(), "scone-osv-scan/dbmanager_service", null);
        ResourceRequirements resources = DeploymentCommon.buildSgxResources(memory);

        Container container = new ContainerBuilder()
            .withName(name + "-container")
            .withImage(image)
            .withImagePullPolicy("Always")
            .withEnv(envVars)
            .withResources(resources)
            .build();

        return DeploymentCommon.buildDeployment(name, namespace, imagePullSecretName, container);
    }

}