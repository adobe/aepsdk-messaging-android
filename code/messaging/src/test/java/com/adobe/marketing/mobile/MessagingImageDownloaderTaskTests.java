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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.graphics.Bitmap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessagingImageDownloaderTask.class, RemoteDownloader.class, MessagingUtils.class})
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
    @Mock
    RemoteDownloader mockRemoteDownloader;
    @Mock
    File mockFile;
    @Mock
    Bitmap mockBitmap;

    @Before
    public void before() throws Exception {
        // setup messaging utils mocks
        PowerMockito.mockStatic(MessagingUtils.class);
        when(MessagingUtils.getBitmapFromFile(any(File.class))).thenReturn(mockBitmap);
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
    public void test_download() throws Exception {
        // setup remote downloader and file mocks
        when(mockRemoteDownloader.startDownloadSync()).thenReturn(mockFile);
        PowerMockito.whenNew(RemoteDownloader.class).withAnyArguments().thenReturn(mockRemoteDownloader);
        // setup
        InputStream inputStream = MessagingImageDownloaderTaskTests.class.getClassLoader().getResourceAsStream("experience_cloud.png");
        when(mockHttpConnection.getInputStream()).thenReturn(inputStream);
        messagingImageDownloaderTask = new MessagingImageDownloaderTask(IMAGE_URL, mockPlatformServices);
        // test
        Bitmap bitmap = messagingImageDownloaderTask.call();
        // verify 1 startDownloadSync called
        verify(mockRemoteDownloader, times(1)).startDownloadSync();
        // verify messagingImageDownloaderTask downloaded file
        assertEquals(mockBitmap, bitmap);
    }

    @Test
    public void test_download_EmptyURL() throws MissingPlatformServicesException {
        // setup
        InputStream inputStream = MessagingImageDownloaderTaskTests.class.getClassLoader().getResourceAsStream("experience_cloud.png");
        when(mockHttpConnection.getInputStream()).thenReturn(inputStream);
        messagingImageDownloaderTask = new MessagingImageDownloaderTask("", mockPlatformServices);
        // test
        Bitmap bitmap = messagingImageDownloaderTask.call();
        // verify no startDownloadSync called
        verify(mockRemoteDownloader, times(0)).startDownloadSync();
        // verify messagingImageDownloaderTask did not download file
        assertEquals(null, bitmap);
    }

    @Test (expected = MissingPlatformServicesException.class)
    public void test_download_NullPlatformServices() throws Exception {
        // setup
        InputStream inputStream = MessagingImageDownloaderTaskTests.class.getClassLoader().getResourceAsStream("experience_cloud.png");
        when(mockHttpConnection.getInputStream()).thenReturn(inputStream);
        messagingImageDownloaderTask = new MessagingImageDownloaderTask(IMAGE_URL, null);
        // test
        Bitmap bitmap = messagingImageDownloaderTask.call();
        // verify no startDownloadSync called
        verify(mockRemoteDownloader, times(0)).startDownloadSync();
        // verify messagingImageDownloaderTask did not download file
        assertEquals(null, bitmap);
    }
}
