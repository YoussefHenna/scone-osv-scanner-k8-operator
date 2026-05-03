package com.youssefhenna.dependent.database;

import com.youssefhenna.dependent.database.mariadb.AbstractMariadbStatefulSetDependentResource;
import com.youssefhenna.spec.DatabaseSpec;
import com.youssefhenna.spec.MariadbSpec;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;

@KubernetesDependent(informer = @Informer(labelSelector = Constants.DEPENDANT_SELECTOR))
public class MariadbReplicaStatefulSetDependentResource extends AbstractMariadbStatefulSetDependentResource {

    @Override
    protected MariadbSpec getMariadbSpec(DatabaseSpec dbSpec) {
        return dbSpec.getMariadbReplica();
    }

    @Override
    protected int getReplicas(DatabaseSpec dbSpec) {
        return dbSpec.getMariadbReplica().getReplicas();
    }

    @Override
    protected String getStatefulSetName(String primaryName) {
        return Constants.getMariadbReplicaName(primaryName);
    }

    @Override
    protected String getEntrypointScript() {
        return "/start/replica-entrypoint.sh";
    }

    @Override
    protected String getPortName() {
        return Constants.MARIADB_REPLICA_PORT_NAME;
    }

    @Override
    protected int getPort() {
        return Constants.MARIADB_REPLICA_PORT;
    }

    @Override
    protected List<Container> getInitContainers(String image, List<EnvVar> envVars, ResourceRequirements resources) {
        return List.of(new ContainerBuilder()
            .withName("wait-for-primary")
            .withImage(image)
            .withCommand("/bin/bash", "-ec",
                "export SCONE_HEAP=32M\nexport SCONE_CONFIG_ID=\"$BASE_SCONE_CONFIG_ID/wait-for-primary\"\n\nuntil mysqladmin; do\n  echo \"waiting for primary...\"\n  sleep 10\ndone")
            .withEnv(envVars)
            .withResources(resources)
            .build());
    }
}