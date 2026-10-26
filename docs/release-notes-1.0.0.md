## HzLab 1.0.0 — first public release

HzLab is a clean-room, 64-bit-compatible tuner and pitch visualiser for modern Android. It revives the workflow of the abandoned PitchLab Lite app without using any of its code, native libraries or visual assets.

### Highlights

- **Instrument** radial strobe with string/note targeting and harmonic folding
- **Dial & Strobe** needle and moving linear strobe
- **Spectrogram** with a continuous 240-bin pitch axis, 5-cent resolution, live cents readout, vibrato and microtone display
- **Chords** grid for major, major 7th, dominant 7th, minor, minor 7th and diminished chords
- **Big Strobe** for small screens
- 52 built-in tunings and 17 historical/equal temperaments
- Custom instrument and temperament editors
- A4 reference 220–880 Hz, sharp/flat notation, selectable in-tune tolerance
- 44.1 / 22.05 / 11.025 / 8 kHz input with automatic fallback
- Pure Java, **no native libraries**, installs on arm64-only phones
- No ads, analytics, network permission or third-party runtime SDKs
- GPL-3.0-or-later

### Install

Download `HzLab-1.0.0.apk` below and install it on Android 6.0 or newer. Allow microphone access on first launch.

### Verification

The APK is signed with the HzLab release key (certificate fingerprint and SHA-256 are in `SHA256SUMS.txt`). The package is `io.github.mohammedalamkhan.hzlab`, versionCode 1.

### Notes

- Chord recognition is an estimate and works best with clean, sustained chords.
- Preset tunings and historical temperament values are factual musical data; no original app assets are included.
- See the README for architecture, build instructions and known limitations.
