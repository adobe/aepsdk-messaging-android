/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adobe.marketing.mobile.aepcomposeui.utils.UIUtils
import com.adobe.marketing.mobile.messaging.MessagingConstants.CACHE_EXPIRY_TIME
import com.adobe.marketing.mobile.messaging.MessagingConstants.CONTENT_CARD_CACHE_SUBDIRECTORY
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheEntry
import com.adobe.marketing.mobile.services.caching.CacheExpiry
import com.adobe.marketing.mobile.services.caching.CacheResult
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ContentCardImageManager {
    private val SELF_TAG: String = "ContentCardManager"

    /**
     * Fetches the image from cache if present in cache, else downloads the image from the given URL and caches it for future calls.
     *
     * @param imageUrl the url of the image to be fetched
     * @param cacheName(optional) the name of the cache for fetching or caching the image, default value used if cache name is not provided
     * @param completion is a completion callback. Result.success() method is invoked with the image bitmap fetched. In case of any failure, Result.failure() method is invoked with a throwable
     * */
    internal fun getContentCardImageBitmap(imageUrl: String, cacheName: String? = CONTENT_CARD_CACHE_SUBDIRECTORY, completion: (Result<Bitmap>) -> Unit) {
        val resolvedCacheName: String = cacheName ?: CONTENT_CARD_CACHE_SUBDIRECTORY
        if (isImageCached(imageUrl, resolvedCacheName)) {
            getImageBitmapFromCache(imageUrl, resolvedCacheName, completion)
        } else {
            downloadAndCacheImageBitmap(imageUrl, resolvedCacheName, completion)
        }
    }

    /**
     * Checks whether the image at given url is present in the cache or not.
     *
     * @param imageUrl the url of the image
     * @param cacheName the name of the cache for fetching or caching the image
     * @return `True` if the image is found in cache, `False` otherwise
     * */
    private fun isImageCached(imageUrl: String, cacheName: String): Boolean {
        val cacheValue = ServiceProvider.getInstance().cacheService?.get(cacheName, imageUrl)
        return cacheValue != null
    }

    /**
     * Fetches the image from the cache.
     *
     * @param imageUrl the url of the image to be fetched
     * @param cacheName the name of the cache for fetching the image
     * @param completion is a completion callback. Result.success() method is invoked with the image bitmap fetched. In case of any failure, Result.failure() method is invoked with a throwable
     * */
    private fun getImageBitmapFromCache(imageUrl: String, cacheName: String, completion: (Result<Bitmap>) -> Unit) {
        val cachedImageBitmap: CacheResult? = ServiceProvider.getInstance().cacheService?.get(cacheName, imageUrl)
        val inputStream = cachedImageBitmap?.data

        if (inputStream == null) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "getImageBitmapFromCache - Unable to read cached data as the inputStream is null"
            )
            completion(Result.failure(Exception("Unable to read cached bitmap data as the inputStream is null for the url: $imageUrl, cacheName: $cacheName")))
            return
        }

        // Convert the InputStream to a Bitmap
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (bitmap == null) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "getImageBitmapFromCache - Unable to convert the cached file input stream into a bitmap for the url: $imageUrl."
            )
            completion(Result.failure(Exception("Unable to convert the cached file input stream into a bitmap for the url: $imageUrl")))
            return
        }

        Log.trace(
            MessagingConstants.LOG_TAG,
            SELF_TAG,
            "getImageBitmapFromCache - Image retrieved from cache for url: $imageUrl"
        )
        completion(Result.success(bitmap))
    }

    /**
     * Downloads the image from the given url and caches it.
     *
     * @param imageUrl the url of the image to be downloaded
     * @param completion is a completion callback. Result.success() method is invoked with the image bitmap downloaded. In case of any failure, Result.failure() method is invoked with a throwable
     * */
    private fun downloadAndCacheImageBitmap(imageUrl: String, cacheName: String, completion: (Result<Bitmap>) -> Unit) {
        UIUtils.downloadImage(imageUrl) {
            it.onSuccess { bitmap ->
                // return the downloaded image bitmap before caching
                completion(Result.success(bitmap))

                // cache the downloaded bitmap
                val isImageCacheSuccessful = cacheImage(bitmap, imageUrl, cacheName)
                if (!isImageCacheSuccessful) {
                    Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "downloadAndCacheImageBitmap - Image downloaded but failed to cache the image from url: $imageUrl"
                    )
                }
            }
            it.onFailure { failure ->
                Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "downloadAndCacheImageBitmap - Unable to download image from url: $imageUrl"
                )
                completion(Result.failure(failure))
            }
        }
    }

    /**
     * Caches the given image.
     *
     * @param imageBitmap image to be cached
     * @param imageName the unique `key` for storing the image in cache
     * @param cacheName name of the cache where cache entry is to be created
     *
     * @return `True` if image is caches successfully, `False` otherwise
     * */
    private fun cacheImage(imageBitmap: Bitmap, imageName: String, cacheName: String): Boolean {
        val imageInputStream: InputStream = imageBitmap.let { bitmap ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            byteArrayOutputStream.use { baos ->
                baos.toByteArray().inputStream()
            }
        }

        val cacheEntry = CacheEntry(imageInputStream, CacheExpiry.after(CACHE_EXPIRY_TIME), null)
        return ServiceProvider.getInstance().cacheService?.set(cacheName, imageName, cacheEntry) ?: false
    }
}
