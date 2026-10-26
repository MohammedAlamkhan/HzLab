// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MIDI and note-name helpers. C0 = MIDI 0 and A4 = MIDI 69.
 * The displayed octave follows scientific pitch notation.
 */
public final class Notes {
    public static final String[][] NAMES = {
            {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"},
            {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"}
    };

    private static final Map<String, Integer> SEMITONES = new HashMap<String, Integer>();

    static {
        for (int style = 0; style < NAMES.length; style++) {
            for (int i = 0; i < 12; i++) {
                SEMITONES.put(NAMES[style][i], i);
            }
        }
    }

    private Notes() {
    }

    public static int parse(String note) {
        if (note == null) {
            throw new IllegalArgumentException("null note");
        }
        String s = note.trim();
        int split = 0;
        while (split < s.length() && (Character.isLetter(s.charAt(split)) || s.charAt(split) == '#' || s.charAt(split) == 'b')) {
            split++;
        }
        String pitch = s.substring(0, split);
        Integer semitone = SEMITONES.get(pitch);
        if (semitone == null) {
            throw new IllegalArgumentException("Bad note: " + note);
        }
        int octave = Integer.parseInt(s.substring(split));
        return semitone + (octave + 1) * 12;
    }

    public static String name(int midi, int style) {
        int pc = ((midi % 12) + 12) % 12;
        int octave = Math.floorDiv(midi, 12) - 1;
        return NAMES[style == 1 ? 1 : 0][pc] + octave;
    }

    public static String nameWithOctave(int midi, int style) {
        return name(midi, style);
    }

    public static String pitchClass(int midi, int style) {
        int pc = ((midi % 12) + 12) % 12;
        return NAMES[style == 1 ? 1 : 0][pc];
    }

    public static int pitchClass(int midi) {
        return ((midi % 12) + 12) % 12;
    }

    public static double midiToHz(double midi, double a4) {
        return a4 * Math.pow(2.0, (midi - 69.0) / 12.0);
    }

    public static double hzToMidi(double hz, double a4) {
        return 69.0 + 12.0 * Math.log(hz / a4) / Math.log(2.0);
    }

    /** Frequency of a note in an arbitrary temperament (cents offset per pitch class). */
    public static double targetHz(int midi, double a4, float[] temperamentCents) {
        double cents = 0.0;
        if (temperamentCents != null && temperamentCents.length == 12) {
            cents = temperamentCents[pitchClass(midi)];
        }
        return midiToHz(midi, a4) * Math.pow(2.0, cents / 1200.0);
    }

    public static String formatHz(double hz) {
        if (hz <= 0) {
            return "--.- Hz";
        }
        return String.format(Locale.US, "%.1f Hz", hz);
    }

    public static String formatCents(double cents) {
        return String.format(Locale.US, "%+.1f", cents);
    }

    public static String[] parseNotes(String notes) {
        String[] parts = notes.trim().split("\\s+");
        int count = 0;
        for (String p : parts) {
            if (p.length() > 0) {
                count++;
            }
        }
        String[] result = new String[count];
        int n = 0;
        for (String p : parts) {
            if (p.length() > 0) {
                result[n++] = p;
            }
        }
        return result;
    }

    public static String joinNotes(int[] midi, int style) {
        StringBuilder sb = new StringBuilder();
        for (int value : midi) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(name(value, style));
        }
        return sb.toString();
    }
}
