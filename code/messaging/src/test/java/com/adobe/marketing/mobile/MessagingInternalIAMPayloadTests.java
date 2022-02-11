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

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES;
import static com.adobe.marketing.mobile.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class MessagingInternalIAMPayloadTests {

    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map> ids = new ArrayList<>();
    private final byte[] base64EncodedBytes = "decisionScope".getBytes(StandardCharsets.UTF_8);
    private final static String REMOTE_URL = "https://www.adobe.com/adobe.png";
    private MessagingInternal messagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private CacheManager cacheManager;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    Application mockApplication;
    @Mock
    Context context;
    @Mock
    Core mockCore;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    UIService mockUIService;
    @Mock
    AndroidEncodingService mockAndroidEncodingService;
    @Mock
    AndroidSystemInfoService mockAndroidSystemInfoService;
    @Mock
    AndroidNetworkService mockAndroidNetworkService;
    @Mock
    PackageManager packageManager;
    @Mock
    ApplicationInfo applicationInfo;
    @Mock
    Bundle bundle;

    @Before
    public void setup() throws PackageManager.NameNotFoundException, InterruptedException, MissingPlatformServicesException {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;
        when(MobileCore.getCore()).thenReturn(mockCore);

        // setup activity id mocks
        when(App.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(context);
        when(mockApplication.getPackageName()).thenReturn("mock_package_name");
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        Whitebox.setInternalState(applicationInfo, "metaData", bundle);
        when(bundle.getString("activityId")).thenReturn("mock_activity");
        when(bundle.getString("placementId")).thenReturn("mock_placement");

        // setup services mocks
        platformServices = new AndroidPlatformServices();
        jsonUtilityService = platformServices.getJsonUtilityService();
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        when(mockPlatformServices.getEncodingService()).thenReturn(mockAndroidEncodingService);
        when(mockPlatformServices.getSystemInfoService()).thenReturn(mockAndroidSystemInfoService);
        when(mockPlatformServices.getNetworkService()).thenReturn(mockAndroidNetworkService);
        when(mockAndroidEncodingService.base64Encode(any(byte[].class))).thenReturn(base64EncodedBytes);

        // setup mock cache
        final File mockCache = new File("mock_cache");
        when(mockAndroidSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);
        cacheManager = new CacheManager(mockAndroidSystemInfoService);
        // ensure cache is cleared before testing
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), IMAGES_CACHE_SUBDIRECTORY);
        cacheManager.deleteFilesNotInList(new ArrayList<String>(), MESSAGES_CACHE_SUBDIRECTORY);

        // write a image file from resources to the mock image asset cache
        final File mockCachedImage = cacheManager.createNewCacheFile(REMOTE_URL, IMAGES_CACHE_SUBDIRECTORY, new Date(System.currentTimeMillis()));
        TestUtils.readInputStreamIntoFile(mockCachedImage, TestUtils.convertResourceFileToInputStream("adobe", ".png"), false);

        // setup configuration shared state mock
        when(mockExtensionApi.getSharedEventState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, "mock_dataset_id");

        // setup identity shared state mock
        when(mockExtensionApi.getXDMSharedEventState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockIdentityState);
        ecidMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
        ids.add(ecidMap);
        identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ECID, ids);
        mockIdentityState.put(MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        messagingInternal = new MessagingInternal(mockExtensionApi);
    }

    // ========================================================================================
    // IAM rules retrieval from Offers
    // ========================================================================================
    @Test
    public void test_fetchMessages_CalledOnExtensionStart_WhenConfigurationAndIdentitySharedStateReady() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // expected dispatched event data
        String refreshInAppMessagesEventData = "{\"requesttype\":\"updatepropositions\",\"decisionscopes\":[{\"name\":\"decisionScope\"}]}";

        // Mocks
        Event mockEvent = mock(Event.class);
        // when mock event getType called return a generic event
        when(mockEvent.getType()).thenReturn("generic");

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify dispatch event is called
        // 1 event dispatched: Offers iam fetch event when extension is registered
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(refreshInAppMessagesEventData, event.getData().toString());
    }

    @Test
    public void test_refreshInAppMessages_Invoked() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // trigger event
        EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);
        // expected dispatched event data
        String refreshInAppMessagesEventData = "{\"requesttype\":\"updatepropositions\",\"decisionscopes\":[{\"name\":\"decisionScope\"}]}";

        // Mocks
        Event mockEvent = mock(Event.class);
        // when mock event getType called return MESSAGING
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        // when get eventData called return data with "REFRESH_MESSAGES, true"
        when(mockEvent.getEventData()).thenReturn(eventData.toObjectMap());

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify dispatch event is called
        // 2 events dispatched: Offers iam fetch event on initial launch and Offers iam fetch event when refresh in app messages event is received
        PowerMockito.verifyStatic(MobileCore.class, times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(refreshInAppMessagesEventData, event.getData().toString());
    }

    // ========================================================================================
    // Offers rules payload processing
    // ========================================================================================
    @Test
    public void test_handleEdgeResponseEvent_ValidOffersIAMPayloadPresent() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(1, false, false, false, false, false, false, false));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(1, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_MultipleValidOffersIAMPayloadPresent() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(3, false, false, false, false, false, false, false));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify 3 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(3, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OneInvalidIAMPayloadPresent() {
        // setup
        // trigger event
        List<Map> payload = TestUtils.generateMessagePayload(2, false, false, false, false, false, false, false);
        List<Map> invalidPayload = TestUtils.generateMessagePayload(1, true, true, false, false, false, false, false);
        payload.addAll(invalidPayload);
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify 2 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(2, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageId() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(1, false, true, false, false, false, false, false));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageType() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(1, false, false, true, false, false, false, false));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageDetail() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(1, false, false, false, true, false, false, false));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadIsEmpty() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        List<Map> payload = new ArrayList<>();
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadJsonIsNull() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", null);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadHasInvalidActivityId() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(1, false, false, false, false, false, true, false));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadHasInvalidPlacementId() {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", TestUtils.generateMessagePayload(1, false, false, false, false, false, false, true));
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }
}
