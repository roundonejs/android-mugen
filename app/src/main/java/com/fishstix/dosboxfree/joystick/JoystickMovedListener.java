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

import com.fishstix.dosboxfree.DBGLSurfaceView;
import com.fishstix.dosboxfree.DosBoxControl;

public class JoystickMovedListener {
    public void onMoved(final int pan, final int tilt) {
        DosBoxControl.nativeJoystick(
            pan,
            tilt,
            DBGLSurfaceView.ACTION_MOVE,
            -1
        );
    }

    public void onReleased() { }

    public void onReturnedToCenter() {
        DosBoxControl.nativeJoystick(0, 0, DBGLSurfaceView.ACTION_MOVE, -1);
    }
}
