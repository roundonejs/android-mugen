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
import android.view.View;

public class JoystickDirectional extends JoystickViewObject {
    private static final int BACKGROUND_COLOR = 0xA0888888;
    private static final int HANDLE_COLOR = 0xB0444444;
    private static final int INNER_PADDING = 10;
    private static final float MINIMUM_POINT_DISTANCE = 1;
    private static final int NUMBER_FRAMES_HANDLE_TO_CENTER = 5;
    private static final int DELAY_BETWEEN_FRAMES = 40;
    private final Paint backgroudPaint;
    private final Paint handlePaint;
    private int backgroundPosition;
    private float handlePositionX, handlePositionY;
    private float lastHandlePointX, lastHandlePointY;
    private View view;

    public JoystickDirectional(final View currentView) {
        backgroudPaint = JoystickHelper.createPaint(BACKGROUND_COLOR);
        handlePaint = JoystickHelper.createPaint(HANDLE_COLOR);
        view = currentView;
    }

    public void setAlpha(final int alpha) {
        backgroudPaint.setAlpha(alpha);
        handlePaint.setAlpha(alpha);
    }

    public void draw(final Canvas canvas) {
        drawBackground(canvas);
        drawHandle(canvas);
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

    private void drawHandle(final Canvas canvas) {
        float handleAbsolutePositionX = handlePositionX + backgroundPosition;
        float handleAbsolutePositionY = handlePositionY + backgroundPosition;
        int handleRadius = getHandleRadius();

        canvas.drawCircle(
            handleAbsolutePositionX,
            handleAbsolutePositionY,
            handleRadius,
            handlePaint
        );
    }

    public void setBackgroundPosition(final int position) {
        backgroundPosition = position;
    }

    @Override
    public void release() {
        super.release();

        returnHandleToCenter();
    }

    private void returnHandleToCenter() {
        final double intervalsX = - (
            handlePositionX / NUMBER_FRAMES_HANDLE_TO_CENTER
        );
        final double intervalsY = - (
            handlePositionY / NUMBER_FRAMES_HANDLE_TO_CENTER
        );

        for (int i = 0; i < NUMBER_FRAMES_HANDLE_TO_CENTER; i++) {
            final int frameNumber = i;
            Runnable viewAnimationHandleToCenter = new Runnable() {
                public void run() {
                    handlePositionX += intervalsX;
                    handlePositionY += intervalsY;

                    reportOnMoved();
                    view.invalidate();

                    if (frameNumber == (NUMBER_FRAMES_HANDLE_TO_CENTER - 1)) {
                        JoystickMovedListener.onReturnedToCenter();
                    }
                }
            };

            view.postDelayed(
                viewAnimationHandleToCenter,
                i * DELAY_BETWEEN_FRAMES
            );
        }

        JoystickMovedListener.onReleased();
    }

    public void moveHandle(
        final float touchPositionX,
        final float touchPositionY
    ) {
        handlePositionX = calculateHandlePoint(touchPositionX);
        handlePositionY = calculateHandlePoint(touchPositionY);

        reportOnMoved();
    }

    private float calculateHandlePoint(final float touchAbsolutePosition) {
        int handleRadius = getHandleRadius();
        float touchPosition = touchAbsolutePosition - backgroundPosition;

        return Math.max(
            Math.min(
                touchPosition,
                handleRadius
            ),
            -handleRadius
        );
    }

    private void reportOnMoved() {
        if (isMoved()) {
            lastHandlePointX = handlePositionX;
            lastHandlePointY = handlePositionY;

            int percentagePositionX = calculateHandlePercentagePosition(
                handlePositionX
            );
            int percentagePositionY = -calculateHandlePercentagePosition(
                handlePositionY
            );
            JoystickMovedListener.onMoved(
                percentagePositionX,
                percentagePositionY
            );
        }
    }

    private boolean isMoved() {
        return (
            (
                Math.abs(handlePositionX - lastHandlePointX)
                >= MINIMUM_POINT_DISTANCE
            )
            || (
                Math.abs(handlePositionY - lastHandlePointY)
                >= MINIMUM_POINT_DISTANCE
            )
        );
    }

    private int calculateHandlePercentagePosition(final float handlePosition) {
        return (int) (handlePosition / getHandleRadius() * 100);
    }

    private int getHandleRadius() {
        return backgroundPosition / 2;
    }
}
