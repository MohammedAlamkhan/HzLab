// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/** Sample-clocked metronome. No audio files or third-party libraries. */
public final class MetronomeEngine {
    public interface Listener {
        void onBeat(int beat, boolean accent);
    }

    private static final int SAMPLE_RATE = 44100;
    private static final int BLOCK = 256;

    private final Listener listener;
    private volatile int bpm = 100;
    private volatile int beats = 4;
    private volatile int subdivision = 1;
    private volatile boolean accent = true;
    private volatile float volume = 0.75f;
    private volatile boolean running;
    private AudioTrack track;
    private Thread thread;

    public MetronomeEngine(Listener listener) {
        this.listener = listener;
    }

    public void setBpm(int value) {
        bpm = Math.max(40, Math.min(240, value));
    }

    public void setBeats(int value) {
        beats = Math.max(2, Math.min(12, value));
    }

    public void setSubdivision(int value) {
        subdivision = value == 2 || value == 4 ? value : 1;
    }

    public void setAccent(boolean value) {
        accent = value;
    }

    public void setVolume(float value) {
        volume = Math.max(0f, Math.min(1f, value));
        AudioTrack t = track;
        if (t != null) {
            try {
                t.setVolume(volume);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) {
            return;
        }
        int min = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int buffer = Math.max(min * 2, 8192);
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(buffer)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.setVolume(volume);
            track.play();
        } catch (RuntimeException e) {
            android.util.Log.e("HzLabMetronome", "Could not start AudioTrack", e);
            releaseTrack();
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                render();
            }
        }, "HzLab-Metronome");
        thread.start();
    }

    public void stop() {
        running = false;
        AudioTrack t = track;
        if (t != null) {
            try {
                t.pause();
                t.flush();
            } catch (IllegalStateException ignored) {
            }
        }
        Thread worker = thread;
        thread = null;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        releaseTrack();
    }

    private void releaseTrack() {
        AudioTrack t = track;
        track = null;
        if (t != null) {
            try {
                t.stop();
            } catch (IllegalStateException ignored) {
            }
            try {
                t.release();
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void render() {
        short[] block = new short[BLOCK];
        long sample = 0;
        long lastClick = -1;
        long nextClick = 0;
        int clickCounter = 0;
        long clickStart = -1000000;
        boolean clickAccent = true;
        while (running) {
            int localBpm = bpm;
            int localBeats = beats;
            int localSubdivision = subdivision;
            double samplesPerClick = (60.0 / localBpm) * SAMPLE_RATE / localSubdivision;
            for (int i = 0; i < BLOCK; i++) {
                if (sample >= nextClick) {
                    lastClick = sample;
                    nextClick = lastClick + Math.max(1, Math.round(samplesPerClick));
                    clickStart = sample;
                    int beat = (clickCounter / localSubdivision) % localBeats;
                    clickAccent = accent && (clickCounter % (localBeats * localSubdivision) == 0);
                    if (listener != null) {
                        listener.onBeat(beat, clickAccent);
                    }
                    clickCounter++;
                }
                double elapsed = (sample - clickStart) / (double) SAMPLE_RATE;
                double v = 0;
                if (elapsed >= 0 && elapsed < 0.045) {
                    double env = Math.exp(-elapsed * 72.0);
                    double freq = clickAccent ? 1900.0 : 1350.0;
                    v = Math.sin(2.0 * Math.PI * freq * elapsed) * env * (clickAccent ? 0.62 : 0.42);
                }
                int pcm = (int) Math.round(v * 32767.0);
                if (pcm > 32767) pcm = 32767;
                if (pcm < -32768) pcm = -32768;
                block[i] = (short) pcm;
                sample++;
            }
            AudioTrack t = track;
            if (t == null) {
                break;
            }
            try {
                t.write(block, 0, BLOCK);
            } catch (IllegalStateException e) {
                break;
            }
        }
    }
}
