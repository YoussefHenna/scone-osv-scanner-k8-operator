package com.youssefhenna.utils;

import com.youssefhenna.policy.PolicySync;
import com.youssefhenna.spec.policy.PolicyUpstreamSpec;
import com.youssefhenna.status.*;
import com.youssefhenna.updates.model.DependantResourceType;
import com.youssefhenna.updates.model.RunUpdateResult;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StatusUtils {

    public static DependantStatus buildDeploymentStatus(Deployment deployment, String targetVersion, DependantStatus previous, KubernetesClient client, String namespace) {
        if (deployment == null || deployment.getStatus() == null) {
            return buildDependantStatus(DependantState.STARTING, null, targetVersion, previous);
        }
        DependantState state = resolveReplicaState(deployment.getStatus().getReadyReplicas(), deployment.getStatus().getReplicas());
        String currentVersion = resolveRunningVersion(client, namespace, deployment.getSpec().getSelector().getMatchLabels());
        return buildDependantStatus(state, currentVersion, targetVersion, previous);
    }

    public static DependantStatus buildStatefulSetStatus(StatefulSet statefulSet, String targetVersion, DependantStatus previous, KubernetesClient client, String namespace) {
        if (statefulSet == null || statefulSet.getStatus() == null) {
            return buildDependantStatus(DependantState.STARTING, null, targetVersion, previous);
        }
        DependantState state = resolveReplicaState(statefulSet.getStatus().getReadyReplicas(), statefulSet.getStatus().getReplicas());
        String currentVersion = resolveRunningVersion(client, namespace, statefulSet.getSpec().getSelector().getMatchLabels());
        return buildDependantStatus(state, currentVersion, targetVersion, previous);
    }


    private static DependantStatus buildDependantStatus(DependantState state, String currentVersion, String targetVersion, DependantStatus previous) {
        DependantStatus status = new DependantStatus(state, currentVersion);
        status.setTargetVersion(targetVersion);
        if (previous != null) {
            status.setLastUpdateStatus(previous.getLastUpdateStatus());
        }
        return status;
    }

    private static String resolveRunningVersion(KubernetesClient client, String namespace, Map<String, String> labelSelector) {
        return client.pods()
            .inNamespace(namespace)
            .withLabels(labelSelector)
            .list()
            .getItems()
            .stream()
            .filter(
                pod -> pod.getStatus() != null
                && pod.getStatus().getContainerStatuses() != null
                && !pod.getStatus().getContainerStatuses().isEmpty()
                && Boolean.TRUE.equals(pod.getStatus().getContainerStatuses().getFirst().getReady())
            )
            .findFirst()
            .map(pod -> Common.extractImageVersion(pod.getSpec().getContainers().getFirst().getImage()))
            .orElse(null);
    }

    private static DependantState resolveReplicaState(Integer ready, Integer total) {
        if (ready != null && ready.equals(total) && total > 0) {
            return DependantState.RUNNING;
        }
        if (ready != null && total != null && ready < total) {
            return DependantState.FAILING;
        }
        return DependantState.STARTING;
    }

    public static void applyUpdateResultsToStatus(SconeOsvScannerStatus status, Map<DependantResourceType, RunUpdateResult> results) {
        applyUpdateResultToStatus(status.getFrontAppStatus(), results.get(DependantResourceType.FRONT_APP));
        applyUpdateResultToStatus(status.getDbManagerStatus(), results.get(DependantResourceType.DB_MANAGER));
        applyUpdateResultToStatus(status.getMaxscaleStatus(), results.get(DependantResourceType.MAXSCALE));

        RunUpdateResult mariadbResult = results.get(DependantResourceType.MARIADB);
        applyUpdateResultToStatus(status.getMariadbPrimaryStatus(), mariadbResult);
        applyUpdateResultToStatus(status.getMariadbReplicaStatus(), mariadbResult);
    }

    private static void applyUpdateResultToStatus(DependantStatus depStatus, RunUpdateResult result) {
        if (depStatus == null || result == null) return;
        depStatus.setLastUpdateStatus(result.getLastUpdateStatus());
    }

    public static PolicyUploadStatus buildPolicyUploadStatus(PolicyUpstreamSpec upstream, PolicySync.SyncPoliciesResult result) {
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

}