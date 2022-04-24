/*
 *  Copyright (C) 2012 Fishstix - Based upon DosBox & anDosBox by Locnet
 *
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.fishstix.dosboxfree;

public class DosBoxControl {
    public static final int KEYCODE_UP_BUTTON = 51;
    public static final int KEYCODE_RIGHT_BUTTON = 32;
    public static final int KEYCODE_DOWN_BUTTON = 47;
    public static final int KEYCODE_LEFT_BUTTON = 29;
    public static final int KEYCODE_A_BUTTON = 38;
    public static final int KEYCODE_B_BUTTON = 39;
    public static final int KEYCODE_C_BUTTON = 40;
    public static final int KEYCODE_X_BUTTON = 49;
    public static final int KEYCODE_Y_BUTTON = 37;
    public static final int KEYCODE_Z_BUTTON = 43;
    public static final int KEYCODE_START_BUTTON = 66;
    public static final int KEYCODE_F1_BUTTON = 131;

    public static native int nativeKey(
        int keyCode,
        int down,
        int ctrl,
        int alt,
        int shift
    );

    public static native int nativeGetCycleCount();

    public static native int nativeGetFrameSkipCount();

    public static native int nativeGetMemSize();

    public static native boolean nativeGetAutoAdjust();

    public static boolean sendNativeKey(
        final int keyCode,
        final boolean down,
        final boolean ctrl,
        final boolean alt,
        final boolean shift
    ) {
        int intDown = (down) ? 1 : 0;
        int intCtrl = (ctrl) ? 1 : 0;
        int intAlt = (alt) ? 1 : 0;
        int intShift = (shift) ? 1 : 0;

        return nativeKey(keyCode, intDown, intCtrl, intAlt, intShift) != 0;
    }
}
