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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

public class JoystickView extends View {
    private static final String TAG = "JoystickView";
    public static final int INVALID_POINTER_ID = -1;

    private Paint bgPaint;
    private Paint handlePaint;

    private JoystickButton buttonA;
    private JoystickButton buttonB;
    private JoystickButton buttonC;
    private JoystickButton buttonX;
    private JoystickButton buttonY;
    private JoystickButton buttonZ;
    private JoystickButton buttonStart;
    private Paint butClickPaint;

    private int innerPadding;
    private int bgRadius;
    private int handleRadius;
    private int buttonRadius;
    private int movementRadius;
    private int handleInnerBoundaries;

    private JoystickMovedListener moveListener;
    private final JoystickClickedListener clickListener = (
        new JoystickClickedListener()
    );

    // # of pixels movement required between reporting to the listener
    private float moveResolution;

    private boolean yAxisInverted;
    private boolean autoReturnToCenter;

    // Max range of movement in user coordinate system
    public final static int CONSTRAIN_BOX = 0;
    public final static int CONSTRAIN_CIRCLE = 1;
    private int movementConstraint;
    private float movementRange;

    // Regular cartesian coordinates
    public final static int COORDINATE_CARTESIAN = 0;
    // Uses polar rotation of 45 degrees to calc differential drive paramaters
    public final static int COORDINATE_DIFFERENTIAL = 1;
    private int userCoordinateSystem;

    // Records touch pressure for click handling
    private boolean clickedJoy = false, clickedA = false, clickedB = false;
    private float clickThreshold;

    // Last touch point in view coordinates
    private int pointerId = INVALID_POINTER_ID;
    private int pointerId_butA = INVALID_POINTER_ID;
    private int pointerId_butB = INVALID_POINTER_ID;
    private float touchX, touchY;

    // Last reported position in view coordinates (allows different reporting sensitivities)
    private float reportX, reportY;

    // Handle center in view coordinates
    private float handleX, handleY;

    // Center of the view in view coordinates
    private int cX, cY;

    private int centerXButtons1;
    private int centerXButtons2;
    private int centerXButtons3;
    private int centerYButtons1;
    private int centerYButtons2;
    private int centerXStart;
    private int centerYStart;

    // Size of the view in view coordinates
    private int dimX, dimY;

    private int fulldimX;

    // Cartesian coordinates of last touch point - joystick center is (0,0)
    private int cartX, cartY;

    // Polar coordinates of the touch point from joystick center
    private double radial;

    // User coordinates of last touch point
    private int userX, userY;

    // Offset co-ordinates (used when touch events are received from parent's coordinate origin)
    private int offsetX;
    private int offsetY;

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

        bgPaint = JoystickHelper.createPaint(0xA0888888);
        handlePaint = JoystickHelper.createPaint(0xB0444444);
        buttonA = new JoystickButton(0xA0FF8888);
        buttonB = new JoystickButton(0xA08888FF);
        buttonC = new JoystickButton(0xA0FF8888);
        buttonX = new JoystickButton(0xA0FF8888);
        buttonY = new JoystickButton(0xA0FF8888);
        buttonZ = new JoystickButton(0xA0FF8888);
        buttonStart = new JoystickButton(0xA0FF8888);
        butClickPaint = JoystickHelper.createPaint(0xA066FF66);

        innerPadding = 10;

        setMovementRange(256);
        setMoveResolution(1.0f);
        setClickThreshold(0.4f);
        setYAxisInverted(true);
        setUserCoordinateSystem(COORDINATE_CARTESIAN);
        setAutoReturnToCenter(true);
    }

    public void setAutoReturnToCenter(boolean autoReturnToCenter) {
        this.autoReturnToCenter = autoReturnToCenter;
    }

    public boolean isAutoReturnToCenter() {
        return autoReturnToCenter;
    }

    public void setUserCoordinateSystem(int userCoordinateSystem) {
        if (
            (userCoordinateSystem < COORDINATE_CARTESIAN) ||
            (movementConstraint > COORDINATE_DIFFERENTIAL)
        ) {
            Log.e(TAG, "invalid value for userCoordinateSystem");
        } else {
            this.userCoordinateSystem = userCoordinateSystem;
        }
    }

    public int getUserCoordinateSystem() {
        return userCoordinateSystem;
    }

    public void setMovementConstraint(int movementConstraint) {
        if (
            (movementConstraint < CONSTRAIN_BOX) ||
            (movementConstraint > CONSTRAIN_CIRCLE)
        ) {
            Log.e(TAG, "invalid value for movementConstraint");
        } else {
            this.movementConstraint = movementConstraint;
        }
    }

    public int getMovementConstraint() {
        return movementConstraint;
    }

    public boolean isYAxisInverted() {
        return yAxisInverted;
    }

    public void setYAxisInverted(boolean yAxisInverted) {
        this.yAxisInverted = yAxisInverted;
    }

    /**
     * Set the pressure sensitivity for registering a click
     * @param clickThreshold threshold 0...1.0f inclusive. 0 will cause clicks to never be reported, 1.0 is a very hard click
     */
    public void setClickThreshold(float clickThreshold) {
        if ((clickThreshold < 0) || (clickThreshold > 1.0f)) {
            Log.e(TAG, "clickThreshold must range from 0...1.0f inclusive");
        } else {
            this.clickThreshold = clickThreshold;
        }
    }

    public float getClickThreshold() {
        return clickThreshold;
    }

    public void setMovementRange(float movementRange) {
        this.movementRange = movementRange;
    }

    public float getMovementRange() {
        return movementRange;
    }

    public void setMoveResolution(float moveResolution) {
        this.moveResolution = moveResolution;
    }

    public float getMoveResolution() {
        return moveResolution;
    }

    public void setTransparency(int val) {
        bgPaint.setAlpha(255 - val);
        handlePaint.setAlpha(255 - val);
        buttonA.setAlpha(255 - val);
        buttonB.setAlpha(255 - val);
        buttonC.setAlpha(255 - val);
        buttonX.setAlpha(255 - val);
        buttonY.setAlpha(255 - val);
        buttonZ.setAlpha(255 - val);
        buttonStart.setAlpha(255 - val);
        butClickPaint.setAlpha(255 - val);
    }

    public void setSize(int val) {
        sizefactor = (((double) val + 1) / 6d) - ((val - 5) * 0.12);
    }

    public void setOnJostickMovedListener(JoystickMovedListener listener) {
        this.moveListener = listener;
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

        int d = Math.min(getMeasuredWidth(), getMeasuredHeight());

        fulldimX = getMeasuredWidth();

        dimX = d;
        dimY = d;

        cX = d / 2;
        cY = d / 2;

        buttonRadius = (int) ((d * 0.15) * sizefactor);

        centerXButtons1 = fulldimX - (int) (buttonRadius * 7.3);
        centerXButtons2 = centerXButtons1 + (int) (buttonRadius * 3);
        centerXButtons3 = centerXButtons2 + (int) (buttonRadius * 3);

        centerYButtons1 = cY - (int) (buttonRadius * 1.5);
        centerYButtons2 = cY + (int) (buttonRadius * 1.5);

        centerXStart = fulldimX / 2;
        centerYStart = cY - (int) (buttonRadius * 2.5);

        bgRadius = (int) (dimX / 2 - innerPadding);
        handleRadius = (int) (d * 0.25);
        handleInnerBoundaries = handleRadius;
        movementRadius = Math.min(cX, cY) - handleInnerBoundaries;
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
    protected void onDraw(Canvas canvas) {
        canvas.save();
        // Draw the background
        canvas.drawCircle(cX, cY, bgRadius, bgPaint);

        // Draw the handle
        handleX = touchX + cX;
        handleY = touchY + cY;
        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint);

        // Draw the buttons
        if (clickedA) {
            canvas.drawCircle(
                centerXButtons1,
                centerYButtons1,
                buttonRadius,
                butClickPaint
            );
        } else {
            canvas.drawCircle(
                centerXButtons1,
                centerYButtons1,
                buttonRadius,
                buttonA.getPaint()
            );
        }

        // Draw the buttons
        if (clickedB) {
            canvas.drawCircle(
                centerXButtons2,
                centerYButtons1,
                buttonRadius,
                butClickPaint
            );
        } else {
            canvas.drawCircle(
                centerXButtons2,
                centerYButtons1,
                buttonRadius,
                buttonB.getPaint()
            );
        }

        canvas.drawCircle(
            centerXButtons3,
            centerYButtons1,
            buttonRadius,
            buttonC.getPaint()
        );
        canvas.drawCircle(
            centerXButtons1,
            centerYButtons2,
            buttonRadius,
            buttonX.getPaint()
        );
        canvas.drawCircle(
            centerXButtons2,
            centerYButtons2,
            buttonRadius,
            buttonY.getPaint()
        );
        canvas.drawCircle(
            centerXButtons3,
            centerYButtons2,
            buttonRadius,
            buttonZ.getPaint()
        );
        canvas.drawCircle(
            centerXStart,
            centerYStart,
            buttonRadius,
            buttonStart.getPaint()
        );

        canvas.restore();
    }

    // Constrain touch within a box
    private void constrainBox() {
        touchX = Math.max(Math.min(touchX, movementRadius), -movementRadius);
        touchY = Math.max(Math.min(touchY, movementRadius), -movementRadius);
    }

    // Constrain touch within a circle
    private void constrainCircle() {
        float diffX = touchX;
        float diffY = touchY;
        double radial = Math.sqrt((diffX * diffX) + (diffY * diffY));

        if (radial > movementRadius) {
            touchX = (int) ((diffX / radial) * movementRadius);
            touchY = (int) ((diffY / radial) * movementRadius);
        }
    }

    public void setPointerId(int id) {
        this.pointerId = id;
    }

    private void setPointerIdButtonA(int id) {
        this.pointerId_butA = id;
    }

    private void setPointerIdButtonB(int id) {
        this.pointerId_butB = id;
    }

    public int getPointerId() {
        return pointerId;
    }

    private boolean inButtonA(int x, int y) {
        if (
            (x <= centerXButtons1 + buttonRadius) &&
            (x >= centerXButtons1 - buttonRadius)
        ) {
            if (
                (y <= centerYButtons1 + buttonRadius) &&
                (y >= centerYButtons1 - buttonRadius)
            ) {
                return true;
            }
        }

        return false;
    }

    private boolean inButtonB(int x, int y) {
        if (
            (x <= centerXButtons2 + buttonRadius) &&
            (x >= centerXButtons2 - buttonRadius)
        ) {
            if (
                (y <= centerYButtons1 + buttonRadius) &&
                (y >= centerYButtons1 - buttonRadius)
            ) {
                return true;
            }
        }

        return false;
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
                if (pId == this.pointerId) {
                    if ((pointerId != INVALID_POINTER_ID) && clickedJoy) {
                        returnHandleToCenter();
                        clickedJoy = false;
                        setPointerId(INVALID_POINTER_ID);

                        return true;
                    }
                } else if (pId == this.pointerId_butA) {
                    if ((pointerId_butA != INVALID_POINTER_ID) && clickedA) {
                        clickedA = false;
                        setPointerIdButtonA(INVALID_POINTER_ID);
                        invalidate();

                        if (clickListener != null) {
                            clickListener.onReleased(0);
                        }

                        return true;
                    }
                } else if (pId == this.pointerId_butB) {
                    if ((pointerId_butB != INVALID_POINTER_ID) && clickedB) {
                        clickedB = false;
                        setPointerIdButtonB(INVALID_POINTER_ID);
                        invalidate();

                        if (clickListener != null) {
                            clickListener.onReleased(1);
                        }

                        return true;
                    }
                }

                break;
            }
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int x = (int) mWrap.getX(ev, pointerIndex);
                int y = (int) mWrap.getY(ev, pointerIndex);

                if (((x >= offsetX) && (x < offsetX + dimX)) && !clickedJoy) {
                    if (pointerId == INVALID_POINTER_ID) {
                        // pointer within joystick
                        setPointerId(pId);
                        clickedJoy = true;

                        return true;
                    }
                } else if (inButtonA(x, y) && !clickedA) {
                    if (pointerId_butA == INVALID_POINTER_ID) {
                        // pointer within A button
                        setPointerIdButtonA(pId);
                        clickedA = true;
                        invalidate();

                        if (clickListener != null) {
                            clickListener.onClicked(0);
                        }

                        return true;
                    }
                } else if (inButtonB(x, y) && !clickedB) {
                    if (pointerId_butB == INVALID_POINTER_ID) {
                        setPointerIdButtonB(pId);
                        clickedB = true;
                        invalidate();

                        if (clickListener != null) {
                            clickListener.onClicked(1);
                        }

                        return true;
                    }
                }

                break;
            }
        }

        return false;
    }

    private boolean processMoveEvent(MotionEvent ev) {
        if (pointerId != INVALID_POINTER_ID) {
            final int pointerIndex = mWrap.findPointerIndex(ev, pointerId);

            // Translate touch position to center of view
            float x = mWrap.getX(ev, pointerIndex);
            touchX = x - cX - offsetX;
            float y = mWrap.getY(ev, pointerIndex);
            touchY = y - cY - offsetY;

            reportOnMoved();
            invalidate();

            return true;
        }

        return false;
    }

    private void reportOnMoved() {
        if (movementConstraint == CONSTRAIN_CIRCLE) {
            constrainCircle();
        } else {
            constrainBox();
        }

        calcUserCoordinates();

        if (moveListener != null) {
            boolean rx = Math.abs(touchX - reportX) >= moveResolution;
            boolean ry = Math.abs(touchY - reportY) >= moveResolution;

            if (rx || ry) {
                this.reportX = touchX;
                this.reportY = touchY;

                moveListener.OnMoved(userX, userY);
            }
        }
    }

    private void calcUserCoordinates() {
        // First convert to cartesian coordinates
        cartX = (int) (touchX / movementRadius * movementRange);
        cartY = (int) (touchY / movementRadius * movementRange);

        radial = Math.sqrt((cartX * cartX) + (cartY * cartY));

        // Invert Y axis if requested
        if (!yAxisInverted) {
            cartY *= -1;
        }

        if (userCoordinateSystem == COORDINATE_CARTESIAN) {
            userX = cartX;
            userY = cartY;
        } else if (userCoordinateSystem == COORDINATE_DIFFERENTIAL) {
            userX = cartY + cartX / 4;
            userY = cartY - cartX / 4;

            if (userX < -movementRange) {
                userX = (int) -movementRange;
            }

            if (userX > movementRange) {
                userX = (int) movementRange;
            }

            if (userY < -movementRange) {
                userY = (int) -movementRange;
            }

            if (userY > movementRange) {
                userY = (int) movementRange;
            }
        }
    }

    private void returnHandleToCenter() {
        if (autoReturnToCenter) {
            final int numberOfFrames = 5;
            final double intervalsX = (0 - touchX) / numberOfFrames;
            final double intervalsY = (0 - touchY) / numberOfFrames;

            for (int i = 0; i < numberOfFrames; i++) {
                final int j = i;
                postDelayed(
                    new Runnable() {
                    public void run() {
                        touchX += intervalsX;
                        touchY += intervalsY;

                        reportOnMoved();
                        invalidate();

                        if ((moveListener != null) && (j == numberOfFrames - 1)) {
                            moveListener.OnReturnedToCenter();
                        }
                    }
                },
                    i * 40
                );
            }

            if (moveListener != null) {
                moveListener.OnReleased();
            }
        }
    }

    public void setTouchOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
    }
}
