/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ImageAssetCachingTests {

    // Mocks
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

    private final static String IMAGE_URL = "https://www.adobe.com/adobe.png";
    private final static String IMAGE_URL2 = "https://www.adobe.com/adobe2.png";
    private final File testCacheDir = new File("testCache");
    private MessagingCacheUtilities messagingCacheUtilities;

    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockUIService);
        reset(mockCacheService);
        reset(mockCacheResult);
        reset(mockDeviceInfoService);
        reset(mockNetworkService);
        if (testCacheDir.exists()) {
            testCacheDir.delete();
        }
    }

    private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            Map<String, String> metaData = new HashMap<>();
            metaData.put(MessagingConstants.HTTP_HEADER_ETAG, "etag");
            metaData.put(MessagingConstants.HTTP_HEADER_LAST_MODIFIED, "lastModified");
            metaData.put(MessagingConstants.METADATA_PATH, "testCachedAssetPath");
            when(mockCacheResult.getMetadata()).thenReturn(metaData);
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheService.set(anyString(), anyString(), any(CacheEntry.class))).thenReturn(true);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(testCacheDir);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);
            messagingCacheUtilities = new MessagingCacheUtilities();

            testRunnable.run();
        }
    }

    @Test
    public void testCacheImageAssets_ValidImageAssetListTriggersRemoteAssetFetch() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            ArgumentCaptor<NetworkRequest> networkRequestArgumentCaptor = ArgumentCaptor.forClass(NetworkRequest.class);
            final List<String> imageAssets = new ArrayList<>();
            imageAssets.add(IMAGE_URL);
            imageAssets.add(IMAGE_URL2);
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify 2 network requests made containing the 2 image URL's
            verify(mockNetworkService, times(2)).connectAsync(networkRequestArgumentCaptor.capture(), any(NetworkCallback.class));
            List<NetworkRequest> networkRequestList = networkRequestArgumentCaptor.getAllValues();
            assertEquals(2, networkRequestList.size());
            NetworkRequest firstRequest = networkRequestList.get(0);
            NetworkRequest secondRequest = networkRequestList.get(1);
            assertEquals(IMAGE_URL, firstRequest.getUrl());
            assertEquals(IMAGE_URL2, secondRequest.getUrl());
        });
    }

    @Test
    public void testCacheImageAssets_MissingAssetCacheDirectory_Then_AssetsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(null);
            messagingCacheUtilities = new MessagingCacheUtilities();
            final List<String> imageAssets = new ArrayList<>();
            imageAssets.add(IMAGE_URL);
            imageAssets.add(IMAGE_URL2);
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify no network requests made because the asset cache is not available
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
        });
    }

    @Test
    public void testCacheImageAssets_CacheServiceNotAvailable_Then_AssetsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            when(mockServiceProvider.getCacheService()).thenReturn(null);
            messagingCacheUtilities = new MessagingCacheUtilities();
            final List<String> imageAssets = new ArrayList<>();
            imageAssets.add(IMAGE_URL);
            imageAssets.add(IMAGE_URL2);
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify no network requests made because the cache service is not available
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
        });
    }

    @Test
    public void testCacheImageAssets_EmptyImageAssetListDoesNotTriggerRemoteAssetFetch() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            final List<String> imageAssets = new ArrayList<>();
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify 0 network requests
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
        });
    }

    @Test
    public void testCacheImageAssets_CacheServiceNotAvailable() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            when(mockServiceProvider.getCacheService()).thenReturn(null);
            final List<String> imageAssets = new ArrayList<>();
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify 0 network requests
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
        });
    }

    @Test
    public void testCacheImageAssets_AssetNotDownloadable_Then_AssetsNotCached() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            final List<String> imageAssets = new ArrayList<>();
            imageAssets.add("someInvalidURL");
            imageAssets.add("mobileapp://someURL");

            // test
            messagingCacheUtilities = new MessagingCacheUtilities();
            messagingCacheUtilities.cacheImageAssets(imageAssets);

            // verify no network requests made because the cache service is not available
            verify(mockNetworkService, times(0)).connectAsync(any(NetworkRequest.class), any(NetworkCallback.class));
        });
    }

    @Test
    public void testCacheImageAssets_DuplicateImagesTriggersRemoteAssetFetchOnceOnly() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            ArgumentCaptor<NetworkRequest> networkRequestArgumentCaptor = ArgumentCaptor.forClass(NetworkRequest.class);
            final List<String> imageAssets = new ArrayList<>();
            imageAssets.add(IMAGE_URL);
            imageAssets.add(IMAGE_URL);
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify 1 network requests made containing the 1 image URL's
            verify(mockNetworkService, times(1)).connectAsync(networkRequestArgumentCaptor.capture(), any(NetworkCallback.class));
            List<NetworkRequest> networkRequestList = networkRequestArgumentCaptor.getAllValues();
            assertEquals(1, networkRequestList.size());
            NetworkRequest firstRequest = networkRequestList.get(0);
            assertEquals(IMAGE_URL, firstRequest.getUrl());
        });
    }

    @Test
    public void testGetAssetMap() {
        // setup
        setupServiceProviderMockAndRunTest(() -> {
            ArgumentCaptor<NetworkRequest> networkRequestArgumentCaptor = ArgumentCaptor.forClass(NetworkRequest.class);
            final List<String> imageAssets = new ArrayList<>();
            imageAssets.add(IMAGE_URL);
            imageAssets.add(IMAGE_URL2);
            // test
            messagingCacheUtilities.cacheImageAssets(imageAssets);
            // verify 2 network requests made containing the 2 image URL's
            verify(mockNetworkService, times(2)).connectAsync(networkRequestArgumentCaptor.capture(), any(NetworkCallback.class));
            List<NetworkRequest> networkRequestList = networkRequestArgumentCaptor.getAllValues();
            assertEquals(2, networkRequestList.size());
            NetworkRequest firstRequest = networkRequestList.get(0);
            NetworkRequest secondRequest = networkRequestList.get(1);
            assertEquals(IMAGE_URL, firstRequest.getUrl());
            assertEquals(IMAGE_URL2, secondRequest.getUrl());
            // test
            Map assetMap = messagingCacheUtilities.getAssetsMap();
            // verify
            assertEquals(2, assetMap.size());
            assertEquals("testCache/messaging/images", assetMap.get(IMAGE_URL));
            assertEquals("testCache/messaging/images", assetMap.get(IMAGE_URL2));
        });
    }
}