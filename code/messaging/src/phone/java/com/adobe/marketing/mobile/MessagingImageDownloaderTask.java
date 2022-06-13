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

import static com.adobe.marketing.mobile.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import android.graphics.Bitmap;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Callable} used to perform the downloading of the image asset.
 */
class MessagingImageDownloaderTask implements Callable<Bitmap> {
    private static final String SELF_TAG = "MessagingImageDownloaderTask";
    private final String url;
    private final NetworkService networkService;
    private final SystemInfoService systemInfoService;

    /**
     * Constructor.
     *
     * @param url {@code String} containing the image asset url to be downloaded
     */
    MessagingImageDownloaderTask(final String url, final PlatformServices platformServices) {
        this.url = url;
        this.networkService = platformServices.getNetworkService();
        this.systemInfoService = platformServices.getSystemInfoService();
    }

    /**
     * Return the result of the image asset download task.
     *
     * @return the downloaded image asset as a {@link Bitmap}
     */
    @Override
    public Bitmap call() {
        return download();
    }

    /**
     * Download the image asset using the {@code RemoteDownloader} and convert it to a {@code Bitmap}. Downloaded image assets are
     * stored in the Message extension cache directory. A {@code CountDownLatch} is used to make the download synchronous as the downloaded
     * image will be used immediately in a displayed push notification.
     *
     * @return the downloaded image asset as a {@link Bitmap}
     */
    private Bitmap download() {
        RemoteDownloader remoteDownloader;
        final Bitmap[] bitmap = new Bitmap[1];
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            remoteDownloader = new RemoteDownloader(networkService, systemInfoService, url, IMAGES_CACHE_SUBDIRECTORY) {
                @Override
                protected void onDownloadComplete(final File downloadedFile) {
                    if (downloadedFile != null) {
                        bitmap[0] = MessagingUtils.getBitmapFromFile(downloadedFile);
                        latch.countDown();
                    } else {
                        Log.debug(LOG_TAG, "%s - Failed to download asset from (%s).", SELF_TAG, url);
                    }
                }
            };
        } catch (final MissingPlatformServicesException exception) {
            Log.warning(LOG_TAG, "%s - Failed to download the image asset: (%s), the platform services were not available.", SELF_TAG, url);
            return null;
        }
        remoteDownloader.startDownload();
        try {
            latch.await(2000, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException exception) {
            Log.warning(LOG_TAG, "%s - Exception occurred when downloading (%s): %s", SELF_TAG, url, exception);
            return null;
        }
        return bitmap[0];
    }
}
