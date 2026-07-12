## Observability

### Status

The operator writes a full status object back to the `SconeOsvScanner` CR on every reconcile.

#### Top-level fields

| Field | Type | Description                                                                                                     |
|-------|------|-----------------------------------------------------------------------------------------------------------------|
| `dbManagerStatus` | [`DbManagerStatus`](#dbmanagerstatus) | Current state of the DB manager deployment, along with with status exposed through manager's `/status` endpoint |
| `frontAppStatus` | [`DependantStatus`](#dependantstatus) | Current state of the front app deployment                                                                       |
| `maxscaleStatus` | [`DependantStatus`](#dependantstatus) | Current state of the MaxScale deployment                                                                        |
| `mariadbPrimaryStatus` | [`DependantStatus`](#dependantstatus) | Current state of the MariaDB primary pod                                                                        |
| `mariadbReplicaStatus` | [`DependantStatus`](#dependantstatus) | Current state of the MariaDB replica pod(s)                                                                     |
| `policyUploadStatus` | [`PolicyUploadStatus`](#policyuploadstatus) | Last policy sync result (only present when `policyUpstream` is configured)                                      |
| `lastAutoUpdateCheckTime` | `string` | timestamp of the most recent auto-update check                                                                  |

#### DependantStatus

Shared structure used by all five component status fields above.

| Field | Type | Description |
|-------|------|-------------|
| `state` | `enum` | Current lifecycle state: `RUNNING`, `STARTING`, `FAILING` |
| `currentVersion` | `string` | Image tag currently running |
| `targetVersion` | `string` | Image tag the operator intends to roll to (set during an update) |
| `lastUpdateStatus` | `enum` | Result of the last auto-update attempt: `SUCCESS_UPDATED`, `SKIPPED_ALREADY_UPTODATE`, `FAILED_HIGHEST_VERSION_NOT_FOUND`, `FAILED_UNKNOWN_ERROR` |

#### DbManagerStatus

Extends [`DependantStatus`](#dependantstatus) (all fields above apply) with the DB manager's own status, fetched from its internal `/status` endpoint (`http://<db-manager-service>.<namespace>:8080/status`). These extra fields are only populated when the DB manager is `RUNNING` and reachable in-cluster; when the endpoint is unreachable (e.g. running the operator locally) only the inherited `DependantStatus` fields are set.

| Field | Type | Description |
|-------|------|-------------|
| `status` | `string` | Manager-reported health: `healthy`, `degraded`, `error` |
| `dbLastUpdate` | `string` | Timestamp of the last successful vulnerability database update |
| `dbVulnerabilityCount` | `int` | Number of vulnerabilities currently in the database |
| `cacheSbomCount` | `int` | Number of cached SBOM hash entries |
| `uptimeSeconds` | `int` | Seconds since the DB manager process started |
| `currentUpdate` | [`UpdateStatus`](#updatestatus) | Progress of an in-flight database update; omitted when idle |

#### UpdateStatus

Progress of in-progress database update, present only while an update is running.

| Field | Type | Description |
|-------|------|-------------|
| `status` | `string` | Update phase: `idle`, `downloading`, `processing`, `completed`, `error` |
| `startTime` | `string` | Timestamp the current update started |
| `lastUpdateTime` | `string` | Timestamp of the most recent progress update |
| `currentEcosystem` | `string` | Ecosystem currently being downloaded |
| `ecosystemProgress` | `string` | Ecosystem download progress, e.g. `3/10` |
| `processingFiles` | `string` | File processing progress, e.g. `1234/5000` |
| `progressPercent` | `double` | Overall progress percentage |
| `message` | `string` | Human-readable status message |
| `error` | `string` | Error detail when `status` is `error` |

**Example** (`kubectl describe sconeosvscanner`, DB manager running with an update in progress):

```
Db Manager Status:
    State:                   RUNNING
    Current Version:         1.0.0
    Target Version:          1.0.0
    Last Update Status:      SKIPPED_ALREADY_UPTODATE
    Status:                  healthy
    Db Last Update:          2026-07-12T02:14:33Z
    Db Vulnerability Count:  284531
    Cache Sbom Count:        112
    Uptime Seconds:          86452
    Current Update:
      Status:              processing
      Start Time:          2026-07-12T02:10:01Z
      Last Update Time:    2026-07-12T02:14:33Z
      Ecosystem Progress:  8/10
      Processing Files:    4200/5000
      Progress Percent:    84
      Message:             Processing vulnerability files
```

When idle (no update running) `Current Update` is omitted:

```
Db Manager Status:
    State:                   RUNNING
    Current Version:         1.0.0
    Target Version:          1.0.0
    Last Update Status:      SKIPPED_ALREADY_UPTODATE
    Status:                  healthy
    Db Last Update:          2026-07-12T02:14:33Z
    Db Vulnerability Count:  284531
    Cache Sbom Count:        112
    Uptime Seconds:          90112
```

#### PolicyUploadStatus

| Field | Type | Description |
|-------|------|-------------|
| `lastRunStatus` | `enum` | Overall result of the last sync run: `SUCCESSFUL`, `FAILED` |
| `lastSyncTime` | `string` |  timestamp of the last sync attempt |
| `lastSyncedUpstream` | `object` | Snapshot of the upstream spec (`gitUrl`, `branch`, `gpgKeys`) that was active during the last sync |
| `policyUpdateStatuses` | `map<string, PolicyUploadStatusItem>` | Per-policy results, keyed by policy name |

#### PolicyUploadStatusItem

| Field | Type | Description |
|-------|------|-------------|
| `name` | `string` | Policy name |
| `lastFile` | `string` | Path of the policy file that was last processed |
| `lastHash` | `string` | SHA hash of the last successfully uploaded policy content |
| `lastState` | `enum` | Result of the last upload attempt for this policy (see values below) |
| `lastStateUpdate` | `string` |  timestamp when `lastState` was last written |

`lastState` values:

| Value | Meaning |
|-------|---------|
| `SUCCESS_UPLOADED` | Policy was uploaded to CAS successfully |
| `SKIPPED_CAS_ALREADY_UPTODATE` | CAS already holds an identical session; no upload needed |
| `FAILED_INVALID_SPOL` | The SPOL file failed validation before upload |
| `FAILED_READ_EXISTING_FORBIDDEN` | Operator lacks permission to read the existing CAS session |
| `FAILED_UNKNOWN_READ_EXISTING_FAILURE` | Unexpected error reading the existing CAS session |
| `FAILED_UNKNOWN_CAS_ERROR` | Unexpected CAS error during upload |
| `FAILED_SESSION_COMPARISON_FAILURE` | Could not compare the existing session with the new one |
| `FAILED_NAMESPACE_NOT_FOUND` | The CAS namespace for this policy does not exist |
| `FAILED_SIGNER_NOT_AUTHORIZED` | The operator's signing key is not authorized on this CAS namespace |
| `FAILED_PREDECESSOR_CONFLICT` | Upload rejected due to a session predecessor conflict |
| `FAILED_UNKNOWN_UPLOAD_SESSION_FAILURE` | Unexpected error during the upload step |

---

### Prometheus Metrics

The operator exposes metrics via the endpoint at `/q/metrics`

The Helm chart creates a `ClusterIP` Service named `scone-osv-scanner-k8-operator` on port `8080`. The service is annotated with standard Prometheus scrape annotations (`prometheus.io/scrape`, `prometheus.io/path`, `prometheus.io/scheme`), which annotation-based scrapers pick up automatically.

If you use the Prometheus Operator, create a `ServiceMonitor` pointing at the service.


All metrics carry `name` and `namespace` labels identifying the `SconeOsvScanner` CR they belong to, allowing multiple CR instances to be monitored independently.

#### Counters

| Metric | Labels | Description |
|--------|--------|-------------|
| `operator_reconcile_count_total` | `name`, `namespace` | Total number of reconcile loop invocations |
| `operator_autoupdate_run_total` | `name`, `namespace`, `resource`, `status` | Incremented on each auto-update check per component |
| `operator_policy_sync_run_total` | `name`, `namespace`, `status` | Incremented on each policy sync run |

**`operator_autoupdate_run_total` label values**

`resource`: `FRONT_APP`, `DB_MANAGER`, `MARIADB`, `MAXSCALE`

`status`: `SUCCESS_UPDATED`, `SKIPPED_ALREADY_UPTODATE`, `FAILED_HIGHEST_VERSION_NOT_FOUND`, `FAILED_UNKNOWN_ERROR`

**`operator_policy_sync_run_total` label values**

`status`: `SUCCESSFUL`, `FAILED`

#### Timers

| Metric | Labels | Description |
|--------|--------|-------------|
| `operator_reconcile_duration_seconds` | `name`, `namespace` | Latency of each reconcile call (exposes `_count`, `_sum`, `_max` in Prometheus format) |

#### Gauges

| Metric | Labels | Values | Description |
|--------|--------|--------|-------------|
| `operator_dependant_state` | `name`, `namespace`, `dependant` | `1`, `0`, `-1` | Component health: `1`=RUNNING, `0`=STARTING, `-1`=FAILING |
| `osv_cert_expiry_seconds` | `name`, `namespace` | seconds or `-1` | Seconds until the front app TLS certificate expires; `-1` when the service is unreachable |
| `osv_cert_expiry_warning` | `name`, `namespace` | `1`, `0`, `-1` | `1` if within the final ⅓ of certificate validity, `0` if not, `-1` when service is unreachable |

**`operator_dependant_state` label values**

`dependant`: `FRONT_APP`, `DB_MANAGER`, `MARIADB`, `MAXSCALE`

> For MariaDB, the gauge reflects the worst state between primary and replica pods.