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

public class JoystickButton extends JoystickViewObject {
    private static final int CLICKED_COLOR = 0xA066FF66;
    private final Paint paint;
    private final Paint clickedPaint;
    private final int key;
    private int x;
    private int y;

    public JoystickButton(final int color, final int buttonKey) {
        paint = JoystickHelper.createPaint(color);
        clickedPaint = JoystickHelper.createPaint(CLICKED_COLOR);
        key = buttonKey;
    }

    public Paint getPaint() {
        if (isClicked()) {
            return clickedPaint;
        }

        return paint;
    }

    public int getKey() {
        return key;
    }

    public void setAlpha(final int alpha) {
        paint.setAlpha(alpha);
        clickedPaint.setAlpha(alpha);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(final int positionX, final int positionY) {
        x = positionX;
        y = positionY;
    }
}
