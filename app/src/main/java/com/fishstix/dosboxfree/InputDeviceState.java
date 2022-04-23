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

package com.fishstix.dosboxfree;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class InputDeviceState {
    private final InputDevice mDevice;
    private final int[] mAxes;
    private final float[] mAxisValues;
    private final SparseIntArray mKeys;

    public InputDeviceState(final InputDevice device) {
        mDevice = device;
        int numAxes = 0;

        for (MotionRange range : device.getMotionRanges()) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                numAxes += 1;
            }
        }

        mAxes = new int[numAxes];
        mAxisValues = new float[numAxes];
        mKeys = new SparseIntArray();

        int i = 0;

        for (MotionRange range : device.getMotionRanges()) {
            if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                mAxes[i++] = range.getAxis();
            }
        }
    }

    public static float ProcessAxis(
        final MotionRange range,
        final float axisvalue
    ) {
        float absaxisvalue = Math.abs(axisvalue);
        float deadzone = range.getFlat();

        if (absaxisvalue <= deadzone) {
            return 0.0f;
        }

        if (axisvalue < 0.0f) {
            return absaxisvalue / range.getMin();
        }

        return absaxisvalue / range.getMax();
    }
}
