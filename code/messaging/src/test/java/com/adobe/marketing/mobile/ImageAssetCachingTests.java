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

package com.adobe.marketing.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.PackageManager;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class ImageAssetCachingTests {
    private final static String IMAGE_URL = "https://www.adobe.com/adobe.png";
    private final static String IMAGE_URL2 = "https://www.adobe.com/adobe2.png";
    // Mocks
    @Mock
    Core mockCore;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    AndroidSystemInfoService mockAndroidSystemInfoService;
    @Mock
    AndroidNetworkService mockAndroidNetworkService;
    @Mock
    SystemInfoService mockSystemInfoService;
    @Mock
    NetworkService mockNetworkService;
    @Mock
    CacheManager mockCacheManager;
    private EventHub eventHub;
    private MessagingCacheUtilities messagingCacheUtilities;

    @Before
    public void setup() throws PackageManager.NameNotFoundException, MissingPlatformServicesException {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();
        setupPlatformServicesMocks();
    }

    void setupMocks() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        Mockito.when(MobileCore.getCore()).thenReturn(mockCore);
    }

    void setupPlatformServicesMocks() {
        // setup services mocks
        Mockito.when(mockPlatformServices.getSystemInfoService()).thenReturn(mockAndroidSystemInfoService);
        Mockito.when(mockPlatformServices.getNetworkService()).thenReturn(mockAndroidNetworkService);

        // setup mock cache
        final File mockCache = new File("mock_cache");
        Mockito.when(mockAndroidSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);
    }

    @Test(expected = MissingPlatformServicesException.class)
    public void testCreateMessagingCacheUtilities_nullCacheManager() throws MissingPlatformServicesException {
        // test
        messagingCacheUtilities = new MessagingCacheUtilities(mockSystemInfoService, mockNetworkService, null);
        // verify messaging cache utilities object wasn't created
        assertNull(messagingCacheUtilities);
    }

    @Test
    public void testCacheImageAssets_ValidImageAssetListTriggersRemoteAssetFetch() throws MissingPlatformServicesException {
        // setup
        messagingCacheUtilities = new MessagingCacheUtilities(mockSystemInfoService, mockNetworkService, mockCacheManager);
        ArgumentCaptor<List> assetListCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NetworkService.Callback> callbackCaptor = ArgumentCaptor.forClass(NetworkService.Callback.class);
        final List<String> imageAssets = new ArrayList<>();
        imageAssets.add(IMAGE_URL);
        imageAssets.add(IMAGE_URL2);
        // test
        messagingCacheUtilities.cacheImageAssets(imageAssets);
        // verify cache manager deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(assetListCaptor.capture(), anyString());
        assertEquals(IMAGE_URL, assetListCaptor.getValue().get(0));
        assertEquals(IMAGE_URL2, assetListCaptor.getValue().get(1));
        // verify 2 network requests made containing the remote downloader in the callback
        verify(mockNetworkService, times(2)).connectUrlAsync(anyString(),
                any(NetworkService.HttpCommand.class),
                ArgumentMatchers.<byte[]>isNull(), ArgumentMatchers.<Map<String, String>>isNull(), anyInt(), anyInt(), callbackCaptor.capture());
        assertEquals(RemoteDownloader.class, callbackCaptor.getValue().getClass().getEnclosingClass());
    }

    @Test
    public void testCacheImageAssets_EmptyImageAssetListDoesNotTriggerRemoteAssetFetch() throws MissingPlatformServicesException {
        // setup
        messagingCacheUtilities = new MessagingCacheUtilities(mockSystemInfoService, mockNetworkService, mockCacheManager);
        ArgumentCaptor<List> assetListCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NetworkService.Callback> callbackCaptor = ArgumentCaptor.forClass(NetworkService.Callback.class);
        final List<String> imageAssets = new ArrayList<>();
        // test
        messagingCacheUtilities.cacheImageAssets(imageAssets);
        // verify cache manager deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(assetListCaptor.capture(), anyString());
        assertEquals(0, assetListCaptor.getValue().size());
        // verify 0 network requests made
        verify(mockNetworkService, times(0)).connectUrlAsync(anyString(),
                any(NetworkService.HttpCommand.class),
                ArgumentMatchers.<byte[]>isNull(), ArgumentMatchers.<Map<String, String>>isNull(), anyInt(), anyInt(), any(NetworkService.Callback.class));
    }

    @Test(expected = MissingPlatformServicesException.class)
    public void testCacheImageAssets_SystemInfoServiceNotAvailable() throws MissingPlatformServicesException {
        // test
        messagingCacheUtilities = new MessagingCacheUtilities(mockSystemInfoService, null, mockCacheManager);
        // verify messaging cache utilities object wasn't created
        assertNull(messagingCacheUtilities);
    }

    @Test(expected = MissingPlatformServicesException.class)
    public void testCacheImageAssets_NetworkServiceNotAvailable() throws MissingPlatformServicesException {
        // test
        messagingCacheUtilities = new MessagingCacheUtilities(null, mockNetworkService, mockCacheManager);
        // verify messaging cache utilities object wasn't created
        assertNull(messagingCacheUtilities);
    }
}