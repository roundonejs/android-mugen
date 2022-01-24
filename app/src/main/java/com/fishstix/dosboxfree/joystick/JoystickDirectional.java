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

import android.graphics.Paint;

public class JoystickDirectional extends JoystickViewObject {
    private static final int INNER_PADDING = 10;
    private final Paint backgroudPaint;
    private final Paint handlePaint;
    private int backgroundPosition;

    public JoystickDirectional() {
        backgroudPaint = JoystickHelper.createPaint(0xA0888888);
        handlePaint = JoystickHelper.createPaint(0xB0444444);
    }

    public Paint getBackground() {
        return backgroudPaint;
    }

    public Paint getHandle() {
        return handlePaint;
    }

    public void setAlpha(final int alpha) {
        backgroudPaint.setAlpha(alpha);
        handlePaint.setAlpha(alpha);
    }

    public void setBackgroundPosition(final int position) {
        backgroundPosition = position;
    }

    public int getBackgroundPosition() {
        return backgroundPosition;
    }

    public int getBackgroundRadius() {
        return backgroundPosition - INNER_PADDING;
    }

    public int getHandleRadius() {
        return backgroundPosition / 2;
    }
}
