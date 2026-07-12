package com.youssefhenna.dependent;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.Map;

@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class DbManagerServiceDependentResource extends CRUDKubernetesDependentResource<Service, SconeOsvScanner> {

    public DbManagerServiceDependentResource() {
        super(Service.class);
    }

    @Override
    protected Service desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String name = Constants.getDbManagerServiceName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();
        String deploymentName = Constants.getDbManagerDeploymentName(primary.getMetadata().getName());

        ObjectMeta metadata = new ObjectMetaBuilder()
            .withName(name)
            .withNamespace(namespace)
            .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
            .build();

        ServicePort port = new ServicePortBuilder()
            .withPort(Constants.DB_MANAGER_PORT)
            .withNewTargetPort(Constants.DB_MANAGER_PORT)
            .withProtocol("TCP")
            .withName("http")
            .build();

        ServiceSpec spec = new ServiceSpecBuilder()
            .withType("ClusterIP").withSelector(Map.of("app", deploymentName))
            .withPorts(port)
            .build();

        return new ServiceBuilder()
            .withMetadata(metadata)
            .withSpec(spec)
            .build();
    }
}
