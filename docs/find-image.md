## Getting the images
This operator runs 3 services, each requiring an image source. The [sample](./sample/osv-scanner.yml) is already setup with images, you may skip this step and use these images; however you need to have access to the scone.cloud and sconecuratedimages registries.
### OSV Scanner
This is the core scanner service and needs 2 images, the 'front app' and the 'db manager'. Source lives in https://github.com/scontain-gmbh/scone-osv-scan. You may build your own images or use one of the available deployed versions.

#### SCONTAIN Hosted (recommended)
Contact the SCONE team for access. This is official recommended release
https://gitlab.scontain.com/scone.cloud/sos-images/container_registry

#### Test Version
These are publically available versions meant for testing this operator. But may not be up to date.
https://hub.docker.com/repository/docker/youssefhenna/sos-dbmanager
https://hub.docker.com/repository/docker/youssefhenna/sos-osvscan


### Database
You need images for a confidential version of mariadb and maxscale.

#### MariaDB
Contact the SCONE team for access. SCONE also offers other mariadb images that may be used as well
https://gitlab.scontain.com/scone.cloud/mariadb-11-alpine/container_registry

#### Maxscale
Contact the SCONE team for access. SCONE also offers other mariadb images that may be used as well
https://gitlab.scontain.com/scone.cloud/mariadb-11-alpine/container_registry

#### Setting the Images
The images can then be set in the CR definition as follows
```yaml
apiVersion: youssefhenna.com/v1
kind: SconeOsvScanner
metadata:
  name: osv-scanner-sample
spec:
  scanner:
    registryUrl: ...
    dbManager:
      imageName: ...
      imageVersion: ...
      ...
    frontApp:
      imageName: ...
      imageVersion: ...
      ...
  database:
    registryUrl: ...
    registryCredentials:
      secretRef:
        name: ...
    maxscale:
      imageName: ...
      imageVersion: ...
      ...
    mariadb:
      imageName: ...
      imageVersion: ...
      ...
```
Note the registry config can be at any level. The nearest registry config is the one to be used. Each level where it is defined, overrides the inherited value.
