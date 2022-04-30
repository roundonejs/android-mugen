/*
 *  Copyright (C) 2012 Fishstix - Based upon Dosbox & AnDOSBox by locnet
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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class DosBoxAudio {
    private boolean mAudioRunning = true;
    private AudioTrack mAudio = null;
    private final DBMain mParent;
    public short[] mAudioBuffer = null;

    public DosBoxAudio(final DBMain ctx) {
        mParent = ctx;
    }

    @SuppressWarnings("deprecation")
    public int initAudio(
        final int rate,
        final int channels,
        final int encoding,
        final int bufSize
    ) {
        if (mAudio != null) {
            return 0;
        }

        int newChannels =
            (channels == 1) ? AudioFormat.CHANNEL_CONFIGURATION_MONO :
            AudioFormat.CHANNEL_CONFIGURATION_STEREO;
        int newEncoding = (encoding == 1) ? AudioFormat.ENCODING_PCM_16BIT :
            AudioFormat.ENCODING_PCM_8BIT;
        int androidAudioBufSize = AudioTrack.getMinBufferSize(
            rate,
            newChannels,
            newEncoding
        );

        if (androidAudioBufSize > bufSize) {
            androidAudioBufSize = Math.max(androidAudioBufSize, bufSize);
        }

        mAudioBuffer =
            new short[bufSize >>
                ((mParent.mPrefMixerHackOn == true) ? 3 : 2)];
        mAudio = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            rate,
            newChannels,
            newEncoding,
            androidAudioBufSize,
            AudioTrack.MODE_STREAM
        );
        mAudio.pause();

        return bufSize;
    }

    public void shutDownAudio() {
        if (mAudio != null) {
            mAudio.stop();
            mAudio.release();
            mAudio = null;
        }

        mAudioBuffer = null;
    }

    public void AudioWriteBuffer(final int size) {
        if ((mAudioBuffer != null) && mAudioRunning && (size > 0)) {
            writeSamples(mAudioBuffer, (size << 1));
        }
    }

    private void writeSamples(final short[] samples, final int size) {
        if ((mAudioRunning) && (mAudio != null)) {
            mAudio.write(samples, 0, size);

            if (mAudio.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                play();
            }
        }
    }

    private void play() {
        if (mAudio != null) {
            mAudio.play();
        }
    }

    public void pause() {
        if (mAudio != null) {
            mAudio.pause();
        }
    }
}
