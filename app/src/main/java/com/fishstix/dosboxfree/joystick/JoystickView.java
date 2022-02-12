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

import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

public class JoystickView extends View {
    private static final String TAG = "JoystickView";
    private static final int KEYCODE_A_BUTTON = 38;
    private static final int KEYCODE_B_BUTTON = 39;
    private static final int KEYCODE_C_BUTTON = 40;
    private static final int KEYCODE_X_BUTTON = 49;
    private static final int KEYCODE_Y_BUTTON = 37;
    private static final int KEYCODE_Z_BUTTON = 43;
    private static final int KEYCODE_START_BUTTON = 66;
    private static final int KEYCODE_F1_BUTTON = 131;
    private static final int COLOR_A_BUTTON = 0xA0FF8888;
    private static final int COLOR_B_BUTTON = 0xA088FF88;
    private static final int COLOR_C_BUTTON = 0xA08888FF;
    private static final int COLOR_X_BUTTON = 0xA0FFFF88;
    private static final int COLOR_Y_BUTTON = 0xA0FF88FF;
    private static final int COLOR_Z_BUTTON = 0xA088FFFF;
    private static final int COLOR_START_BUTTON = 0xA0DD8833;
    private static final int COLOR_F1_BUTTON = 0xA0DDDDDD;

    private JoystickDirectional directional;
    private JoystickButton[] buttons;

    private int sizeView;

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

        directional = new JoystickDirectional(this);
        JoystickButton buttonA = new JoystickButton(
            COLOR_A_BUTTON,
            KEYCODE_A_BUTTON
        );
        JoystickButton buttonB = new JoystickButton(
            COLOR_B_BUTTON,
            KEYCODE_B_BUTTON
        );
        JoystickButton buttonC = new JoystickButton(
            COLOR_C_BUTTON,
            KEYCODE_C_BUTTON
        );
        JoystickButton buttonX = new JoystickButton(
            COLOR_X_BUTTON,
            KEYCODE_X_BUTTON
        );
        JoystickButton buttonY = new JoystickButton(
            COLOR_Y_BUTTON,
            KEYCODE_Y_BUTTON
        );
        JoystickButton buttonZ = new JoystickButton(
            COLOR_Z_BUTTON,
            KEYCODE_Z_BUTTON
        );
        JoystickButton buttonStart = new JoystickButton(
            COLOR_START_BUTTON,
            KEYCODE_START_BUTTON
        );
        JoystickButton buttonF1 = new JoystickButton(
            COLOR_F1_BUTTON,
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

        int buttonRadius = (int) ((sizeView * 0.125) * sizefactor);
        JoystickButton.setButtonRadius(buttonRadius);

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

        directional.draw(canvas);

        for (JoystickButton button : buttons) {
            button.draw(canvas);
        }

        canvas.restore();
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
            invalidate();
            button.release();

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
        if (button.inButton(x, y) && !button.isClicked()) {
            invalidate();
            button.click(pId);

            return true;
        }

        return false;
    }

    private boolean processMoveEvent(final MotionEvent ev) {
        if (directional.isClicked()) {
            int pointerIndex = mWrap.findPointerIndex(
                ev,
                directional.getPointerId()
            );

            float touchPositionX = mWrap.getX(ev, pointerIndex);
            float touchPositionY = mWrap.getY(ev, pointerIndex);
            directional.moveHandle(touchPositionX, touchPositionY);
            invalidate();

            return true;
        }

        return false;
    }
}
