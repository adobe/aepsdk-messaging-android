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

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.DataStoring;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingExtensionTests {

    // Mocks
    @Mock ExtensionApi mockExtensionApi;
    @Mock ServiceProvider mockServiceProvider;
    @Mock DataStoring mockDataStoring;
    @Mock NamedCollection mockNamedCollection;
    @Mock CacheService mockCacheService;
    @Mock DeviceInforming mockDeviceInfoService;
    @Mock LaunchRulesEngine mockMessagingRulesEngine;
    @Mock ContentCardRulesEngine mockContentCardRulesEngine;
    @Mock EdgePersonalizationResponseHandler mockEdgePersonalizationResponseHandler;
    @Mock SharedStateResult mockConfigData;
    @Mock SharedStateResult mockEdgeIdentityData;
    @Mock Application mockApplication;
    @Mock PresentableMessageMapper.InternalMessage mockInternalMessage;
    @Mock LaunchRule mockLaunchRule;
    @Mock RuleConsequence mockRuleConsequence;
    @Mock SerialWorkDispatcher<Event> mockSerialWorkDispatcher;
    @Mock AdobeCallback mockAdobeCallback;

    private static final String mockCJMData =
            "{\n"
                + "        \"mixins\" :{\n"
                + "          \"_experience\": {\n"
                + "            \"customerJourneyManagement\": {\n"
                + "              \"messageExecution\": {\n"
                + "                \"messageExecutionID\": \"16-Sept-postman\",\n"
                + "                \"messageID\": \"567\",\n"
                + "                \"journeyVersionID\": \"some-journeyVersionId\",\n"
                + "                \"journeyVersionInstanceId\": \"someJourneyVersionInstanceId\"\n"
                + "              }\n"
                + "            }\n"
                + "          }\n"
                + "        }\n"
                + "      }";

    private MessagingExtension messagingExtension;
    private CompletionHandler handler1;
    private CompletionHandler handler2;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        handler1 = new CompletionHandler("originatingId", mockAdobeCallback);
        handler1.edgeRequestEventId = "edgeRequestId";
        handler2 = new CompletionHandler("originatingId2", mockAdobeCallback);
        handler2.edgeRequestEventId = "edgeRequestId2";

        List<CompletionHandler> handlers = new ArrayList<>();
        handlers.add(handler1);
        handlers.add(handler2);

        synchronized (MessagingExtension.completionHandlersMutex) {
            MessagingExtension.completionHandlers = handlers;
        }
    }

    @After
    public void tearDown() {
        reset(mockExtensionApi);
        reset(mockServiceProvider);
        reset(mockCacheService);
        reset(mockMessagingRulesEngine);
        reset(mockContentCardRulesEngine);
        reset(mockEdgePersonalizationResponseHandler);
        reset(mockConfigData);
        reset(mockEdgeIdentityData);
        reset(mockDeviceInfoService);
        reset(mockApplication);
        reset(mockInternalMessage);
        reset(mockLaunchRule);
        reset(mockRuleConsequence);
        reset(mockSerialWorkDispatcher);
        reset(mockAdobeCallback);
        reset(mockNamedCollection);
        reset(mockDataStoring);
        InternalMessagingUtils.resetPushTokenSyncTimestamp();
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockDataStoring.getNamedCollection(anyString())).thenReturn(mockNamedCollection);
            when(mockServiceProvider.getDataStoreService()).thenReturn(mockDataStoring);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");
            when(mockCacheService.get(any(), any())).thenReturn(null);
            when(mockConfigData.getValue())
                    .thenReturn(
                            new HashMap<String, Object>() {
                                {
                                    put("messaging.eventDataset", "mock_datasetId");
                                }
                            });
            when(mockEdgeIdentityData.getValue())
                    .thenReturn(
                            new HashMap<String, Object>() {
                                {
                                    put("key", "value");
                                }
                            });

            messagingExtension =
                    new MessagingExtension(
                            mockExtensionApi,
                            mockMessagingRulesEngine,
                            mockContentCardRulesEngine,
                            mockEdgePersonalizationResponseHandler);

            runnable.run();
        }
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_TestableConstructor() {
        runUsingMockedServiceProvider(
                () -> {
                    assertNotNull(messagingExtension.messagingRulesEngine);
                    assertNotNull(messagingExtension.edgePersonalizationResponseHandler);
                });
    }

    @Test
    public void test_Constructor() {
        runUsingMockedServiceProvider(
                () -> {
                    messagingExtension = new MessagingExtension(mockExtensionApi);
                    assertNotNull(messagingExtension.messagingRulesEngine);
                    assertNotNull(messagingExtension.edgePersonalizationResponseHandler);
                });
    }

    @Test
    public void test_onRegistered_pushTokenIsStoredInNamedCollection() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    when(mockNamedCollection.getString(anyString(), any()))
                            .thenReturn("mockPushToken");
                    messagingExtension.setSerialWorkDispatcher(mockSerialWorkDispatcher);

                    // test
                    messagingExtension.onRegistered();

                    // verify 7 listeners are registered
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.GENERIC_IDENTITY),
                                    eq(EventSource.REQUEST_CONTENT),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.GENERIC_IDENTITY),
                                    eq(EventSource.REQUEST_RESET),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.EDGE),
                                    eq(
                                            MessagingTestConstants.EventSource
                                                    .PERSONALIZATION_DECISIONS),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.WILDCARD), eq(EventSource.WILDCARD), any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.MESSAGING),
                                    eq(EventSource.REQUEST_CONTENT),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.RULES_ENGINE),
                                    eq(EventSource.RESPONSE_CONTENT),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.MESSAGING),
                                    eq(EventSource.CONTENT_COMPLETE),
                                    any());

                    // verify serial dispatcher started
                    verify(mockSerialWorkDispatcher, times(1)).start();

                    // verify push token is added to the shared state if it is present in the
                    // named collection
                    verify(mockNamedCollection, times(1))
                            .getString(
                                    eq(
                                            MessagingConstants.NamedCollectionKeys.Messaging
                                                    .PUSH_IDENTIFIER),
                                    any());
                    // verify push token is stored in shared state
                    verify(mockExtensionApi, times(1))
                            .createSharedState(
                                    argThat(
                                            map ->
                                                    map.containsKey(
                                                            MessagingConstants.SharedState.Messaging
                                                                    .PUSH_IDENTIFIER)),
                                    any());
                });
    }

    @Test
    public void test_onRegistered_pushTokenNotStoredInNamedCollection() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    messagingExtension.setSerialWorkDispatcher(mockSerialWorkDispatcher);

                    // test
                    messagingExtension.onRegistered();

                    // verify 7 listeners are registered
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.GENERIC_IDENTITY),
                                    eq(EventSource.REQUEST_CONTENT),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.GENERIC_IDENTITY),
                                    eq(EventSource.REQUEST_RESET),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.EDGE),
                                    eq(
                                            MessagingTestConstants.EventSource
                                                    .PERSONALIZATION_DECISIONS),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.WILDCARD), eq(EventSource.WILDCARD), any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.MESSAGING),
                                    eq(EventSource.REQUEST_CONTENT),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.RULES_ENGINE),
                                    eq(EventSource.RESPONSE_CONTENT),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .registerEventListener(
                                    eq(EventType.MESSAGING),
                                    eq(EventSource.CONTENT_COMPLETE),
                                    any());

                    // verify serial dispatcher started
                    verify(mockSerialWorkDispatcher, times(1)).start();

                    // verify null push token is added to the shared state if it isn't present in
                    // the
                    // named collection
                    verify(mockNamedCollection, times(1))
                            .getString(
                                    eq(
                                            MessagingConstants.NamedCollectionKeys.Messaging
                                                    .PUSH_IDENTIFIER),
                                    any());
                    verify(mockExtensionApi, times(1))
                            .createSharedState(argThat(Map::isEmpty), isNull());
                });
    }

    // ========================================================================================
    // SerialWorkDispatcher tests
    // ========================================================================================
    @Test
    public void test_serialWorkDispatcher_offerRetrieveMessageEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Event event =
                            new Event.Builder(
                                            "Test Get propositions",
                                            EventType.MESSAGING,
                                            EventSource.REQUEST_CONTENT)
                                    .setEventData(
                                            new HashMap<String, Object>() {
                                                {
                                                    put(
                                                            MessagingTestConstants.EventDataKeys
                                                                    .Messaging.GET_PROPOSITIONS,
                                                            true);
                                                }
                                            })
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
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .retrieveInMemoryPropositions(any(), any());
                });
    }

    @Test
    public void test_serialWorkDispatcher_offerEdgeContentCompletedEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, List<Surface>> requestedSurfacesForEventId = new HashMap<>();
                    requestedSurfacesForEventId.put(
                            "testEventId",
                            new ArrayList<Surface>() {
                                {
                                    add(new Surface());
                                }
                            });
                    when(mockEdgePersonalizationResponseHandler.getRequestedSurfacesForEventId())
                            .thenReturn(requestedSurfacesForEventId);
                    messagingExtension =
                            new MessagingExtension(
                                    mockExtensionApi,
                                    mockMessagingRulesEngine,
                                    mockContentCardRulesEngine,
                                    mockEdgePersonalizationResponseHandler);

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

                    // verify serialWorkDispatcher.offer returns false as this is a content complete
                    // event with a matching event id
                    assertEquals(true, messagingExtension.getSerialWorkDispatcher().offer(event));
                });
    }

    @Test
    public void test_serialWorkDispatcher_offerNonEdgeEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Event event =
                            new Event.Builder(
                                            "Not an Edge Content Complete event",
                                            EventType.WILDCARD,
                                            EventSource.RESPONSE_CONTENT)
                                    .build();

                    // test
                    messagingExtension.onRegistered();

                    // verify serialWorkDispatcher.offer returns true as this is an unhandled event
                    // type
                    assertEquals(true, messagingExtension.getSerialWorkDispatcher().offer(event));
                });
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    String moduleName = messagingExtension.getName();
                    assertEquals(
                            "getName should return the correct module name",
                            MessagingTestConstants.EXTENSION_NAME,
                            moduleName);
                });
    }

    @Test
    public void test_getFriendlyName() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    String friendlyName = messagingExtension.getFriendlyName();
                    assertEquals(
                            "getFriendlyName should return the correct value",
                            MessagingTestConstants.FRIENDLY_EXTENSION_NAME,
                            friendlyName);
                });
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        runUsingMockedServiceProvider(
                () -> {
                    // test
                    String moduleVersion = messagingExtension.getVersion();
                    assertEquals(
                            "getVersion should return the correct module version",
                            Messaging.extensionVersion(),
                            moduleVersion);
                });
    }

    // =================================================================================================================
    // readyForEvent
    // =================================================================================================================
    @Test
    public void
            test_readyForEvent_when_eventReceived_and_configurationAndIdentitySharedStateDataPresent_then_readyForEventTrue() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    when(mockExtensionApi.getSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.Configuration
                                                    .EXTENSION_NAME),
                                    any(Event.class),
                                    anyBoolean(),
                                    any(SharedStateResolution.class)))
                            .thenReturn(mockConfigData);
                    when(mockExtensionApi.getXDMSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.EdgeIdentity
                                                    .EXTENSION_NAME),
                                    any(Event.class),
                                    anyBoolean(),
                                    any(SharedStateResolution.class)))
                            .thenReturn(mockEdgeIdentityData);

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.CONFIGURATION,
                                            EventSource.RESPONSE_CONTENT)
                                    .build();

                    // verify
                    assertTrue(messagingExtension.readyForEvent(testEvent));
                });
    }

    @Test
    public void
            test_readyForEvent_when_eventReceived_and_configurationSharedStateNotReady_then_readyForEventIsFalse() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    when(mockExtensionApi.getSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.Configuration
                                                    .EXTENSION_NAME),
                                    any(Event.class),
                                    anyBoolean(),
                                    any(SharedStateResolution.class)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.PENDING, new HashMap<>()));
                    when(mockExtensionApi.getXDMSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.EdgeIdentity
                                                    .EXTENSION_NAME),
                                    any(Event.class),
                                    anyBoolean(),
                                    any(SharedStateResolution.class)))
                            .thenReturn(mockEdgeIdentityData);

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.CONFIGURATION,
                                            EventSource.RESPONSE_CONTENT)
                                    .build();

                    // verify
                    assertFalse(messagingExtension.readyForEvent(testEvent));
                });
    }

    @Test
    public void
            test_readyForEvent_when_eventReceived_and_identitySharedStateNotReady_then_readyForEventIsFalse() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    when(mockExtensionApi.getSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.Configuration
                                                    .EXTENSION_NAME),
                                    any(Event.class),
                                    anyBoolean(),
                                    any(SharedStateResolution.class)))
                            .thenReturn(mockConfigData);
                    when(mockExtensionApi.getXDMSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.EdgeIdentity
                                                    .EXTENSION_NAME),
                                    any(Event.class),
                                    anyBoolean(),
                                    any(SharedStateResolution.class)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.PENDING, new HashMap<>()));

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.CONFIGURATION,
                                            EventSource.RESPONSE_CONTENT)
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
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("key", "value");
                    Event mockEvent = mock(Event.class);
                    List<RuleConsequence> mockRuleConsequenceList = new ArrayList<>();
                    mockRuleConsequenceList.add(mockRuleConsequence);

                    when(mockMessagingRulesEngine.evaluateEvent(any()))
                            .thenReturn(mockRuleConsequenceList);
                    when(mockEvent.getEventData()).thenReturn(eventData);
                    when(mockEvent.getType()).thenReturn(EventType.GENERIC_TRACK);
                    when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);

                    // test
                    messagingExtension.handleWildcardEvents(mockEvent);

                    // verify rules engine processes event
                    verify(mockMessagingRulesEngine, times(1)).processEvent(eq(mockEvent));
                });
    }

    @Test
    public void test_handleWildcardEvents_when_validAssuranceSpoofEventReceived() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    Map<String, Object> triggeredConsequenceMap = new HashMap<>();
                    triggeredConsequenceMap.put(
                            MessagingTestConstants.EventDataKeys.RulesEngine
                                    .MESSAGE_CONSEQUENCE_TYPE,
                            MessagingTestConstants.ConsequenceDetailKeys.SCHEMA);
                    triggeredConsequenceMap.put(
                            MessagingTestConstants.EventDataKeys.RulesEngine
                                    .MESSAGE_CONSEQUENCE_DETAIL,
                            new HashMap<>());
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED,
                            triggeredConsequenceMap);

                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getName())
                            .thenReturn(
                                    MessagingTestConstants.EventName
                                            .ASSURANCE_SPOOFED_IAM_EVENT_NAME);
                    when(mockEvent.getEventData()).thenReturn(eventData);

                    // test
                    messagingExtension.handleWildcardEvents(mockEvent);

                    // verify
                    verify(mockMessagingRulesEngine, times(0)).processEvent(mockEvent);
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .createInAppMessage(any());
                });
    }

    @Test
    public void
            test_handleWildcardEvents_whenTriggeredConsequenceMapIsNull_thenIgnoresAssuranceEvent() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getName())
                            .thenReturn(
                                    MessagingTestConstants.EventName
                                            .ASSURANCE_SPOOFED_IAM_EVENT_NAME);
                    when(mockEvent.getEventData()).thenReturn(null);

                    // test
                    messagingExtension.handleWildcardEvents(mockEvent);

                    // verify
                    verify(mockMessagingRulesEngine, times(0)).processEvent(mockEvent);
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any());
                });
    }

    @Test
    public void
            test_handleWildcardEvents_whenTriggeredConsequenceMapIsEmpty_thenIgnoresAssuranceEvent() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getName())
                            .thenReturn(
                                    MessagingTestConstants.EventName
                                            .ASSURANCE_SPOOFED_IAM_EVENT_NAME);
                    when(mockEvent.getEventData()).thenReturn(new HashMap<>());

                    // test
                    messagingExtension.handleWildcardEvents(mockEvent);

                    // verify
                    verify(mockMessagingRulesEngine, times(0)).processEvent(mockEvent);
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any());
                });
    }

    @Test
    public void
            test_handleWildcardEvents_whenConsequenceTypeIsNotSchema_thenIgnoresAssuranceEvent() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    Map<String, Object> triggeredConsequenceMap = new HashMap<>();
                    triggeredConsequenceMap.put(
                            MessagingTestConstants.EventDataKeys.RulesEngine
                                    .MESSAGE_CONSEQUENCE_TYPE,
                            "notSchema");
                    triggeredConsequenceMap.put(
                            MessagingTestConstants.EventDataKeys.RulesEngine
                                    .MESSAGE_CONSEQUENCE_DETAIL,
                            new HashMap<>());
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED,
                            triggeredConsequenceMap);

                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getName())
                            .thenReturn(
                                    MessagingTestConstants.EventName
                                            .ASSURANCE_SPOOFED_IAM_EVENT_NAME);
                    when(mockEvent.getEventData()).thenReturn(eventData);

                    // test
                    messagingExtension.handleWildcardEvents(mockEvent);

                    // verify
                    verify(mockMessagingRulesEngine, times(0)).processEvent(mockEvent);
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any());
                });
    }

    // =================================================================================================================
    // handleRuleEngineResponseEvents
    // =================================================================================================================
    @Test
    public void
            test_handleRuleEngineResponseEvents_when_validConsequence_then_createInAppMessageCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    List<String> assetList = new ArrayList<>();
                    assetList.add("remoteAsset.png");
                    Map<String, Object> data =
                            new HashMap<String, Object>() {
                                {
                                    put("remoteAssets", assetList);
                                    put("mobileParameters", new HashMap<String, Object>());
                                    put("html", "iam html content");
                                }
                            };
                    Map<String, Object> detail =
                            new HashMap<String, Object>() {
                                {
                                    put("id", "testId");
                                    put("schema", SchemaType.INAPP.toString());
                                    put("data", data);
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

                    Map<String, Object> ruleConsequenceMap =
                            new HashMap<String, Object>() {
                                {
                                    {
                                        put("triggeredconsequence", triggeredConsequence);
                                    }
                                }
                            };

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(ruleConsequenceMap)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    @Test
    public void
            test_handleRuleEngineResponseEvents_when_nullEventData_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(null)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    @Test
    public void
            test_handleRuleEngineResponseEvents_when_nullTriggeredConsequence_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    List<String> assetList = new ArrayList<>();
                    assetList.add("remoteAsset.png");
                    Map<String, Object> ruleConsequenceMap =
                            new HashMap<String, Object>() {
                                {
                                    {
                                        put("triggeredconsequence", null);
                                    }
                                }
                            };

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(ruleConsequenceMap)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    @Test
    public void
            test_handleRuleEngineResponseEvents_when_invalidType_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    List<String> assetList = new ArrayList<>();
                    assetList.add("remoteAsset.png");
                    Map<String, Object> data =
                            new HashMap<String, Object>() {
                                {
                                    put("remoteAssets", assetList);
                                    put("mobileParameters", new HashMap<String, Object>());
                                    put("html", "iam html content");
                                }
                            };
                    Map<String, Object> detail =
                            new HashMap<String, Object>() {
                                {
                                    put("id", "testId");
                                    put("schema", SchemaType.INAPP.toString());
                                    put("data", data);
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

                    Map<String, Object> ruleConsequenceMap =
                            new HashMap<String, Object>() {
                                {
                                    {
                                        put("triggeredconsequence", triggeredConsequence);
                                    }
                                }
                            };

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(ruleConsequenceMap)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    @Test
    public void
            test_handleRuleEngineResponseEvents_when_nullDetailsPresentInConsequence_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> triggeredConsequence =
                            new HashMap<String, Object>() {
                                {
                                    put("id", "testId");
                                    put("type", "schema");
                                    put("detail", null);
                                }
                            };

                    Map<String, Object> ruleConsequenceMap =
                            new HashMap<String, Object>() {
                                {
                                    {
                                        put("triggeredconsequence", triggeredConsequence);
                                    }
                                }
                            };

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(ruleConsequenceMap)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    @Test
    public void
            test_handleRuleEngineResponseEvents_when_invalidDetailsPresentInConsequence_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> detail =
                            new HashMap<String, Object>() {
                                {
                                    put("id", "testId");
                                    put("schema", SchemaType.INAPP.toString());
                                    put("data", null);
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

                    Map<String, Object> ruleConsequenceMap =
                            new HashMap<String, Object>() {
                                {
                                    {
                                        put("triggeredconsequence", triggeredConsequence);
                                    }
                                }
                            };

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(ruleConsequenceMap)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    @Test
    public void
            test_handleRuleEngineResponseEvents_when_schemaTypeIsNotInApp_then_createInAppMessageNotCalled() {
        // setup
        runUsingMockedServiceProvider(
                () -> {
                    List<String> assetList = new ArrayList<>();
                    assetList.add("remoteAsset.png");
                    Map<String, Object> data =
                            new HashMap<String, Object>() {
                                {
                                    put("remoteAssets", assetList);
                                    put("mobileParameters", new HashMap<String, Object>());
                                    put("html", "iam html content");
                                }
                            };
                    Map<String, Object> detail =
                            new HashMap<String, Object>() {
                                {
                                    put("id", "testId");
                                    put("schema", SchemaType.HTML_CONTENT.toString());
                                    put("data", data);
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

                    Map<String, Object> ruleConsequenceMap =
                            new HashMap<String, Object>() {
                                {
                                    {
                                        put("triggeredconsequence", triggeredConsequence);
                                    }
                                }
                            };

                    Event testEvent =
                            new Event.Builder(
                                            "Test event",
                                            EventType.RULES_ENGINE,
                                            EventSource.RESPONSE_CONTENT)
                                    .setEventData(ruleConsequenceMap)
                                    .build();

                    // test
                    messagingExtension.handleRuleEngineResponseEvents(testEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .createInAppMessage(any(PropositionItem.class));
                });
    }

    // ========================================================================================
    // processEvents
    // ========================================================================================
    @Test
    public void test_processEvent_when_NullEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<Log> logMockedStatic = mockStatic(Log.class)) {
                        // test
                        messagingExtension.processEvent(null);

                        // verify
                        logMockedStatic.verify(
                                () ->
                                        Log.debug(
                                                anyString(),
                                                anyString(),
                                                eq("Invalid event, ignoring.")),
                                times(1));
                    }
                });
    }

    // ========================================================================================
    // processEvents GenericIdentityRequestEvent
    // ========================================================================================
    @Test
    public void test_processEvent_genericIdentityEvent_whenEventContainsPushToken() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"data\":{\"pushNotificationDetails\":[{\"denylisted\":false,\"identity\":{\"namespace\":{\"code\":\"ECID\"},\"id\":\"mock_ecid\"},\"appID\":\"mockPackageName\",\"platform\":\"fcm\",\"token\":\"mock_push_token\"}]}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
                    final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
                    final Map<String, Object> ecidsMap = new HashMap<>();
                    final Map<String, Object> identityMap = new HashMap<>();
                    final List<Map<String, Object>> ecidList = new ArrayList<>();
                    identityMap.put(
                            MessagingTestConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
                    ecidList.add(identityMap);
                    ecidsMap.put("ECID", ecidList);
                    mockEdgeIdentityState.put("identityMap", ecidsMap);
                    when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

                    try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                            Mockito.mockStatic(MobileCore.class)) {
                        mobileCoreMockedStatic
                                .when(MobileCore::getApplication)
                                .thenReturn(mockApplication);
                        when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put(
                                MessagingTestConstants.EventDataKeys.Identity.PUSH_IDENTIFIER,
                                "mock_push_token");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                        when(mockEvent.getTimestamp()).thenReturn(System.currentTimeMillis());
                        when(mockExtensionApi.getSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.Configuration
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockConfigData);
                        when(mockExtensionApi.getXDMSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.EdgeIdentity
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockEdgeIdentityData);

                        // test
                        messagingExtension.processEvent(mockEvent);

                        // verify
                        // 1 event dispatched: edge event with push profile data
                        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

                        // verify event
                        Event event = eventCaptor.getValue();
                        assertNotNull(event.getEventData());
                        assertEquals(
                                MessagingTestConstants.EventName.PUSH_PROFILE_EDGE_EVENT,
                                event.getName());
                        assertEquals(MessagingTestConstants.EventType.EDGE, event.getType());
                        assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
                        assertEquals(expectedEventData, event.getEventData());

                        // verify push token is stored in shared state
                        verify(mockExtensionApi, times(1))
                                .createSharedState(
                                        argThat(
                                                map ->
                                                        map.containsKey(
                                                                MessagingConstants.SharedState
                                                                        .Messaging
                                                                        .PUSH_IDENTIFIER)),
                                        any(Event.class));
                    }
                });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEventHasNullPushToken() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
                    final Map<String, Object> ecidsMap = new HashMap<>();
                    final Map<String, Object> identityMap = new HashMap<>();
                    final List<Map<String, Object>> ecidList = new ArrayList<>();
                    identityMap.put(
                            MessagingTestConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
                    ecidList.add(identityMap);
                    ecidsMap.put("ECID", ecidList);
                    mockEdgeIdentityState.put("identityMap", ecidsMap);
                    when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

                    try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                            Mockito.mockStatic(MobileCore.class)) {
                        mobileCoreMockedStatic
                                .when(MobileCore::getApplication)
                                .thenReturn(mockApplication);
                        when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put(
                                MessagingTestConstants.EventDataKeys.Identity.PUSH_IDENTIFIER,
                                null);
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                        when(mockExtensionApi.getSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.Configuration
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockConfigData);
                        when(mockExtensionApi.getXDMSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.EdgeIdentity
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockEdgeIdentityData);

                        // test
                        messagingExtension.processEvent(mockEvent);

                        // verify
                        // no event dispatched: edge event with push profile data
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                        // verify no push token is stored in shared state
                        verify(mockExtensionApi, times(0))
                                .createSharedState(any(Map.class), any(Event.class));
                    }
                });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEventDataIsNull() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
                    final Map<String, Object> ecidsMap = new HashMap<>();
                    final Map<String, Object> identityMap = new HashMap<>();
                    final List<Map<String, Object>> ecidList = new ArrayList<>();
                    identityMap.put(
                            MessagingTestConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
                    ecidList.add(identityMap);
                    ecidsMap.put("ECID", ecidList);
                    mockEdgeIdentityState.put("identityMap", ecidsMap);
                    when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

                    try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                            Mockito.mockStatic(MobileCore.class)) {
                        mobileCoreMockedStatic
                                .when(MobileCore::getApplication)
                                .thenReturn(mockApplication);
                        when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                        Map<String, Object> eventData = new HashMap<>();
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                        when(mockExtensionApi.getSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.Configuration
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockConfigData);
                        when(mockExtensionApi.getXDMSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.EdgeIdentity
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockEdgeIdentityData);

                        // test
                        messagingExtension.processEvent(mockEvent);

                        // verify
                        // no event dispatched: edge event with push profile data
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                        // verify no push token is stored in shared state
                        verify(mockExtensionApi, times(0))
                                .createSharedState(any(Map.class), any(Event.class));
                    }
                });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEventHasEmptyPushToken() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
                    final Map<String, Object> ecidsMap = new HashMap<>();
                    final Map<String, Object> identityMap = new HashMap<>();
                    final List<Map<String, Object>> ecidList = new ArrayList<>();
                    identityMap.put(
                            MessagingTestConstants.SharedState.EdgeIdentity.ID, "mock_ecid");
                    ecidList.add(identityMap);
                    ecidsMap.put("ECID", ecidList);
                    mockEdgeIdentityState.put("identityMap", ecidsMap);
                    when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

                    try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                            Mockito.mockStatic(MobileCore.class)) {
                        mobileCoreMockedStatic
                                .when(MobileCore::getApplication)
                                .thenReturn(mockApplication);
                        when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put(
                                MessagingTestConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                        when(mockExtensionApi.getSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.Configuration
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockConfigData);
                        when(mockExtensionApi.getXDMSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.EdgeIdentity
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockEdgeIdentityData);

                        // test
                        messagingExtension.processEvent(mockEvent);

                        // verify
                        // no event dispatched: edge event with push profile data
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                        // verify no push token is stored in shared state
                        verify(mockExtensionApi, times(0))
                                .createSharedState(any(Map.class), any(Event.class));
                    }
                });
    }

    @Test
    public void test_processEvent_genericIdentityEvent_whenEcidIsNull() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    final Map<String, Object> mockEdgeIdentityState = new HashMap<>();
                    final Map<String, Object> ecidsMap = new HashMap<>();
                    final Map<String, Object> identityMap = new HashMap<>();
                    final List<Map<String, Object>> ecidList = new ArrayList<>();
                    identityMap.put(MessagingTestConstants.SharedState.EdgeIdentity.ID, null);
                    ecidList.add(identityMap);
                    ecidsMap.put("ECID", ecidList);
                    mockEdgeIdentityState.put("identityMap", ecidsMap);
                    when(mockEdgeIdentityData.getValue()).thenReturn(mockEdgeIdentityState);

                    try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                            Mockito.mockStatic(MobileCore.class)) {
                        mobileCoreMockedStatic
                                .when(MobileCore::getApplication)
                                .thenReturn(mockApplication);
                        when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put(
                                MessagingTestConstants.EventDataKeys.Identity.PUSH_IDENTIFIER,
                                "mock_push_token");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                        when(mockExtensionApi.getSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.Configuration
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockConfigData);
                        when(mockExtensionApi.getXDMSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.EdgeIdentity
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockEdgeIdentityData);

                        // test
                        messagingExtension.processEvent(mockEvent);

                        // verify
                        // no event dispatched: edge event with push profile data
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));

                        // verify no push token is stored in shared state
                        verify(mockExtensionApi, times(0))
                                .createSharedState(any(Map.class), any(Event.class));
                    }
                });
    }

    // ========================================================================================
    // processEvents GenericIdentityRequestResetEvent
    // ========================================================================================
    @Test
    public void test_processEvent_genericIdentityRequestResetEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                            Mockito.mockStatic(MobileCore.class)) {
                        mobileCoreMockedStatic
                                .when(MobileCore::getApplication)
                                .thenReturn(mockApplication);
                        when(mockApplication.getPackageName()).thenReturn("mockPackageName");

                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY);
                        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_RESET);
                        when(mockExtensionApi.getSharedState(
                                        eq(
                                                MessagingTestConstants.SharedState.Configuration
                                                        .EXTENSION_NAME),
                                        eq(mockEvent),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(mockConfigData);

                        // test
                        messagingExtension.processEvent(mockEvent);

                        // verify
                        // messaging shared state cleared by adding an empty state
                        verify(mockExtensionApi, times(1))
                                .createSharedState(argThat(Map::isEmpty), eq(mockEvent));

                        // verify push token removed from named collection
                        verify(mockNamedCollection, times(1))
                                .remove(
                                        MessagingConstants.NamedCollectionKeys.Messaging
                                                .PUSH_IDENTIFIER);
                    }
                });
    }

    // ========================================================================================
    // processEvents MessagingRequestContentEvent
    // ========================================================================================
    @Test
    public void test_processEvent_messageTrackingEvent_whenApplicationOpened() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent("pushOpened", "messageId", null, true, null);
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
                    assertEquals(
                            PushTrackingStatus.TRACKING_INITIATED.getValue(),
                            pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
                    assertEquals(
                            PushTrackingStatus.TRACKING_INITIATED.getDescription(),
                            pushTrackingStatusEvent
                                    .getEventData()
                                    .get("pushTrackingStatusMessage"));

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertEquals(
                            "messageId",
                            edgeTrackingData.get(
                                    "xdm.pushNotificationTracking.pushProviderMessageID"));
                    assertEquals("1", edgeTrackingData.get("xdm.application.launches.value"));
                    assertEquals("pushOpened", edgeTrackingData.get("xdm.eventType"));
                    assertEquals("mock_datasetId", edgeTrackingData.get("meta.collect.datasetId"));
                    assertEquals(
                            "fcm",
                            edgeTrackingData.get("xdm.pushNotificationTracking.pushProvider"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenApplicationOpened_withCustomAction() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushClicked", "messageId", "actionId", true, null);
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify rules engine processes event
                    verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

                    // verify push tracking status event
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertNull(edgeTrackingData.get("xdm.trackingkey"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_adobeXDMIsValid() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushClicked",
                                    "messageId",
                                    "actionId",
                                    true,
                                    "{\"cjm\": {\"_experience\": {\"customerJourneyManagement\": {"
                                            + " \"trackingkey\": \"trackingValue\"}}}}");
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify rules engine processes event
                    verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

                    // verify push tracking status event
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertEquals(
                            "trackingValue",
                            edgeTrackingData.get(
                                    "xdm._experience.customerJourneyManagement.trackingkey"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_adobeXDMIsMalformed() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushClicked",
                                    "messageId",
                                    "actionId",
                                    true,
                                    "{ \"cjm\": {\"trackingkey\": \"trackingvalue\"}");
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify rules engine processes event
                    verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

                    // verify push tracking status event
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertNull(edgeTrackingData.get("xdm.trackingkey"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_adobeXDMCJMKeyIsNotMap() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushClicked",
                                    "messageId",
                                    "actionId",
                                    true,
                                    "[ \"cjm\": {\"trackingkey\": \"trackingvalue\"}]");
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify rules engine processes event
                    verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

                    // verify push tracking status event
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertNull(edgeTrackingData.get("xdm.trackingkey"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_adobeXDMHasMixinsKey() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushClicked",
                                    "messageId",
                                    "actionId",
                                    true,
                                    "{ \"mixins\": {\"mixinKey\": \"mixinValue\"}}");
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify rules engine processes event
                    verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

                    // verify push tracking status event
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertEquals("mixinValue", edgeTrackingData.get("xdm.mixinKey"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_adobeXDMMixinsKeyIsNotMap() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushClicked",
                                    "messageId",
                                    "actionId",
                                    true,
                                    "{ \"mixins\": \"string\"}");
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify rules engine processes event
                    verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

                    // verify push tracking status event
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());

                    // verify push tracking status event
                    final Event pushTrackingEdgeEvent = eventCaptor.getAllValues().get(1);
                    assertEquals("Push tracking edge event", pushTrackingEdgeEvent.getName());
                    assertEquals(EventType.EDGE, pushTrackingEdgeEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, pushTrackingEdgeEvent.getSource());
                    // verify edge tracking event data
                    Map<String, String> edgeTrackingData =
                            MessagingTestUtils.flattenMap(pushTrackingEdgeEvent.getEventData());
                    assertNull(edgeTrackingData.get("xdm.mixinKey"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenEventDataNull() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getEventData()).thenReturn(null);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT);
                    when(mockExtensionApi.getSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.Configuration
                                                    .EXTENSION_NAME),
                                    eq(mockEvent),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(mockConfigData);
                    when(mockExtensionApi.getXDMSharedState(
                                    eq(
                                            MessagingTestConstants.SharedState.EdgeIdentity
                                                    .EXTENSION_NAME),
                                    eq(mockEvent),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(mockEdgeIdentityData);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    // no edge event dispatched
                    verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenTrackInfoEventTypeNull() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(null, "messageId", "actionId", true, null);
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify push tracking status event sent
                    verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
                    assertEquals(
                            PushTrackingStatus.UNKNOWN_ERROR.getValue(),
                            pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
                    assertEquals(
                            PushTrackingStatus.UNKNOWN_ERROR.getDescription(),
                            pushTrackingStatusEvent
                                    .getEventData()
                                    .get("pushTrackingStatusMessage"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenMessageIdNull() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent("pushOpened", null, "actionId", true, null);
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify push tracking status event sent
                    verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
                    assertEquals(
                            PushTrackingStatus.INVALID_MESSAGE_ID.getValue(),
                            pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
                    assertEquals(
                            PushTrackingStatus.INVALID_MESSAGE_ID.getDescription(),
                            pushTrackingStatusEvent
                                    .getEventData()
                                    .get("pushTrackingStatusMessage"));
                });
    }

    @Test
    public void test_processEvent_messageTrackingEvent_whenExperienceEventDatasetIdIsEmpty() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    when(mockConfigData.getValue())
                            .thenReturn(
                                    new HashMap<String, Object>() {
                                        {
                                            put("messaging.eventDataset", "");
                                        }
                                    });
                    mockConfigSharedState();
                    final Event event =
                            samplePushTrackingEvent(
                                    "pushOpened", "messageId", "actionId", true, null);
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

                    // test
                    messagingExtension.processEvent(event);

                    // verify push tracking status event sent
                    verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
                    final Event pushTrackingStatusEvent = eventCaptor.getAllValues().get(0);
                    assertEquals("Push tracking status event", pushTrackingStatusEvent.getName());
                    assertEquals(
                            PushTrackingStatus.NO_DATASET_CONFIGURED.getValue(),
                            pushTrackingStatusEvent.getEventData().get("pushTrackingStatus"));
                    assertEquals(
                            PushTrackingStatus.NO_DATASET_CONFIGURED.getDescription(),
                            pushTrackingStatusEvent
                                    .getEventData()
                                    .get("pushTrackingStatusMessage"));
                });
    }

    // ========================================================================================
    // processEvents refreshMessagesEvent
    // ========================================================================================

    @Test
    public void test_processEvent_fetchMessagesEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("refreshmessages", true);
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getEventData()).thenReturn(eventData);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource())
                            .thenReturn(MessagingTestConstants.EventSource.REQUEST_CONTENT);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .fetchPropositions(mockEvent, null);
                });
    }

    // ========================================================================================
    // processEvent edgePersonalizationEvent
    // ========================================================================================

    @Test
    public void test_processEvent_edgePersonalizationEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("proposition_data", "mock_proposition_data");
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getEventData()).thenReturn(eventData);
                    when(mockEvent.getType()).thenReturn(MessagingTestConstants.EventType.EDGE);
                    when(mockEvent.getSource())
                            .thenReturn(
                                    MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .handleEdgePersonalizationNotification(any(Event.class));
                });
    }

    // ========================================================================================
    // processEvent updatePropositionsEvent
    // ========================================================================================

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
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("updatepropositions", true);
                    eventData.put("surfaces", surfaces);
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getEventData()).thenReturn(eventData);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource())
                            .thenReturn(MessagingTestConstants.EventSource.REQUEST_CONTENT);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .fetchPropositions(any(Event.class), listArgumentCaptor.capture());
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
    // processEvent getPropositionsEvent
    // ========================================================================================

    @Test
    public void test_processEvent_getPropositionsEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    messagingExtension.setSerialWorkDispatcher(mockSerialWorkDispatcher);
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("getpropositions", true);
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getEventData()).thenReturn(eventData);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource())
                            .thenReturn(MessagingTestConstants.EventSource.REQUEST_CONTENT);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockSerialWorkDispatcher, times(1)).offer(mockEvent);
                });
    }

    // ========================================================================================
    // processEvent TrackingPropositionsEvent
    // ========================================================================================
    @Test
    public void test_processEvent_trackingPropositionsEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    final Map<String, Object> interactionData = new HashMap<>();
                    interactionData.put("someKey", "someValue");
                    final Map<String, Object> eventData = new HashMap<>();
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.Messaging.TRACK_PROPOSITIONS,
                            true);
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.Messaging.PROPOSITION_INTERACTION,
                            interactionData);
                    final Event trackingEvent =
                            new Event.Builder(
                                            "Track propositions event",
                                            EventType.MESSAGING,
                                            EventSource.REQUEST_CONTENT)
                                    .setEventData(eventData)
                                    .build();

                    // test
                    messagingExtension.processEvent(trackingEvent);

                    // verify dispatch event is called
                    // 1 event dispatched: edge event with in app interact event tracking info
                    final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
                    verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

                    // verify event
                    Event event = eventCaptor.getValue();
                    assertNotNull(event.getEventData());
                    assertEquals(
                            MessagingTestConstants.EventName.MESSAGE_INTERACTION_EVENT,
                            event.getName());
                    assertEquals(MessagingTestConstants.EventType.EDGE, event.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, event.getSource());
                    assertEquals(
                            interactionData,
                            event.getEventData().get(MessagingTestConstants.TrackingKeys.XDM));
                });
    }

    @Test
    public void test_processEvent_trackingPropositionsEvent_nullPropositionInteraction() {
        runUsingMockedServiceProvider(
                () -> {
                    final Map<String, Object> eventData = new HashMap<>();
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.Messaging.TRACK_PROPOSITIONS,
                            true);
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.Messaging.PROPOSITION_INTERACTION,
                            null);
                    final Event trackingEvent =
                            new Event.Builder(
                                            "Track propositions event",
                                            EventType.MESSAGING,
                                            EventSource.REQUEST_CONTENT)
                                    .setEventData(eventData)
                                    .build();

                    // test
                    messagingExtension.processEvent(trackingEvent);

                    // verify dispatch event is called
                    // 1 event dispatched: edge event with in app interact event tracking info
                    verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                });
    }

    @Test
    public void test_processEvent_trackingPropositionsEvent_emptyPropositionInteraction() {
        runUsingMockedServiceProvider(
                () -> {
                    final Map<String, Object> eventData = new HashMap<>();
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.Messaging.TRACK_PROPOSITIONS,
                            true);
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.Messaging.PROPOSITION_INTERACTION,
                            new HashMap<>());
                    final Event trackingEvent =
                            new Event.Builder(
                                            "Track propositions event",
                                            EventType.MESSAGING,
                                            EventSource.REQUEST_CONTENT)
                                    .setEventData(eventData)
                                    .build();

                    // test
                    messagingExtension.processEvent(trackingEvent);

                    // verify dispatch event is called
                    // 1 event dispatched: edge event with in app interact event tracking info
                    verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                });
    }

    // ========================================================================================
    // processEvents edgePersonalizationRequestCompleteEvent
    // ========================================================================================
    @Test
    public void test_processEvent_edgePersonalizationRequestCompleteEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource()).thenReturn(EventSource.CONTENT_COMPLETE);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .handleProcessCompletedEvent(mockEvent);
                });
    }

    // ========================================================================================
    // processEvents EventHistoryDisqualifyEvent
    // ========================================================================================
    @Test
    public void test_processEvent_eventHistoryDisqualifyEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    Map<String, String> eventHistoryMap = new HashMap<>();
                    eventHistoryMap.put(
                            MessagingTestConstants.EventMask.Keys.EVENT_TYPE,
                            MessagingEdgeEventType.DISQUALIFY.getPropositionEventType());
                    eventHistoryMap.put(
                            MessagingTestConstants.EventMask.Keys.MESSAGE_ID, "mockActivityId");
                    eventHistoryMap.put(MessagingTestConstants.EventMask.Keys.TRACKING_ACTION, "");
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.IAM_HISTORY, eventHistoryMap);

                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource())
                            .thenReturn(MessagingTestConstants.EventSource.EVENT_HISTORY_WRITE);
                    when(mockEvent.getEventData()).thenReturn(eventData);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(1))
                            .handleEventHistoryDisqualifyEvent(mockEvent);
                });
    }

    @Test
    public void test_processEvent_eventHistoryDisplayEvent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> eventData = new HashMap<>();
                    Map<String, String> eventHistoryMap = new HashMap<>();
                    eventHistoryMap.put(
                            MessagingTestConstants.EventMask.Keys.EVENT_TYPE,
                            MessagingEdgeEventType.DISPLAY.getPropositionEventType());
                    eventHistoryMap.put(
                            MessagingTestConstants.EventMask.Keys.MESSAGE_ID, "mockActivityId");
                    eventHistoryMap.put(MessagingTestConstants.EventMask.Keys.TRACKING_ACTION, "");
                    eventData.put(
                            MessagingTestConstants.EventDataKeys.IAM_HISTORY, eventHistoryMap);

                    Event mockEvent = mock(Event.class);
                    when(mockEvent.getType())
                            .thenReturn(MessagingTestConstants.EventType.MESSAGING);
                    when(mockEvent.getSource())
                            .thenReturn(MessagingTestConstants.EventSource.EVENT_HISTORY_WRITE);
                    when(mockEvent.getEventData()).thenReturn(eventData);

                    // test
                    messagingExtension.processEvent(mockEvent);

                    // verify
                    verify(mockEdgePersonalizationResponseHandler, times(0))
                            .handleEventHistoryDisqualifyEvent(mockEvent);
                });
    }

    // ========================================================================================
    // completion handler tests
    // ========================================================================================
    @Test
    public void test_completionHandlerForOriginatingEventId_found() {
        runUsingMockedServiceProvider(
                () -> {
                    CompletionHandler result =
                            messagingExtension.completionHandlerForOriginatingEventId(
                                    "originatingId");
                    assertNotNull(result);
                    assertEquals("originatingId", result.originatingEventId);
                });
    }

    @Test
    public void test_completionHandlerForOriginatingEventId_notFound() {
        runUsingMockedServiceProvider(
                () -> {
                    CompletionHandler result =
                            messagingExtension.completionHandlerForOriginatingEventId(
                                    "nonExistentId");
                    assertNull(result);
                });
    }

    @Test
    public void test_completionHandlerForEdgeRequestEventId_found() {
        runUsingMockedServiceProvider(
                () -> {
                    CompletionHandler result =
                            messagingExtension.completionHandlerForEdgeRequestEventId(
                                    "edgeRequestId");
                    assertNotNull(result);
                    assertEquals("edgeRequestId", result.edgeRequestEventId);
                });
    }

    @Test
    public void test_completionHandlerForEdgeRequestEventId_notFound() {
        runUsingMockedServiceProvider(
                () -> {
                    CompletionHandler result =
                            messagingExtension.completionHandlerForEdgeRequestEventId(
                                    "nonExistentId");
                    assertNull(result);
                });
    }

    // ========================================================================================
    // test handlePushToken
    // ========================================================================================
    @Test
    public void test_handlePushToken_whenPushTokenIsNull() {
        runUsingMockedServiceProvider(
                () -> {
                    Event event =
                            new Event.Builder("event", "type", "source")
                                    .setEventData(new HashMap<>())
                                    .build();

                    messagingExtension.handlePushToken(event);

                    verify(mockExtensionApi, never()).dispatch(any(Event.class));
                });
    }

    @Test
    public void test_handlePushToken_whenPushTokenIsEmpty() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("pushidentifier", "");
                    Event event =
                            new Event.Builder("event", "type", "source")
                                    .setEventData(eventData)
                                    .build();

                    messagingExtension.handlePushToken(event);

                    verify(mockExtensionApi, never()).dispatch(any(Event.class));
                });
    }

    @Test
    public void test_handlePushToken_whenPushTokenIsValid() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("pushidentifier", "validToken");
                    Event event =
                            new Event.Builder("event", "type", "source")
                                    .setEventData(eventData)
                                    .build();

                    Map<String, Object> edgeIdentitySharedState = new HashMap<>();
                    Map<String, Object> ecidMap = new HashMap<>();
                    Map<String, Object> identityMap = new HashMap<>();
                    List<Map<String, Object>> ecids = new ArrayList<>();
                    ecidMap.put("id", "mock_ecid");
                    ecids.add(ecidMap);
                    identityMap.put("ECID", ecids);
                    edgeIdentitySharedState.put("identityMap", identityMap);
                    when(mockExtensionApi.getXDMSharedState(
                                    eq("com.adobe.edge.identity"),
                                    any(Event.class),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.SET, edgeIdentitySharedState));

                    messagingExtension.handlePushToken(event);

                    // verify push token is persisted in the named collection
                    verify(mockNamedCollection, times(1))
                            .setString(
                                    eq(
                                            MessagingConstants.NamedCollectionKeys.Messaging
                                                    .PUSH_IDENTIFIER),
                                    anyString());

                    // verify push token added to shared state
                    verify(mockExtensionApi, times(1))
                            .createSharedState(any(Map.class), any(Event.class));

                    // event dispatched as the registration is paused
                    verify(mockExtensionApi, times(1)).dispatch(any());
                });
    }

    @Test
    public void test_handlePushToken_whenPushTokenIsTheSame_forceSyncIsFalse() {
        runUsingMockedServiceProvider(
                () -> {
                    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("pushidentifier", "validToken");
                    Event event =
                            new Event.Builder("event", "type", "source")
                                    .setEventData(eventData)
                                    .build();

                    when(mockNamedCollection.getString(anyString(), any()))
                            .thenReturn("validToken");

                    Map<String, Object> configSharedState = new HashMap<>();
                    configSharedState.put(
                            MessagingConstants.SharedState.Configuration.PUSH_FORCE_SYNC, false);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(Event.class),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.SET, configSharedState));

                    Map<String, Object> edgeIdentitySharedState = new HashMap<>();
                    Map<String, Object> ecidMap = new HashMap<>();
                    Map<String, Object> identityMap = new HashMap<>();
                    List<Map<String, Object>> ecids = new ArrayList<>();
                    ecidMap.put("id", "mock_ecid");
                    ecids.add(ecidMap);
                    identityMap.put("ECID", ecids);
                    edgeIdentitySharedState.put("identityMap", identityMap);
                    when(mockExtensionApi.getXDMSharedState(
                                    eq("com.adobe.edge.identity"),
                                    any(Event.class),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.SET, edgeIdentitySharedState));

                    messagingExtension.handlePushToken(event);

                    // verify push token not persisted in the named collection
                    verify(mockNamedCollection, times(0))
                            .setString(
                                    eq(
                                            MessagingConstants.NamedCollectionKeys.Messaging
                                                    .PUSH_IDENTIFIER),
                                    anyString());

                    // verify push token not added to shared state
                    verify(mockExtensionApi, times(0))
                            .createSharedState(any(Map.class), any(Event.class));

                    // no event dispatched as the push token is the same
                    verify(mockExtensionApi, times(0)).dispatch(eventCaptor.capture());
                });
    }

    @Test
    public void test_handlePushToken_whenPushTokenIsTheSame_forceSyncIsTrue() {
        runUsingMockedServiceProvider(
                () -> {
                    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("pushidentifier", "validToken");
                    Event event =
                            new Event.Builder("event", "type", "source")
                                    .setEventData(eventData)
                                    .build();

                    Map<String, Object> configSharedState = new HashMap<>();
                    configSharedState.put(
                            MessagingConstants.SharedState.Configuration.PUSH_FORCE_SYNC, true);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(Event.class),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.SET, configSharedState));

                    Map<String, Object> edgeIdentitySharedState = new HashMap<>();
                    Map<String, Object> ecidMap = new HashMap<>();
                    Map<String, Object> identityMap = new HashMap<>();
                    List<Map<String, Object>> ecids = new ArrayList<>();
                    ecidMap.put("id", "mock_ecid");
                    ecids.add(ecidMap);
                    identityMap.put("ECID", ecids);
                    edgeIdentitySharedState.put("identityMap", identityMap);
                    when(mockExtensionApi.getXDMSharedState(
                                    eq("com.adobe.edge.identity"),
                                    any(Event.class),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(
                                    new SharedStateResult(
                                            SharedStateStatus.SET, edgeIdentitySharedState));

                    messagingExtension.handlePushToken(event);

                    // verify push token persisted in the named collection
                    verify(mockNamedCollection, times(1))
                            .setString(
                                    eq(
                                            MessagingConstants.NamedCollectionKeys.Messaging
                                                    .PUSH_IDENTIFIER),
                                    eq("validToken"));

                    // verify push token added to shared state
                    verify(mockExtensionApi, times(1))
                            .createSharedState(
                                    argThat(
                                            map -> {
                                                String pushIdentifier =
                                                        (String) map.get("pushidentifier");
                                                return "validToken".equals(pushIdentifier);
                                            }),
                                    any(Event.class));

                    // event dispatched with push token
                    verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

                    assertEquals(
                            MessagingConstants.EventSource.REQUEST_CONTENT,
                            eventCaptor.getValue().getSource());
                    assertEquals(
                            MessagingConstants.EventType.EDGE, eventCaptor.getValue().getType());
                    assertEquals(
                            MessagingConstants.EventName.PUSH_PROFILE_EDGE_EVENT,
                            eventCaptor.getValue().getName());
                    Map<String, Object> eventDataMap = eventCaptor.getValue().getEventData();
                    List<Map<String, Object>> pushNotificationDetails =
                            (List<Map<String, Object>>)
                                    ((Map<String, Object>) eventDataMap.get("data"))
                                            .get("pushNotificationDetails");
                    assertEquals("validToken", pushNotificationDetails.get(0).get("token"));
                });
    }

    // ========================================================================================
    // private helpers
    // ========================================================================================
    private Event samplePushTrackingEvent(
            final String eventType,
            final String messageId,
            final String actionId,
            final boolean applicationOpened,
            final String adobeXdm) {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(TRACK_INFO_KEY_EVENT_TYPE, eventType);
        eventData.put(TRACK_INFO_KEY_MESSAGE_ID, messageId);
        eventData.put(TRACK_INFO_KEY_ACTION_ID, actionId);
        eventData.put(TRACK_INFO_KEY_APPLICATION_OPENED, applicationOpened);
        eventData.put(TRACK_INFO_KEY_ADOBE_XDM, adobeXdm);

        final Event event =
                new Event.Builder(
                                "mock_event_name",
                                MessagingTestConstants.EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();
        return event;
    }

    private void mockConfigSharedState() {
        when(mockExtensionApi.getSharedState(
                        eq(MessagingTestConstants.SharedState.Configuration.EXTENSION_NAME),
                        any(Event.class),
                        eq(false),
                        eq(SharedStateResolution.LAST_SET)))
                .thenReturn(mockConfigData);
    }
}
