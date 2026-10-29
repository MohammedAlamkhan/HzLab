# Changelog

## 1.0.1 — F-Droid preparation

- Add Fastlane listing metadata (title, descriptions, screenshots, changelogs).
- Add `fdroid-build.sh`, a portable unsigned build path for F-Droid's build server.
- Add build-tools/platform fallback discovery so the manual build works across SDK installations.
- No changes to audio processing or the five visualisers.


## 1.0.0 — first public HzLab release

- Clean-room HzLab app, package `io.github.mohammedalamkhan.hzlab`.
- Five visualisers: Instrument, Dial & Strobe, Spectrogram, Chords, Big Strobe.
- 52 built-in instrument tunings, 17 temperaments, custom instrument and temperament editors.
- A4 reference 220–880 Hz, sharp/flat notation, selectable in-tune threshold and orientation.
- 44.1/22.05/11.025/8 kHz sample rates with automatic fallback; low/medium/high analysis rate.
- 240-bin (5-cent) continuous Spectrogram with live cents readout, vibrato and microtone display.
- Chord estimation with temporal smoothing, root/bass weighting and candidate cells.
- Pure Java, no native libraries; installs on 64-bit-only Android devices.
- No ads, analytics, network permission or bundled third-party SDKs.
