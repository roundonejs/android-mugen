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

package com.fishstix.dosboxfree.touchevent;

import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;

public abstract class TouchEventWrapper {
    public abstract int getPointerId(MotionEvent event, int pointerIndex);
    public abstract int getPointerCount(MotionEvent event);
    public abstract float getX(MotionEvent event, int id);
    public abstract float getY(MotionEvent event, int id);
    public abstract int getButtonState(MotionEvent event);
    public abstract int getSource(MotionEvent event);
    public abstract boolean onGenericMotionEvent(
        SurfaceView view,
        MotionEvent event
    );
    public abstract int[] getDeviceIds();
    public abstract int findPointerIndex(MotionEvent event, int pointerId);

    public static final int SOURCE_CLASS_JOYSTICK = 16;
    public static final int SOURCE_CLASS_MASK = 255;

    public static TouchEventWrapper newInstance() {
        final int sdkVersion = Build.VERSION.SDK_INT;

        if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
            return new CupcakeTouchEvent();
        } else if (sdkVersion < Build.VERSION_CODES.GINGERBREAD) {
            return new FroyoTouchEvent();
        } else if (sdkVersion < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return new GingerbreadTouchEvent();
        } else if (sdkVersion < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new HoneycombTouchEvent();
        }

        return new ICSTouchEvent();
    }
}
