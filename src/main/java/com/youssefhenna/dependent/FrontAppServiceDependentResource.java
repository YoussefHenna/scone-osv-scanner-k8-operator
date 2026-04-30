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
public class FrontAppServiceDependentResource extends CRUDKubernetesDependentResource<Service, SconeOsvScanner> {

    public FrontAppServiceDependentResource() {
        super(Service.class);
    }

    @Override
    protected Service desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String name = Constants.getFrontAppServiceName(primary.getMetadata().getName());
        String namespace = primary.getMetadata().getNamespace();
        String deploymentName = Constants.getFrontAppDeploymentName(primary.getMetadata().getName());

        ObjectMeta metadata = new ObjectMetaBuilder()
            .withName(name)
            .withNamespace(namespace)
            .withAnnotations(Map.of(
                "service.beta.kubernetes.io/azure-load-balancer-health-probe-request-path", "/health",
                "service.beta.kubernetes.io/azure-load-balancer-health-probe-protocol", "https"
            ))
            .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
            .build();

        ServicePort port = new ServicePortBuilder()
            .withPort(Constants.FRONT_APP_PORT)
            .withNewTargetPort(Constants.FRONT_APP_PORT)
            .withProtocol("TCP")
            .withName("https")
            .build();

        ServiceSpec spec = new ServiceSpecBuilder()
            .withType("LoadBalancer").withSelector(Map.of("app", deploymentName))
            .withPorts(port)
            .build();

        return new ServiceBuilder()
            .withMetadata(metadata)
            .withSpec(spec)
            .build();
    }
}