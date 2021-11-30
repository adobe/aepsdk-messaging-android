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

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
public class MessageCachingTests {
    @Mock
    CacheManager mockCacheManager;

    @Before
    public void setup() {

    }

    @Test
    public void testLoadCachedMessages() throws URISyntaxException {
        // setup
        final Map<String, Variant> payload = TestUtils.loadJsonFromFile("show_once");
        final File cachedFile = new File(TestUtils.class.getClassLoader().getResource("show_once.json").toURI());
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
        // test
        final Map<String, Variant> retrievedPayload = MessagingUtils.getCachedMessages(mockCacheManager);
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify payload retrieved
        assertEquals(payload, retrievedPayload);
    }

    @Test
    public void testLoadCachedMessages_WhenNoMessageCached() {
        // test
        final Map<String, Variant> retrievedPayload = MessagingUtils.getCachedMessages(mockCacheManager);
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testLoadCachedMessages_WhenCacheManagerIsNull() {
        // test
        final Map<String, Variant> retrievedPayload = MessagingUtils.getCachedMessages(null);
        // verify getFileForCachedURL not called
        verify(mockCacheManager, times(0)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testLoadCachedMessages_WhenCacheFileNotFound() {
        // setup
        final File cachedFile = null;
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
        // test
        final Map<String, Variant> retrievedPayload = MessagingUtils.getCachedMessages(mockCacheManager);
        // verify getFileForCachedURL not called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }


    @Test
    public void testLoadCachedMessages_WhenCacheMessageIsInvalid() throws URISyntaxException {
        // setup
        final File cachedFile = new File(TestUtils.class.getClassLoader().getResource("invalid.json").toURI());
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
        // test
        final Map<String, Variant> retrievedPayload = MessagingUtils.getCachedMessages(mockCacheManager);
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testCacheMessagePayload() {
        // setup
        final Map<String, Variant> payload = TestUtils.loadJsonFromFile("show_once");
        // test
        MessagingUtils.cacheRetrievedMessages(mockCacheManager, payload);
        // verify deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(any(List.class), anyString(), anyBoolean());
        // verify createNewCacheFile called
        verify(mockCacheManager, times(1)).createNewCacheFile(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void testClearCache() {
        // test
        MessagingUtils.clearCachedMessages(mockCacheManager);
        // verify deleteFilesNotInList called
        verify(mockCacheManager, times(1)).deleteFilesNotInList(any(List.class), anyString(), anyBoolean());
    }
}