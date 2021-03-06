/*
 *  Copyright (C) 2012 Fishstix - Based upon DosBox & anDosBox by Locnet
 *
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
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

import java.nio.Buffer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;

import com.fishstix.dosboxfree.joystick.JoystickView;

public class DBMain extends Activity {
    public static final String START_COMMAND_ID = "start_command";
    public String mConfPath;

    public native void nativeInit(Object ctx);
    public static native void nativeShutDown();
    public static native void nativeSetOption(
        int option,
        int value,
        String value2,
        boolean l
    );
    public native void nativeStart(
        Object ctx,
        Bitmap bitmap,
        int width,
        int height,
        String confPath
    );
    public static native void nativePause(int state);
    public static native void nativeStop();
    public static native void nativePrefs();

    public DBGLSurfaceView mSurfaceView = null;
    private DosBoxAudio mAudioDevice = null;
    private DosBoxThread mDosBoxThread = null;
    public MugenDirectoryCreator mugenDirectoryCreator;
    public final Handler mHandler;
    public JoystickView mJoystickView = null;

    public DBMain() {
        mHandler = new PreferenceHandler(this);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mugenDirectoryCreator = new MugenDirectoryCreator(
            getAssets(),
            getApplication().getApplicationInfo().dataDir
        );
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        mConfPath = getExternalDosBoxDir();
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        System.loadLibrary("dosbox");

        mSurfaceView = (DBGLSurfaceView) findViewById(R.id.mSurf);
        mJoystickView = (JoystickView) findViewById(R.id.mJoystickView);
        mJoystickView.setVisibility(View.GONE);
        registerForContextMenu(mSurfaceView);

        mugenDirectoryCreator.createMugenDirectory();
        DBMenuSystem.loadPreference(this);

        initDosBox();
        startDosBox();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // calculate joystick constants
        ViewTreeObserver observer = mSurfaceView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // re-calculate joystick constants
                mSurfaceView.setDirty();

                Rect r = new Rect();
                // r will be populated with the coordinates of your view that area still visible.
                mSurfaceView.getWindowVisibleDisplayFrame(r);
            }
        }
        );
    }

    private void initDosBox() {
        mAudioDevice = new DosBoxAudio();

        nativeInit(this);

        String argStartCommand = getIntent().getStringExtra(START_COMMAND_ID);

        if (argStartCommand == null) {
            argStartCommand = "";
        }

        nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_START_COMMAND,
            0,
            argStartCommand,
            true
        );
        nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_MIXER_HACK_ON,
            1,
            null,
            true
        );
        nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON,
            1,
            null,
            true
        );

        mDosBoxThread = new DosBoxThread(this);
    }

    private void startDosBox() {
        if (mDosBoxThread != null) {
            mDosBoxThread.start();
        }

        if ((mSurfaceView != null) && (mSurfaceView.mVideoThread != null)) {
            mSurfaceView.mVideoThread.start();
        }
    }

    @Override
    protected void onDestroy() {
        stopDosBox();
        shutDownDosBox();
        mSurfaceView.shutDown();
        mSurfaceView = null;
        super.onDestroy();
    }

    void stopDosBox() {
        nativePause(0);        // it won't die if not running

        if (mAudioDevice != null) {
            mAudioDevice.pause();
        }

        mSurfaceView.mVideoThread.setRunning(false);

        nativeStop();
    }

    private void shutDownDosBox() {
        boolean retry = true;

        while (retry) {
            try {
                mDosBoxThread.join();
                retry = false;
            } catch (InterruptedException e) { }
        }

        nativeShutDown();

        if (mAudioDevice != null) {
            mAudioDevice.shutDownAudio();
            mAudioDevice = null;
        }

        mDosBoxThread = null;
    }

    @Override
    protected void onPause() {
        pauseDosBox();
        super.onPause();
    }

    private void pauseDosBox() {
        nativePause(1);

        if (mAudioDevice != null) {
            mAudioDevice.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeDosBox();

        mSurfaceView.mDirty.set(true);
    }

    private void resumeDosBox() {
        nativePause(0);
    }

    public void callbackExit() {
        if (mDosBoxThread != null) {
            mDosBoxThread.doExit();
        }
    }

    public void callbackVideoRedraw(int w, int h, int s, int e) {
        if ((mSurfaceView.mSrc_width != w) || (mSurfaceView.mSrc_height != h)) {
            mSurfaceView.bDirtyCoords.set(true);
        }

        mSurfaceView.mSrc_width = w;
        mSurfaceView.mSrc_height = h;

        synchronized (mSurfaceView.mDirty) {
            if (mSurfaceView.mDirty.get()) {
                mSurfaceView.mStartLine = Math.min(mSurfaceView.mStartLine, s);
                mSurfaceView.mEndLine = Math.max(mSurfaceView.mEndLine, e);
            } else {
                mSurfaceView.mStartLine = s;
                mSurfaceView.mEndLine = e;
            }

            mSurfaceView.mDirty.set(true);
        }
    }

    public Bitmap callbackVideoSetMode(int w, int h) {
        if ((mSurfaceView.mSrc_width != w) || (mSurfaceView.mSrc_height != h)) {
            mSurfaceView.bDirtyCoords.set(true);
        }

        mSurfaceView.mSrc_width = w;
        mSurfaceView.mSrc_height = h;
        mSurfaceView.resetScreen(false);
        Bitmap newBitmap = null;
        newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);

        if (newBitmap != null) {
            mSurfaceView.mBitmap = null;
            mSurfaceView.mBitmap = newBitmap;

            // locnet, 2011-04-28, support 2.1 or below
            return mSurfaceView.mBitmap;
        }

        return null;
    }

    // locnet, 2011-04-28, support 2.1 or below
    public Buffer callbackVideoGetBuffer() {
        return null;
    }

    public int callbackAudioInit(
        int rate,
        int channels,
        int encoding,
        int bufSize
    ) {
        if (mAudioDevice != null) {
            return mAudioDevice.initAudio(rate, channels, encoding, bufSize);
        }

        return 0;
    }

    public void callbackAudioShutdown() {
        if (mAudioDevice != null) {
            mAudioDevice.shutDownAudio();
        }
    }

    public void callbackAudioWriteBuffer(int size) {
        if (mAudioDevice != null) {
            mAudioDevice.AudioWriteBuffer(size);
        }
    }

    public short[] callbackAudioGetBuffer() {
        if (mAudioDevice != null) {
            return mAudioDevice.mAudioBuffer;
        }

        return null;
    }

    private String getExternalDosBoxDir() {
        return this.getFilesDir().getAbsolutePath() + "/";
    }
}
