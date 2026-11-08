#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if ! command -v gh >/dev/null 2>&1; then
  if [ -x "$ROOT/../tools/gh/bin/gh" ]; then
    export PATH="$ROOT/../tools/gh/bin:$PATH"
  else
    echo "Install GitHub CLI first: https://cli.github.com/" >&2
    exit 1
  fi
fi
if ! gh auth status >/dev/null 2>&1; then
  gh auth login --hostname github.com --git-protocol https --web
fi

# Guard against accidentally publishing an outdated binary.
./build.sh release
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$ROOT/../tools/android-sdk}}"
BT="$ANDROID_HOME/build-tools/35.0.0"
HASH="$(sha256sum dist/HzLab-1.0.3.apk | awk '{print $1}')"
CERT="$("$BT/apksigner" verify --print-certs dist/HzLab-1.0.3.apk | awk -F': ' '/certificate SHA-256/{print $2; exit}')"
cat > dist/SHA256SUMS.txt <<EOF
$HASH  HzLab-1.0.3.apk

Signing certificate (SHA-256):
$CERT
EOF

git push -u origin main
git push origin v1.0.3 2>/dev/null || true
if gh release view v1.0.3 >/dev/null 2>&1; then
  gh release upload v1.0.3 dist/HzLab-1.0.3.apk dist/SHA256SUMS.txt --clobber
else
  gh release create v1.0.3 \
    dist/HzLab-1.0.3.apk \
    dist/SHA256SUMS.txt \
    --title "HzLab 1.0.3" \
    --notes-file docs/release-notes-1.0.3.md \
    --verify-tag
fi

echo "Published: https://github.com/MohammedAlamkhan/HzLab/releases/tag/v1.0.3"
