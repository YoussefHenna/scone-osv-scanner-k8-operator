package com.youssefhenna.dependent.database;

import com.youssefhenna.dependent.database.mariadb.AbstractMariadbServiceDependentResource;
import com.youssefhenna.utils.Constants;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbReplicaServiceDependentResource extends AbstractMariadbServiceDependentResource {

    @Override
    protected String getServiceName(String primaryName) {
        return Constants.getMariadbReplicaServiceName(primaryName);
    }

    @Override
    protected String getStatefulSetName(String primaryName) {
        return Constants.getMariadbReplicaName(primaryName);
    }

    @Override
    protected int getPort() {
        return Constants.MARIADB_REPLICA_PORT;
    }

    @Override
    protected String getPortName() {
        return Constants.MARIADB_REPLICA_PORT_NAME;
    }
}