// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Settings screen for tuning, display and input preferences. */
public final class OptionsActivity extends Activity {
    private Settings settings;
    private LinearLayout content;
    private TextView subtitle;
    private Typeface condensed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = Settings.get(this);
        condensed = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff07131e);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackgroundColor(0xff0b1723);

        TextView back = new TextView(this);
        back.setText("<");
        back.setTextColor(0xffdceeff);
        back.setTextSize(24);
        back.setTypeface(condensed);
        back.setGravity(Gravity.CENTER);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setShape(GradientDrawable.OVAL);
        backBg.setColor(0xff132335);
        backBg.setStroke(dp(1), 0xff20384c);
        back.setBackground(backBg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(0xfff3f8fc);
        title.setTextSize(22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        subtitle = new TextView(this);
        subtitle.setTextColor(0xff91a7ba);
        subtitle.setTextSize(11);
        subtitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        View headerLine = new View(this);
        headerLine.setBackgroundColor(0xff20384c);
        root.addView(headerLine, new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(6), dp(16), dp(28));
        scroll.addView(content, new LinearLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        rebuildRows();
    }

    private void rebuildRows() {
        content.removeAllViews();
        subtitle.setText(settings.selectedTuning().displayName() + "  ·  A4 "
                + String.format(Locale.US, "%.1f", settings.a4) + " Hz");

        addSection("Instrument");
        addRow("Preset Instrument", "Pick a built-in tuning for guitar, bass, violin, banjo, ukulele, mandolin and more.", new Runnable() {
            @Override
            public void run() {
                chooseInstrument();
            }
        });
        addRow("Custom Instrument", "Create and manage your own string and note tunings.", new Runnable() {
            @Override
            public void run() {
                CustomEditors.editInstrumentPicker(OptionsActivity.this, settings, new Runnable() {
                    @Override
                    public void run() {
                        rebuildRows();
                    }
                });
            }
        });

        addSection("Tuning");
        addRow("A4 Frequency (hz)", "Set the reference pitch for A4.", new Runnable() {
            @Override
            public void run() {
                editA4();
            }
        });
        addRow("Preset Temperament", "Choose an equal or historical temperament.", new Runnable() {
            @Override
            public void run() {
                chooseTemperament();
            }
        });
        addRow("Custom Temperament", "Create a 12-note temperament with your own cent offsets.", new Runnable() {
            @Override
            public void run() {
                CustomEditors.editTemperamentPicker(OptionsActivity.this, settings, new Runnable() {
                    @Override
                    public void run() {
                        rebuildRows();
                    }
                });
            }
        });

        addSection("Display");
        addRow("Display Settings", "Choose note names, in-tune sensitivity and screen orientation.", new Runnable() {
            @Override
            public void run() {
                editDisplay();
            }
        });

        addSection("Input");
        addRow("Sample Rate", "Choose the microphone sample rate. Higher rates improve high-frequency accuracy; lower rates use less power.", new Runnable() {
            @Override
            public void run() {
                chooseSampleRate();
            }
        });
        addRow("Analysis Rate", "Choose how often pitch is calculated. Higher rates react faster; lower rates save battery.", new Runnable() {
            @Override
            public void run() {
                chooseAnalysisRate();
            }
        });

        addSection("Advanced DSP");
        addRow("Noise Gate", "Suppress background hiss and room rumble. Default: -62 dB.", new Runnable() {
            @Override
            public void run() {
                editNoiseGate();
            }
        });
        addRow("Clarity Threshold", "Minimum pitch confidence. Lower values find weaker notes; higher values reject noise. Default: 36%.", new Runnable() {
            @Override
            public void run() {
                editClarity();
            }
        });
        addRow("Harmonic Search", "How many harmonics Instrument mode checks when folding an upper harmonic back to a string. Default: 8.", new Runnable() {
            @Override
            public void run() {
                editHarmonics();
            }
        });
        addRow("Pitch Smoothing", "Higher values steady the needle and strobe; lower values react faster. Default: 72%.", new Runnable() {
            @Override
            public void run() {
                editPitchSmoothing();
            }
        });
        addRow("Spectrogram Contrast", "Gamma of the scrolling pitch display. Lower values brighten weak partials. Default: 0.72.", new Runnable() {
            @Override
            public void run() {
                editSpectroGamma();
            }
        });
        addRow("Spectrogram Speed", "Scroll speed in columns per analysis frame. Default: 1x.", new Runnable() {
            @Override
            public void run() {
                editSpectroScroll();
            }
        });
        addRow("Chord Gate", "Minimum signal level for chord analysis. Default: -56 dB.", new Runnable() {
            @Override
            public void run() {
                editChordGate();
            }
        });
        addRow("Chord Smoothing", "How long chord changes are held before the grid updates. Default: 72%.", new Runnable() {
            @Override
            public void run() {
                editChordSmoothing();
            }
        });

        addSection("Support");
        addRow("Help & Information", "Read the quick-start guide, privacy notes and license summary.", new Runnable() {
            @Override
            public void run() {
                showHelp();
            }
        });
        addRow("Factory Reset", "Restore the default settings. Custom instruments and temperaments are kept.", new Runnable() {
            @Override
            public void run() {
                confirmReset();
            }
        });
    }

    private void addSection(String title) {
        TextView tv = new TextView(this);
        tv.setText(title.toUpperCase(Locale.US));
        tv.setTextColor(0xff91a7ba);
        tv.setTextSize(10);
        tv.setLetterSpacing(0.14f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setPadding(dp(4), dp(18), dp(4), dp(8));
        content.addView(tv, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addRow(String title, String description, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(12), dp(14));

        GradientDrawable background = new GradientDrawable();
        background.setColor(0xff0d1926);
        background.setCornerRadius(dp(16));
        background.setStroke(dp(1), 0xff20384c);
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff), background, null));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(0xfff3f8fc);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        TextView desc = new TextView(this);
        desc.setText(description);
        desc.setTextColor(0xff91a7ba);
        desc.setTextSize(12);
        desc.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        desc.setPadding(0, dp(3), 0, 0);
        texts.addView(titleView);
        texts.addView(desc);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        TextView chevron = new TextView(this);
        chevron.setText("›");
        chevron.setTextColor(0xff536a7d);
        chevron.setTextSize(24);
        chevron.setTypeface(condensed);
        chevron.setGravity(Gravity.CENTER);
        chevron.setPadding(dp(8), 0, dp(2), 0);
        row.addView(chevron, new LinearLayout.LayoutParams(-2, -2));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(8));
        content.addView(row, lp);
    }

    private void chooseInstrument() {
        final List<Tuning> all = InstrumentLibrary.withCustom(settings.customTunings);
        String[] labels = new String[all.size()];
        int selected = 0;
        for (int i = 0; i < all.size(); i++) {
            Tuning t = all.get(i);
            labels[i] = t.displayName() + "   (" + t.noteText(settings.noteStyle) + ")";
            if (t.name.equals(settings.instrumentName)) {
                selected = i;
            }
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Preset Instrument");
        b.setSingleChoiceItems(labels, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                settings.instrumentName = all.get(which).name;
                settings.save(OptionsActivity.this);
                rebuildRows();
                dialog.dismiss();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void chooseTemperament() {
        final List<Temperament> all = TemperamentLibrary.withCustom(settings.customTemperaments);
        String[] labels = new String[all.size()];
        int selected = 0;
        for (int i = 0; i < all.size(); i++) {
            labels[i] = all.get(i).displayName();
            if (all.get(i).name.equals(settings.temperamentName)) {
                selected = i;
            }
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Preset Temperament");
        b.setSingleChoiceItems(labels, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                settings.temperamentName = all.get(which).name;
                settings.save(OptionsActivity.this);
                rebuildRows();
                dialog.dismiss();
            }
        });
        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private void editA4() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(10), dp(18), 0);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.US, "%.1f", settings.a4));
        input.setSelectAllOnFocus(true);
        box.addView(input);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(8), 0, 0);
        String[] labels = {"<<", "<", ">", ">>"};
        final float[] steps = {-1.0f, -0.1f, 0.1f, 1.0f};
        for (int i = 0; i < labels.length; i++) {
            Button button = new Button(this);
            button.setText(labels[i]);
            final float step = steps[i];
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        float value = Float.parseFloat(input.getText().toString()) + step;
                        value = Math.max(220.0f, Math.min(880.0f, Math.round(value * 10f) / 10f));
                        input.setText(String.format(Locale.US, "%.1f", value));
                        input.setSelection(input.getText().length());
                    } catch (NumberFormatException ignored) {
                    }
                }
            });
            buttons.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        }
        box.addView(buttons);

        new AlertDialog.Builder(this)
                .setTitle("A4 Frequency (hz)")
                .setView(box)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            float value = Float.parseFloat(input.getText().toString());
                            settings.a4 = Math.max(220.0f, Math.min(880.0f, value));
                        } catch (NumberFormatException ignored) {
                        }
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                    }
                })
                .setNeutralButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.a4 = 440.0f;
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editDisplay() {
        final int oldNoteStyle = settings.noteStyle;
        final float oldThreshold = settings.inTuneThreshold;
        final int oldOrientation = settings.screenOrientation;
        final String[] noteStyles = {"English (sharp)", "English (flat)"};
        final String[] thresholds = {"5 cents", "10 cents", "15 cents"};
        final float[] thresholdValues = {5f, 10f, 15f};
        final String[] orientations = {"dynamic", "landscape", "portrait"};

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);
        final TextView noteValue = addChoiceRow(box, "Note names", noteStyles[settings.noteStyle], new Runnable() {
            @Override
            public void run() {
            }
        });
        noteValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(OptionsActivity.this)
                        .setTitle("Note names")
                        .setSingleChoiceItems(noteStyles, settings.noteStyle, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settings.noteStyle = which;
                                noteValue.setText(noteStyles[which]);
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        int thresholdIndex = 0;
        for (int i = 0; i < thresholdValues.length; i++) {
            if (Math.abs(settings.inTuneThreshold - thresholdValues[i]) < 0.1f) {
                thresholdIndex = i;
            }
        }
        final TextView thresholdValue = addChoiceRow(box, "In-tune threshold", thresholds[thresholdIndex], null);
        thresholdValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected = 0;
                for (int i = 0; i < thresholdValues.length; i++) {
                    if (Math.abs(settings.inTuneThreshold - thresholdValues[i]) < 0.1f) {
                        selected = i;
                    }
                }
                new AlertDialog.Builder(OptionsActivity.this)
                        .setTitle("In-tune threshold")
                        .setSingleChoiceItems(thresholds, selected, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settings.inTuneThreshold = thresholdValues[which];
                                thresholdValue.setText(thresholds[which]);
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        final TextView orientationValue = addChoiceRow(box, "Screen orientation",
                orientations[Math.max(0, Math.min(2, settings.screenOrientation))], null);
        orientationValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(OptionsActivity.this)
                        .setTitle("Screen orientation")
                        .setSingleChoiceItems(orientations, settings.screenOrientation, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settings.screenOrientation = which;
                                orientationValue.setText(orientations[which]);
                                dialog.dismiss();
                            }
                        }).show();
            }
        });

        final AlertDialog[] dialogHolder = new AlertDialog[1];
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Display Options")
                .setView(box)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        settings.noteStyle = oldNoteStyle;
                        settings.inTuneThreshold = oldThreshold;
                        settings.screenOrientation = oldOrientation;
                        rebuildRows();
                    }
                })
                .create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                settings.noteStyle = oldNoteStyle;
                settings.inTuneThreshold = oldThreshold;
                settings.screenOrientation = oldOrientation;
            }
        });
        dialogHolder[0] = dialog;
        dialog.show();
    }

    private TextView addChoiceRow(LinearLayout parent, String label, String value, Runnable unused) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(0xffe6e6e6);
        title.setTextSize(15);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(0xff67c9ff);
        valueView.setTextSize(15);
        valueView.setPadding(dp(10), dp(4), dp(10), dp(4));
        valueView.setBackgroundColor(0xff152a3b);
        row.addView(valueView, new LinearLayout.LayoutParams(-2, -2));
        parent.addView(row);
        return valueView;
    }

    private interface FloatSetter {
        void set(float value);
    }

    private void editNoiseGate() {
        float db = (float) (20.0 * Math.log10(Math.max(0.00005f, settings.noiseGateRms)));
        showSliderDialog("Noise Gate", "Signals below this RMS are ignored. Raise it in a noisy room; lower it for quiet instruments.",
                -80f, -30f, Math.max(-80f, Math.min(-30f, db)), "%.0f dB", false, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.noiseGateRms = (float) Math.pow(10.0, value / 20.0);
                    }
                });
    }

    private void editClarity() {
        showSliderDialog("Clarity Threshold", "Minimum confidence for a reported pitch. Lower values find weaker notes; higher values reject noise.",
                0.05f, 0.95f, settings.minClarity, "%.0f%%", true, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.minClarity = value;
                    }
                });
    }

    private void editHarmonics() {
        showSliderDialog("Harmonic Search", "Instrument mode checks the fundamental and this many harmonics.",
                1f, 8f, settings.harmonicDepth, "%.0f", false, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.harmonicDepth = Math.round(value);
                    }
                });
    }

    private void editPitchSmoothing() {
        showSliderDialog("Pitch Smoothing", "Higher values steady the needle and strobe; lower values react faster.",
                0f, 0.95f, settings.pitchSmoothing, "%.0f%%", true, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.pitchSmoothing = value;
                    }
                });
    }

    private void editSpectroGamma() {
        showSliderDialog("Spectrogram Contrast", "Gamma of the scrolling pitch display. Lower values brighten weak partials.",
                0.30f, 1.20f, settings.spectroGamma, "%.2f", false, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.spectroGamma = value;
                    }
                });
    }

    private void editSpectroScroll() {
        showSliderDialog("Spectrogram Speed", "Columns advanced per analysis frame.",
                1f, 4f, settings.spectroScroll, "%.0fx", false, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.spectroScroll = Math.round(value);
                    }
                });
    }

    private void editChordGate() {
        float db = (float) (20.0 * Math.log10(Math.max(0.0002f, settings.chordGateRms)));
        showSliderDialog("Chord Gate", "Minimum signal level for chord analysis. Lower it for quiet fingerpicking.",
                -70f, -40f, Math.max(-70f, Math.min(-40f, db)), "%.0f dB", false, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.chordGateRms = (float) Math.pow(10.0, value / 20.0);
                    }
                });
    }

    private void editChordSmoothing() {
        showSliderDialog("Chord Smoothing", "How long chord changes are held before the grid updates.",
                0f, 0.95f, settings.chordSmoothing, "%.0f%%", true, new FloatSetter() {
                    @Override
                    public void set(float value) {
                        settings.chordSmoothing = value;
                    }
                });
    }

    private void showSliderDialog(String title, String description, final float min, final float max,
                                  float value, final String format, final boolean percent,
                                  final FloatSetter setter) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), 0);

        TextView descriptionView = new TextView(this);
        descriptionView.setText(description);
        descriptionView.setTextColor(0xff9a9a9a);
        descriptionView.setTextSize(13);
        descriptionView.setTypeface(condensed);
        box.addView(descriptionView);

        final TextView valueView = new TextView(this);
        valueView.setTextColor(0xff67c9ff);
        valueView.setTextSize(22);
        valueView.setTypeface(condensed);
        valueView.setPadding(0, dp(10), 0, 0);
        box.addView(valueView);

        final SeekBar bar = new SeekBar(this);
        bar.setMax(1000);
        final float initial = Math.max(min, Math.min(max, value));
        final float[] current = {initial};
        bar.setContentDescription(title + ": " + formatValue(initial, format, percent));
        updateSliderLabel(valueView, current[0], format, percent);
        bar.setProgress(Math.round((current[0] - min) / (max - min) * 1000f));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                current[0] = min + (max - min) * progress / 1000f;
                updateSliderLabel(valueView, current[0], format, percent);
                seekBar.setContentDescription(title + ": " + formatValue(current[0], format, percent));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        TextView range = new TextView(this);
        range.setText(rangeLabel(min, format, percent) + "  —  " + rangeLabel(max, format, percent));
        range.setTextColor(0xff777777);
        range.setTextSize(11);
        range.setTypeface(condensed);
        box.addView(range);

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Math.abs(current[0] - initial) > 1e-6f) {
                            setter.set(current[0]);
                        }
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSliderLabel(TextView view, float value, String format, boolean percent) {
        view.setText(formatValue(value, format, percent));
    }

    private String formatValue(float value, String format, boolean percent) {
        return String.format(Locale.US, format, percent ? value * 100f : value);
    }

    private String rangeLabel(float value, String format, boolean percent) {
        return formatValue(value, format, percent);
    }

    private void chooseSampleRate() {
        final int[] rates = {44100, 22050, 11025, 8000};
        String[] labels = {"44.1 kHz", "22.05 kHz", "11.025 kHz", "8 kHz"};
        int selected = 0;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] == settings.sampleRate) {
                selected = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Sample Rate")
                .setSingleChoiceItems(labels, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.sampleRate = rates[which];
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void chooseAnalysisRate() {
        final String[] labels = {"Low", "Medium", "High"};
        final int[] values = {-1, 0, 1};
        int selected = settings.analysisRate + 1;
        new AlertDialog.Builder(this)
                .setTitle("Analysis Rate")
                .setSingleChoiceItems(labels, selected, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.analysisRate = values[which];
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHelp() {
        String text = "QUICK START\n\n"
                + "Swipe left or right to change view. Long-press the display or tap the three dots to open the view menu and settings.\n\n"
                + "Instrument: a radial strobe with string indicators for the selected tuning.\n\n"
                + "Dial & Strobe: a cent scale with a moving needle and linear strobe. Flat moves left; sharp moves right.\n\n"
                + "Spectrogram: a 5-cent-resolution scrolling pitch display. Useful for voice, guitar and any sustained instrument.\n\n"
                + "Chords: estimates major, major 7th, dominant 7th, minor, minor 7th and diminished chords.\n\n"
                + "Big Strobe: a large, single-strobe display for small screens.\n\n"
                + "ADVANCED DSP\n\n"
                + "Noise gate, clarity threshold, harmonic search depth, pitch smoothing, spectrogram contrast and speed, chord gate and chord smoothing. Defaults preserve the original HzLab behaviour.\n\n"
                + "TUNING\n\n"
                + "A4 reference, historical temperaments and custom temperaments apply to every display. Custom instruments support up to 12 strings or notes.\n\n"
                + "PRIVACY\n\n"
                + "HzLab uses the microphone only for live pitch analysis. Audio is processed on the device and is never recorded, uploaded or shared. The app has no ads, analytics or network permissions.\n\n"
                + "ABOUT\n\n"
                + "HzLab is an independent, open-source tuner for modern 64-bit Android devices.";
        new AlertDialog.Builder(this)
                .setTitle("Help & Information")
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Factory Reset?")
                .setMessage("Reset all settings to factory defaults? Custom instruments and temperaments will NOT be deleted.")
                .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        settings.resetKeepCustom();
                        settings.save(OptionsActivity.this);
                        rebuildRows();
                        Toast.makeText(OptionsActivity.this, "Settings reset", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onPause() {
        settings.save(this);
        super.onPause();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        settings.save(this);
        super.onBackPressed();
    }
}
