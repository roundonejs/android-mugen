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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.ViewGroup.LayoutParams;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;

public class DBMenuSystem {
    private static final int MAX_MEMORY = 128;

    // following must sync with AndroidOSfunc.cpp
    public final static int DOSBOX_OPTION_ID_SOUND_MODULE_ON = 1;
    public final static int DOSBOX_OPTION_ID_MEMORY_SIZE = 2;
    public final static int DOSBOX_OPTION_ID_CYCLES = 10;
    public final static int DOSBOX_OPTION_ID_FRAMESKIP = 11;
    public final static int DOSBOX_OPTION_ID_REFRESH_HACK_ON = 12;
    public final static int DOSBOX_OPTION_ID_CYCLE_HACK_ON = 13;
    public final static int DOSBOX_OPTION_ID_MIXER_HACK_ON = 14;
    public final static int DOSBOX_OPTION_ID_AUTO_CPU_ON = 15;
    public final static int DOSBOX_OPTION_ID_JOYSTICK_ENABLE = 18;
    public final static int DOSBOX_OPTION_ID_GLIDE_ENABLE = 19;
    public final static int DOSBOX_OPTION_ID_START_COMMAND = 50;

    public static void loadPreference(
        final DBMain context,
        final SharedPreferences prefs
    ) {
        loadDosBoxConfiguration(context, prefs);
        loadAppConfiguration(context, prefs);
    }

    private static void loadDosBoxConfiguration(
        final DBMain context,
        final SharedPreferences prefs
    ) {
        File configurationFile = new File(
            context.mConfPath + context.mConfFile
        );

        if (!configurationFile.exists()) {
            writeDosBoxConfigurationFile(context, prefs);
        }
    }

    private static void writeDosBoxConfigurationFile(
        final DBMain context,
        final SharedPreferences prefs
    ) {
        PrintStream out;
        InputStream myInput;
        try {
            myInput = context.getAssets().open(DosBoxPreferences.CONFIG_FILE);
            Scanner scanner = new Scanner(myInput);
            out = new PrintStream(
                new FileOutputStream(context.mConfPath + context.mConfFile)
            );
            // Write text to file
            out.println("[dosbox]");
            out.print("memsize=");
            out.println(getMemorySize(context));

            out.println("vmemsize=16");
            out.println("machine=svga_s3");
            out.println();
            out.println("[render]");
            out.println("frameskip=0");
            out.println();
            out.println("[cpu]");
            out.println("core=dynamic");
            out.println("cputype=auto");
            out.println("cycles=max");
            out.println("cycleup=500");
            out.println("cycledown=500");
            out.println("isapnpbios=false");

            out.println();
            out.println("[sblaster]");
            out.println("sbtype=sb16");
            out.println("mixer=true");
            out.println("oplmode=auto");
            out.println("oplemu=fast");
            out.println("oplrate=22050");
            out.println();
            out.println("[mixer]");
            out.println("prebuffer=15");
            out.println("rate=22050");
            out.println("blocksize=1024");
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
            out.println("ipx=false");

            out.println();
            out.println("[joystick]");
            out.println("joysticktype=2axis");
            out.println("timed=false");

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

            out.println("tandyrate=22050");

            // concat dosbox conf
            while (scanner.hasNextLine()) {
                out.println(scanner.nextLine());
            }

            out.println(
                "mount c: "
                + context.mugenDirectoryCreator.getMugenDataPath()
                + " \nc:"
            );
            out.println("MUGEN.EXE");
            printInformationOnDosBox(out);

            out.flush();
            out.close();
            myInput.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printInformationOnDosBox(final PrintStream out) {
        out.println("ECHO This game runs through a fork of DosBox Turbo.");
        out.println("ECHO Fork: https://github.com/roundonejs/android-mugen");
        out.println(
            "ECHO DosBox Turbo: https://sites.google.com/site/dosboxturbo/"
        );

        out.println("ECHO DosBox Turbo licence:");
        out.println(
            "ECHO Copyright (C) 2012 Fishstix (ruebsamen.gene@gmail.com)"
        );
        out.println("ECHO.");
        out.println(
            "ECHO Copyright (C) 2011 Locnet (android.locnet@gmail.com)"
        );
        out.println("ECHO.");
        out.println(
            "ECHO This program is free software; you can redistribute it and/or modify"
            + " it under the terms of the GNU General Public License as published by"
            + " the Free Software Foundation; either version 2 of the License, or"
            + " (at your option) any later version."
        );
        out.println("ECHO.");
        out.println(
            "ECHO This program is distributed in the hope that it will be useful,"
            + " but WITHOUT ANY WARRANTY; without even the implied warranty of"
            + " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the"
            + " GNU General Public License for more details."
        );
        out.println("ECHO.");
        out.println(
            "ECHO You should have received a copy of the GNU General Public License"
            + " along with this program; if not, write to the Free Software"
            + " Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA."
        );
    }

    private static void loadAppConfiguration(
        final DBMain context,
        final SharedPreferences prefs
    ) {
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_CYCLES,
            -1,
            null,
            true
        );

        // Set Frameskip
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_FRAMESKIP,
            0,
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
        DBMain.nativeSetOption(
            DOSBOX_OPTION_ID_MIXER_HACK_ON,
            1,
            null,
            true
        );
        // 3DFX (GLIDE) EMULATION
        DBMain.nativeSetOption(
            DBMenuSystem.DOSBOX_OPTION_ID_GLIDE_ENABLE,
            0,
            null,
            true
        );
        // AUTO CPU
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

        context.mSurfaceView.forceRedraw();
    }

    private static int getMemorySize(final DBMain context) {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        ActivityManager am = (ActivityManager) context.getSystemService(
            Context.ACTIVITY_SERVICE
        );
        int memoryClass = am.getMemoryClass();
        int maxMem = (int) Math.max(maxMemory / 1024, memoryClass) * 4;

        return Math.min(MAX_MEMORY, maxMem);
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
