package com.fishstix.dosboxfree.joystick;

import android.graphics.Canvas;
import android.view.View;

import com.fishstix.dosboxfree.DosBoxControl;

public class JoystickKeypad {
    private static final double DISTANCE_BUTTON_RATIO = 2.75;
    private static final double BUTTON_RADIUS_RATIO = 0.125;
    private static final double DISTANTE_ACTION_BUTTONS_RATIO = 7;
    private static final int COLOR_A_BUTTON = 0x5FFF8888;
    private static final int COLOR_B_BUTTON = 0x5F88FF88;
    private static final int COLOR_C_BUTTON = 0x5F8888FF;
    private static final int COLOR_X_BUTTON = 0x5FFFFF88;
    private static final int COLOR_Y_BUTTON = 0x5FFF88FF;
    private static final int COLOR_Z_BUTTON = 0x5F88FFFF;
    private static final int COLOR_START_BUTTON = 0x5FDD8833;
    private static final int COLOR_F1_BUTTON = 0x5FDDDDDD;
    private static final int COLOR_PAUSE_BUTTON = 0x5FDD3388;
    private static final int COLOR_ESCAPE_BUTTON = 0x5F88DD33;
    private static final String LABEL_A_BUTTON = "A";
    private static final String LABEL_B_BUTTON = "B";
    private static final String LABEL_C_BUTTON = "C";
    private static final String LABEL_X_BUTTON = "X";
    private static final String LABEL_Y_BUTTON = "Y";
    private static final String LABEL_Z_BUTTON = "Z";
    private static final String LABEL_START_BUTTON = "S";
    private static final String LABEL_F1_BUTTON = "F1";
    private static final String LABEL_PAUSE_BUTTON = "P";
    private static final String LABEL_ESCAPE_BUTTON = "E";
    private final JoystickButton[] buttons;
    private final JoystickButton[] specialButtons;
    private final JoystickButton[][] actionButtons;
    private final View view;

    public JoystickKeypad(final View currentView) {
        view = currentView;
        JoystickButton buttonA = new JoystickButton(
            COLOR_A_BUTTON,
            DosBoxControl.KEYCODE_A_BUTTON,
            LABEL_A_BUTTON
        );
        JoystickButton buttonB = new JoystickButton(
            COLOR_B_BUTTON,
            DosBoxControl.KEYCODE_B_BUTTON,
            LABEL_B_BUTTON
        );
        JoystickButton buttonC = new JoystickButton(
            COLOR_C_BUTTON,
            DosBoxControl.KEYCODE_C_BUTTON,
            LABEL_C_BUTTON
        );
        JoystickButton buttonX = new JoystickButton(
            COLOR_X_BUTTON,
            DosBoxControl.KEYCODE_X_BUTTON,
            LABEL_X_BUTTON
        );
        JoystickButton buttonY = new JoystickButton(
            COLOR_Y_BUTTON,
            DosBoxControl.KEYCODE_Y_BUTTON,
            LABEL_Y_BUTTON
        );
        JoystickButton buttonZ = new JoystickButton(
            COLOR_Z_BUTTON,
            DosBoxControl.KEYCODE_Z_BUTTON,
            LABEL_Z_BUTTON
        );
        JoystickButton buttonStart = new JoystickButton(
            COLOR_START_BUTTON,
            DosBoxControl.KEYCODE_START_BUTTON,
            LABEL_START_BUTTON
        );
        JoystickButton buttonF1 = new JoystickButton(
            COLOR_F1_BUTTON,
            DosBoxControl.KEYCODE_F1_BUTTON,
            LABEL_F1_BUTTON
        );
        JoystickButton buttonPause = new JoystickButton(
            COLOR_PAUSE_BUTTON,
            DosBoxControl.KEYCODE_PAUSE_BUTTON,
            LABEL_PAUSE_BUTTON
        );
        JoystickButton buttonEscape = new JoystickButton(
            COLOR_ESCAPE_BUTTON,
            DosBoxControl.KEYCODE_ESCAPE_BUTTON,
            LABEL_ESCAPE_BUTTON
        );

        buttons = new JoystickButton[] {
            buttonStart,
            buttonX,
            buttonY,
            buttonZ,
            buttonF1,
            buttonA,
            buttonB,
            buttonC,
            buttonPause,
            buttonEscape
        };
        specialButtons = new JoystickButton[] {
            buttonF1, buttonStart, buttonPause, buttonEscape
        };
        actionButtons = new JoystickButton[][] {
            {buttonX, buttonY, buttonZ},
            {buttonA, buttonB, buttonC}
        };
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
        int specialCenter = (screenWidth / 2) + buttonRadius;

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
