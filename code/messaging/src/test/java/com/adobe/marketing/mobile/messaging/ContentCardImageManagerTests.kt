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

    @Mock
    private lateinit var mockBitmap: Bitmap

    @Mock
    private lateinit var mockedStaticBitmapFactory: MockedStatic<BitmapFactory>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)
        `when`(mockServiceProvider.cacheService).thenReturn(mockCacheService)

        // setup for bitmap download simulation
        mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        // Ensure static mocking is not duplicated
        if (!::mockedStaticBitmapFactory.isInitialized) {
            mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        }
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(mockBitmap)

        testCachePath = CONTENT_CARD_TEST_CACHE_SUBDIRECTORY
    }

    @After
    fun tearDown() {
        mockedStaticServiceProvider.close()
        Mockito.validateMockitoUsage()
        if (::mockedStaticBitmapFactory.isInitialized) {
            mockedStaticBitmapFactory.close()
        }
    }

    @Test
    fun `Get image for the first time when it is not in cache, download and cache is successful`() {

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.SET_SUCCESS_URL, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )
    }

    @Test
    fun `Get image for the first time when it is not in cache, download and cache is successful with null cache path`() {

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.SET_SUCCESS_URL, null,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )
    }

    @Test
    fun `Get image for the first time when it is not in cache, download is successful, cache fails`() {

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.SET_FAILURE_VALID_URL, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )
    }

    @Test
    fun `Get image for the first time when it is not in cache, download fails`() {

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.SET_FAILURE_INVALID_URL, testCachePath,
            {
                it.onSuccess { bitmap ->
                    fail("Test failed as download should have failed for invalid url")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )
    }

    @Test
    fun `Get image for the first time when it is not in cache, download is successful, cache exception`() {

        val simulatedResponse = simulateNetworkResponse(HttpURLConnection.HTTP_OK, bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(Mockito.any(), Mockito.any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.SET_EXCEPTION_URL, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )
    }

    @Test
    fun `Get image from cache, success`() {

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.GET_SUCCESS_URL, testCachePath,
            {
                it.onSuccess { bitmap ->
                    assertNotNull(bitmap)
                }
                it.onFailure {
                    fail("Test failed as unable to fetch image from cache")
                }
            }
        )
    }

    @Test
    fun `Get image from cache, failure`() {
        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.GET_FAILURE_URL, testCachePath,
            {
                it.onSuccess {
                    fail("Test failed as exception should have been thrown")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )
    }

    @Test
    fun `Get image from cache, exception`() {
        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.GET_EXCEPTION_URL, testCachePath,
            {
                it.onSuccess {
                    fail("Test failed as exception should have been thrown")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )
    }

    @Test
    fun `Get image from cache, null stream`() {
        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.GET_NULL_STREAM_URL, testCachePath,
            {
                it.onSuccess {
                    fail("Test failed as exception should have been thrown")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )
    }

    @Test
    fun `Get image from cache, exception in bitmap decoding`() {
        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.GET_EXCEPTION_STREAM_URL, testCachePath,
            {
                it.onSuccess {
                    fail("Test failed as exception should have been thrown")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )
    }

    @Test
    fun `Get image from cache, bitmap is null`() {
        // Mock BitmapFactory to return null
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(Mockito.any()) }
            .thenReturn(null)

        ContentCardImageManager.getContentCardImageBitmap(
            MockCacheService.GET_SUCCESS_URL, testCachePath,
            {
                it.onSuccess {
                    fail("Test failed as bitmap should be null")
                }
                it.onFailure { failure ->
                    assertNotNull(failure)
                }
            }
        )
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
        `when`(mockResponse.getResponsePropertyValue(Mockito.any())).then {
            return@then metadata[it.getArgument(0)]
        }
        return mockResponse
    }
}
