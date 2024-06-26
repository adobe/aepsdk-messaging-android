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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingCacheUtilitiesTests {
    @Mock CacheService mockCacheService;
    @Mock ServiceProvider mockServiceProvider;
    @Mock CacheResult mockCacheResult;
    @Mock DeviceInforming mockDeviceInfoService;

    MessagingCacheUtilities messagingCacheUtilities;
    InputStream propositionPayloadInputStream;
    InputStream propositionInputStream;
    Proposition proposition;
    PropositionPayload propositionPayload;
    File cacheDir = new File("cache");

    Map<String, Object> propositionItemMap = new HashMap<>();
    Map<String, Object> eventDataMap = new HashMap<>();
    List<PropositionItem> propositionItems = new ArrayList<>();
    List<Map<String, Object>> propositionItemMaps = new ArrayList<>();
    Map<String, String> fakeMetaData = new HashMap<>();

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // setup mock cached PropositionPayloads
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalizationPayloadV1.json"));
        propositionPayload = MessagingTestUtils.getPropositionPayloadsFromMaps(testPayload).get(0);

        ByteArrayOutputStream propositionPayloadBaos = null;
        ObjectOutputStream propositionPayloadOos = null;
        try {
            propositionPayloadBaos = new ByteArrayOutputStream();
            propositionPayloadOos = new ObjectOutputStream(propositionPayloadBaos);
            final List<PropositionPayload> propositionPayloads = new ArrayList<>();
            propositionPayloads.add(propositionPayload);
            final Map<Surface, List<PropositionPayload>> payload = new HashMap<>();
            payload.put(Surface.fromUriString("mobileapp://mockPackageName"), propositionPayloads);
            propositionPayloadOos.writeObject(payload);
            propositionPayloadOos.flush();
            propositionPayloadInputStream =
                    new ByteArrayInputStream(propositionPayloadBaos.toByteArray());
        } catch (IOException ex) {
            final IOException exception = ex;
        } finally {
            if (propositionPayloadBaos != null) {
                propositionPayloadBaos.close();
            }
            if (propositionPayloadOos != null) {
                propositionPayloadOos.close();
            }
        }

        // setup mock cached Propositions
        Map<String, Object> characteristics = new HashMap<>();
        characteristics.put("eventToken", "eventToken");

        Map<String, Object> activity = new HashMap<>();
        activity.put("id", "activityId");

        Map<String, Object> scopeDetails = new HashMap<>();
        scopeDetails.put("decisionProvider", "AJO");
        scopeDetails.put("correlationID", "correlationID");
        scopeDetails.put("characteristics", characteristics);
        scopeDetails.put("activity", activity);

        propositionItemMap = MessagingTestUtils.getMapFromFile("feedPropositionItem.json");
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(propositionItemMap);
        propositionItems.add(propositionItem);
        propositionItemMaps.add(propositionItemMap);
        eventDataMap.put("id", "uniqueId");
        eventDataMap.put("scope", "mobileapp://mockPackageName");
        eventDataMap.put("scopeDetails", scopeDetails);
        eventDataMap.put("items", propositionItemMaps);
        proposition = Proposition.fromEventData(eventDataMap);

        ByteArrayOutputStream propositionBaos = null;
        ObjectOutputStream propositionOos = null;
        try {
            propositionBaos = new ByteArrayOutputStream();
            propositionOos = new ObjectOutputStream(propositionBaos);
            final List<Proposition> propositions = new ArrayList<>();
            propositions.add(proposition);
            final Map<Surface, List<Proposition>> payload = new HashMap<>();
            payload.put(Surface.fromUriString("mobileapp://mockPackageName"), propositions);
            propositionOos.writeObject(payload);
            propositionOos.flush();
            propositionInputStream = new ByteArrayInputStream(propositionBaos.toByteArray());
        } catch (IOException ex) {
            final IOException exception = ex;
        } finally {
            if (propositionBaos != null) {
                propositionBaos.close();
            }
            if (propositionOos != null) {
                propositionOos.close();
            }
        }

        // setup metadata map
        fakeMetaData.put("fakeKey", "fakeValue");
    }

    @After
    public void tearDown() {
        Mockito.reset(mockCacheService);
        Mockito.reset(mockServiceProvider);
        Mockito.reset(mockCacheResult);
        reset(mockDeviceInfoService);
    }

    void runWithMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");

            messagingCacheUtilities = new MessagingCacheUtilities();

            runnable.run();
        }
    }

    @Test
    public void arePropositionsCached_returnsTrue_whenCacheServiceReturnsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY)))
                            .thenReturn(null);

                    // test
                    boolean result = messagingCacheUtilities.arePropositionsCached();

                    // verify
                    assertFalse(result);
                });
    }

    @Test
    public void arePropositionsCached_returnsFalse_whenCacheServiceReturnsNonNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY)))
                            .thenReturn(mockCacheResult);

                    // test
                    boolean result = messagingCacheUtilities.arePropositionsCached();

                    // verify
                    assertTrue(result);
                });
    }

    @Test
    public void testGetCachedPropositions_WhenPropositionPayloadObjectsPreviouslyCached() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(propositionPayloadInputStream);

                    // test
                    final Map<Surface, List<Proposition>> retrievedPayload =
                            messagingCacheUtilities.getCachedPropositions();

                    // verify
                    verify(mockCacheService, times(1)).get(anyString(), anyString());
                    assertNotNull(retrievedPayload);
                    assertEquals(1, retrievedPayload.size());
                });
    }

    @Test
    public void testGetCachedPropositions_WhenPropositionObjectsPreviouslyCached() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(propositionInputStream);

                    // test
                    final Map<Surface, List<Proposition>> retrievedPayload =
                            messagingCacheUtilities.getCachedPropositions();

                    // verify
                    verify(mockCacheService, times(1)).get(anyString(), anyString());
                    assertNotNull(retrievedPayload);
                    assertEquals(1, retrievedPayload.size());
                });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenNoPropositionsCached() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString())).thenReturn(null);

                    // test
                    final Map<Surface, List<Proposition>> retrievedPayload =
                            messagingCacheUtilities.getCachedPropositions();

                    // verify
                    verify(mockCacheService, times(1)).get(anyString(), anyString());
                    assertNull(retrievedPayload);
                });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenCacheFileReturnedIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(null);

                    // test
                    final Map<Surface, List<Proposition>> retrievedPayload =
                            messagingCacheUtilities.getCachedPropositions();

                    // verify
                    verify(mockCacheService, times(1)).get(anyString(), anyString());
                    assertNull(retrievedPayload);
                });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenReadCacheDataIsNull() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    try (MockedConstruction<ObjectInputStream> objectInputStreamMockedConstruction =
                            Mockito.mockConstruction(
                                    ObjectInputStream.class,
                                    (mock, context) -> {
                                        when(mock.readObject()).thenReturn(null);
                                    })) {
                        // test
                        final Map<Surface, List<Proposition>> retrievedPayload =
                                messagingCacheUtilities.getCachedPropositions();

                        // verify
                        verify(mockCacheService, times(1)).get(anyString(), anyString());
                        assertNull(retrievedPayload);
                    }
                });
    }

    @Test
    public void
            testGetCachedPropositionPayload_ReturnsNullPayload_WhenCachedDataElementsAreEmpty() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    try (MockedConstruction<ObjectInputStream> objectInputStreamMockedConstruction =
                            Mockito.mockConstruction(
                                    ObjectInputStream.class,
                                    (mock, context) -> {
                                        Map<Surface, List<Object>> cachedData = new HashMap<>();
                                        when(mock.readObject()).thenReturn(cachedData);
                                    })) {

                        // test
                        final Map<Surface, List<Proposition>> retrievedPayload =
                                messagingCacheUtilities.getCachedPropositions();

                        // verify
                        verify(mockCacheService, times(1)).get(anyString(), anyString());
                        assertNull(retrievedPayload);
                    }
                });
    }

    @Test
    public void
            testGetCachedPropositionPayload_ReturnsNullPayload_WhenClassNotFoundExceptionOccurs() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    try (MockedConstruction<ObjectInputStream> objectInputStreamMockedConstruction =
                            Mockito.mockConstruction(
                                    ObjectInputStream.class,
                                    (mock, context) -> {
                                        when(mock.readObject())
                                                .thenThrow(new ClassNotFoundException());
                                    })) {
                        // test
                        final Map<Surface, List<Proposition>> retrievedPayload =
                                messagingCacheUtilities.getCachedPropositions();

                        // verify
                        verify(mockCacheService, times(1)).get(anyString(), anyString());
                        assertNull(retrievedPayload);
                    }
                });
    }

    @Test
    public void testGetCachedPropositionPayload_ReturnsNullPayload_WhenIOExceptionOccurs() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    try (MockedConstruction<ObjectInputStream> objectInputStreamMockedConstruction =
                            Mockito.mockConstruction(
                                    ObjectInputStream.class,
                                    (mock, context) -> {
                                        doThrow(new IOException()).when(mock).close();
                                    })) {

                        // test
                        final Map<Surface, List<Proposition>> retrievedPayload =
                                messagingCacheUtilities.getCachedPropositions();

                        // verify
                        verify(mockCacheService, times(1)).get(anyString(), anyString());
                        assertNull(retrievedPayload);
                    }
                });
    }

    @Test
    public void
            testGetCachedPropositionPayload_ReturnsNullPayload_WhenCachedPropositionsAreInvalid() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData())
                            .thenReturn(
                                    MessagingTestUtils.convertResourceFileToInputStream(
                                            "invalidRules.json"));

                    // test
                    final Map<Surface, List<Proposition>> retrievedPayload =
                            messagingCacheUtilities.getCachedPropositions();

                    // verify
                    verify(mockCacheService, times(1)).get(anyString(), anyString());
                    assertNull(retrievedPayload);
                });
    }

    @Test
    public void testCachePropositionPayload() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(propositionInputStream);

                    final List<Proposition> list = new ArrayList<>();
                    list.add(proposition);
                    final Map<Surface, List<Proposition>> propositions = new HashMap<>();
                    propositions.put(new Surface(), list);

                    // test
                    messagingCacheUtilities.cachePropositions(propositions, Collections.EMPTY_LIST);

                    // verify
                    verify(mockCacheService, times(1))
                            .set(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY),
                                    any());
                });
    }

    @Test
    public void testCachePropositionPayload_WithSurfacesToRemove() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(propositionInputStream);

                    final List<Proposition> list = new ArrayList<>();
                    list.add(proposition);
                    final Map<Surface, List<Proposition>> propositions = new HashMap<>();
                    propositions.put(new Surface(), list);

                    // test
                    messagingCacheUtilities.cachePropositions(
                            propositions, Arrays.asList(new Surface()));

                    // verify
                    verify(mockCacheService, times(0))
                            .set(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY),
                                    any());
                });
    }

    @Test
    public void testCachePropositionPayload_EmptyPropositions() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(null);

                    final List<Proposition> list = new ArrayList<>();
                    list.add(proposition);
                    final Map<Surface, List<Proposition>> propositions = new HashMap<>();

                    // test
                    messagingCacheUtilities.cachePropositions(propositions, Collections.EMPTY_LIST);

                    // verify cache cleared as no propositions were present
                    verify(mockCacheService, times(1))
                            .remove(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY));
                });
    }

    @Test
    public void
            testCachePropositionPayload_DoesNotSetCache_WhenIOExceptionOccursWhenWritingObject() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(propositionInputStream);
                    try (MockedConstruction<ObjectOutputStream>
                            objectOutputStreamMockedConstruction =
                                    Mockito.mockConstruction(
                                            ObjectOutputStream.class,
                                            (mock, context) -> {
                                                doThrow(new IOException())
                                                        .when(mock)
                                                        .writeObject(any(Map.class));
                                            })) {

                        final List<Proposition> list = new ArrayList<>();
                        list.add(proposition);
                        final Map<Surface, List<Proposition>> propositions = new HashMap<>();

                        // test
                        messagingCacheUtilities.cachePropositions(
                                propositions, Collections.EMPTY_LIST);

                        // verify cache not set
                        verify(mockCacheService, times(0)).set(anyString(), anyString(), any());
                    }
                });
    }

    @Test
    public void
            testCachePropositionPayload_SetsCache_WhenIOExceptionOccursWhenClosingObjectOutputStream() {
        runWithMockedServiceProvider(
                () -> {
                    // setup
                    when(mockCacheService.get(anyString(), anyString()))
                            .thenReturn(mockCacheResult);
                    when(mockCacheResult.getMetadata()).thenReturn(fakeMetaData);
                    when(mockCacheResult.getData()).thenReturn(propositionInputStream);
                    try (MockedConstruction<ObjectOutputStream>
                            objectOutputStreamMockedConstruction =
                                    Mockito.mockConstruction(
                                            ObjectOutputStream.class,
                                            (mock, context) -> {
                                                doThrow(new IOException()).when(mock).close();
                                            })) {
                        final List<Proposition> list = new ArrayList<>();
                        list.add(proposition);
                        final Map<Surface, List<Proposition>> propositions = new HashMap<>();

                        // test
                        messagingCacheUtilities.cachePropositions(
                                propositions, Collections.EMPTY_LIST);

                        // verify
                        verify(mockCacheService, times(1))
                                .set(
                                        eq(MessagingTestConstants.CACHE_BASE_DIR),
                                        eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY),
                                        any());
                    }
                });
    }

    @Test
    public void testClearCache() {
        runWithMockedServiceProvider(
                () -> {
                    // test
                    messagingCacheUtilities.clearCachedData();

                    // verify
                    verify(mockCacheService, times(1))
                            .remove(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.PROPOSITIONS_CACHE_SUBDIRECTORY));
                    verify(mockCacheService, times(1))
                            .remove(
                                    eq(MessagingTestConstants.CACHE_BASE_DIR),
                                    eq(MessagingTestConstants.IMAGES_CACHE_SUBDIRECTORY));
                });
    }
}
