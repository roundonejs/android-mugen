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

package com.fishstix.dosboxfree;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.input.JoystickHandleListener;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

public class DBGLSurfaceView extends GLSurfaceView implements SurfaceHolder.
    Callback {
    private final static int DEFAULT_WIDTH = 512;
    private final static int DEFAULT_HEIGHT = 512;
    private final static int BUTTON_REPEAT_DELAY = 100;
    private final static int EVENT_THRESHOLD_DECAY = 100;

    private DBMain mParent = null;
    private boolean mSurfaceViewRunning = false;
    public DosBoxVideoThread mVideoThread = null;
    public KeyHandler mKeyHandler = null;

    boolean mScale = false;
    boolean mInputLowLatency = false;
    public boolean mGPURendering = false;

    Bitmap mBitmap = null;
    private Paint mBitmapPaint = null;

    int mSrc_width = 0;
    int mSrc_height = 0;
    final AtomicBoolean bDirtyCoords = new AtomicBoolean(false);
    private int mScroll_x = 0;
    private int mScroll_y = 0;

    final AtomicBoolean mDirty = new AtomicBoolean(false);
    int mStartLine = 0;
    int mEndLine = 0;

    public OpenGLRenderer mRenderer;
    private Map<Integer, Integer> keyEventToMugenButton;

    class DosBoxVideoThread extends Thread {
        private static final int UPDATE_INTERVAL = 40;
        private static final int UPDATE_INTERVAL_MIN = 20;
        private static final int RESET_INTERVAL = 100;

        private boolean mVideoRunning = false;

        private long startTime = 0;
        private int frameCount = 0;
        private long curTime, nextUpdateTime, sleepTime;

        void setRunning(boolean running) {
            mVideoRunning = running;
        }

        public void run() {
            mVideoRunning = true;

            while (mVideoRunning) {
                if (mSurfaceViewRunning) {

                    curTime = System.currentTimeMillis();

                    if (frameCount > RESET_INTERVAL) {
                        frameCount = 0;
                    }

                    if (frameCount == 0) {
                        startTime = curTime - UPDATE_INTERVAL;
                    }

                    frameCount++;

                    synchronized (mDirty) {
                        if (mDirty.get()) {
                            if (bDirtyCoords.get()) {
                                calcScreenCoordinates(mSrc_width, mSrc_height);
                            }

                            videoRedraw(
                                mBitmap,
                                mSrc_width,
                                mSrc_height,
                                mStartLine,
                                mEndLine
                            );
                            mDirty.set(false);
                        }
                    }

                    try {
                        nextUpdateTime = startTime + (frameCount + 1) *
                            UPDATE_INTERVAL;
                        sleepTime = nextUpdateTime - System.currentTimeMillis();
                        Thread.sleep(Math.max(sleepTime, UPDATE_INTERVAL_MIN));
                    } catch (InterruptedException e) { }
                } else {
                    try {
                        frameCount = 0;
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { }
                }
            }
        }

        private void calcScreenCoordinates(
            final int src_width,
            final int src_height
        ) {
            if ((src_width <= 0) || (src_height <= 0)) {
                return;
            }

            mRenderer.width = getWidth();
            mRenderer.height = getHeight();

            boolean isLandscape = (mRenderer.width > mRenderer.height);

            if (mScale) {
                mRenderer.x = src_width * mRenderer.height / src_height;

                if (mRenderer.x < mRenderer.width) {
                    mRenderer.width = mRenderer.x;
                } else if (mRenderer.x > mRenderer.width) {
                    mRenderer.height = src_height * mRenderer.width / src_width;
                }

                mRenderer.x = (getWidth() - mRenderer.width) / 2;

                if (isLandscape) {
                    mRenderer.width *= (mParent.mPrefScaleFactor * 0.01f);
                    mRenderer.height *= (mParent.mPrefScaleFactor * 0.01f);
                    mRenderer.x = (getWidth() - mRenderer.width) / 2;

                    mRenderer.y = (getHeight() - mRenderer.height) / 2;
                } else {
                    mRenderer.y = 0;
                }

                // no power of two extenstion
                mRenderer.mCropWorkspace[0] = 0;
                mRenderer.mCropWorkspace[1] = src_height;
                mRenderer.mCropWorkspace[2] = src_width;
                mRenderer.mCropWorkspace[3] = -src_height;
            } else {
                if ((mScroll_x + src_width) < mRenderer.width) {
                    mScroll_x = mRenderer.width - src_width;
                }

                if ((mScroll_y + src_height) < mRenderer.height) {
                    mScroll_y = mRenderer.height - src_height;
                }

                mScroll_x = Math.min(mScroll_x, 0);
                mScroll_y = Math.min(mScroll_y, 0);
                mRenderer.mCropWorkspace[0] = -mScroll_x; // left
                mRenderer.mCropWorkspace[1] = Math.min(
                    mRenderer.height - mScroll_y,
                    src_height
                    ) + mScroll_y; // bottom - top
                mRenderer.mCropWorkspace[2] = Math.min(
                    mRenderer.width - mScroll_x,
                    src_width
                ); // right
                mRenderer.mCropWorkspace[3] = -mRenderer.mCropWorkspace[1]; // -(bottom - top)
                mRenderer.width = mRenderer.mCropWorkspace[2] -
                    mRenderer.mCropWorkspace[0];
                mRenderer.height =
                    (Math.max(
                        -mScroll_y,
                        0
                    ) + mScroll_y + mRenderer.mCropWorkspace[1]) -
                    (Math.max(-mScroll_y, 0) + mScroll_y);

                mRenderer.x = (getWidth() - mRenderer.width) / 2;
                mRenderer.y = 0;
            }

            bDirtyCoords.set(false);
            mRenderer.filter_on = mParent.mPrefScaleFilterOn;
        }
    }

    public DBGLSurfaceView(final Context context) {
        super(context);
        mDirty.set(false);

        if (!this.isInEditMode()) {
            setup(context);
        }

        init();
    }

    public DBGLSurfaceView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        if (!this.isInEditMode()) {
            setup(context);
        }

        init();
    }

    public DBGLSurfaceView(
        final Context context,
        final AttributeSet attrs,
        final int defStyle
    ) {
        super(context, attrs);

        if (!this.isInEditMode()) {
            setup(context);
        }

        init();
    }

    private void init() {
        keyEventToMugenButton = new HashMap<>();
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_DPAD_UP,
            DosBoxControl.KEYCODE_UP_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_DPAD_RIGHT,
            DosBoxControl.KEYCODE_RIGHT_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_DPAD_DOWN,
            DosBoxControl.KEYCODE_DOWN_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_DPAD_LEFT,
            DosBoxControl.KEYCODE_LEFT_BUTTON
        );

        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_X,
            DosBoxControl.KEYCODE_X_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_Y,
            DosBoxControl.KEYCODE_Y_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_L1,
            DosBoxControl.KEYCODE_Y_BUTTON
        );

        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_A,
            DosBoxControl.KEYCODE_A_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_B,
            DosBoxControl.KEYCODE_B_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_R1,
            DosBoxControl.KEYCODE_C_BUTTON
        );

        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_START,
            DosBoxControl.KEYCODE_START_BUTTON
        );
        keyEventToMugenButton.put(
            KeyEvent.KEYCODE_BUTTON_SELECT,
            DosBoxControl.KEYCODE_F1_BUTTON
        );
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setup(final Context context) {
        mParent = (DBMain) context;

        mBitmapPaint = new Paint();
        mBitmapPaint.setFilterBitmap(true);

        mBitmap = Bitmap.createBitmap(
            DEFAULT_WIDTH,
            DEFAULT_HEIGHT,
            Bitmap.Config.RGB_565
        );

        mRenderer = new OpenGLRenderer(mParent);
        mRenderer.setBitmap(mBitmap);
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        if (mGPURendering) {
            requestRender();
        }

        mVideoThread = new DosBoxVideoThread();
        mKeyHandler = new KeyHandler(this);

        requestFocus();
        setFocusableInTouchMode(true);
        setFocusable(true);
        requestFocus();
        requestFocusFromTouch();

        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGB_565);
        getHolder().setKeepScreenOn(true);

        if (Build.VERSION.SDK_INT >= 14) {
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            setOnSystemUiVisibilityChangeListener(
                new MySystemUiVisibilityChangeListener()
            );
        } else if (Build.VERSION.SDK_INT >= 11) {
            setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
            setOnSystemUiVisibilityChangeListener(
                new MySystemUiVisibilityChangeListener()
            );
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private class MySystemUiVisibilityChangeListener implements View.
        OnSystemUiVisibilityChangeListener {
        @Override
        public void onSystemUiVisibilityChange(final int visibility) {
            Timer timer = new Timer();
            timer.schedule(
                new TimerTask() {
                @Override
                public void run() {
                    mParent.runOnUiThread(
                        new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 14) {
                                setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                                );
                            } else if (Build.VERSION.SDK_INT >= 11) {
                                setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
                            }
                        }
                    }
                    );
                }
            },
                6000
            );
        }
    }

    public void shutDown() {
        mBitmap = null;
        mVideoThread = null;
        mKeyHandler = null;
    }

    private Rect mSrcRect = new Rect();
    private Rect mDstRect = new Rect();
    private Rect mDirtyRect = new Rect();
    private int mDirtyCount = 0;

    private void canvasDraw(
        final Bitmap bitmap,
        final int src_width,
        final int src_height,
        final int startLine,
        final int endLine
    ) {
        SurfaceHolder surfaceHolder = getHolder();
        Surface surface = surfaceHolder.getSurface();
        Canvas canvas = null;

        try {
            synchronized (surfaceHolder) {
                boolean isDirty = false;
                int newStartLine;
                int newEndLine;

                if (mDirtyCount < 3) {
                    mDirtyCount++;
                    isDirty = true;
                    newStartLine = 0;
                    newEndLine = src_height;
                } else {
                    newStartLine = startLine;
                    newEndLine = endLine;
                }

                if (mScale) {
                    mDstRect.set(0, 0, mRenderer.width, mRenderer.height);
                    mSrcRect.set(0, 0, src_width, src_height);
                    mDstRect.offset(mRenderer.x, mRenderer.y);

                    mDirtyRect.set(
                        0,
                        newStartLine * mRenderer.height / src_height,
                        mRenderer.width,
                        newEndLine * mRenderer.height / src_height + 1
                    );

                    // locnet, 2011-04-21, a strip on right side not updated
                    mDirtyRect.offset(mRenderer.x, mRenderer.y);
                } else {
                    // L,T,R,B
                    mSrcRect.set(
                        -mScroll_x,
                        Math.max(-mScroll_y, newStartLine),
                        mRenderer.mCropWorkspace[2],
                        Math.min(
                            Math.min(getHeight() - mScroll_y, src_height),
                            newEndLine
                        )
                    );
                    mDstRect.set(
                        0,
                        mSrcRect.top + mScroll_y,
                        mSrcRect.width(),
                        mSrcRect.top + mScroll_y + mSrcRect.height()
                    );

                    mDstRect.offset((getWidth() - mSrcRect.width()) / 2, 0);

                    mDirtyRect.set(mDstRect);
                }

                if ((surface != null) && surface.isValid()) {
                    if (isDirty) {
                        canvas = surfaceHolder.lockCanvas(null);
                        canvas.drawColor(0xff000000);
                    } else {
                        canvas = surfaceHolder.lockCanvas(mDirtyRect);
                    }

                    if (mScale) {
                        canvas.drawBitmap(
                            bitmap,
                            mSrcRect,
                            mDstRect,
                            (mParent.mPrefScaleFilterOn) ? mBitmapPaint : null
                        );
                    } else {
                        canvas.drawBitmap(bitmap, mSrcRect, mDstRect, null);
                    }
                }
            }
        } finally {
            if ((canvas != null) && (surface != null) && surface.isValid()) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        surfaceHolder = null;
    }

    private TouchEventWrapper mWrap = TouchEventWrapper.newInstance();

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private void processJoystickInput(
        final MotionEvent event,
        final int historyPos
    ) {
        int percentagePositionX = getJoystickPercentagePosition(
            event,
            historyPos,
            MotionEvent.AXIS_X
        );
        int percentagePositionY = getJoystickPercentagePosition(
            event,
            historyPos,
            MotionEvent.AXIS_Y
        );

        if ((percentagePositionX == 0) && (percentagePositionY == 0)) {
            percentagePositionX = getJoystickPercentagePosition(
                event,
                historyPos,
                MotionEvent.AXIS_HAT_X
            );
            percentagePositionY = getJoystickPercentagePosition(
                event,
                historyPos,
                MotionEvent.AXIS_HAT_Y
            );
        }

        JoystickHandleListener.onMoved(
            percentagePositionX,
            -percentagePositionY
        );
    }

    private int getJoystickPercentagePosition(
        final MotionEvent event,
        final int historyPos,
        final int axisCode
    ) {
        InputDevice.MotionRange range = event.getDevice().getMotionRange(
            axisCode,
            event.getSource()
        );

        if (range != null) {
            float axisValue;

            if (historyPos >= 0) {
                axisValue = event.getHistoricalAxisValue(axisCode, historyPos);
            } else {
                axisValue = event.getAxisValue(axisCode);
            }

            return (int) (InputDeviceState.ProcessAxis(range, axisValue) * 100);
        }

        return 0;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    public boolean onGenericMotionEvent(final MotionEvent event) {
        if (
            event.getEventTime() + EVENT_THRESHOLD_DECAY <
            SystemClock.uptimeMillis()
        ) {
            return true;                // get rid of old events
        }

        if (
            (MotionEventCompat.getActionMasked(event) ==
            MotionEvent.ACTION_MOVE) &&
            ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) ==
            TouchEventWrapper.SOURCE_CLASS_JOYSTICK)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                // use new 3.1 API to handle joystick movements
                int historySize = event.getHistorySize();

                for (int i = 0; i < historySize; i++) {
                    processJoystickInput(event, i);
                }

                processJoystickInput(event, -1);

                return true;
            } else {
                // use older 2.2+ API to handle joystick movements
                int pointerIndex = MotionEventCompat.getActionIndex(event);
                int pointerId = MotionEventCompat.getPointerId(
                    event,
                    pointerIndex
                );

                float x = mWrap.getX(event, pointerId);
                float y = mWrap.getY(event, pointerId);

                int percentagePositionX = (int) (x * 100);
                int percentagePositionY = (int) (y * 100);
                JoystickHandleListener.onMoved(
                    percentagePositionX,
                    -percentagePositionY
                );

                return true;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return super.onGenericMotionEvent(event);
        }

        return false;
    }

    // Map of Custom Maps
    public SparseIntArray customMap = new SparseIntArray(
        DosBoxPreferences.NUM_USB_MAPPINGS
    );

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        return handleMugenKey(keyCode, true) || handleKey(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        return handleMugenKey(keyCode, false) || handleKey(keyCode, event);
    }

    private boolean handleMugenKey(final int keyCode, final boolean down) {
        if (keyEventToMugenButton.containsKey(keyCode)) {
            DosBoxControl.sendNativeKey(
                keyEventToMugenButton.get(keyCode),
                down,
                false,
                false,
                false
            );

            return true;
        }

        return false;
    }

    private boolean handleKey(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                DBMenuSystem.doConfirmQuit(mParent);
            }

            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false;
        }

        boolean down = (event.getAction() == KeyEvent.ACTION_DOWN);

        if (!down || (event.getRepeatCount() == 0)) {
            int unicode = event.getUnicodeChar();

            // filter system generated keys, but not hardware keypresses
            if (
                (event.isAltPressed() || event.isShiftPressed())
                && (unicode == 0)
                && ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) == 0)
            ) {
                return false;
            }

            if ((keyCode > 255) || (unicode > 255)) {
                // unknown keys
                return false;
            }

            int newKeyCode = keyCode | (unicode << 8);

            long diff = event.getEventTime() - event.getDownTime();

            if (!down && (diff < 50)) {
                mKeyHandler.removeMessages(newKeyCode);
                mKeyHandler.sendEmptyMessageDelayed(
                    newKeyCode,
                    BUTTON_REPEAT_DELAY - diff
                );
            } else if (!(down && mKeyHandler.hasMessages(newKeyCode))) {
                return DosBoxControl.sendNativeKey(
                    newKeyCode,
                    down,
                    false,
                    false,
                    false
                );
            }
        }

        return false;
    }

    public void setDirty() {
        mDirtyCount = 0;
        bDirtyCoords.set(true);
        mDirty.set(true);
    }

    public void resetScreen(boolean redraw) {
        setDirty();
        mScroll_x = 0;
        mScroll_y = 0;

        if (redraw) {
            forceRedraw();
        }
    }

    public void forceRedraw() {
        setDirty();
        videoRedraw(mBitmap, mSrc_width, mSrc_height, 0, mSrc_height);
    }

    private void videoRedraw(
        final Bitmap bitmap,
        final int src_width,
        final int src_height,
        final int startLine,
        final int endLine
    ) {
        if (
            !mSurfaceViewRunning || (bitmap == null) || (src_width <= 0) ||
            (src_height <= 0)
        ) {
            return;
        }

        if (mGPURendering) {
            mRenderer.setBitmap(bitmap);
            requestRender();
        } else {
            canvasDraw(bitmap, src_width, src_height, startLine, endLine);
        }
    }

    public void surfaceChanged(
        SurfaceHolder holder,
        int format,
        int width,
        int height
    ) {
        resetScreen(true);

        if (mGPURendering) {
            super.surfaceChanged(holder, format, width, height);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceViewRunning = true;

        if (mGPURendering) {
            super.surfaceCreated(holder);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceViewRunning = false;

        if (mGPURendering) {
            super.surfaceDestroyed(holder);
        }
    }

    // Fix for Motorola Keyboards!!! - fishstix
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new BaseInputConnection(this, false) {
                   @Override
                   public boolean deleteSurroundingText(
                       int beforeLength,
                       int afterLength
                   ) {
                       // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
                       if ((beforeLength == 1) && (afterLength == 0)) {
                           // backspace
                           super.sendKeyEvent(
                               new KeyEvent(
                                   KeyEvent.ACTION_DOWN,
                                   KeyEvent.KEYCODE_DEL
                               )
                           );

                           return super.sendKeyEvent(
                               new KeyEvent(
                                   KeyEvent.ACTION_UP,
                                   KeyEvent.KEYCODE_DEL
                               )
                           );
                       }

                       return super.deleteSurroundingText(
                           beforeLength,
                           afterLength
                       );
                   }
        };
    }
}
