## SCONE Policies
The policies in the [sample](./sample/policies) are setup for the full flow and should be used as a starting point. You should update to another namespace, but the core of the polices should mostly stay the same to guarantee correct behavior.


### Policy Upstream (recommended)
The recommend way for policy upload is through setting up a policy upstream as follows. This allows the operator to query the signed policies and upload them to the CAS. Since the operator controls the flow, it can react and restart the appropriate services when policies change. [See here](./policy-upstream.md) on how to setup an upstream repo.
```yaml
apiVersion: youssefhenna.com/v1
kind: SconeOsvScanner
metadata:
  name: osv-scanner-sample
spec:
  casAddress: ...
  policyUpstream:
    gitUrl: ...
    branch: ...
    gpgKeys:
      - ...
    poll:
      every: 10
      unit: MINUTES
```

### Manual Policy Upload
You may choose to upload these policies to the CAS [manually with the cli](https://sconedocs.github.io/latest/CAS_cli-intro/). You have to make sure to then make sure external policy changes do not break the existing deployment, as services will not auto restart on policy change.

### Policy Environment Variables
The provided sample policies rely on several environment variables that you need to define:

**MRENCLAVE Values** :
- `MRENCLAVE_SOS_DB_MANAGER`
- `MRENCLAVE_SOS_OSV_SCAN`
- `MRENCLAVE_MAXSCALE`
- `MRENCLAVE_MYSQL`
- `MRENCLAVE_MYSQLD` 
- `MRENCLAVE_PROBE`

These can be obtained using [this script](../scripts/determine-mrenclaves.sh). The script accepts envs for the image sources and for heap/memory you'll be using for each service. When running with no envs set, will use the defaults as defined in [the sample](../sample/osv-scanner.yml); this will not match the correct MRENCLAVE if you're using different images or setting different memory values.

**Kubernetes service names**:
- `MAXSCALE_SERVICE_NAME`: The operator deploys this at `<YOUR_CHOSEN_CR_NAME>-maxscale-service`
- `MARIADB_PRIMARY_SERVICE_NAME`: The operator deploys this at `<YOUR_CHOSEN_CR_NAME>-mariadb-primary-service`
- `MARIADB_REPLICA_SERVICE_NAME`: The operator deploys this at `<YOUR_CHOSEN_CR_NAME>-mariadb-replica-service`

**Configuration:**
- `CAS_ADDRESS`: The CAS address where the policies live
- `CA_REQUEST_CERT_URL`: URL of the CA for issuing of a certificate. If deploying the [sample CA](../sample/session-ca.yml), the value is `https://scone-session-ca:8443/issue-certificate`
  - To not use CA based certificates, update the [osv-scan](../sample/policies/osv-scan.yml) policy to uncomment the lines for direct certificate injection instead.
- `OSV_ECOSYSTEMS_LIST`: URL that points to an OSV ecosystem list. Use an empty value or `https://osv-vulnerabilities.storage.googleapis.com/ecosystems.txt` to default to all ecosystems.