## Policy Upstream Repo

See https://github.com/YoussefHenna/scone-osv-scanner-policies for a sample of an upstream repo.


The upstream repo is a git repo that includes signed policies with GPG backed signatures that prevent malicious behaviors.

### Creating the signed policies
Using the provided [script](../scripts/create-signed-session.sh), can be called as follows:
```shell
create-signed-session.sh <VERSION> <POLICY_YML> <REPO_URL> <GPG_PRIVATE_KEY_ID>
```
for example:
```shell
create-signed-session.sh 0 osv-scan.yml https://github.com/YoussefHenna/scone-osv-scanner-policies <MY_KEY_ID>
```
This creates 2 files, both prepended with the given version
1. `0-osv-scan-signed.json`: Standard SCONE signed session, signed with cli's current identity.
2. `0-osv-scan-signed.json.asc`: Signature over the signed policy file, signed with the given GPG key and including in the signed contents the SHA256 of the signed session, the repo url, file name, and date. This signature prevents cross repo copying of files, or manipulating the signed policy after it has been added.

The operator picks up the latest version of each signed policy, verifies the signature, and only then upload it to CAS. Refer to the sample repo for a sample on how the sample policies here are described in that repo.

### GPG Key Verification
The repo also holds a list of GPG public keys that are allowed to create signatures. Like the GPG used above in the generation of the signed policies. These keys are stored in a `gpg-key-authorization-<VERSION>.yml.asc` file. There maybe several versions, each can contain one or many valid keys. The yaml starts out in the following format:
```yaml
repo: <REPO_URL>
validity:
  notBefore: 20260503183034Z
  notAfter: 20270101000000Z
signers:
  <LIST_OF_ALLOWED_SIGNER_PUB_KEYS>
```
This yaml describes a list of allowed signers given a specified period. The operator only accepts a signed policy when the verification signature is from an allowed signer within the given period.

This yaml file itself is not added to the upstream repo, but a gpg signature file of it is added. The signing of `gpg-key-authorization` is done with 'root' gpg keys (with a simple `gpg --clearsign` call). These are the keys passed into the CR spec and are the root of trust. The operator only accepts a signed session when:
1. There is given valid gpg signer in a `gpg-key-authorization` file, in which the `gpg-key-authorization` is signed with a trust root key.
2. The signed session signature is signed with a valid signer
3. The session signature matches the correct signed session hash and repo

 Refer to sample repo to see how the gpg authorization file is set up.

