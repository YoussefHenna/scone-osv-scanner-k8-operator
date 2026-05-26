package com.youssefhenna;

import com.youssefhenna.dependent.DbManagerDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppDeploymentDependentResource;
import com.youssefhenna.dependent.FrontAppServiceDependentResource;
import com.youssefhenna.dependent.database.*;
import com.youssefhenna.dependent.kyverno.*;
import com.youssefhenna.policy.PolicySync;
import com.youssefhenna.policy.cas.HttpCASClient;
import com.youssefhenna.spec.SconeOsvScannerSpec;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.status.PolicyUploadStatus;
import com.youssefhenna.status.SconeOsvScannerStatus;
import com.youssefhenna.updates.ContainerUpdate;
import com.youssefhenna.updates.model.DependantResourceType;
import com.youssefhenna.updates.model.RunUpdateResult;
import com.youssefhenna.updates.registry_read.RegistryImageVersionReader;
import com.youssefhenna.updates.registry_read.RegistryImageVersionReaderImpl;
import com.youssefhenna.utils.*;
import com.youssefhenna.utils.Constants;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import com.youssefhenna.model.SecretRef;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


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
        @Dependent(type = MariadbReplicaStatefulSetDependentResource.class, name = Constants.MARIADB_REPLICA_STATEFULSET_DEPENDENT_NAME),
        @Dependent(type = DbManagerImageValidatingPolicyDependentResource.class, name = Constants.DB_MANAGER_IMAGE_VALIDATING_POLICY_DEPENDENT_NAME, activationCondition = DbManagerImageValidatingPolicyDependentResource.Condition.class),
        @Dependent(type = FrontAppImageValidatingPolicyDependentResource.class, name = Constants.FRONT_APP_IMAGE_VALIDATING_POLICY_DEPENDENT_NAME, activationCondition = FrontAppImageValidatingPolicyDependentResource.Condition.class),
        @Dependent(type = MaxscaleImageValidatingPolicyDependentResource.class, name = Constants.MAXSCALE_IMAGE_VALIDATING_POLICY_DEPENDENT_NAME, activationCondition = MaxscaleImageValidatingPolicyDependentResource.Condition.class),
        @Dependent(type = MariadbPrimaryImageValidatingPolicyDependentResource.class, name = Constants.MARIADB_PRIMARY_IMAGE_VALIDATING_POLICY_DEPENDENT_NAME, activationCondition = MariadbPrimaryImageValidatingPolicyDependentResource.Condition.class),
        @Dependent(type = MariadbReplicaImageValidatingPolicyDependentResource.class, name = Constants.MARIADB_REPLICA_IMAGE_VALIDATING_POLICY_DEPENDENT_NAME, activationCondition = MariadbReplicaImageValidatingPolicyDependentResource.Condition.class)
    })
@ControllerConfiguration()
public class SconeOsvScannerReconciler implements Reconciler<SconeOsvScanner> {

    private final MeterRegistry meterRegistry;
    private final Counter reconcileCounter;
    private final Timer reconcileTimer;
    private final Map<DependantResourceType, AtomicInteger> dependantStateGauges;
    private final MetricsUtils.CertExpiryGauges certExpiryGauges;

    @Inject
    public SconeOsvScannerReconciler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.reconcileCounter = MetricsUtils.registerReconcileCounter(meterRegistry);
        this.reconcileTimer = MetricsUtils.registerReconcileTimer(meterRegistry);
        this.dependantStateGauges = MetricsUtils.registerDependantStateGauges(meterRegistry);
        this.certExpiryGauges = MetricsUtils.registerCertExpiryGauges(meterRegistry);
    }

    @Override
    public UpdateControl<SconeOsvScanner> reconcile(SconeOsvScanner resource, Context<SconeOsvScanner> context) {
        reconcileCounter.increment();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        try {
            KubernetesClient client = context.getClient();
            SconeOsvScannerStatus status = buildDependantsStatus(resource, context);

            MetricsUtils.updateDependantStateGauges(dependantStateGauges, status);
            String frontAppHost = Constants.getFrontAppServiceName(resource.getMetadata().getName()) + "." + resource.getMetadata().getNamespace();

            MetricsUtils.updateCertExpiryGauges(certExpiryGauges, status.getFrontAppStatus().getState(), frontAppHost, Constants.FRONT_APP_PORT);

            if (shouldRunAutoUpdate(resource)) {
                RegistryImageVersionReader imageVersionReader = new RegistryImageVersionReaderImpl(client, resource.getMetadata().getNamespace());
                Map<DependantResourceType, RunUpdateResult> updateResults = ContainerUpdate.runContainerUpdates(
                    resource,
                    imageVersionReader,
                    // runContainerUpdates is impure and modifies the resource object directly, as such can use 'resource' as is for patch
                    () -> client.resource(resource).patch()
                );
                MetricsUtils.recordAutoUpdateMetrics(meterRegistry, updateResults);
                StatusUtils.applyUpdateResultsToStatus(status, updateResults);
                status.setLastAutoUpdateCheckTime(Common.dateToString(new Date()));
            }

            PolicyUpstreamSpec upstream = resource.getSpec().getPolicyUpstream();
            if (upstream != null) {
                PolicyUploadStatus previousUploadStatus = resource.getStatus() != null
                    ? resource.getStatus().getPolicyUploadStatus()
                    : null;

                PolicyUploadStatus newUploadStatus = previousUploadStatus;
                if (shouldRunPolicySync(previousUploadStatus, upstream)) {
                    String gitToken = resolveGitToken(client, resource.getMetadata().getNamespace(), upstream.getGitTokenSecretRef());
                    PolicySync.SyncPoliciesResult result = PolicySync.syncPolicies(
                        upstream,
                        new HttpCASClient(resource.getSpec().getCasAddress(), resource.getSpec().getCasPort()),
                        gitToken
                    );
                    MetricsUtils.recordPolicySyncMetric(meterRegistry, result);
                    newUploadStatus = StatusUtils.buildPolicyUploadStatus(upstream, result);
                }
                status.setPolicyUploadStatus(newUploadStatus);

                // if hashes changed, immediately reconcile so that dependant resource can restart if needed
                Duration reschedule = newUploadStatus.hashesChanged(previousUploadStatus)
                    ? Duration.ZERO
                    : PollUtils.minDuration(PollUtils.toDuration(upstream.getPoll()), PollUtils.toDuration(resource.getSpec().getAutoUpdatePoll()));
                return patchStatus(resource, status, reschedule);
            }

            return patchStatus(resource, status, PollUtils.toDuration(resource.getSpec().getAutoUpdatePoll()));
        } finally {
            timerSample.stop(reconcileTimer);
        }
    }

    private SconeOsvScannerStatus buildDependantsStatus(SconeOsvScanner resource, Context<SconeOsvScanner> context) {
        Set<Deployment> dependantDeployments = context.getSecondaryResources(Deployment.class);
        Set<StatefulSet> dependantStatefulSets = context.getSecondaryResources(StatefulSet.class);

        String primaryName = resource.getMetadata().getName();
        String namespace = resource.getMetadata().getNamespace();
        KubernetesClient client = context.getClient();
        SconeOsvScannerSpec spec = resource.getSpec();
        SconeOsvScannerStatus previousStatus = resource.getStatus();

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
        status.setDbManagerStatus(
            StatusUtils.buildDeploymentStatus(
                dbManager,
                spec.getScanner().getDbManager().getImageVersion(),
                previousStatus != null ? previousStatus.getDbManagerStatus() : null,
                client,
                namespace
            )
        );
        status.setFrontAppStatus(
            StatusUtils.buildDeploymentStatus(
                frontApp,
                spec.getScanner().getFrontApp().getImageVersion(),
                previousStatus != null ? previousStatus.getFrontAppStatus() : null,
                client,
                namespace
            )
        );
        status.setMaxscaleStatus(
            StatusUtils.buildDeploymentStatus(
                maxscale,
                spec.getDatabase().getMaxscale().getImageVersion(),
                previousStatus != null ? previousStatus.getMaxscaleStatus() : null,
                client,
                namespace
            )
        );
        status.setMariadbPrimaryStatus(
            StatusUtils.buildStatefulSetStatus(
                mariadbPrimary,
                spec.getDatabase().getMariadb().getImageVersion(),
                previousStatus != null ? previousStatus.getMariadbPrimaryStatus() : null,
                client,
                namespace
            )
        );
        status.setMariadbReplicaStatus(
            StatusUtils.buildStatefulSetStatus(
                mariadbReplica,
                spec.getDatabase().getMariadb().getImageVersion(),
                previousStatus != null ? previousStatus.getMariadbReplicaStatus() : null,
                client,
                namespace
            )
        );

        if (previousStatus != null) {
            status.setLastAutoUpdateCheckTime(previousStatus.getLastAutoUpdateCheckTime());
        }

        return status;
    }

    private boolean shouldRunAutoUpdate(SconeOsvScanner resource) {
        if (resource.getStatus() == null) return true;
        return PollUtils.isPollElapsed(resource.getStatus().getLastAutoUpdateCheckTime(), resource.getSpec().getAutoUpdatePoll());
    }

    private String resolveGitToken(KubernetesClient client, String namespace, SecretRef secretRef) {
        if (secretRef == null) return null;
        var data = client.secrets().inNamespace(namespace).withName(secretRef.getName()).get().getData();
        String encoded = data.get("token");
        if (encoded == null) return null;
        return new String(java.util.Base64.getDecoder().decode(encoded));
    }

    private boolean shouldRunPolicySync(PolicyUploadStatus existingStatus, PolicyUpstreamSpec upstream) {
        return existingStatus == null
            || PollUtils.isPollElapsed(existingStatus.getLastSyncTime(), upstream.getPoll())
            || !upstream.equals(existingStatus.getLastSyncedUpstream());
    }

    private UpdateControl<SconeOsvScanner> patchStatus(SconeOsvScanner resource, SconeOsvScannerStatus status, Duration reschedule) {
        resource.setStatus(status);
        return UpdateControl.patchStatus(resource).rescheduleAfter(reschedule);
    }

}