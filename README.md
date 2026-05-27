# SCONE OSV Scanner K8 Operator
A Kubernetes operator to run a reliable, confidential, and verifiable SCONE vulnerability scanner. Depends on [the scone-osv-scan project](https://github.com/scontain-gmbh/scone-osv-scan) and the [SCONE framework](https://sconedocs.github.io/latest/).  

### Features
- **Operates Full Deployment of the SCONE OSV Scanner**
  - Load balanced version of the scanner
  - MariaDB primary + replica(s) with Maxscale proxy
  - Configurable replica counts and resource allowances
  - Configurable image sources and registries
- **SCONE Policy Upload**
  - Configurable to handle signed SCONE policy upload automatically
  - Restarts dependant services on policy change
- **Image Updates**
  - Configurable to auto update to latest image versions of the services
  - Validates Cosign image signatures before updating
  - Rolling no-downtime updates
- **Observability & Metrics**
  - Provides detailed status on all running services and scheduled tasks
  - Exposes a Prometheus compatible metrics endpoint
- **Confidential Verification**
  -  CA backed certificate for verification service is running confidentially with the correct SCONE policy

### Getting Started
- [Finding & preparing your container images](./docs/find-image.md)
- [Setting up SCONE policies](./docs/policies.md)
- [Setting up CA](./docs/ca.md)
- [Installing and running](./docs/running.md)
- [Spec definition](./docs/spec.md)
- [Observability and metrics](./docs/observability.md)



## Development

This project uses Quarkus, if you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

### Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

If you have namespace limited access over the cluster you may also run this to have quarkus only watch a specific namespace.

```shell script
WATCH_NAMESPACE=<NAMESPACE> ./mvnw quarkus:dev
```

### Building & Deploying

Releases are published automatically via the [Release workflow](.github/workflows/release.yml) when a GitHub release is created. See [here](./docs/running.md) for install instructions.

### Related Guides

- Operator SDK ([guide](https://docs.quarkiverse.io/quarkus-operator-sdk/dev/index.html)): Quarkus extension for the Java Operator SDK (https://javaoperatorsdk.io)


