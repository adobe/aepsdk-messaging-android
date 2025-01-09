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

package com.adobe.marketing.mobile.messaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adobe.marketing.mobile.messaging.MessagingTestConstants.CONTENT_CARD_TEST_CACHE_SUBDIRECTORY
import com.adobe.marketing.mobile.messaging.imagecaching.MockCacheService
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheEntry
import com.adobe.marketing.mobile.services.caching.CacheExpiry
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContentCardImageManagerTests {

    private var mockCacheService = MockCacheService()

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Mock
    private lateinit var mockNetworkService: Networking

    private lateinit var testCachePath: String
    private val imageUrl = "https://fastly.picsum.photos/id/43/400/300.jpg?hmac=fAPJ5p1wbFahFpnqtg004Nny-vTEADhmMxMkwLUSfw0"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)
        `when`(mockServiceProvider.cacheService).thenReturn(mockCacheService)
        testCachePath = CONTENT_CARD_TEST_CACHE_SUBDIRECTORY
    }

    @After
    fun tearDown() {
        mockedStaticServiceProvider.close()
        Mockito.validateMockitoUsage()
    }

    @Test
    fun `Get image for the first time when it is not in cache, download and cache is successful`() {

        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )

        mockedStaticBitmapFactory.close()
    }

    @Test
    fun `Get image for the first time when it is not in cache, download fails`() {

        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            "invalidUrl", testCachePath,
            {
                it.onSuccess { bitmap ->
                    fail("Test failed as download should have failed for invalid url")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )

        mockedStaticBitmapFactory.close()
    }

    @Test
    fun `Get image from cache`() {

        // setup for bitmap decoding simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        mockCacheService.set(
            name = testCachePath,
            key = imageUrl,
            value = CacheEntry(
                bitmapToInputStream(mockBitmap), CacheExpiry.never(), emptyMap()
            )
        )

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )

        mockedStaticBitmapFactory.close()
    }

    private fun bitmapToInputStream(bitmap: Bitmap): ByteArrayInputStream {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return ByteArrayInputStream(byteArray)
    }

    private fun simulateNetworkResponse(
        responseCode: Int,
        responseStream: InputStream?,
        metadata: Map<String, String>
    ): HttpConnecting {
        val mockResponse = mock(HttpConnecting::class.java)
        `when`(mockResponse.responseCode).thenReturn(responseCode)
        `when`(mockResponse.inputStream).thenReturn(responseStream)
        `when`(mockResponse.getResponsePropertyValue(org.mockito.kotlin.any())).then {
            return@then metadata[it.getArgument(0)]
        }
        return mockResponse
    }
}
