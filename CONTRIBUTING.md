# Contributing to HzLab

Thanks for helping improve a free tuner for musicians.

## Build

Requirements:

- JDK 21
- Android SDK platform 35 and build-tools 35.0.0
- `JAVA_HOME` and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) set

```bash
./build.sh debug
```

The debug APK is written to `dist/HzLab-1.0.0-debug.apk`. A local signing key is generated under `signing/` on first build; that directory is intentionally git-ignored.

## Testing

The DSP tests use generated tones and do not need a device:

```bash
./test/run.sh
```

For device testing, install the debug APK, grant microphone access, and use the demo intent if you need synthetic audio:

```bash
adb shell am start -n io.github.mohammedalamkhan.hzlab/.MainActivity \
  --ez demo_audio true --ei demo_mode 0
```

## Pull requests

- Keep changes focused and explain the musical or platform problem being solved.
- Test on a physical device when changing audio capture, pitch detection or rendering.
- Include the device model, Android version and ABI in audio/rendering bug reports.
- Do not add copyrighted artwork, audio, source code or other assets from another app. HzLab is a clean-room project.
- Prefer small, readable Java and standard Android APIs. The project intentionally has no third-party runtime dependencies.

## Reporting bugs

Please include:

- device model and Android version
- whether the device is 32-bit, 64-bit or 64-bit-only
- selected tuning and temperament
- what you played and what the display showed
- a screenshot if the problem is visual
