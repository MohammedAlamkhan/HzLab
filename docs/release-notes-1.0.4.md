## HzLab 1.0.4 — Practice tools and themes

This release adds the four practice tools requested and a complete three-theme system.

### New tools

- **Metronome** — 40–240 BPM, tap tempo, 2–12 beats per bar, 1×/2×/4× subdivisions, accent and volume. Timing is sample-clocked with `AudioTrack`; it keeps running while the screen is open.
- **Reference tone** — chromatic note picker with octave controls, using the selected A4 reference and temperament. Includes volume and a headphone reminder.
- **Capo** — transpose the selected tuning and its string targets from 0 to 12 semitones.
- **Quick presets** — switch instrument tunings from the top menu.

### Themes

- **Midnight** — deep navy with cyan accents (default).
- **Pitch black** — true-black background for OLED.
- **Daylight** — light surfaces, navy text and blue accents.

The selected theme applies to the tuner, settings, metronome and reference-tone screens and persists across launches.

### Unchanged

All 52 tunings, 17 temperaments, custom editors, chord/spectrogram DSP, gestures and advanced controls remain available. Defaults are unchanged.

### Install

Download `HzLab-1.0.4.apk` and install it on Android 6.0 or newer. Existing 1.0.3 installs signed with the same HzLab key can update in place.

### Verification

Package: `io.github.mohammedalamkhan.hzlab`, versionCode 5. Pure Java, no native libraries, no network permission. The metronome and tone use device audio output only.
