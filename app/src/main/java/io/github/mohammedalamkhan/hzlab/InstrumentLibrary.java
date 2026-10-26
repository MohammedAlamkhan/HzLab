// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Common instrument tunings, expressed as MIDI note numbers. */
public final class InstrumentLibrary {
    private static List<Tuning> builtIns;

    private InstrumentLibrary() {
    }

    public static synchronized List<Tuning> builtIns() {
        if (builtIns == null) {
            ArrayList<Tuning> list = new ArrayList<Tuning>();
            add(list, "Guitar, Standard", "E2 A2 D3 G3 B3 E4");
            add(list, "Guitar, Drop D", "D2 A2 D3 G3 B3 E4");
            add(list, "Guitar, Open D", "D2 A2 D3 F#3 A3 D4");
            add(list, "Guitar, Open G", "D2 G2 D3 G3 B3 D4");
            add(list, "Guitar, Open A", "E2 A2 E3 A3 C#4 E4");
            add(list, "Guitar, Lute", "E2 A2 D3 F#3 B3 E4");
            add(list, "Guitar, Irish", "D2 A2 D3 G3 A3 D4");
            add(list, "Bass Guitar, (4 String), Standard", "E1 A1 D2 G2");
            add(list, "Bass Guitar, (5 String), Low B", "B0 E1 A1 D2 G2");
            add(list, "Bass Guitar, (5 String), Low E", "E1 A1 D2 G2 C3");
            add(list, "Bass Guitar, (6 String), Standard", "B0 E1 A1 D2 G2 C3");
            add(list, "Violin", "G3 D4 A4 E5");
            add(list, "Violin (Tenor)", "G2 D3 A3 E4");
            add(list, "Viola", "C3 G3 D4 A4");
            add(list, "Fiddle, Standard", "G3 D4 A4 E5");
            add(list, "Fiddle, Cajun", "F3 C4 G4 D5");
            add(list, "Fiddle, Open G", "G3 D4 G4 B4");
            add(list, "Fiddle, Sawmill", "G3 D4 G4 D5");
            add(list, "Fiddle, Gee-dad", "G3 D4 A4 D5");
            add(list, "Fiddle, Open D", "D3 D4 A4 D5");
            add(list, "Fiddle, High bass", "A3 D4 A4 E5");
            add(list, "Fiddle, Cross tuning", "A3 E4 A4 E5");
            add(list, "Fiddle, Calico", "A3 E4 A4 C#5");
            add(list, "Mandolin", "G3 G3 D4 D4 A4 A4 E5 E5");
            add(list, "Mandolin (Octave)", "G2 G2 D3 D3 A3 A3 E4 E4");
            add(list, "Ukulele (Concert), Standard", "G4 C4 E4 A4");
            add(list, "Ukulele (Concert), D6/Soprano", "A4 D4 F#4 B4");
            add(list, "Ukulele (Tenor)", "G3 C4 E4 A4");
            add(list, "Ukulele (Baritone)", "D3 G3 B3 E4");
            add(list, "Ukulele (Bass)", "E1 A1 D2 G2");
            add(list, "Banjo (5 string), Standard/Open G", "G4 D3 G3 B3 D4");
            add(list, "Banjo (5 string), C tuning", "G4 C3 G3 B3 D4");
            add(list, "Banjo (5 string), Double C", "G4 C3 G3 C4 D4");
            add(list, "Banjo (5 string), Sawmill", "G4 D3 G3 C4 D4");
            add(list, "Banjo (5 string), Open D", "F#4 D3 F#3 A3 D4");
            add(list, "Banjo (5 string), Guitar", "G4 D3 G3 B3 E4");
            add(list, "Banjo (5 string), Willie Moore", "G4 D3 G3 A3 D4");
            add(list, "Banjo (5 string), Dock Boggs (Low C)", "F#4 C3 G3 A3 D4");
            add(list, "Banjo (5 string), Dock Boggs (Low D)", "F#4 D3 G3 A3 D4");
            add(list, "Banjo (5 string), Cumberland Gap", "G4 E3 A3 D4 E4");
            add(list, "Banjo (5 string), G Minor", "G4 D3 G3 A#3 D4");
            add(list, "Banjo (5 string), Open C", "G4 C3 G3 C4 E4");
            add(list, "Banjo (Plectrum)", "C3 G3 B3 D4");
            add(list, "Banjo (Tenor), Standard", "C3 G3 D4 A4");
            add(list, "Banjo (Tenor), Irish", "G2 D3 A3 E4");
            add(list, "Banjo (Long neck)", "G4 B2 E3 G#3 B3");
            add(list, "Bouzouki (8 string)", "C3 C4 F3 F4 A3 A3 D4 D4");
            add(list, "Bouzouki (6 string)", "D3 D4 A3 A3 D4 D4");
            add(list, "Pedal Steel, Standard/E9th", "B2 D3 E3 F#3 G#3 B3 E4 G#4 D#4 F#4");
            add(list, "Pedal Steel, C6th", "C2 F2 A2 C3 E3 G3 A3 C4 E4 D4");
            add(list, "Pedal Steel, A7th", "A1 E2 G2 A2 C#3 E3 G3 A3 C#4 E4");
            add(list, "Pedal Steel, C Diatonic", "G2 A2 C3 E3 F3 G3 A3 B3 C4 E4");
            builtIns = Collections.unmodifiableList(list);
        }
        return builtIns;
    }

    private static void add(List<Tuning> list, String name, String notes) {
        list.add(new Tuning(name, notes));
    }

    public static Tuning find(String name, List<Tuning> custom) {
        if (name != null) {
            for (Tuning t : builtIns()) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
            if (custom != null) {
                for (Tuning t : custom) {
                    if (t.name.equals(name)) {
                        return t;
                    }
                }
            }
        }
        return builtIns().get(0);
    }

    public static Tuning findIn(List<Tuning> list, String name) {
        if (list != null) {
            for (Tuning t : list) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
        }
        return null;
    }

    public static List<Tuning> withCustom(List<Tuning> custom) {
        ArrayList<Tuning> list = new ArrayList<Tuning>(builtIns());
        if (custom != null) {
            list.addAll(custom);
        }
        return list;
    }

    public static List<Tuning> sorted(List<Tuning> list) {
        ArrayList<Tuning> result = new ArrayList<Tuning>(list);
        Collections.sort(result, new Comparator<Tuning>() {
            @Override
            public int compare(Tuning a, Tuning b) {
                return a.name.compareTo(b.name);
            }
        });
        return result;
    }

    public static void upsert(List<Tuning> list, Tuning tuning) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(tuning.name)) {
                list.set(i, tuning);
                return;
            }
        }
        list.add(tuning);
    }

    public static List<Tuning> copy(List<Tuning> list) {
        return list == null ? new ArrayList<Tuning>() : new ArrayList<Tuning>(list);
    }
}
