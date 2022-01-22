package com.fishstix.dosboxfree.joystick;

import android.graphics.Paint;

public class JoystickDirectional {
    private static final int INNER_PADDING = 10;
    private final Paint backgroudPaint;
    private final Paint handlePaint;
    private int backgroundPosition;
    private int pointerId;

    public JoystickDirectional() {
        backgroudPaint = JoystickHelper.createPaint(0xA0888888);
        handlePaint = JoystickHelper.createPaint(0xB0444444);
        pointerId = JoystickHelper.INVALID_POINTER_ID;
    }

    public Paint getBackground() {
        return backgroudPaint;
    }

    public Paint getHandle() {
        return handlePaint;
    }

    public void setAlpha(final int alpha) {
        backgroudPaint.setAlpha(alpha);
        handlePaint.setAlpha(alpha);
    }

    public void setBackgroundPosition(final int position) {
        backgroundPosition = position;
    }

    public int getBackgroundPosition() {
        return backgroundPosition;
    }

    public int getBackgroundRadius() {
        return backgroundPosition - INNER_PADDING;
    }

    public int getHandleRadius() {
        return backgroundPosition / 2;
    }

    public void click(final int newPointerId) {
        pointerId = newPointerId;
    }

    public int getPointerId() {
        return pointerId;
    }

    public boolean isClicked() {
        return pointerId != JoystickHelper.INVALID_POINTER_ID;
    }

    public void release() {
        pointerId = JoystickHelper.INVALID_POINTER_ID;
    }
}