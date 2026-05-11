#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: $0 <version> <file> <repo-url> <gpg-key-id>"
  echo "  <version>    Version number prepended to output filenames"
  echo "  <file>       Session YAML file to sign"
  echo "  <repo-url>   Git repo URL to embed in the signature manifest"
  echo "  <gpg-key-id> GPG key ID or fingerprint used for signing"
  exit 1
}

[[ $# -ne 4 ]] && usage

VERSION="$1"
FILE="$2"
REPO="$3"
GPG_KEY="$4"

if [[ ! -f "$FILE" ]]; then
  echo "Error: file not found: $FILE" >&2
  exit 1
fi

BASENAME=$(basename "$FILE")
STEM="${BASENAME%.*}"
VERSIONED_BASENAME="${VERSION}-${STEM}"
SIGNED_FILE="$(dirname "$FILE")/${VERSIONED_BASENAME}-signed.json"
ASC_FILE="$(dirname "$FILE")/${VERSIONED_BASENAME}-signed.json.asc"
MANIFEST_TMP=$(mktemp --suffix=".yml")
trap 'rm -f "$MANIFEST_TMP"' EXIT

echo "Signing session with SCONE..."
scone session sign "$FILE" --use-env > "$SIGNED_FILE"

SHA256=$(sha256sum "$SIGNED_FILE" | awk '{print $1}')
DATE=$(date -u '+%Y%m%d%H%M%SZ')

cat > "$MANIFEST_TMP" <<EOF
filename: $(basename "$SIGNED_FILE")
sha256: $SHA256
date: $DATE
repo: $REPO
EOF

echo "GPG signing manifest..."
gpg --clearsign --default-key "$GPG_KEY" --output "$ASC_FILE" "$MANIFEST_TMP"

echo ""
echo "Done."
echo "  Signed session : $SIGNED_FILE"
echo "  Manifest (asc) : $ASC_FILE"