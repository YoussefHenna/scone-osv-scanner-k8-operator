package com.youssefhenna;

import com.youssefhenna.model.DeploymentStatus;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class SconeOsvScannerReconciler implements Reconciler<SconeOsvScanner> {

    private final KubernetesClient kubernetesClient;

    public SconeOsvScannerReconciler(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public UpdateControl<SconeOsvScanner> reconcile(SconeOsvScanner resource, Context<SconeOsvScanner> context) {
        String name = resource.getMetadata().getName();
        String namespace = resource.getMetadata().getNamespace();

        SconeOsvScannerSpec spec = resource.getSpec();
        String imageRepo = spec.getRegistryRepository();

        String dbManagerName = name + "-db-manager";
        String frontAppName = name + "-front-app";

        //TODO: Version checking and verification, not always latest
        String dbManagerImageName = imageRepo + "/" + spec.getDbManagerImageName() + ":latest";
        String frontAppImageName = imageRepo + "/" + spec.getFrontAppImageName() + ":latest";


        applyDeployment(buildDeployment(dbManagerName, namespace, dbManagerImageName, dbManagerName + "-container", resource));
        applyDeployment(buildDeployment(frontAppName, namespace, frontAppImageName, frontAppName + "-container", resource));

        SconeOsvScannerStatus status = new SconeOsvScannerStatus();
        status.setDbManagerDeploymentStatus(resolveDeploymentStatus(getDeployment(dbManagerName, namespace)));
        status.setFrontAppDeploymentStatus(resolveDeploymentStatus(getDeployment(frontAppName, namespace)));
        resource.setStatus(status);

        return UpdateControl.patchStatus(resource);
    }

    private void applyDeployment(Deployment desired) {
        String name = desired.getMetadata().getName();
        String namespace = desired.getMetadata().getNamespace();
        Deployment existing = getDeployment(name, namespace);
        if (existing == null) {
            kubernetesClient.apps().deployments().inNamespace(namespace).resource(desired).create();
        } else {
            kubernetesClient.apps().deployments().inNamespace(namespace).resource(desired).update();
        }
    }

    private Deployment getDeployment(String name, String namespace) {
        return kubernetesClient.apps().deployments().inNamespace(namespace).withName(name).get();
    }

    private Deployment buildDeployment(String name, String namespace, String image, String containerName, SconeOsvScanner owner) {
        ObjectMeta metadata = new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToOwnerReferences(buildOwnerReference(owner))
                .build();

        LabelSelector selector = new LabelSelectorBuilder()
                .addToMatchLabels("app", name)
                .build();

        PodTemplateSpec podTemplate = new PodTemplateSpecBuilder()
                .withNewMetadata()
                .addToLabels("app", name)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(containerName)
                .withImage(image)
                .endContainer()
                .endSpec()
                .build();


        DeploymentSpec spec = new DeploymentSpecBuilder()
                .withReplicas(1)
                .withSelector(selector)
                .withTemplate(podTemplate)
                .build();

        Deployment deployment = new DeploymentBuilder().build();
        deployment.setMetadata(metadata);
        deployment.setSpec(spec);

        return deployment;
    }

    private OwnerReference buildOwnerReference(SconeOsvScanner owner) {
        return new OwnerReferenceBuilder()
                .withApiVersion(owner.getApiVersion())
                .withKind(owner.getKind())
                .withName(owner.getMetadata().getName())
                .withUid(owner.getMetadata().getUid())
                .withController(true)
                .build();
    }

    private DeploymentStatus resolveDeploymentStatus(Deployment deployment) {
        if (deployment == null || deployment.getStatus() == null) {
            return DeploymentStatus.STARTING;
        }
        Integer ready = deployment.getStatus().getReadyReplicas();
        Integer total = deployment.getStatus().getReplicas();
        if (ready != null && ready.equals(total) && total > 0) {
            return DeploymentStatus.RUNNING;
        }
        Integer unavailable = deployment.getStatus().getUnavailableReplicas();
        if (unavailable != null && unavailable > 0) {
            return DeploymentStatus.FAILING;
        }
        return DeploymentStatus.STARTING;
    }
}