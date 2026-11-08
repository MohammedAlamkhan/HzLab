// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Continuous sine reference tone generated in real time. */
public final class ToneEngine {
    private static final int SAMPLE_RATE = 44100;
    private static final int BLOCK = 256;

    private volatile double frequency = 440.0;
    private volatile float volume = 0.55f;
    private volatile boolean running;
    private AudioTrack track;
    private Thread thread;

    public void setFrequency(double hz) {
        if (hz > 20.0 && hz < 12000.0) {
            frequency = hz;
        }
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
                    .setBufferSizeInBytes(Math.max(min * 2, 8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.setVolume(volume);
            track.play();
        } catch (RuntimeException e) {
            android.util.Log.e("HzLabTone", "Could not start AudioTrack", e);
            releaseTrack();
            return;
        }
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                render();
            }
        }, "HzLab-Tone");
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
        double phase = 0;
        long samples = 0;
        while (running) {
            double step = 2.0 * Math.PI * frequency / SAMPLE_RATE;
            for (int i = 0; i < BLOCK; i++) {
                double fade = Math.min(1.0, samples / (SAMPLE_RATE * 0.012));
                double v = Math.sin(phase) * 0.24 * fade;
                int pcm = (int) Math.round(v * 32767.0);
                if (pcm > 32767) pcm = 32767;
                if (pcm < -32768) pcm = -32768;
                block[i] = (short) pcm;
                phase += step;
                if (phase > Math.PI * 2.0) {
                    phase -= Math.PI * 2.0;
                }
                samples++;
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
