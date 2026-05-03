package com.youssefhenna.dependent;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.spec.scanner.FrontAppSpec;
import com.youssefhenna.spec.scanner.ScannerSpec;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;


@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class FrontAppDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, SconeOsvScanner> {

    public FrontAppDeploymentDependentResource() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        SconeOsvScannerSpec primarySpec = primary.getSpec();
        ScannerSpec scannerSpec = primarySpec.getScanner();
        FrontAppSpec spec = scannerSpec.getFrontApp();

        String name = Constants.getFrontAppDeploymentName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();

        String image = Common.buildImage(scannerSpec.getRegistryUrl(), scannerSpec.getRegistryRepository(), spec.getImageName(), spec.getImageVersion());
        String imagePullSecretName = scannerSpec.getRegistryCredentials().getSecretRef().getName();

        String memory = spec.getMemory();
        List<EnvVar> envVars = Common.buildSconeEnvVars(memory, primarySpec.getCasAddress(), spec.getSconeConfigId());
        ResourceRequirements resources = Common.buildSgxResources(memory);

        Probe livenessProbe = new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/health")
                .withPort(new IntOrString(Constants.FRONT_APP_PORT))
                .withScheme("HTTPS")
                .build())
            .withInitialDelaySeconds(30)
            .withPeriodSeconds(60)
            .withTimeoutSeconds(5)
            .withFailureThreshold(3)
            .build();

        Probe readinessProbe = new ProbeBuilder()
            .withHttpGet(new HTTPGetActionBuilder()
                .withPath("/health")
                .withPort(new IntOrString(Constants.FRONT_APP_PORT))
                .withScheme("HTTPS")
                .build())
            .withInitialDelaySeconds(10)
            .withPeriodSeconds(30)
            .withTimeoutSeconds(3)
            .withFailureThreshold(3)
            .build();

        Container container = new ContainerBuilder()
            .withName(name + "-container")
            .withImage(image)
            .withImagePullPolicy("Always")
            .withEnv(envVars)
            .withResources(resources)
            .withPorts(new ContainerPortBuilder().withContainerPort(Constants.FRONT_APP_PORT).withName("https").build())
            .withLivenessProbe(livenessProbe)
            .withReadinessProbe(readinessProbe)
            .build();

        return Common.buildDeployment(name, namespace, imagePullSecretName, container, spec.getReplicas());
    }


}