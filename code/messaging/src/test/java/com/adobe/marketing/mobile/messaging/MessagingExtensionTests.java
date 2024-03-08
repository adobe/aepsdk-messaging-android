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
import com.adobe.marketing.mobile.messaging.PushTrackingStatus;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM;

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
    FeedRulesEngine mockFeedRulesEngine;
    @Mock
    EdgePersonalizationResponseHandler mockEdgePersonalizationResponseHandler;
    @Mock
    SharedStateResult mockConfigData;
    @Mock
    SharedStateResult mockEdgeIdentityData;
    @Mock
    Application mockApplication;
    @Mock
    PresentableMessageMapper.InternalMessage mockInternalMessage;
    @Mock
    LaunchRule mockLaunchRule;
    @Mock
    RuleConsequence mockRuleConsequence;
    @Mock
    SerialWorkDispatcher<Event> mockSerialWorkDispatcher;

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
        reset(mockFeedRulesEngine);
        reset(mockEdgePersonalizationResponseHandler);
        reset(mockConfigData);
        reset(mockEdgeIdentityData);
        reset(mockDeviceInfoService);
        reset(mockApplication);
        reset(mockInternalMessage);
        reset(mockLaunchRule);
        reset(mockRuleConsequence);
        reset(mockSerialWorkDispatcher);
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

            messagingExtension = new MessagingExtension(mockExtensionApi, mockMessagingRulesEngine, mockFeedRulesEngine, mockEdgePersonalizationResponseHandler);

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
            assertNotNull(messagingExtension.edgePersonalizationResponseHandler);
        });
    }

    @Test
    public void test_Constructor() {
        runUsingMockedServiceProvider(() -> {
            messagingExtension = new MessagingExtension(mockExtensionApi);
            assertNotNull(messagingExtension.messagingRulesEngine);
            assertNotNull(messagingExtension.edgePersonalizationResponseHandler);
        });
    }

    @Test
    public void test_onRegistered() {
        runUsingMockedServiceProvider(() -> {
            // setup
            messagingExtension.setSerialWorkDispatcher(mockSerialWorkDispatcher);

            // test
            messagingExtension.onRegistered();

            // verify 6 listeners are registered
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.GENERIC_IDENTITY), eq(EventSource.REQUEST_CONTENT), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.EDGE), eq(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.WILDCARD), eq(EventSource.WILDCARD), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.MESSAGING), eq(EventSource.REQUEST_CONTENT), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.RULES_ENGINE), eq(EventSource.RESPONSE_CONTENT), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.MESSAGING), eq(EventSource.CONTENT_COMPLETE), any());

            // verify serial dispatcher started
            verify(mockSerialWorkDispatcher, times(1)).start();
        });
    }

    // ========================================================================================
    // SerialWorkDispatcher tests
    // ========================================================================================
    @Test
    public void test_serialWorkDispatcher_offerRetrieveMessageEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Event event = new Event.Builder("Test Get propositions", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                    .setEventData(new HashMap<String, Object>() {{
                        put(MessagingConstants.EventDataKeys.Messaging.GET_PROPOSITIONS, true);
                    }})
                    .build();

            // test
            messagingExtension.onRegistered();
            try {
                messagingExtension.getSerialWorkDispatcher().offer(event);
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                fail(exception.getLocalizedMessage());
            }

            // verify EdgePersonalizationResponseHandler.retrieveMessages called
            verify(mockEdgePersonalizationResponseHandler, times(1)).retrieveMessages(any(), any());
        });
    }

    @Test
    public void test_serialWorkDispatcher_offerEdgeContentCompletedEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, List<Surface>> requestedSurfacesForEventId = new HashMap<>();
            requestedSurfacesForEventId.put("testEventId", new ArrayList<Surface>() {{
                add(new Surface());
            }});
            when(mockEdgePersonalizationResponseHandler.getRequestedSurfacesForEventId()).thenReturn(requestedSurfacesForEventId);
            messagingExtension = new MessagingExtension(mockExtensionApi, mockMessagingRulesEngine, mockFeedRulesEngine, mockEdgePersonalizationResponseHandler);

            Event event = Mockito.mock(Event.class);
            when(event.getType()).thenReturn(EventType.EDGE);
            when(event.getSource()).thenReturn(EventSource.CONTENT_COMPLETE);
            when(event.getUniqueIdentifier()).thenReturn("testEventId");

            // test
            messagingExtension.onRegistered();
            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                fail(exception.getLocalizedMessage());
            }

            // verify serialWorkDispatcher.offer returns false as this is a content complete event with a matching event id
            assertEquals(true, messagingExtension.getSerialWorkDispatcher().offer(event));
        });
    }

    @Test
    public void test_serialWorkDispatcher_offerNonEdgeEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Event event = new Event.Builder("Not an Edge Content Complete event", EventType.WILDCARD, EventSource.RESPONSE_CONTENT)
                    .build();

            // test
            messagingExtension.onRegistered();

            // verify serialWorkDispatcher.offer returns true as this is an unhandled event type
            assertEquals(true, messagingExtension.getSerialWorkDispatcher().offer(event));
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
    public void test_handleWildcardEvents_when_validEventReceived() {
        // setup
        runUsingMockedServiceProvider(() -> {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("key", "value");
            Event mockEvent = mock(Event.class);
            List<RuleConsequence> mockRuleConsequenceList = new ArrayList<>();
            mockRuleConsequenceList.add(mockRuleConsequence);

            when(mockMessagingRulesEngine.evaluateEvent(any())).thenReturn(mockRuleConsequenceList);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(EventType.GENERIC_TRACK);
            when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.handleWildcardEvents(mockEvent);

            // verify rules engine processes event
            verify(mockMessagingRulesEngine, times(1)).processEvent(eq(mockEvent));
        });
    }

    // =================================================================================================================
    // handleRuleEngineResponseEvents
    // =================================================================================================================
    @Test
    public void test_handleRuleEngineResponseEvents_when_validConsequence_then_createInAppMessageCalled() {
        // setup
        runUsingMockedServiceProvider(() -> {
            List<String> assetList = new ArrayList<>();
            assetList.add("remoteAsset.png");
            Map<String, Object> detail =
                    new HashMap<String, Object>() {
                        {
                            put("remoteAssets", assetList);
                            put("mobileParameters", new HashMap<String, Object>());
                            put("html", "iam html content");
                        }
                    };
            Map<String, Object> triggeredConsequence =
                    new HashMap<String, Object>() {
                        {
                            put("id", "testId");
                            put("type", "schema");
                            put("detail", detail);
                        }
                    };

            Map<String, Object> ruleConsequenceMap = new HashMap<String, Object>() {
                {
                    {
                        put("triggeredconsequence", triggeredConsequence);
                    }
                }
            };

            Event testEvent = new Event.Builder("Test event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
                    .setEventData(ruleConsequenceMap)
                    .build();

            // test
            messagingExtension.handleRuleEngineResponseEvents(testEvent);

            // verify
            verify(mockEdgePersonalizationResponseHandler, times(1)).createInAppMessage(any(RuleConsequence.class));
        });
    }

    @Test
    public void test_handleRuleEngineResponseEvents_when_invalidType_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(() -> {
            List<String> assetList = new ArrayList<>();
            assetList.add("remoteAsset.png");
            Map<String, Object> detail =
                    new HashMap<String, Object>() {
                        {
                            put("remoteAssets", assetList);
                            put("mobileParameters", new HashMap<String, Object>());
                            put("html", "iam html content");
                        }
                    };
            Map<String, Object> triggeredConsequence =
                    new HashMap<String, Object>() {
                        {
                            put("id", "testId");
                            put("type", "invalid");
                            put("detail", detail);
                        }
                    };

            Map<String, Object> ruleConsequenceMap = new HashMap<String, Object>() {
                {
                    {
                        put("triggeredconsequence", triggeredConsequence);
                    }
                }
            };

            Event testEvent = new Event.Builder("Test event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
                    .setEventData(ruleConsequenceMap)
                    .build();

            // test
            messagingExtension.handleRuleEngineResponseEvents(testEvent);

            // verify
            verify(mockEdgePersonalizationResponseHandler, times(0)).createInAppMessage(any(RuleConsequence.class));
        });
    }

    @Test
    public void test_handleRuleEngineResponseEvents_when_nullTriggeredConsequence_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(() -> {
            List<String> assetList = new ArrayList<>();
            assetList.add("remoteAsset.png");
            Map<String, Object> ruleConsequenceMap = new HashMap<String, Object>() {
                {
                    {
                        put("triggeredconsequence", null);
                    }
                }
            };

            Event testEvent = new Event.Builder("Test event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
                    .setEventData(ruleConsequenceMap)
                    .build();

            // test
            messagingExtension.handleRuleEngineResponseEvents(testEvent);

            // verify
            verify(mockEdgePersonalizationResponseHandler, times(0)).createInAppMessage(any(RuleConsequence.class));
        });
    }

    @Test
    public void test_handleRuleEngineResponseEvents_when_nullDetailsPresentInConsequence_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(() -> {
            List<String> assetList = new ArrayList<>();
            assetList.add("remoteAsset.png");
            Map<String, Object> triggeredConsequence =
                    new HashMap<String, Object>() {
                        {
                            put("id", "testId");
                            put("type", "cjmiam");
                            put("detail", null);
                        }
                    };

            Map<String, Object> ruleConsequenceMap = new HashMap<String, Object>() {
                {
                    {
                        put("triggeredconsequence", triggeredConsequence);
                    }
                }
            };

            Event testEvent = new Event.Builder("Test event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
                    .setEventData(ruleConsequenceMap)
                    .build();

            // test
            messagingExtension.handleRuleEngineResponseEvents(testEvent);

            // verify
            verify(mockEdgePersonalizationResponseHandler, times(0)).createInAppMessage(any(RuleConsequence.class));
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
    public void test_processEvent_messageTrackingEvent_whenApplicationOpened() {
        runUsingMockedServiceProvider(() -> {
            // setup
            mockConfigSharedState();
            final Event event = samplePushTrackingEvent("pushOpened", "messageId", null, true);
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.processEvent(event);

            // verify
            verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

            // verify push tracking status event
            final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
            assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
            assertEquals(EventType.MESSAGING, pushTrackingStatusEvent.getType());
            assertEquals(EventSource.RESPONSE_CONTENT, pushTrackingStatusEvent.getSource());
            assertEquals(PushTrackingStatus.TRACKING_INITIATED.getValue(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
            assertEquals(PushTrackingStatus.TRACKING_INITIATED.getDescription(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatusMessage"));

            // verify push tracking status event
            final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
            assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
            assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
            // verify edge tracking event data
            Map<String, String> edgeTrackingData = MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
            assertEquals("messageId", edgeTrackingData.get("xdm.pushNotificationTracking.pushProviderMessageID"));
            assertEquals("1", edgeTrackingData.get("xdm.application.launches.value"));
            assertEquals("pushOpened", edgeTrackingData.get("xdm.eventType"));
            assertEquals("mock_datasetId", edgeTrackingData.get("meta.collect.datasetId"));
            assertEquals("fcm", edgeTrackingData.get("xdm.pushNotificationTracking.pushProvider"));
            assertEquals("trackingvalue", edgeTrackingData.get("xdm.trackingkey"));
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenApplicationOpened_withCustomAction() {
        runUsingMockedServiceProvider(() -> {
            // setup
            mockConfigSharedState();
            final Event event = samplePushTrackingEvent("pushClicked", "messageId", "actionId", true);
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.processEvent(event);

            // verify rules engine processes event
            verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

            // verify push tracking status event
            final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
            assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
            assertEquals(PushTrackingStatus.TRACKING_INITIATED.getValue(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
            assertEquals(PushTrackingStatus.TRACKING_INITIATED.getDescription(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatusMessage"));

            // verify push tracking status event
            final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
            assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
            assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
            // verify edge tracking event data
            Map<String, String> edgeTrackingData = MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
            assertEquals("messageId", edgeTrackingData.get("xdm.pushNotificationTracking.pushProviderMessageID"));
            assertEquals("actionId", edgeTrackingData.get("xdm.pushNotificationTracking.customAction.actionID"));
            assertEquals("1", edgeTrackingData.get("xdm.application.launches.value"));
            assertEquals("pushClicked", edgeTrackingData.get("xdm.eventType"));
            assertEquals("mock_datasetId", edgeTrackingData.get("meta.collect.datasetId"));
            assertEquals("fcm", edgeTrackingData.get("xdm.pushNotificationTracking.pushProvider"));
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
            mockConfigSharedState();
            final Event event = samplePushTrackingEvent(null, "messageId", "actionId", true);
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.processEvent(event);

            // verify push tracking status event sent
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
            final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
            assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
            assertEquals(PushTrackingStatus.UNKNOWN_ERROR.getValue(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
            assertEquals(PushTrackingStatus.UNKNOWN_ERROR.getDescription(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatusMessage"));
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenMessageIdNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            mockConfigSharedState();
            final Event event = samplePushTrackingEvent("pushOpened", null, "actionId", true);
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.processEvent(event);

            // verify push tracking status event sent
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
            final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
            assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
            assertEquals(PushTrackingStatus.INVALID_MESSAGE_ID.getValue(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
            assertEquals(PushTrackingStatus.INVALID_MESSAGE_ID.getDescription(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatusMessage"));
        });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenExperienceEventDatasetIdIsEmpty() {
        runUsingMockedServiceProvider(() -> {
            // setup
            when(mockConfigData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("messaging.eventDataset", "");
            }});
            mockConfigSharedState();
            final Event event = samplePushTrackingEvent("pushOpened", "messageId", "actionId", true);
            final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

            // test
            messagingExtension.processEvent(event);

            // verify push tracking status event sent
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
            final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
            assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
            assertEquals(PushTrackingStatus.NO_DATASET_CONFIGURED.getValue(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
            assertEquals(PushTrackingStatus.NO_DATASET_CONFIGURED.getDescription(), pushTrackingStatusEvent.getEventData().get("pushTrackingStatusMessage"));
        });
    }

    @Test
    public void test_processEvent_fetchMessagesEvent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("refreshmessages", true);
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            verify(mockEdgePersonalizationResponseHandler, times(1)).fetchMessages(mockEvent, null);
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
            verify(mockEdgePersonalizationResponseHandler, times(1)).handleEdgePersonalizationNotification(any(Event.class));
        });
    }

    @Test
    public void test_processEvent_updatePropositionsEvent() {
        ArgumentCaptor<List<Surface>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
        List<Map<String, Object>> surfaces = new ArrayList<>();
        Map<String, Object> surface1 = new HashMap<>();
        Map<String, Object> surface2 = new HashMap<>();
        surface1.put("uri", "mobileapp://mockPackageName/promos/feed1");
        surface2.put("uri", "mobileapp://mockPackageName/promos/feed2");
        surfaces.add(surface1);
        surfaces.add(surface2);
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("updatepropositions", true);
            eventData.put("surfaces", surfaces);
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);
            when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
            when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.REQUEST_CONTENT);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify
            verify(mockEdgePersonalizationResponseHandler, times(1)).fetchMessages(any(Event.class), listArgumentCaptor.capture());
            List<Surface> capturedSurfaces = listArgumentCaptor.getValue();
            assertEquals(2, capturedSurfaces.size());
            List<String> sortedList = new ArrayList<>();
            sortedList.add(capturedSurfaces.get(0).getUri());
            sortedList.add(capturedSurfaces.get(1).getUri());
            sortedList.sort(null);
            assertEquals("mobileapp://mockPackageName/promos/feed1", sortedList.get(0));
            assertEquals("mobileapp://mockPackageName/promos/feed2", sortedList.get(1));
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
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionInteract\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"interact\":1},\"propositionAction\":{\"id\":\"confirm\",\"label\":\"confirm\"},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mockPackageName\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"confirm\",\"eventType\":\"interact\"}}"));
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
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionDismiss\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"dismiss\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mockPackageName\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"dismiss\"}}"));
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
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionDisplay\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"display\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mockPackageName\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"display\"}}"));
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
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionTrigger\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"trigger\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mockPackageName\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"trigger\"}}"));
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

    // ========================================================================================
    // private helpers
    // ========================================================================================
    private Event samplePushTrackingEvent(final String eventType, final String messageId, final String actionId, final boolean applicationOpened) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(TRACK_INFO_KEY_EVENT_TYPE, eventType);
        eventData.put(TRACK_INFO_KEY_MESSAGE_ID, messageId);
        eventData.put(TRACK_INFO_KEY_ACTION_ID, actionId);
        eventData.put(TRACK_INFO_KEY_APPLICATION_OPENED, applicationOpened);
        eventData.put(TRACK_INFO_KEY_ADOBE_XDM, "{ \"cjm\": {\"trackingkey\": \"trackingvalue\"}}");

        final Event event = new Event.Builder("mock_event_name", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
        return event;
    }

    private void mockConfigSharedState() {
        when(mockExtensionApi.getSharedState(
                eq(MessagingConstants.SharedState.Configuration.EXTENSION_NAME),
                any(Event.class),
                eq(false),
                eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
    }
}