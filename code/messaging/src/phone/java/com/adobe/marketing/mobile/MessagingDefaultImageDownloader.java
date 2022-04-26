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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class MessagingDefaultImageDownloader implements IMessagingImageDownloader {
    private final LruCache<String, Bitmap> cache;
    private final ExecutorService executorService;

    private static volatile MessagingDefaultImageDownloader singletonInstance = null;

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

    @Override
    public Bitmap getBitmapFromUrl(Context context, String imageUrl) {
        if (imageUrl == null) {
            // log url is null
            return null;
        }

        Bitmap bitmap = getBitmapFromMemCache(imageUrl);
        if (bitmap != null) {
            return bitmap;
        }

        Future<Bitmap> bitmapFuture = executorService.submit(new MessagingImageDownloaderTask(imageUrl));
        try {
            bitmap = bitmapFuture.get();
            if (bitmap != null) {
                addBitmapToMemCache(imageUrl, bitmap);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.w(MessagingConstant.LOG_TAG, "Failed to download the image", e);
        }
        return bitmap;
    }

    private void addBitmapToMemCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            cache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return cache.get(key);
    }
}
