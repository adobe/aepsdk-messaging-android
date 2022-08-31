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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class MessagingInternalIAMPayloadTests {
    private static final String mockAppId = "mock_applicationId";
    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map> ids = new ArrayList<>();
    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    Application mockApplication;
    @Mock
    Context mockContext;
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
    Bundle bundle;
    private MessagingInternal messagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private MessagingCacheUtilities messagingCacheUtilities;

    @Before
    public void setup() throws PackageManager.NameNotFoundException, InterruptedException, MissingPlatformServicesException {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();
        setupApplicationIdMocks();
        setupPlatformServicesMocks();
        setupSharedStateMocks();

        messagingInternal = new MessagingInternal(mockExtensionApi);
    }

    void setupMocks() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        when(MobileCore.getCore()).thenReturn(mockCore);
    }

    void setupApplicationIdMocks() {
        when(App.getApplication()).thenReturn(mockApplication);
        when(App.getAppContext()).thenReturn(mockContext);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        when(mockApplication.getPackageName()).thenReturn(mockAppId);
        when(mockContext.getPackageName()).thenReturn(mockAppId);
    }

    void setupPlatformServicesMocks() {
        platformServices = new AndroidPlatformServices();
        jsonUtilityService = platformServices.getJsonUtilityService();
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        when(mockPlatformServices.getEncodingService()).thenReturn(mockAndroidEncodingService);
        when(mockPlatformServices.getSystemInfoService()).thenReturn(mockAndroidSystemInfoService);
        when(mockPlatformServices.getNetworkService()).thenReturn(mockAndroidNetworkService);
    }

    void setupSharedStateMocks() {
        // setup configuration shared state mock
        when(mockExtensionApi.getSharedEventState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, "mock_dataset_id");

        // setup identity shared state mock
        when(mockExtensionApi.getXDMSharedEventState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(mockIdentityState);
        ecidMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
        ids.add(ecidMap);
        identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ECID, ids);
        mockIdentityState.put(MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);
    }

    @After
    public void cleanup() throws MissingPlatformServicesException {
        // use messaging cache utilities to clean the cache after each test
        messagingCacheUtilities = new MessagingCacheUtilities(platformServices.getSystemInfoService(), platformServices.getNetworkService(), new CacheManager(platformServices.getSystemInfoService()));
        messagingCacheUtilities.clearCachedDataFromSubdirectory(MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY);
    }

    private EventData getExpectedEventData() {
        final EventData eventData = new EventData();
        List<Variant> surfaces = new ArrayList<>();
        Map xdm = new HashMap<String, Variant>();
        Map query = new HashMap<String, Variant>();
        Map personalization = new HashMap<String, Variant>();
        xdm.put("eventType", Variant.fromString("personalization.request"));
        surfaces.add(Variant.fromString("mobileapp://mock_applicationId"));
        personalization.put("surfaces", Variant.fromVariantList(surfaces));
        query.put("personalization", Variant.fromVariantMap(personalization));
        eventData.putVariant("query", Variant.fromVariantMap(query));
        eventData.putVariant("xdm", Variant.fromVariantMap(xdm));
        return eventData;
    }

    // ========================================================================================
    // IAM rules retrieval from Offers
    // ========================================================================================
    @Test
    public void test_fetchMessages_CalledOnExtensionStart_WhenConfigurationAndIdentitySharedStateReady() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // expected dispatched event data
        EventData expectedEventData = getExpectedEventData();

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
        assertEquals(expectedEventData, event.getData());
    }

    @Test
    public void test_refreshInAppMessages_ConfiguredWithApplicationId_VerifyEncodedDecisionScope() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        messagingInternal = new MessagingInternal(mockExtensionApi);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // trigger event
        EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);
        // expected dispatched event data
        EventData expectedEventData = getExpectedEventData();

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
        // 2 events dispatched: iam fetch event on initial launch and iam fetch event when refresh in app messages event is received
        PowerMockito.verifyStatic(MobileCore.class, times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(expectedEventData, event.getData());
    }

    // ========================================================================================
    // Offers rules payload processing, activity id and placement id present
    // ========================================================================================
    @Test
    public void test_handleEdgeResponseEvent_ValidOffersIAMPayloadPresent() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", MessagingTestUtils.generateMessagePayload(config));
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
        MessageTestConfig config = new MessageTestConfig();
        config.count = 3;
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", MessagingTestUtils.generateMessagePayload(config));
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
        MessageTestConfig validPayloadConfig = new MessageTestConfig();
        validPayloadConfig.count = 2;

        MessageTestConfig invalidPayloadConfig = new MessageTestConfig();
        invalidPayloadConfig.count = 1;
        invalidPayloadConfig.isMissingRulesKey = true;

        // trigger event
        List<Map> payload = MessagingTestUtils.generateMessagePayload(validPayloadConfig);
        List<Map> invalidPayload = MessagingTestUtils.generateMessagePayload(invalidPayloadConfig);
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

    // ========================================================================================
    // Offers rules payload processing, application id present
    // ========================================================================================
    @Test
    public void test_handleEdgeResponseEvent_OffersConfigUsingApplicationId_ValidOffersIAMPayloadPresent() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        messagingInternal = new MessagingInternal(mockExtensionApi);
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", MessagingTestUtils.generateMessagePayload(config));
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
    public void test_handleEdgeResponseEvent_OffersConfigUsingApplicationId_PayloadContainsNonMatchingScope() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        messagingInternal = new MessagingInternal(mockExtensionApi);
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        eventData.put("payload", MessagingTestUtils.generateMessagePayload(config));
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

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }
}
