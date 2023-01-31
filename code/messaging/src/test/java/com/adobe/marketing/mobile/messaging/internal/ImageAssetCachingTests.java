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

package com.adobe.marketing.mobile.messaging.internal;

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

import java.util.ArrayList;
import java.util.List;

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
    private MessagingCacheUtilities messagingCacheUtilities;

    @Before
    public void setup() {
        messagingCacheUtilities = new MessagingCacheUtilities();
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockUIService);
        reset(mockCacheService);
        reset(mockCacheResult);
        reset(mockDeviceInfoService);
        reset(mockNetworkService);
    }

    private void setupServiceProviderMockAndRunTest(Runnable testRunnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheService.set(anyString(), anyString(), any(CacheEntry.class))).thenReturn(true);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getUIService()).thenReturn(mockUIService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);

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
}