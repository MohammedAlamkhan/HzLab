// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;

import java.util.Random;

/**
 * Owns the microphone and the analysis thread, and delivers immutable-ish
 * analysis frames to the UI.  The DSP itself is platform independent.
 */
public final class PitchEngine implements AudioCapture.Listener {
    public interface Listener {
        void onPitch(PitchResult result);
        void onAudioError(String message);
        void onAudioStarted(int sampleRate);
    }

    private static final int RING_SIZE = 32768;

    private final Context context;
    private final Settings settings;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final float[] ring = new float[RING_SIZE];
    private final AudioCapture capture = new AudioCapture();

    private int ringWrite;
    private long ringTotal;
    private volatile boolean running;
    private volatile boolean demo;
    private Thread analysisThread;
    private PitchDetector detector;
    private float[] frame;
    private float[] chroma;
    private int activeSampleRate;
    private int analysisInterval;
    private double lastFrequency;
    private long lastPitchNanos;
    private DemoSource demoSource;
    private boolean audioErrorReported;

    public PitchEngine(Context context, Settings settings, Listener listener) {
        this.context = context.getApplicationContext();
        this.settings = settings;
        this.listener = listener;
        this.demo = (this.context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0
                && this.context.getSharedPreferences("hzlab_debug", Context.MODE_PRIVATE)
                .getBoolean("demo_audio", false);
    }

    public boolean isDemo() {
        return demo;
    }

    public void setDemo(boolean enabled) {
        demo = enabled;
    }

    public int getActiveSampleRate() {
        return activeSampleRate;
    }

    public int getAnalysisWindow() {
        return detector == null ? 0 : detector.getWindowSize();
    }

    public void start() {
        stop();
        audioErrorReported = false;
        activeSampleRate = settings.sampleRate;
        analysisInterval = settings.analysisIntervalMs();
        synchronized (lock) {
            ringWrite = 0;
            ringTotal = 0;
            lastFrequency = 0;
            lastPitchNanos = 0;
        }
        if (demo) {
            activeSampleRate = 44100;
            detector = new PitchDetector(activeSampleRate);
            frame = new float[detector.getWindowSize()];
            chroma = new float[12];
            demoSource = new DemoSource(activeSampleRate);
            running = true;
            startAnalysisThread();
            main.post(new Runnable() {
                @Override
                public void run() {
                    listener.onAudioStarted(activeSampleRate);
                }
            });
            return;
        }

        if (!hasPermission()) {
            listener.onAudioError("Microphone permission is required");
            return;
        }
        if (capture.start(activeSampleRate, this)) {
            detector = new PitchDetector(activeSampleRate);
            frame = new float[detector.getWindowSize()];
            chroma = new float[12];
            running = true;
            startAnalysisThread();
            final int sr = activeSampleRate;
            main.post(new Runnable() {
                @Override
                public void run() {
                    listener.onAudioStarted(sr);
                }
            });
        } else {
            // Most devices support 44.1 kHz. Try the other common rates before
            // surfacing an error; try each common rate before giving up.
            int[] fallbacks = {44100, 22050, 11025, 8000};
            for (int rate : fallbacks) {
                if (rate == activeSampleRate) {
                    continue;
                }
                if (capture.start(rate, this)) {
                    activeSampleRate = rate;
                    detector = new PitchDetector(rate);
                    frame = new float[detector.getWindowSize()];
                    chroma = new float[12];
                    running = true;
                    startAnalysisThread();
                    final int sr = rate;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onAudioStarted(sr);
                        }
                    });
                    return;
                }
            }
            listener.onAudioError("Could not start the microphone at a supported sample rate. "
                    + "Try 44.1 kHz in Options, or reboot the phone.");
        }
    }

    private boolean hasPermission() {
        return context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void startAnalysisThread() {
        analysisThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                while (running) {
                    long started = System.nanoTime();
                    if (demo) {
                        demoSource.fill(ring, lock, PitchEngine.this);
                    }
                    analyseLatest();
                    long elapsedMs = (System.nanoTime() - started) / 1000000L;
                    long sleep = Math.max(2, analysisInterval - elapsedMs);
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }, "HzLab-Analysis");
        analysisThread.start();
    }

    private void analyseLatest() {
        int window = detector.getWindowSize();
        if (frame == null || frame.length != window) {
            frame = new float[window];
        }
        synchronized (lock) {
            if (ringTotal < window) {
                return;
            }
            int start = (ringWrite - window + RING_SIZE) % RING_SIZE;
            if (start + window <= RING_SIZE) {
                System.arraycopy(ring, start, frame, 0, window);
            } else {
                int first = RING_SIZE - start;
                System.arraycopy(ring, start, frame, 0, first);
                System.arraycopy(ring, 0, frame, first, window - first);
            }
        }
        detector.noiseGateRms = settings.noiseGateRms;
        detector.minClarity = settings.minClarity;
        detector.spectroGamma = settings.spectroGamma;
        double raw = detector.analyse(frame, window, chroma, settings.a4);
        if (raw > 0 && settings.viewMode == HzLabView.MODE_INSTRUMENT) {
            raw = foldToInstrument(raw);
        }
        PitchResult result = new PitchResult();
        result.timestampNanos = System.nanoTime();
        result.rms = detector.rms;
        result.confidence = detector.confidence;
        System.arraycopy(chroma, 0, result.chroma, 0, 12);
        System.arraycopy(detector.bassChroma, 0, result.bassChroma, 0, 12);
        System.arraycopy(detector.chordChroma, 0, result.chordChroma, 0, 12);
        System.arraycopy(detector.spectro, 0, result.spectro, 0, PitchDetector.SPECTRO_BINS);
        if (raw > 0) {
            result.rawFrequency = raw;
            double frequency = raw;
            synchronized (lock) {
                long now = result.timestampNanos;
                if (lastFrequency > 0 && now - lastPitchNanos < 250000000L) {
                    double cents = 1200.0 * Math.log(frequency / lastFrequency) / Math.log(2.0);
                    if (Math.abs(cents) < 18.0) {
                        frequency = lastFrequency * settings.pitchSmoothing
                                + frequency * (1f - settings.pitchSmoothing);
                    }
                }
                lastFrequency = frequency;
                lastPitchNanos = now;
            }
            result.valid = true;
            result.frequency = frequency;
            result.midiFloat = Notes.hzToMidi(frequency, settings.a4);
            result.setTemperamentTarget(settings.a4, settings.selectedTemperamentCents());
        }
        final PitchResult delivered = result;
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onPitch(delivered);
            }
        });
    }


    /**
     * Instrument mode tunes strings, not arbitrary high harmonics. Fold an
     * upper harmonic back to the string it belongs to and reject notes that are
     * not close to any string in the selected tuning. This is what prevents a
     * bright harmonic from being reported as an in-tune high note.
     */
    private double foldToInstrument(double raw) {
        Tuning tuning = settings.selectedTuning();
        if (tuning == null || tuning.notes.length == 0) {
            return raw;
        }
        float[] temperament = settings.selectedTemperamentCents();
        double lowest = Double.MAX_VALUE;
        double highest = 0;
        for (int midi : tuning.notes) {
            double hz = Notes.targetHz(midi, settings.a4, temperament);
            if (hz < lowest) {
                lowest = hz;
            }
            if (hz > highest) {
                highest = hz;
            }
        }
        double min = lowest * Math.pow(2.0, -3.0 / 12.0);
        double max = highest * Math.pow(2.0, 3.0 / 12.0);
        double bestHz = 0;
        double bestCents = 1e9;
        // Check the fundamental and the harmonics selected by the user.
        for (int divisor = 1; divisor <= settings.harmonicDepth; divisor++) {
            double candidate = raw / divisor;
            if (candidate < min * 0.94 || candidate > max * 1.06) {
                continue;
            }
            for (int midi : tuning.notes) {
                double target = Notes.targetHz(midi, settings.a4, temperament);
                double cents = 1200.0 * Math.log(candidate / target) / Math.log(2.0);
                if (Math.abs(cents) < Math.abs(bestCents)) {
                    bestCents = cents;
                    bestHz = candidate;
                }
            }
        }
        return Math.abs(bestCents) <= 80.0 ? bestHz : 0;
    }


    @Override
    public void onSamples(short[] data, int count, int sampleRate) {
        synchronized (lock) {
            for (int i = 0; i < count; i++) {
                ring[ringWrite] = data[i] / 32768.0f;
                ringWrite = (ringWrite + 1) % RING_SIZE;
            }
            ringTotal += count;
        }
    }

    @Override
    public void onAudioError(String message) {
        if (audioErrorReported) {
            return;
        }
        audioErrorReported = true;
        final String text = message;
        main.post(new Runnable() {
            @Override
            public void run() {
                listener.onAudioError(text);
            }
        });
    }

    public void stop() {
        running = false;
        capture.stop();
        Thread thread = analysisThread;
        analysisThread = null;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        demoSource = null;
    }

    /** Generates useful test signals in debuggable builds only. */
    private final class DemoSource {
        private final int sampleRate;
        private final Random random = new Random(7);
        private double phase;
        private double phase2;
        private double phase3;
        private final double[] chordPhase = new double[3];
        private double sweepStart;
        private long lastFill;

        DemoSource(int sampleRate) {
            this.sampleRate = sampleRate;
            this.sweepStart = System.nanoTime();
        }

        void fill(float[] ring, Object lock, PitchEngine engine) {
            long now = System.nanoTime();
            if (lastFill == 0) {
                lastFill = now;
                return;
            }
            int want = (int) ((now - lastFill) * sampleRate / 1_000_000_000L);
            lastFill = now;
            if (want <= 0) {
                return;
            }
            if (want > sampleRate / 5) {
                want = sampleRate / 5;
            }
            int mode = engine.settings.viewMode;
            // A slowly detuned A4 gives the strobe displays visible motion.
            double t = (now - sweepStart) / 1_000_000_000.0;
            double cents = 7.0 * Math.sin(t * 0.85) + 0.6 * Math.sin(t * 4.3);
            if (mode == 3) {
                // Chords: cycle through A major, D major and E minor every 2.4s.
                // Keep per-voice phase continuous across audio blocks; otherwise
                // the test signal produces spectral splatter that looks like chord
                // changes.
                int idx = (int) (t / 2.4) % 3;
                double[][] chords = {{110.0, 277.18, 329.63}, {146.83, 369.99, 440.0}, {82.41, 196.00, 246.94}};
                double[] chord = chords[idx];
                for (int i = 0; i < want; i++) {
                    double v = 0;
                    for (int voice = 0; voice < 3; voice++) {
                        chordPhase[voice] += 2 * Math.PI * chord[voice] / sampleRate;
                        v += 0.22 * Math.sin(chordPhase[voice]);
                    }
                    write(ring, (float) v);
                }
            } else {
                double f = 329.6276 * Math.pow(2.0, cents / 1200.0);
                double step = 2 * Math.PI * f / sampleRate;
                double step2 = step * 2.0;
                double step3 = step * 3.0;
                for (int i = 0; i < want; i++) {
                    phase += step;
                    phase2 += step2;
                    phase3 += step3;
                    double v = 0.40 * Math.sin(phase) + 0.18 * Math.sin(phase2)
                            + 0.08 * Math.sin(phase3) + 0.002 * (random.nextDouble() - 0.5);
                    write(ring, (float) v);
                }
            }
        }

        private void write(float[] ring, float value) {
            synchronized (lock) {
                ring[ringWrite] = value;
                ringWrite = (ringWrite + 1) % RING_SIZE;
                ringTotal++;
            }
        }
    }
}
