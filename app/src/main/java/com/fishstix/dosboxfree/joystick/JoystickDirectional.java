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

public class JoystickDirectional extends JoystickViewObject {
    private static final int INNER_PADDING = 10;
    private final Paint backgroudPaint;
    private final Paint handlePaint;
    private int backgroundPosition;

    public JoystickDirectional() {
        backgroudPaint = JoystickHelper.createPaint(0xA0888888);
        handlePaint = JoystickHelper.createPaint(0xB0444444);
    }

    public void setAlpha(final int alpha) {
        backgroudPaint.setAlpha(alpha);
        handlePaint.setAlpha(alpha);
    }

    public void draw(
        final Canvas canvas,
        final float touchPointX,
        final float touchPointY
    ) {
        drawBackground(canvas);
        drawHandle(canvas, touchPointX, touchPointY);
    }

    private void drawBackground(final Canvas canvas) {
        int backgroundRadius = backgroundPosition - INNER_PADDING;

        canvas.drawCircle(
            backgroundPosition,
            backgroundPosition,
            backgroundRadius,
            backgroudPaint
        );
    }

    private void drawHandle(
        final Canvas canvas,
        final float touchPointX,
        final float touchPointY
    ) {
        float handleX = touchPointX + backgroundPosition;
        float handleY = touchPointY + backgroundPosition;
        int handleRadius = backgroundPosition / 2;

        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint);
    }

    public void setBackgroundPosition(final int position) {
        backgroundPosition = position;
    }

    public int getBackgroundPosition() {
        return backgroundPosition;
    }

    public int getHandleRadius() {
        return backgroundPosition / 2;
    }
}
