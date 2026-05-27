## Observability

### Status

The operator writes a full status object back to the `SconeOsvScanner` CR on every reconcile.

#### Top-level fields

| Field | Type | Description |
|-------|------|-------------|
| `dbManagerStatus` | [`DependantStatus`](#dependantstatus) | Current state of the DB manager deployment |
| `frontAppStatus` | [`DependantStatus`](#dependantstatus) | Current state of the front app deployment |
| `maxscaleStatus` | [`DependantStatus`](#dependantstatus) | Current state of the MaxScale deployment |
| `mariadbPrimaryStatus` | [`DependantStatus`](#dependantstatus) | Current state of the MariaDB primary pod |
| `mariadbReplicaStatus` | [`DependantStatus`](#dependantstatus) | Current state of the MariaDB replica pod(s) |
| `policyUploadStatus` | [`PolicyUploadStatus`](#policyuploadstatus) | Last policy sync result (only present when `policyUpstream` is configured) |
| `lastAutoUpdateCheckTime` | `string` |  timestamp of the most recent auto-update check |

#### DependantStatus

Shared structure used by all five component status fields above.

| Field | Type | Description |
|-------|------|-------------|
| `state` | `enum` | Current lifecycle state: `RUNNING`, `STARTING`, `FAILING` |
| `currentVersion` | `string` | Image tag currently running |
| `targetVersion` | `string` | Image tag the operator intends to roll to (set during an update) |
| `lastUpdateStatus` | `enum` | Result of the last auto-update attempt: `SUCCESS_UPDATED`, `SKIPPED_ALREADY_UPTODATE`, `FAILED_HIGHEST_VERSION_NOT_FOUND`, `FAILED_UNKNOWN_ERROR` |

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

The operator exposes metrics via the standard Micrometer/Quarkus endpoint (`/q/metrics`).

#### Counters

| Metric | Labels | Description |
|--------|--------|-------------|
| `operator_reconcile_count_total` | — | Total number of reconcile loop invocations |
| `operator_autoupdate_run_total` | `resource`, `status` | Incremented on each auto-update check per component |
| `operator_policy_sync_run_total` | `status` | Incremented on each policy sync run |

**`operator_autoupdate_run_total` label values**

`resource`: `FRONT_APP`, `DB_MANAGER`, `MARIADB`, `MAXSCALE`

`status`: `SUCCESS_UPDATED`, `SKIPPED_ALREADY_UPTODATE`, `FAILED_HIGHEST_VERSION_NOT_FOUND`, `FAILED_UNKNOWN_ERROR`

**`operator_policy_sync_run_total` label values**

`status`: `SUCCESSFUL`, `FAILED`

#### Timers

| Metric | Labels | Description |
|--------|--------|-------------|
| `operator_reconcile_duration_seconds` | — | Latency of each reconcile call (exposes `_count`, `_sum`, `_max` in Prometheus format) |

#### Gauges

| Metric | Labels | Values | Description |
|--------|--------|--------|-------------|
| `operator_dependant_state` | `dependant` | `1`, `0`, `-1` | Component health: `1`=RUNNING, `0`=STARTING, `-1`=FAILING |
| `osv_cert_expiry_seconds` | — | seconds or `-1` | Seconds until the front app TLS certificate expires; `-1` when the service is unreachable |
| `osv_cert_expiry_warning` | — | `1`, `0`, `-1` | `1` if within the final ⅓ of certificate validity, `0` if not, `-1` when service is unreachable |

**`operator_dependant_state` label values**

`dependant`: `FRONT_APP`, `DB_MANAGER`, `MARIADB`, `MAXSCALE`

> For MariaDB, the gauge reflects the worst state between primary and replica pods.