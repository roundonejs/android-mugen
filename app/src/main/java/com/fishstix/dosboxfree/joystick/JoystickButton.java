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

public class JoystickButton {
    private static final int CLICKED_COLOR = 0xA066FF66;
    private final Paint paint;
    private final Paint clickedPaint;
    private boolean clicked;

    public JoystickButton(final int color) {
        paint = JoystickHelper.createPaint(color);
        clickedPaint = JoystickHelper.createPaint(CLICKED_COLOR);
    }

    public Paint getPaint() {
        if (clicked) {
            return clickedPaint;
        }

        return paint;
    }

    public void setAlpha(final int alpha) {
        paint.setAlpha(alpha);
        clickedPaint.setAlpha(alpha);
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(final boolean buttonClicked) {
        clicked = buttonClicked;
    }
}
