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

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class DosBoxThread extends Thread {
    private final DBMain mParent;
    private final DBGLSurfaceView mSurfaceView;

    public DosBoxThread(final DBMain parent) {
        mParent = parent;
        mSurfaceView = mParent.mSurfaceView;
    }

    public void run() {
        String path = mParent.mConfPath + DBMenuSystem.CONFIG_FILE;

        mParent.nativeStart(
            mParent,
            mSurfaceView.mBitmap,
            mSurfaceView.mBitmap.getWidth(),
            mSurfaceView.mBitmap.getHeight(),
            path
        );
    }

    public void doExit() {
        if (mSurfaceView != null) {
            InputMethodManager imm = (
                (InputMethodManager) mParent.getSystemService(
                    Context.INPUT_METHOD_SERVICE
                )
            );

            if (imm != null) {
                imm.hideSoftInputFromWindow(mSurfaceView.getWindowToken(), 0);
            }
        }

        mParent.finish();
    }
}
