// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import java.util.Arrays;

/** A named instrument tuning. Names beginning with '!' are user-created. */
public final class Tuning {
    public final String name;
    public final int[] notes;

    public Tuning(String name, String noteList) {
        this(name, Notes.parseNotes(noteList));
    }

    public Tuning(String name, String[] noteList) {
        this.name = name;
        int[] parsed = new int[noteList.length];
        for (int i = 0; i < noteList.length; i++) {
            parsed[i] = Notes.parse(noteList[i]);
        }
        this.notes = parsed;
    }

    public Tuning(String name, int[] notes) {
        this.name = name;
        this.notes = notes.clone();
    }

    public boolean isCustom() {
        return name != null && name.length() > 0 && name.charAt(0) == '!';
    }

    public String displayName() {
        return isCustom() ? name.substring(1) : name;
    }

    public String noteText(int style) {
        return Notes.joinNotes(notes, style);
    }

    @Override
    public String toString() {
        return displayName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tuning)) {
            return false;
        }
        Tuning other = (Tuning) o;
        return name.equals(other.name) && Arrays.equals(notes, other.notes);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + Arrays.hashCode(notes);
    }
}
