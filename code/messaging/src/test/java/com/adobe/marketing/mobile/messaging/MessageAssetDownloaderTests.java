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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.NetworkCallback;
import com.adobe.marketing.mobile.services.NetworkRequest;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.ui.UIService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessageAssetDownloaderTests {
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    UIService mockUIService;
    @Mock
    CacheService mockCacheService;
    @Mock
    CacheResult mockCacheResult;
    @Mock
    DeviceInforming mockDeviceInfoService;
    @Mock
    Networking mockNetworkService;
    @Mock
    HttpConnecting mockHttpConnection;

    private MessageAssetDownloader messageAssetsDownloader;
    private ArrayList<String> assets;
    private String expectedCacheLocation;
    private File testCacheDir;
    private HashMap<String, String> metadataMap;
    private static final String assetUrl = "https://www.adobe.com/logo.png";
    private static final String MESSAGES_CACHE = MessagingConstants.CACHE_BASE_DIR + File.separator + MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;

    @Before
    public void setup() {
        String sha256HashForRemoteUrl = "fb0d3704b73d5fa012a521ea31013a61020e79610a3c27e8dd1007f3ec278195";
        String cachedFileName = sha256HashForRemoteUrl + ".12345"; //12345 is just a random extension.
        metadataMap = new HashMap<>();
        metadataMap.put(MessagingConstants.METADATA_PATH, MESSAGES_CACHE + File.separator + cachedFileName);

        // setup assets for testing
        assets = new ArrayList<>();
        assets.add(assetUrl);
        expectedCacheLocation = "testCache/messaging/images";
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockUIService);
        reset(mockCacheService);
        reset(mockCacheResult);
        reset(mockDeviceInfoService);
        reset(mockNetworkService);
        reset(mockHttpConnection);
        clearCacheFiles(testCacheDir);
    }

    /**
     * Deletes the directory and all files inside it.
     *
     * @param file instance of {@link File} points to the directory need to be deleted.
     */
    private static void clearCacheFiles(final File file) {
        // clear files from directory first
        if (file.isDirectory()) {
            String[] children = file.list();

            if (children != null) {
                for (final String child : children) {
                    final File childFile = new File(file, child);
                    clearCacheFiles(childFile);
                }
            }
        }

        file.delete(); // delete file or empty directory
    }

    private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
        testCacheDir = new File("testCache");
        testCacheDir.mkdirs();
        testCacheDir.setWritable(true);
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheService.set(anyString(), anyString(), any(CacheEntry.class))).thenReturn(true);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(testCacheDir);
            // create MessageAssetDownloader instance
            messageAssetsDownloader = new MessageAssetDownloader(assets);
            testRunnable.run();
        }
    }

    @Test
    public void testConstructor_when_assetLocationIsNull_then_AssetCacheDirectoryIsNotCreated() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            File mockCacheDir = mock(File.class);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(null);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(mockCacheDir);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(null);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(mockCacheDir);

            // test
            messageAssetsDownloader = new MessageAssetDownloader(assets);

            // verify
            verifyNoInteractions(mockCacheDir);
        });
    }

    @Test
    public void testConstructor_when_assetLocationIsValid_and_assetDirDoesNotExist_then_assetDirectoryIsCreated() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            final File[] mockedFile = new File[1];
            try (MockedConstruction<File> fileMockedConstruction = Mockito.mockConstruction(File.class,
                    (mock, context) -> {
                        when(mock.exists()).thenReturn(false);
                        mockedFile[0] = mock;
                    })) {

                // test
                messageAssetsDownloader = new MessageAssetDownloader(assets);

                // verify
                verify(mockedFile[0], times(1)).mkdirs();
            }
        });
    }

    // ====================================================================================================
    // void downloadAssetCollection()
    // ====================================================================================================
    @Test
    public void testDownloadAssetCollection_when_assetNotInCache_then_assetIsCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(mockHttpConnection.getInputStream()).thenReturn(new ByteArrayInputStream("assetData".getBytes(StandardCharsets.UTF_8)));
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            // test
            messageAssetsDownloader.downloadAssetCollection();
            // verify
            verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify asset cached
            verify(mockCacheService, times(1)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
        });
    }

    @Test
    public void testDownloadAssetCollection_when_existingAssetInCache_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_MODIFIED);
            when(mockHttpConnection.getInputStream()).thenReturn(new ByteArrayInputStream("assetData".getBytes(StandardCharsets.UTF_8)));
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            // test
            messageAssetsDownloader.downloadAssetCollection();
            // verify
            verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify asset not cached
            verify(mockCacheService, times(0)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetInCacheIsNotForActiveMessage_then_cachedAssetIsDeleted() throws
            Exception {
        // setup
        final File existingCacheDir = new File("testCache/messaging/images/d38a46f6-4f43-435a-a862-4038c27b90a1");
        existingCacheDir.mkdirs();
        final File existingCachedFile = new
                File("testCache/messaging/images/d38a46f6-4f43-435a-a862-4038c27b90a1/028dbbd3617ccfb5e302f4aa2df2eb312d1571ee40b3f4aa448658c9082b0411");
        existingCachedFile.createNewFile();

        setupServiceProviderMockAndRunTest(() -> {
            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            when(mockHttpConnection.getInputStream()).thenReturn(new ByteArrayInputStream("assetData".getBytes(StandardCharsets.UTF_8)));
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            // test
            messageAssetsDownloader.downloadAssetCollection();
            // verify
            verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify new asset cached
            verify(mockCacheService, times(1)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
            // verify non matching cached asset deleted
            assertFalse(existingCachedFile.exists());
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetIsNotDownloadable_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {

            when(mockHttpConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
            doAnswer((Answer<Void>) invocation -> {
                NetworkCallback callback = invocation.getArgument(1);
                callback.call(mockHttpConnection);
                return null;
            }).when(mockNetworkService)
                    .connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));

            // test
            messageAssetsDownloader.downloadAssetCollection();
            // verify
            verify(mockCacheService, times(1)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(1)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify asset not cached
            verify(mockCacheService, times(0)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetLocationIsNull_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(null);

            // test
            messageAssetsDownloader = new MessageAssetDownloader(assets);
            messageAssetsDownloader.downloadAssetCollection();

            // verify
            verify(mockCacheService, times(0)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify asset cached
            verify(mockCacheService, times(0)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetIsNull_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            // test
            messageAssetsDownloader = new MessageAssetDownloader(null);
            messageAssetsDownloader.downloadAssetCollection();

            // verify
            verify(mockCacheService, times(0)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify asset cached
            verify(mockCacheService, times(0)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
        });
    }

    @Test
    public void testDownloadAssetCollection_when_assetIsEmpty_then_assetIsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            // test
            messageAssetsDownloader = new MessageAssetDownloader(new ArrayList<>());
            messageAssetsDownloader.downloadAssetCollection();

            // verify
            verify(mockCacheService, times(0)).get(eq(expectedCacheLocation), eq(assetUrl));
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
            // verify asset cached
            verify(mockCacheService, times(0)).set(eq(expectedCacheLocation), eq(assetUrl), any(CacheEntry.class));
        });
    }
}
