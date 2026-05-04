## Operator for PolicySigner/KBS/...

We want to generate an operator to keep the PolicySigner/KBS/... running and up-to-date:

- we configure the operator with an manifest that describes the following:

```yaml
apiversion: beta1
kind: certified-service
upstream:
git: <https link to repo>
branch: <main>
gpg:
- <verification GPG key of files> # e.g., current key
- <verification GPG key of files> # e.g., next key after key roll
poll:
period: <X min | Y hour | Z days>
```

Update the PolicySigner/KBS/... automatically if there are some upstream updates in the git repo. We assume that the git is organized as follows:

```text
manifests-0-<N>
manifests-<N+1>-<O>
manifests-<O+1>-<P>
...
```

where `manifests*` is a directory which either contains further manifests directories (but not other files) or contains a list of changes (but NO manifests* directories). We call the latter a "Leaf Node" and the former "Internal nodes".

The root node can either be a "Leaf Node" or an "Internal Node". The organization of a git repo can change without any former notice. However, the history cannot change, i.e., the publisher of the manifests promises that

A leaf node can contain SPOLs (CAS Signed Policies), K8s manifests, and request for evidence. Later, we might add more

SPOLS are ordered (strictly monotonic, no gaps):

```text
0-SPOL.yaml
1-SPOL.yaml
2-SPOL.yaml
...
```

Updates of Kubernetes manifests are also ordered in this sequence:

`3-K8sManifest.yaml` - a reference manifest that can be modified by the provider of the service

We can ask the operator to collect evidence and send to the specified CA:

`4-Evidence.yaml`

An evidence file contains the following keys:

```yaml
CA:
- url: ca.scontain.com:1234/policy-signing/v1 # URL of CA
- cert: <PEM ENCODED>

nonce:
source: CA # ask CA for nonce via REST API

evidence: # list of evidence to upload
- attestation-report # provide actual attestation report for CAS with new nonce
- 0-SPOL, # provide evidence that SPOL was created (signed upload report or signed report with policy hash)
- 2-SPOL

responses:
- SPOL # respons returns a signed policy that can be uploaded to the CAS to authorize the service

```

If there are more than one evidence files, the operator needs to upload the evidence specified in the latests evidence files (i.e., with the highest prefix number of all evidence files in the repo).


### Integrity protection of files

We want to ensure that:

- (I1) A publisher cannot change the history without the subscribers being able to prove that the publisher has changed the history.

- (I2) An adversary cannot exchange files from different repos or rename files in the same repo such that it would look like that the publisher changed the history.

- (I3) We can roll the GPG key used to sign files, i.e., we can update the keys periodically

- (I4) We can verify if the key is authorized to sign the keys.

#### Non-Repudiation

Non-repudiation is a security property that ensures a party cannot later deny having performed an action—such as sending a message, approving a transaction, or signing a document. (I1) means that all manifests must be signed by the publisher, i.e., a subscriber can store the manifests and their signatures to prove that the publisher provided this manifest.

An adversary might try to disrupt the operation of the publisher and change the the name and hence, the ordering of manifests or swap manifests between repos. Also, a malicious publisher might say that the manifest was published in a different repo.

To ensure (I1) and (I2), we use the following way to sign manifests:

- we define a `0-SPOL.yaml.asc` which uses `gpg --clearsign` to embed a yaml file into a single file:

```yaml
-----BEGIN PGP SIGNED MESSAGE-----
filename: 0-SPOL.yaml
sha256: 3b7e... # the sha256 of the file
date: YYYYMMDDHHMMSSZ # UTC - time of this signature
repo: https://github.com/myorg/myrepo # ensure this is for the correct repo
-----BEGIN PGP SIGNATURE-----
...
-----END PGP SIGNATURE-----
```

- we verify the integrity of a file `0-SPOL.yaml` by reading the detached signature file `0-SPOL.yaml.asc`
- checking that the sha256 of file `0-SPOL.yaml` matches the sha256 in the signature file
- the filename in the signature file is `0-SPOL.yaml`
- the repo is the expected repo name


In a nutshell, the file content, the file name, the repo to which is file belongs are signed. An changes of the file content, file name, or repo are detected. Files replaced by the publisher can be identified and can be shown that they were performed by the publisher.

#### GPG Authorization and GPG Key Roll

In each repo, the publisher can authorize GPG keys that can be signed by one of the authorized keys (addressing I4) with the help of a `gpg-key-authorization-<N>.yaml.asc` file that authorizes signing keys for a given time period. There can be multiple keys that are authorized at a given point in time.

```yaml
-----BEGIN PGP SIGNED MESSAGE-----
repo: https://github.com/myorg/myrepo # ensure this is for the correct repo
validity:
notBefore: YYYYMMDDHHMMSSZ # UTC
notAfter: YYYYMMDDHHMMSSZ # UTC
signers: # one or more signers
- |
-----BEGIN PGP PUBLIC KEY-----
...
-----END PGP PUBLIC KEY-----

-----END PGP SIGNATURE-----
```

Later extension:

- the publisher publishes for each content and signature a trusted timestamp (e.g, RFC 3161). One can prove that the signature happened before a given time stamp.
