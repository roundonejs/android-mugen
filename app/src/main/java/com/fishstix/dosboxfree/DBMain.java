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

import java.io.File;
import java.nio.Buffer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.fishstix.dosboxfree.joystick.JoystickView;

public class DBMain extends Activity {
    public static final String START_COMMAND_ID = "start_command";
    public String mConfFile = DBMenuSystem.CONFIG_FILE;
    public String mConfPath;
    public static final int HANDLER_ADD_JOYSTICK = 20;
    public static final int HANDLER_REMOVE_JOYSTICK = 21;
    public static final int HANDLER_SEND_KEYCODE = 1011;
    public static final int HANDLER_DISABLE_GPU = 323;

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
    private SharedPreferences prefs;
    private DBMain mContext;
    public MugenDirectoryCreator mugenDirectoryCreator;

    public JoystickView mJoystickView = null;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mugenDirectoryCreator = new MugenDirectoryCreator(
            getAssets(),
            getApplication().getApplicationInfo().dataDir
        );
        mContext = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        mConfPath = getExternalDosBoxDir();
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (!DBMenuSystem.MT32_ROM_exists(this)) {
            getMIDIRoms();
        }

        System.loadLibrary("dosbox");

        mSurfaceView = (DBGLSurfaceView) findViewById(R.id.mSurf);
        mJoystickView = (JoystickView) findViewById(R.id.mJoystickView);
        mJoystickView.setVisibility(View.GONE);
        registerForContextMenu(mSurfaceView);

        mSurfaceView.mGPURendering = prefs.getBoolean("confgpu", true);
        mugenDirectoryCreator.createMugenDirectory();
        DBMenuSystem.loadPreference(this, prefs);

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

        nativeInit(mContext);

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

        DBMenuSystem.loadPreference(this, prefs);

        if (Integer.valueOf(prefs.getString("confrotation", "0")) == 0) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (Integer.valueOf(prefs.getString("confrotation", "0")) == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        // check for developer option "dont keep activities"
        int value = Settings.System.getInt(
            getContentResolver(),
            Settings.System.ALWAYS_FINISH_ACTIVITIES,
            0
        );

        if (value != 0) {
            // Dont Keep Activities is enabled
            Toast.makeText(
                this,
                R.string.dontkeepactivities,
                Toast.LENGTH_SHORT
            ).show();
        }

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

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_ADD_JOYSTICK:
                    mJoystickView.setVisibility(View.VISIBLE);

                    DBMenuSystem.saveBooleanPreference(
                        mContext,
                        "confjoyoverlay",
                        true
                    );

                    break;
                case HANDLER_REMOVE_JOYSTICK:
                    mJoystickView.setVisibility(View.GONE);

                    DBMenuSystem.saveBooleanPreference(
                        mContext,
                        "confjoyoverlay",
                        false
                    );
                    break;
                case HANDLER_SEND_KEYCODE:

                    if (msg.arg1 == 0) {
                        mSurfaceView.onKeyDown(msg.arg2, (KeyEvent) msg.obj);
                    } else {
                        mSurfaceView.onKeyUp(msg.arg2, (KeyEvent) msg.obj);
                    }

                    break;
                case HANDLER_DISABLE_GPU:
                    DBMenuSystem.saveBooleanPreference(
                        mContext,
                        "confgpu",
                        false
                    );
                    Toast.makeText(
                        mContext,
                        msg.getData().getString("msg"),
                        Toast.LENGTH_LONG
                    ).show();
                    break;
                default:
                    Toast.makeText(
                        mContext,
                        msg.getData().getString("msg"),
                        Toast.LENGTH_LONG
                    ).show();
            }
        }
    };

    private boolean getMIDIRoms() {
        File ctrlrom = new File(getFilesDir().toString() + "/MT32_CONTROL.ROM");
        File pcmrom = new File(getFilesDir().toString() + "/MT32_PCM.ROM");

        File romdir = getExternalFilesDir(null);

        if (romdir != null) {
            for (File f : romdir.listFiles()) {
                if (f.getName().equalsIgnoreCase("MT32_CONTROL.ROM")) {
                    ctrlrom = f;
                }

                if (f.getName().equalsIgnoreCase("MT32_PCM.ROM")) {
                    pcmrom = f;
                }
            }
        }

        if (!ctrlrom.exists() || !pcmrom.exists()) {
            ctrlrom = DBMenuSystem.openFile(
                getExternalDosBoxDir() + "MT32_CONTROL.ROM"
            );
            pcmrom = DBMenuSystem.openFile(
                getExternalDosBoxDir() + "MT32_PCM.ROM"
            );
            romdir = DBMenuSystem.openFile(getExternalDosBoxDir());

            for (File f : romdir.listFiles()) {
                if (f.getName().equalsIgnoreCase("MT32_CONTROL.ROM")) {
                    ctrlrom = f;
                }

                if (f.getName().equalsIgnoreCase("MT32_PCM.ROM")) {
                    pcmrom = f;
                }
            }
        }

        if (ctrlrom.exists() && pcmrom.exists()) {
            DBMenuSystem.CopyROM(this, ctrlrom);
            DBMenuSystem.CopyROM(this, pcmrom);

            return true;
        }

        return false;
    }

    private String getExternalDosBoxDir() {
        return this.getFilesDir().getAbsolutePath() + "/";
    }
}
