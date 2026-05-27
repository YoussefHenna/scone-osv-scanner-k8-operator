## Session CA
The OSV scanner can retrieve a certificate from a CA that matches the process describe here: https://github.com/YoussefHenna/scone-session-ca. Read through the README at this repo to fully understand the process and guarantees it provides/lacks.

Note that you need to ensure you have an image that supports this updated logic:
- Available in the given test images at https://hub.docker.com/repository/docker/youssefhenna/sos-dbmanager
  & https://hub.docker.com/repository/docker/youssefhenna/sos-osvscan
- Should be available on the official SCONE deployments and any self built images given that [this PR](https://github.com/scontain-gmbh/scone-osv-scan/pull/152) has been merged into the main codebase.


### Sample CA
A sample CA is available to deploy on your K8 cluster using [this manifest](../sample/session-ca.yml). However, this is merely a proof of concept, a real CA should be external and trusted. But this is sufficient for testing. Once the CA is up, make sure to set the `CA_REQUEST_CERT_URL` in the policy as [describe here](./policies.md)
```shell
kubectl apply -f sample/session-ca.yml -n <YOUR_NAMESPACE>
```

### Opt Out CA
You may choose to use a standard SCONE generated certificate instead of a CA generated one. For that make updates to the [policy here](../sample/policies/osv-scan.yml), uncomment the certificate generation and comment out the CA challenge attributes and envs.