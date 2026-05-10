package com.youssefhenna;

import com.youssefhenna.dependent.DbManagerDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppServiceDependentResource;
import com.youssefhenna.dependent.database.*;
import com.youssefhenna.model.DependantStatus;
import com.youssefhenna.model.PollConfig;
import com.youssefhenna.policy.PolicySync;
import com.youssefhenna.policy.cas.HttpCASClient;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.status.PolicyUploadStatus;
import com.youssefhenna.status.PolicyUploadStatusItem;
import com.youssefhenna.status.SconeOsvScannerStatus;
import com.youssefhenna.utils.Common;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import java.text.ParseException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
        SconeOsvScannerStatus status = buildDependantsStatus(resource, context);

        PolicyUpstreamSpec upstream = resource.getSpec().getPolicyUpstream();
        if (upstream != null) {
            status.setPolicyUploadStatus(resolvePolicyUploadStatus(resource, upstream));
            resource.setStatus(status);
            return UpdateControl.patchStatus(resource).rescheduleAfter(toDuration(upstream.getPoll()));
        }

        resource.setStatus(status);
        return UpdateControl.patchStatus(resource);
    }

    private PolicyUploadStatus resolvePolicyUploadStatus(SconeOsvScanner resource, PolicyUpstreamSpec upstream) {
        PolicyUploadStatus existingUploadStatus = resource.getStatus() != null
            ? resource.getStatus().getPolicyUploadStatus()
            : null;

        Duration pollDuration = toDuration(upstream.getPoll());
        boolean shouldSync = true;
        if (existingUploadStatus != null && existingUploadStatus.getLastSyncTime() != null) {
            try {
                Date lastSync = Common.parseDate(existingUploadStatus.getLastSyncTime());
                Date nextSync = new Date(lastSync.getTime() + pollDuration.toMillis());
                shouldSync = new Date().after(nextSync) || !upstream.equals(existingUploadStatus.getLastSyncedUpstream());
            } catch (ParseException ignored) {
            }
        }

        if (!shouldSync) {
            return existingUploadStatus;
        }

        PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(
            upstream,
            new HttpCASClient(resource.getSpec().getCasAddress(), resource.getSpec().getCasPort())
        );
        return buildPolicyUploadStatus(upstream, result);
    }

    private PolicyUploadStatus buildPolicyUploadStatus(PolicyUpstreamSpec upstream, PolicySync.SyncPoliciesResult result) {
        Map<String, PolicyUploadStatusItem> statues = new HashMap<>();
        for (PolicyUploadStatusItem item : result.statuses()) {
            statues.put(item.getName(), item);
        }
        PolicyUploadStatus uploadStatus = new PolicyUploadStatus();
        uploadStatus.setLastRunStatus(result.overallStatus());
        uploadStatus.setPolicyUpdateStatuses(statues);
        uploadStatus.setLastSyncTime(Common.dateToString(new Date()));
        uploadStatus.setLastSyncedUpstream(upstream);
        return uploadStatus;
    }

    private Duration toDuration(PollConfig poll) {
        return switch (poll.getUnit()) {
            case DAYS -> Duration.ofDays(poll.getEvery());
            case HOURS -> Duration.ofHours(poll.getEvery());
            case MINUTES -> Duration.ofMinutes(poll.getEvery());
        };
    }

    private SconeOsvScannerStatus buildDependantsStatus(SconeOsvScanner resource, Context<SconeOsvScanner> context) {
        Set<Deployment> dependantDeployments = context.getSecondaryResources(Deployment.class);
        Set<StatefulSet> dependantStatefulSets = context.getSecondaryResources(StatefulSet.class);

        String primaryName = resource.getMetadata().getName();
        Deployment dbManager = null;
        Deployment frontApp = null;
        Deployment maxscale = null;
        for (Deployment d : dependantDeployments) {
            String name = d.getMetadata().getName();
            if (name.equals(Constants.getDbManagerDeploymentName(primaryName))) {
                dbManager = d;
            } else if (name.equals(Constants.getFrontAppDeploymentName(primaryName))) {
                frontApp = d;
            } else if (name.equals(Constants.getMaxscaleDeploymentName(primaryName))) {
                maxscale = d;
            }
        }

        StatefulSet mariadbPrimary = null;
        StatefulSet mariadbReplica = null;
        for (StatefulSet ss : dependantStatefulSets) {
            String name = ss.getMetadata().getName();
            if (name.equals(Constants.getMariadbPrimaryName(primaryName))) {
                mariadbPrimary = ss;
            } else if (name.equals(Constants.getMariadbReplicaName(primaryName))) {
                mariadbReplica = ss;
            }
        }

        SconeOsvScannerStatus status = new SconeOsvScannerStatus();
        status.setDbManagerStatus(resolveDeploymentStatus(dbManager));
        status.setFrontAppStatus(resolveDeploymentStatus(frontApp));
        status.setMaxscaleStatus(resolveDeploymentStatus(maxscale));
        status.setMariadbPrimaryStatus(resolveStatefulSetStatus(mariadbPrimary));
        status.setMariadbReplicaStatus(resolveStatefulSetStatus(mariadbReplica));
        return status;
    }

    private DependantStatus resolveDeploymentStatus(Deployment deployment) {
        if (deployment == null || deployment.getStatus() == null) {
            return DependantStatus.STARTING;
        }
        return resolveReplicaStatus(deployment.getStatus().getReadyReplicas(), deployment.getStatus().getReplicas());
    }

    private DependantStatus resolveStatefulSetStatus(StatefulSet statefulSet) {
        if (statefulSet == null || statefulSet.getStatus() == null) {
            return DependantStatus.STARTING;
        }
        return resolveReplicaStatus(statefulSet.getStatus().getReadyReplicas(), statefulSet.getStatus().getReplicas());
    }

    private DependantStatus resolveReplicaStatus(Integer ready, Integer total) {
        if (ready != null && ready.equals(total) && total > 0) {
            return DependantStatus.RUNNING;
        }
        if (ready != null && total != null && ready < total) {
            return DependantStatus.FAILING;
        }
        return DependantStatus.STARTING;
    }
}
