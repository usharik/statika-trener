#!/usr/bin/env bash
# Runs a command with the App Store Connect API key from Infisical (project of CzechQuiz, env prod, /app-store),
# like the other apps of the team: ASC_KEY_ID, ASC_ISSUER_ID in the environment, the .p8 in a temporary file in
# ASC_KEY_PATH (deleted afterwards). Run from mobile/, e.g.
#   iosApp/store/asc.sh python3 iosApp/store/tools/asc_setup.py --bundle-id cz.usharik.statika status
#   iosApp/store/asc.sh bundle exec fastlane ios listing
set -euo pipefail
[ $# -gt 0 ] || { echo "usage: $0 <command> [args…]" >&2; exit 2; }
if [ -z "${ASC_KEY_P8:-}" ]; then
    exec infisical run --projectId 2e201525-a8e8-4d99-802e-f17d1355659a --env prod --path /app-store --silent -- "$0" "$@"
fi
KEY=$(mktemp "${TMPDIR:-/tmp}/asc-key.XXXXXX")
trap 'rm -f "$KEY"' EXIT
printf '%s\n' "$ASC_KEY_P8" > "$KEY"
unset ASC_KEY_P8
ASC_KEY_PATH="$KEY" "$@"
