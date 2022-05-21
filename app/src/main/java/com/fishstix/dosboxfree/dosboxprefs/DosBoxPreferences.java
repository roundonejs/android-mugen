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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.fishstix.dosboxfree.R;

public class DosBoxPreferences extends PreferenceActivity implements
    OnSharedPreferenceChangeListener, OnPreferenceClickListener {
    private Preference doscpu = null;
    private Preference doscycles = null;
    private Preference dosframeskip = null;
    private Preference dossbtype = null;
    private Preference dossbrate = null;
    private Preference dosmixerprebuffer = null;
    private Preference dosmixerblocksize = null;
    private Preference doskblayout = null;
    private Preference dosautoexec = null;
    private Preference dosems = null;
    private Preference dosxms = null;
    private Preference dosumb = null;
    private Preference dosmt32 = null;
    private Preference dospcspeaker = null;
    private Preference dostimedjoy = null;
    private Preference dosmachine = null;
    private Preference doscputype = null;
    private Preference confgpu = null;
    private Preference confreset = null;
    private Preference version = null;

    public static final String CONFIG_FILE = "dosbox.conf";
    public String CONFIG_PATH;
    public String STORAGE_PATH;

    private PreferenceCategory prefCatOther = null;

    private SharedPreferences prefs;

    private Context ctx = null;

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

        addPreferencesFromResource(R.xml.preferences);
        doscpu = (Preference) findPreference("doscpu");
        doscputype = (Preference) findPreference("doscputype");
        doscycles = (Preference) findPreference("doscycles");
        dosframeskip = (Preference) findPreference("dosframeskip");
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
        dosmt32 = (Preference) findPreference("dosmt32");
        confreset = (Preference) findPreference("confreset");
        confgpu = (Preference) findPreference("confgpu");
        confreset.setOnPreferenceClickListener(this);
        version = (Preference) findPreference("version");
        version.setOnPreferenceClickListener(this);

        prefCatOther = (PreferenceCategory) findPreference("prefCatOther");
    }

    @Override
    public void onResume() {
        super.onResume();

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
        if (
            key.contentEquals("doscycles") &&
            prefs.getString("doscycles", "").contentEquals("auto")
        ) {
            // turn on cpuauto and disable it
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        } else if (
            (key.contentEquals("doscpu"))
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
        ) {
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        }
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
        }

        return false;
    }

    public static String getExternalDosBoxDir(final Context ctx) {
        return ctx.getFilesDir().getAbsolutePath() + "/";
    }
}
