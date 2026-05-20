package com.youssefhenna.dependent;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.CommonRegistrySpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.scanner.DbManagerSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
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
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        ScannerSpec scannerSpec = primarySpec.getScanner();
        DbManagerSpec spec = scannerSpec.getDbManager();

        String name = Constants.getDbManagerDeploymentName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();

        CommonRegistrySpec registrySpec = spec.resolveRegistrySpec(scannerSpec, primarySpec);
        if(registrySpec == null){
            throw new RuntimeException("Cannot resolve image registry for db manager");
        }

        String image = Common.buildImage(registrySpec.getRegistryUrl(), spec.getImageName(), spec.getImageVersion());
        String imagePullSecretName = Common.getImagePullSecretName(registrySpec.getRegistryCredentials());

        String memory = spec.getMemory();
        List<EnvVar> envVars = Common.buildSconeEnvVars(memory, primarySpec.getCasAddress(), spec.getSconeConfigId());
        ResourceRequirements resources = Common.buildSgxResources(memory);

        Container container = new ContainerBuilder()
            .withName(name + "-container")
            .withImage(image)
            .withImagePullPolicy("Always")
            .withEnv(envVars)
            .withResources(resources)
            .build();

        return Common.buildDeployment(name, namespace, imagePullSecretName, container, 1, Common.getPolicyHashAnnotation(primary, spec.getSconeConfigId()));
    }

}