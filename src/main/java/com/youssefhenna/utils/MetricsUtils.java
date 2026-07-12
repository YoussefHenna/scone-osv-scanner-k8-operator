package com.youssefhenna.utils;

import com.youssefhenna.policy.PolicySync;
import com.youssefhenna.status.DependantState;
import com.youssefhenna.status.SconeOsvScannerStatus;
import com.youssefhenna.updates.model.DependantResourceType;
import com.youssefhenna.updates.model.RunUpdateResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.logging.Log;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsUtils {

    public record CertExpiryGauges(AtomicLong expirySeconds, AtomicInteger warningFlag) {
    }

    public record ResourceMetrics(
        Counter reconcileCounter,
        Timer reconcileTimer,
        Map<DependantResourceType, AtomicInteger> dependantStateGauges,
        CertExpiryGauges certExpiryGauges
    ) {
    }

    private static final Map<DependantState, Integer> DEPENDANT_STATE_VALUE = Map.of(
        DependantState.RUNNING, 1,
        DependantState.STARTING, 0,
        DependantState.FAILING, -1
    );

    public static ResourceMetrics createResourceMetrics(MeterRegistry registry, String name, String namespace) {
        return new ResourceMetrics(
            Counter.builder("operator.reconcile.count")
                .tag("name", name)
                .tag("namespace", namespace)
                .description("Total number of reconcile calls")
                .register(registry),
            Timer.builder("operator.reconcile.duration")
                .tag("name", name)
                .tag("namespace", namespace)
                .description("Duration of each reconcile call")
                .register(registry),
            registerDependantStateGauges(registry, name, namespace),
            registerCertExpiryGauges(registry, name, namespace)
        );
    }

    private static Map<DependantResourceType, AtomicInteger> registerDependantStateGauges(MeterRegistry registry, String name, String namespace) {
        Map<DependantResourceType, AtomicInteger> states = new EnumMap<>(DependantResourceType.class);
        for (DependantResourceType type : DependantResourceType.values()) {
            AtomicInteger value = new AtomicInteger(0);
            Gauge.builder("operator.dependant.state", value, AtomicInteger::get)
                .tag("name", name)
                .tag("namespace", namespace)
                .tag("dependant", type.name())
                .description("State of dependant: 1=RUNNING, 0=STARTING, -1=FAILING")
                .register(registry);
            states.put(type, value);
        }
        return states;
    }

    private static CertExpiryGauges registerCertExpiryGauges(MeterRegistry registry, String name, String namespace) {
        AtomicLong expirySeconds = new AtomicLong(-1);
        AtomicInteger warningFlag = new AtomicInteger(-1);
        Gauge.builder("osv.cert.expiry.seconds", expirySeconds, AtomicLong::get)
            .tag("name", name)
            .tag("namespace", namespace)
            .description("Seconds until the front app TLS certificate expires, -1 if service unavailable")
            .register(registry);
        Gauge.builder("osv.cert.expiry.warning", warningFlag, AtomicInteger::get)
            .tag("name", name)
            .tag("namespace", namespace)
            .description("1 if within the final 1/3 of certificate validity, 0 if not, -1 if service unavailable")
            .register(registry);
        return new CertExpiryGauges(expirySeconds, warningFlag);
    }

    public static void updateDependantStateGauges(Map<DependantResourceType, AtomicInteger> states, SconeOsvScannerStatus status) {
        updateDependantState(states, DependantResourceType.DB_MANAGER, status.getDbManagerStatus().getState());
        updateDependantState(states, DependantResourceType.FRONT_APP, status.getFrontAppStatus().getState());
        updateDependantState(states, DependantResourceType.MAXSCALE, status.getMaxscaleStatus().getState());
        // mariadb has separate primary/replica statuses, show the worst of the two
        updateDependantState(states, DependantResourceType.MARIADB, worstState(
            status.getMariadbPrimaryStatus() != null ? status.getMariadbPrimaryStatus().getState() : null,
            status.getMariadbReplicaStatus() != null ? status.getMariadbReplicaStatus().getState() : null
        ));
    }

    private static void updateDependantState(Map<DependantResourceType, AtomicInteger> states, DependantResourceType type, DependantState state) {
        if (state == null) return;
        states.get(type).set(DEPENDANT_STATE_VALUE.get(state));
    }

    private static DependantState worstState(DependantState a, DependantState b) {
        if (a == null) return b;
        if (b == null) return a;
        return DEPENDANT_STATE_VALUE.get(a) <= DEPENDANT_STATE_VALUE.get(b) ? a : b;
    }

    public static void updateCertExpiryGauges(CertExpiryGauges gauges, DependantState frontAppState, String host, int port) {
        if (frontAppState != DependantState.RUNNING) {
            gauges.expirySeconds().set(-1);
            gauges.warningFlag().set(-1);
            return;
        }
        try {
            CertChecker.CertInfo cert = CertChecker.checkFirstCert(host, port);
            long now = System.currentTimeMillis();
            long notAfterMs = cert.notAfter().getTime();
            long totalValidityMs = notAfterMs - cert.notBefore().getTime();
            long warningThresholdMs = notAfterMs - (totalValidityMs / 3);

            gauges.expirySeconds().set((notAfterMs - now) / 1000);
            gauges.warningFlag().set(now >= warningThresholdMs ? 1 : 0);
        } catch (Exception e) {
            // Always fails on local dev. Locally running operator cannot access front app through K8 hostname
            Log.warn("Failed to check front app certificate: " + e.getMessage());
            gauges.expirySeconds().set(-1);
            gauges.warningFlag().set(-1);
        }
    }

    public static void recordAutoUpdateMetrics(MeterRegistry registry, String name, String namespace, Map<DependantResourceType, RunUpdateResult> results) {
        results.forEach((type, result) ->
            registry.counter("operator.autoupdate.run",
                "name", name,
                "namespace", namespace,
                "resource", type.name(),
                "status", result.getLastUpdateStatus().name()
            ).increment()
        );
    }

    public static void recordPolicySyncMetric(MeterRegistry registry, String name, String namespace, PolicySync.SyncPoliciesResult result) {
        registry.counter("operator.policy_sync.run",
            "name", name,
            "namespace", namespace,
            "status", result.overallStatus().name()
        ).increment();
    }
}
