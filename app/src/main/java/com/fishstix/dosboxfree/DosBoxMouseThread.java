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

public class DosBoxMouseThread extends Thread {
    private static final int UPDATE_INTERVAL = 35;
    private boolean mMouseRunning = false;
    private boolean mPaused;
    private Object mPauseLock = new Object();
    private int x = 0;
    private int y = 0;

    public void setRunning(final boolean running) {
        mMouseRunning = running;
    }

    public void setCoord(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public void onPause() {
        synchronized (mPauseLock) {
            mPaused = true;
        }
    }

    public void onResume() {
        synchronized (mPauseLock) {
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }

    public void run() {
        mMouseRunning = true;

        while (mMouseRunning) {
            if ((x != 0) || (y != 0)) {
                DosBoxControl.nativeMouse(
                    0,
                    0,
                    x,
                    y,
                    DosBoxControl.ACTION_MOVE,
                    -1
                );
                DosBoxControl.nativeMouse(
                    0,
                    0,
                    0,
                    0,
                    DosBoxControl.ACTION_MOVE,
                    -1
                );
            }

            try {
                Thread.sleep(UPDATE_INTERVAL);
            } catch (InterruptedException e) { }

            synchronized (mPauseLock) {
                while (mPaused) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) { }
                }
            }
        }
    }
}
