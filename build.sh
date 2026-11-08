#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="${JAVA_HOME:-$ROOT/../tools/jdk}"
ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$ROOT/../tools/android-sdk}}"
BT="$ANDROID_HOME/build-tools/35.0.0"
if [ ! -x "$BT/aapt2" ]; then
  BT="$(ls -d "$ANDROID_HOME"/build-tools/* 2>/dev/null | sort -V | tail -1)"
fi
PLATFORM="$ANDROID_HOME/platforms/android-35/android.jar"
if [ ! -f "$PLATFORM" ]; then
  PLATFORM="$(find "$ANDROID_HOME/platforms" -maxdepth 2 -name android.jar 2>/dev/null | sort -V | tail -1)"
fi
if [ ! -x "$JAVA_HOME/bin/javac" ]; then
  JAVA_HOME="$(dirname "$(dirname "$(command -v javac)")")"
fi
APP="$ROOT/app/src/main"
BUILD="$ROOT/build"
DIST="$ROOT/dist"
SIGNING="$ROOT/signing"
KEYSTORE="$SIGNING/hzlab-release.jks"
KS_PASS_FILE="$SIGNING/hzlab-release.pass"
KS_ALIAS="hzlab"
VERSION_CODE="4"
VERSION_NAME="1.0.3"
MODE="${1:-release}"

for tool in "$JAVA_HOME/bin/javac" "$BT/aapt2" "$BT/d8" "$BT/zipalign" "$PLATFORM"; do
  if [ ! -e "$tool" ]; then
    echo "Missing required tool: $tool" >&2
    echo "Set JAVA_HOME and ANDROID_HOME to a JDK 21 and Android SDK 35 installation." >&2
    exit 1
  fi
done

mkdir -p "$DIST"
if [ "$MODE" != "unsigned" ] && [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$SIGNING"
  PASS="$(python3 -c 'import secrets,string; print("".join(secrets.choice(string.ascii_letters+string.digits) for _ in range(28)))')"
  printf '%s' "$PASS" > "$KS_PASS_FILE"
  chmod 600 "$KS_PASS_FILE"
  "$JAVA_HOME/bin/keytool" -genkeypair -v \
    -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$PASS" \
    -alias "$KS_ALIAS" -keypass "$PASS" -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=HzLab, OU=Open Source, O=HzLab, C=IN" >/dev/null 2>&1
fi
if [ "$MODE" != "unsigned" ]; then
  PASS="$(cat "$KS_PASS_FILE")"
fi

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$BUILD/res"
"$BT/aapt2" compile --dir "$APP/res" -o "$BUILD/res.zip"
LINK_ARGS=(
  -o "$BUILD/app-unaligned.apk"
  -I "$PLATFORM"
  --manifest "$APP/AndroidManifest.xml"
  -R "$BUILD/res.zip"
  --java "$BUILD/gen"
  --min-sdk-version 23
  --target-sdk-version 35
  --version-code "$VERSION_CODE"
  --version-name "$VERSION_NAME"
  --auto-add-overlay
)
if [ "$MODE" = "debug" ]; then
  LINK_ARGS+=(--debug-mode)
fi
"$BT/aapt2" link "${LINK_ARGS[@]}"

find "$APP/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
"$JAVA_HOME/bin/javac" -source 8 -target 8 -encoding UTF-8 \
  -classpath "$PLATFORM" -d "$BUILD/classes" @"$BUILD/sources.txt" \
  -Xlint:-options -Xlint:-deprecation -Xlint:-unchecked
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
"$BT/d8" --release --min-api 23 --lib "$PLATFORM" --output "$BUILD/dex" @"$BUILD/classes.txt"
(cd "$BUILD/dex" && zip -q "$BUILD/app-unaligned.apk" classes.dex)
"$BT/zipalign" -p -f 4 "$BUILD/app-unaligned.apk" "$BUILD/app-aligned.apk"

if [ "$MODE" = "unsigned" ]; then
  OUT="$DIST/HzLab-$VERSION_NAME-unsigned.apk"
  cp "$BUILD/app-aligned.apk" "$OUT"
elif [ "$MODE" = "debug" ]; then
  OUT="$DIST/HzLab-$VERSION_NAME-debug.apk"
else
  OUT="$DIST/HzLab-$VERSION_NAME.apk"
fi
if [ "$MODE" != "unsigned" ]; then
  "$BT/apksigner" sign \
    --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" \
    --ks-pass "pass:$PASS" --key-pass "pass:$PASS" \
    --min-sdk-version 23 --v1-signing-enabled true \
    --v2-signing-enabled true --v3-signing-enabled true \
    --out "$OUT" "$BUILD/app-aligned.apk"
  "$BT/apksigner" verify --verbose --print-certs "$OUT" > "$BUILD/verify.txt"
fi
printf '\nBuilt: %s\n' "$OUT"
sha256sum "$OUT"
