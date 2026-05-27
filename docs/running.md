## Running the Operator
Make sure to go [through finding the images](./find-image.md) and [preparing the policies](./policies.md) first.


### Install

```shell
helm install scone-osv-scanner-k8-operator \
  https://github.com/YoussefHenna/scone-osv-scanner-k8-operator/releases/download/v<version>/scone-osv-scanner-k8-operator.tgz
```

By default the operator watches all namespaces. To restrict it to a specific namespace:

```shell
helm install scone-osv-scanner-k8-operator \
  https://github.com/YoussefHenna/scone-osv-scanner-k8-operator/releases/download/v<version>/scone-osv-scanner-k8-operator.tgz \
  --set app.envs.WATCH_NAMESPACE=<your-namespace>
```

Then also make sure to [install kyverno](https://kyverno.io/docs/installation/installation/) to add cosign image validation capability. You may choose to not install noting that cosign validation will be skipped.


### Upgrade

`helm upgrade` does not update CRDs. Apply any CRD changes manually first, then upgrade:

```shell
tar -xzf scone-osv-scanner-k8-operator.tgz scone-osv-scanner-k8-operator/crds/
kubectl apply -f scone-osv-scanner-k8-operator/crds/

helm upgrade scone-osv-scanner-k8-operator \
  https://github.com/YoussefHenna/scone-osv-scanner-k8-operator/releases/download/v<version>/scone-osv-scanner-k8-operator.tgz
```


### Apply
You can apply the sample to get started quickly
```shell
kubectl apply -f sample/osv-scanner.yml -n <YOUR_NAMESPACE>
```

or create your own CR definition. See full list of available fields [here](./spec.md) 
