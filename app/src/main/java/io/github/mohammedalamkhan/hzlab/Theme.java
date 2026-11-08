// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mohammedalamkhan.hzlab;

/** App-wide colour palette. Updated before an activity builds its views. */
public final class Theme {
    public static final int MIDNIGHT = 0;
    public static final int PITCH_BLACK = 1;
    public static final int DAYLIGHT = 2;

    public static int bgTop;
    public static int bgBottom;
    public static int surface;
    public static int raised;
    public static int border;
    public static int text;
    public static int dim;
    public static int faint;
    public static int grid;
    public static int accent;
    public static int accentBright;
    public static int success;
    public static int warning;
    public static int onAccent;
    public static int overlay;
    public static int nav;
    public static boolean light;

    static {
        apply(MIDNIGHT);
    }

    private Theme() {
    }

    public static void apply(int mode) {
        if (mode == DAYLIGHT) {
            bgTop = 0xfff4f8fc;
            bgBottom = 0xffe8f0f7;
            surface = 0xffffffff;
            raised = 0xffe7eef6;
            border = 0xffd1deea;
            text = 0xff102333;
            dim = 0xff5b7286;
            faint = 0xff8ba0b2;
            grid = 0xffd4e0eb;
            accent = 0xff0077b6;
            accentBright = 0xff0096d6;
            success = 0xff138a5b;
            warning = 0xffa96400;
            onAccent = 0xffffffff;
            overlay = 0xb3ffffff;
            nav = 0xffffffff;
            light = true;
        } else if (mode == PITCH_BLACK) {
            bgTop = 0xff000000;
            bgBottom = 0xff000000;
            surface = 0xff0b0b0b;
            raised = 0xff171717;
            border = 0xff2d2d2d;
            text = 0xfff7f7f7;
            dim = 0xffa3a3a3;
            faint = 0xff666666;
            grid = 0xff2c2c2c;
            accent = 0xff5cc8ff;
            accentBright = 0xff8ce7ff;
            success = 0xff63e6a8;
            warning = 0xfff5c86a;
            onAccent = 0xff03121c;
            overlay = 0xcc000000;
            nav = 0xff050505;
            light = false;
        } else {
            bgTop = 0xff07131e;
            bgBottom = 0xff02060a;
            surface = 0xff0d1926;
            raised = 0xff132335;
            border = 0xff20384c;
            text = 0xfff3f8fc;
            dim = 0xff91a7ba;
            faint = 0xff536a7d;
            grid = 0xff2a4054;
            accent = 0xff5cc8ff;
            accentBright = 0xff8ce7ff;
            success = 0xff63e6a8;
            warning = 0xfff5c86a;
            onAccent = 0xff03121c;
            overlay = 0xe6000000;
            nav = 0xff0d1926;
            light = false;
        }
    }
}
