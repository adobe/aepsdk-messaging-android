/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} used to perform the downloading of the image asset.
 */
class MessagingImageDownloaderTask implements Callable<Bitmap> {
    private static final String SELF_TAG = "MessagingImageDownloaderTask";
    private final String url;

    /**
     * Constructor.
     *
     * @param url {@code String} containing the image asset url to be downloaded
     */
    MessagingImageDownloaderTask(final String url) {
        this.url = url;
    }

    /**
     * Return the result of the image asset download task.
     *
     * @return the downloaded image asset as a {@link Bitmap}
     */
    @Override
    public Bitmap call() {
        return download(url);
    }

    /**
     * Download the image asset and converts it to a {@link Bitmap}.
     *
     * @param url {@code String} containing the image asset url to be downloaded
     * @return the downloaded image asset as a {@link Bitmap}
     */
    private Bitmap download(final String url) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        try {
            final URL imageUrl = new URL(url);
            connection = (HttpURLConnection) imageUrl.openConnection();
            bitmap = BitmapFactory.decodeStream(connection.getInputStream());
        } catch (final IOException e) {
            Log.warning(LOG_TAG, "%s - Exception occurred when downloading the image: %s", SELF_TAG, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return bitmap;
    }
}
