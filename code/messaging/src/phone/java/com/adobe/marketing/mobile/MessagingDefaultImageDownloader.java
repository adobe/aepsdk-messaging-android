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

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The Messaging extension implementation of {@link IMessagingImageDownloader}.
 */
class MessagingDefaultImageDownloader implements IMessagingImageDownloader {
    private static final String SELF_TAG = "MessagingDefaultImageDownloader";
    private static final int THREAD_POOL_SIZE = 1;
    private static volatile MessagingDefaultImageDownloader singletonInstance = null;
    private ExecutorService executorService;
    private PlatformServices platformServices;
    private SystemInfoService systemInfoService;
    private CacheManager cacheManager;

    /**
     * Constructor.
     */
    private MessagingDefaultImageDownloader() {
        final Core core = MobileCore.getCore();
        if (core == null) {
            Log.debug(LOG_TAG, "%s - Unable to create a MessagingDefaultImageDownloader, the Core instance is null.", SELF_TAG);
            return;
        }
        platformServices = core.eventHub.getPlatformServices();
        systemInfoService = platformServices.getSystemInfoService();
        try {
            cacheManager = new CacheManager(systemInfoService);
        } catch (final MissingPlatformServicesException exception) {
            Log.warning(LOG_TAG, "%s - CacheManager implementation missing: (%s)", SELF_TAG, exception.getMessage());
        }
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        createImageAssetsCacheDirectory();
    }

    /**
     * Singleton method to get the {@code MessagingDefaultImageDownloader} instance.
     *
     * @return the {@link MessagingDefaultImageDownloader} singleton
     */
    public static MessagingDefaultImageDownloader getInstance() {
        if (singletonInstance == null) {
            synchronized (MessagingDefaultImageDownloader.class) {
                if (singletonInstance == null) {
                    singletonInstance = new MessagingDefaultImageDownloader();
                }
            }
        }
        return singletonInstance;
    }

    /**
     * Downloads the asset then caches it in the Messaging extension cache directory.
     *
     * @param context  the application {@link Context}
     * @param imageUrl a {@code String} containing the image asset to be downloaded
     * @return the {@link Bitmap} created from the downloaded image asset
     */
    @Override
    public Bitmap getBitmapFromUrl(final Context context, final String imageUrl) {
        if (StringUtils.isNullOrEmpty(imageUrl)) {
            Log.debug(LOG_TAG, "%s - Unable to download the image asset, the provided URL is null or empty.", SELF_TAG);
            return null;
        }

        if (!StringUtils.stringIsUrl(imageUrl)) {
            Log.debug(LOG_TAG, "%s - Unable to download the image asset, the provided URL is invalid.", SELF_TAG);
            return null;
        }

        Bitmap bitmap = MessagingUtils.getBitmapFromFile(cacheManager.getFileForCachedURL(imageUrl, IMAGES_CACHE_SUBDIRECTORY, false));
        if (bitmap != null) {
            Log.debug(LOG_TAG, "%s - Found cached image asset for (%s).", SELF_TAG, imageUrl);
            return bitmap;
        }

        try {
            final Future<Bitmap> bitmapFuture = executorService.submit(new MessagingImageDownloaderTask(imageUrl, platformServices));
            bitmap = bitmapFuture.get();
        } catch (final MissingPlatformServicesException | ExecutionException | InterruptedException exception) {
            Log.warning(LOG_TAG, "%s - Failed to download the image asset from (%s), exception occurred: %s", SELF_TAG, imageUrl, exception.getMessage());
        }
        return bitmap;
    }

    /**
     * Creates the "images" cache directory for the {@code Messaging} extension.
     * <p>
     * This method checks if the cache directory already exists in which case no new directory is created for assets.
     */
    private void createImageAssetsCacheDirectory() {
        final File assetDir = new File(systemInfoService.getApplicationCacheDir() + File.separator + IMAGES_CACHE_SUBDIRECTORY);

        if (!assetDir.exists() && !assetDir.mkdirs()) {
            Log.warning(LOG_TAG, "%s - Unable to create directory at (%s) for caching image assets", SELF_TAG, assetDir.getAbsolutePath());
        }
    }
}
