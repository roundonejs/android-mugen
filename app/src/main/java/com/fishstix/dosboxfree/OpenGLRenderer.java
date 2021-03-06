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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Message;

public class OpenGLRenderer implements GLSurfaceView.Renderer {
    private final int[] mTextureName;
    private final DBMain mContext;
    public final int[] mCropWorkspace;
    private int mViewWidth;
    private int mViewHeight;
    private Bitmap mBitmap;
    public int x, y, width, height;
    private int errorCounter;

    public OpenGLRenderer(final DBMain context) {
        mContext = context;
        mTextureName = new int[1];
        mCropWorkspace = new int[4];
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        gl10.glClearColor(0.0f, 0.0f, 0.0f, 1);
        gl10.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
        gl10.glShadeModel(GL10.GL_FLAT);
        gl10.glDisable(GL10.GL_DEPTH_TEST);
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

        gl10.glViewport(0, 0, mViewWidth, mViewHeight);
        gl10.glMatrixMode(GL10.GL_PROJECTION);
        gl10.glLoadIdentity();
        gl10.glEnable(GL10.GL_BLEND);
        gl10.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl10.glShadeModel(GL10.GL_FLAT);
        gl10.glEnable(GL10.GL_TEXTURE_2D);

        GLU.gluOrtho2D(gl10, 0, mViewWidth, mViewHeight, 0);
        gl10.glGenTextures(1, mTextureName, 0);
        errorCounter = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        /*
         * Set our projection matrix. This doesn't have to be done each time we
         * draw, but usually a new projection needs to be set when the viewport
         * is resized.
         */
        mViewWidth = width;
        mViewHeight = height;

        gl10.glViewport(0, 0, mViewWidth, mViewHeight);
        gl10.glLoadIdentity();
        GLU.gluOrtho2D(gl10, 0, mViewWidth, mViewHeight, 0);
    }

    public void setBitmap(final Bitmap newBitmap) {
        mBitmap = newBitmap;
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        // Just clear the screen and depth buffer.
        loadSingleTexture(gl, mBitmap);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        // Begin drawing
        // --------------
        // These function calls can be experimented with for various effects such as transparency
        // although certain functionality maybe device specific.
        gl.glShadeModel(GL10.GL_FLAT);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4x(0x10000, 0x10000, 0x10000, 0x10000);

        // Setup correct projection matrix
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0.0f, mViewWidth, 0.0f, mViewHeight, 0.0f, 1.0f);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glEnable(GL10.GL_TEXTURE_2D);

        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureName[0]);
        ((GL11Ext) gl).glDrawTexfOES(
            mViewWidth - width - x,
            mViewHeight - height - y,
            0,
            width,
            height
        );

        // Finish drawing
        gl.glDisable(GL10.GL_BLEND);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    private void loadSingleTexture(final GL10 gl, final Bitmap bmp) {
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureName[0]);
        gl.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MIN_FILTER,
            GL10.GL_NEAREST
        );
        gl.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_MAG_FILTER,
            GL10.GL_NEAREST
        );
        gl.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_WRAP_S,
            GL10.GL_CLAMP_TO_EDGE
        );
        gl.glTexParameterf(
            GL10.GL_TEXTURE_2D,
            GL10.GL_TEXTURE_WRAP_T,
            GL10.GL_CLAMP_TO_EDGE
        );
        gl.glTexEnvf(
            GL10.GL_TEXTURE_ENV,
            GL10.GL_TEXTURE_ENV_MODE,
            GL10.GL_REPLACE
        );

        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);

        ((GL11) gl).glTexParameteriv(
            GL10.GL_TEXTURE_2D,
            GL11Ext.GL_TEXTURE_CROP_RECT_OES,
            mCropWorkspace,
            0
        );

        int error = gl.glGetError();

        if (error != GL10.GL_NO_ERROR) {
            errorCounter++;

            if (errorCounter > 10) {
                // send msg
                Message msg = new Message();
                msg.what = PreferenceHandler.HANDLER_DISABLE_GPU;
                Bundle b = new Bundle();

                b.putString(
                    "msg",
                    "GPU Rendering Not Supported. GPU Preference Disabled. Please Restart DosBox Turbo."
                );
                msg.setData(b);
                mContext.mHandler.sendMessage(msg);
            }
        }
    }
}
