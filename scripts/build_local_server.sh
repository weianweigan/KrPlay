#!/bin/sh
# Explicit private local provisioning. Never invoked by the public Actions workflow.
set -eu
KRPLAY_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ASSETS="$KRPLAY_ROOT/.local/auth-assets"
[ -z "${GITHUB_ACTIONS:-}" ] || { echo 'Private provisioning is disabled in GitHub Actions' >&2; exit 1; }
for FILE in identity.pk8 certificate.p7b; do
    [ -s "$ASSETS/offline-mfi/$FILE" ] || { echo 'Import local authentication assets first using scripts/import_auth_assets.py' >&2; exit 1; }
done
WHEELPLAY_AUTH_ASSETS_DIR="$ASSETS" "$KRPLAY_ROOT/krplay/gradlew" -p "$KRPLAY_ROOT/krplay" \
    --no-daemon :mobile:assembleProvisionedDebug :mobile:exportKrPlayDebug "$@"
