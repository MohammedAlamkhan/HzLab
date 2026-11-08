// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/** Continuous reference tone using the selected A4 and temperament. */
public final class ToneActivity extends Activity {
    private Settings settings;
    private ToneEngine engine;
    private TextView noteValue;
    private TextView freqValue;
    private TextView octaveValue;
    private TextView playButton;
    private GridLayout noteGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int theme = Settings.readTheme(this);
        setTheme(theme == Theme.DAYLIGHT ? R.style.Theme_HzLab_Light : R.style.Theme_HzLab);
        super.onCreate(savedInstanceState);
        settings = Settings.get(this);
        Theme.apply(settings.themeMode);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Theme.bgTop);
        getWindow().setNavigationBarColor(Theme.bgBottom);
        buildUi();
        engine = new ToneEngine();
        engine.setFrequency(frequency());
        engine.setVolume(settings.toneVolume);
        updateToneUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.bgTop);
        root.addView(header(), new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(28));
        scroll.addView(content, new LinearLayout.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout tone = card();
        tone.addView(sectionLabel("REFERENCE TONE"));
        noteValue = new TextView(this);
        noteValue.setTextColor(Theme.text);
        noteValue.setTextSize(58);
        noteValue.setGravity(Gravity.CENTER);
        noteValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tone.addView(noteValue);
        freqValue = new TextView(this);
        freqValue.setTextColor(Theme.dim);
        freqValue.setTextSize(20);
        freqValue.setGravity(Gravity.CENTER);
        tone.addView(freqValue);

        playButton = pill("PLAY", true);
        playButton.setTextSize(16);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggle(); }
        });
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(-1, dp(52));
        playLp.topMargin = dp(12);
        tone.addView(playButton, playLp);
        content.addView(tone, cardParams());

        LinearLayout notes = card();
        LinearLayout noteHeader = new LinearLayout(this);
        noteHeader.setOrientation(LinearLayout.HORIZONTAL);
        noteHeader.setGravity(Gravity.CENTER_VERTICAL);
        noteHeader.addView(sectionLabel("NOTE"), new LinearLayout.LayoutParams(0, -2, 1));
        noteHeader.addView(roundButton("-", new View.OnClickListener() {
            @Override public void onClick(View v) { setMidi(settings.toneMidi - 12); }
        }), new LinearLayout.LayoutParams(dp(40), dp(40)));
        octaveValue = new TextView(this);
        octaveValue.setTextColor(Theme.text);
        octaveValue.setTextSize(14);
        octaveValue.setGravity(Gravity.CENTER);
        noteHeader.addView(octaveValue, new LinearLayout.LayoutParams(dp(58), -1));
        noteHeader.addView(roundButton("+", new View.OnClickListener() {
            @Override public void onClick(View v) { setMidi(settings.toneMidi + 12); }
        }), new LinearLayout.LayoutParams(dp(40), dp(40)));
        notes.addView(noteHeader);

        noteGrid = new GridLayout(this);
        noteGrid.setColumnCount(4);
        noteGrid.setPadding(0, dp(10), 0, 0);
        notes.addView(noteGrid, new LinearLayout.LayoutParams(-1, -2));
        content.addView(notes, cardParams());

        LinearLayout volumeCard = card();
        volumeCard.addView(sectionLabel("VOLUME"));
        SeekBar volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(Math.round(settings.toneVolume * 100));
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                settings.toneVolume = progress / 100f;
                engine.setVolume(settings.toneVolume);
                settings.save(ToneActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        volumeCard.addView(volume);
        content.addView(volumeCard, cardParams());

        TextView hint = new TextView(this);
        hint.setText("Use headphones if you are also using the tuner. The tone follows your selected A4 reference and temperament.");
        hint.setTextColor(Theme.dim);
        hint.setTextSize(12);
        hint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        hint.setPadding(dp(4), dp(4), dp(4), 0);
        content.addView(hint);

        setContentView(root);
        buildNoteGrid();
    }

    private View header() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setBackgroundColor(Theme.surface);

        TextView back = new TextView(this);
        back.setText("<");
        back.setTextColor(Theme.text);
        back.setTextSize(24);
        back.setGravity(Gravity.CENTER);
        GradientDrawable backBg = new GradientDrawable();
        backBg.setShape(GradientDrawable.OVAL);
        backBg.setColor(Theme.raised);
        backBg.setStroke(dp(1), Theme.border);
        back.setBackground(backBg);
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("Reference tone");
        title.setTextColor(Theme.text);
        title.setTextSize(22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        TextView subtitle = new TextView(this);
        subtitle.setText("Practice tools");
        subtitle.setTextColor(Theme.dim);
        subtitle.setTextSize(11);
        titles.addView(title);
        titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        View line = new View(this);
        line.setBackgroundColor(Theme.border);
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.addView(header, new LinearLayout.LayoutParams(-1, -2));
        outer.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        return outer;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.surface);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), Theme.border);
        card.setBackground(bg);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10);
        return lp;
    }

    private TextView sectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(Theme.dim);
        label.setTextSize(10);
        label.setLetterSpacing(0.14f);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setPadding(0, 0, 0, dp(6));
        return label;
    }

    private TextView roundButton(String text, View.OnClickListener listener) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Theme.text);
        button.setTextSize(22);
        button.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Theme.raised);
        bg.setStroke(dp(1), Theme.border);
        button.setBackground(bg);
        button.setOnClickListener(listener);
        return button;
    }

    private TextView pill(String text, boolean filled) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        if (filled) {
            bg.setColor(Theme.accent);
            button.setTextColor(Theme.onAccent);
        } else {
            bg.setColor(Theme.raised);
            bg.setStroke(dp(1), Theme.border);
            button.setTextColor(Theme.accentBright);
        }
        button.setBackground(bg);
        return button;
    }

    private void buildNoteGrid() {
        noteGrid.removeAllViews();
        int octave = settings.toneMidi / 12 - 1;
        int selectedPc = ((settings.toneMidi % 12) + 12) % 12;
        for (int pc = 0; pc < 12; pc++) {
            final int notePc = pc;
            TextView button = new TextView(this);
            button.setText(Notes.pitchClass(pc, settings.noteStyle));
            button.setGravity(Gravity.CENTER);
            button.setTextSize(15);
            button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(pc == selectedPc ? Theme.accent : Theme.raised);
            bg.setStroke(dp(1), pc == selectedPc ? Theme.accent : Theme.border);
            button.setBackground(bg);
            button.setTextColor(pc == selectedPc ? Theme.onAccent : Theme.text);
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    setMidi((settings.toneMidi / 12) * 12 + notePc);
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(pc / 4, 1, 1f), GridLayout.spec(pc % 4, 1, 1f));
            lp.width = 0;
            lp.height = dp(48);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            noteGrid.addView(button, lp);
        }
        octaveValue.setText("Octave " + octave);
    }

    private void setMidi(int value) {
        value = Math.max(24, Math.min(108, value));
        settings.toneMidi = value;
        settings.save(this);
        engine.setFrequency(frequency());
        updateToneUi();
        buildNoteGrid();
    }

    private double frequency() {
        return Notes.targetHz(settings.toneMidi, settings.a4,
                settings.selectedTemperamentCents());
    }

    private void updateToneUi() {
        noteValue.setText(Notes.name(settings.toneMidi, settings.noteStyle));
        freqValue.setText(Notes.formatHz(frequency()));
        updatePlayButton();
    }

    private void toggle() {
        if (engine.isRunning()) {
            engine.stop();
        } else {
            engine.setFrequency(frequency());
            engine.start();
            if (!engine.isRunning()) {
                android.widget.Toast.makeText(this, "Audio output is unavailable. Check the phone volume.",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        updatePlayButton();
    }

    private void updatePlayButton() {
        if (playButton == null) return;
        boolean running = engine != null && engine.isRunning();
        playButton.setText(running ? "STOP" : "PLAY");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(running ? Theme.raised : Theme.accent);
        bg.setStroke(dp(1), running ? Theme.border : Theme.accent);
        playButton.setBackground(bg);
        playButton.setTextColor(running ? Theme.accentBright : Theme.onAccent);
    }

    @Override
    protected void onPause() {
        if (engine != null) engine.stop();
        settings.save(this);
        updatePlayButton();
        super.onPause();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
