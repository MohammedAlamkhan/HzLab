// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Locale;

/**
 * The five HzLab visualisers, drawn with the platform Canvas.
 * No native graphics or DSP library is required.
 */
public final class HzLabView extends View {
    public static final int MODE_INSTRUMENT = 0;
    public static final int MODE_DIAL = 1;
    public static final int MODE_SPECTROGRAM = 2;
    public static final int MODE_CHORDS = 3;
    public static final int MODE_BIG_STROBE = 4;
    public static final String[] MODE_NAMES = {
            "INSTRUMENT", "DIAL & STROBE", "SPECTROGRAM", "CHORDS", "BIG STROBE"
    };

    public interface Listener {
        void onRequestOptions();

        void onRequestPermission();

        void onModeChanged(int mode);
    }

    private static final int BG = 0xff000000;
    private static final int TEXT = 0xffe7e7e7;
    private static final int DIM = 0xff808080;
    private static final int FAINT = 0xff3c3c3c;
    private static final int GRID = 0xff464646;
    private static final int BLUE = 0xff54aeff;
    private static final int CYAN = 0xff84dcff;
    private static final int GREEN = 0xff70dd91;
    private static final int AMBER = 0xffffc45c;

    private final Settings settings;
    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF oval = new RectF();
    private final GestureDetector gesture;

    private Typeface condensed;
    private Typeface regular;
    private float density;

    private PitchResult result;
    private long lastResultNanos;
    private int mode;
    private int targetString = -1;
    private double targetCents;
    private double targetFrequency;
    private double displayCents;
    private double displayHz;
    private double strobePhase;
    private long lastFrameNanos;
    private long modeLabelUntil;
    private boolean showPermission;
    private String audioMessage;
    private boolean audioStarted;
    private boolean attached;

    private int[] spectroPixels;
    private Bitmap spectroBitmap;
    private long lastSpectroFrame;
    private final RectF spectroRect = new RectF();
    private final float[] chordScores = new float[72];
    private final float[] smoothedChordChroma = new float[12];
    private final float[] smoothedBassChroma = new float[12];
    private long lastChordFrame;
    private long lastChordResultTimestamp;
    private long lastChordDataNanos;
    private static final int SPECTRO_WIDTH = 640;
    private static final int SPECTRO_ROWS = PitchDetector.SPECTRO_BINS;
    private static final int SPECTRO_BINS_PER_SEMITONE = SPECTRO_ROWS / 12;

    private final int[] spectroPalette = {
            0xff5bd75b, 0xffb8e34a, 0xff44d6a1, 0xff35cfe0,
            0xff3f9fff, 0xff7d7cff, 0xffb06cff, 0xffff5be0,
            0xffff5b9e, 0xffff5d5d, 0xffff9b4a, 0xffffe45c
    };

    private static final String[] CHORD_LABELS = {"maj", "maj7", "dom7", "min", "min7", "dim"};
    private static final int[][] CHORD_INTERVALS = {
            {0, 4, 7}, {0, 4, 7, 11}, {0, 4, 7, 10}, {0, 3, 7}, {0, 3, 7, 10}, {0, 3, 6, 9}
    };

    public HzLabView(Context context, Settings settings, Listener listener) {
        super(context);
        this.settings = settings;
        this.listener = listener;
        this.mode = settings.viewMode;
        density = getResources().getDisplayMetrics().density;
        condensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        regular = Typeface.create("sans-serif", Typeface.NORMAL);
        paint.setTypeface(condensed);
        stroke.setStyle(Paint.Style.STROKE);
        gesture = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                listener.onRequestOptions();
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dx) > dp(44) && Math.abs(velocityX) > Math.abs(velocityY) * 1.4f) {
                    if (dx < 0) {
                        setMode((mode + 1) % MODE_NAMES.length, true);
                    } else {
                        setMode((mode + MODE_NAMES.length - 1) % MODE_NAMES.length, true);
                    }
                    return true;
                }
                return false;
            }
        });
        setBackgroundColor(BG);
        setKeepScreenOn(true);
    }

    public void setPitch(PitchResult pitch) {
        this.result = pitch;
        this.lastResultNanos = SystemClock.elapsedRealtimeNanos();
        if (pitch != null) {
            if (pitch.valid) {
                if (displayHz <= 0) {
                    displayHz = pitch.frequency;
                } else {
                    double jump = 1200.0 * Math.log(pitch.frequency / displayHz) / Math.log(2.0);
                    displayHz = Math.abs(jump) > 90 ? pitch.frequency : displayHz * 0.55 + pitch.frequency * 0.45;
                }
                double nextCents = pitch.cents;
                if (Math.abs(nextCents - displayCents) > 80) {
                    displayCents = nextCents;
                } else {
                    displayCents = displayCents * settings.pitchSmoothing
                            + nextCents * (1.0 - settings.pitchSmoothing);
                }
                updateTargetString();
            } else {
                targetString = -1;
            }
            updateSpectrogram(pitch);
            updateChordScores(pitch);
        }
        invalidate();
    }

    public void setPermissionNeeded(boolean needed) {
        showPermission = needed;
        if (needed) {
            audioStarted = false;
        }
        invalidate();
    }

    public void setAudioError(String message) {
        audioMessage = message;
        audioStarted = false;
        invalidate();
    }

    public void setAudioStarted(boolean started) {
        audioStarted = started;
        if (started) {
            audioMessage = null;
            showPermission = false;
        }
        invalidate();
    }

    public boolean hasAudio() {
        return audioStarted;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int newMode, boolean notify) {
        if (newMode < 0 || newMode >= MODE_NAMES.length || newMode == mode) {
            return;
        }
        mode = newMode;
        if (mode == MODE_CHORDS) {
            java.util.Arrays.fill(chordScores, 0f);
            java.util.Arrays.fill(smoothedChordChroma, 0f);
            java.util.Arrays.fill(smoothedBassChroma, 0f);
            lastChordFrame = 0;
            lastChordResultTimestamp = 0;
            lastChordDataNanos = 0;
        }
        settings.viewMode = newMode;
        settings.viewedMask |= 1 << newMode;
        modeLabelUntil = SystemClock.elapsedRealtime() + 1800;
        targetString = -1;
        if (notify) {
            listener.onModeChanged(mode);
        }
        invalidate();
    }

    public void refreshSettings() {
        targetString = -1;
        if (result != null && result.valid) {
            updateTargetString();
        }
        invalidate();
    }

    private void updateTargetString() {
        if (result == null || !result.valid) {
            targetString = -1;
            return;
        }
        Tuning tuning = settings.selectedTuning();
        float[] temperament = settings.selectedTemperamentCents();
        int best = -1;
        double bestCents = 1e9;
        double bestHz = 0;
        for (int i = 0; i < tuning.notes.length && i < 12; i++) {
            int midi = tuning.notes[i];
            double target = Notes.targetHz(midi, settings.a4, temperament);
            double cents = 1200.0 * Math.log(result.frequency / target) / Math.log(2.0);
            // If a string is more than a whole tone away it is not the string
            // being played; don't pretend it is.
            if (Math.abs(cents) < 200 && Math.abs(cents) < Math.abs(bestCents)) {
                bestCents = cents;
                best = i;
                bestHz = target;
            }
        }
        targetString = best;
        targetCents = bestCents;
        targetFrequency = bestHz;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        lastFrameNanos = 0;
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (gearHit(event.getX(), event.getY())) {
                listener.onRequestOptions();
                return true;
            }
            if (showPermission || audioMessage != null) {
                listener.onRequestPermission();
                return true;
            }
        }
        gesture.onTouchEvent(event);
        return true;
    }

    private boolean gearHit(float x, float y) {
        int w = getWidth();
        int h = getHeight();
        return x > w - dp(62)
                && (y < dp(92) || y > h - dp(64));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0.016f : Math.min(0.05f, (now - lastFrameNanos) / 1e9f);
        lastFrameNanos = now;

        boolean voiced = result != null && result.valid && (SystemClock.elapsedRealtimeNanos() - lastResultNanos) < 600000000L;
        if (voiced) {
            // Flat motion moves left, sharp motion moves right. The ring uses
            // the equivalent angular motion.
            strobePhase += dt * clamp(displayCents, -50, 50) * 2.4;
        } else {
            strobePhase += dt * 7.0;
            displayCents *= 0.9;
            if (displayCents < 0.02 && displayCents > -0.02) {
                displayCents = 0;
            }
        }

        switch (mode) {
            case MODE_DIAL:
                drawDial(canvas, voiced);
                break;
            case MODE_SPECTROGRAM:
                drawSpectrogram(canvas, voiced);
                break;
            case MODE_CHORDS:
                drawChords(canvas, voiced);
                break;
            case MODE_BIG_STROBE:
                drawBigStrobe(canvas, voiced);
                break;
            case MODE_INSTRUMENT:
            default:
                drawInstrument(canvas, voiced);
                break;
        }
        drawChrome(canvas, now);
        if (showPermission || audioMessage != null) {
            drawAudioOverlay(canvas);
        }
        if (attached) {
            postInvalidateOnAnimation();
        }
    }

    // ---------------------------------------------------------------------
    // Instrument

    private void drawInstrument(Canvas canvas, boolean voiced) {
        int w = getWidth();
        int h = getHeight();
        Tuning tuning = settings.selectedTuning();
        int count = Math.min(12, tuning.notes.length);
        float cx = w * 0.50f;
        float cy = h * 0.455f;
        float rx = Math.min(w * 0.425f, h * 0.285f);
        float ry = Math.min(rx, h * 0.335f);
        float spacing = count <= 1 ? 0 : Math.min(h * 0.062f, (ry * 1.65f) / (count - 1));
        float firstY = cy - (count - 1) * spacing / 2f;

        // String rails.
        paint.setTypeface(condensed);
        for (int i = 0; i < count; i++) {
            float y = firstY + i * spacing;
            boolean active = i == targetString;
            stroke.setColor(active ? 0xffc8c8c8 : 0xff5a5a5a);
            stroke.setStrokeWidth(active ? dp(1.4f) : dp(0.8f));
            canvas.drawLine(0, y, w, y, stroke);
            if (active) {
                stroke.setColor(0x3354aeff);
                stroke.setStrokeWidth(dp(7));
                canvas.drawLine(0, y, w, y, stroke);
            }
            paint.setColor(active ? CYAN : TEXT);
            float labelSize = Math.min(font(22f), spacing * 0.44f);
            paint.setTextSize(labelSize);
            paint.setTextAlign(Paint.Align.LEFT);
            String note = Notes.name(tuning.notes[i], settings.noteStyle);
            canvas.drawText(note, dp(18), y + labelSize * 0.32f, paint);
            if (active) {
                paint.setColor(BLUE);
                canvas.drawCircle(dp(7), y, dp(3), paint);
            }
        }

        // Outer tuning ring.
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        stroke.setColor(0xff6f6f6f);
        stroke.setStrokeWidth(dp(1.5f));
        canvas.drawOval(oval, stroke);

        // Fine radial ticks form the outer scale.
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            boolean major = i % 5 == 0;
            float x0 = (float) (cx + Math.cos(angle) * rx * 0.925f);
            float y0 = (float) (cy + Math.sin(angle) * ry * 0.925f);
            float inner = major ? 0.865f : 0.885f;
            float x1 = (float) (cx + Math.cos(angle) * rx * inner);
            float y1 = (float) (cy + Math.sin(angle) * ry * inner);
            stroke.setColor(major ? 0xffb0b0b0 : 0xff666666);
            stroke.setStrokeWidth(major ? dp(1.5f) : dp(0.8f));
            canvas.drawLine(x0, y0, x1, y1, stroke);
        }

        // A fine segmented band rotates around the ring. It is deliberately
        // subtle: the blue deviation arc remains the primary tuning cue.
        int segments = 72;
        float sweep = 360f / segments;
        float phaseOffset = (float) ((strobePhase * 2.4) % sweep);
        oval.set(cx - rx * 0.855f, cy - ry * 0.80f, cx + rx * 0.855f, cy + ry * 0.80f);
        stroke.setStrokeWidth(dp(1.8f));
        for (int i = 0; i < segments; i++) {
            float start = i * sweep + phaseOffset;
            boolean bright = (i & 1) == 0;
            stroke.setColor(bright ? 0xffb4b4b4 : 0xff303030);
            canvas.drawArc(oval, start, sweep * 0.58f, false, stroke);
        }

        // The blue arc is the radial deviation indicator: centred at the bottom
        // when perfectly in tune, moving right when sharp.
        if (voiced && targetString >= 0) {
            float base = 90f + (float) clamp(displayCents, -50, 50) * 0.75f;
            float arcSweep = 42f;
            oval.set(cx - rx * 1.005f, cy - ry * 0.985f, cx + rx * 1.005f, cy + ry * 0.985f);
            stroke.setColor(0x3354aeff);
            stroke.setStrokeWidth(dp(10));
            canvas.drawArc(oval, base - arcSweep / 2, arcSweep, false, stroke);
            stroke.setColor(BLUE);
            stroke.setStrokeWidth(dp(2.4f));
            canvas.drawArc(oval, base - arcSweep / 2, arcSweep, false, stroke);
            paint.setColor(BLUE);
            canvas.drawCircle((float) (cx + Math.cos(Math.toRadians(base)) * rx * 1.005f),
                    (float) (cy + Math.sin(Math.toRadians(base)) * ry * 0.985f), dp(3.4f), paint);
        }

        // Direction prompt and note readout.
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(condensed);
        if (voiced) {
            double cents = targetString >= 0 ? targetCents : displayCents;
            String prompt;
            int colour;
            if (targetString < 0) {
                prompt = "NO STRING";
                colour = AMBER;
            } else if (Math.abs(cents) <= settings.inTuneThreshold) {
                prompt = "IN TUNE!";
                colour = GREEN;
            } else if (cents < 0) {
                prompt = "TUNE UP";
                colour = TEXT;
            } else {
                prompt = "TUNE DOWN";
                colour = TEXT;
            }
            paint.setColor(colour);
            paint.setTextSize(font(16f));
            canvas.drawText(prompt, cx + rx * 0.48f, cy - ry * 0.92f, paint);
            if (targetString >= 0) {
                paint.setColor(CYAN);
                paint.setTextSize(font(20f));
                String note = Notes.name(tuning.notes[targetString], settings.noteStyle);
                canvas.drawText(note + "   " + Notes.formatHz(result.frequency), cx, cy, paint);
            } else {
                paint.setColor(CYAN);
                paint.setTextSize(font(20f));
                String note = Notes.name(result.midi, settings.noteStyle);
                canvas.drawText(note + "   " + Notes.formatHz(result.frequency), cx, cy, paint);
            }
        } else if (!showPermission && audioMessage == null) {
            paint.setColor(0xff6e6e6e);
            paint.setTextSize(font(16f));
            canvas.drawText("PLAY A NOTE", cx, cy, paint);
            paint.setTextSize(font(11f));
            canvas.drawText("LISTENING FOR PITCH", cx, cy + dp(24), paint);
        }

        paint.setColor(TEXT);
        paint.setTextSize(font(17f));
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(tuning.displayName(), cx, h - dp(35), paint);
    }

    // ---------------------------------------------------------------------
    // Dial & Strobe

    private void drawDial(Canvas canvas, boolean voiced) {
        int w = getWidth();
        int h = getHeight();
        float cx = w * 0.5f;
        float cy = h * 0.36f;
        float radius = Math.min(w * 0.42f, h * 0.26f);
        float cents = voiced ? (float) displayCents : 0f;

        // Scale arc.
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);
        stroke.setColor(0xff656565);
        stroke.setStrokeWidth(dp(1.5f));
        canvas.drawArc(oval, 200, 140, false, stroke);

        for (int i = -50; i <= 50; i++) {
            float angle = (float) Math.toRadians(270 + i * 1.4);
            boolean major = i % 10 == 0;
            boolean middle = i % 5 == 0;
            float outer = radius;
            float inner = radius - (major ? dp(15) : middle ? dp(10) : dp(6));
            float x0 = (float) (cx + Math.cos(angle) * inner);
            float y0 = (float) (cy + Math.sin(angle) * inner);
            float x1 = (float) (cx + Math.cos(angle) * outer);
            float y1 = (float) (cy + Math.sin(angle) * outer);
            stroke.setColor(major ? 0xffbcbcbc : 0xff777777);
            stroke.setStrokeWidth(major ? dp(1.6f) : dp(0.9f));
            canvas.drawLine(x0, y0, x1, y1, stroke);
            if (major) {
                paint.setColor(i == 0 ? TEXT : 0xffa0a0a0);
                paint.setTextSize(font(13f));
                paint.setTextAlign(Paint.Align.CENTER);
                float tx = (float) (cx + Math.cos(angle) * (radius + dp(17)));
                float ty = (float) (cy + Math.sin(angle) * (radius + dp(17))) + font(13f) * 0.34f;
                canvas.drawText(i == 0 ? "0" : (i > 0 ? "+" + i : Integer.toString(i)), tx, ty, paint);
            }
        }

        // The two linear strobe windows behind the needle. The pattern moves
        // horizontally with the pitch error.
        float bandTop = cy + radius * 0.15f;
        float bandHeight = radius * 0.44f;
        float left = cx - radius * 0.70f;
        float right = cx + radius * 0.70f;
        drawStrobeWindow(canvas, left, bandTop, cx - dp(7), bandTop + bandHeight, false);
        drawStrobeWindow(canvas, cx + dp(7), bandTop, right, bandTop + bandHeight, true);

        // Needle.
        float angle = (float) Math.toRadians(270 + cents * 1.4);
        float nx = (float) (cx + Math.cos(angle) * radius * 0.72f);
        float ny = (float) (cy + Math.sin(angle) * radius * 0.72f);
        stroke.setColor(0x552b8fff);
        stroke.setStrokeWidth(dp(13));
        canvas.drawLine(cx, cy, nx, ny, stroke);
        stroke.setColor(BLUE);
        stroke.setStrokeWidth(dp(3));
        canvas.drawLine(cx, cy, nx, ny, stroke);
        paint.setColor(BLUE);
        canvas.drawCircle(cx, cy, dp(5), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(condensed);
        if (voiced) {
            paint.setColor(TEXT);
            paint.setTextSize(font(62f));
            String note = Notes.name(result.midi, settings.noteStyle);
            canvas.drawText(note, cx, h * 0.70f, paint);
            paint.setColor(0xffb4b4b4);
            paint.setTextSize(font(22f));
            canvas.drawText(Notes.formatHz(result.frequency), cx, h * 0.755f, paint);
            paint.setColor(Math.abs(cents) <= settings.inTuneThreshold ? GREEN : TEXT);
            paint.setTextSize(font(15f));
            String direction = Math.abs(cents) <= settings.inTuneThreshold ? "IN TUNE!"
                    : cents < 0 ? "TUNE UP" : "TUNE DOWN";
            canvas.drawText(direction + "   " + Notes.formatCents(cents) + " cents", cx, h * 0.80f, paint);
        } else if (!showPermission && audioMessage == null) {
            paint.setColor(0xff7d7d7d);
            paint.setTextSize(font(44f));
            canvas.drawText("--", cx, h * 0.70f, paint);
            paint.setTextSize(font(18f));
            canvas.drawText("PLAY A NOTE", cx, h * 0.755f, paint);
        }
    }

    private void drawStrobeWindow(Canvas canvas, float left, float top, float right, float bottom, boolean rightSide) {
        if (right <= left) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x55f2f2f2);
        canvas.drawRect(left, top, right, bottom, paint);
        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        float stripe = Math.max(dp(9), (right - left) / 11f);
        float offset = (float) ((strobePhase * 3.3) % (stripe * 2));
        paint.setColor(0xccf7f7f7);
        for (float x = left - stripe * 2 + offset; x < right + stripe; x += stripe * 2) {
            canvas.drawRect(x, top, x + stripe, bottom, paint);
        }
        paint.setColor(0x22000000);
        for (float x = left - stripe + offset; x < right + stripe; x += stripe * 2) {
            canvas.drawRect(x, top, x + stripe * 0.45f, bottom, paint);
        }
        canvas.restore();
        paint.setColor(0xff111111);
        canvas.drawRect((rightSide ? right - dp(1) : left), top, (rightSide ? right : left + dp(1)), bottom, paint);
    }

    // ---------------------------------------------------------------------
    // Big strobe

    private void drawBigStrobe(Canvas canvas, boolean voiced) {
        int w = getWidth();
        int h = getHeight();
        float top = h * 0.28f;
        float bottom = h * 0.56f;
        int columns = 18;
        float cell = w / (float) columns;
        float offset = (float) ((strobePhase * 4.0) % (cell * 2));
        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < 3; row++) {
            float y0 = top + row * (bottom - top) / 3f;
            float y1 = y0 + (bottom - top) / 3f - dp(3);
            paint.setColor(0x22ffffff);
            canvas.drawRect(0, y0, w, y1, paint);
            for (int i = -1; i < columns + 1; i++) {
                float x = i * cell + offset;
                if (((i & 1) == 0)) {
                    paint.setColor(0xccf3f3f3);
                } else {
                    paint.setColor(0x22000000);
                }
                canvas.drawRect(x + dp(1), y0, x + cell - dp(1), y1, paint);
            }
        }

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(condensed);
        if (voiced) {
            paint.setColor(TEXT);
            paint.setTextSize(font(60f));
            canvas.drawText(Notes.name(result.midi, settings.noteStyle), w * 0.5f, h * 0.69f, paint);
            paint.setColor(0xffb8b8b8);
            paint.setTextSize(font(22f));
            canvas.drawText(Notes.formatHz(result.frequency), w * 0.5f, h * 0.745f, paint);
            boolean tuned = Math.abs(displayCents) <= settings.inTuneThreshold;
            paint.setColor(tuned ? GREEN : TEXT);
            paint.setTextSize(font(16f));
            canvas.drawText((tuned ? "IN TUNE!" : displayCents < 0 ? "TUNE UP" : "TUNE DOWN")
                    + "   " + Notes.formatCents(displayCents) + " cents", w * 0.5f, h * 0.80f, paint);
        } else {
            paint.setColor(0xff777777);
            paint.setTextSize(font(18f));
            canvas.drawText("PLAY A NOTE", w * 0.5f, h * 0.71f, paint);
        }
    }

    // ---------------------------------------------------------------------
    // Spectrogram

    private void ensureSpectrogram() {
        if (spectroPixels == null) {
            spectroPixels = new int[SPECTRO_WIDTH * SPECTRO_ROWS];
            spectroBitmap = Bitmap.createBitmap(SPECTRO_WIDTH, SPECTRO_ROWS, Bitmap.Config.ARGB_8888);
        }
    }

    private void updateSpectrogram(PitchResult pitch) {
        ensureSpectrogram();
        if (pitch == null || pitch.timestampNanos == lastSpectroFrame) {
            return;
        }
        lastSpectroFrame = pitch.timestampNanos;
        int shift = Math.max(1, Math.min(4, settings.spectroScroll));
        System.arraycopy(spectroPixels, shift, spectroPixels, 0, spectroPixels.length - shift);
        for (int column = 0; column < shift; column++) {
            int base = SPECTRO_WIDTH - shift + column;
            for (int b = 0; b < SPECTRO_ROWS; b++) {
                // Bitmap row 0 is displayed at the top. Bin 240 is just above B,
                // bin 0 just below C; the note lines sit at bins 10, 30, ... 230.
                int pixelRow = SPECTRO_ROWS - 1 - b;
                float value = pitch.spectro[b];
                if (pitch.rms < 0.0012f) {
                    value = 0f;
                } else {
                    value = Math.max(0f, (value - 0.06f) / 0.94f);
                }
                value = Math.min(1f, value);
                int pc = Math.min(11, Math.max(0, (int) ((b + 0.5f) / SPECTRO_BINS_PER_SEMITONE)));
                int alpha = (int) (255 * Math.pow(value, 0.82));
                int colour = spectroPalette[pc];
                int r = (colour >> 16) & 0xff;
                int g = (colour >> 8) & 0xff;
                int bl = colour & 0xff;
                int rr = (int) (r * (0.28f + 0.72f * value));
                int gg = (int) (g * (0.28f + 0.72f * value));
                int bb = (int) (bl * (0.28f + 0.72f * value));
                spectroPixels[pixelRow * SPECTRO_WIDTH + base] = Color.argb(alpha, rr, gg, bb);
            }
        }
        spectroBitmap.setPixels(spectroPixels, 0, SPECTRO_WIDTH, 0, 0, SPECTRO_WIDTH, SPECTRO_ROWS);
    }

    private float spectroY(float bin, float top, float bottom) {
        return bottom - (bin / (float) SPECTRO_ROWS) * (bottom - top);
    }

    private void drawSpectrogram(Canvas canvas, boolean voiced) {
        int w = getWidth();
        int h = getHeight();
        float left = w * 0.145f;
        float right = w * 0.97f;
        float top = h * 0.045f;
        float bottom = h * 0.955f;
        if (spectroBitmap == null) {
            ensureSpectrogram();
        }

        // Main scrolling pitch field. Bilinear filtering makes the 20-bin-per-
        // semitone data smooth enough to see vibrato and microtonal movement.
        spectroRect.set(left, top, right, bottom);
        boolean oldFilter = paint.isFilterBitmap();
        paint.setFilterBitmap(true);
        canvas.drawBitmap(spectroBitmap, null, spectroRect, paint);
        paint.setFilterBitmap(oldFilter);

        // Note grid. Each line is the centre of a semitone, so a correctly
        // tuned note sits on its line and vibrato visibly swings around it.
        paint.setTypeface(condensed);
        for (int pc = 0; pc < 12; pc++) {
            float bin = (pc + 0.5f) * SPECTRO_BINS_PER_SEMITONE;
            float y = spectroY(bin, top, bottom);
            stroke.setColor(pc == 0 ? 0xff777777 : 0xff4a4a4a);
            stroke.setStrokeWidth(dp(1));
            canvas.drawLine(left, y, right, y, stroke);
            paint.setColor(0xffdedede);
            paint.setTextSize(font(20f));
            paint.setTextAlign(Paint.Align.LEFT);
            String label = Notes.pitchClass(pc, settings.noteStyle);
            canvas.drawText(label, dp(13), y + font(20f) * 0.34f, paint);
        }
        stroke.setColor(0xff5b5b5b);
        stroke.setStrokeWidth(dp(1));
        canvas.drawRect(left, top, right, bottom, stroke);
        stroke.setColor(0xff262626);
        for (int i = 1; i < 12; i++) {
            float x = left + (right - left) * i / 12f;
            canvas.drawLine(x, top, x, bottom, stroke);
        }

        // Live pitch cursor. This is deliberately separate from the history:
        // the cursor shows exactly where the current note sits within the
        // semitone, while the coloured trail preserves the last few seconds.
        if (result != null && result.rawFrequency > 0 && result.rms > 0.0012f) {
            double midi = Notes.hzToMidi(result.rawFrequency, settings.a4);
            int nearest = (int) Math.round(midi);
            double cents = (midi - nearest) * 100.0;
            double position = ((midi % 12.0) + 12.0) % 12.0;
            float bin = (float) ((position + 0.5) * SPECTRO_BINS_PER_SEMITONE);
            float y = spectroY(bin, top, bottom);
            stroke.setColor(0x7754aeff);
            stroke.setStrokeWidth(dp(1.2f));
            canvas.drawLine(left, y, right, y, stroke);
            paint.setColor(CYAN);
            canvas.drawCircle(right - dp(4), y, dp(3), paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(font(13f));
            String readout = Notes.name(nearest, settings.noteStyle) + "  " + Notes.formatCents(cents) + " cents";
            canvas.drawText(readout, left + dp(2), dp(26), paint);
        } else {
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(font(11f));
            paint.setColor(0xff666666);
            canvas.drawText("PLAY A NOTE", left + dp(2), dp(26), paint);
        }

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(font(11f));
        paint.setColor(0xff6c6c6c);
        canvas.drawText("CHROMA / TIME  ·  5-cent resolution", dp(13), h - dp(31), paint);
    }

    // ---------------------------------------------------------------------
    // Chords

    private void drawChords(Canvas canvas, boolean voiced) {
        int w = getWidth();
        int h = getHeight();
        float left = dp(52);
        float right = w - dp(14);
        float top = dp(42);
        float bottom = h - dp(52);
        float cellW = (right - left) / 6f;
        float cellH = (bottom - top) / 12f;

        float[] scores = chordScores;
        int bestIndex = -1;
        float bestScore = 0;
        float secondScore = 0;
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > bestScore) {
                secondScore = bestScore;
                bestScore = scores[i];
                bestIndex = i;
            } else if (scores[i] > secondScore) {
                secondScore = scores[i];
            }
        }
        float coverage = 0;
        int strongTones = 0;
        if (bestIndex >= 0 && result != null) {
            int root = bestIndex / 6;
            int type = bestIndex % 6;
            float chordEnergy = 0;
            float totalEnergy = 0;
            for (int i = 0; i < 12; i++) {
                totalEnergy += result.chordChroma[i];
            }
            for (int interval : CHORD_INTERVALS[type]) {
                int pc = (root + interval) % 12;
                chordEnergy += result.chordChroma[pc];
                if (result.chordChroma[pc] > 0.18f) {
                    strongTones++;
                }
            }
            coverage = totalEnergy > 0.001f ? chordEnergy / totalEnergy : 0;
        }
        boolean recent = (SystemClock.elapsedRealtimeNanos() - lastChordFrame) < 500_000_000L;
        float chordDisplayGate = Math.max(0.002f, settings.chordGateRms * (0.002f / 0.0015f));
        boolean signal = recent && result != null && result.rms > chordDisplayGate && bestScore > 0.05f;
        // Show a chord tile whenever a plausible chord is present. Confidence
        // changes the brightness; it must not hide the whole grid during real
        // strummed chords, where 7th/relative-minor templates often run close.
        boolean display = signal && coverage >= 0.28f && strongTones >= 2;
        boolean confident = display && coverage >= 0.45f && strongTones >= 3
                && secondScore < bestScore * 0.94f;

        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < 12; row++) {
            int pc = 11 - row;
            float y = top + row * cellH;
            for (int col = 0; col < 6; col++) {
                float x = left + col * cellW;
                int index = pc * 6 + col;
                int fill = 0xff050505;
                if (display && index == bestIndex) {
                    fill = confident ? 0xff67c9ff : 0xff2f7fa8;
                } else if (display && scores[index] > bestScore * 0.85f) {
                    fill = 0xff1d4f6c;
                }
                paint.setColor(fill);
                canvas.drawRect(x + dp(1), y + dp(1), x + cellW - dp(1), y + cellH - dp(1), paint);
                stroke.setColor(0xff414141);
                stroke.setStrokeWidth(dp(0.8f));
                canvas.drawRect(x, y, x + cellW, y + cellH, stroke);
            }
            paint.setColor(0xffdedede);
            paint.setTextSize(font(19f));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(Notes.pitchClass(pc, settings.noteStyle), dp(13), y + cellH * 0.5f + font(19f) * 0.33f, paint);
        }

        // Rotated chord-type labels along the bottom edge.
        for (int col = 0; col < 6; col++) {
            float x = left + (col + 0.5f) * cellW;
            canvas.save();
            canvas.rotate(-90, x, bottom + dp(8));
            paint.setColor(0xffdedede);
            paint.setTextSize(font(16f));
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(CHORD_LABELS[col], x - cellH * 0.3f, bottom + dp(8), paint);
            canvas.restore();
        }
        stroke.setColor(0xff5a5a5a);
        stroke.setStrokeWidth(dp(1));
        canvas.drawRect(left, top, right, bottom, stroke);

        if (display) {
            int pc = bestIndex / 6;
            int type = bestIndex % 6;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(confident ? CYAN : 0xff8db8cb);
            paint.setTextSize(font(13f));
            canvas.drawText(Notes.pitchClass(pc, settings.noteStyle) + " " + CHORD_LABELS[type].toUpperCase(Locale.US),
                    (left + right) * 0.5f, top - dp(11), paint);
        }
    }

    private void updateChordScores(PitchResult pitch) {
        if (mode != MODE_CHORDS || pitch == null) {
            return;
        }
        if (pitch.timestampNanos == lastChordResultTimestamp) {
            return;
        }
        lastChordResultTimestamp = pitch.timestampNanos;
        if (pitch.rms < settings.chordGateRms) {
            return;
        }
        long now = SystemClock.elapsedRealtimeNanos();
        if (now - lastChordDataNanos > 450_000_000L) {
            java.util.Arrays.fill(smoothedChordChroma, 0f);
            java.util.Arrays.fill(smoothedBassChroma, 0f);
        }
        lastChordDataNanos = now;
        // The result timestamp uses System.nanoTime(), while the view uses the
        // elapsed-realtime clock. They have different zero points across deep
        // sleep on real phones, so never compare the two directly.
        lastChordFrame = now;
        float smoothing = settings.chordSmoothing;
        for (int i = 0; i < 12; i++) {
            smoothedChordChroma[i] = smoothedChordChroma[i] * smoothing
                    + pitch.chordChroma[i] * (1f - smoothing);
            smoothedBassChroma[i] = smoothedBassChroma[i] * smoothing
                    + pitch.bassChroma[i] * (1f - smoothing);
        }
        float[] raw = computeChordScores(smoothedChordChroma, smoothedBassChroma);
        System.arraycopy(raw, 0, chordScores, 0, chordScores.length);
        int best = 0;
        for (int i = 1; i < raw.length; i++) {
            if (raw[i] > raw[best]) {
                best = i;
            }
        }
    }

    private float[] computeChordScores(float[] c, float[] bass) {
        float[] scores = new float[72];
        float total = 0;
        for (float v : c) {
            total += v;
        }
        if (total < 0.01f) {
            return scores;
        }
        for (int root = 0; root < 12; root++) {
            for (int type = 0; type < 6; type++) {
                float hit = 0;
                float weight = 0;
                float weakestTone = 1f;
                int[] intervals = CHORD_INTERVALS[type];
                boolean[] chordTone = new boolean[12];
                for (int i = 0; i < intervals.length; i++) {
                    int pc = (root + intervals[i]) % 12;
                    chordTone[pc] = true;
                    float w = i == 0 ? 1.55f : 1.0f;
                    hit += c[pc] * w;
                    weight += w;
                    weakestTone = Math.min(weakestTone, c[pc]);
                }
                float outside = 0;
                for (int pc = 0; pc < 12; pc++) {
                    if (!chordTone[pc]) {
                        outside += c[pc];
                    }
                }
                float score = (hit / weight - outside * 0.30f)
                        * (0.40f + 0.60f * weakestTone);
                // Seventh/diminished chords must earn their extra note; this
                // stops a weak harmonic from turning a major triad into maj7.
                if (type != 0 && type != 3) {
                    score *= 0.92f;
                }
                // Reward a strong root and a strong low note.
                score += c[root] * 0.18f;
                // The lowest note in a guitar chord is normally the root. A
                // dedicated bass chroma resolves common confusions such as
                // Em (E-G-B) versus G (G-B-D).
                score += bass[root] * 0.22f;
                scores[root * 6 + type] = Math.max(0, score);
            }
        }
        float max = 0;
        for (float s : scores) {
            max = Math.max(max, s);
        }
        if (max > 0) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] /= max;
            }
        }
        return scores;
    }

    // ---------------------------------------------------------------------
    // Chrome and status

    private void drawChrome(Canvas canvas, long now) {
        int w = getWidth();
        int h = getHeight();
        paint.setTypeface(condensed);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(0xff777777);
        paint.setTextSize(font(11f));
        if (now < modeLabelUntil) {
            canvas.drawText(MODE_NAMES[mode], dp(18), dp(26), paint);
        }

        // Kebab menu. This makes the old long-press menu discoverable on modern
        // phones while keeping long-press as a secondary gesture.
        paint.setColor(0xffb8b8b8);
        float gx = w - dp(25);
        float gy = dp(58);
        for (int i = -1; i <= 1; i++) {
            canvas.drawCircle(gx, gy + i * dp(5.2f), dp(1.7f), paint);
        }
        // A second, always reachable menu affordance sits above the mode dots.
        float my = h - dp(40);
        for (int i = -1; i <= 1; i++) {
            canvas.drawCircle(gx, my + i * dp(4.2f), dp(1.5f), paint);
        }

        // Mode dots.
        float dotY = h - dp(12);
        float gap = dp(14);
        float start = w * 0.5f - (MODE_NAMES.length - 1) * gap * 0.5f;
        for (int i = 0; i < MODE_NAMES.length; i++) {
            paint.setColor(i == mode ? BLUE : 0xff444444);
            canvas.drawCircle(start + i * gap, dotY, dp(i == mode ? 2.8f : 2.1f), paint);
        }

        if (audioMessage != null) {
            paint.setColor(AMBER);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(font(11f));
            canvas.drawText(audioMessage, w * 0.5f, h - dp(31), paint);
        }
    }

    private void drawAudioOverlay(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xdd000000);
        canvas.drawRect(0, 0, w, h, paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(condensed);
        paint.setColor(TEXT);
        paint.setTextSize(font(24f));
        canvas.drawText("MICROPHONE ACCESS", w * 0.5f, h * 0.43f, paint);
        paint.setColor(0xffb0b0b0);
        paint.setTextSize(font(14f));
        String message = audioMessage != null ? audioMessage
                : "HzLab needs the microphone to tune your instrument.";
        drawMultiline(canvas, message, w * 0.5f, h * 0.49f, w * 0.78f, font(14f), dp(21));
        paint.setColor(BLUE);
        paint.setTextSize(font(15f));
        canvas.drawText("TAP TO " + (audioMessage != null ? "RETRY" : "ALLOW ACCESS"), w * 0.5f, h * 0.61f, paint);
    }

    private void drawMultiline(Canvas canvas, String text, float cx, float top, float maxWidth, float textSize, float lineHeight) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float y = top;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), cx, y, paint);
                y += lineHeight;
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString(), cx, y, paint);
        }
    }

    private float dp(float value) {
        return value * density;
    }

    private float font(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
