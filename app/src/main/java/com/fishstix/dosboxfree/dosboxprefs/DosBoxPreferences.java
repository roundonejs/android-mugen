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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.fishstix.dosboxfree.R;

public class DosBoxPreferences extends PreferenceActivity implements
    OnSharedPreferenceChangeListener {
    private Preference dossbtype = null;
    private Preference doskblayout = null;
    private Preference dosems = null;
    private Preference dosxms = null;
    private Preference dosumb = null;
    private Preference dosmt32 = null;
    private Preference dospcspeaker = null;
    private Preference dosmachine = null;

    public static final String CONFIG_FILE = "dosbox.conf";

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
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.preferences);
        dossbtype = (Preference) findPreference("dossbtype");
        doskblayout = (Preference) findPreference("doskblayout");
        dospcspeaker = (Preference) findPreference("dospcspeaker");
        dosmachine = (Preference) findPreference("dosmachine");
        dosxms = (Preference) findPreference("dosxms");
        dosems = (Preference) findPreference("dosems");
        dosumb = (Preference) findPreference("dosumb");
        dosmt32 = (Preference) findPreference("dosmt32");
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
            (key.contentEquals("dossbtype"))
            || (key.contentEquals("doskblayout"))
            || (key.contentEquals("dosems"))
            || (key.contentEquals("dosxms"))
            || (key.contentEquals("dosumb"))
            || (key.contentEquals("dospcspeaker"))
            || (key.contentEquals("dosmachine"))
            || (key.contentEquals("dosmt32"))
        ) {
            Toast.makeText(ctx, R.string.restart, Toast.LENGTH_SHORT).show();
        }
    }

    public static String getExternalDosBoxDir(final Context ctx) {
        return ctx.getFilesDir().getAbsolutePath() + "/";
    }
}
