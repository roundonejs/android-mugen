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

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;

public class KeyHandler extends Handler {
    private static final int SPLASH_TIMEOUT_MESSAGE = -1;
    private final WeakReference<DBGLSurfaceView> mSurface;

    public KeyHandler(final DBGLSurfaceView surface) {
        mSurface = new WeakReference<DBGLSurfaceView>(surface);
    }

    @Override
    public void handleMessage(final Message msg) {
        if (msg.what == SPLASH_TIMEOUT_MESSAGE) {
            DBGLSurfaceView surf = mSurface.get();
            surf.setBackgroundResource(0);
        } else {
            DosBoxControl.releaseNativeKey(msg.what);
        }
    }
}
