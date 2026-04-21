package com.youssefhenna;

import com.youssefhenna.dependent.DbManagerDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppServiceDependentResource;
import com.youssefhenna.model.DeploymentStatus;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;


@Workflow(
    dependents = {
        @Dependent(type = DbManagerDeploymentDependentResource.class, name = Constants.DB_MANAGER_DEPENDENT_NAME),
        @Dependent(type = FrontAppDeploymentDependentResource.class, name = Constants.FRONT_APP_DEPENDENT_NAME),
        @Dependent(type = FrontAppServiceDependentResource.class, name = Constants.FRONT_APP_SERVICE_DEPENDENT_NAME)
    })
@ControllerConfiguration()
public class SconeOsvScannerReconciler implements Reconciler<SconeOsvScanner> {

    @Override
    public UpdateControl<SconeOsvScanner> reconcile(SconeOsvScanner resource, Context<SconeOsvScanner> context) {
        SconeOsvScannerStatus status = new SconeOsvScannerStatus();

        Deployment dbManager = context.getSecondaryResource(Deployment.class, Constants.DB_MANAGER_DEPENDENT_NAME).orElse(null);
        Deployment frontApp = context.getSecondaryResource(Deployment.class, Constants.FRONT_APP_DEPENDENT_NAME).orElse(null);

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
