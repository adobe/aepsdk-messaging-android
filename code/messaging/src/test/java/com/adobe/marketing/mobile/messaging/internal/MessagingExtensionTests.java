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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingExtensionTests {

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    CacheService mockCacheService;
    @Mock
    DeviceInforming mockDeviceInfoService;
    @Mock
    LaunchRulesEngine mockMessagingRulesEngine;
    @Mock
    AJOPayloadHandler mockAJOPayloadHandler;
    @Mock
    SharedStateResult mockConfigData;
    @Mock
    SharedStateResult mockEdgeIdentityData;
    @Mock
    Application mockApplication;
    @Mock
    InternalMessage mockInternalMessage;
    @Mock
    LaunchRule mockLaunchRule;
    @Mock
    RuleConsequence mockRuleConsequence;

    private static final String mockCJMData = "{\n" +
            "        \"mixins\" :{\n" +
            "          \"_experience\": {\n" +
            "            \"customerJourneyManagement\": {\n" +
            "              \"messageExecution\": {\n" +
            "                \"messageExecutionID\": \"16-Sept-postman\",\n" +
            "                \"messageID\": \"567\",\n" +
            "                \"journeyVersionID\": \"some-journeyVersionId\",\n" +
            "                \"journeyVersionInstanceId\": \"someJourneyVersionInstanceId\"\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }";

    private MessagingExtension messagingExtension;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() {
        reset(mockExtensionApi);
        reset(mockServiceProvider);
        reset(mockCacheService);
        reset(mockMessagingRulesEngine);
        reset(mockAJOPayloadHandler);
        reset(mockConfigData);
        reset(mockEdgeIdentityData);
        reset(mockDeviceInfoService);
        reset(mockApplication);
        reset(mockInternalMessage);
        reset(mockLaunchRule);
        reset(mockRuleConsequence);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");
            when(mockCacheService.get(any(), any())).thenReturn(null);
            when(mockConfigData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("messaging.eventDataset", "mock_datasetId");
            }});
            when(mockEdgeIdentityData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("key", "value");
            }});

            messagingExtension = new MessagingExtension(mockExtensionApi, mockMessagingRulesEngine, mockAJOPayloadHandler);

            runnable.run();
        }
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_TestableConstructor() {
        runUsingMockedServiceProvider(() -> {
            assertNotNull(messagingExtension.messagingRulesEngine);
            assertNotNull(messagingExtension.AJOPayloadHandler);
        });
    }

    @Test
    public void test_Constructor() {
        runUsingMockedServiceProvider(() -> {
            messagingExtension = new MessagingExtension(mockExtensionApi);
            assertNotNull(messagingExtension.messagingRulesEngine);
            assertNotNull(messagingExtension.AJOPayloadHandler);
        });
    }

    @Test
    public void test_onRegistered() {
        runUsingMockedServiceProvider(() -> {
            // test
            messagingExtension.onRegistered();

            // verify 4 listeners are registered
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.GENERIC_IDENTITY), eq(EventSource.REQUEST_CONTENT), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.EDGE), eq(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.WILDCARD), eq(EventSource.WILDCARD), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(MessagingConstants.EventType.MESSAGING), eq(EventSource.REQUEST_CONTENT), any());
        });
    }


    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        runUsingMockedServiceProvider(() -> {
            // test
            String moduleName = messagingExtension.getName();
            assertEquals("getName should return the correct module name", MessagingConstants.EXTENSION_NAME, moduleName);
        });
    }

    @Test
    public void test_getFriendlyName() {
        runUsingMockedServiceProvider(() -> {
            // test
            String friendlyName = messagingExtension.getFriendlyName();
            assertEquals("getFriendlyName should return the correct value", MessagingConstants.FRIENDLY_EXTENSION_NAME, friendlyName);
        });
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        runUsingMockedServiceProvider(() -> {
            // test
            String moduleVersion = messagingExtension.getVersion();
            assertEquals("getVersion should return the correct module version", MessagingConstants.EXTENSION_VERSION,
                    moduleVersion);
        });
    }

    // =================================================================================================================
    // readyForEvent
    // =================================================================================================================
    @Test
    public void test_readyForEvent_when_eventReceived_and_configurationAndIdentitySharedStateDataPresent_then_readyForEventTrue() {
        // setup
        runUsingMockedServiceProvider(() -> {
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(mockEdgeIdentityData);

            Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                    .build();

            // verify
            assertTrue(messagingExtension.readyForEvent(testEvent));
        });
    }

    @Test
    public void test_readyForEvent_when_eventReceived_and_configurationSharedStateNotReady_then_readyForEventIsFalse() {
        // setup
        runUsingMockedServiceProvider(() -> {
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(new SharedStateResult(SharedStateStatus.PENDING, new HashMap<>()));
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(mockEdgeIdentityData);

            Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                    .build();

            // verify
            assertFalse(messagingExtension.readyForEvent(testEvent));
        });
    }

    @Test
    public void test_readyForEvent_when_eventReceived_and_identitySharedStateNotReady_then_readyForEventIsFalse() {
        // setup
        runUsingMockedServiceProvider(() -> {
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(new SharedStateResult(SharedStateStatus.PENDING, new HashMap<>()));

            Event testEvent = new Event.Builder("Test event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
                    .build();

            // verify
            assertFalse(messagingExtension.readyForEvent(testEvent));
        });
    }

    // =================================================================================================================
    // handleWildcardEvents
    // =================================================================================================================
    @Test
    public void test_handleWildcardEvents_when_triggeredRulesReturnedFromRulesEngine() {
        // setup
        List<RuleConsequence> ruleConsequenceList = new ArrayList<>();
        ruleConsequenceList.add(mockRuleConsequence);
        when(mockLaunchRule.getConsequenceList()).thenReturn(ruleConsequenceList);
        List<LaunchRule> launchRuleList = new ArrayList<>();
        launchRuleList.add(mockLaunchRule);
        when(mockMessagingRulesEngine.process(any(Event.class))).thenReturn(launchRuleList);

        runUsingMockedServiceProvider(() -> {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("key", "value");
            Event mockEvent = mock(Event.class);

            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(EventType.GENERIC_TRACK);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.handleWildcardEvents(mockEvent);

            // verify triggered rule consequence passed to InAppNotificationHandler to create a Message
            verify(mockAJOPayloadHandler, times(1)).createInAppMessage(eq(mockRuleConsequence));
        });
    }

    @Test
    public void test_handleWildcardEvents_when_noTriggeredRulesReturnedFromRulesEngine() {
        // setup
        when(mockMessagingRulesEngine.process(any(Event.class))).thenReturn(new ArrayList<>());

        runUsingMockedServiceProvider(() -> {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("key", "value");
            Event mockEvent = mock(Event.class);

            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(EventType.GENERIC_TRACK);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.handleWildcardEvents(mockEvent);

            // verify
            // no event dispatched: rules response event
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    // ========================================================================================
    // processEvents
    // ========================================================================================
    @Test
    public void test_processEvent_when_NullEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                // test
                messagingExtension.processEvent(null);

                // verify
                logMockedStatic.verify(() -> Log.debug(anyString(), anyString(), eq("Event or EventData is null, ignoring the event.")), times(1));
            }
        });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEventContainsPushToken() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"data\":{\"pushNotificationDetails\":[{\"denylisted\":false,\"identity\":{\"namespace\":{\"code\":\"ECID\"},\"id\":\"mock_ecid\"},\"appID\":\"mockPackageName\",\"platform\":\"fcm\",\"token\":\"mock_push_token\"}]}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
            final Map<String, Object> ecidsMap = new HashMap<>();
            final Map<String, Object> identityMap = new HashMap<>();
            final List<Map<String, Object>> ecidList = new ArrayList<>();
            identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
            ecidList.add(identityMap);
            ecidsMap.put("ECID", ecidList);
            mockEdgeIdentityState.put("identityMap", ecidsMap);
            when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

            try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
                mobileCoreMockedStatic.when(MobileCore::getApplication).thenReturn(mockApplication);
                when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
                when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify
                // 1 event dispatched: edge event with push profile data
                verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

                // verify event
                Event event = eventCaptor.getValue();
                assertNotNull(event.getEventData());
                assertEquals(MessagingConstants.EventName.PUSH_PROFILE_EDGE_EVENT, event.getName());
                assertEquals(MessagingConstants.EventType.EDGE, event.getType());
                assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
                assertEquals(expectedEventData, event.getEventData());

                // verify if the push token is stored in shared state
                verify(mockExtensionApi, times(1)).createSharedState(any(Map.class), any(Event.class));
            }
        });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEventHasNullPushToken() {
        runUsingMockedServiceProvider(() -> {
            // setup
            final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
            final Map<String, Object> ecidsMap = new HashMap<>();
            final Map<String, Object> identityMap = new HashMap<>();
            final List<Map<String, Object>> ecidList = new ArrayList<>();
            identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
            ecidList.add(identityMap);
            ecidsMap.put("ECID", ecidList);
            mockEdgeIdentityState.put("identityMap", ecidsMap);
            when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

            try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
                mobileCoreMockedStatic.when(MobileCore::getApplication).thenReturn(mockApplication);
                when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, null);
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
                when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify
                // no event dispatched: edge event with push profile data
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                // verify no push token is stored in shared state
                verify(mockExtensionApi, times(0)).createSharedState(any(Map.class), any(Event.class));
            }
        });
    }


    @Test
    public void test_processEvent_genericIdentityEvent_whenEventDataIsNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
            final Map<String, Object> ecidsMap = new HashMap<>();
            final Map<String, Object> identityMap = new HashMap<>();
            final List<Map<String, Object>> ecidList = new ArrayList<>();
            identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
            ecidList.add(identityMap);
            ecidsMap.put("ECID", ecidList);
            mockEdgeIdentityState.put("identityMap", ecidsMap);
            when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

            try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
                mobileCoreMockedStatic.when(MobileCore::getApplication).thenReturn(mockApplication);
                when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                Map<String, Object> eventData = new HashMap<>();
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
                when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify
                // no event dispatched: edge event with push profile data
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                // verify no push token is stored in shared state
                verify(mockExtensionApi, times(0)).createSharedState(any(Map.class), any(Event.class));
            }
        });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEventHasEmptyPushToken() {
        runUsingMockedServiceProvider(() -> {
            // setup
            final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
            final Map<String, Object> ecidsMap = new HashMap<>();
            final Map<String, Object> identityMap = new HashMap<>();
            final List<Map<String, Object>> ecidList = new ArrayList<>();
            identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
            ecidList.add(identityMap);
            ecidsMap.put("ECID", ecidList);
            mockEdgeIdentityState.put("identityMap", ecidsMap);
            when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

            try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
                mobileCoreMockedStatic.when(MobileCore::getApplication).thenReturn(mockApplication);
                when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
                when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify
                // no event dispatched: edge event with push profile data
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                // verify no push token is stored in shared state
                verify(mockExtensionApi, times(0)).createSharedState(any(Map.class), any(Event.class));
            }
        });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEcidIsNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
            final Map<String, Object> ecidsMap = new HashMap<>();
            final Map<String, Object> identityMap = new HashMap<>();
            final List<Map<String, Object>> ecidList = new ArrayList<>();
            identityMap.put(MessagingConstants.SharedState.EdgeIdentity.ID, null);
            ecidList.add(identityMap);
            ecidsMap.put("ECID", ecidList);
            mockEdgeIdentityState.put("identityMap", ecidsMap);
            when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

            try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
                mobileCoreMockedStatic.when(MobileCore::getApplication).thenReturn(mockApplication);
                when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
                when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify
                // no event dispatched: edge event with push profile data
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                // verify no push token is stored in shared state
                verify(mockExtensionApi, times(0)).createSharedState(any(Map.class), any(Event.class));
            }
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenApplicationOpened_withCustomAction() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mock_actionId\"},\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"mock_eventType\"},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);

            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // 1 event dispatched: edge event with push tracking data
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.PUSH_TRACKING_EDGE_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenApplicationOpened_withoutCustomAction() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"pushNotificationTracking\":{\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"mock_eventType\"},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);

            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // 1 event dispatched: edge event with push tracking data
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.PUSH_TRACKING_EDGE_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenMixinsDataPresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mock_actionId\"},\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":0}},\"eventType\":\"mock_eventType\",\"_experience\":{\"customerJourneyManagement\":{\"pushChannelContext\":{\"platform\":\"fcm\"},\"messageExecution\":{\"messageExecutionID\":\"16-Sept-postman\",\"journeyVersionInstanceId\":\"someJourneyVersionInstanceId\",\"messageID\":\"567\",\"journeyVersionID\":\"some-journeyVersionId\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/push\"}}}}},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);

            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // 1 event dispatched: edge event with push tracking data
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.PUSH_TRACKING_EDGE_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenEventDataNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(null);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // no edge event dispatched
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenTrackInfoEventTypeNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);

            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // no edge event dispatched
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenMessageIdNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);

            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // no edge event dispatched
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenExperienceEventDatasetIdIsEmpty() {
        runUsingMockedServiceProvider(() -> {
            // setup
            when(mockConfigData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("messaging.eventDataset", "");
            }});

            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
            eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);

            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
            when(mockExtensionApi.getSharedState(eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
            when(mockExtensionApi.getXDMSharedState(eq(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            // no edge event dispatched
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    @Test
    public void test_processEvent_fetchMessagesEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("refreshMessages", true);
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            verify(mockAJOPayloadHandler, times(1)).fetchMessages(null);
        });
    }

    @Test
    public void test_processEvent_edgePersonalizationEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("proposition_data", "mock_proposition_data");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);
            when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            verify(mockAJOPayloadHandler, times(1)).handleEdgePersonalizationNotification(any(Event.class));
        });
    }

    @Test
    public void test_processEvent_updateFeedsForSurfacePathsEvent() {
        ArgumentCaptor<List<String>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        List<String> surfacePaths = new ArrayList<>();
        surfacePaths.add("promos/feed1");
        surfacePaths.add("promos/feed2");

        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("updatefeeds", true);
            eventData.put("surfaces", surfacePaths);
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            verify(mockAJOPayloadHandler, times(1)).fetchMessages(listArgumentCaptor.capture());
            assertEquals(surfacePaths, listArgumentCaptor.getValue());
        });
    }

    // ========================================================================================
    // sendPropositionInteraction
    // ========================================================================================
    @Test
    public void test_sendPropositionInteraction_InAppInteractTracking() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try {
                mockInternalMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionInteract\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"interact\":1},\"propositionAction\":{\"id\":\"confirm\",\"label\":\"confirm\"},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"confirm\",\"eventType\":\"interact\"}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.sendPropositionInteraction("confirm", MessagingEdgeEventType.IN_APP_INTERACT, mockInternalMessage);

            // verify dispatch event is called
            // 1 event dispatched: edge event with in app interact event tracking info
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_sendPropositionInteraction_InAppInteractTracking_WhenScopeDetailsNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try {
                mockInternalMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(true);
            } catch (Exception e) {
                fail(e.getMessage());
            }

            // test
            messagingExtension.sendPropositionInteraction("confirm", MessagingEdgeEventType.IN_APP_INTERACT, mockInternalMessage);

            // verify dispatch event is called
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    @Test
    public void test_sendPropositionInteraction_InAppDismissTracking() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try {
                mockInternalMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionDismiss\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"dismiss\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"dismiss\"}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.sendPropositionInteraction(null, MessagingEdgeEventType.IN_APP_DISMISS, mockInternalMessage);

            // verify dispatch event is called
            // 1 event dispatched: edge event with in app dismiss event tracking info
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_sendPropositionInteraction_InAppDisplayTracking() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try {
                mockInternalMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionDisplay\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"display\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"display\"}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.sendPropositionInteraction(null, MessagingEdgeEventType.IN_APP_DISPLAY, mockInternalMessage);

            // verify dispatch event is called
            // 1 event dispatched: edge event with in app display event tracking info
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_sendPropositionInteraction_InAppTriggeredTracking() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try {
                mockInternalMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
            } catch (Exception e) {
                fail(e.getMessage());
            }
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionTrigger\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"trigger\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"trigger\"}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.sendPropositionInteraction(null, MessagingEdgeEventType.IN_APP_TRIGGER, mockInternalMessage);

            // verify dispatch event is called
            // 1 event dispatched: edge event with in app triggered event tracking info
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event
            Event event = eventCaptor.getValue();
            assertNotNull(event.getEventData());
            assertEquals(MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT, event.getName());
            assertEquals(MessagingConstants.EventType.EDGE, event.getType());
            assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
            assertEquals(expectedEventData, event.getEventData());
        });
    }
}