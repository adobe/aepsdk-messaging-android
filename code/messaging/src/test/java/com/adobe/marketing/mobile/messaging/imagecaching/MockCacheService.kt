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

class MockCacheService: CacheService {

    private val entrySet = mutableMapOf<String, CacheEntry>()

    override fun set(name: String, key: String, value: CacheEntry): Boolean {
        entrySet[name+key] = value
        return true
    }

    override fun get(name: String, key: String): CacheResult? {
        return if(entrySet.containsKey(name+key)) {
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
        if(entrySet.containsKey(name+key)) {
            entrySet.remove(name + key)
            return true
        }
        else {
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