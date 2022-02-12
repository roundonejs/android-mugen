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

import android.graphics.Canvas;
import android.graphics.Paint;

import com.fishstix.dosboxfree.DosBoxControl;

public class JoystickButton extends JoystickViewObject {
    private static final int MAX_ALPHA = 255;
    private static final int CLICKED_BUTTON_ALPHA_PLUS = 32;
    private static final int CLICKED_BUTTON_COLOR_PLUS = 0x20000000;
    private static int buttonRadius;
    private final Paint paint;
    private final Paint clickedPaint;
    private final int key;
    private int x;
    private int y;

    public JoystickButton(final int color, final int buttonKey) {
        paint = JoystickHelper.createPaint(color);
        clickedPaint = JoystickHelper.createPaint(
            color
            + CLICKED_BUTTON_COLOR_PLUS
        );
        key = buttonKey;
    }

    public static void setButtonRadius(final int newButtonRadius) {
        buttonRadius = newButtonRadius;
    }

    public void setAlpha(final int alpha) {
        paint.setAlpha(alpha);

        clickedPaint.setAlpha(
            Math.min(alpha + CLICKED_BUTTON_ALPHA_PLUS, MAX_ALPHA)
        );
    }

    public void draw(final Canvas canvas) {
        canvas.drawCircle(x, y, buttonRadius, getActivePaint());
    }

    private Paint getActivePaint() {
        if (isClicked()) {
            return clickedPaint;
        }

        return paint;
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

    @Override
    public void click(final int newPointerId) {
        super.click(newPointerId);

        DosBoxControl.sendNativeKey(key, true, false, false, false);
    }

    @Override
    public void release() {
        super.release();

        DosBoxControl.sendNativeKey(key, false, false, false, false);
    }

    public boolean inButton(final int positionX, final int positionY) {
        return (
            (positionX <= x + buttonRadius) &&
            (positionX >= x - buttonRadius) &&
            (positionY <= y + buttonRadius) &&
            (positionY >= y - buttonRadius)
        );
    }
}
