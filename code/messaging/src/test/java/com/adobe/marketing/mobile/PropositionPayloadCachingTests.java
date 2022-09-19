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

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, MessagingUtils.class})
public class PropositionPayloadCachingTests {
    @Mock
    CacheManager mockCacheManager;
    @Mock
    SystemInfoService mockSystemInfoService;
    @Mock
    NetworkService mockNetworkService;

    private MessagingCacheUtilities messagingCacheUtilities;
    private final File cachedProposition = new File("cached_proposition");

    @Before
    public void setup() throws Exception {
        when(mockCacheManager.createNewCacheFile(anyString(), anyString(), any(Date.class))).thenReturn(cachedProposition);
        messagingCacheUtilities = new MessagingCacheUtilities(mockSystemInfoService, mockNetworkService, mockCacheManager);
    }

    @After
    public void tearDown() {
        if (!cachedProposition.exists()) {
            cachedProposition.delete();
        }
    }

    @Test
    public void testGetCachedPropositionPayload() {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload.json"));
        final List<PropositionPayload> payload = MessagingUtils.createPropositionPayload(testPayload);
        messagingCacheUtilities.cachePropositions(payload);
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedProposition);
        // test
        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify cached payload retrieved
        assertEquals(MessagingTestUtils.convertPayloadToString(payload), MessagingTestUtils.convertPayloadToString(retrievedPayload));
    }

    @Test
    public void testGetCachedPropositionPayload_WhenNoPropositionsCached() {
        // test
        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testGetCachedPropositionPayload_WhenCacheFileReturnedIsNull() {
        // setup
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(null);
        // test
        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testGetCachedPropositionPayload_WhenCachedPropositionsAreInvalid() throws URISyntaxException {
        // setup
        final File cachedFile = new File(MessagingTestUtils.class.getClassLoader().getResource("invalid.json").toURI());
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
        // test
        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
        // verify getFileForCachedURL called
        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
        // verify null payload retrieved
        assertEquals(null, retrievedPayload);
    }

    @Test
    public void testCachePropositionPayload() throws URISyntaxException {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload.json"));
        final List<PropositionPayload> payload = MessagingUtils.createPropositionPayload(testPayload);
        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedProposition);
        // test
        messagingCacheUtilities.cachePropositions(payload);
        // verify deleteFilesNotInList called 2 times as image and proposition cache are deleted
        verify(mockCacheManager, times(2)).deleteFilesNotInList(ArgumentMatchers.<List<String>>isNull(), anyString(), anyBoolean());
        // verify createNewCacheFile called
        verify(mockCacheManager, times(1)).createNewCacheFile(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void testClearCache() {
        // test
        messagingCacheUtilities.clearCachedDataFromSubdirectory();
        // verify deleteFilesNotInList called 2 times as image and proposition cache are deleted
        verify(mockCacheManager, times(2)).deleteFilesNotInList(ArgumentMatchers.<List<String>>isNull(), anyString(), anyBoolean());
    }
}