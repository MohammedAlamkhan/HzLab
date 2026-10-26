// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

/** One frame of pitch/chroma analysis. Reused by the audio thread. */
public final class PitchResult {
    public boolean valid;
    public double frequency;
    public double rawFrequency;
    public double midiFloat;
    public int midi;
    public double cents;
    public float rms;
    public float confidence;
    public long timestampNanos;
    public final float[] chroma = new float[12];
    public final float[] bassChroma = new float[12];
    public final float[] chordChroma = new float[12];
    public final float[] spectro = new float[PitchDetector.SPECTRO_BINS];

    public void clear() {
        valid = false;
        frequency = 0;
        rawFrequency = 0;
        midiFloat = 0;
        midi = 0;
        cents = 0;
        rms = 0;
        confidence = 0;
        for (int i = 0; i < 12; i++) {
            chroma[i] = 0;
            bassChroma[i] = 0;
            chordChroma[i] = 0;
        }
        for (int i = 0; i < spectro.length; i++) {
            spectro[i] = 0;
        }
    }

    public void setTemperamentTarget(double a4, float[] temperamentCents) {
        if (!valid) {
            return;
        }
        double midiRounded = Math.round(midiFloat);
        int bestMidi = (int) midiRounded;
        double bestCents = 1e9;
        // A temperament offset can move the true target by over half a semitone
        // for historical temperaments, so inspect the neighbouring notes too.
        for (int candidate = (int) midiRounded - 1; candidate <= (int) midiRounded + 1; candidate++) {
            if (candidate < 0 || candidate > 127) {
                continue;
            }
            double target = Notes.targetHz(candidate, a4, temperamentCents);
            double c = 1200.0 * Math.log(frequency / target) / Math.log(2.0);
            if (Math.abs(c) < Math.abs(bestCents)) {
                bestCents = c;
                bestMidi = candidate;
            }
        }
        midi = bestMidi;
        cents = bestCents;
    }
}
