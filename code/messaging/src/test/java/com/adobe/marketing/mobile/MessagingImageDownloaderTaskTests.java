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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BitmapFactory.class, MessagingImageDownloaderTask.class, RemoteDownloader.class})
public class MessagingImageDownloaderTaskTests {
    MessagingImageDownloaderTask messagingImageDownloaderTask;
    String IMAGE_URL = "https://www.adobe.com/image.jpg";
    String TEST_DATE = "Fri, 10 Jun 2022 23:49:49 +0000";
    CacheManager cacheManager;

    @Mock
    PlatformServices mockPlatformServices;
    @Mock
    NetworkService mockNetworkService;
    @Mock
    SystemInfoService mockSystemInfoService;
    @Mock
    NetworkService.HttpConnection mockHttpConnection;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(BitmapFactory.class);
        // setup services mocks
        when(mockPlatformServices.getSystemInfoService()).thenReturn(mockSystemInfoService);
        Mockito.when(mockPlatformServices.getNetworkService()).thenReturn(mockNetworkService);

        // setup mock cache
        cacheManager = new CacheManager(mockSystemInfoService);
        final File mockCache = new File("mock_cache");
        when(mockSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);
        PowerMockito.whenNew(CacheManager.class).withAnyArguments().thenReturn(cacheManager);

        // setup mocks
        when(mockHttpConnection.getResponseCode()).thenReturn(200);
        when(mockHttpConnection.getResponsePropertyValue("ETag")).thenReturn("etag");
        when(mockHttpConnection.getResponsePropertyValue("Last-Modified")).thenReturn(TEST_DATE);
    }

    @After
    public void cleanup() {
        // clean cache
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), "images");
    }

    @Test
    public void test_download() {
        // setup
        ArgumentCaptor<NetworkService.Callback> callbackCaptor = ArgumentCaptor.forClass(NetworkService.Callback.class);
        InputStream inputStream = MessagingImageDownloaderTaskTests.class.getClassLoader().getResourceAsStream("experience_cloud.png");
        Mockito.when(mockHttpConnection.getInputStream()).thenReturn(inputStream);
        Bitmap mockBitmap = Mockito.mock(Bitmap.class);
        PowerMockito.when(BitmapFactory.decodeStream(any(InputStream.class))).thenReturn(mockBitmap);
        messagingImageDownloaderTask = new MessagingImageDownloaderTask(IMAGE_URL, mockPlatformServices);
        // test
       messagingImageDownloaderTask.call();
        // verify 1 network request made containing the remote downloader in the callback
        Mockito.verify(mockNetworkService, Mockito.times(1)).connectUrlAsync(anyString(),
                any(NetworkService.HttpCommand.class),
                ArgumentMatchers.<byte[]>isNull(), ArgumentMatchers.<Map<String, String>>isNull(), anyInt(), anyInt(), callbackCaptor.capture());
        assertEquals(RemoteDownloader.class, callbackCaptor.getValue().getClass().getEnclosingClass());
        callbackCaptor.getValue().call(mockHttpConnection);
        // verify cache file was created from the downloaded image asset
        File cachedFile = cacheManager.getFileForCachedURL(IMAGE_URL, "images", true);
        assertEquals(mockBitmap, MessagingUtils.getBitmapFromFile(cachedFile));
    }
}
