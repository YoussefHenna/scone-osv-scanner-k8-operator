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
public class MariadbPrimaryServiceDependentResource extends CRUDKubernetesDependentResource<Service, SconeOsvScanner> {

    public MariadbPrimaryServiceDependentResource() {
        super(Service.class);
    }

    @Override
    protected Service desired(SconeOsvScanner primary, Context<SconeOsvScanner> context) {
        String primaryName = primary.getMetadata().getName();
        String name = Constants.getMariadbPrimaryServiceName(primaryName);
        String namespace = primary.getMetadata().getNamespace();
        String statefulSetName = Constants.getMariadbPrimaryName(primaryName);

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
                    .withPort(Constants.MARIADB_PRIMARY_PORT)
                    .withNewTargetPort(Constants.MARIADB_PRIMARY_PORT_NAME)
                    .withProtocol("TCP")
                    .withName(Constants.MARIADB_PRIMARY_PORT_NAME)
                    .build())
                .build())
            .build();
    }
}