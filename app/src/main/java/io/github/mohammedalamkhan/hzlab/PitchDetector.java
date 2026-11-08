// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

/**
 * Monophonic pitch detector and 12-bin chroma extractor.
 *
 * The detector uses the normalised square difference function (NSDF), the same
 * family of algorithm used by high quality hardware strobe tuners.  The
 * autocorrelation is calculated with an FFT so a long (186 ms) window remains
 * cheap enough to run continuously.  Parabolic interpolation of the NSDF peak
 * provides sub-cent resolution, including on the low strings of a bass.
 */
public final class PitchDetector {
    private static final double MIN_FREQUENCY = 25.0;
    private static final double MAX_FREQUENCY = 5000.0;
    public static final int SPECTRO_BINS = 240;
    private static final double SPECTRO_SIGMA = 0.045;

    private final int sampleRate;
    private final int window;
    private final int fftN;
    private final int maxLag;
    private final int minLag;

    private final double[] re;
    private final double[] im;
    private final double[] power;
    private final double[] windowed;
    private final double[] prefix;
    private final double[] nsdf;
    private final int[] rev;

    public float rms;
    public float confidence;
    /** Advanced settings, copied from Settings before each analysis. */
    public float noiseGateRms = 0.0008f;
    public float minClarity = 0.36f;
    public float spectroGamma = 0.72f;
    /** Low-frequency (bass) chroma used to identify the root of a chord. */
    public final float[] bassChroma = new float[12];
    /** Chroma limited to the fundamental register, for chord estimation. */
    public final float[] chordChroma = new float[12];
    /** Continuous pitch-class spectrum: 240 bins over one octave (5 cents/bin). */
    public final float[] spectro = new float[SPECTRO_BINS];

    public PitchDetector(int sampleRate) {
        this.sampleRate = sampleRate;
        int target = (int) Math.round(sampleRate * 0.185);
        window = Math.max(2048, nextPowerOfTwo(target));
        fftN = window * 2;
        maxLag = Math.min(window - 2, (int) (sampleRate / MIN_FREQUENCY));
        minLag = Math.max(3, (int) (sampleRate / MAX_FREQUENCY));
        re = new double[fftN];
        im = new double[fftN];
        power = new double[fftN / 2 + 1];
        windowed = new double[window];
        prefix = new double[window + 1];
        nsdf = new double[maxLag + 2];
        rev = new int[fftN];
        computeBitReverse();
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getWindowSize() {
        return window;
    }

    /**
     * Analyses a window of mono samples.  Returns the detected frequency, or 0
     * when no reliable pitch is present.  Chroma is always filled when there is
     * meaningful signal, so the chord/spectrogram modes work with polyphony.
     */
    public double analyse(float[] samples, int count, float[] chromaOut, double a4) {
        if (count < window) {
            count = Math.min(count, window);
        }
        if (count < 256) {
            rms = 0;
            confidence = 0;
            clear(chromaOut, 0);
            clear(spectro, 0);
            clear(bassChroma, 0);
            clear(chordChroma, 0);
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < count; i++) {
            sum += samples[i];
        }
        double mean = sum / count;
        double energy = 0;
        int n = window;
        for (int i = 0; i < n; i++) {
            double v;
            if (i < count) {
                v = samples[i] - mean;
            } else {
                v = 0;
            }
            // No taper: tapering a correlation window biases the period estimate
            // on bass notes where only a handful of cycles fit in the window.
            windowed[i] = v;
            energy += v * v;
            prefix[i + 1] = prefix[i] + v * v;
        }
        rms = (float) Math.sqrt(energy / n);
        if (rms < noiseGateRms) {
            confidence = 0;
            clear(chromaOut, 0);
            clear(spectro, 0);
            clear(bassChroma, 0);
            clear(chordChroma, 0);
            return 0;
        }

        for (int i = 0; i < fftN; i++) {
            re[i] = i < n ? windowed[i] : 0.0;
            im[i] = 0.0;
        }
        fft(false);

        int bins = fftN / 2;
        for (int k = 0; k <= bins; k++) {
            double rr = re[k];
            double ii = im[k];
            power[k] = rr * rr + ii * ii;
            re[k] = power[k];
            im[k] = 0.0;
        }
        for (int k = 1; k < bins; k++) {
            re[fftN - k] = power[k];
            im[fftN - k] = 0.0;
        }
        im[bins] = 0.0;
        fft(true);

        for (int tau = 0; tau <= maxLag + 1; tau++) {
            // The inverse FFT is normalised by 1/fftN, so re[tau] is already
            // the linear autocorrelation r[tau] here.
            double r = re[tau];
            double m = prefix[n - tau] + (prefix[n] - prefix[tau]);
            nsdf[tau] = m > 1e-12 ? 2.0 * r / m : 0.0;
        }

        // Local maxima of the NSDF.
        int[] peaks = new int[96];
        double[] peakValues = new double[96];
        int peakCount = 0;
        double globalMax = 0;
        for (int tau = minLag; tau <= maxLag; tau++) {
            double v = nsdf[tau];
            if (v > 0.25 && v >= nsdf[tau - 1] && v > nsdf[tau + 1]) {
                if (peakCount < peaks.length) {
                    peaks[peakCount] = tau;
                    peakValues[peakCount] = v;
                    peakCount++;
                }
                if (v > globalMax) {
                    globalMax = v;
                }
            }
        }
        if (peakCount == 0 || globalMax < minClarity * (0.30f / 0.36f)) {
            confidence = (float) Math.max(0, globalMax);
            buildChroma(chromaOut, a4);
            return 0;
        }

        double threshold = Math.max(0.55, 0.93 * globalMax);
        int chosen = -1;
        for (int i = 0; i < peakCount; i++) {
            if (peakValues[i] >= threshold) {
                chosen = i;
                break;
            }
        }
        if (chosen < 0) {
            chosen = 0;
            for (int i = 1; i < peakCount; i++) {
                if (peakValues[i] > peakValues[chosen]) {
                    chosen = i;
                }
            }
        }

        int tau = peaks[chosen];
        double value = peakValues[chosen];

        int useTau = tau;
        double y0 = nsdf[tau - 1];
        double y1 = nsdf[tau];
        double y2 = nsdf[tau + 1];
        double denom = y0 - 2.0 * y1 + y2;
        double delta = 0.0;
        if (Math.abs(denom) > 1e-9) {
            delta = 0.5 * (y0 - y2) / denom;
            if (delta > 0.5) {
                delta = 0.5;
            } else if (delta < -0.5) {
                delta = -0.5;
            }
        }
        double refinedLag = useTau + delta;
        double frequency = refinedLag > 0 ? sampleRate / refinedLag : 0;
        confidence = (float) Math.max(0, Math.min(1, value));

        buildChroma(chromaOut, a4);
        if (frequency >= MIN_FREQUENCY && frequency <= MAX_FREQUENCY) {
            addFundamentalSpectro(frequency, a4);
            normaliseSpectro();
        }

        if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY || confidence < minClarity) {
            return 0;
        }
        return frequency;
    }

    private void buildChroma(float[] chromaOut, double a4) {
        clear(chromaOut, 0);
        clear(bassChroma, 0);
        clear(chordChroma, 0);
        clear(spectro, 0);
        double binHz = (double) sampleRate / fftN;
        int first = Math.max(2, (int) (45.0 / binHz));
        int last = Math.min(power.length - 2, (int) (4200.0 / binHz));
        if (last <= first) {
            return;
        }
        // Estimate the broadband noise floor. Tonal peaks stand well above it;
        // microphone hiss and room rumble do not. Without this step a noisy
        // room fills every row of the spectrogram and every chord template.
        double sum = 0;
        int count = 0;
        for (int k = first; k <= last; k++) {
            sum += Math.sqrt(power[k]);
            count++;
        }
        double noise = count > 0 ? sum / count : 0;
        double threshold = noise * 1.75;
        double peak = 0;
        for (int k = first; k <= last; k++) {
            double mag = Math.sqrt(power[k]);
            if (mag <= threshold) {
                continue;
            }
            // Keep only spectral peaks. This also prevents the skirts of a
            // strong fundamental from leaking into neighbouring semitones.
            if (power[k] < power[k - 1] || power[k] < power[k + 1]) {
                continue;
            }
            // Parabolic interpolation is essential at low frequencies: one FFT
            // bin can be 20 cents wide around E2, which otherwise makes a low
            // E look weaker than the same-level G or B in a chord.
            double y0 = Math.sqrt(power[k - 1]);
            double y1 = mag;
            double y2 = Math.sqrt(power[k + 1]);
            double denom = y0 - 2.0 * y1 + y2;
            double delta = Math.abs(denom) > 1e-12 ? 0.5 * (y0 - y2) / denom : 0.0;
            if (delta > 0.5) {
                delta = 0.5;
            } else if (delta < -0.5) {
                delta = -0.5;
            }
            double f = (k + delta) * binHz;
            double midi = 69.0 + 12.0 * Math.log(f / a4) / Math.log(2.0);
            int nearest = (int) Math.round(midi);
            double distance = midi - nearest;
            if (Math.abs(distance) > 0.46) {
                continue;
            }
            int pc = ((nearest % 12) + 12) % 12;
            double w = Math.exp(-0.5 * (distance / 0.22) * (distance / 0.22));
            double fw = Math.min(1.0, Math.max(0.0, (f - 30.0) / 30.0));
            if (f > 1400.0) {
                fw *= 1400.0 / f;
            }
            double value = Math.max(0.0, mag - noise * 0.6) * w * fw;
            float energy = (float) (value * value);
            double pitchPosition = ((midi % 12.0) + 12.0) % 12.0;
            addSpectro(pitchPosition, (float) Math.max(0.0, mag - noise * 0.6));
            chromaOut[pc] += energy;
            if (f < 300.0) {
                bassChroma[pc] += energy;
            }
            if (f < 1000.0) {
                chordChroma[pc] += energy;
            }
            if (chromaOut[pc] > peak) {
                peak = chromaOut[pc];
            }
        }
        if (peak > 0) {
            for (int i = 0; i < 12; i++) {
                float v = (float) Math.sqrt(chromaOut[i] / peak);
                v = Math.max(0f, (v - 0.16f) / 0.84f);
                chromaOut[i] = v;
            }
        }
        float bassPeak = 0;
        for (int i = 0; i < 12; i++) {
            bassPeak = Math.max(bassPeak, bassChroma[i]);
        }
        if (bassPeak > 0) {
            for (int i = 0; i < 12; i++) {
                bassChroma[i] = (float) Math.sqrt(bassChroma[i] / bassPeak);
            }
        }
        float chordPeak = 0;
        for (int i = 0; i < 12; i++) {
            chordPeak = Math.max(chordPeak, chordChroma[i]);
        }
        if (chordPeak > 0) {
            for (int i = 0; i < 12; i++) {
                chordChroma[i] = (float) Math.sqrt(chordChroma[i] / chordPeak);
            }
        }
        normaliseSpectro();
    }

    private void addSpectro(double pitchPosition, float amplitude) {
        if (pitchPosition < 0 || pitchPosition >= 12 || amplitude <= 0) {
            return;
        }
        // Position 0.5 is the centre of the C line; B is at 11.5.
        double centre = (pitchPosition + 0.5) * SPECTRO_BINS / 12.0;
        int radius = 4;
        int c = (int) Math.round(centre);
        for (int b = c - radius; b <= c + radius; b++) {
            if (b < 0 || b >= SPECTRO_BINS) {
                continue;
            }
            double d = (b - centre) / (SPECTRO_SIGMA * SPECTRO_BINS / 12.0);
            float v = (float) (amplitude * Math.exp(-0.5 * d * d));
            if (v > spectro[b]) {
                spectro[b] = v;
            }
        }
    }

    private void addFundamentalSpectro(double frequency, double a4) {
        if (frequency <= 0) {
            return;
        }
        double midi = 69.0 + 12.0 * Math.log(frequency / a4) / Math.log(2.0);
        double position = ((midi % 12.0) + 12.0) % 12.0;
        double centre = (position + 0.5) * SPECTRO_BINS / 12.0;
        int c = (int) Math.round(centre);
        for (int b = c - 3; b <= c + 3; b++) {
            if (b < 0 || b >= SPECTRO_BINS) {
                continue;
            }
            double d = (b - centre) / (0.035 * SPECTRO_BINS / 12.0);
            float v = (float) Math.exp(-0.5 * d * d);
            if (v > spectro[b]) {
                spectro[b] = v;
            }
        }
    }

    private void normaliseSpectro() {
        float max = 0;
        for (float v : spectro) {
            max = Math.max(max, v);
        }
        if (max <= 1e-9f) {
            return;
        }
        for (int i = 0; i < SPECTRO_BINS; i++) {
            float v = spectro[i] / max;
            spectro[i] = (float) Math.pow(Math.max(0f, v), spectroGamma);
        }
    }

    private static void clear(float[] array, float value) {
        if (array == null) {
            return;
        }
        for (int i = 0; i < array.length; i++) {
            array[i] = value;
        }
    }

    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    private void computeBitReverse() {
        int bits = Integer.numberOfTrailingZeros(fftN);
        for (int i = 0; i < fftN; i++) {
            rev[i] = Integer.reverse(i) >>> (32 - bits);
        }
    }

    /** In-place iterative radix-2 FFT. */
    private void fft(boolean inverse) {
        int n = fftN;
        for (int i = 0; i < n; i++) {
            int j = rev[i];
            if (j > i) {
                double tr = re[i];
                re[i] = re[j];
                re[j] = tr;
                double ti = im[i];
                im[i] = im[j];
                im[j] = ti;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double angle = (inverse ? 2.0 : -2.0) * Math.PI / len;
            double wlenR = Math.cos(angle);
            double wlenI = Math.sin(angle);
            int half = len >> 1;
            for (int i = 0; i < n; i += len) {
                double wr = 1.0;
                double wi = 0.0;
                for (int j = 0; j < half; j++) {
                    int a = i + j;
                    int b = a + half;
                    double xr = re[b] * wr - im[b] * wi;
                    double xi = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - xr;
                    im[b] = im[a] - xi;
                    re[a] += xr;
                    im[a] += xi;
                    double nwr = wr * wlenR - wi * wlenI;
                    wi = wr * wlenI + wi * wlenR;
                    wr = nwr;
                }
            }
        }
        if (inverse) {
            double inv = 1.0 / n;
            for (int i = 0; i < n; i++) {
                re[i] *= inv;
                im[i] *= inv;
            }
        }
    }
}
