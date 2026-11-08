## HzLab 1.0.2 — Advanced DSP controls

This release adds user-adjustable DSP controls while preserving the previous defaults.

### New advanced settings

- **Noise gate** — ignore signals below a chosen RMS (default -62 dB).
- **Clarity threshold** — minimum pitch confidence (default 36%).
- **Harmonic search** — how many harmonics Instrument mode checks (default 8).
- **Pitch smoothing** — needle/strobe response (default 72%).
- **Spectrogram contrast** — display gamma (default 0.72).
- **Spectrogram speed** — scroll columns per analysis frame (default 1x).
- **Chord gate** — minimum signal level for chord analysis (default -56 dB).
- **Chord smoothing** — how long chord changes are held (default 72%).

Each control uses the previous hardcoded value as its default, applies immediately, and persists across launches. A factory reset restores every default.

### Install

Download `HzLab-1.0.2.apk` and install it on Android 6.0 or newer. Existing 1.0.1 installs signed with the same HzLab key can update in place.

### Verification

Package: `io.github.mohammedalamkhan.hzlab`, versionCode 3. Pure Java, no native libraries, no network permission.
