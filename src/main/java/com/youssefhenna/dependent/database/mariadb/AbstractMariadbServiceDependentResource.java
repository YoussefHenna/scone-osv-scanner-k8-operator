package com.youssefhenna.dependent.database.mariadb;

import com.youssefhenna.SconeOsvScanner;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.Map;

public abstract class AbstractMariadbServiceDependentResource extends CRUDKubernetesDependentResource<Service, SconeOsvScanner> {

    protected AbstractMariadbServiceDependentResource() {
        super(Service.class);
    }

    protected abstract String getServiceName(String primaryName);
    protected abstract String getStatefulSetName(String primaryName);
    protected abstract int getPort();
    protected abstract String getPortName();

    @Override
    protected Service desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String primaryName = primary.getMetadata().getName();
        String name = getServiceName(primaryName);
        String namespace = primary.getMetadata().getNamespace();
        String statefulSetName = getStatefulSetName(primaryName);

        return new ServiceBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(Constants.DEPENDANT_LABEL_KEY, Constants.DEPENDANT_LABEL_VALUE)
                .build())
            .withSpec(new ServiceSpecBuilder()
                .withType("ClusterIP")
                .withSelector(Map.of("app", statefulSetName))
                .withPorts(new ServicePortBuilder()
                    .withPort(getPort())
                    .withNewTargetPort(getPortName())
                    .withProtocol("TCP")
                    .withName(getPortName())
                    .build())
                .build())
            .build();
    }
}