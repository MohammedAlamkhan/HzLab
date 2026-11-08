# Changelog

## 1.0.4 — Practice tools and themes

- Added a sample-clocked **metronome** with tap tempo, 2–12 beats, 1×/2×/4× subdivisions, accent and volume.
- Added a **reference tone** screen: chromatic note picker, octave controls, selected A4/temperament and volume.
- Added **capo/transpose** (0–12 semitones) to the instrument tuning and string targets.
- Added **quick presets** to the top menu for switching instruments.
- Added **Midnight**, **Pitch black** and **Daylight** themes, persisted across launches.
- Added a Tools section to Settings and a redesigned, theme-aware metronome/reference-tone UI.
- All existing tuner, DSP, gesture and advanced-control behavior remains intact.


## 1.0.3 — Visual overhaul

- New dark design system: deep navy surfaces, cyan accent, refined typography and spacing.
- Compact top app bar with the HzLab wordmark, current mode/listening state and menu button.
- Labelled five-item bottom navigation replaces the unlabelled dots, with direct mode switching.
- Redesigned Settings screen with rounded cards, section labels, chevrons and ripple feedback.
- New microphone-permission card and refreshed dial, spectrogram, chord and strobe layouts.
- All DSP controls, gestures, tuning data and advanced settings from 1.0.2 are unchanged.


## 1.0.2 — Advanced DSP controls

- Added a persistent Advanced DSP section: noise gate, clarity threshold, harmonic search depth, pitch smoothing, spectrogram contrast, spectrogram speed, chord gate and chord smoothing.
- Defaults are the previous hardcoded values, so existing behaviour is unchanged until a control is adjusted.
- Added accessible slider dialogs with live values and range labels.
- No change to tuning or temperament data.


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
