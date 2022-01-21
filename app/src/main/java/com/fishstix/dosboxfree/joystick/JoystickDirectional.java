package com.fishstix.dosboxfree.joystick;

import android.graphics.Paint;

public class JoystickDirectional {
    private final Paint backgroudPaint;
    private final Paint handlePaint;
    private int backgroundPosition;

    public JoystickDirectional() {
        backgroudPaint = JoystickHelper.createPaint(0xA0888888);
        handlePaint = JoystickHelper.createPaint(0xB0444444);
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
}
