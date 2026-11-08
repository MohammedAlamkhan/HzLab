// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Sample-clocked practice metronome with a modern card layout. */
public final class MetronomeActivity extends Activity implements MetronomeEngine.Listener {
    private Settings settings;
    private MetronomeEngine engine;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView bpmValue;
    private SeekBar bpmBar;
    private TextView startButton;
    private TextView beatsValue;
    private LinearLayout beatRow;
    private LinearLayout subdivisionRow;
    private final List<TextView> beatViews = new ArrayList<TextView>();
    private long tapLast;
    private long tapTotal;
    private int tapCount;

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
        engine = new MetronomeEngine(this);
        engine.setBpm(settings.metronomeBpm);
        engine.setBeats(settings.metronomeBeats);
        engine.setSubdivision(settings.metronomeSubdivision);
        engine.setAccent(settings.metronomeAccent);
        engine.setVolume(settings.metronomeVolume);
        rebuildBeatDots();
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

        LinearLayout tempo = card();
        tempo.addView(sectionLabel("TEMPO"));
        LinearLayout tempoRow = new LinearLayout(this);
        tempoRow.setOrientation(LinearLayout.HORIZONTAL);
        tempoRow.setGravity(Gravity.CENTER_VERTICAL);
        tempoRow.addView(roundButton("-", new View.OnClickListener() {
            @Override public void onClick(View v) { setBpm(settings.metronomeBpm - 1); }
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        bpmValue = new TextView(this);
        bpmValue.setTextColor(Theme.text);
        bpmValue.setTextSize(58);
        bpmValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        bpmValue.setGravity(Gravity.CENTER);
        tempoRow.addView(bpmValue, new LinearLayout.LayoutParams(0, -2, 1));
        tempoRow.addView(roundButton("+", new View.OnClickListener() {
            @Override public void onClick(View v) { setBpm(settings.metronomeBpm + 1); }
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        tempo.addView(tempoRow);

        bpmBar = new SeekBar(this);
        bpmBar.setMax(200);
        bpmBar.setProgress(settings.metronomeBpm - 40);
        bpmBar.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) setBpm(progress + 40);
            }
        });
        tempo.addView(bpmBar);
        TextView tap = pill("TAP TEMPO", false);
        tap.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tapTempo(); }
        });
        LinearLayout.LayoutParams tapLp = new LinearLayout.LayoutParams(-1, dp(44));
        tapLp.topMargin = dp(4);
        tempo.addView(tap, tapLp);
        content.addView(tempo, cardParams());

        LinearLayout beatsCard = card();
        beatsCard.addView(sectionLabel("TIME"));
        LinearLayout beatsHeader = new LinearLayout(this);
        beatsHeader.setOrientation(LinearLayout.HORIZONTAL);
        beatsHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView beatsLabel = new TextView(this);
        beatsLabel.setText("Beats per bar");
        beatsLabel.setTextColor(Theme.text);
        beatsLabel.setTextSize(15);
        beatsLabel.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        beatsHeader.addView(beatsLabel, new LinearLayout.LayoutParams(0, -2, 1));
        beatsHeader.addView(roundButton("-", new View.OnClickListener() {
            @Override public void onClick(View v) { setBeats(settings.metronomeBeats - 1); }
        }), new LinearLayout.LayoutParams(dp(40), dp(40)));
        beatsValue = new TextView(this);
        beatsValue.setTextColor(Theme.text);
        beatsValue.setTextSize(20);
        beatsValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        beatsValue.setGravity(Gravity.CENTER);
        beatsHeader.addView(beatsValue, new LinearLayout.LayoutParams(dp(34), -1));
        beatsHeader.addView(roundButton("+", new View.OnClickListener() {
            @Override public void onClick(View v) { setBeats(settings.metronomeBeats + 1); }
        }), new LinearLayout.LayoutParams(dp(40), dp(40)));
        beatsCard.addView(beatsHeader);

        beatRow = new LinearLayout(this);
        beatRow.setOrientation(LinearLayout.HORIZONTAL);
        beatRow.setGravity(Gravity.CENTER);
        beatRow.setPadding(0, dp(14), 0, dp(8));
        beatsCard.addView(beatRow, new LinearLayout.LayoutParams(-1, dp(44)));

        subdivisionRow = new LinearLayout(this);
        subdivisionRow.setOrientation(LinearLayout.HORIZONTAL);
        subdivisionRow.setGravity(Gravity.CENTER);
        beatsCard.addView(subdivisionRow);
        content.addView(beatsCard, cardParams());

        LinearLayout options = card();
        options.addView(sectionLabel("CLICK"));
        CheckBox accent = new CheckBox(this);
        accent.setText("Accent the first beat");
        accent.setTextColor(Theme.text);
        accent.setTextSize(14);
        accent.setChecked(settings.metronomeAccent);
        accent.setButtonTintList(android.content.res.ColorStateList.valueOf(Theme.accent));
        accent.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                settings.metronomeAccent = isChecked;
                engine.setAccent(isChecked);
                settings.save(MetronomeActivity.this);
            }
        });
        options.addView(accent);

        TextView volumeLabel = new TextView(this);
        volumeLabel.setText("Volume");
        volumeLabel.setTextColor(Theme.text);
        volumeLabel.setTextSize(14);
        volumeLabel.setPadding(0, dp(8), 0, 0);
        options.addView(volumeLabel);
        SeekBar volume = new SeekBar(this);
        volume.setMax(100);
        volume.setProgress(Math.round(settings.metronomeVolume * 100));
        volume.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                settings.metronomeVolume = progress / 100f;
                engine.setVolume(settings.metronomeVolume);
                settings.save(MetronomeActivity.this);
            }
        });
        options.addView(volume);
        content.addView(options, cardParams());

        startButton = pill("START", true);
        startButton.setTextSize(17);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggle(); }
        });
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(-1, dp(56));
        startLp.topMargin = dp(4);
        content.addView(startButton, startLp);

        TextView hint = new TextView(this);
        hint.setText("The metronome keeps running while this screen is open. It stops when you leave.");
        hint.setTextColor(Theme.dim);
        hint.setTextSize(12);
        hint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(8), dp(14), dp(8), 0);
        content.addView(hint);

        setContentView(root);
        updateBpmLabel();
        updateBeatsLabel();
        buildSubdivisionButtons();
        updateStartButton();
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
        title.setText("Metronome");
        title.setTextColor(Theme.text);
        title.setTextSize(22);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        TextView subtitle = new TextView(this);
        subtitle.setText("Practice tools");
        subtitle.setTextColor(Theme.dim);
        subtitle.setTextSize(11);
        subtitle.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
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

    private void setBpm(int value) {
        value = Math.max(40, Math.min(240, value));
        settings.metronomeBpm = value;
        bpmValue.setText(Integer.toString(value));
        if (bpmBar != null && bpmBar.getProgress() != value - 40) {
            bpmBar.setProgress(value - 40);
        }
        engine.setBpm(value);
        settings.save(this);
    }

    private void updateBpmLabel() {
        bpmValue.setText(Integer.toString(settings.metronomeBpm));
    }

    private void setBeats(int value) {
        value = Math.max(2, Math.min(12, value));
        settings.metronomeBeats = value;
        updateBeatsLabel();
        engine.setBeats(value);
        rebuildBeatDots();
        settings.save(this);
    }

    private void updateBeatsLabel() {
        beatsValue.setText(Integer.toString(settings.metronomeBeats));
    }

    private void buildSubdivisionButtons() {
        subdivisionRow.removeAllViews();
        final int[] values = {1, 2, 4};
        final String[] labels = {"1x", "2x", "4x"};
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            TextView button = pill(labels[i], settings.metronomeSubdivision == value);
            button.setTextSize(12);
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    settings.metronomeSubdivision = value;
                    engine.setSubdivision(value);
                    settings.save(MetronomeActivity.this);
                    buildSubdivisionButtons();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1);
            lp.setMargins(dp(3), dp(8), dp(3), 0);
            subdivisionRow.addView(button, lp);
        }
    }

    private void rebuildBeatDots() {
        beatRow.removeAllViews();
        beatViews.clear();
        for (int i = 0; i < settings.metronomeBeats; i++) {
            TextView dot = new TextView(this);
            dot.setText(Integer.toString(i + 1));
            dot.setTextSize(11);
            dot.setGravity(Gravity.CENTER);
            dot.setTextColor(Theme.dim);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Theme.raised);
            bg.setStroke(dp(1), Theme.border);
            dot.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
            lp.setMargins(dp(3), 0, dp(3), 0);
            beatRow.addView(dot, lp);
            beatViews.add(dot);
        }
    }

    private void highlightBeat(int beat) {
        for (int i = 0; i < beatViews.size(); i++) {
            TextView dot = beatViews.get(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(i == beat ? Theme.accent : Theme.raised);
            bg.setStroke(dp(1), i == beat ? Theme.accent : Theme.border);
            dot.setBackground(bg);
            dot.setTextColor(i == beat ? Theme.onAccent : Theme.dim);
        }
    }

    private void toggle() {
        if (engine.isRunning()) {
            engine.stop();
        } else {
            engine.start();
            if (!engine.isRunning()) {
                android.widget.Toast.makeText(this, "Audio output is unavailable. Check the phone volume.",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
        updateStartButton();
    }

    private void updateStartButton() {
        if (startButton == null) return;
        boolean running = engine != null && engine.isRunning();
        startButton.setText(running ? "STOP" : "START");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(running ? Theme.raised : Theme.accent);
        bg.setStroke(dp(1), running ? Theme.border : Theme.accent);
        startButton.setBackground(bg);
        startButton.setTextColor(running ? Theme.accentBright : Theme.onAccent);
    }

    private void tapTempo() {
        long now = SystemClock.elapsedRealtime();
        if (tapLast > 0 && now - tapLast < 2000) {
            tapTotal += now - tapLast;
            tapCount++;
            int bpm = (int) Math.round(60000.0 * tapCount / tapTotal);
            setBpm(bpm);
        } else {
            tapTotal = 0;
            tapCount = 0;
        }
        tapLast = now;
    }

    @Override
    public void onBeat(final int beat, final boolean accent) {
        main.post(new Runnable() {
            @Override public void run() {
                highlightBeat(beat);
            }
        });
    }

    @Override
    protected void onPause() {
        if (engine != null) engine.stop();
        settings.save(this);
        updateStartButton();
        super.onPause();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar seekBar) { }
        @Override public void onStopTrackingTouch(SeekBar seekBar) { }
    }
}
