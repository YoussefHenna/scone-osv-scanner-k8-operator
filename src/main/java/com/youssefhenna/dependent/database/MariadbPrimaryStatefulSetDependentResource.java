package com.youssefhenna.dependent.database;

import com.youssefhenna.dependent.database.mariadb.AbstractMariadbStatefulSetDependentResource;
import com.youssefhenna.spec.database.DatabaseSpec;
import com.youssefhenna.spec.database.MariadbSpec;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;

@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbPrimaryStatefulSetDependentResource extends AbstractMariadbStatefulSetDependentResource {

    @Override
    protected int getReplicas(DatabaseSpec dbSpec) {
        return 1;
    }

    @Override
    protected String getConfigId(MariadbSpec spec) {
        return spec.getSconeConfigId();
    }

    @Override
    protected String getStatefulSetName(String primaryName) {
        return Constants.getMariadbPrimaryName(primaryName);
    }

    @Override
    protected String getEntrypointScript() {
        return "/start/primary-entrypoint.sh";
    }

    @Override
    protected String getPortName() {
        return Constants.MARIADB_PRIMARY_PORT_NAME;
    }

    @Override
    protected int getPort() {
        return Constants.MARIADB_PRIMARY_PORT;
    }

    @Override
    protected List<Container> getInitContainers(String image, List<EnvVar> envVars, ResourceRequirements resources) {
        return List.of();
    }
}