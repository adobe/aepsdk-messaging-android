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

import java.nio.charset.StandardCharsets;
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
    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map> ids = new ArrayList<>();
    private final String testActivityAndPlacement = "{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}";
    private final String testApplicationId = "{\"xdm:name\":\"mock_applicationId\"}";
    private final String nonMatchingApplicationId = "{\"xdm:name\":\"non_matching_applicationId\"}";
    private final String base64EncodedActivityAndPlacement = "eyJhY3Rpdml0eUlkIjoibW9ja19hY3Rpdml0eSIsInBsYWNlbWVudElkIjoibW9ja19wbGFjZW1lbnQiLCJpdGVtQ291bnQiOjMwfQ==";
    private final String base64EncodedApplicationId = "eyJ4ZG06bmFtZSI6Im1vY2tfYXBwbGljYXRpb25JZCJ9";
    private final String base64EncodedOtherApplicationId = "eyJ4ZG06bmFtZSI6Im5vbl9tYXRjaGluZ19hcHBsaWNhdGlvbklkIn0=";
    private MessagingInternal messagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private static int activityAndPlacementConfig = 0;
    private static int applicationIdConfig = 1;
    private MessagingCacheUtilities messagingCacheUtilities;

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
        when(App.getAppContext()).thenReturn(mockContext);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        when(mockApplication.getPackageName()).thenReturn("mock_applicationId");
        when(mockContext.getPackageName()).thenReturn("mock_applicationId");
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
        // mock for encoded/decoded activity and placement id
        when(mockAndroidEncodingService.base64Encode(testActivityAndPlacement.getBytes(StandardCharsets.UTF_8))).thenReturn(base64EncodedActivityAndPlacement.getBytes(StandardCharsets.UTF_8));
        when(mockAndroidEncodingService.base64Decode(base64EncodedActivityAndPlacement)).thenReturn(testActivityAndPlacement.getBytes(StandardCharsets.UTF_8));
        // mock for encoded/decoded application id
        when(mockAndroidEncodingService.base64Encode(testApplicationId.getBytes(StandardCharsets.UTF_8))).thenReturn(base64EncodedApplicationId.getBytes(StandardCharsets.UTF_8));
        when(mockAndroidEncodingService.base64Decode(base64EncodedApplicationId)).thenReturn(testApplicationId.getBytes(StandardCharsets.UTF_8));
        // mock for encoded/decoded non matching application id
        when(mockAndroidEncodingService.base64Encode(nonMatchingApplicationId.getBytes(StandardCharsets.UTF_8))).thenReturn(base64EncodedOtherApplicationId.getBytes(StandardCharsets.UTF_8));
        when(mockAndroidEncodingService.base64Decode(base64EncodedOtherApplicationId)).thenReturn(nonMatchingApplicationId.getBytes(StandardCharsets.UTF_8));

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

    @After
    public void cleanup() throws MissingPlatformServicesException {
        // use messaging cache utilities to clean the cache after each test
        messagingCacheUtilities = new MessagingCacheUtilities(platformServices.getSystemInfoService(), platformServices.getNetworkService());
        messagingCacheUtilities.clearCachedDataFromSubdirectory(MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY);
    }

    private EventData getExpectedEventData(int offersConfigType) {
        EventData eventData = new EventData();
        if (offersConfigType == activityAndPlacementConfig) {
            List<Variant> decisionScopes = new ArrayList<>();
            Map encodedScope = new HashMap<String, Variant>();
            encodedScope.put("name", Variant.fromString(base64EncodedActivityAndPlacement));
            decisionScopes.add(Variant.fromVariantMap(encodedScope));
            VectorVariant decisionScope = VectorVariant.from(decisionScopes);
            eventData.putVariant("requesttype", Variant.fromString("updatepropositions"));
            eventData.putVariant("decisionscopes", decisionScope);
        } else if (offersConfigType == applicationIdConfig) {
            List<Variant> decisionScopes = new ArrayList<>();
            Map encodedScope = new HashMap<String, Variant>();
            encodedScope.put("name", Variant.fromString(base64EncodedApplicationId));
            decisionScopes.add(Variant.fromVariantMap(encodedScope));
            VectorVariant decisionScope = VectorVariant.from(decisionScopes);
            eventData.putVariant("requesttype", Variant.fromString("updatepropositions"));
            eventData.putVariant("decisionscopes", decisionScope);
        }
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
        EventData expectedEventData = getExpectedEventData(activityAndPlacementConfig);

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
    public void test_refreshInAppMessages_ConfiguredWithActivityAndPlacement_VerifyEncodedDecisionScope() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // trigger event
        EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);
        // expected dispatched event data
        EventData expectedEventData = getExpectedEventData(activityAndPlacementConfig);

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
        Event event = eventCaptor.getAllValues().get(1);
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
        EventData expectedEventData = getExpectedEventData(applicationIdConfig);

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
        assertEquals(expectedEventData, event.getData());
    }

    // ========================================================================================
    // Offers rules payload processing, activity id and placement id present
    // ========================================================================================
    @Test
    public void test_handleEdgeResponseEvent_ValidOffersIAMPayloadPresent() {
        // setup
        MessagePayloadConfig config = new MessagePayloadConfig();
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
        MessagePayloadConfig config = new MessagePayloadConfig();
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
        MessagePayloadConfig validPayloadConfig = new MessagePayloadConfig();
        validPayloadConfig.count = 2;

        MessagePayloadConfig invalidPayloadConfig = new MessagePayloadConfig();
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
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
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
    public void test_handleEdgeResponseEvent_OffersConfigUsingApplicationId_PayloadContainsNonMatchingApplicationId() {
        // setup
        when(bundle.getString("activityId")).thenReturn(null);
        when(bundle.getString("placementId")).thenReturn(null);
        messagingInternal = new MessagingInternal(mockExtensionApi);
        MessagePayloadConfig config = new MessagePayloadConfig();
        config.count = 1;
        config.isUsingApplicationId = true;
        config.invalidApplicationId = true;
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
