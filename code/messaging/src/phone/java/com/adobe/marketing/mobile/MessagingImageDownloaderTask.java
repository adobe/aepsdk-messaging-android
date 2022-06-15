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
     * @throws {@link MissingPlatformServicesException} if any of the {@link PlatformServices} dependencies are null
     */
    MessagingImageDownloaderTask(final String url, final PlatformServices platformServices) throws MissingPlatformServicesException {
        this.url = url;
        if (platformServices == null) {
            Log.debug(LOG_TAG, "%s - Unable to start a MessagingImageDownloaderTask, the platform services are null.", SELF_TAG);
            throw new MissingPlatformServicesException("Platform services were not found!");
        }
        this.networkService = platformServices.getNetworkService();
        this.systemInfoService = platformServices.getSystemInfoService();
    }

    /**
     * Return the result of the image asset download task.
     *
     * @return the downloaded image asset as a {@link Bitmap}
     * @throws {@link MissingPlatformServicesException}
     */
    @Override
    public Bitmap call() throws MissingPlatformServicesException {
        return download();
    }

    /**
     * Download the image asset using the {@code RemoteDownloader} and convert it to a {@code Bitmap}. Downloaded image assets are
     * stored in the Message extension cache directory.
     *
     * @return the downloaded image asset as a {@link Bitmap}
     * @throws {@link MissingPlatformServicesException} if the {@link NetworkService} or {@link SystemInfoService} are null
     */
    private Bitmap download() throws MissingPlatformServicesException {
        final RemoteDownloader remoteDownloader;
        Bitmap bitmap = null;
        remoteDownloader = new RemoteDownloader(networkService, systemInfoService, url, IMAGES_CACHE_SUBDIRECTORY);
        final File downloadedFile = remoteDownloader.startDownloadSync();
        if (downloadedFile != null) {
            bitmap = MessagingUtils.getBitmapFromFile(downloadedFile);
        } else {
            Log.debug(LOG_TAG, "%s - Failed to download asset from (%s).", SELF_TAG, url);
        }
        return bitmap;
    }
}
