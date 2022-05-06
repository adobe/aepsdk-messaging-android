/*
 Copyright 2022 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@RunWith(PowerMockRunner.class)
public class MessagingDefaultImageDownloaderTests {
    private static final String IMAGE_URL = "https://www.adobe.com/image.jpg";
    MessagingDefaultImageDownloader messagingDefaultImageDownloader;

    @Mock
    Context mockContext;
    @Mock
    Future<Bitmap> mockBitmapFuture;
    @Mock
    Bitmap mockBitmap;
    @Mock
    ExecutorService mockExecutorService;

    @Before
    public void before() {
        messagingDefaultImageDownloader = MessagingDefaultImageDownloader.getInstance();
    }

    @Test
    public void test_getBitmapForUrl() {
        // setup
        Whitebox.setInternalState(messagingDefaultImageDownloader, "executorService", mockExecutorService);
        try {
            when(mockBitmapFuture.get()).thenReturn(mockBitmap);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(mockExecutorService.submit(any(Callable.class))).thenReturn(mockBitmapFuture);

        // test
        Bitmap bitmap = messagingDefaultImageDownloader.getBitmapFromUrl(mockContext, IMAGE_URL);
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // verify
        verify(mockExecutorService, times(1)).submit(any(Callable.class));
        try {
            verify(mockBitmapFuture, times(1)).get();
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
        assertEquals(mockBitmap, bitmap);
    }

    @Test
    public void test_getBitmapForUrl_ImagePreviouslyCached() {
        // setup
        LruCache<String, Bitmap> mockCache = Mockito.mock(LruCache.class);
        when(mockCache.get(IMAGE_URL)).thenReturn(mockBitmap);
        Whitebox.setInternalState(messagingDefaultImageDownloader, "cache", mockCache);
        Whitebox.setInternalState(messagingDefaultImageDownloader, "executorService", mockExecutorService);
        try {
            when(mockBitmapFuture.get()).thenReturn(mockBitmap);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(mockExecutorService.submit(any(Callable.class))).thenReturn(mockBitmapFuture);

        // test
        Bitmap bitmap = messagingDefaultImageDownloader.getBitmapFromUrl(mockContext, IMAGE_URL);

        // verify
        verify(mockExecutorService, times(0)).submit(any(Callable.class));
        try {
            verify(mockBitmapFuture, times(0)).get();
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
        assertEquals(mockBitmap, bitmap);
    }

    @Test
    public void test_getBitmapForUrl_InvalidUrl() {
        // setup
        String invalidImageUrl = "image.jpg";
        Whitebox.setInternalState(messagingDefaultImageDownloader, "executorService", mockExecutorService);
        try {
            when(mockBitmapFuture.get()).thenReturn(mockBitmap);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(mockExecutorService.submit(any(Callable.class))).thenReturn(mockBitmapFuture);

        // test
        Bitmap bitmap = messagingDefaultImageDownloader.getBitmapFromUrl(mockContext, invalidImageUrl);

        // verify
        verify(mockExecutorService, times(0)).submit(any(Callable.class));
        try {
            verify(mockBitmapFuture, times(0)).get();
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
        assertNull(bitmap);
    }

    @Test
    public void test_getBitmapForUrl_EmptyUrl() {
        // setup
        String emptyImageUrl = "";
        Whitebox.setInternalState(messagingDefaultImageDownloader, "executorService", mockExecutorService);
        try {
            when(mockBitmapFuture.get()).thenReturn(mockBitmap);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(mockExecutorService.submit(any(Callable.class))).thenReturn(mockBitmapFuture);

        // test
        Bitmap bitmap = messagingDefaultImageDownloader.getBitmapFromUrl(mockContext, emptyImageUrl);

        // verify
        verify(mockExecutorService, times(0)).submit(any(Callable.class));
        try {
            verify(mockBitmapFuture, times(0)).get();
        } catch (InterruptedException | ExecutionException e) {
            fail(e.getMessage());
        }
        assertNull(bitmap);
    }
}
