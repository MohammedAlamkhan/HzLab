// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;

/** Thin, modernised AudioRecord wrapper. */
public final class AudioCapture {
    public interface Listener {
        void onSamples(short[] data, int count, int sampleRate);
        void onAudioError(String message);
    }

    private AudioRecord record;
    private Thread thread;
    private volatile boolean running;
    private int sampleRate;
    private String lastError = "Could not start the microphone";

    public synchronized boolean start(final int requestedSampleRate, final Listener listener) {
        stop();
        sampleRate = requestedSampleRate;
        final int min = AudioRecord.getMinBufferSize(requestedSampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            lastError = "Microphone does not support " + requestedSampleRate + " Hz";
            return false;
        }
        final int bufferSize = Math.max(min * 2, requestedSampleRate / 5 * 2);
        try {
            record = new AudioRecord(MediaRecorder.AudioSource.MIC, requestedSampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                release();
                lastError = "Could not initialise the microphone at " + requestedSampleRate + " Hz";
                return false;
            }
            record.startRecording();
        } catch (SecurityException e) {
            release();
            lastError = "Microphone permission is required";
            return false;
        } catch (Exception e) {
            release();
            lastError = "Could not start the microphone at " + requestedSampleRate + " Hz";
            return false;
        }

        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
                final short[] buffer = new short[Math.max(1024, (int) (sampleRate * 0.04))];
                while (running) {
                    int read;
                    try {
                        read = record.read(buffer, 0, buffer.length);
                    } catch (Exception e) {
                        if (running) {
                            listener.onAudioError("Microphone read failed");
                        }
                        break;
                    }
                    if (read > 0) {
                        listener.onSamples(buffer, read, sampleRate);
                    } else if (read < 0 && running) {
                        listener.onAudioError("Microphone read error " + read);
                        break;
                    }
                }
            }
        }, "HzLab-Audio");
        thread.start();
        return true;
    }

    public synchronized void stop() {
        running = false;
        AudioRecord r = record;
        record = null;
        if (r != null) {
            try {
                r.stop();
            } catch (Exception ignored) {
            }
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
        Thread t = thread;
        thread = null;
        if (t != null) {
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void release() {
        if (record != null) {
            try {
                record.release();
            } catch (Exception ignored) {
            }
            record = null;
        }
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public String getLastError() {
        return lastError;
    }
}
