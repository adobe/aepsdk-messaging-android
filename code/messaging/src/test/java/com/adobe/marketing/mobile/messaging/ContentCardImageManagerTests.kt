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
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.caching.CacheEntry
import com.adobe.marketing.mobile.services.caching.CacheResult
import com.adobe.marketing.mobile.services.caching.CacheService
import junit.framework.TestCase.assertNull
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream
import java.net.HttpURLConnection
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContentCardImageManagerTests {

    @Mock
    private lateinit var mockCacheService: CacheService
    @Mock
    private lateinit var mockCacheResult: CacheResult
    @Mock
    private lateinit var mockInputStream: InputStream

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var mockedStaticBitmapFactory: MockedStatic<BitmapFactory>

    @Mock
    private lateinit var mockNetworkService: Networking

    private lateinit var testCachePath: String
    private val imageUrl = "https://fastly.picsum.photos/id/43/400/300.jpg?hmac=fAPJ5p1wbFahFpnqtg004Nny-vTEADhmMxMkwLUSfw0"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockCacheService = mock(CacheService::class.java)
        mockCacheResult = mock(CacheResult::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)
        `when`(mockServiceProvider.cacheService).thenReturn(mockCacheService)
        testCachePath = CONTENT_CARD_TEST_CACHE_SUBDIRECTORY
    }

    @After
    fun tearDown() {
        mockedStaticBitmapFactory.close()
        mockedStaticServiceProvider.close()
        Mockito.validateMockitoUsage()
    }

    @Test
    fun `Get image for the first time when it is not in cache, download and cache is successful`() {
        // setup image caching success
        `when`(mockCacheService.set(eq(testCachePath), eq(imageUrl), any(CacheEntry::class.java))).thenReturn(true)

        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, mockInputStream, emptyMap())
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
                    fail("Test failed as unable to download the image")
                }
            }
        )

        // verify cache service is called to check if the image exists in cache
        verify(mockCacheService, times(1)).get(eq(testCachePath), eq(imageUrl))

        // verify cache service is called to cache the image in the specified cache path
        verify(mockCacheService, times(1)).set(eq(testCachePath), eq(imageUrl), any(CacheEntry::class.java))
    }

    @Test
    fun `Get image for the first time when it is not in cache, download failed`() {
        // setup an http not found response
        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_NOT_FOUND, mockInputStream, emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNull(bitmap)
                    fail("Test should fail as image download failed")
                }
                it.onFailure { failure ->
                    // expect a failure so this should be non null
                    assertNotNull(failure)
                }
            }
        )

        // verify cache service is called to check if the image exists in cache
        verify(mockCacheService, times(1)).get(eq(testCachePath), eq(imageUrl))

        // verify cache service is not called to cache the image
        verify(mockCacheService, times(0)).set(eq(testCachePath), eq(imageUrl), any(CacheEntry::class.java))
    }

    @Test
    fun `Get image for the first time when it is not in cache, caching the downloaded image fails`() {
        // setup image caching failure
        `when`(mockCacheService.set(eq(testCachePath), eq(imageUrl), any(CacheEntry::class.java))).thenReturn(false)

        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, mockInputStream, emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    // download didn't fail so the bitmap is still returned
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to download the image")
                }
            }
        )

        // verify cache service is called to check if the image exists in cache
        verify(mockCacheService, times(1)).get(eq(testCachePath), eq(imageUrl))

        // verify cache service is called to cache the image in the specified cache path
        verify(mockCacheService, times(1)).set(eq(testCachePath), eq(imageUrl), any(CacheEntry::class.java))
    }

    @Test
    fun `Get image for the first time when it is not in default cache, download and cache is successful`() {
        // setup image caching success
        `when`(mockCacheService.set(eq(testCachePath), eq(imageUrl), any(CacheEntry::class.java))).thenReturn(true)

        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, mockInputStream, emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, null,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )

        // verify cache service is called to check if the image exists in the default cache
        verify(mockCacheService, times(1)).get(eq(MessagingConstants.CONTENT_CARD_CACHE_SUBDIRECTORY), eq(imageUrl))

        // verify cache service is called to cache the image in the default cache path
        verify(mockCacheService, times(1)).set(eq(MessagingConstants.CONTENT_CARD_CACHE_SUBDIRECTORY), eq(imageUrl), any(CacheEntry::class.java))
    }

    @Test
    fun `Get image for the first time when it is not in cache, download fails`() {
        // setup for bitmap download simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, mockInputStream, emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            "invalidUrl", testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNull(bitmap)
                    fail("Test should have failed for invalid url")
                }
                it.onFailure { failure ->
                    // expect a failure so this should be non null
                    assertNotNull(failure)
                }
            }
        )

        // verify cache service is called to check if the image exists in cache
        verify(mockCacheService, times(1)).get(eq(testCachePath), eq("invalidUrl"))

        // verify cache service is not called to cache the image in the specified cache path
        verify(mockCacheService, times(0)).set(eq(testCachePath), eq("invalidUrl"), any(CacheEntry::class.java))
    }

    @Test
    fun `Get image from cache`() {
        // setup for bitmap decoding simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        // setup mock cache result
        `when`(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult)
        `when`(mockCacheResult.data).thenReturn(mockInputStream)

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

        // verify cache service get is called twice:
        // 1. to check if the image exists in cache
        // 2. to retrieve the image from the cache
        verify(mockCacheService, times(2)).get(eq(testCachePath), eq(imageUrl))
    }

    @Test
    fun `Get image from default cache`() {
        // setup for bitmap decoding simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        // setup mock cache result
        `when`(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult)
        `when`(mockCacheResult.data).thenReturn(mockInputStream)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        `when`(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult)

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, null,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )

        // verify cache service get is called twice:
        // 1. to check if the image exists in cache
        // 2. to retrieve the image from the cache
        verify(mockCacheService, times(2)).get(eq(MessagingConstants.CONTENT_CARD_CACHE_SUBDIRECTORY), eq(imageUrl))
    }

    @Test
    fun `Get image from cache when cache service is not available`() {
        // setup null cache service
        `when`(mockServiceProvider.cacheService).thenReturn(null)

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

        // verify cache service is not called
        verify(mockCacheService, times(0)).get(eq(testCachePath), eq(imageUrl))
    }

    @Test
    fun `Get image from cache when cached data is invalid`() {
        // setup for bitmap decoding simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        // setup mock cache result with invalid data
        `when`(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult)
        `when`(mockCacheResult.data).thenReturn(null)

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNull(bitmap)
                    fail("Test should have failed as cached data is invalid")
                }
                it.onFailure { failure ->
                    // expect a failure so this should be non null
                    assertNotNull(failure)
                }
            }
        )

        // verify cache service get is called twice:
        // 1. to check if the image exists in cache
        // 2. to retrieve the image from the cache
        verify(mockCacheService, times(2)).get(eq(testCachePath), eq(imageUrl))
    }

    @Test
    fun `Get image from cache when cached result is null`() {
        // setup a null mock cache result
        `when`(mockCacheService.get(anyString(), anyString())).thenReturn(null)

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNull(bitmap)
                    fail("Test should have failed as cached result is null")
                }
                it.onFailure { failure ->
                    // expect a failure so this should be non null
                    assertNotNull(failure)
                }
            }
        )

        // verify cache service get is called once:
        // 1. to check if the image exists in cache
        verify(mockCacheService, times(1)).get(eq(testCachePath), eq(imageUrl))
    }

    @Test
    fun `Get image from cache when cached data failed to be decoded into a bitmap`() {
        // setup for bitmap decoding simulation
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(null)

        // setup mock cache result
        `when`(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult)
        `when`(mockCacheResult.data).thenReturn(mockInputStream)

        ContentCardImageManager.getContentCardImageBitmap(
            imageUrl, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNull(bitmap)
                    fail("Test should have failed as the cached data couldn't be decoded into a bitmap")
                }
                it.onFailure { failure ->
                    // expect a failure so this should be non null
                    assertNotNull(failure)
                }
            }
        )

        // verify cache service get is called twice:
        // 1. to check if the image exists in cache
        // 2. to retrieve the image from the cache
        verify(mockCacheService, times(2)).get(eq(testCachePath), eq(imageUrl))
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
