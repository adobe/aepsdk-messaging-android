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

    private val entrySet = mutableMapOf<String, CacheEntry>()

    override fun set(name: String, key: String, value: CacheEntry): Boolean {
        entrySet[name + key] = value
        return true
    }

    override fun get(name: String, key: String): CacheResult? {
        return if (entrySet.containsKey(name + key)) {
            object : CacheResult {
                override fun getData(): InputStream {
                    val mockBitmap: Bitmap = mock(Bitmap::class.java)
                    `when`(mockBitmap.width).thenReturn(100)
                    `when`(mockBitmap.height).thenReturn(100)
                    return bitmapToInputStream(mockBitmap)
                }
                override fun getExpiry() = CacheExpiry.never()
                override fun getMetadata() = null
            }
        } else {
            null
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
