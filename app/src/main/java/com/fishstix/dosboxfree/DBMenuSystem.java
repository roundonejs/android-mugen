/*
 *  Copyright (C) 2012 Fishstix (ruebsamen.gene@gmail.com)
 *
 *  Copyright (C) 2011 Locnet (android.locnet@gmail.com)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

public class DBMenuSystem {
    private static final String mPrefCycleString = "max";       // default slow system
    private static final Uri CONTENT_URI = Uri.parse(
        "content://com.fishstix.dosboxlauncher.files/"
    );
    private static final int MAX_MEMORY = 128;

    public static final int KEYCODE_F1 = 131;

    private final static int MENU_KEYBOARD_ESC = 65;
    private final static int MENU_KEYBOARD_TAB = 66;
    private final static int MENU_KEYBOARD_DEL = 67;
    private final static int MENU_KEYBOARD_INSERT = 68;
    private final static int MENU_KEYBOARD_PAUSE_BREAK = 82;
    private final static int MENU_KEYBOARD_SCROLL_LOCK = 83;

    private final static int MENU_KEYBOARD_F1 = 70;
    private final static int MENU_KEYBOARD_F12 = 81;
    private final static int MENU_KEYBOARD_SWAP_MEDIA = 91;
    private final static int MENU_KEYBOARD_TURBO = 92;

    private final static int MENU_CYCLE_AUTO = 150;
    private final static int MENU_CYCLE_55000 = 205;

    private final static int MENU_FRAMESKIP_0 = 206;
    private final static int MENU_FRAMESKIP_10 = 216;

    private final static String PREF_KEY_FRAMESKIP = "dosframeskip";
    private final static String PREF_KEY_CYCLES = "doscycles";

    // following must sync with AndroidOSfunc.cpp
    public final static int DOSBOX_OPTION_ID_SOUND_MODULE_ON = 1;
    public final static int DOSBOX_OPTION_ID_MEMORY_SIZE = 2;
    public final static int DOSBOX_OPTION_ID_CYCLES = 10;
    public final static int DOSBOX_OPTION_ID_FRAMESKIP = 11;
    public final static int DOSBOX_OPTION_ID_REFRESH_HACK_ON = 12;
    public final static int DOSBOX_OPTION_ID_CYCLE_HACK_ON = 13;
    public final static int DOSBOX_OPTION_ID_MIXER_HACK_ON = 14;
    public final static int DOSBOX_OPTION_ID_AUTO_CPU_ON = 15;
    public final static int DOSBOX_OPTION_ID_TURBO_ON = 16;
    public final static int DOSBOX_OPTION_ID_JOYSTICK_ENABLE = 18;
    public final static int DOSBOX_OPTION_ID_GLIDE_ENABLE = 19;
    public final static int DOSBOX_OPTION_ID_SWAP_MEDIA = 21;
    public final static int DOSBOX_OPTION_ID_START_COMMAND = 50;

    static public void loadPreference(
        DBMain context,
        final SharedPreferences prefs
    ) {
        // gracefully handle upgrade from previous versions, fishstix
        /*if (Integer.valueOf(prefs.getString("confcontroller", "-1")) >= 0) {
                DosBoxPreferences.upgrade(prefs);
           }*/
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        ActivityManager am = (ActivityManager) context.getSystemService(
            Context.ACTIVITY_SERVICE
        );
        int memoryClass = am.getMemoryClass();
        int maxMem = (int) Math.max(maxMemory / 1024, memoryClass) * 4;

        if (!prefs.getBoolean("dosmanualconf", false)) {          // only write conf if not in manual config mode
            // Build DosBox config
            // Set Application Prefs
            PrintStream out;
            InputStream myInput;
            try {
                myInput =
                    context.getAssets().open(DosBoxPreferences.CONFIG_FILE);
                Scanner scanner = new Scanner(myInput);
                out =
                    new PrintStream(
                    new FileOutputStream(
                        context.mConfPath +
                        context.mConfFile
                    )
                    );
                // Write text to file
                out.println("[dosbox]");

                if (MAX_MEMORY < maxMem) {
                    out.println("memsize=" + MAX_MEMORY);
                } else {
                    out.println("memsize=" + maxMem);
                }

                out.println("vmemsize=16");
                out.println(
                    "machine=" + prefs.getString(
                        "dosmachine",
                        "svga_s3"
                    )
                );
                out.println();
                out.println("[render]");
                out.println("frameskip=0");
                out.println();
                out.println("[cpu]");
                out.println("core=" + prefs.getString("doscpu", "dynamic"));
                out.println("cputype=" + prefs.getString("doscputype", "auto"));

                if (prefs.getString("doscycles", "-1").contentEquals("-1")) {
                    out.println("cycles=" + mPrefCycleString);                          // auto performance
                } else {
                    out.println(
                        "cycles=" +
                        prefs.getString("doscycles", "3000")
                    );
                }

                out.println("cycleup=500");
                out.println("cycledown=500");
                out.print("isapnpbios=");

                if (prefs.getBoolean("dospnp", true)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.println();
                out.println("[sblaster]");
                out.println("sbtype=" + prefs.getString("dossbtype", "sb16"));
                out.println("mixer=true");
                out.println("oplmode=auto");
                out.println("oplemu=fast");
                out.println("oplrate=" + prefs.getString("dossbrate", "22050"));
                out.println();
                out.println("[mixer]");
                try {
                    out.println(
                        "prebuffer=" +
                        prefs.getInt("dosmixerprebuffer", 15)
                    );
                } catch (Exception e) {
                    out.println("prebuffer=15");
                }
                out.println("rate=" + prefs.getString("dossbrate", "22050"));
                out.println(
                    "blocksize=" +
                    prefs.getString("dosmixerblocksize", "1024")
                );
                out.println();
                out.println("[dos]");
                out.print("xms=");

                if (prefs.getBoolean("dosxms", true)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.print("ems=");

                if (prefs.getBoolean("dosems", true)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.print("umb=");

                if (prefs.getBoolean("dosumb", true)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.println(
                    "keyboardlayout=" +
                    prefs.getString("doskblayout", "auto")
                );
                out.println();
                out.println("[ipx]");
                out.print("ipx=");

                if (prefs.getBoolean("dosipx", false)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.println();
                out.println("[joystick]");
                out.println("joysticktype=2axis");
                out.print("timed=");

                if (prefs.getBoolean("dostimedjoy", false)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.println();
                out.println("[midi]");

                if (prefs.getBoolean("dosmt32", false)) {
                    out.println("mpu401=intelligent");
                    out.println("mididevice=mt32");
                    out.println("mt32.thread=on");
                    out.println("mt32.verbose=off");
                } else {
                    out.println("mpu401=none");
                    out.println("mididevice=none");
                }

                out.println();
                out.println("[speaker]");
                out.print("pcspeaker=");

                if (prefs.getBoolean("dospcspeaker", false)) {
                    out.println("true");
                } else {
                    out.println("false");
                }

                out.println(
                    "tandyrate=" +
                    prefs.getString("dossbrate", "22050")
                );

                // concat dosbox conf
                while (scanner.hasNextLine()) {
                    out.println(scanner.nextLine());
                }

                // handle autoexec
                if (prefs.getString("dosautoexec", "-1").contains("-1")) {
                    out.println(
                        "mount c: "
                        + context.mugenDirectoryCreator.getMugenDataPath()
                        + " \nc:"
                    );
                } else {
                    out.println(
                        prefs.getString(
                            "dosautoexec",
                            "mount c: "
                            + context.mugenDirectoryCreator.getMugenDataPath()
                            + " \nc:"
                        )
                    );
                }

                out.println("MUGEN.EXE");
                out.flush();
                out.close();
                myInput.close();
                scanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // SCALE SCREEN
        context.mSurfaceView.mScale = prefs.getBoolean("confscale", false);

        // SCREEN SCALE FACTOR
        context.mPrefScaleFactor = prefs.getInt("confresizefactor", 100);

        // SCALE MODE
        if (Integer.valueOf(prefs.getString("confscalemode", "0")) == 0) {
            context.mPrefScaleFilterOn = false;
        } else {
            context.mPrefScaleFilterOn = true;
        }

        // SET Cycles
        if (!prefs.getBoolean("dosmanualconf", false)) {
            try {
                DBMain.nativeSetOption(
                    DBMenuSystem.DOSBOX_OPTION_ID_CYCLES,
                    Integer.valueOf(
                        prefs.getString(
                            "doscycles",
                            "5000"
                        )
                    ),
                    null,
                    true
                );
            } catch (NumberFormatException e) {
                // set default to 5000 cycles on exception
                DBMain.nativeSetOption(
                    DBMenuSystem.DOSBOX_OPTION_ID_CYCLES,
                    2000,
                    null,
                    true
                );
            }
        }

        /*	if (!DBMain.mLicenseResult || !DBMain.mSignatureResult) {
                        prefs.edit().putString("doscycles", "2000").commit();
                        DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_CYCLES, 5000 ,null, DBMain.getLicResult());
                } */


        // Set Frameskip
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_FRAMESKIP,
            Integer.valueOf(
                prefs.getString(
                    "dosframeskip",
                    "2"
                )
            ),
            null,
            true
        );

        // TURBO CYCLE
        DBMain.nativeSetOption(
            DOSBOX_OPTION_ID_CYCLE_HACK_ON,
            prefs.getBoolean("confturbocycle", false) ? 1 : 0,
            null,
            true
        );
        // TURBO VGA
        DBMain.nativeSetOption(
            DOSBOX_OPTION_ID_REFRESH_HACK_ON,
            prefs.getBoolean(
                "confturbovga",
                false
            ) ? 1 : 0,
            null,
            true
        );
        // TURBO AUDIO
        context.mPrefMixerHackOn = prefs.getBoolean("confturbomixer", true);
        DBMain.nativeSetOption(
            DOSBOX_OPTION_ID_MIXER_HACK_ON,
            context.mPrefMixerHackOn ? 1 : 0,
            null,
            true
        );
        // 3DFX (GLIDE) EMULATION
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_GLIDE_ENABLE,
            prefs.getBoolean(
                "dosglide",
                false
            ) ? 1 : 0,
            null,
            true
        );
        // SOUND
        context.mPrefSoundModuleOn = prefs.getBoolean("confsound", true);
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON,
            context.mPrefSoundModuleOn ? 1 : 0,
            null,
            true
        );
        // AUTO CPU
        // context.mPrefAutoCPUOn = prefs.getBoolean("dosautocpu", false);
        // DBMain.nativeSetOption(DBMenuSystem.DOSBOX_OPTION_ID_AUTO_CPU_ON, context.mPrefAutoCPUOn?1:0,null,DBMain.getLicResult());
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_AUTO_CPU_ON,
            0,
            null,
            true
        );

        // VIRTUAL JOYSTICK
        // test enabled
        if (prefs.getBoolean("confjoyoverlay", false)) {
            context.mHandler.sendMessage(
                context.mHandler.obtainMessage(
                    DBMain.
                    HANDLER_ADD_JOYSTICK,
                    0,
                    0
                )
            );
        } else {
            context.mHandler.sendMessage(
                context.mHandler.obtainMessage(
                    DBMain.
                    HANDLER_REMOVE_JOYSTICK,
                    0,
                    0
                )
            );
        }

        LayoutParams params = context.mJoystickView.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            175,
            context.getResources().getDisplayMetrics()
        );
        context.mJoystickView.invalidate();

        // Input Resolution
        if (Integer.valueOf(prefs.getString("confinputlatency", "0")) == 0) {
            // absolute tracking
            context.mSurfaceView.mInputLowLatency = false;
        } else {
            context.mSurfaceView.mInputLowLatency = true;
        }

        // SOUND
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_SOUND_MODULE_ON,
            (prefs.getBoolean("confsound", true)) ? 1 : 0,
            null,
            true
        );

        context.mSurfaceView.forceRedraw();
    }

    public static void copyConfigFile(final DBMain context) {
        try {

            InputStream myInput = new FileInputStream(
                context.mConfPath + context.mConfFile
            );
            myInput.close();
            myInput = null;
        } catch (FileNotFoundException f) {
            try {
                InputStream myInput =
                    context.getAssets().open(context.mConfFile);
                OutputStream myOutput = new FileOutputStream(
                    context.mConfPath + context.mConfFile
                );
                byte[] buffer = new byte[1024];
                int length;

                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }

                myOutput.flush();
                myOutput.close();
                myInput.close();
            } catch (IOException e) { }
        } catch (IOException e) { }
    }


    static public void savePreference(
        DBMain context,
        String key,
        String value
    ) {
        SharedPreferences sharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPrefs != null) {
            SharedPreferences.Editor editor = sharedPrefs.edit();

            if (editor != null) {
                // if (PREF_KEY_REFRESH_HACK_ON.equals(key)) {
                // editor.putBoolean(PREF_KEY_REFRESH_HACK_ON, context.mPrefRefreshHackOn);
                // }
                editor.putString(key, value);

                editor.commit();
            }
        }
    }

    static public void saveBooleanPreference(
        Context context,
        String key,
        boolean value
    ) {
        SharedPreferences sharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPrefs != null) {
            SharedPreferences.Editor editor = sharedPrefs.edit();

            if (editor != null) {
                editor.putBoolean(key, value);
                editor.commit();
            }
        }
    }

    static public boolean getBooleanPreference(Context context, String key) {
        SharedPreferences sharedPrefs =
            PreferenceManager.getDefaultSharedPreferences(context);

        return sharedPrefs.getBoolean(key, false);
    }

    static public void doShowMenu(DBMain context) {
        context.openOptionsMenu();
    }

    static public void doHideMenu(DBMain context) {
        context.closeOptionsMenu();
    }

    static public void doConfirmQuit(final DBMain context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name);
        builder.setMessage("Exit DosBox?");

        builder.setPositiveButton(
            "OK",
            new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                context.stopDosBox();
            }
        }
        );
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    static public void doShowTextDialog(final DBMain context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.app_name);
        builder.setMessage(message);

        builder.setPositiveButton("OK", null);

        builder.create().show();
    }

    public static void doSendDownUpKey(
        final DBMain context,
        final int keyCode
    ) {
        DosBoxControl.sendNativeKey(keyCode, true, false, false, false);
        DosBoxControl.sendNativeKey(keyCode, false, false, false, false);
    }

    public static boolean doContextItemSelected(
        final DBMain context,
        final MenuItem item
    ) {
        int itemID = item.getItemId();

        switch (itemID) {
            case MENU_KEYBOARD_TAB:
                doSendDownUpKey(context, KeyEvent.KEYCODE_TAB);
                break;
            case MENU_KEYBOARD_ESC:
                doSendDownUpKey(context, TouchEventWrapper.KEYCODE_ESCAPE);
                break;
            case MENU_KEYBOARD_DEL:
                doSendDownUpKey(context, TouchEventWrapper.KEYCODE_FORWARD_DEL);
                break;
            case MENU_KEYBOARD_INSERT:
                doSendDownUpKey(context, TouchEventWrapper.KEYCODE_INSERT);
                break;
            case MENU_KEYBOARD_PAUSE_BREAK:
                doSendDownUpKey(context, TouchEventWrapper.KEYCODE_PAUSE_BREAK);
                break;
            case MENU_KEYBOARD_SCROLL_LOCK:
                doSendDownUpKey(context, TouchEventWrapper.KEYCODE_SCROLL_LOCK);
                break;
            case MENU_KEYBOARD_TURBO:
                context.mTurboOn = !context.mTurboOn;
                DBMain.nativeSetOption(
                    DOSBOX_OPTION_ID_TURBO_ON,
                    context.mTurboOn ? 1 : 0,
                    null,
                    true
                );
            case MENU_KEYBOARD_SWAP_MEDIA:
                DBMain.nativeSetOption(
                    DOSBOX_OPTION_ID_SWAP_MEDIA,
                    1,
                    null,
                    true
                );
                break;
            default:

                if (
                    (itemID >= MENU_KEYBOARD_F1) &&
                    (itemID <= MENU_KEYBOARD_F12)
                ) {
                    doSendDownUpKey(
                        context,
                        KEYCODE_F1 + (itemID - MENU_KEYBOARD_F1)
                    );
                } else if (
                    (itemID >= MENU_CYCLE_AUTO) &&
                    (itemID <= MENU_CYCLE_55000)
                ) {
                    if (context.mTurboOn) {
                        context.mTurboOn = false;
                        DBMain.nativeSetOption(
                            DBMenuSystem.DOSBOX_OPTION_ID_TURBO_ON,
                            context.mTurboOn ? 1 : 0,
                            null,
                            true
                        );
                    }

                    int cycles = -1;

                    if (itemID == MENU_CYCLE_AUTO) {
                        cycles = -1;
                    } else {
                        cycles = (itemID - MENU_CYCLE_AUTO) * 1000;
                    }

                    savePreference(
                        context,
                        PREF_KEY_CYCLES,
                        String.valueOf(cycles)
                    );
                    DBMain.nativeSetOption(
                        DOSBOX_OPTION_ID_CYCLES,
                        cycles,
                        null,
                        true
                    );
                } else if (
                    (itemID >= MENU_FRAMESKIP_0) &&
                    (itemID <= MENU_FRAMESKIP_10)
                ) {
                    int frameskip = (itemID - MENU_FRAMESKIP_0);
                    savePreference(
                        context,
                        PREF_KEY_FRAMESKIP,
                        String.valueOf(frameskip)
                    );
                    DBMain.nativeSetOption(
                        DOSBOX_OPTION_ID_FRAMESKIP,
                        frameskip,
                        null,
                        true
                    );
                }

                break;
        }

        return true;
    }

    public static void getData(DBMain context, String pid) {
        try {
            InputStream is = context.getContentResolver().openInputStream(
                Uri.parse(
                    CONTENT_URI + pid + ".xml"
                )
            );
            FileOutputStream fostream;
            // Samsung workaround:
            File file = new File(
                "/dbdata/databases/com.fishstix.dosbox/shared_prefs/"
            );

            if (file.isDirectory() && file.exists()) {
                // samsung
                fostream = new FileOutputStream(
                    "/dbdata/databases/com.fishstix.dosbox/shared_prefs/" + pid +
                    ".xml"
                );
            } else {
                // every one else.
                fostream = new FileOutputStream(
                    context.getFilesDir() + "/../shared_prefs/" + pid + ".xml"
                );
            }

            PrintStream out = new PrintStream(fostream);
            Scanner scanner = new Scanner(is);

            while (scanner.hasNextLine()) {
                out.println(scanner.nextLine());
            }

            out.flush();
            is.close();
            out.close();
            scanner.close();
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void CopyAsset(DBMain ctx, String assetfile) {
        AssetManager assetManager = ctx.getAssets();

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetfile);       // if files resides inside the "Files" directory itself
            out = ctx.openFileOutput(assetfile, Context.MODE_PRIVATE);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) { }
    }

    public static void CopyROM(DBMain ctx, File infile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(infile);       // if files resides inside the "Files" directory itself
            out = ctx.openFileOutput(
                infile.getName().toUpperCase(
                    Locale.US
                ),
                Context.MODE_PRIVATE
            );
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) { }
    }

    public static boolean MT32_ROM_exists(DBMain ctx) {
        File ctrlrom = new File(ctx.getFilesDir(), "MT32_CONTROL.ROM");
        File pcmrom = new File(ctx.getFilesDir(), "MT32_PCM.ROM");

        if (ctrlrom.exists() && pcmrom.exists()) {
            return true;
        }

        return false;
    }

    public static File openFile(String name) {
        File origFile = new File(name);
        File dir = origFile.getParentFile();

        if (dir.listFiles() != null) {
            for (File f : dir.listFiles()) {
                if (f.getName().equalsIgnoreCase(origFile.getName())) {
                    return new File(f.getAbsolutePath());
                }
            }
        }

        return new File(name);
    }

    private static void copyFile(
        InputStream in,
        OutputStream out
    ) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
