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

public abstract class JoystickViewObject {
    private static final int INVALID_POINTER_ID = -1;
    private int pointerId;

    public JoystickViewObject() {
        pointerId = INVALID_POINTER_ID;
    }

    public void click(final int newPointerId) {
        pointerId = newPointerId;
    }

    public int getPointerId() {
        return pointerId;
    }

    public boolean isClicked() {
        return pointerId != INVALID_POINTER_ID;
    }

    public void release() {
        pointerId = INVALID_POINTER_ID;
    }
}
