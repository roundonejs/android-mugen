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

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

public class PreferenceHandler extends Handler {
    public static final int HANDLER_ADD_JOYSTICK = 20;
    public static final int HANDLER_REMOVE_JOYSTICK = 21;
    public static final int HANDLER_DISABLE_GPU = 323;

    private final DBMain mContext;

    public PreferenceHandler(final DBMain context) {
        mContext = context;
    }

    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {
            case HANDLER_ADD_JOYSTICK:
                mContext.mJoystickView.setVisibility(View.VISIBLE);

                DBMenuSystem.saveBooleanPreference(
                    mContext,
                    "confjoyoverlay",
                    true
                );

                break;
            case HANDLER_REMOVE_JOYSTICK:
                mContext.mJoystickView.setVisibility(View.GONE);

                DBMenuSystem.saveBooleanPreference(
                    mContext,
                    "confjoyoverlay",
                    false
                );
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
        }
    }
}
