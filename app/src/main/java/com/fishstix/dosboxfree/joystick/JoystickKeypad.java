package com.fishstix.dosboxfree.joystick;

import android.graphics.Canvas;
import android.view.View;

public class JoystickKeypad {
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
    private static final String LABEL_A_BUTTON = "A";
    private static final String LABEL_B_BUTTON = "B";
    private static final String LABEL_C_BUTTON = "C";
    private static final String LABEL_X_BUTTON = "X";
    private static final String LABEL_Y_BUTTON = "Y";
    private static final String LABEL_Z_BUTTON = "Z";
    private static final String LABEL_START_BUTTON = "S";
    private static final String LABEL_F1_BUTTON = "F1";
    private final JoystickButton[] buttons;
    private final View view;

    public JoystickKeypad(final View currentView) {
        view = currentView;
        JoystickButton buttonA = new JoystickButton(
            COLOR_A_BUTTON,
            KEYCODE_A_BUTTON,
            LABEL_A_BUTTON
        );
        JoystickButton buttonB = new JoystickButton(
            COLOR_B_BUTTON,
            KEYCODE_B_BUTTON,
            LABEL_B_BUTTON
        );
        JoystickButton buttonC = new JoystickButton(
            COLOR_C_BUTTON,
            KEYCODE_C_BUTTON,
            LABEL_C_BUTTON
        );
        JoystickButton buttonX = new JoystickButton(
            COLOR_X_BUTTON,
            KEYCODE_X_BUTTON,
            LABEL_X_BUTTON
        );
        JoystickButton buttonY = new JoystickButton(
            COLOR_Y_BUTTON,
            KEYCODE_Y_BUTTON,
            LABEL_Y_BUTTON
        );
        JoystickButton buttonZ = new JoystickButton(
            COLOR_Z_BUTTON,
            KEYCODE_Z_BUTTON,
            LABEL_Z_BUTTON
        );
        JoystickButton buttonStart = new JoystickButton(
            COLOR_START_BUTTON,
            KEYCODE_START_BUTTON,
            LABEL_START_BUTTON
        );
        JoystickButton buttonF1 = new JoystickButton(
            COLOR_F1_BUTTON,
            KEYCODE_F1_BUTTON,
            LABEL_F1_BUTTON
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

    public void setAlpha(final int alpha) {
        for (JoystickButton button : buttons) {
            button.setAlpha(alpha);
        }
    }

    public void setSize(final int sizeView, final int screenWidth) {
        int centerViewPosition = sizeView / 2;
        int buttonRadius = (int) (sizeView * 0.125);
        int centerXButton = screenWidth - (int) (buttonRadius * 9.3);
        int[] centerYButtons = new int[2];
        centerYButtons[0] = centerViewPosition - (int) (buttonRadius * 1.5);
        centerYButtons[1] = centerViewPosition + (int) (buttonRadius * 1.5);

        for (int i = 0, length = buttons.length / 2; i < length; i++) {
            for (int j = 0; j < 2; j++) {
                JoystickButton button = buttons[(j * length) + i];
                button.setPosition(centerXButton, centerYButtons[j]);
                button.setRadius(buttonRadius);
            }

            centerXButton += (int) (buttonRadius * 2.75);
        }
    }

    public void draw(final Canvas canvas) {
        for (JoystickButton button : buttons) {
            button.draw(canvas);
        }
    }

    public boolean release(final int pointerId) {
        for (JoystickButton button : buttons) {
            if (releaseButton(button, pointerId)) {
                return true;
            }
        }

        return false;
    }

    private boolean releaseButton(
        final JoystickButton button,
        final int pointerId
    ) {
        if ((pointerId == button.getPointerId()) && button.isClicked()) {
            view.invalidate();
            button.release();

            return true;
        }

        return false;
    }

    public boolean click(final int pointerId, final int x, final int y) {
        for (JoystickButton button : buttons) {
            if (clickButton(button, pointerId, x, y)) {
                return true;
            }
        }

        return false;
    }

    private boolean clickButton(
        final JoystickButton button,
        final int pointerId,
        final int x,
        final int y
    ) {
        if (button.inButton(x, y) && !button.isClicked()) {
            view.invalidate();
            button.click(pointerId);

            return true;
        }

        return false;
    }
}
