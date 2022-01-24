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
package com.fishstix.dosboxfree.joystick;

import com.fishstix.dosboxfree.DosBoxControl;

public class JoystickMovedListener {
    private static final int KEYCODE_UP_BUTTON = 51;
    private static final int KEYCODE_RIGHT_BUTTON = 32;
    private static final int KEYCODE_DOWN_BUTTON = 47;
    private static final int KEYCODE_LEFT_BUTTON = 29;
    private static final int DEADZONE = 50;

    public void onMoved(final int x, final int y) {
        onMoved(x, KEYCODE_RIGHT_BUTTON, KEYCODE_LEFT_BUTTON);
        onMoved(y, KEYCODE_UP_BUTTON, KEYCODE_DOWN_BUTTON);
    }

    private void onMoved(
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

    public void onReleased() { }

    public void onReturnedToCenter() {
        release(KEYCODE_UP_BUTTON);
        release(KEYCODE_RIGHT_BUTTON);
        release(KEYCODE_DOWN_BUTTON);
        release(KEYCODE_LEFT_BUTTON);
    }

    private void press(final int key) {
        DosBoxControl.sendNativeKey(key, true, false, false, false);
    }

    private void release(final int key) {
        DosBoxControl.sendNativeKey(key, false, false, false, false);
    }
}
