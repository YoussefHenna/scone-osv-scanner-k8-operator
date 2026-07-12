package com.youssefhenna.dependent;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.CommonRegistrySpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.scanner.DbManagerSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import com.youssefhenna.status.DbManagerStatus;
import com.youssefhenna.status.SconeOsvScannerStatus;
import com.youssefhenna.model.RegistryCredentials;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategyBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.logging.Log;

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

        String image = Common.buildImage(registrySpec.getRegistryUrl(), spec.getImageName(), resolveImageVersion(primary, spec.getImageVersion()));
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

        Deployment deployment = Common.buildDeployment(name, namespace, imagePullSecretName, container, 1, Common.getPolicyHashAnnotation(primary, spec.getSconeConfigId()));
        // 'Recreate' to destroy old instance before starting new one, only one writer/DB syncer.
        // resolveImageVersion ensures new version does not disrupt ongoing update
        deployment.getSpec().setStrategy(new DeploymentStrategyBuilder().withType("Recreate").build());
        return deployment;
    }

    // Postpone/delay a version roll while the running manager has a database update in progress
    private static String resolveImageVersion(SconeOsvScanner primary, String targetVersion) {
        SconeOsvScannerStatus status = primary.getStatus();
        if (status == null || status.getDbManagerStatus() == null) {
            return targetVersion;
        }
        DbManagerStatus dbManagerStatus = status.getDbManagerStatus();
        String runningVersion = dbManagerStatus.getCurrentVersion();
        if (runningVersion != null && !runningVersion.equals(targetVersion) && dbManagerStatus.isUpdateInProgress()) {
            Log.info("Delaying db manager rollout to " + targetVersion + ", database update in progress");
            return runningVersion;
        }
        return targetVersion;
    }

}