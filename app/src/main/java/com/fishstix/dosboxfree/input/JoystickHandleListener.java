/*
 *  Copyright (C) 2012 Fishstix (Gene Ruebsamen - ruebsamen.gene@gmail.com)
 *
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
package com.fishstix.dosboxfree.input;

import java.util.HashMap;
import java.util.Map;

import com.fishstix.dosboxfree.DosBoxControl;

public class JoystickHandleListener {
    private static final int DEADZONE = 50;
    private static final Map<Integer, Boolean> pressedKeys;

    static {
        pressedKeys = new HashMap<>();
        pressedKeys.put(DosBoxControl.KEYCODE_UP_BUTTON, false);
        pressedKeys.put(DosBoxControl.KEYCODE_RIGHT_BUTTON, false);
        pressedKeys.put(DosBoxControl.KEYCODE_DOWN_BUTTON, false);
        pressedKeys.put(DosBoxControl.KEYCODE_LEFT_BUTTON, false);
    }

    public static void onMoved(final int x, final int y) {
        onMoved(
            x,
            DosBoxControl.KEYCODE_RIGHT_BUTTON,
            DosBoxControl.KEYCODE_LEFT_BUTTON
        );
        onMoved(
            y,
            DosBoxControl.KEYCODE_UP_BUTTON,
            DosBoxControl.KEYCODE_DOWN_BUTTON
        );
    }

    private static void onMoved(
        final int position,
        final int positiveKeycode,
        final int negativeKeycode
    ) {
        if (position > DEADZONE) {
            press(positiveKeycode);
        } else if (position < -DEADZONE) {
            press(negativeKeycode);
        } else {
            release(positiveKeycode);
            release(negativeKeycode);
        }
    }

    public static void onReleased() {
        release(DosBoxControl.KEYCODE_UP_BUTTON);
        release(DosBoxControl.KEYCODE_RIGHT_BUTTON);
        release(DosBoxControl.KEYCODE_DOWN_BUTTON);
        release(DosBoxControl.KEYCODE_LEFT_BUTTON);
    }

    private static void press(final int key) {
        if (!pressedKeys.get(key).booleanValue()) {
            DosBoxControl.pressNativeKey(key);
            pressedKeys.put(key, true);
        }
    }

    private static void release(final int key) {
        if (pressedKeys.get(key).booleanValue()) {
            DosBoxControl.releaseNativeKey(key);
            pressedKeys.put(key, false);
        }
    }
}
