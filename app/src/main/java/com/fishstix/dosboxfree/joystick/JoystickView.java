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
    private JoystickDirectional directional;
    private JoystickKeypad keypad;
    private int sizeView;
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
        keypad = new JoystickKeypad(this);
    }

    public void setTransparency(final int val) {
        int alpha = 255 - val;

        directional.setAlpha(alpha);
        keypad.setAlpha(alpha);
    }

    public void setSize(final int val) {
        keypad.setSizeFactor(val);
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
        keypad.setSize(sizeView, getMeasuredWidth());
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        canvas.save();

        directional.draw(canvas);
        keypad.draw(canvas);

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

                return keypad.release(pId);
            }
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int x = (int) mWrap.getX(ev, pointerIndex);
                int y = (int) mWrap.getY(ev, pointerIndex);

                if ((x >= 0) && (x < sizeView) && !directional.isClicked()) {
                    directional.click(pId);

                    return true;
                }

                return keypad.click(pId, x, y);
            }
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
