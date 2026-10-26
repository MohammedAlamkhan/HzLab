// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent settings for HzLab.
 */
public final class Settings {
    public static final String PREFS = "io.github.mohammedalamkhan.hzlab.settings";
    public static final int DEFAULT_SAMPLE_RATE = 44100;

    private static Settings instance;

    public volatile int viewMode = 0;
    public int sampleRate = DEFAULT_SAMPLE_RATE;
    public int analysisRate = 0;          // -1 low, 0 medium, 1 high
    public volatile float a4 = 440.0f;
    public volatile int noteStyle = 0;             // 0 sharps, 1 flats
    public volatile float inTuneThreshold = 5.0f;
    public int screenOrientation = 0;     // 0 dynamic, 1 landscape, 2 portrait
    public int viewedMask = 0;
    public int spectrogramTexture = 5;
    public String audioSetupVersion = "";
    public String audioAnalysisVersion = "";
    public String instrumentName = "";
    public String temperamentName = "";
    public boolean welcomeShown = false;
    public int customInstrumentFocus = -1;
    public int customTemperamentFocus = -1;
    public List<Tuning> customTunings = new ArrayList<Tuning>();
    public List<Temperament> customTemperaments = new ArrayList<Temperament>();

    private Settings() {
    }

    public static synchronized Settings get(Context context) {
        if (instance == null) {
            instance = new Settings();
            instance.load(context.getApplicationContext());
        }
        return instance;
    }

    public static synchronized Settings peek() {
        return instance;
    }

    private void load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        viewMode = p.getInt("viewMode", 0);
        sampleRate = p.getInt("sampleRate", DEFAULT_SAMPLE_RATE);
        analysisRate = p.getInt("analysisRate", 0);
        a4 = p.getFloat("A4ReferenceHz", 440.0f);
        noteStyle = p.getInt("noteNameStyle", 0);
        inTuneThreshold = p.getFloat("inTuneThreshold", 5.0f);
        screenOrientation = p.getInt("screenOrientation", 0);
        viewedMask = p.getInt("usageMask", 0);
        spectrogramTexture = p.getInt("texTypeSpectrogram", 5);
        audioSetupVersion = p.getString("audioSetupVersion", "");
        audioAnalysisVersion = p.getString("audioAnalysisVersion", "");
        instrumentName = p.getString("instrumentName", "");
        temperamentName = p.getString("temperamentName", "");
        welcomeShown = p.getBoolean("welcomeShown_1_0_23", p.getBoolean("welcomeShown_1_0_22", false));
        customInstrumentFocus = p.getInt("customInstrumentFocus", -1);
        customTemperamentFocus = p.getInt("customTemperamentFocus", -1);

        customTunings = new ArrayList<Tuning>();
        int instrumentCount = p.getInt("customInstrumentCount", 0);
        for (int i = 0; i < instrumentCount; i++) {
            String prefix = "customInstrumentInstance" + i + "_";
            if (p.contains(prefix + "Name")) {
                try {
                    customTunings.add(new Tuning(p.getString(prefix + "Name", "<unknown>"),
                            p.getString(prefix + "Notes", "")));
                } catch (RuntimeException ignored) {
                }
            }
        }

        customTemperaments = new ArrayList<Temperament>();
        int temperamentCount = p.getInt("customTemperamentCount", 0);
        for (int i = 0; i < temperamentCount; i++) {
            String prefix = "customTemperamentInstance" + i + "_";
            if (p.contains(prefix + "Name")) {
                float[] cents = new float[12];
                for (int n = 0; n < 12; n++) {
                    cents[n] = p.getFloat(prefix + "Cents" + n, 0.0f);
                }
                customTemperaments.add(new Temperament(p.getString(prefix + "Name", "<unknown>"), cents));
            }
        }

        ensureValidSelection();
    }

    public void ensureValidSelection() {
        if (viewMode < 0 || viewMode >= 5) {
            viewMode = 0;
        }
        if (InstrumentLibrary.findIn(InstrumentLibrary.withCustom(customTunings), instrumentName) == null) {
            instrumentName = InstrumentLibrary.builtIns().get(0).name;
        }
        if (TemperamentLibrary.findIn(TemperamentLibrary.withCustom(customTemperaments), temperamentName) == null) {
            temperamentName = TemperamentLibrary.builtIns().get(0).name;
        }
        if (analysisRate < -1 || analysisRate > 1) {
            analysisRate = 0;
        }
        if (a4 < 220.0f) {
            a4 = 220.0f;
        } else if (a4 > 880.0f) {
            a4 = 880.0f;
        }
        if (inTuneThreshold < 1.0f) {
            inTuneThreshold = 5.0f;
        }
    }

    public Tuning selectedTuning() {
        return InstrumentLibrary.find(instrumentName, customTunings);
    }

    public Temperament selectedTemperament() {
        return TemperamentLibrary.find(temperamentName, customTemperaments);
    }

    public float[] selectedTemperamentCents() {
        return selectedTemperament().cents;
    }

    public int analysisIntervalMs() {
        switch (analysisRate) {
            case 1:
                return 12;
            case -1:
                return 40;
            default:
                return 22;
        }
    }

    public synchronized void save(Context context) {
        ensureValidSelection();
        SharedPreferences.Editor e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putInt("viewMode", viewMode);
        e.putInt("sampleRate", sampleRate);
        e.putInt("analysisRate", analysisRate);
        e.putFloat("A4ReferenceHz", a4);
        e.putInt("noteNameStyle", noteStyle);
        e.putFloat("inTuneThreshold", inTuneThreshold);
        e.putInt("screenOrientation", screenOrientation);
        e.putInt("usageMask", viewedMask);
        e.putInt("texTypeSpectrogram", spectrogramTexture);
        e.putString("audioSetupVersion", audioSetupVersion);
        e.putString("audioAnalysisVersion", audioAnalysisVersion);
        e.putString("instrumentName", instrumentName);
        e.putString("temperamentName", temperamentName);
        e.putInt("customInstrumentFocus", customInstrumentFocus);
        e.putInt("customTemperamentFocus", customTemperamentFocus);
        e.putBoolean("welcomeShown_1_0_23", welcomeShown);

        e.putInt("customInstrumentCount", customTunings.size());
        for (int i = 0; i < customTunings.size(); i++) {
            Tuning t = customTunings.get(i);
            String prefix = "customInstrumentInstance" + i + "_";
            e.putString(prefix + "Name", t.name);
            e.putString(prefix + "Notes", t.noteText(0));
        }

        e.putInt("customTemperamentCount", customTemperaments.size());
        for (int i = 0; i < customTemperaments.size(); i++) {
            Temperament t = customTemperaments.get(i);
            String prefix = "customTemperamentInstance" + i + "_";
            e.putString(prefix + "Name", t.name);
            for (int n = 0; n < 12; n++) {
                e.putFloat(prefix + "Cents" + n, t.cents[n]);
            }
        }
        e.apply();
    }

    public synchronized void resetKeepCustom() {
        viewMode = 0;
        sampleRate = DEFAULT_SAMPLE_RATE;
        analysisRate = 0;
        a4 = 440.0f;
        noteStyle = 0;
        inTuneThreshold = 5.0f;
        screenOrientation = 0;
        viewedMask = 0;
        spectrogramTexture = 5;
        audioSetupVersion = "";
        audioAnalysisVersion = "";
        instrumentName = InstrumentLibrary.builtIns().get(0).name;
        temperamentName = TemperamentLibrary.builtIns().get(0).name;
        welcomeShown = true;
        customInstrumentFocus = -1;
        customTemperamentFocus = -1;
    }
}
