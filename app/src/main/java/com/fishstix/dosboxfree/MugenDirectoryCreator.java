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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.res.AssetManager;
import android.util.Log;

public class MugenDirectoryCreator {
    private static final String MUGEN_DIRECTORY = "mugen";
    private final AssetManager assetManager;
    private final String dataPath;

    public MugenDirectoryCreator(
        final AssetManager currentAssetManager,
        final String currentDataPath
    ) {
        assetManager = currentAssetManager;
        dataPath = currentDataPath;
    }

    public void createMugenDirectory() {
        if (!new File(getMugenDataPath()).exists()) {
            copyAssetsToDataDirectory(MUGEN_DIRECTORY);
        }
    }

    private void copyAssetsToDataDirectory(final String directoryName) {
        createDataDirectory(directoryName);

        String[] files = getFilesFromAssets(directoryName);

        for (String filename : files) {
            try {
                copyFileFromAssetsToDataDirectory(directoryName, filename);
            } catch (IOException exception) {
                copyAssetsToDataDirectory(directoryName + "/" + filename);
            }
        }
    }

    private void createDataDirectory(final String directoryName) {
        File directory = new File(dataPath + "/" + directoryName);
        directory.mkdir();
    }

    private String[] getFilesFromAssets(final String directoryName) {
        try {
            return assetManager.list(directoryName);
        } catch (IOException e) {
            Log.e("DosBoxTurbo", "Failed to get asset file list.", e);

            throw new RuntimeException(e);
        }
    }

    private void copyFileFromAssetsToDataDirectory(
        final String directoryName,
        final String filename
    ) throws IOException {
        InputStream in = assetManager.open(directoryName + "/" + filename);
        OutputStream out = new FileOutputStream(
            dataPath + "/" + directoryName + "/" + filename
        );
        copyFile(in, out);
        in.close();
        out.flush();
        out.close();
    }

    private void copyFile(
        final InputStream in,
        final OutputStream out
    ) throws IOException {
        byte[] buffer = new byte[1024];
        int read;

        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public String getMugenDataPath() {
        File mugenDataDirectory = new File(dataPath, MUGEN_DIRECTORY);

        return mugenDataDirectory.getAbsolutePath();
    }
}
