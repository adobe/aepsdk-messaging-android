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
import android.graphics.BitmapFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, BitmapFactory.class})
public class MessagingDefaultImageDownloaderTests {
    private EventHub eventHub;
    String IMAGE_URL = "https://www.adobe.com/image.jpg";
    String HASHED_FILE_NAME = "8327a0022daca165b7d989cceb57c1f48a78a4bebb3f21448b226ffad4dbfd36";
    MessagingDefaultImageDownloader messagingDefaultImageDownloader;
    CacheManager cacheManager;
    File mockCache;

    @Mock
    Core mockCore;
    @Mock
    Context mockContext;
    @Mock
    Future<Bitmap> mockBitmapFuture;
    @Mock
    Bitmap mockBitmap;
    @Mock
    ExecutorService mockExecutorService;
    @Mock
    PlatformServices mockPlatformServices;
    @Mock
    SystemInfoService mockSystemInfoService;
    @Mock
    NetworkService mockNetworkService;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(BitmapFactory.class);
        PowerMockito.when(BitmapFactory.decodeStream(any(InputStream.class))).thenReturn(mockBitmap);
        // setup services and core mocks
        Mockito.when(mockPlatformServices.getSystemInfoService()).thenReturn(mockSystemInfoService);
        Mockito.when(mockPlatformServices.getNetworkService()).thenReturn(mockNetworkService);
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;
        Mockito.when(MobileCore.getCore()).thenReturn(mockCore);
        // setup mock cache
        cacheManager = new CacheManager(mockSystemInfoService);
        mockCache = new File("mock_cache");
        when(mockSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);
        PowerMockito.whenNew(CacheManager.class).withAnyArguments().thenReturn(cacheManager);

        messagingDefaultImageDownloader = MessagingDefaultImageDownloader.getInstance();
    }

    @After
    public void cleanup() {
        // clean cache
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), "images");
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
            Thread.sleep(5000);
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
    public void test_getBitmapForUrl_ImagePreviouslyCached() throws IOException {
        // setup
        File imageCacheDir = new File(mockCache, "images");
        File cachedImage = new File(imageCacheDir, HASHED_FILE_NAME);
        File testFile = new File(MessagingDefaultImageDownloaderTests.class.getClassLoader().getResource("experience_cloud.png").getFile());
        TestUtils.writeInputStreamIntoFile(cachedImage, new FileInputStream(testFile), false);
        cacheManager.createNewCacheFile(IMAGE_URL, "images", new Date());
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
