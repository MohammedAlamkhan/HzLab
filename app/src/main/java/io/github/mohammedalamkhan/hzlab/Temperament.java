// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import java.util.Arrays;

/** A named 12-note temperament, with a cents offset for each pitch class. */
public final class Temperament {
    public final String name;
    public final float[] cents;

    public Temperament(String name, float[] cents) {
        this.name = name;
        this.cents = cents.clone();
    }

    public Temperament(String name, double... cents) {
        this.name = name;
        float[] values = new float[cents.length];
        for (int i = 0; i < cents.length; i++) {
            values[i] = (float) cents[i];
        }
        this.cents = values;
    }

    public boolean isCustom() {
        return name != null && name.length() > 0 && name.charAt(0) == '!';
    }

    public String displayName() {
        return isCustom() ? name.substring(1) : name;
    }

    @Override
    public String toString() {
        return displayName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Temperament)) {
            return false;
        }
        Temperament other = (Temperament) o;
        return name.equals(other.name) && Arrays.equals(cents, other.cents);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + Arrays.hashCode(cents);
    }
}
