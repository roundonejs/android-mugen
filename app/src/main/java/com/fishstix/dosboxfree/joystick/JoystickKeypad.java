package com.fishstix.dosboxfree.joystick;

import android.graphics.Canvas;
import android.view.View;

public class JoystickKeypad {
    private static final double DISTANCE_BUTTON_RATIO = 2.75;
    private static final double BUTTON_RADIUS_RATIO = 0.125;
    private static final double DISTANTE_ACTION_BUTTONS_RATIO = 7;
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
    private final JoystickButton[] specialButtons;
    private final JoystickButton[][] actionButtons;
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
        specialButtons = new JoystickButton[] {buttonF1, buttonStart};
        actionButtons = new JoystickButton[][] {
            {buttonX, buttonY, buttonZ},
            {buttonA, buttonB, buttonC}
        };
    }

    public void setAlpha(final int alpha) {
        for (JoystickButton button : buttons) {
            button.setAlpha(alpha);
        }
    }

    public void setSize(final int sizeView, final int screenWidth) {
        int buttonRadius = (int) (sizeView * BUTTON_RADIUS_RATIO);

        setSpecialButtonsPosition(screenWidth, buttonRadius);
        setActionButtonsPosition(sizeView, screenWidth, buttonRadius);
    }

    private void setSpecialButtonsPosition(
        final int screenWidth,
        final int buttonRadius
    ) {
        int specialCenter = (screenWidth / 2) - buttonRadius;

        for (int i = 0, length = specialButtons.length; i < length; i++) {
            JoystickButton button = specialButtons[i];
            button.setPosition(specialCenter, buttonRadius);
            button.setRadius(buttonRadius);

            specialCenter += (int) (buttonRadius * DISTANCE_BUTTON_RATIO);
        }
    }

    private void setActionButtonsPosition(
        final int sizeView,
        final int screenWidth,
        final int buttonRadius
    ) {
        int centerViewPosition = sizeView / 2;
        int distanceButtons = (int) (buttonRadius * DISTANCE_BUTTON_RATIO);
        int[] centerYButtons = new int[] {
            centerViewPosition,
            centerViewPosition + distanceButtons
        };

        for (int i = 0, length = actionButtons.length; i < length; i++) {
            int centerXButton = (
                screenWidth
                - (int) (buttonRadius * DISTANTE_ACTION_BUTTONS_RATIO)
            );

            for (JoystickButton button : actionButtons[i]) {
                button.setPosition(centerXButton, centerYButtons[i]);
                button.setRadius(buttonRadius);

                centerXButton += distanceButtons;
            }
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
