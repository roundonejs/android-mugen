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
    private static final float MINIMUM_POINT_DISTANCE = 1;
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

    private float touchPointX, touchPointY;
    private float lastTouchPointX, lastTouchPointY;

    private int sizeView;

    // Cartesian coordinates of last touch point - joystick center is (0,0)
    private int cartX, cartY;

    private double sizefactor = 1.0;

    private TouchEventWrapper mWrap = TouchEventWrapper.newInstance();

    public JoystickView(final Context context) {
        super(context);
        initJoystickView();
    }

    public JoystickView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initJoystickView();
    }

    public JoystickView(
        final Context context,
        final AttributeSet attrs,
        final int defStyle
    ) {
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

    public void setTransparency(final int val) {
        directional.setAlpha(255 - val);

        for (JoystickButton button : buttons) {
            button.setAlpha(255 - val);
        }
    }

    public void setSize(final int val) {
        sizefactor = (((double) val + 1) / 6d) - ((val - 5) * 0.12);
    }

    @Override
    protected void onMeasure(
        final int widthMeasureSpec,
        final int heightMeasureSpec
    ) {
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private int measure(final int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            return 200;
        }

        return MeasureSpec.getSize(measureSpec);
    }

    @Override
    protected void onLayout(
        final boolean changed,
        final int left,
        final int top,
        final int right,
        final int bottom
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

    @Override
    protected void onDraw(final Canvas canvas) {
        canvas.save();

        drawDirectional(canvas);

        for (JoystickButton button : buttons) {
            button.draw(canvas, buttonRadius);
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
        float handleX = touchPointX + backgroundPosition;
        float handleY = touchPointY + backgroundPosition;
        canvas.drawCircle(
            handleX,
            handleY,
            directional.getHandleRadius(),
            directional.getHandle()
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
    public boolean onTouchEvent(final MotionEvent ev) {
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
            touchPointX = x - backgroundPosition;
            float y = mWrap.getY(ev, pointerIndex);
            touchPointY = y - backgroundPosition;

            reportOnMoved();
            invalidate();

            return true;
        }

        return false;
    }

    private void reportOnMoved() {
        constrainBox();
        calcUserCoordinates();

        if (
            (Math.abs(touchPointX - lastTouchPointX) >= MINIMUM_POINT_DISTANCE)
            || (
                Math.abs(touchPointY - lastTouchPointY)
                >= MINIMUM_POINT_DISTANCE
            )
        ) {
            lastTouchPointX = touchPointX;
            lastTouchPointY = touchPointY;

            moveListener.onMoved(cartX, cartY);
        }
    }

    private void constrainBox() {
        int movementRadius = directional.getHandleRadius();
        touchPointX = Math.max(
            Math.min(
                touchPointX,
                movementRadius
            ),
            -movementRadius
        );
        touchPointY = Math.max(
            Math.min(
                touchPointY,
                movementRadius
            ),
            -movementRadius
        );
    }

    private void calcUserCoordinates() {
        // First convert to cartesian coordinates
        int movementRadius = directional.getHandleRadius();
        cartX = (int) (touchPointX / movementRadius * MOVEMENT_RANGE);
        cartY = (int) (-touchPointY / movementRadius * MOVEMENT_RANGE);
    }

    private void returnHandleToCenter() {
        final double intervalsX = -touchPointX / NUMBER_OF_FRAMES;
        final double intervalsY = -touchPointY / NUMBER_OF_FRAMES;

        for (int i = 0; i < NUMBER_OF_FRAMES; i++) {
            final int j = i;
            postDelayed(
                new Runnable() {
                public void run() {
                    touchPointX += intervalsX;
                    touchPointY += intervalsY;

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
