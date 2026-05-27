# Spec Reference

Full field reference for the `SconeOsvScanner` custom resource.

## Root

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `registryUrl` | `string` | No | — | Default container registry URL applied to all sub-components |
| `registryCredentials` | [`RegistryCredentials`](#registrycredentials) | No | — | Default registry credentials applied to all sub-components |
| `casAddress` | `string` | **Yes** | — | SCONE CAS hostname or IP |
| `casPort` | `int` | No | `8081` | SCONE CAS port |
| `scanner` | [`ScannerSpec`](#scanner) | **Yes** | — | Configuration for the scanner application components |
| `database` | [`DatabaseSpec`](#database) | **Yes** | — | Configuration for the database tier |
| `policyUpstream` | [`PolicyUpstreamSpec`](#policyupstream) | No | — | Git upstream for Kyverno policy sync |
| `autoUpdatePoll` | [`PollConfig`](#pollconfig) | No | `every: 10, unit: MINUTES` | How often to check for component image updates |

---

## scanner

Configures the OSV scanner application components. Registry fields here override root-level defaults for scanner components.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `registryUrl` | `string` | No | — | Registry URL override for scanner components |
| `registryCredentials` | [`RegistryCredentials`](#registrycredentials) | No | — | Registry credentials override for scanner components |
| `dbManager` | [`DbManagerSpec`](#scannerdbmanager) | **Yes** | — | DB manager sidecar configuration |
| `frontApp` | [`FrontAppSpec`](#scannerfrontapp) | **Yes** | — | Front-facing scanner application configuration |

### scanner.dbManager

Inherits all [CommonDependantSpec](#commondependantspec) fields. No additional fields.

### scanner.frontApp

Inherits all [CommonDependantSpec](#commondependantspec) fields, plus:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `replicas` | `int` | No | `1` | Number of front app replicas |

---

## database

Configures the database tier. Registry fields here override root-level defaults for database components.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `registryUrl` | `string` | No | — | Registry URL override for database components |
| `registryCredentials` | [`RegistryCredentials`](#registrycredentials) | No | — | Registry credentials override for database components |
| `maxscale` | [`MaxscaleSpec`](#databasemaxscale) | **Yes** | — | MaxScale proxy configuration |
| `mariadb` | [`MariadbSpec`](#databasemariadb) | **Yes** | — | MariaDB configuration |

### database.maxscale

Inherits all [CommonDependantSpec](#commondependantspec) fields, plus:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `replicas` | `int` | No | `1` | Number of MaxScale replicas |

### database.mariadb

Inherits all [CommonDependantSpec](#commondependantspec) fields, plus:

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `storageSize` | `string` | **Yes** | — | PVC storage size (e.g. `10Gi`) |
| `storageClassName` | `string` | **Yes** | — | Storage class to use for the PVC |
| `disablePersistence` | `bool` | No | `false` | Skip PVC creation; data is lost on pod restart |
| `replicas` | `int` | No | `1` | Number of MariaDB replicas |
| `replicaSconeConfigId` | `string` | **Yes** | — | SCONE config ID used by replica pods |

---

## policyUpstream

Configures a Git repository as the source of SCONE policies, polled on a schedule.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `gitUrl` | `string` | **Yes** | — | Git repository URL |
| `gitTokenSecretRef` | [`SecretRef`](#secretref) | No | — | K8s secret containing the Git auth token (for private repos) |
| `branch` | `string` | **Yes** | — | Branch to track |
| `gpgKeys` | `string[]` | **Yes** | — | GPG public keys used to verify signed commits |
| `poll` | [`PollConfig`](#pollconfig) | **Yes** | — | How often to poll the upstream for changes |

---

## CommonDependantSpec

Shared fields inherited by `scanner.dbManager`, `scanner.frontApp`, `database.maxscale`, and `database.mariadb`. Registry fields at this level override their parent section's values.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `registryUrl` | `string` | No | — | Registry URL override for this specific component |
| `registryCredentials` | [`RegistryCredentials`](#registrycredentials) | No | — | Registry credentials override for this specific component |
| `imageName` | `string` | **Yes** | — | Container image name (without tag) |
| `imageVersion` | `string` | **Yes** | — | Container image tag or version |
| `memory` | `string` | **Yes** | — | Memory resource request/limit (e.g. `512Mi`) |
| `sconeConfigId` | `string` | **Yes** | — | SCONE session config ID for this component |
| `autoUpdate` | `bool` | No | `false` | Automatically redeploy when a new image version is detected |
| `cosignPublicKey` | `string` | No | — | Cosign public key for image signature verification |

---

## PollConfig

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `every` | `int` | **Yes** | — | Polling interval value |
| `unit` | `enum` | **Yes** | — | Polling interval unit: `DAYS`, `HOURS`, `MINUTES`, `SECONDS` |

## RegistryCredentials

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `secretRef` | [`SecretRef`](#secretref) | No | — | Reference to a K8s secret containing registry credentials |

## SecretRef

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | `string` | No | — | Name of the Kubernetes secret in the same namespace |