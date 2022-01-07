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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class MessagePayloadCachingTests {
    @Mock
    CacheManager mockCacheManager;
    @Mock
    SystemInfoService mockSystemInfoService;
    @Mock
    NetworkService mockNetworkService;

    private MessagingCacheUtilities messagingCacheUtilities;

    @Before
    public void setup() throws MissingPlatformServicesException {
        messagingCacheUtilities = new MessagingCacheUtilities(mockSystemInfoService, mockNetworkService);
        messagingCacheUtilities.setCacheManager(mockCacheManager);
    }

    @Test
    public void testGetCachedMessages() throws URISyntaxException {
        // setup
        final Map<String, Variant> payload = TestUtils.getMapFromFile("show_once");
        final File cachedFile = new File(TestUtils.class.getClassLoader().getResource("show_once.json").toURI());
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
        // test
        final Map<String, Variant> retrievedPayload = messagingCacheUtilities.getCachedMessages();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify payload retrieved
        assertEquals(payload, retrievedPayload);
    }

    @Test
    public void testGetCachedMessages_WhenNoMessageCached() {
        // test
        final Map<String, Variant> retrievedPayload = messagingCacheUtilities.getCachedMessages();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testGetCachedMessages_WhenCacheFileReturnedIsNull() {
        // setup
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(null);
        // test
        final Map<String, Variant> retrievedPayload = messagingCacheUtilities.getCachedMessages();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testGetCachedMessages_WhenCacheMessageIsInvalid() throws URISyntaxException {
        // setup
        final File cachedFile = new File(TestUtils.class.getClassLoader().getResource("invalid.json").toURI());
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
        // test
        final Map<String, Variant> retrievedPayload = messagingCacheUtilities.getCachedMessages();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testCacheMessagePayload() throws URISyntaxException {
        // setup
        final Map<String, Variant> payload = TestUtils.getMapFromFile("show_once");
        final File cachedMessageLocation = new File(TestUtils.class.getClassLoader().getResource("cached_message.json").toURI());
        when(mockCacheManager.createNewCacheFile(anyString(), anyString(), any(Date.class))).thenReturn(cachedMessageLocation);
        // test
        messagingCacheUtilities.cacheRetrievedMessages(payload);
        // verify deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(any(List.class), anyString(), anyBoolean());
        // verify createNewCacheFile called
        verify(mockCacheManager, times(1)).createNewCacheFile(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void testCacheMessagePayload_WhenPayloadIsNull() throws URISyntaxException {
        // setup
        final Map<String, Variant> payload = null;
        // test
        messagingCacheUtilities.cacheRetrievedMessages(payload);
        // verify deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(any(List.class), anyString(), anyBoolean());
        // verify createNewCacheFile not called
        verify(mockCacheManager, times(0)).createNewCacheFile(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void testClearCache() {
        // test
        messagingCacheUtilities.clearCachedDataFromSubdirectory(MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY);
        // verify deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(any(List.class), anyString(), anyBoolean());
    }
}