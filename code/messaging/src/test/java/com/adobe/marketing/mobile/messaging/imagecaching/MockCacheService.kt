/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.imagecaching

import android.graphics.Bitmap
import com.adobe.marketing.mobile.services.caching.CacheEntry
import com.adobe.marketing.mobile.services.caching.CacheExpiry
import com.adobe.marketing.mobile.services.caching.CacheResult
import com.adobe.marketing.mobile.services.caching.CacheService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MockCacheService : CacheService {

    companion object {
        const val SET_SUCCESS_URL = "https://fastly.picsum.photos/set_success.jpg"
        const val SET_FAILURE_INVALID_URL = "set_fail.jpg"
        const val SET_FAILURE_VALID_URL = "https://fastly.picsum.photos/set_failure.jpg"
        const val SET_EXCEPTION_URL = "https://fastly.picsum.photos/set_exception.jpg"
        const val GET_SUCCESS_URL = "https://fastly.picsum.photos/get_success.jpg"
        const val GET_FAILURE_URL = "https://fastly.picsum.photos/get_fail.jpg"
        const val GET_EXCEPTION_URL = "https://fastly.picsum.photos/get_exception.jpg"
        const val GET_NULL_STREAM_URL = "https://fastly.picsum.photos/get_null_stream_url.jpg"
        const val GET_EXCEPTION_STREAM_URL = "https://fastly.picsum.photos/get_exception_stream_url.jpg"
    }

    private val cacheResult = object : CacheResult {
        override fun getData(): InputStream {
            val mockBitmap: Bitmap = mock(Bitmap::class.java)
            `when`(mockBitmap.width).thenReturn(100)
            `when`(mockBitmap.height).thenReturn(100)
            return bitmapToInputStream(mockBitmap)
        }

        override fun getExpiry() = CacheExpiry.never()
        override fun getMetadata() = null
    }

    private val nullStreamResult = object : CacheResult {
        override fun getData(): InputStream? = null
        override fun getExpiry(): CacheExpiry = CacheExpiry.never()
        override fun getMetadata(): MutableMap<String, String>? = null
    }

    private val exceptionStreamResult = object : CacheResult {
        override fun getData(): InputStream? = throw Exception("He he hee")
        override fun getExpiry(): CacheExpiry = CacheExpiry.never()
        override fun getMetadata(): MutableMap<String, String>? = null
    }

    private val entrySet = mutableMapOf<String, CacheEntry>()

    override fun set(name: String, key: String, value: CacheEntry): Boolean {
        return when (key) {
            SET_SUCCESS_URL -> {
                entrySet[name + key] = value
                true
            }
            SET_FAILURE_INVALID_URL, SET_FAILURE_VALID_URL -> false
            SET_EXCEPTION_URL -> throw Exception("Exception")
            else -> true
        }
    }

    override fun get(name: String, key: String): CacheResult? {
        return when (key) {
            GET_SUCCESS_URL -> cacheResult
            GET_NULL_STREAM_URL -> nullStreamResult
            GET_EXCEPTION_STREAM_URL -> exceptionStreamResult
            GET_EXCEPTION_URL -> throw Exception("Whatt!!??")
            GET_FAILURE_URL -> null
            else -> null
        }
    }

    override fun remove(name: String, key: String): Boolean {
        if (entrySet.containsKey(name + key)) {
            entrySet.remove(name + key)
            return true
        } else {
            return false
        }
    }

    private fun bitmapToInputStream(bitmap: Bitmap): ByteArrayInputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return ByteArrayInputStream(byteArray)
    }
}
