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

import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheEntry;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.internal.caching.FileCacheService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionPayloadCachingTests {
    @Mock
    CacheService mockCacheService;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    CacheResult mockCacheResult;

    private MessagingCacheUtilities messagingCacheUtilities = new MessagingCacheUtilities();
    private final File cachedProposition = new File("cached_proposition");
    private InputStream propositionInputStream;
    final Map<String, String> fakeMetaData = new HashMap<>();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
//        serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class);



        try {
            propositionInputStream = new FileInputStream(cachedProposition);
        } catch (IOException ex) { }
        fakeMetaData.put("fakeKey", "fakeValue");
    }

    @After
    public void tearDown() {
//        serviceProviderMockedStatic.close();
    }

    @Test
    public void testGetCachedPropositionPayload() {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);

            // setup
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
            when(mockCacheResult.getData()).thenReturn(propositionInputStream);

            // test
            final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();

            // verify getFileForCachedURL called
            verify(mockCacheService, times(1)).get(anyString(), anyString());
        }


    }
//
//    @Test
//    public void testGetCachedPropositionPayload_WhenNoPropositionsCached() {
//        // test
//        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
//        // verify getFileForCachedURL called
//        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
//        // verify null payload retrieved
//        assertEquals(null, retrievedPayload);
//    }
//
//    @Test
//    public void testGetCachedPropositionPayload_WhenCacheFileReturnedIsNull() {
//        // setup
//        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(null);
//        // test
//        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
//        // verify getFileForCachedURL called
//        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
//        // verify null payload retrieved
//        assertEquals(null, retrievedPayload);
//    }
//
//    @Test
//    public void testGetCachedPropositionPayload_WhenCachedPropositionsAreInvalid() throws URISyntaxException {
//        // setup
//        final File cachedFile = new File(MessagingTestUtils.class.getClassLoader().getResource("invalid.json").toURI());
//        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedFile);
//        // test
//        final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();
//        // verify getFileForCachedURL called
//        verify(mockCacheManager, times(1)).getFileForCachedURL(anyString(), anyString(), anyBoolean());
//        // verify null payload retrieved
//        assertEquals(null, retrievedPayload);
//    }
//
//    @Test
//    public void testCachePropositionPayload() {
//        // setup
//        final List<Map<String, Object>> testPayload = new ArrayList<>();
//        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload.json"));
//        final List<PropositionPayload> payload = MessagingUtils.getPropositionPayloads(testPayload);
//        when(mockCacheManager.getFileForCachedURL(anyString(), anyString(), anyBoolean())).thenReturn(cachedProposition);
//        // test
//        messagingCacheUtilities.cachePropositions(payload);
//        // verify deleteFilesNotInList called 2 times as image and proposition cache are deleted
//        verify(mockCacheManager, times(2)).deleteFilesNotInList(ArgumentMatchers.<List<String>>isNull(), anyString(), anyBoolean());
//        // verify createNewCacheFile called
//        verify(mockCacheManager, times(1)).createNewCacheFile(anyString(), anyString(), any(Date.class));
//    }
//
//    @Test
//    public void testClearCache() {
//        // test
//        messagingCacheUtilities.clearCachedData();
//        // verify deleteFilesNotInList called 2 times as image and proposition cache are deleted
//        verify(mockCacheManager, times(2)).deleteFilesNotInList(ArgumentMatchers.<List<String>>isNull(), anyString(), anyBoolean());
//    }
}