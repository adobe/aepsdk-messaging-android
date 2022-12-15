/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventType.EDGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class, MessagingCacheUtilities.class, InAppNotificationHandler.class})
public class InAppNotificationHandlerTests {
    private static final String mockAppId = "mock_applicationId";
    private final Map<String, Object> mockConfigState = new HashMap<>();
    private final Map<String, Object> mockIdentityState = new HashMap<>();
    private final Map<String, Object> identityMap = new HashMap<>();
    private final Map<String, Object> ecidMap = new HashMap<>();
    private final List<Map<String, Object>> ids = new ArrayList<>();
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
    MessagingInternal mockMessagingInternal;
    @Mock
    MessagingCacheUtilities mockMessagingCacheUtilities;
    @Mock
    Message mockMessage;
    @Mock
    Looper mockLooper;

    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private MessagingCacheUtilities messagingCacheUtilities;
    private InAppNotificationHandler inAppNotificationHandler;

    @Before
    public void setup() throws PackageManager.NameNotFoundException, InterruptedException, MissingPlatformServicesException {
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;

        setupMocks();
        setupApplicationIdMocks();
        setupPlatformServicesMocks();
        setupSharedStateMocks();

        messagingCacheUtilities = new MessagingCacheUtilities(mockAndroidSystemInfoService, mockAndroidNetworkService, new CacheManager(mockAndroidSystemInfoService));
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);
        Whitebox.setInternalState(inAppNotificationHandler, "requestMessagesEventId", "TESTING_ID");
    }

    void setupMocks() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        when(MobileCore.getCore()).thenReturn(mockCore);
    }

    void setupApplicationIdMocks() {
        when(MobileCore.getApplication()).thenReturn(mockApplication);
        when(App.getApplication()).thenReturn(mockApplication);
        when(App.getAppContext()).thenReturn(mockContext);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        when(mockApplication.getPackageName()).thenReturn(mockAppId);
        when(mockApplication.getMainLooper()).thenReturn(mockLooper);
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
        // setup mock cache file
        final File mockCache = new File("mock_cache");
        when(mockAndroidSystemInfoService.getApplicationCacheDir()).thenReturn(mockCache);
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
    public void cleanup() {
        // use messaging cache utilities to clean the cache after each test
        messagingCacheUtilities.clearCachedData();
    }

    // ========================================================================================
    // fetchMessages
    // ========================================================================================
    @Test
    public void test_fetchMessages_appIdPresent() {
        // setup
        String expectedEventData = "{\"xdm\":{\"eventType\":\"personalization.request\"},\"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mock_applicationId\"]}}}";
        // test
        inAppNotificationHandler.fetchMessages();

        // verify MobileCore.dispatchEvent called
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event data
        Event event = eventCaptor.getValue();
        assertEquals(expectedEventData, event.getData().toString());
    }

    @Test
    public void test_fetchMessages_emptyAppId() {
        // setup
        when(mockContext.getPackageName()).thenReturn("");

        // test
        inAppNotificationHandler.fetchMessages();

        // verify MobileCore.dispatchEvent not called
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        PowerMockito.verifyStatic(MobileCore.class, times(0));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // handlePersonalizationPayload
    // ========================================================================================
    @Test
    public void test_handleEdgePersonalizationNotification_ValidIAMPayloadPresent() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(1, loadedRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_MultipleValidIAMPayloadPresent() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 3;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify 3 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(3, loadedRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_OneInvalidIAMPayloadPresent() {
        // setup
        MessageTestConfig validPayloadConfig = new MessageTestConfig();
        validPayloadConfig.count = 2;
        MessageTestConfig invalidPayloadConfig = new MessageTestConfig();
        invalidPayloadConfig.count = 1;
        invalidPayloadConfig.isMissingRulesKey = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(validPayloadConfig);
        List<Map<String, Object>> invalidPayload = MessagingTestUtils.generateMessagePayload(invalidPayloadConfig);
        payload.addAll(invalidPayload);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify 2 rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(2, loadedRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadMissingMessageId() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.isMissingMessageId = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadMissingMessageType() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.isMissingMessageType = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadMissingMessageDetail() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.isMissingMessageDetail = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadIsEmpty() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.hasEmptyPayload = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_IAMPayloadIsNull() {
        // setup
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", null);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();
        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify no rules loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, moduleRules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_VeryImageAssetPresentInPayloadIsCached() {
        // setup
        String IMAGE_URL = "https://www.adobe.com/adobe.png";
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, mockMessagingCacheUtilities);
        Whitebox.setInternalState(inAppNotificationHandler, "requestMessagesEventId", "TESTING_ID");
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        ConcurrentLinkedQueue<Rule> loadedRules = rules.iterator().next();
        assertEquals(1, loadedRules.size());

        // verify image asset attempted to be cached
        ArgumentCaptor<List<String>> imageAssetListCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(imageAssetListCaptor.capture());
        assertEquals(IMAGE_URL, imageAssetListCaptor.getValue().get(0));
    }

    // ========================================================================================
    // handlePersonalizationPayload
    // ========================================================================================
    @Test
    public void test_handlePersonalizationPayload_PayloadContainsNonMatchingScope() {
        // setup
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);
        Whitebox.setInternalState(inAppNotificationHandler, "requestMessagesEventId", "TESTING_ID");

        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;
        config.nonMatchingAppSurfaceInPayload = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    @Test
    public void test_handlePersonalizationPayload_PayloadMissingScope() {
        // setup
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);
        Whitebox.setInternalState(inAppNotificationHandler, "requestMessagesEventId", "TESTING_ID");

        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;
        List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("payload", payload);
        eventData.put("requestEventId", "TESTING_ID");
        Event edgeEvent = new Event.Builder("personalization event", EDGE, PERSONALIZATION_DECISIONS)
                .setEventData(eventData)
                .build();

        // test
        inAppNotificationHandler.handleEdgePersonalizationNotification(edgeEvent);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    // ========================================================================================
    // inAppNotificationHandler load cached propositions on instantiation
    // ========================================================================================
    @Test
    public void test_cachedPropositions_ValidPayload() {
        // setup
        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        List<PropositionPayload> payload = MessagingUtils.getPropositionPayloads(MessagingTestUtils.generateMessagePayload(config));
        messagingCacheUtilities.cachePropositions(payload);

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify 1 rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(1, rules.size());
    }

    @Test
    public void test_cachedPropositions_nonMatchingScope() {
        // setup
        mockMessagingInternal = new MessagingInternal(mockExtensionApi);

        MessageTestConfig config = new MessageTestConfig();
        config.count = 1;
        config.noValidAppSurfaceInPayload = true;
        config.nonMatchingAppSurfaceInPayload = true;
        List<PropositionPayload> payload = MessagingUtils.getPropositionPayloads(MessagingTestUtils.generateMessagePayload(config));
        messagingCacheUtilities.cachePropositions(payload);

        // test
        inAppNotificationHandler = new InAppNotificationHandler(mockMessagingInternal, messagingCacheUtilities);

        // verify no rule loaded
        ConcurrentHashMap moduleRules = mockCore.eventHub.getModuleRuleAssociation();
        Collection<ConcurrentLinkedQueue<Rule>> rules = moduleRules.values();
        assertEquals(0, rules.size());
    }

    // ========================================================================================
    // createInAppMessage
    // ========================================================================================
    @Test
    public void test_createInAppMessage() {
        // setup
        try {
            PowerMockito.whenNew(Message.class).withArguments(any(MessagingInternal.class), anyMap(), anyMap(), anyMap()).thenReturn(mockMessage);
        } catch (Exception e) {
            fail("Failed to create mock Message: " + e.getMessage());
        }
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mobileParameters = new HashMap<>();

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);
        Event mockEvent = mock(Event.class);

        // when get eventData called return event data containing a valid rules response content event with a triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        inAppNotificationHandler.createInAppMessage(mockEvent);

        // verify MessagingFullscreenMessage.show() and MessagingFullscreenMessage.trigger() called
        verify(mockMessage, times(1)).trigger();
        verify(mockMessage, times(1)).show();
    }

    @Test
    public void test_createInAppMessage_InvalidRuleConsequence() {
        // setup
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        Map<String, Object> mobileParameters = new HashMap<>();

        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, "notCjmIam");
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);
        Event mockEvent = mock(Event.class);

        // when get eventData called return event data containing a valid rules response content event with a triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        inAppNotificationHandler.createInAppMessage(mockEvent);

        // verify MessagingFullscreenMessage.show() and MessagingFullscreenMessage.trigger() not called
        verify(mockMessage, times(0)).trigger();
        verify(mockMessage, times(0)).show();
    }
}
