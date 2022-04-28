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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The Messaging extension implementation of {@link IMessagingImageDownloader}.
 */
class MessagingDefaultImageDownloader implements IMessagingImageDownloader {
    private static final String SELF_TAG = "MessagingDefaultImageDownloader";
    private static volatile MessagingDefaultImageDownloader singletonInstance = null;
    private final LruCache<String, Bitmap> cache;
    private final ExecutorService executorService;

    /**
     * Constructor.
     */
    private MessagingDefaultImageDownloader() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        cache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (value != null) {
                    return value.getAllocationByteCount() / 1024; //size of bitmap in KB.
                }
                return 0;
            }
        };
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Singleton method to get the {@link MessagingDefaultImageDownloader} instance.
     *
     * @return the {@code MessagingDefaultImageDownloader} singleton
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
     * Downloads the asset then caches it in memory.
     * TODO: Store the image asset in the disk cache added in the feature/iam branch
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

        if (StringUtils.stringIsUrl(imageUrl)) {
            Log.debug(LOG_TAG, "%s - Unable to download the image asset, the provided URL is invalid.", SELF_TAG);
            return null;
        }

        Bitmap bitmap = getBitmapFromMemCache(imageUrl);
        if (bitmap != null) {
            return bitmap;
        }

        final Future<Bitmap> bitmapFuture = executorService.submit(new MessagingImageDownloaderTask(imageUrl));
        try {
            bitmap = bitmapFuture.get();
            if (bitmap != null) {
                addBitmapToMemCache(imageUrl, bitmap);
            }
        } catch (final ExecutionException | InterruptedException e) {
            Log.warning(LOG_TAG, "%s - Failed to download the image, exception occurred: %s", SELF_TAG, e.getMessage());
        }
        return bitmap;
    }

    /**
     * Stores the provided {@link Bitmap} in the cache.
     *
     * @param key    The {@code String} key to use for caching the {@code Bitmap}
     * @param bitmap a {@code Bitmap} to be cached
     */
    private void addBitmapToMemCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            cache.put(key, bitmap);
        }
    }

    /**
     * Retrieves a cached {@link Bitmap} from the cache.
     *
     * @param key The {@code String} key to use for retrieving the {@code Bitmap}
     * @return {@code Bitmap} retrieved from the cache
     */
    private Bitmap getBitmapFromMemCache(String key) {
        return cache.get(key);
    }
}
