// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Historical and equal-temperament cent offsets. */
public final class TemperamentLibrary {
    private static List<Temperament> builtIns;

    private TemperamentLibrary() {
    }

    public static synchronized List<Temperament> builtIns() {
        if (builtIns == null) {
            ArrayList<Temperament> list = new ArrayList<Temperament>();
            add(list, "Equal Tempered (Perfect Octave)", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            add(list, "Equal Tempered (Perfect Fifth)", -2.5, -2.2, -2.0, -1.7, -1.4, -1.1, -0.8, -0.6, -0.3, 0, 0.3, 0.6);
            add(list, "Kirnberger I", 15.6, 5.9, 19.6, 9.8, 2.0, 13.7, 5.9, 17.6, 7.8, 0, 11.7, 3.9);
            add(list, "Kirnberger IIa", 4.9, -4.9, 8.8, -1.0, -8.8, 2.9, -4.9, 6.8, -2.9, 0, 1.0, -6.8);
            add(list, "Kirnberger IIb", 5.9, -3.9, 9.8, 0, -9.8, 3.9, -5.9, 7.8, -2.0, 0, 2.0, -7.8);
            add(list, "Kirnberger III", 10.3, 0.5, 3.4, 4.4, -3.4, 8.3, 0.5, 6.8, 2.4, 0, 6.4, -1.5);
            add(list, "Meantone", 8.8, -9.8, 2.9, 15.6, -2.9, 11.7, -7.8, 5.9, -10.8, 0, 13.7, -5.9);
            add(list, "Meantone (#, -1/4)", 10.3, -13.7, 3.4, -20.5, -3.4, 13.7, -10.3, 6.8, -17.1, 0, -24.0, -6.8);
            add(list, "Meantone (b, -1/4)", 10.3, 27.4, 3.4, 20.5, -3.4, 13.7, 30.8, 6.8, 24.0, 0, 17.1, -6.8);
            add(list, "Meantone (Homogeneous)", 7.0, -9.4, 2.3, 14.1, -2.3, 9.4, -7.0, 4.7, -11.7, 0, 11.7, -4.7);
            add(list, "Meantone (Homogeneous third)", 12.6, -16.8, 4.2, 25.1, -4.2, 16.8, -12.6, 8.4, -21.0, 0, 21.0, -8.4);
            add(list, "Meantone (Homogeneous gradated)", 4.9, -6.5, 1.6, 9.8, -1.6, 6.5, -4.9, 3.3, -8.1, 0, 8.1, -3.3);
            add(list, "Pythagorean", -5.9, 7.8, -2.0, -11.7, 2.0, -7.8, 5.9, -3.9, 9.8, 0, -9.8, 3.9);
            add(list, "Pythagorean (Perfect)", 15.6, 7.8, 19.6, 11.7, 2.0, 13.7, 5.9, 17.6, 9.8, 0, 11.7, 3.9);
            add(list, "Valotti", 5.9, 0, 2.0, 3.9, -2.0, 7.8, -2.0, 3.9, 2.0, 0, 5.9, -3.9);
            add(list, "Young I", 5.9, 0, 2.0, 3.9, -2.0, 5.9, -2.0, 3.9, 2.0, 0, 5.9, -2.0);
            add(list, "Young II", 5.9, -3.9, 2.0, 0, -2.0, 3.9, -5.9, 3.9, -2.0, 0, 2.0, -3.9);
            builtIns = Collections.unmodifiableList(list);
        }
        return builtIns;
    }

    private static void add(List<Temperament> list, String name, double... cents) {
        list.add(new Temperament(name, cents));
    }

    public static Temperament find(String name, List<Temperament> custom) {
        if (name != null) {
            for (Temperament t : builtIns()) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
            if (custom != null) {
                for (Temperament t : custom) {
                    if (t.name.equals(name)) {
                        return t;
                    }
                }
            }
        }
        return builtIns().get(0);
    }

    public static Temperament findIn(List<Temperament> list, String name) {
        if (list != null) {
            for (Temperament t : list) {
                if (t.name.equals(name)) {
                    return t;
                }
            }
        }
        return null;
    }

    public static List<Temperament> withCustom(List<Temperament> custom) {
        ArrayList<Temperament> list = new ArrayList<Temperament>(builtIns());
        if (custom != null) {
            list.addAll(custom);
        }
        return list;
    }

    public static List<Temperament> sorted(List<Temperament> list) {
        ArrayList<Temperament> result = new ArrayList<Temperament>(list);
        Collections.sort(result, new Comparator<Temperament>() {
            @Override
            public int compare(Temperament a, Temperament b) {
                return a.name.compareTo(b.name);
            }
        });
        return result;
    }

    public static void upsert(List<Temperament> list, Temperament temperament) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(temperament.name)) {
                list.set(i, temperament);
                return;
            }
        }
        list.add(temperament);
    }

    public static List<Temperament> copy(List<Temperament> list) {
        return list == null ? new ArrayList<Temperament>() : new ArrayList<Temperament>(list);
    }
}
