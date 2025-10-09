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

package com.adobe.marketing.mobile.aepcomposeui.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.adobe.marketing.mobile.messaging.MessagingTestUtils
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import java.net.HttpURLConnection

class UIUtilsTests {
    companion object {
        const val SAMPLE_URL = "http://assets.adobe.com/1234"
        const val INVALID_URL = "someinvalid url"
    }

    @Mock
    private lateinit var mockNetworkService: Networking

    @Mock
    private lateinit var mockCompletionCallback: (Result<Bitmap>) -> Unit

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockedStaticServiceProvider = mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.networkService).thenReturn(mockNetworkService)
    }

    @After
    fun tearDown() {
        mockedStaticServiceProvider.close()
    }

    @Test
    fun `Download never makes a network request for a null url`() {
        // test
        UIUtils.downloadImage(null, mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Failed to download image, the URL is null or empty.", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
        verifyNoInteractions(mockNetworkService)
    }

    @Test
    fun `Download never makes a network request for a blank url`() {
        // test
        UIUtils.downloadImage("", mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Failed to download image, the URL is null or empty.", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
        verifyNoInteractions(mockNetworkService)
    }

    @Test
    fun `Download never makes a network request for an invalid URL`() {
        // test
        UIUtils.downloadImage(INVALID_URL, mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Failed to download image, the URL is null or empty.", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
        verifyNoInteractions(mockNetworkService)
    }

    @Test
    fun `Download returns failure for null connection`() {
        // setup
        `when`(mockNetworkService.connectAsync(any(), any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(null)
        }

        // test
        UIUtils.downloadImage(SAMPLE_URL, mockCompletionCallback)

        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Failed to download image from url ($SAMPLE_URL), received a null connection.", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
    }

    @Test
    fun `Download returns failure for HTTP error response`() {
        // setup
        val simulatedResponse = MessagingTestUtils.simulateNetworkResponse(HttpURLConnection.HTTP_NOT_FOUND, null, emptyMap())
        `when`(mockNetworkService.connectAsync(any(), any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        // test
        UIUtils.downloadImage(SAMPLE_URL, mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Failed to download image from url ($SAMPLE_URL). Response code was: ${HttpURLConnection.HTTP_NOT_FOUND}.", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
    }

    @Test
    fun `Download returns failure when decoded bitmap is null`() {
        // setup
        val invalidBitmap = "invalid bitmap".byteInputStream()
        val simulatedResponse = MessagingTestUtils.simulateNetworkResponse(HttpURLConnection.HTTP_OK, invalidBitmap, emptyMap())
        `when`(mockNetworkService.connectAsync(any(), any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        // test
        UIUtils.downloadImage(SAMPLE_URL, mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Failed to download image from url ($SAMPLE_URL), decode image from input stream failed.", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
    }

    @Test
    fun `Download returns failure when exception occurs during decoding`() {
        // setup
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(any()) }
            .thenThrow(RuntimeException("Decoding error"))

        val simulatedResponse = MessagingTestUtils.simulateNetworkResponse(HttpURLConnection.HTTP_OK, MessagingTestUtils.bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(any(), any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        // test
        UIUtils.downloadImage(SAMPLE_URL, mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isFailure)
        assertEquals("Decoding error", resultArgumentCaptor.firstValue.exceptionOrNull()?.message)
        mockedStaticBitmapFactory.close()
    }

    @Test
    fun `Download returns success when image is successfully downloaded`() {
        // setup
        val mockBitmap: Bitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(100)
        `when`(mockBitmap.height).thenReturn(100)

        val mockedStaticBitmapFactory = mockStatic(BitmapFactory::class.java)
        mockedStaticBitmapFactory.`when`<Bitmap?> { BitmapFactory.decodeStream(any()) }
            .thenReturn(mockBitmap)

        val simulatedResponse = MessagingTestUtils.simulateNetworkResponse(HttpURLConnection.HTTP_OK, MessagingTestUtils.bitmapToInputStream(mockBitmap), emptyMap())
        `when`(mockNetworkService.connectAsync(any(), any())).thenAnswer {
            val callback = it.getArgument<NetworkCallback>(1)
            callback.call(simulatedResponse)
        }

        // test
        UIUtils.downloadImage(SAMPLE_URL, mockCompletionCallback)

        // verify
        val resultArgumentCaptor = argumentCaptor<Result<Bitmap>>()
        verify(mockCompletionCallback).invoke(resultArgumentCaptor.capture())
        assertTrue(resultArgumentCaptor.firstValue.isSuccess)
        assertEquals(mockBitmap, resultArgumentCaptor.firstValue.getOrNull())
        mockedStaticBitmapFactory.close()
    }
}
