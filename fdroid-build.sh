#!/usr/bin/env bash
# F-Droid build entry point: unsigned APK, no maintainer signing key required.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-$ROOT/../tools/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$ROOT/../tools/android-sdk}}"
exec "$ROOT/build.sh" unsigned
