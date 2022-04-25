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
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TableRow;
import android.widget.Toast;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.joystick.JoystickView;


public class DBMain extends Activity {
    public static final int SPLASH_TIMEOUT_MESSAGE = -1;
    public static final String START_COMMAND_ID = "start_command";
    public String mConfFile = DosBoxPreferences.CONFIG_FILE;
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
    public static native boolean nativeHasNEON(Object ctx);
    public static native boolean nativeIsARMv7(Object ctx);
    public static native boolean nativeIsARMv15(Object ctx);
    public static native int nativeGetCPUFamily();

    public DBGLSurfaceView mSurfaceView = null;
    public DosBoxAudio mAudioDevice = null;
    public DosBoxThread mDosBoxThread = null;
    public SharedPreferences prefs;
    private static DBMain mDosBoxLauncher = null;
    public FrameLayout mFrameLayout = null;
    public MugenDirectoryCreator mugenDirectoryCreator;

    public boolean mPrefScaleFilterOn = false;
    public boolean mPrefSoundModuleOn = true;
    public boolean mPrefMixerHackOn = true;
    public boolean mTurboOn = false;
    public String mPID = null;
    public int mPrefScaleFactor = 100;
    private Context mContext;

    // Private Views
    public JoystickView mJoystickView = null;

    static {
        System.loadLibrary("fishstix_util");
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mugenDirectoryCreator = new MugenDirectoryCreator(
            getAssets(),
            getApplication().getApplicationInfo().dataDir
        );
        mDosBoxLauncher = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);
        mContext = this;


        mConfPath = DosBoxPreferences.getExternalDosBoxDir(mContext);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // copy mt32 libs (if necessary)
        if (!DBMenuSystem.MT32_ROM_exists(this)) {
            getMIDIRoms();
        }

        System.loadLibrary("dosbox");


        mFrameLayout = (FrameLayout) findViewById(R.id.mFrame);
        mSurfaceView = (DBGLSurfaceView) findViewById(R.id.mSurf);
        mJoystickView = (JoystickView) findViewById(R.id.mJoystickView);
        mJoystickView.setVisibility(View.GONE);
        registerForContextMenu(mSurfaceView);

        if (prefs.getBoolean("dosmanualconf", false)) {
            File f;
            f =
                new File(
                prefs.getString(
                    "dosmanualconf_file",
                    DosBoxPreferences.getExternalDosBoxDir(mContext) +
                    DosBoxPreferences.CONFIG_FILE
                )
                );

            mConfPath = f.getParent() + "/";
            mConfFile = f.getName();
        }

        mSurfaceView.mGPURendering = true;
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

    @Override
    protected void onDestroy() {
        stopDosBox();
        shutDownDosBox();
        mSurfaceView.shutDown();
        mSurfaceView = null;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        pauseDosBox(true);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pauseDosBox(false);

        DBMenuSystem.loadPreference(this, prefs);

        // set rotation
        if (Integer.valueOf(prefs.getString("confrotation", "0")) == 0) {
            // auto
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (Integer.valueOf(prefs.getString("confrotation", "0")) == 1) {
            // portrait
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
        } else {

            if (mTurboOn) {
                Toast.makeText(
                    this,
                    R.string.fastforward,
                    Toast.LENGTH_SHORT
                ).show();
            } else {
                if (DosBoxControl.nativeGetAutoAdjust()) {
                    Toast.makeText(
                        this,
                        "Auto Cycles [" + DosBoxControl.nativeGetCycleCount() + "%]",
                        Toast.LENGTH_SHORT
                    ).show();
                } else {
                    Toast.makeText(
                        this,
                        "DosBox Cycles: " + DosBoxControl.nativeGetCycleCount(),
                        Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }

        mSurfaceView.mDirty.set(true);
    }

    private String ReadCPUgovernor() {
        ProcessBuilder cmd;
        String result = "";

        try {
            String[] args =
            {"/system/bin/cat",
             "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"};
            cmd = new ProcessBuilder(args);

            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[1024];

            while (in.read(re) != -1) {
                // System.out.println(new String(re));
                result = result + new String(re);
            }

            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "unknown";
        }

        if (result.length() == 0) {
            result = "unknown";
        }

        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (DBMenuSystem.doContextItemSelected(this, item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void pauseDosBox(boolean pause) {
        if (pause) {
            mDosBoxThread.mDosBoxRunning = false;

            nativePause(1);

            if (mAudioDevice != null) {
                mAudioDevice.pause();
            }
        }
        else {
            nativePause(0);
            mDosBoxThread.mDosBoxRunning = true;
            // will auto play audio when have data
            // if (mAudioDevice != null)
            // mAudioDevice.play();
        }
    }

    void initDosBox() {
        mAudioDevice = new DosBoxAudio(this);

        nativeInit(mDosBoxLauncher);

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
            (mPrefMixerHackOn) ? 1 : 0,
            null,
            true
        );
        nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON,
            (mPrefSoundModuleOn) ? 1 : 0,
            null,
            true
        );

        mDosBoxThread = new DosBoxThread(this);
    }

    void shutDownDosBox() {
        boolean retry;
        retry = true;

        while (retry) {
            try {
                mDosBoxThread.join();
                retry = false;
            } catch (InterruptedException e) {           // try again shutting down the thread
            }
        }

        nativeShutDown();

        if (mAudioDevice != null) {
            mAudioDevice.shutDownAudio();
            mAudioDevice = null;
        }

        mDosBoxThread = null;
    }

    void startDosBox() {
        if (mDosBoxThread != null) {
            mDosBoxThread.start();
        }

        if ((mSurfaceView != null) && (mSurfaceView.mVideoThread != null)) {
            mSurfaceView.mVideoThread.start();
        }
    }

    void stopDosBox() {
        nativePause(0);        // it won't die if not running

        // stop audio AFTER above
        if (mAudioDevice != null) {
            mAudioDevice.pause();
        }

        mSurfaceView.mVideoThread.setRunning(false);

        nativeStop();
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
            }
            else {
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

        // newBitmap = Bitmap.createBitmap(OpenGLRenderer.getNearestPowerOfTwoWithShifts(w), OpenGLRenderer.getNearestPowerOfTwoWithShifts(h), Bitmap.Config.RGB_565);
        if (newBitmap != null) {
            mSurfaceView.mBitmap = null;
            mSurfaceView.mBitmap = newBitmap;

            // locnet, 2011-04-28, support 2.1 or below
            // mSurfaceView.mVideoBuffer = null;
            // mSurfaceView.mVideoBuffer = ByteBuffer.allocateDirect(w * h * DBGLSurfaceView.PIXEL_BYTES);
            // mSurfaceView.mVideoBuffer = ByteBuffer.allocateDirect(OpenGLRenderer.getNearestPowerOfTwoWithShifts(w) * OpenGLRenderer.getNearestPowerOfTwoWithShifts(h) * DBGLSurfaceView.PIXEL_BYTES);
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
        else {
            return 0;
        }
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
        else {
            return null;
        }
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
                        mDosBoxLauncher,
                        msg.getData().getString("msg"),
                        Toast.LENGTH_LONG
                    ).show();
                    break;
                default:
                    Toast.makeText(
                        mDosBoxLauncher,
                        msg.getData().getString("msg"),
                        Toast.LENGTH_LONG
                    ).show();
                    // do something in the user interface to display data from message
            }
        }
    };

    public boolean getMIDIRoms() {
        File ctrlrom = new File(getFilesDir().toString() + "/MT32_CONTROL.ROM");
        File pcmrom = new File(getFilesDir().toString() + "/MT32_PCM.ROM");

        File romdir = getExternalFilesDir(null);

        // File rom = null;
        if (romdir != null) {
            for (File f : romdir.listFiles()) {
                if (f.getName().equalsIgnoreCase("MT32_CONTROL.ROM")) {
                    // found ROM
                    ctrlrom = f;
                }

                if (f.getName().equalsIgnoreCase("MT32_PCM.ROM")) {
                    // found ROM
                    pcmrom = f;
                }
            }
        }

        if (!ctrlrom.exists() || !pcmrom.exists()) {
            ctrlrom = DBMenuSystem.openFile(
                DosBoxPreferences.getExternalDosBoxDir(
                    mContext
                ) + "MT32_CONTROL.ROM"
            );
            pcmrom = DBMenuSystem.openFile(
                DosBoxPreferences.getExternalDosBoxDir(
                    mContext
                ) + "MT32_PCM.ROM"
            );
            romdir = DBMenuSystem.openFile(
                DosBoxPreferences.getExternalDosBoxDir(
                    mContext
                )
            );

            for (File f : romdir.listFiles()) {
                if (f.getName().equalsIgnoreCase("MT32_CONTROL.ROM")) {
                    // found ROM
                    ctrlrom = f;
                }

                if (f.getName().equalsIgnoreCase("MT32_PCM.ROM")) {
                    // found ROM
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
}
