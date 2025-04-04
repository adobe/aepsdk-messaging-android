/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for Building AJO push notification.
 *
 * <p>This class is used internally by the Messaging extension's push builder to build the push
 * notification. This class is not intended to be used by the customers.
 */
class MessagingPushUtils {
    private static final String SELF_TAG = "MessagingPushUtils";

    static Bitmap download(final String url) {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            final URL imageUrl = new URL(url);
            connection = (HttpURLConnection) imageUrl.openConnection();
            inputStream = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to download push notification image from url (%s). Exception: %s",
                    url,
                    e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.warning(
                            MessagingPushConstants.LOG_TAG,
                            SELF_TAG,
                            "IOException during closing Input stream while push notification image"
                                    + " from url (%s). Exception: %s ",
                            url,
                            e.getMessage());
                }
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return bitmap;
    }

    static int getDefaultAppIcon(final Context context) {
        final String packageName = context.getPackageName();
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Package manager NameNotFoundException while reading default application icon."
                            + " Exception: %s",
                    e.getMessage());
        }
        return -1;
    }

    /**
     * Returns the Uri for the sound file with the given name. The sound file must be in the res/raw
     * directory. The sound file should be in format of .mp3, .wav, or .ogg
     *
     * @param soundName the name of the sound file
     * @param context the application {@link Context}
     * @return the Uri for the sound file with the given name
     */
    static Uri getSoundUriForResourceName(final @NonNull String soundName, final Context context) {
        return Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE
                        + "://"
                        + context.getPackageName()
                        + "/raw/"
                        + soundName);
    }

    /**
     * Returns the resource id for the icon with the given name. The icon file must be in the
     * res/drawable directory. If the icon file is not found, 0 is returned.
     *
     * @param iconName the name of the icon file
     * @param context the application {@link Context}
     * @return the resource id for the icon with the given name
     */
    static int getSmallIconWithResourceName(final String iconName, final Context context) {
        if (StringUtils.isNullOrEmpty(iconName)) {
            return 0;
        }
        return context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
    }

    /**
     * Returns the local file {@code Uri} for the cached rich media by reading the path to the file
     * from the metadata of the {@code CacheResult} and then using the {@link FileProvider} to build
     * the Uri.
     *
     * @param cachedRichMedia the {@link CacheResult} containing the cached rich media asset
     * @return the local file {@link Uri} for the cached rich media asset
     */
    static Uri getCachedRichMediaFileUri(@NonNull final CacheResult cachedRichMedia) {
        if (cachedRichMedia == null) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to get cached rich media file Uri. Cache result is null.");
            return null;
        }

        final Map<String, String> metadata = cachedRichMedia.getMetadata();
        if (metadata == null) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to find metadata in cached rich media cache result.");
            return null;
        }

        final String pathToFile = metadata.get("pathToFile");
        if (pathToFile == null) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to find path to file in cached rich media cache result.");
            return null;
        }

        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();
        if (context == null) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to get application context. Can't create Uri for cached rich media"
                            + " file.");
            return null;
        }

        final File cachedFile = new File(pathToFile);
        final String authority =
                context.getPackageName() + MessagingConstants.MESSAGING_FILE_PROVIDER_AUTHORITY;
        return FileProvider.getUriForFile(context, authority, cachedFile);
    }

    /**
     * Determines if the push payload contains GIF content in the adb_image field
     *
     * @param url the adb_image url
     * @return true if the adb_image contains a GIF file, false otherwise
     */
    static boolean isGifContent(@NonNull final String url) {
        return url.endsWith(MessagingConstants.GIF_FILE_EXTENSION);
    }

    /**
     * Downloads the asset from the given URL and caches it using the {@link CacheService}. The
     * downloaded asset is then retrieved from the cache and returned in a {@code CompletableFuture}
     * supplying a {@code CacheResult}.
     *
     * @param singleThreadScheduledExecutor the {@link Executor} to run the download and cache task
     * @param url a {@code String} containing the URL of the asset to download
     * @param timeoutInMillis an (code int} timeout in milliseconds for the download and cache
     *     operation
     * @return a {@link CompletableFuture} that will complete with the {@link CacheResult} of the
     *     cached asset
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    static CompletableFuture<CacheResult> downloadAndCacheAsset(
            @NonNull final Executor singleThreadScheduledExecutor,
            @NonNull final String url,
            final int timeoutInMillis) {
        return CompletableFuture.supplyAsync(
                () -> {
                    final AtomicReference<CacheResult> retrievedCacheResult =
                            new AtomicReference<>();
                    final CountDownLatch latch = new CountDownLatch(1);
                    final MessageAssetDownloader messageAssetDownloader =
                            new MessageAssetDownloader();
                    messageAssetDownloader.downloadAsset(
                            url,
                            cacheResult -> {
                                retrievedCacheResult.set(cacheResult);
                                latch.countDown();
                            });
                    try {
                        latch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
                    } catch (final InterruptedException e) {
                        Log.debug(
                                MessagingConstants.LOG_TAG,
                                SELF_TAG,
                                "downloadAndCacheAsset - Interrupted while waiting for asset to be"
                                        + " downloaded and cached.");
                        return null;
                    }
                    return retrievedCacheResult.get();
                },
                singleThreadScheduledExecutor);
    }
}
