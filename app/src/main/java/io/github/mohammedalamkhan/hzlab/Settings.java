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
    // Advanced DSP parameters. Defaults are the values that were hardcoded
    // before these controls existed, so existing behaviour is unchanged.
    public volatile float noiseGateRms = 0.0008f;
    public volatile float minClarity = 0.36f;
    public volatile int harmonicDepth = 8;
    public volatile float pitchSmoothing = 0.72f;
    public volatile float spectroGamma = 0.72f;
    public volatile int spectroScroll = 1;
    public volatile float chordGateRms = 0.0015f;
    public volatile float chordSmoothing = 0.72f;
    // Appearance and instrument transposition.
    public int themeMode = Theme.MIDNIGHT;
    public int capo = 0;
    // Practice tools.
    public int metronomeBpm = 100;
    public int metronomeBeats = 4;
    public int metronomeSubdivision = 1;
    public boolean metronomeAccent = true;
    public float metronomeVolume = 0.75f;
    public int toneMidi = 69;
    public float toneVolume = 0.55f;
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
        noiseGateRms = p.getFloat("noiseGateRms", 0.0008f);
        minClarity = p.getFloat("minClarity", 0.36f);
        harmonicDepth = p.getInt("harmonicDepth", 8);
        pitchSmoothing = p.getFloat("pitchSmoothing", 0.72f);
        spectroGamma = p.getFloat("spectroGamma", 0.72f);
        spectroScroll = p.getInt("spectroScroll", 1);
        chordGateRms = p.getFloat("chordGateRms", 0.0015f);
        chordSmoothing = p.getFloat("chordSmoothing", 0.72f);
        themeMode = p.getInt("themeMode", Theme.MIDNIGHT);
        capo = p.getInt("capo", 0);
        metronomeBpm = p.getInt("metronomeBpm", 100);
        metronomeBeats = p.getInt("metronomeBeats", 4);
        metronomeSubdivision = p.getInt("metronomeSubdivision", 1);
        metronomeAccent = p.getBoolean("metronomeAccent", true);
        metronomeVolume = p.getFloat("metronomeVolume", 0.75f);
        toneMidi = p.getInt("toneMidi", 69);
        toneVolume = p.getFloat("toneVolume", 0.55f);
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
        if (noiseGateRms < 0.00005f || noiseGateRms > 0.02f) {
            noiseGateRms = 0.0008f;
        }
        if (minClarity < 0.05f || minClarity > 0.95f) {
            minClarity = 0.36f;
        }
        if (harmonicDepth < 1 || harmonicDepth > 8) {
            harmonicDepth = 8;
        }
        if (pitchSmoothing < 0f || pitchSmoothing > 0.95f) {
            pitchSmoothing = 0.72f;
        }
        if (spectroGamma < 0.3f || spectroGamma > 1.5f) {
            spectroGamma = 0.72f;
        }
        if (spectroScroll < 1 || spectroScroll > 4) {
            spectroScroll = 1;
        }
        if (chordGateRms < 0.0002f || chordGateRms > 0.02f) {
            chordGateRms = 0.0015f;
        }
        if (chordSmoothing < 0f || chordSmoothing > 0.95f) {
            chordSmoothing = 0.72f;
        }
        if (themeMode < Theme.MIDNIGHT || themeMode > Theme.DAYLIGHT) {
            themeMode = Theme.MIDNIGHT;
        }
        if (capo < 0 || capo > 12) {
            capo = 0;
        }
        if (metronomeBpm < 40 || metronomeBpm > 240) {
            metronomeBpm = 100;
        }
        if (metronomeBeats < 2 || metronomeBeats > 12) {
            metronomeBeats = 4;
        }
        if (metronomeSubdivision != 2 && metronomeSubdivision != 4) {
            metronomeSubdivision = 1;
        }
        if (metronomeVolume < 0f || metronomeVolume > 1f) {
            metronomeVolume = 0.75f;
        }
        if (toneMidi < 12 || toneMidi > 120) {
            toneMidi = 69;
        }
        if (toneVolume < 0f || toneVolume > 1f) {
            toneVolume = 0.55f;
        }
    }

    /** Read the theme before an Activity calls super.onCreate(). */
    public static int readTheme(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("themeMode", Theme.MIDNIGHT);
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
        e.putFloat("noiseGateRms", noiseGateRms);
        e.putFloat("minClarity", minClarity);
        e.putInt("harmonicDepth", harmonicDepth);
        e.putFloat("pitchSmoothing", pitchSmoothing);
        e.putFloat("spectroGamma", spectroGamma);
        e.putInt("spectroScroll", spectroScroll);
        e.putFloat("chordGateRms", chordGateRms);
        e.putFloat("chordSmoothing", chordSmoothing);
        e.putInt("themeMode", themeMode);
        e.putInt("capo", capo);
        e.putInt("metronomeBpm", metronomeBpm);
        e.putInt("metronomeBeats", metronomeBeats);
        e.putInt("metronomeSubdivision", metronomeSubdivision);
        e.putBoolean("metronomeAccent", metronomeAccent);
        e.putFloat("metronomeVolume", metronomeVolume);
        e.putInt("toneMidi", toneMidi);
        e.putFloat("toneVolume", toneVolume);
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
        noiseGateRms = 0.0008f;
        minClarity = 0.36f;
        harmonicDepth = 8;
        pitchSmoothing = 0.72f;
        spectroGamma = 0.72f;
        spectroScroll = 1;
        chordGateRms = 0.0015f;
        chordSmoothing = 0.72f;
        themeMode = Theme.MIDNIGHT;
        capo = 0;
        metronomeBpm = 100;
        metronomeBeats = 4;
        metronomeSubdivision = 1;
        metronomeAccent = true;
        metronomeVolume = 0.75f;
        toneMidi = 69;
        toneVolume = 0.55f;
        audioSetupVersion = "";
        audioAnalysisVersion = "";
        instrumentName = InstrumentLibrary.builtIns().get(0).name;
        temperamentName = TemperamentLibrary.builtIns().get(0).name;
        welcomeShown = true;
        customInstrumentFocus = -1;
        customTemperamentFocus = -1;
    }
}
