## Running the Operator
Make sure to go [through finding the images](./find-image.md) and [preparing the policies](./policies.md) first.


### Install
First step is to install the operator from helm using
TODO
```shell
helm instal ....
```

Then also make sure to [install kyverno](https://kyverno.io/docs/installation/installation/) to add cosign image validation capability. You may choose to not install noting that cosign validation will be skipped.

### Apply
You can apply the sample to get started quickly
```shell
kubectl apply -f sample/osv-scanner.yml -n <YOUR_NAMESPACE>
```

or create your own CR definition. See full list of available fields [here](./spec.md) 
