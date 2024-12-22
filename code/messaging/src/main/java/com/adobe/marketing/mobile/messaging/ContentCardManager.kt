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
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.adobe.marketing.mobile.aepcomposeui.utils.UIUtils
import com.adobe.marketing.mobile.messaging.MessagingConstants.CONTENT_CARD_CACHE_SUBDIRECTORY
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheEntry
import com.adobe.marketing.mobile.services.caching.CacheExpiry
import com.adobe.marketing.mobile.services.caching.CacheResult
import com.adobe.marketing.mobile.services.caching.CacheService
import com.adobe.marketing.mobile.util.StringUtils
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer

class ContentCardManager() {
    private val SELF_TAG: String = "ContentCardManager"
    private val cacheService: CacheService? = ServiceProvider.getInstance().cacheService
    // todo - update cache location
    private val assetCacheLocation: String? = InternalMessagingUtils.getAssetCacheLocation()
    private var contentCardImageBitmap: Bitmap? = null
    private var assetDir: File? = null

    init {
        createAssetCacheDirectory()
    }

    fun getContentCardImageBitmap(imageUrl: String): Bitmap {
        contentCardImageBitmap = if (isImageCached(imageUrl)) {
            getImageBitmapFromCache(imageUrl)
        } else {
            getImageBitmapDownloaded(imageUrl)
        }
        return contentCardImageBitmap ?: getDefaultImageBitmap()
    }

    private fun isImageCached(imageUrl: String): Boolean {
        // checks <imageUrl: imageBitmap> cache
        // returns true or false based on whether cached image is found

        return (cacheService?.get(CONTENT_CARD_CACHE_SUBDIRECTORY, imageUrl) != null)
    }

//    private fun getImageBitmapFromCache(imageUrl: String): Bitmap? {
//        // checks <imageUrl: imageBitmap> cache
//        // returns imageBitmap
//        // returns null in case of error / cache not found
//
//        // todo - convert cacheResult to Bitmap
//        val cachedImageBitmap:CacheResult? = cacheService?.get(CONTENT_CARD_CACHE_SUBDIRECTORY, imageUrl)
//
//        return imageBitmap
//    }

    private fun getImageBitmapFromCache(imageUrl: String): Bitmap? {
        val cachedImageBitmap: CacheResult? = cacheService?.get(CONTENT_CARD_CACHE_SUBDIRECTORY, imageUrl)
        val inputStream = cachedImageBitmap?.getData()

        // Convert the InputStream to a Bitmap
        return inputStream?.let {
            try {
                BitmapFactory.decodeStream(it)
            } catch (e: Exception) {
                Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to read cached data into a bitmap"
                )
                null
            }
        }
    }

    /**
     * Downloads image bitmap
     * and caches it
     * returns downloaded image bitmap
     * */
    private fun getImageBitmapDownloaded(imageUrl: String): Bitmap? {
        var imageBitmap: Bitmap? = null
        UIUtils.downloadImage(imageUrl) {
            it.onSuccess { bitmap ->
                imageBitmap = bitmap
            }
            it.onFailure {
                imageBitmap = getDefaultImageBitmap()
            }
        }

        // todo - add try catch block
        val imageInputStream: InputStream? = imageBitmap?.let { bitmap ->
            val byteArray = ByteArray(bitmap.byteCount)
            val buffer = ByteBuffer.wrap(byteArray)
            bitmap.copyPixelsToBuffer(buffer)
            buffer.rewind() // Reset the buffer position to the beginning
            byteArray.inputStream() // Create InputStream from byte array
        }

        val cacheEntry = imageInputStream?.let {
            CacheEntry(it, CacheExpiry.never(), null)
        }

        if (cacheEntry != null) {
            cacheService?.set(CONTENT_CARD_CACHE_SUBDIRECTORY, imageUrl, cacheEntry)
        }

        return imageBitmap
    }

    /**
     * returns default image
     * can be used in case image cannot be downloaded
     * */
    // todo - update the default image
    private fun getDefaultImageBitmap(): Bitmap = Bitmap.createBitmap(
        200,
        200,
        Bitmap.Config.ARGB_8888
    )
        .apply {
            Canvas(this)
                .drawColor(
                    Color.Gray.toArgb()
                )
        }

    // todo - may not need this method
    /**
     * Creates assets cache directory for a `Message`.
     *
     *
     * This method checks if the cache directory already exists in which case no new directory is
     * created for image assets.
     */
    private fun createAssetCacheDirectory(): Boolean {
        if (StringUtils.isNullOrEmpty(assetCacheLocation)) {
            Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "createAssetCacheDirectory - Failed to create asset cache directory, the asset" +
                    " cache location is not available."
            )
            return false
        }

        try {
            assetDir = File(assetCacheLocation)
            if (!assetDir!!.exists()) {
                return assetDir!!.mkdirs()
            }
            return true
        } catch (ex: java.lang.Exception) {
            Log.warning(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "createAssetCacheDirectory - An unexpected error occurred while managing the image cache directory: \n %s",
                ex.localizedMessage
            )
            return false
        }
    }
}
