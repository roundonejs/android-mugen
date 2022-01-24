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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.fishstix.dosboxfree.DosBoxControl;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

public class JoystickView extends View {
    private static final String TAG = "JoystickView";
    private static final int NUMBER_OF_FRAMES = 5;
    private static final float MOVEMENT_RANGE = 100;
    private static final float MOVE_RESOLUTION = 1;
    private static final int KEYCODE_A_BUTTON = 38;
    private static final int KEYCODE_B_BUTTON = 39;
    private static final int KEYCODE_C_BUTTON = 40;
    private static final int KEYCODE_X_BUTTON = 49;
    private static final int KEYCODE_Y_BUTTON = 37;
    private static final int KEYCODE_Z_BUTTON = 43;
    private static final int KEYCODE_START_BUTTON = 66;
    private static final int KEYCODE_F1_BUTTON = 131;

    private JoystickDirectional directional;
    private JoystickButton[] buttons;

    private int buttonRadius;

    private JoystickMovedListener moveListener;

    // Last touch point in view coordinates
    private float touchX, touchY;

    // Last reported position in view coordinates (allows different reporting sensitivities)
    private float reportX, reportY;

    private int sizeView;

    // Cartesian coordinates of last touch point - joystick center is (0,0)
    private int cartX, cartY;

    private double sizefactor = 1.0;

    private TouchEventWrapper mWrap = TouchEventWrapper.newInstance();

    public JoystickView(Context context) {
        super(context);
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initJoystickView();
    }

    private void initJoystickView() {
        setFocusable(true);

        directional = new JoystickDirectional();
        JoystickButton buttonA = new JoystickButton(
            0xA0FF8888,
            KEYCODE_A_BUTTON
        );
        JoystickButton buttonB = new JoystickButton(
            0xA08888FF,
            KEYCODE_B_BUTTON
        );
        JoystickButton buttonC = new JoystickButton(
            0xA0FF8888,
            KEYCODE_C_BUTTON
        );
        JoystickButton buttonX = new JoystickButton(
            0xA0FF8888,
            KEYCODE_X_BUTTON
        );
        JoystickButton buttonY = new JoystickButton(
            0xA0FF8888,
            KEYCODE_Y_BUTTON
        );
        JoystickButton buttonZ = new JoystickButton(
            0xA0FF8888,
            KEYCODE_Z_BUTTON
        );
        JoystickButton buttonStart = new JoystickButton(
            0xA0FF8888,
            KEYCODE_START_BUTTON
        );
        JoystickButton buttonF1 = new JoystickButton(
            0xA0FF8888,
            KEYCODE_F1_BUTTON
        );

        buttons = new JoystickButton[] {
            buttonStart,
            buttonX,
            buttonY,
            buttonZ,
            buttonF1,
            buttonA,
            buttonB,
            buttonC
        };

        moveListener = new JoystickMovedListener();
    }

    public void setTransparency(int val) {
        directional.setAlpha(255 - val);

        for (JoystickButton button : buttons) {
            button.setAlpha(255 - val);
        }
    }

    public void setSize(int val) {
        sizefactor = (((double) val + 1) / 6d) - ((val - 5) * 0.12);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Here we make sure that we have a perfect circle
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(
        boolean changed,
        int left,
        int top,
        int right,
        int bottom
    ) {
        super.onLayout(changed, left, top, right, bottom);

        sizeView = Math.min(getMeasuredWidth(), getMeasuredHeight());

        int centerViewPosition = sizeView / 2;
        directional.setBackgroundPosition(centerViewPosition);

        buttonRadius = (int) ((sizeView * 0.125) * sizefactor);

        int centerXButton = getMeasuredWidth() - (int) (buttonRadius * 9.3);
        int[] centerYButtons = new int[2];
        centerYButtons[0] = centerViewPosition - (int) (buttonRadius * 1.5);
        centerYButtons[1] = centerViewPosition + (int) (buttonRadius * 1.5);

        for (int i = 0, length = buttons.length / 2; i < length; i++) {
            for (int j = 0; j < 2; j++) {
                JoystickButton button = buttons[(j * length) + i];
                button.setPosition(centerXButton, centerYButtons[j]);
            }

            centerXButton += (int) (buttonRadius * 2.75);
        }
    }

    private int measure(int measureSpec) {
        int result = 0;
        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }

        return result;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        canvas.save();

        drawDirectional(canvas);

        for (JoystickButton button : buttons) {
            drawButton(canvas, button);
        }

        canvas.restore();
    }

    private void drawDirectional(final Canvas canvas) {
        int backgroundPosition = directional.getBackgroundPosition();
        // Draw the background
        canvas.drawCircle(
            backgroundPosition,
            backgroundPosition,
            directional.getBackgroundRadius(),
            directional.getBackground()
        );

        // Draw the handle
        float handleX = touchX + backgroundPosition;
        float handleY = touchY + backgroundPosition;
        canvas.drawCircle(
            handleX,
            handleY,
            directional.getHandleRadius(),
            directional.getHandle()
        );
    }

    private void drawButton(final Canvas canvas, final JoystickButton button) {
        canvas.drawCircle(
            button.getX(),
            button.getY(),
            buttonRadius,
            button.getPaint()
        );
    }

    private boolean inButton(
        final JoystickButton button,
        final int x,
        final int y
    ) {
        int buttonPositionX = button.getX();
        int buttonPositionY = button.getY();

        return (
            (x <= buttonPositionX + buttonRadius) &&
            (x >= buttonPositionX - buttonRadius) &&
            (y <= buttonPositionY + buttonRadius) &&
            (y >= buttonPositionY - buttonRadius)
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int pointerIndex = (
            (action & MotionEvent.ACTION_POINTER_ID_MASK) >>
            MotionEvent.ACTION_POINTER_ID_SHIFT
        );
        final int pId = mWrap.getPointerId(ev, pointerIndex);

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                return processMoveEvent(ev);
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                if (
                    (pId == directional.getPointerId())
                    && directional.isClicked()
                ) {
                    returnHandleToCenter();
                    directional.release();

                    return true;
                }

                for (JoystickButton button : buttons) {
                    if (releaseButton(button, pId)) {
                        return true;
                    }
                }

                return false;
            }
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int x = (int) mWrap.getX(ev, pointerIndex);
                int y = (int) mWrap.getY(ev, pointerIndex);

                if ((x >= 0) && (x < sizeView) && !directional.isClicked()) {
                    directional.click(pId);

                    return true;
                }

                for (JoystickButton button : buttons) {
                    if (clickButton(button, pId, x, y)) {
                        return true;
                    }
                }

                return false;
            }
        }

        return false;
    }

    private boolean releaseButton(final JoystickButton button, final int pId) {
        if ((pId == button.getPointerId()) && button.isClicked()) {
            button.release();
            invalidate();

            DosBoxControl.sendNativeKey(
                button.getKey(),
                false,
                false,
                false,
                false
            );

            return true;
        }

        return false;
    }

    private boolean clickButton(
        final JoystickButton button,
        final int pId,
        final int x,
        final int y
    ) {
        if (inButton(button, x, y) && !button.isClicked()) {
            button.click(pId);
            invalidate();

            DosBoxControl.sendNativeKey(
                button.getKey(),
                true,
                false,
                false,
                false
            );

            return true;
        }

        return false;
    }

    private boolean processMoveEvent(final MotionEvent ev) {
        if (directional.isClicked()) {
            int backgroundPosition = directional.getBackgroundPosition();
            int pointerIndex = mWrap.findPointerIndex(
                ev,
                directional.getPointerId()
            );

            // Translate touch position to center of view
            float x = mWrap.getX(ev, pointerIndex);
            touchX = x - backgroundPosition;
            float y = mWrap.getY(ev, pointerIndex);
            touchY = y - backgroundPosition;

            reportOnMoved();
            invalidate();

            return true;
        }

        return false;
    }

    private void reportOnMoved() {
        constrainBox();
        calcUserCoordinates();

        boolean rx = Math.abs(touchX - reportX) >= MOVE_RESOLUTION;
        boolean ry = Math.abs(touchY - reportY) >= MOVE_RESOLUTION;

        if (rx || ry) {
            this.reportX = touchX;
            this.reportY = touchY;

            moveListener.onMoved(cartX, cartY);
        }
    }

    private void constrainBox() {
        int movementRadius = directional.getHandleRadius();
        touchX = Math.max(Math.min(touchX, movementRadius), -movementRadius);
        touchY = Math.max(Math.min(touchY, movementRadius), -movementRadius);
    }

    private void calcUserCoordinates() {
        // First convert to cartesian coordinates
        int movementRadius = directional.getHandleRadius();
        cartX = (int) (touchX / movementRadius * MOVEMENT_RANGE);
        cartY = (int) (-touchY / movementRadius * MOVEMENT_RANGE);
    }

    private void returnHandleToCenter() {
        final double intervalsX = -touchX / NUMBER_OF_FRAMES;
        final double intervalsY = -touchY / NUMBER_OF_FRAMES;

        for (int i = 0; i < NUMBER_OF_FRAMES; i++) {
            final int j = i;
            postDelayed(
                new Runnable() {
                public void run() {
                    touchX += intervalsX;
                    touchY += intervalsY;

                    reportOnMoved();
                    invalidate();

                    if (j == (NUMBER_OF_FRAMES - 1)) {
                        moveListener.onReturnedToCenter();
                    }
                }
            },
                i * 40
            );
        }

        moveListener.onReleased();
    }
}
