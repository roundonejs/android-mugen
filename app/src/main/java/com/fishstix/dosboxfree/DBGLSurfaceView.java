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
import android.content.res.Configuration;
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
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
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
import android.widget.Toast;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.input.JoystickHandleListener;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

public class DBGLSurfaceView extends GLSurfaceView implements SurfaceHolder.
    Callback {
    private final static int DEFAULT_WIDTH = 512;    // 800;
    private final static int DEFAULT_HEIGHT = 512;    // 600;

    private final static int BUTTON_REPEAT_DELAY = 100;
    private final static int EVENT_THRESHOLD_DECAY = 100;
    private final static int DEADZONE = 8;

    public final static int INPUT_MODE_MOUSE = 0xf1;
    public final static int INPUT_MODE_SCROLL = 0xf2;
    public final static int INPUT_MODE_JOYSTICK = 0xf3;
    public final static int INPUT_MODE_REAL_MOUSE = 0xf4;
    public final static int INPUT_MODE_REAL_JOYSTICK = 0xf5;

    public final static int PIXEL_BYTES = 2;

    private static final int MAX_POINT_CNT = 4;

    private DBMain mParent = null;
    private boolean mSurfaceViewRunning = false;
    public DosBoxVideoThread mVideoThread = null;
    public KeyHandler mKeyHandler = null;
    private GestureDetector gestureScanner;

    boolean mScale = false;
    int mInputMode = INPUT_MODE_MOUSE;
    boolean mEnableDpad = false;
    boolean mAbsolute = true;
    boolean mInputLowLatency = false;
    boolean mUseLeftAltOn = false;
    public boolean mLongPress = true;
    public boolean mDebug = false;
    private static final int CLICK_DELAY = 125;         // in ms
    private boolean mDoubleLong = false;
    public boolean mScreenTop = false;
    public boolean mGPURendering = false;
    public boolean mKeyboardVisible = false;

    int mDpadRate = 7;
    private boolean mLongClick = false;
    // boolean mCalibrate = false;
    boolean mMaintainAspect = true;
    // private boolean mHasMoved = false;
    private boolean mSPenButton = false;

    int mContextMenu = 0;

    Bitmap mBitmap = null;
    private Paint mBitmapPaint = null;
    private Paint mTextPaint = null;

    int mSrc_width = 0;
    int mSrc_height = 0;
    final AtomicBoolean bDirtyCoords = new AtomicBoolean(false);
    private int mScroll_x = 0;
    private int mScroll_y = 0;

    final AtomicBoolean mDirty = new AtomicBoolean(false);
    boolean isLandscape = false;
    int mStartLine = 0;
    int mEndLine = 0;
    boolean mFilterLongClick = false;

    boolean mModifierCtrl = false;
    boolean mModifierAlt = false;
    boolean mModifierShift = false;
    public int mActionBarHeight;
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
                                calcScreenCoordinates(
                                    mSrc_width,
                                    mSrc_height,
                                    mStartLine,
                                    mEndLine
                                );
                            }

                            VideoRedraw(
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
    private void setup(Context context) {
        mParent = (DBMain) context;
        // setRenderMode(RENDERMODE_WHEN_DIRTY);

        gestureScanner = new GestureDetector(context, new MyGestureDetector());
        mBitmapPaint = new Paint();
        mBitmapPaint.setFilterBitmap(true);
        mTextPaint = new Paint();
        mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setSubpixelText(false);

        mBitmap = Bitmap.createBitmap(
            DEFAULT_WIDTH,
            DEFAULT_HEIGHT,
            Bitmap.Config.RGB_565
        );

        // setEGLContextClientVersion(1);
        mRenderer = new OpenGLRenderer(mParent);
        mRenderer.setBitmap(mBitmap);
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        if (mGPURendering) {
            requestRender();
        }

        mVideoThread = new DosBoxVideoThread();
        mKeyHandler = new KeyHandler(this);

        // Receive keyboard events
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
        public void onSystemUiVisibilityChange(int visibility) {
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
                                    View.
                                    SYSTEM_UI_FLAG_LOW_PROFILE
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

    public void calcScreenCoordinates(
        int src_width,
        int src_height,
        int startLine,
        int endLine
    ) {
        if ((src_width <= 0) || (src_height <= 0)) {
            return;
        }

        mRenderer.width = getWidth();
        mRenderer.height = getHeight();

        isLandscape = (mRenderer.width > mRenderer.height);

        if (mScale) {
            if (!mMaintainAspect && isLandscape) {
                mRenderer.x = 0;
            } else {
                mRenderer.x = src_width * mRenderer.height / src_height;

                if (mRenderer.x < mRenderer.width) {
                    mRenderer.width = mRenderer.x;
                } else if (mRenderer.x > mRenderer.width) {
                    mRenderer.height = src_height * mRenderer.width / src_width;
                }

                mRenderer.x = (getWidth() - mRenderer.width) / 2;
            }

            if (isLandscape) {
                mRenderer.width *= (mParent.mPrefScaleFactor * 0.01f);
                mRenderer.height *= (mParent.mPrefScaleFactor * 0.01f);
                mRenderer.x = (getWidth() - mRenderer.width) / 2;

                if (!mScreenTop) {
                    mRenderer.y = (getHeight() - mRenderer.height) / 2;
                } else {
                    mRenderer.y = 0;
                }
            } else {
                // portrait
                mRenderer.y = mActionBarHeight;
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
            mRenderer.mCropWorkspace[0] = -mScroll_x;                     // left
            mRenderer.mCropWorkspace[1] = Math.min(
                mRenderer.height - mScroll_y,
                src_height
                ) + mScroll_y;                                                                                                // bottom - top
            mRenderer.mCropWorkspace[2] = Math.min(
                mRenderer.width - mScroll_x,
                src_width
            );                                                                                                  // right
            mRenderer.mCropWorkspace[3] = -mRenderer.mCropWorkspace[1];                     // -(bottom - top)
            mRenderer.width = mRenderer.mCropWorkspace[2] -
                mRenderer.mCropWorkspace[0];                                                      // Math.min(mRenderer.width - mScroll_x, src_width) + mScroll_x;
            mRenderer.height =
                (Math.max(
                    -mScroll_y,
                    0
                ) + mScroll_y + mRenderer.mCropWorkspace[1]) -
                (Math.max(-mScroll_y, 0) + mScroll_y);

            if (isLandscape) {
                mRenderer.x = (getWidth() - mRenderer.width) / 2;
                mRenderer.y = 0;
            } else {
                mRenderer.x = (getWidth() - mRenderer.width) / 2;
                mRenderer.y = mActionBarHeight;
            }
        }

        bDirtyCoords.set(false);
        mRenderer.filter_on = mParent.mPrefScaleFilterOn;
    }
    private Rect mSrcRect = new Rect();
    private Rect mDstRect = new Rect();
    private Rect mDirtyRect = new Rect();
    private int mDirtyCount = 0;
    private void canvasDraw(
        Bitmap bitmap,
        int src_width,
        int src_height,
        int startLine,
        int endLine
    ) {
        SurfaceHolder surfaceHolder = getHolder();
        Surface surface = surfaceHolder.getSurface();
        Canvas canvas = null;

        try {
            synchronized (surfaceHolder)
            {

                boolean isDirty = false;

                if (mDirtyCount < 3) {
                    mDirtyCount++;
                    isDirty = true;
                    startLine = 0;
                    endLine = src_height;
                }

                if (mScale) {
                    mDstRect.set(0, 0, mRenderer.width, mRenderer.height);
                    mSrcRect.set(0, 0, src_width, src_height);
                    mDstRect.offset(mRenderer.x, mRenderer.y);

                    mDirtyRect.set(
                        0,
                        startLine * mRenderer.height / src_height,
                        mRenderer.width,
                        endLine * mRenderer.height / src_height + 1
                    );

                    // locnet, 2011-04-21, a strip on right side not updated
                    mDirtyRect.offset(mRenderer.x, mRenderer.y);
                } else {
                    // L,T,R,B
                    mSrcRect.set(
                        -mScroll_x,
                        Math.max(
                            -mScroll_y,
                            startLine
                        ),
                        mRenderer.mCropWorkspace[2],
                        Math.min(
                            Math.min(getHeight() - mScroll_y, src_height),
                            endLine
                        )
                    );
                    mDstRect.set(
                        0,
                        mSrcRect.top + mScroll_y,
                        mSrcRect.width(),
                        mSrcRect.top + mScroll_y + mSrcRect.height()
                    );

                    if (isLandscape) {
                        mDstRect.offset((getWidth() - mSrcRect.width()) / 2, 0);
                    } else {
                        mDstRect.offset(
                            (getWidth() - mSrcRect.width()) / 2,
                            mActionBarHeight
                        );
                    }

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

    public void VideoRedraw(
        Bitmap bitmap,
        int src_width,
        int src_height,
        int startLine,
        int endLine
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


    private int[] mButtonDown = new int[MAX_POINT_CNT];

    private final static int BTN_A = 0;
    private final static int BTN_B = 1;

    float[] x = new float[MAX_POINT_CNT];
    float[] y = new float[MAX_POINT_CNT];

    float[] x_last = new float[MAX_POINT_CNT];
    float[] y_last = new float[MAX_POINT_CNT];
    boolean[] virtButton = new boolean[MAX_POINT_CNT];

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
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (
            event.getEventTime() + EVENT_THRESHOLD_DECAY <
            SystemClock.uptimeMillis()
        ) {
            // Log.i("DosBoxTurbo","eventtime: "+event.getEventTime() + " systemtime: "+SystemClock.uptimeMillis());
            return true;                // get rid of old events
        }

        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final int pointerId =
            MotionEventCompat.getPointerId(event, pointerIndex);

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
                if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
                    x[pointerId] = mWrap.getX(event, pointerId);
                    y[pointerId] = mWrap.getY(event, pointerId);

                    int percentagePositionX = (int) x[pointerId] * 100;
                    int percentagePositionY = (int) y[pointerId] * 100;
                    JoystickHandleListener.onMoved(
                        percentagePositionX,
                        -percentagePositionY
                    );

                    return true;
                }
            }
        } else if (
            (MotionEventCompat.getActionMasked(event) ==
            MotionEventCompat.ACTION_HOVER_MOVE) &&
            ((mWrap.getSource(event) & TouchEventWrapper.SOURCE_CLASS_MASK) ==
            TouchEventWrapper.SOURCE_CLASS_POINTER)
        ) {
            if (mInputMode == INPUT_MODE_REAL_MOUSE) {
                x_last[pointerId] = x[pointerId];
                y_last[pointerId] = y[pointerId];
                x[pointerId] = mWrap.getX(event, pointerId);
                y[pointerId] = mWrap.getY(event, pointerId);

                if (mAbsolute) {
                    DosBoxControl.nativeMouseWarp(
                        x[pointerId],
                        y[pointerId],
                        mRenderer.x,
                        mRenderer.y,
                        mRenderer.width,
                        mRenderer.height
                    );
                } else {
                    DosBoxControl.nativeMouse(
                        (int) x[pointerId],
                        (int) y[pointerId],
                        (int) x_last[pointerId],
                        (int) y_last[pointerId],
                        2,
                        -1
                    );
                }

                int buttonState = mWrap.getButtonState(event);

                if (
                    ((buttonState & TouchEventWrapper.BUTTON_SECONDARY) != 0) &&
                    !mSPenButton
                ) {
                    // Handle Samsung SPen Button (RMB) - DOWN
                    DosBoxControl.nativeMouse(
                        0,
                        0,
                        0,
                        0,
                        DosBoxControl.ACTION_DOWN,
                        BTN_B
                    );
                    mSPenButton = true;
                } else if (
                    ((buttonState &
                    TouchEventWrapper.BUTTON_SECONDARY) == 0) && mSPenButton
                ) {
                    // Handle Samsung SPen Button (RMB) - UP
                    DosBoxControl.nativeMouse(
                        0,
                        0,
                        0,
                        0,
                        DosBoxControl.ACTION_UP,
                        BTN_B
                    );
                    mSPenButton = false;
                }

                try {
                    if (!mInputLowLatency) {
                        Thread.sleep(95);
                    } else {
                        Thread.sleep(65);
                    }
                } catch (InterruptedException e) { }

                return true;
            }
        } else if (
            MotionEventCompat.getActionMasked(event) ==
            MotionEventCompat.ACTION_HOVER_EXIT
        ) {
            if (mInputMode == INPUT_MODE_REAL_MOUSE) {
                // hover exit
                int buttonState = mWrap.getButtonState(event);

                if (
                    ((buttonState & TouchEventWrapper.BUTTON_SECONDARY) == 0) &&
                    mSPenButton
                ) {
                    // Handle Samsung SPen Button (RMB) - UP
                    DosBoxControl.nativeMouse(
                        0,
                        0,
                        0,
                        0,
                        DosBoxControl.ACTION_UP,
                        BTN_B
                    );
                    mSPenButton = false;

                    return true;
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return super.onGenericMotionEvent(event);
        } else {
            return false;
        }
    }


    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final int pointCnt = mWrap.getPointerCount(event);
        final int pointerId =
            MotionEventCompat.getPointerId(event, pointerIndex);

        if (pointCnt < MAX_POINT_CNT) {
            // if (pointerIndex <= MAX_POINT_CNT - 1){
            {
                for (int i = 0; i < pointCnt; i++) {
                    int id = MotionEventCompat.getPointerId(event, i);

                    if (id < MAX_POINT_CNT) {
                        x_last[id] = x[id];
                        y_last[id] = y[id];
                        x[id] = mWrap.getX(event, i);
                        y[id] = mWrap.getY(event, i);
                    }
                }

                switch (MotionEventCompat.getActionMasked(event)) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEventCompat.ACTION_POINTER_DOWN:
                        int button = -1;

                        // Save the ID of this pointer
                        if (
                            mInputMode ==
                            INPUT_MODE_MOUSE
                        ) { } else if (
                            mInputMode ==
                            INPUT_MODE_REAL_JOYSTICK
                        ) {
                            int buttonState = mWrap.getButtonState(event);

                            if (
                                (buttonState &
                                TouchEventWrapper.BUTTON_PRIMARY) != 0
                            ) {
                                button = BTN_A;
                            } else if (
                                (buttonState &
                                TouchEventWrapper.BUTTON_SECONDARY) != 0
                            ) {
                                button = BTN_B;
                            }

                            DosBoxControl.nativeJoystick(
                                0,
                                0,
                                DosBoxControl.ACTION_DOWN,
                                button
                            );
                        } else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
                            int buttonState = mWrap.getButtonState(event);

                            if (
                                (buttonState &
                                TouchEventWrapper.BUTTON_PRIMARY) != 0
                            ) {
                                button = BTN_A;
                            } else if (
                                (buttonState &
                                TouchEventWrapper.BUTTON_SECONDARY) != 0
                            ) {
                                button = BTN_B;
                            } else if (buttonState == 0) {
                                // handle trackpad presses as button clicks
                                button = BTN_A;
                            }

                            DosBoxControl.nativeMouse(
                                0,
                                0,
                                0,
                                0,
                                DosBoxControl.ACTION_DOWN,
                                button
                            );
                        }

                        mButtonDown[pointerId] = button;
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEventCompat.ACTION_POINTER_UP:

                        if (mInputMode == INPUT_MODE_MOUSE) {
                            if (mLongClick) {
                                // single tap long click release
                                DosBoxControl.nativeMouse(
                                    0,
                                    0,
                                    0,
                                    0,
                                    DosBoxControl.ACTION_UP,
                                    mGestureSingleClick -
                                    GESTURE_LEFT_CLICK
                                );
                                mLongClick = false;

                                return true;
                            } else if (mDoubleLong) {
                                // double tap long click release
                                try {
                                    Thread.sleep(CLICK_DELAY);
                                } catch (InterruptedException e) { }
                                DosBoxControl.nativeMouse(
                                    0,
                                    0,
                                    -1,
                                    -1,
                                    DosBoxControl.ACTION_UP,
                                    mGestureDoubleClick -
                                    GESTURE_LEFT_CLICK
                                );
                                mDoubleLong = false;
                                // return true;
                            } else if (pointCnt == 2) {
                                // handle 2 finger tap gesture
                                if (mLongPress) {
                                    if (!mTwoFingerAction) {
                                        // press button down
                                        DosBoxControl.nativeMouse(
                                            0,
                                            0,
                                            -1,
                                            -1,
                                            DosBoxControl.ACTION_DOWN,
                                            mGestureTwoFinger -
                                            GESTURE_LEFT_CLICK
                                        );
                                        mTwoFingerAction = true;
                                    } else {
                                        // already pressing button - release and press again
                                        DosBoxControl.nativeMouse(
                                            0,
                                            0,
                                            -1,
                                            -1,
                                            DosBoxControl.ACTION_UP,
                                            mGestureTwoFinger -
                                            GESTURE_LEFT_CLICK
                                        );
                                        try {
                                            Thread.sleep(CLICK_DELAY);
                                        } catch (InterruptedException e) { }
                                        DosBoxControl.nativeMouse(
                                            0,
                                            0,
                                            -1,
                                            -1,
                                            DosBoxControl.ACTION_DOWN,
                                            mGestureTwoFinger -
                                            GESTURE_LEFT_CLICK
                                        );
                                    }
                                } else {
                                    mouseClick(
                                        mGestureTwoFinger - GESTURE_LEFT_CLICK
                                    );
                                }

                                return true;
                            } else if ((pointCnt == 1) && mTwoFingerAction) {
                                // release two finger gesture
                                DosBoxControl.nativeMouse(
                                    0,
                                    0,
                                    -1,
                                    -1,
                                    DosBoxControl.ACTION_UP,
                                    mGestureTwoFinger -
                                    GESTURE_LEFT_CLICK
                                );
                                mTwoFingerAction = false;
                                // return true;
                            }
                        } else if (mInputMode == INPUT_MODE_REAL_MOUSE) {
                            // Log.v("Mouse","BUTTON UP: " + (mButtonDown[pointerId]));
                            DosBoxControl.nativeMouse(
                                0,
                                0,
                                0,
                                0,
                                DosBoxControl.ACTION_UP,
                                mButtonDown[pointerId]
                            );

                            if (mWrap.getButtonState(event) > 0) {
                                return true;                            // capture button touches, pass screen touches through to gesture detetor
                            }
                        } else if (mInputMode == INPUT_MODE_REAL_JOYSTICK) {
                            DosBoxControl.nativeJoystick(
                                0,
                                0,
                                DosBoxControl.ACTION_UP,
                                (mButtonDown[pointerId])
                            );

                            if (mWrap.getButtonState(event) > 0) {
                                return true;
                            }
                        }

                        break;
                    case MotionEvent.ACTION_MOVE:

                        // isTouch[pointerId] = true;
                        switch (mInputMode) {
                            case INPUT_MODE_SCROLL:
                                mScroll_x +=
                                    (int) (x[pointerId] - x_last[pointerId]);
                                mScroll_y +=
                                    (int) (y[pointerId] - y_last[pointerId]);
                                forceRedraw();
                                break;
                            case INPUT_MODE_MOUSE:
                            case INPUT_MODE_REAL_MOUSE:

                                if (
                                    event.getEventTime() +
                                    EVENT_THRESHOLD_DECAY <
                                    SystemClock.uptimeMillis()
                                ) {
                                    return true;                                // get rid of old events
                                }

                                int idx = (!virtButton[0]) ? 0 : 1;

                                if (mAbsolute) {
                                    DosBoxControl.nativeMouseWarp(
                                        x[idx],
                                        y[idx],
                                        mRenderer.x,
                                        mRenderer.y,
                                        mRenderer.width,
                                        mRenderer.height
                                    );
                                } else {
                                    DosBoxControl.nativeMouse(
                                        (int) x[idx],
                                        (int) y[idx],
                                        (int) x_last[idx],
                                        (int) y_last[idx],
                                        DosBoxControl.ACTION_MOVE,
                                        -1
                                    );
                                }

                                try {
                                    if (!mInputLowLatency) {
                                        Thread.sleep(95);
                                    } else {
                                        Thread.sleep(65);
                                    }
                                } catch (InterruptedException e) { }

                                break;
                            default:
                        }

                        break;
                }
            }
        }

        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Thread.yield();
        return gestureScanner.onTouchEvent(event);
    }

    private final static int MAP_EVENT_CONSUMED = -1;
    private final static int MAP_NONE = 0;
    private final static int MAP_LEFTCLICK = 20000;
    private final static int MAP_RIGHTCLICK = 20001;
    private final static int MAP_CYCLEUP = 20002;
    private final static int MAP_CYCLEDOWN = 20003;
    private final static int MAP_SHOWKEYBOARD = 20004;
    private final static int MAP_SPECIALKEYS = 20005;
    private final static int MAP_ADJUSTCYCLES = 20006;
    private final static int MAP_ADJUSTFRAMES = 20007;
    private final static int MAP_UNLOCK_SPEED = 20008;
    private final static int MAP_JOYBTN_A = 20009;
    private final static int MAP_JOYBTN_B = 20010;


    // private boolean mMapCapture = false;

    // Map of Custom Maps
    public SparseIntArray customMap = new SparseIntArray(
        DosBoxPreferences.NUM_USB_MAPPINGS
    );

    private final int getMappedKeyCode(final int button, final KeyEvent event) {
        switch (button) {
            case MAP_LEFTCLICK:
                DosBoxControl.nativeMouse(
                    0,
                    0,
                    0,
                    0,
                    (event.getAction() == KeyEvent.ACTION_DOWN) ? 0 : 1,
                    BTN_A
                );

                return MAP_EVENT_CONSUMED;
            case MAP_RIGHTCLICK:
                DosBoxControl.nativeMouse(
                    0,
                    0,
                    0,
                    0,
                    (event.getAction() == KeyEvent.ACTION_DOWN) ? 0 : 1,
                    BTN_B
                );

                return MAP_EVENT_CONSUMED;
            case MAP_JOYBTN_A:
                DosBoxControl.nativeJoystick(
                    0,
                    0,
                    (event.getAction() == KeyEvent.ACTION_DOWN) ? 0 : 1,
                    BTN_A
                );

                return MAP_EVENT_CONSUMED;
            case MAP_JOYBTN_B:
                DosBoxControl.nativeJoystick(
                    0,
                    0,
                    (event.getAction() == KeyEvent.ACTION_DOWN) ? 0 : 1,
                    BTN_B
                );

                return MAP_EVENT_CONSUMED;
            case MAP_CYCLEUP:

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (mParent.mTurboOn) {
                        mParent.mTurboOn = false;
                        DBMain.nativeSetOption(
                            DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON,
                            mParent.mTurboOn ? 1 : 0,
                            null,
                            true
                        );
                    }

                    DBMain.nativeSetOption(
                        DBMenuSystem.DOSBOX_OPTION_ID_CYCLE_ADJUST,
                        1,
                        null,
                        true
                    );

                    if (DosBoxControl.nativeGetAutoAdjust()) {
                        Toast.makeText(
                            mParent,
                            "Auto Cycles [" + DosBoxControl.nativeGetCycleCount() + "%]",
                            Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                            mParent,
                            "DosBox Cycles: " + DosBoxControl.nativeGetCycleCount(),
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                }

                return MAP_EVENT_CONSUMED;
            case MAP_CYCLEDOWN:

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (mParent.mTurboOn) {
                        mParent.mTurboOn = false;
                        DBMain.nativeSetOption(
                            DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON,
                            mParent.mTurboOn ? 1 : 0,
                            null,
                            true
                        );
                    }

                    DBMain.nativeSetOption(
                        DBMenuSystem.DOSBOX_OPTION_ID_CYCLE_ADJUST,
                        0,
                        null,
                        true
                    );

                    if (DosBoxControl.nativeGetAutoAdjust()) {
                        Toast.makeText(
                            mParent,
                            "Auto Cycles [" + DosBoxControl.nativeGetCycleCount() + "%]",
                            Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                            mParent,
                            "DosBox Cycles: " + DosBoxControl.nativeGetCycleCount(),
                            Toast.LENGTH_SHORT
                        ).show();
                    }
                }

                return MAP_EVENT_CONSUMED;
            case MAP_SPECIALKEYS:

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mContextMenu = DBMenuSystem.CONTEXT_MENU_SPECIAL_KEYS;
                    mParent.openContextMenu(this);
                }

                return MAP_EVENT_CONSUMED;
            case MAP_ADJUSTCYCLES:

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mContextMenu = DBMenuSystem.CONTEXT_MENU_CYCLES;
                    mParent.openContextMenu(this);
                }

                return MAP_EVENT_CONSUMED;
            case MAP_ADJUSTFRAMES:

                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mContextMenu = DBMenuSystem.CONTEXT_MENU_FRAMESKIP;
                    mParent.openContextMenu(this);
                }

                return MAP_EVENT_CONSUMED;
            case MAP_UNLOCK_SPEED:

                if (mParent.mTurboOn) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        DBMain.nativeSetOption(
                            DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON,
                            0,
                            null,
                            true
                        );                                                                                              // turn off
                        mParent.mTurboOn = false;
                    }
                } else {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        DBMain.nativeSetOption(
                            DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON,
                            1,
                            null,
                            true
                        );                                                                                              // turn on
                        mParent.mTurboOn = true;
                    }
                }

                return MAP_EVENT_CONSUMED;
            default:
                return button;
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        return handleMugenKey(keyCode, true) || handleKey(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        return handleMugenKey(keyCode, false) || handleKey(keyCode, event);
    }

    private boolean handleMugenKey(
        final int keyCode,
        final boolean down
    ) {
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

    private boolean handleKey(int keyCode, final KeyEvent event) {
        int tKeyCode = 0;

        // check for xperia play back case
        if ((keyCode == KeyEvent.KEYCODE_BACK) && event.isAltPressed()) {
            int backval = customMap.get(DosBoxPreferences.XPERIA_BACK_BUTTON);

            if (backval > 0) {
                // Special Sony XPeria Play case
                if (mEnableDpad) {
                    // FIRE2
                    if (
                        (mInputMode == INPUT_MODE_MOUSE) ||
                        (mInputMode == INPUT_MODE_REAL_MOUSE)
                    ) {
                        DosBoxControl.nativeMouse(
                            0,
                            0,
                            0,
                            0,
                            (event.getAction() == KeyEvent.ACTION_DOWN) ? 0 : 1,
                            BTN_B
                        );
                    } else if (
                        (mInputMode == INPUT_MODE_JOYSTICK) ||
                        (mInputMode == INPUT_MODE_REAL_JOYSTICK)
                    ) {
                        DosBoxControl.nativeJoystick(
                            0,
                            0,
                            (event.getAction() == KeyEvent.ACTION_DOWN) ? 0 : 1,
                            BTN_B
                        );
                    }
                } else {
                    // sony xperia play O (circle) button
                    DosBoxControl.sendNativeKey(
                        backval,
                        (event.getAction() == KeyEvent.ACTION_DOWN),
                        mModifierCtrl,
                        mModifierAlt,
                        mModifierShift
                    );

                    return true;                        // consume event
                }
            }

            return true;                // consume event
        }

        // Handle all other keyevents
        int value = customMap.get(keyCode);

        if (value > 0) {
            // found a valid mapping
            tKeyCode = getMappedKeyCode(value, event);

            if (tKeyCode > MAP_NONE) {
                DosBoxControl.sendNativeKey(
                    tKeyCode,
                    (event.getAction() == KeyEvent.ACTION_DOWN),
                    mModifierCtrl,
                    mModifierAlt,
                    mModifierShift
                );

                return true;                 // consume KeyEvent
            } else if (tKeyCode == MAP_EVENT_CONSUMED) {
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // fishstix, allow remap of Android back button
            // catch no mapping
            if (event.getAction() == KeyEvent.ACTION_UP) {
                DBMenuSystem.doConfirmQuit(mParent);
            }

            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_UNKNOWN:
                break;

            default:
                boolean down = (event.getAction() == KeyEvent.ACTION_DOWN);

                if (!down || (event.getRepeatCount() == 0)) {
                    int unicode = event.getUnicodeChar();

                    // filter system generated keys, but not hardware keypresses
                    if (
                        (event.isAltPressed() || event.isShiftPressed()) &&
                        (unicode == 0) &&
                        ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) == 0)
                    ) {
                        break;
                    }

                    // fixed alt key problem for physical keyboard with only left alt
                    if (
                        (!mUseLeftAltOn) &&
                        (keyCode == KeyEvent.KEYCODE_ALT_LEFT)
                    ) {
                        break;
                    }

                    if (
                        (!mUseLeftAltOn) &&
                        (keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT)
                    ) {
                        break;
                    }

                    if ((keyCode > 255) || (unicode > 255)) {
                        // unknown keys
                        break;
                    }

                    keyCode = keyCode | (unicode << 8);

                    long diff = event.getEventTime() - event.getDownTime();

                    if (!down && (diff < 50)) {
                        mKeyHandler.removeMessages(keyCode);
                        mKeyHandler.sendEmptyMessageDelayed(
                            keyCode,
                            BUTTON_REPEAT_DELAY -
                            diff
                        );
                    } else if (
                        down &&
                        mKeyHandler.hasMessages(keyCode)
                    ) { } else {
                        boolean result = DosBoxControl.sendNativeKey(
                            keyCode,
                            down,
                            mModifierCtrl,
                            mModifierAlt,
                            mModifierShift
                        );

                        if (!down) {
                            mModifierCtrl = false;
                            mModifierAlt = false;
                            mModifierShift = false;
                        }

                        return result;
                    }
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
        VideoRedraw(mBitmap, mSrc_width, mSrc_height, 0, mSrc_height);
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

    private final void mouseClick(int button) {
        DosBoxControl.nativeMouse(
            0,
            0,
            -1,
            -1,
            DosBoxControl.ACTION_DOWN,
            button
        );
        try {
            Thread.sleep(CLICK_DELAY);
        } catch (InterruptedException e) { }
        DosBoxControl.nativeMouse(
            0,
            0,
            -1,
            -1,
            DosBoxControl.ACTION_UP,
            button
        );
    }

    // Fix for Motorola Keyboards!!! - fishstix
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new BaseInputConnection(this, false) {
                   @Override
                   public boolean sendKeyEvent(KeyEvent event) {
                       return super.sendKeyEvent(event);
                   }
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

    private final static int GESTURE_NONE = 0;
    public final static int GESTURE_LEFT_CLICK = 3;
    public final static int GESTURE_RIGHT_CLICK = 4;
    public final static int GESTURE_DOUBLE_CLICK = 5;
    public int mGestureUp = GESTURE_NONE;
    public int mGestureDown = GESTURE_NONE;
    public int mGestureSingleClick = GESTURE_NONE;
    public int mGestureDoubleClick = GESTURE_NONE;
    public int mGestureTwoFinger = GESTURE_NONE;
    public boolean mTwoFingerAction = false;

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            if (mInputMode == INPUT_MODE_MOUSE) {
                if (mAbsolute) {
                    final int pointerId =
                        mWrap.getPointerId(
                        event,
                        ((event.getAction() &
                        MotionEvent.ACTION_POINTER_ID_MASK) >>
                        MotionEvent.ACTION_POINTER_ID_SHIFT)
                        );
                    DosBoxControl.nativeMouseWarp(
                        x[pointerId],
                        y[pointerId],
                        mRenderer.x,
                        mRenderer.y,
                        mRenderer.width,
                        mRenderer.height
                    );
                    try {
                        Thread.sleep(85);
                    } catch (InterruptedException e) { }
                }
            }

            return true;
        }
        private static final int SWIPE_MAX_OFF_PATH = 75;
        @Override
        public boolean onFling(
            MotionEvent e1,
            MotionEvent e2,
            float velocityX,
            float velocityY
        ) {
            final float density = getResources().getDisplayMetrics().density;
            int mMarginTouch = (int) (100 * density + 0.5f);                    // 100dp top margin

            if (
                getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT
            ) {
                return false;
            }

            if (e1.getY() < e2.getY()) {
                // swipe down
                if (e1.getY() > mMarginTouch) {
                    return false;
                }

                if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                // mParent.getSupportActionBar().show();
                return true;
            } else {
                // swipe up
                if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                /*if (mParent.getSupportActionBar().isShowing()) {
                        mParent.getSupportActionBar().hide();
                        return true;
                   }*/
                return false;
            }
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            // Log.i("DosBoxTurbo","onDoubleTap()");
            if (mInputMode == INPUT_MODE_MOUSE) {
                switch (mGestureDoubleClick) {
                    case GESTURE_LEFT_CLICK:
                    case GESTURE_RIGHT_CLICK:

                        if (mLongPress) {
                            mDoubleLong = true;
                            DosBoxControl.nativeMouse(
                                0,
                                0,
                                -1,
                                -1,
                                DosBoxControl.ACTION_DOWN,
                                mGestureDoubleClick -
                                GESTURE_LEFT_CLICK
                            );
                        } else {
                            mouseClick(
                                mGestureDoubleClick -
                                GESTURE_LEFT_CLICK
                            );
                        }

                        return true;
                    case GESTURE_DOUBLE_CLICK:
                        mouseClick(BTN_A);
                        try {
                            Thread.sleep(CLICK_DELAY);
                        } catch (InterruptedException e) { }
                        mouseClick(BTN_A);
                }
            }

            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            if (mInputMode == INPUT_MODE_MOUSE) {
                if (
                    (mGestureSingleClick != GESTURE_NONE) &&
                    (mGestureDoubleClick != GESTURE_NONE)
                ) {
                    mouseClick(mGestureSingleClick - GESTURE_LEFT_CLICK);

                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (mInputMode == INPUT_MODE_MOUSE) {
                if (
                    (mGestureDoubleClick == GESTURE_NONE) &&
                    (mGestureSingleClick != GESTURE_NONE)
                ) {                                                                                             // fishstix,fire only when doubleclick gesture is disabled
                    mouseClick(mGestureSingleClick - GESTURE_LEFT_CLICK);

                    return true;
                }
            }

            return false;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            // Log.i("DosBoxTurbo","onLongPress()");
            if (mInputMode == INPUT_MODE_MOUSE) {
                if (
                    !mFilterLongClick && mLongPress && !mDoubleLong &&
                    !mTwoFingerAction
                ) {
                    mLongClick = true;

                    if (mGestureSingleClick != GESTURE_NONE) {
                        DosBoxControl.nativeMouse(
                            0,
                            0,
                            0,
                            0,
                            DosBoxControl.ACTION_DOWN,
                            mGestureSingleClick -
                            GESTURE_LEFT_CLICK
                        );
                    }
                }

                mFilterLongClick = false;
            }
        }
    }
}
