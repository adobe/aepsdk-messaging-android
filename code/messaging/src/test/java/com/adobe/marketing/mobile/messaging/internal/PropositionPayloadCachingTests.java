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

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
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
    @Mock
    PropositionPayload mockPropositionPayload;

    private MessagingCacheUtilities messagingCacheUtilities;
    private InputStream propositionInputStream;
    final Map<String, String> fakeMetaData = new HashMap<>();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            final List<PropositionPayload> list = new ArrayList<>();
            list.add(mockPropositionPayload);
            oos.writeObject(list);
            oos.flush();
            propositionInputStream = new ByteArrayInputStream(baos.toByteArray());


        } catch (IOException ex) {
            final IOException exception = ex;
        } finally {
            if (baos != null) {
                baos.close();
            }
            if (oos != null) {
                oos.close();
            }
        }
        fakeMetaData.put("fakeKey", "fakeValue");
    }

    @After
    public void tearDown() {
        Mockito.reset(mockCacheService);
        Mockito.reset(mockServiceProvider);
        Mockito.reset(mockCacheResult);
    }

    void runWithMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);

            messagingCacheUtilities = new MessagingCacheUtilities();

            runnable.run();
        }
    }

    @Test
    public void testGetCachedPropositionPayload() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
            when(mockCacheResult.getData()).thenReturn(propositionInputStream);

            // test
            final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();

            // verify
            verify(mockCacheService, times(1)).get(anyString(), anyString());
            assertNotNull(retrievedPayload);
            assertEquals(1, retrievedPayload.size());
        });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenNoPropositionsCached() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockCacheService.get(anyString(), anyString())).thenReturn(null);

            // test
            final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();

            // verify
            verify(mockCacheService, times(1)).get(anyString(), anyString());
            assertNull(retrievedPayload);
        });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenCacheFileReturnedIsNull() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
            when(mockCacheResult.getData()).thenReturn(null);

            // test
            final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();

            // verify
            verify(mockCacheService, times(1)).get(anyString(), anyString());
            assertNull(retrievedPayload);
        });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenCachedPropositionsAreInvalid() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
            when(mockCacheResult.getData()).thenReturn(MessagingTestUtils.convertResourceFileToInputStream("invalid.json"));

            // test
            final List<PropositionPayload> retrievedPayload = messagingCacheUtilities.getCachedPropositions();

            // verify
            verify(mockCacheService, times(1)).get(anyString(), anyString());
            assertNull(retrievedPayload);
        });
    }

    @Test
    public void testCachePropositionPayload() {
        runWithMockedServiceProvider(() -> {
            // setup
            when(mockCacheService.get(anyString(), anyString())).thenReturn(mockCacheResult);
            when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
            when(mockCacheResult.getData()).thenReturn(propositionInputStream);

            final List<PropositionPayload> list = new ArrayList<>();
            list.add(mockPropositionPayload);

            // test
            messagingCacheUtilities.cachePropositions(list);

            // verify
            verify(mockCacheService, times(1)).set(eq(MessagingConstants.CACHE_BASE_DIR), eq(MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY), any());
        });
    }

    @Test
    public void testClearCache() {
        runWithMockedServiceProvider(() -> {
            // test
            messagingCacheUtilities.clearCachedData();

            // verify
            verify(mockCacheService, times(1)).remove(eq(MessagingConstants.CACHE_BASE_DIR), eq(MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY));
            verify(mockCacheService, times(1)).remove(eq(MessagingConstants.CACHE_BASE_DIR), eq(MessagingConstants.IMAGES_CACHE_SUBDIRECTORY));
        });
    }
}