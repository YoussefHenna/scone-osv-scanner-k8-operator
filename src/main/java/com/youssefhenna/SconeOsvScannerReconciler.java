package com.youssefhenna;

import com.youssefhenna.dependent.DbManagerDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppServiceDependentResource;
import com.youssefhenna.dependent.database.*;
import com.youssefhenna.model.DeploymentStatus;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import java.util.Set;


@Workflow(
    dependents = {
        @Dependent(type = DbManagerDeploymentDependentResource.class, name = Constants.DB_MANAGER_DEPENDENT_NAME),
        @Dependent(type = FrontAppDeploymentDependentResource.class, name = Constants.FRONT_APP_DEPENDENT_NAME),
        @Dependent(type = FrontAppServiceDependentResource.class, name = Constants.FRONT_APP_SERVICE_DEPENDENT_NAME),
        @Dependent(type = MariadbInitScriptsConfigMapDependentResource.class, name = Constants.MARIADB_INIT_SCRIPTS_CONFIGMAP_DEPENDENT_NAME),
        @Dependent(type = MariadbPrimaryPdbDependentResource.class, name = Constants.MARIADB_PRIMARY_PDB_DEPENDENT_NAME),
        @Dependent(type = MariadbReplicaPdbDependentResource.class, name = Constants.MARIADB_REPLICA_PDB_DEPENDENT_NAME),
        @Dependent(type = MaxscaleAdminServiceDependentResource.class, name = Constants.MAXSCALE_ADMIN_SERVICE_DEPENDENT_NAME),
        @Dependent(type = MaxscaleServiceDependentResource.class, name = Constants.MAXSCALE_SERVICE_DEPENDENT_NAME),
        @Dependent(type = MariadbPrimaryServiceDependentResource.class, name = Constants.MARIADB_PRIMARY_SERVICE_DEPENDENT_NAME),
        @Dependent(type = MariadbReplicaServiceDependentResource.class, name = Constants.MARIADB_REPLICA_SERVICE_DEPENDENT_NAME),
        @Dependent(type = MaxscaleDeploymentDependentResource.class, name = Constants.MAXSCALE_DEPLOYMENT_DEPENDENT_NAME),
        @Dependent(type = MariadbPrimaryStatefulSetDependentResource.class, name = Constants.MARIADB_PRIMARY_STATEFULSET_DEPENDENT_NAME),
        @Dependent(type = MariadbReplicaStatefulSetDependentResource.class, name = Constants.MARIADB_REPLICA_STATEFULSET_DEPENDENT_NAME)
    })
@ControllerConfiguration()
public class SconeOsvScannerReconciler implements Reconciler<SconeOsvScanner> {

    @Override
    public UpdateControl<SconeOsvScanner> reconcile(SconeOsvScanner resource, Context<SconeOsvScanner> context) {
        SconeOsvScannerStatus status = new SconeOsvScannerStatus();

        Set<Deployment> dependantDeployments = context.getSecondaryResources(Deployment.class);

        String primaryName = resource.getMetadata().getName();
        Deployment dbManager = null;
        Deployment frontApp = null;
        for (Deployment d : dependantDeployments) {
            String name = d.getMetadata().getName();
            if (name.equals(Constants.getDbManagerDeploymentName(primaryName))) {
                dbManager = d;
            } else if (name.equals(Constants.getFrontAppDeploymentName(primaryName))) {
                frontApp = d;
            }
        }

        status.setDbManagerDeploymentStatus(resolveDeploymentStatus(dbManager));
        status.setFrontAppDeploymentStatus(resolveDeploymentStatus(frontApp));
        resource.setStatus(status);

        return UpdateControl.patchStatus(resource);
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
