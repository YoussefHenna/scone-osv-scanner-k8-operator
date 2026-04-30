package com.youssefhenna.dependent.database;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.Map;

@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MaxscaleServiceDependentResource extends CRUDKubernetesDependentResource<Service, SconeOsvScanner> {

    public MaxscaleServiceDependentResource() {
        super(Service.class);
    }

    @Override
    protected Service desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String primaryName = primary.getMetadata().getName();
        String name = Constants.getMaxscaleServiceName(primaryName);
        String namespace = primary.getMetadata().getNamespace();
        String deploymentName = Constants.getMaxscaleDeploymentName(primaryName);

        return new ServiceBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withSpec(new ServiceSpecBuilder()
                .withType("ClusterIP")
                .withSelector(Map.of("app", deploymentName))
                .withPorts(new ServicePortBuilder()
                    .withPort(Constants.MAXSCALE_PORT)
                    .withNewTargetPort(Constants.MAXSCALE_PORT_NAME)
                    .withProtocol("TCP")
                    .withName(Constants.MAXSCALE_PORT_NAME)
                    .build())
                .build())
            .build();
    }
}
