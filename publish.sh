#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

if ! command -v gh >/dev/null 2>&1; then
  echo "Install GitHub CLI first: https://cli.github.com/" >&2
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  gh auth login --hostname github.com --git-protocol https --web
fi

# Guard against accidentally publishing an outdated binary.
./build.sh release

git push -u origin main
git push origin v1.0.0 2>/dev/null || true
gh release create v1.0.0 \
  dist/HzLab-1.0.0.apk \
  dist/SHA256SUMS.txt \
  --title "HzLab 1.0.0" \
  --notes-file docs/release-notes-1.0.0.md \
  --verify-tag

echo "Published: https://github.com/MohammedAlamkhan/HzLab/releases/tag/v1.0.0"
