## HzLab 1.0.1 — F-Droid preparation

This is a packaging release. Audio processing, tuning data and the five visualisers are unchanged.

### Changes

- Added Fastlane listing metadata: title, short and full descriptions, screenshots and changelogs.
- Added `fdroid-build.sh`, a portable unsigned build entry point for F-Droid's build server.
- Added build-tools/platform fallback discovery so the manual build works across Android SDK installations.
- Kept the app free of native libraries, network permissions, ads, analytics and third-party runtime dependencies.

### Install

Download `HzLab-1.0.1.apk` below and install it on Android 6.0 or newer. Existing 1.0.0 installs signed with the same HzLab key can update in place.

### Verification

The APK is signed with the HzLab release key. Package: `io.github.mohammedalamkhan.hzlab`, versionCode 2.
