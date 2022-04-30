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

package com.fishstix.dosboxfree.dosboxprefs;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.Toast;

import com.fishstix.dosboxfree.R;
import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.dosboxprefs.preference.HardCodeWrapper;

public class DosBoxPreferences extends PreferenceActivity implements
    OnSharedPreferenceChangeListener, OnPreferenceClickListener {
    private Preference doscpu = null;
    private Preference doscycles = null;
    private Preference dosframeskip = null;
    private Preference dosmemsize = null;
    private Preference dossbtype = null;
    private Preference dossbrate = null;
    private Preference dosmixerprebuffer = null;
    private Preference dosmixerblocksize = null;
    private Preference doskblayout = null;
    private Preference dosautoexec = null;
    private Preference dosems = null;
    private Preference dosxms = null;
    private Preference dosumb = null;
    private Preference dospnp = null;
    private Preference dosmt32 = null;
    private Preference dospcspeaker = null;
    private Preference dostimedjoy = null;
    private Preference dosmachine = null;
    private Preference doscputype = null;
    private Preference dosmanualconf_file = null;
    private Preference doseditconf_file = null;
    private Preference confgpu = null;
    private Preference confreset = null;
    private Preference version = null;

    public static final int XPERIA_BACK_BUTTON = 72617;

    public static final String CONFIG_FILE = "dosbox.conf";
    public String CONFIG_PATH;
    public String STORAGE_PATH;

    private PreferenceCategory prefCatOther = null;

    private SharedPreferences prefs;
    private static HardCodeWrapper kw = HardCodeWrapper.newInstance();

    private Context ctx = null;
    private static boolean isExperiaPlay = false;

    private static final int TOUCHSCREEN_MOUSE = 0;
    private static final int TOUCHSCREEN_JOY = 1;
    private static final int PHYSICAL_MOUSE = 2;
    private static final int PHYSICAL_JOY = 3;
    private static final int SCROLL_SCREEN = 4;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);
        ctx = this;
        STORAGE_PATH = DosBoxPreferences.getExternalDosBoxDir(ctx);
        CONFIG_PATH = DosBoxPreferences.getExternalDosBoxDir(ctx);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getString("dosautoexec", "-1").contentEquals("-1")) {
            prefs.edit().putString(
                "dosautoexec",
                "mount c: " + STORAGE_PATH +
                " \nc:"
            ).commit();
        }

        if (prefs.getString("dosmanualconf_file", "-1").contentEquals("-1")) {
            prefs.edit().putString(
                "dosmanualconf_file",
                CONFIG_PATH + CONFIG_FILE
            ).commit();
        }

        addPreferencesFromResource(R.xml.preferences);
        doscpu = (Preference) findPreference("doscpu");
        doscputype = (Preference) findPreference("doscputype");
        doscycles = (Preference) findPreference("doscycles");
        dosframeskip = (Preference) findPreference("dosframeskip");
        dosmemsize = (Preference) findPreference("dosmemsize");
        dossbtype = (Preference) findPreference("dossbtype");
        dossbrate = (Preference) findPreference("dossbrate");
        dosmixerprebuffer = (Preference) findPreference("dosmixerprebuffer");
        dosmixerblocksize = (Preference) findPreference("dosmixerblocksize");
        doskblayout = (Preference) findPreference("doskblayout");
        dosautoexec = (Preference) findPreference("dosautoexec");
        dospcspeaker = (Preference) findPreference("dospcspeaker");
        dosmachine = (Preference) findPreference("dosmachine");
        dostimedjoy = (Preference) findPreference("dostimedjoy");
        dosxms = (Preference) findPreference("dosxms");
        dosems = (Preference) findPreference("dosems");
        dosumb = (Preference) findPreference("dosumb");
        dospnp = (Preference) findPreference("dospnp");
        dosmt32 = (Preference) findPreference("dosmt32");
        doseditconf_file = (Preference) findPreference("doseditconf_file");
        confreset = (Preference) findPreference("confreset");
        confgpu = (Preference) findPreference("confgpu");
        confreset.setOnPreferenceClickListener(this);
        dosmanualconf_file = (Preference) findPreference("dosmanualconf_file");
        version = (Preference) findPreference("version");
        version.setOnPreferenceClickListener(this);

        prefCatOther = (PreferenceCategory) findPreference("prefCatOther");
        InputFilter[] filterArray = new InputFilter[2];
        filterArray[0] = new InputFilter() {
            @Override
            public CharSequence filter(
                CharSequence source,
                int start,
                int end,
                Spanned dest,
                int dstart,
                int dend
            ) {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);

                    if (!Character.isLetterOrDigit(c)) {
                        return "";
                    }

                    if (Character.isLetter(c)) {
                        if (!Character.isLowerCase(c)) {
                            return "";
                        }
                    }
                }

                return null;
            }
        };
        filterArray[1] = new InputFilter.LengthFilter(1);

        // check for Xperia Play
        if (
            android.os.Build.DEVICE.equalsIgnoreCase("zeus") ||
            android.os.Build.DEVICE.contains("R800")
        ) {
            isExperiaPlay = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // make updated for dosbox.conf manual mode
        update_dosmanualconf();
        final int sdkVersion = Build.VERSION.SDK_INT;

        // update MT32 config

        boolean MTROM_valid = true;
        File rom = new File(getFilesDir().toString() + "/MT32_CONTROL.ROM");

        if (!rom.exists()) {
            MTROM_valid = false;
        }

        rom = new File(getFilesDir().toString() + "/MT32_PCM.ROM");

        if (!rom.exists()) {
            MTROM_valid = false;
        }

        if (!MTROM_valid) {
            dosmt32.setSummary(R.string.mt32missing);
            dosmt32.setEnabled(false);
        }

        // get the two custom preferences
        Preference versionPref = (Preference) findPreference("version");
        Preference helpPref = (Preference) findPreference("help");
        doseditconf_file.setOnPreferenceClickListener(this);
        String versionName = "";
        try {
            versionName =
                getPackageManager().getPackageInfo(
                getPackageName(),
                0
                ).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        versionPref.setSummary(versionName);

        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(
        SharedPreferences preference,
        String key
    ) {
        if (key.contentEquals("dosmanualconf")) {
            update_dosmanualconf();
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        } else if (key.contentEquals("dosmanualconf_file")) {
            dosmanualconf_file.setSummary(
                preference.getString(
                    "dosmanualconf_file",
                    ""
                )
            );
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        } else if (
            key.contentEquals("doscycles") &&
            prefs.getString("doscycles", "").contentEquals("auto")
        ) {
            // turn on cpuauto and disable it
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        } else if (
            (key.contentEquals("doscpu"))
            || (key.contentEquals("dosmemsize"))
            || (key.contentEquals("dossbtype"))
            || (key.contentEquals("dosautoexec"))
            || (key.contentEquals("dossbrate"))
            || (key.contentEquals("confoptimization"))
            || (key.contentEquals("doskblayout"))
            || (key.contentEquals("dosems"))
            || (key.contentEquals("dosxms"))
            || (key.contentEquals("dosumb"))
            || (key.contentEquals("dospcspeaker"))
            || (key.contentEquals("dosmixerprebuffer"))
            || (key.contentEquals("dosmixerblocksize"))
            || (key.contentEquals("confgpu"))
            || (key.contentEquals("conftimedjoy"))
            || (key.contentEquals("dosmachine"))
            || (key.contentEquals("doscputype"))
            || (key.contentEquals("dosmt32"))
            || (key.contentEquals("dospnp"))
        ) {
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        }
    }

    private void update_dosmanualconf() {
        String configFile;

        if (prefs.getBoolean("dosmanualconf", false)) {
            doscpu.setEnabled(false);
            doscputype.setEnabled(false);
            doscycles.setEnabled(false);
            dosframeskip.setEnabled(false);
            dosmemsize.setEnabled(false);
            dossbtype.setEnabled(false);
            dossbrate.setEnabled(false);
            dosmt32.setEnabled(false);
            dosmachine.setEnabled(false);
            dostimedjoy.setEnabled(false);
            dosmixerprebuffer.setEnabled(false);
            dosmixerblocksize.setEnabled(false);
            dosautoexec.setEnabled(false);

            doskblayout.setEnabled(false);
            dosxms.setEnabled(false);
            dosems.setEnabled(false);
            dosumb.setEnabled(false);
            dospnp.setEnabled(false);
            dospcspeaker.setEnabled(false);
            doseditconf_file.setEnabled(true);

            dosmanualconf_file.setEnabled(true);
            configFile = prefs.getString(
                "dosmanualconf_file",
                CONFIG_PATH + CONFIG_FILE
            );
        } else {
            doscpu.setEnabled(true);
            doscputype.setEnabled(true);
            doscycles.setEnabled(true);
            dosframeskip.setEnabled(true);
            dosmemsize.setEnabled(true);
            dossbtype.setEnabled(true);
            dossbrate.setEnabled(true);
            dosmt32.setEnabled(true);
            dosmachine.setEnabled(true);
            dostimedjoy.setEnabled(true);
            dosmixerprebuffer.setEnabled(true);
            dosmixerblocksize.setEnabled(true);
            dosautoexec.setEnabled(true);
            dosmanualconf_file.setEnabled(false);
            doseditconf_file.setEnabled(false);
            doskblayout.setEnabled(true);
            dosxms.setEnabled(true);
            dosems.setEnabled(true);
            dosumb.setEnabled(true);
            dospnp.setEnabled(true);
            dospcspeaker.setEnabled(true);

            configFile = CONFIG_PATH + CONFIG_FILE;
        }

        dosmanualconf_file.setSummary(configFile);
    }

    public static String hardCodeToString(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_DPAD_UP:         return "KEYCODE_DPAD_UP";
            case KeyEvent.KEYCODE_DPAD_DOWN:     return "KEYCODE_DPAD_DOWN";
            case KeyEvent.KEYCODE_DPAD_LEFT:     return "KEYCODE_DPAD_LEFT";
            case KeyEvent.KEYCODE_DPAD_RIGHT:    return "KEYCODE_DPAD_RIGHT";
            case KeyEvent.KEYCODE_DPAD_CENTER:

                if (isExperiaPlay) {
                    if (isXOkeysSwapped()) {
                        return "KEYCODE_SONY_O";
                    } else {
                        return "KEYCODE_SONY_X";
                    }
                } else {
                    return "KEYCODE_DPAD_CENTER";
                }

            case KeyEvent.KEYCODE_CAMERA:        return "KEYCODE_CAMERA";
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: return
                    "KEYCODE_MEDIA_PREVIOUS";
            case KeyEvent.KEYCODE_MEDIA_NEXT:    return "KEYCODE_MEDIA_NEXT";
            case KeyEvent.KEYCODE_MEDIA_REWIND:    return "KEYCODE_MEDIA_REWIND";
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:    return
                    "KEYCODE_MEDIA_FAST_FORWARD";
            case KeyEvent.KEYCODE_MEDIA_STOP:    return "KEYCODE_MEDIA_STOP";
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return
                    "KEYCODE_MEDIA_PLAY_PAUSE";
            case KeyEvent.KEYCODE_MUTE:            return "KEYCODE_MUTE";
            case KeyEvent.KEYCODE_ALT_LEFT:        return "KEYCODE_ALT_LEFT";
            case KeyEvent.KEYCODE_ALT_RIGHT:    return "KEYCODE_ALT_RIGHT";
            case KeyEvent.KEYCODE_CLEAR:        return "KEYCODE_CLEAR";
            case KeyEvent.KEYCODE_ENVELOPE:        return "KEYCODE_ENVELOPE";
            case KeyEvent.KEYCODE_EXPLORER:        return "KEYCODE_EXPLORER";
            case KeyEvent.KEYCODE_FOCUS:        return "KEYCODE_FOCUS";
            case KeyEvent.KEYCODE_BACK:         return "KEYCODE_BACK";
            case KeyEvent.KEYCODE_VOLUME_UP:    return "KEYCODE_VOLUME_UP";
            case KeyEvent.KEYCODE_VOLUME_DOWN:    return "KEYCODE_VOLUME_DOWN";
            // handle virtual buttons
            case HardCodeWrapper.KEYCODE_VIRTUAL_A:    return
                    "KEYCODE_VIRTUAL_A";
            case HardCodeWrapper.KEYCODE_VIRTUAL_B:    return
                    "KEYCODE_VIRTUAL_B";
            case HardCodeWrapper.KEYCODE_VIRTUAL_C:    return
                    "KEYCODE_VIRTUAL_C";
            case HardCodeWrapper.KEYCODE_VIRTUAL_D:    return
                    "KEYCODE_VIRTUAL_D";

            // handle gamepad diagonals
            case HardCodeWrapper.KEYCODE_DPAD_UP_RIGHT: return
                    "KEYCODE_DPAD_UP_RIGHT";
            case HardCodeWrapper.KEYCODE_DPAD_UP_LEFT: return
                    "KEYCODE_DPAD_UP_LEFT";
            case HardCodeWrapper.KEYCODE_DPAD_DOWN_RIGHT: return
                    "KEYCODE_DPAD_DOWN_RIGHT";
            case HardCodeWrapper.KEYCODE_DPAD_DOWN_LEFT: return
                    "KEYCODE_DPAD_DOWN_LEFT";

            case XPERIA_BACK_BUTTON:

                if (isXOkeysSwapped()) {
                    return "KEYCODE_SONY_X";
                } else {
                    return "KEYCODE_SONY_O";
                }

            case HardCodeWrapper.KEYCODE_BUTTON_X:

                if (isExperiaPlay) {
                    return "KEYCODE_SONY_SQUARE";
                }
                else {
                    return "KEYCODE_BUTTON_X";
                }

            case HardCodeWrapper.KEYCODE_BUTTON_Y:

                if (isExperiaPlay) {
                    return "KEYCODE_SONY_TRIANGLE";
                }
                else {
                    return "KEYCODE_BUTTON_Y";
                }

            default:
                return kw.hardCodeToString(keycode);
        }
    }

    private static final char DEFAULT_O_BUTTON_LABEL = 0x25CB;   // hex for WHITE_CIRCLE
    private static boolean isXOkeysSwapped() {
        boolean flag = false;
        int[] ids = kw.getDeviceIds();

        for (int i = 0; ids != null && i < ids.length; i++) {
            KeyCharacterMap kcm = KeyCharacterMap.load(ids[i]);

            if (
                (kcm != null) && (DEFAULT_O_BUTTON_LABEL ==
                kcm.getDisplayLabel(KeyEvent.KEYCODE_DPAD_CENTER))
            ) {
                flag = true;
                break;
            }
        }

        return flag;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == confreset) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirmreset)
            .setCancelable(false)
            .setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // reset prefs
                    PreferenceManager.getDefaultSharedPreferences(
                        getApplicationContext()
                    ).edit().clear().commit();
                    PreferenceManager.setDefaultValues(
                        getApplicationContext(),
                        R.xml.preferences,
                        true
                    );
                    finish();
                }
            }
            )
            .setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            }
            );
            AlertDialog alert = builder.create();
            alert.show();
        } else if (preference == doseditconf_file) {
            // setup intent
            Intent intent = new Intent(Intent.ACTION_EDIT);
            Uri uri =
                Uri.parse(
                "file://" +
                prefs.getString("dosmanualconf_file", "")
                );
            intent.setDataAndType(uri, "text/plain");
            // Check if file exists, if not, copy template
            File f = new File(prefs.getString("dosmanualconf_file", ""));

            if (!f.exists()) {
                try {
                    InputStream in = getApplicationContext().getAssets().open(
                        "template.conf"
                    );
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] buffer = new byte[1024];
                    int len = in.read(buffer);

                    while (len != -1) {
                        out.write(buffer, 0, len);
                        len = in.read(buffer);
                    }

                    in.close();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // launch editor
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(
                    this,
                    R.string.noeditor,
                    Toast.LENGTH_SHORT
                ).show();
            }
        }

        return false;
    }

    public static String getExternalDosBoxDir(final Context ctx) {
        return ctx.getFilesDir().getAbsolutePath() + "/";
    }
}
