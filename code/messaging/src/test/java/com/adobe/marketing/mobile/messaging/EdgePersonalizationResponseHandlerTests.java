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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.ENDING_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.RESPONSE_ERROR;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.FINALIZE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Networking;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.services.caching.CacheService;
import com.adobe.marketing.mobile.services.internal.caching.FileCacheService;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class EdgePersonalizationResponseHandlerTests {

    private final ArgumentCaptor<List<LaunchRule>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackWithErrorArgumentCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);
    private final ArgumentCaptor<List<LaunchRule>> inAppRulesListCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<List<LaunchRule>> feedRulesListCaptor = ArgumentCaptor.forClass(List.class);

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    Event mockEvent;
    @Mock
    Event mockResponseEvent;
    @Mock
    AdobeError mockAdobeError;
    @Mock
    Application mockApplication;
    @Mock
    Context mockContext;
    @Mock
    ServiceProvider mockServiceProvider;
    @Mock
    DeviceInforming mockDeviceInfoService;
    @Mock
    Networking mockNetworkService;
    @Mock
    CacheService mockCacheService;
    @Mock
    CacheResult mockCacheResult;
    @Mock
    MessagingExtension mockMessagingExtension;
    @Mock
    LaunchRulesEngine mockMessagingRulesEngine;
    @Mock
    FeedRulesEngine mockFeedRulesEngine;
    @Mock
    MessagingCacheUtilities mockMessagingCacheUtilities;
    @Mock
    SerialWorkDispatcher<Event> mockSerialWorkDispatcher;

    private File cacheDir;
    private EdgePersonalizationResponseHandler edgePersonalizationResponseHandler;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        cacheDir = new File("cache");
        cacheDir.mkdirs();
        cacheDir.setWritable(true);
    }

    @After
    public void tearDown() {
        reset(mockExtensionApi);
        reset(mockEvent);
        reset(mockResponseEvent);
        reset(mockAdobeError);
        reset(mockApplication);
        reset(mockContext);
        reset(mockServiceProvider);
        reset(mockDeviceInfoService);
        reset(mockNetworkService);
        reset(mockCacheService);
        reset(mockCacheResult);
        reset(mockMessagingExtension);
        reset(mockMessagingCacheUtilities);
        reset(mockMessagingRulesEngine);
        reset(mockFeedRulesEngine);
        reset(mockSerialWorkDispatcher);

        if (cacheDir.exists()) {
            cacheDir.delete();
        }
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class);
             MockedStatic<MobileCore> mobileCoreStatic = Mockito.mockStatic(MobileCore.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            mobileCoreStatic.when(() -> MobileCore.dispatchEventWithResponseCallback(eventArgumentCaptor.capture(), anyLong(), adobeCallbackWithErrorArgumentCaptor.capture())).thenCallRealMethod();
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);

            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");

            edgePersonalizationResponseHandler = new EdgePersonalizationResponseHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockFeedRulesEngine, mockMessagingCacheUtilities);
            edgePersonalizationResponseHandler.setMessagesRequestEventId("TESTING_ID");
            edgePersonalizationResponseHandler.setSerialWorkDispatcher(mockSerialWorkDispatcher);

            when(mockEvent.getUniqueIdentifier()).thenReturn("mockParentId");
            when(mockResponseEvent.getParentID()).thenReturn("mockParentResponseId");
            when(mockResponseEvent.getName()).thenReturn("fetch message response");
            when(mockResponseEvent.getType()).thenReturn(EventType.EDGE);
            when(mockResponseEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

            runnable.run();
        }
    }

    // ========================================================================================
    // fetchMessages
    // ========================================================================================
    @Test
    public void test_fetchMessages_ValidApplicationPackageNamePresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"}, \"request\":{\"sendCompletion\":true}, \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}}, \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"], \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\", \"https://ns.adobe.com/personalization/json-content-item\", \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, null);

            // verify edge request event dispatched
            Event edgeRequestEvent = eventArgumentCaptor.getValue();
            assertEquals(EventType.EDGE, edgeRequestEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
            assertEquals(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT, edgeRequestEvent.getName());
            assertEquals(expectedEventData, edgeRequestEvent.getEventData());

            // answer adobe callback with a response event
            adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

            // verify finalize proposition event dispatched
            verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

            Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
            assertEquals(FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
            assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
            assertEquals(EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
            Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
            assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
        });
    }

    @Test
    public void test_fetchMessages_ValidApplicationPackageNamePresent_AdobeErrorReceived() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"}, \"request\":{\"sendCompletion\":true}, \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}}, \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"], \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\", \"https://ns.adobe.com/personalization/json-content-item\", \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, null);

            // verify edge request event dispatched
            Event edgeRequestEvent = eventArgumentCaptor.getValue();
            assertEquals(EventType.EDGE, edgeRequestEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
            assertEquals(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT, edgeRequestEvent.getName());
            assertEquals(expectedEventData, edgeRequestEvent.getEventData());

            // answer adobe callback with an adobe error
            adobeCallbackWithErrorArgumentCaptor.getValue().fail(mockAdobeError);

            // verify finalize proposition event not dispatched
            verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());

            // verify requested surfaces does not contain fetch messages event id as it is removed when an adobe error is returned
            String fetchMessagesEventId = eventArgumentCaptor.getValue().getUniqueIdentifier();
            Map<String, List<Surface>> requestedSurfaces = edgePersonalizationResponseHandler.getRequestedSurfacesForEventId();
            assertNull(requestedSurfaces.get(fetchMessagesEventId));
        });
    }

    @Test
    public void test_fetchMessages_InvalidApplicationPackageNamePresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("");

            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, null);

            // verify edge request event not dispatched
            assertEquals(0, eventArgumentCaptor.getAllValues().size());

            // verify finalize proposition event not dispatched
            verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());
        });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided() {
        runUsingMockedServiceProvider(() -> {
            // setup
            List<Surface> surfacePaths = new ArrayList<>();
            surfacePaths.add(new Surface("promos/feed1"));
            surfacePaths.add(new Surface("promos/feed2"));
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"}, \"request\":{\"sendCompletion\":true}, \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}}, \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName/promos/feed1\", \"mobileapp://mockPackageName/promos/feed2\"], \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\", \"https://ns.adobe.com/personalization/json-content-item\", \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, surfacePaths);

            // verify edge request event dispatched
            Event edgeRequestEvent = eventArgumentCaptor.getValue();
            assertEquals(EventType.EDGE, edgeRequestEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
            assertEquals(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT, edgeRequestEvent.getName());
            assertEquals(expectedEventData, edgeRequestEvent.getEventData());

            // answer adobe callback with a response event
            adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

            // verify finalize proposition event dispatched
            verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

            Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
            assertEquals(FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
            assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
            assertEquals(EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
            Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
            assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
        });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided_InvalidPathsDropped() {
        runUsingMockedServiceProvider(() -> {
            // setup
            List<Surface> surfacePaths = new ArrayList<>();
            surfacePaths.add(new Surface("promos/feed1"));
            surfacePaths.add(new Surface("##invalid"));
            surfacePaths.add(new Surface("alsoinvalid##"));
            surfacePaths.add(new Surface("promos/feed2"));
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"}, \"request\":{\"sendCompletion\":true}, \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}}, \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName/promos/feed1\", \"mobileapp://mockPackageName/promos/feed2\"], \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\", \"https://ns.adobe.com/personalization/json-content-item\", \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, surfacePaths);

            // verify edge request event dispatched
            Event edgeRequestEvent = eventArgumentCaptor.getValue();
            assertEquals(EventType.EDGE, edgeRequestEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
            assertEquals(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT, edgeRequestEvent.getName());
            assertEquals(expectedEventData, edgeRequestEvent.getEventData());


            // answer adobe callback with a response event
            adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

            // verify finalize proposition event dispatched
            verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

            Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
            assertEquals(FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
            assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
            assertEquals(EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
            Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
            assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
        });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided_InvalidPathsOnly() {
        runUsingMockedServiceProvider(() -> {
            // setup
            List<Surface> surfacePaths = new ArrayList<>();
            surfacePaths.add(new Surface("##invalid"));
            surfacePaths.add(new Surface("alsoinvalid##"));

            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, surfacePaths);

            // verify edge request event not dispatched
            assertEquals(0, eventArgumentCaptor.getAllValues().size());

            // verify finalize proposition event not dispatched
            verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());
        });
    }

    @Test
    public void test_fetchMessages_EmptySurfacePathsProvided() {
        runUsingMockedServiceProvider(() -> {
            // setup
            List<Surface> surfacePaths = new ArrayList<>();
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"}, \"request\":{\"sendCompletion\":true}, \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}}, \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"], \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\", \"https://ns.adobe.com/personalization/json-content-item\", \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            edgePersonalizationResponseHandler.fetchMessages(mockEvent, surfacePaths);

            // verify edge request event dispatched
            Event edgeRequestEvent = eventArgumentCaptor.getValue();
            assertEquals(EventType.EDGE, edgeRequestEvent.getType());
            assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
            assertEquals(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT, edgeRequestEvent.getName());
            assertEquals(expectedEventData, edgeRequestEvent.getEventData());


            // answer adobe callback with a response event
            adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

            // verify finalize proposition event dispatched
            verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

            Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
            assertEquals(FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
            assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
            assertEquals(EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
            Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
            assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
        });
    }

    // ========================================================================================
    // handleEdgePersonalizationNotification
    // ========================================================================================
    @Test
    public void test_handleEdgePersonalizationNotification_ValidPayloadPresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                MessageTestConfig config = new MessageTestConfig();
                config.count = 1;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map updated
                Map<Surface, List<Proposition>> inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(1, inProgressPropositions.size());
                Surface surface = inProgressPropositions.keySet().stream().findFirst().get();
                assertEquals("mobileapp://mockPackageName", surface.getUri());
                List<Proposition> propositions = inProgressPropositions.get(surface);
                assertEquals(1, propositions.size());
                Proposition proposition = propositions.get(0);
                assertNotNull(proposition);
                assertEquals(1, proposition.getItems().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_MultiplePersonalizationRequestHandlesReceived_Then_AllValidPropositionsAddedToInProgressPropositions() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                MessageTestConfig config = new MessageTestConfig();
                config.count = 3;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map updated
                Map<Surface, List<Proposition>> inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(1, inProgressPropositions.size());
                Surface surface = inProgressPropositions.keySet().stream().findFirst().get();
                assertEquals("mobileapp://mockPackageName", surface.getUri());
                List<Proposition> propositions = inProgressPropositions.get(surface);
                assertEquals(1, propositions.size());
                Proposition proposition = propositions.get(0);
                assertNotNull(proposition);
                assertEquals(3, proposition.getItems().size());

                // mock a second personalization event containing the same requestId
                config.count = 4;
                payload = MessagingTestUtils.generateMessagePayload(config);
                eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map updated with propositions from second personalization event
                inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(1, inProgressPropositions.size());
                surface = inProgressPropositions.keySet().stream().findFirst().get();
                assertEquals("mobileapp://mockPackageName", surface.getUri());
                propositions = inProgressPropositions.get(surface);
                assertEquals(2, propositions.size());
                proposition = propositions.get(1);
                assertNotNull(proposition);
                assertEquals(4, proposition.getItems().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_NullPayload() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", null);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map not updated
                Map<Surface, List<Proposition>> inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(0, inProgressPropositions.size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_EmptyPayload() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", Collections.emptyList());
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map not updated
                Map<Surface, List<Proposition>> inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(0, inProgressPropositions.size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_NonMatchingRequestEventId() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", Collections.emptyList());
                eventData.put("requestEventId", "NON_MATCHING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map not updated
                Map<Surface, List<Proposition>> inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(0, inProgressPropositions.size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_EmptyRequestEventId() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                // need to add an empty request event id for testing purposes
                edgePersonalizationResponseHandler.setMessagesRequestEventId("");
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", Collections.emptyList());
                eventData.put("requestEventId", "");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify in progress propositions map not updated
                Map<Surface, List<Proposition>> inProgressPropositions = edgePersonalizationResponseHandler.getInProgressPropositions();
                assertEquals(0, inProgressPropositions.size());
            }
        });
    }

    // ========================================================================================
    // edgePersonalizationResponseHandler handleProcessCompletedEvent
    // ========================================================================================
    @Test
    public void test_handleProcessCompletedEvent_InAppAndFeedRulesCompleted() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                Surface surface = new Surface();
                Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                matchedFeedRules.put(surface, MessagingTestUtils.createMessagingPropositionItemList(4));
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenCallRealMethod();
                when(mockFeedRulesEngine.evaluate(any(Event.class))).thenReturn(matchedFeedRules);

                // setup in progress in-app propositions
                List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>() {{
                    for (int i = 0; i < 3; i++) {
                        add(MessagingTestUtils.getMapFromFile("unitTestInAppPayload.json"));
                    }
                }};

                // setup in progress feed propositions and add them to the payload
                MessageTestConfig config = new MessageTestConfig();
                config.count = 4;
                payload.addAll(MessagingTestUtils.generateFeedPayload(config));

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // setup processing completed event
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                // verify parsed rules replaced in in-app rules engine
                verify(mockMessagingRulesEngine, times(1)).replaceRules(inAppRulesListCaptor.capture());
                assertEquals(3, inAppRulesListCaptor.getValue().size());

                // verify parsed rules replaced in feed rules engine
                verify(mockFeedRulesEngine, times(1)).replaceRules(feedRulesListCaptor.capture());
                assertEquals(4, feedRulesListCaptor.getValue().size());

                // verify received propositions event not dispatched
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
            }
        });
    }

    @Test
    public void test_handleProcessCompletedEvent_EmptyPayload() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                // setup an empty payload
                List<Map<String, Object>> payload = new ArrayList<>();

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // setup processing completed event
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                // verify rules not replaced in in-app rules engine
                verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());

                // verify rules not replaced in feed rules engine
                verify(mockFeedRulesEngine, times(0)).replaceRules(anyList());

                // verify received propositions event not dispatched
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
            }
        });
    }

    @Test
    public void test_handleProcessCompletedEvent_ProcessCompletedEventMissingRequestId() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                Surface surface = new Surface();
                Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                matchedFeedRules.put(surface, MessagingTestUtils.createMessagingPropositionItemList(4));
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenCallRealMethod();
                when(mockFeedRulesEngine.evaluate(any(Event.class))).thenReturn(matchedFeedRules);

                // setup in progress in-app propositions
                MessageTestConfig config = new MessageTestConfig();
                config.count = 3;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);

                // setup in progress feed propositions and add them to the payload
                config = new MessageTestConfig();
                config.count = 4;
                payload.addAll(MessagingTestUtils.generateFeedPayload(config));

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");

                // setup processing completed event missing request event id
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, null);
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                // verify rules not replaced in in-app rules engine
                verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());

                // verify rules not replaced in feed rules engine
                verify(mockFeedRulesEngine, times(0)).replaceRules(anyList());

                // verify received propositions event not dispatched
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
            }
        });
    }

    @Test
    public void test_handleProcessCompletedEvent_NoValidRulesInPayload() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                // setup in progress invalid in-app propositions
                MessageTestConfig config = new MessageTestConfig();
                config.isMissingRulesKey = true;
                config.count = 1;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);

                // setup in progress invalid feed propositions and add them to the payload
                config = new MessageTestConfig();
                config.isMissingRulesKey = true;
                config.count = 1;
                payload.addAll(MessagingTestUtils.generateFeedPayload(config));

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // setup processing completed event
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                // verify rules not replaced in in-app rules engine
                verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());

                // verify rules not replaced in feed rules engine
                verify(mockFeedRulesEngine, times(0)).replaceRules(anyList());

                // verify received propositions event not dispatched
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
            }
        });
    }

    // ========================================================================================
    // edgePersonalizationResponseHandler retrieveMessages
    // ========================================================================================
    @Test
    public void test_retrieveMessages() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                // setup valid surfaces
                Surface surface = new Surface();
                List<Surface> surfaces = new ArrayList<Surface>() {{
                    add(surface);
                }};

                // setup feed rules engine
                Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                matchedFeedRules.put(surface, MessagingTestUtils.createMessagingPropositionItemList(3));
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenCallRealMethod();
                when(mockFeedRulesEngine.evaluate(any(Event.class))).thenReturn(matchedFeedRules);

                // setup in progress feed propositions
                MessageTestConfig config = new MessageTestConfig();
                config.count = 4;
                List<Map<String, Object>> payload = MessagingTestUtils.generateFeedPayload(config);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // setup processing completed event
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);
                reset(mockExtensionApi);

                // test retrieveMessages
                edgePersonalizationResponseHandler.retrieveMessages(surfaces, mockEvent);

                // verify message propositions response event dispatched with 1 feed proposition
                verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                Event propositionsResponseEvent = eventArgumentCaptor.getValue();
                assertEquals(MESSAGE_PROPOSITIONS_RESPONSE, propositionsResponseEvent.getName());
                assertEquals(EventType.MESSAGING, propositionsResponseEvent.getType());
                assertEquals(EventSource.RESPONSE_CONTENT, propositionsResponseEvent.getSource());
                eventData = propositionsResponseEvent.getEventData();
                assertEquals("propositions", eventData.keySet().stream().findFirst().get());
                List<Map<String, Object>> propositions = DataReader.optTypedListOfMap(Object.class, eventData, "propositions", null);
                assertEquals(1, propositions.size());
            }
        });
    }

    @Test
    public void test_retrieveMessages_invalidSurfacesProvided() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                // setup invalid surfaces
                List<Surface> surfaces = new ArrayList<>();
                surfaces.add(new Surface("##invalid"));
                surfaces.add(new Surface("alsoinvalid##"));

                // setup feed rules engine
                Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                matchedFeedRules.put(new Surface(), MessagingTestUtils.createMessagingPropositionItemList(3));
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenCallRealMethod();
                when(mockFeedRulesEngine.evaluate(any(Event.class))).thenReturn(matchedFeedRules);

                // setup in progress feed propositions
                MessageTestConfig config = new MessageTestConfig();
                config.count = 4;
                List<Map<String, Object>> payload = MessagingTestUtils.generateFeedPayload(config);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // setup processing completed event
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);
                reset(mockExtensionApi);

                // test retrieveMessages
                edgePersonalizationResponseHandler.retrieveMessages(surfaces, mockEvent);

                // verify error response event dispatched
                verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                Event propositionsResponseEvent = eventArgumentCaptor.getValue();
                assertEquals(MESSAGE_PROPOSITIONS_RESPONSE, propositionsResponseEvent.getName());
                assertEquals(EventType.MESSAGING, propositionsResponseEvent.getType());
                assertEquals(EventSource.RESPONSE_CONTENT, propositionsResponseEvent.getSource());
                eventData = propositionsResponseEvent.getEventData();
                assertEquals(RESPONSE_ERROR, eventData.keySet().stream().findFirst().get());
                assertEquals(AdobeErrorExt.INVALID_REQUEST.getErrorName(), eventData.get(RESPONSE_ERROR));
            }
        });
    }

    @Test
    public void test_retrieveMessages_emptySurfacesProvided() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                // setup empty surfaces
                List<Surface> surfaces = new ArrayList<>();

                // setup feed rules engine
                Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                matchedFeedRules.put(new Surface(), MessagingTestUtils.createMessagingPropositionItemList(3));
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenCallRealMethod();
                when(mockFeedRulesEngine.evaluate(any(Event.class))).thenReturn(matchedFeedRules);

                // setup in progress feed propositions
                MessageTestConfig config = new MessageTestConfig();
                config.count = 4;
                List<Map<String, Object>> payload = MessagingTestUtils.generateFeedPayload(config);

                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(mockEvent);

                // setup processing completed event
                eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);
                edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);
                reset(mockExtensionApi);

                // test retrieveMessages
                edgePersonalizationResponseHandler.retrieveMessages(surfaces, mockEvent);

                // verify no response event dispatched
                verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
            }
        });
    }

    // ========================================================================================
    // edgePersonalizationResponseHandler load cached propositions on instantiation
    // ========================================================================================
    @Test
    public void test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction() {
        int inAppCount = 5;
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);

                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenCallRealMethod();

                CacheService cacheService = new FileCacheService();
                when(mockServiceProvider.getCacheService()).thenReturn(cacheService);
                Map<Surface, List<Proposition>> payload = new HashMap<>();
                try {
                    payload.put(new Surface(), InternalMessagingUtils.getPropositionsFromPayloads(new ArrayList<Map<String, Object>>() {{
                        for (int i = 0; i < inAppCount; i++) {
                            add(MessagingTestUtils.getMapFromFile("unitTestInAppPayload.json"));
                        }
                    }}));
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                when(mockMessagingCacheUtilities.getCachedPropositions()).thenReturn(payload);

                // test
                edgePersonalizationResponseHandler = new EdgePersonalizationResponseHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockFeedRulesEngine, mockMessagingCacheUtilities);

                // verify cached rules replaced in rules engine
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(5, listArgumentCaptor.getValue().size());
            }
        });
    }

    @Test
    public void test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction_whenPropositionsNotCached() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(false);
                ;

                // test
                edgePersonalizationResponseHandler = new EdgePersonalizationResponseHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockFeedRulesEngine, mockMessagingCacheUtilities);

                // verify cached rules not replaced in rules engine
                verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());
            }
        });
    }

    // ========================================================================================
    // createInAppMessage
    // ========================================================================================
    @Test
    public void test_createInAppMessage() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<InternalMessage> mockedConstruction = Mockito.mockConstruction(InternalMessage.class)) {
                Map<String, Object> details = new HashMap<>();
                Map<String, Object> data = new HashMap<>();
                Map<String, Object> mobileParameters = new HashMap<>();

                data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                data.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, data);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, MessagingConstants.SchemaValues.SCHEMA_IAM);
                RuleConsequence consequence = new RuleConsequence("123456789", MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

                // test
                edgePersonalizationResponseHandler.createInAppMessage(consequence);

                // verify MessagingFullscreenMessage.trigger() then MessagingFullscreenMessage.show() called
                InternalMessage mockInternalMessage = mockedConstruction.constructed().get(0);
                verify(mockInternalMessage, times(1)).trigger();
                verify(mockInternalMessage, times(1)).show(eq(true));
            }
        });
    }

    @Test
    public void test_createInAppMessage_EmptyConsequenceType() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<InternalMessage> mockedConstruction = Mockito.mockConstruction(InternalMessage.class)) {
                Map<String, Object> details = new HashMap<>();
                Map<String, Object> mobileParameters = new HashMap<>();

                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                RuleConsequence consequence = new RuleConsequence("123456789", "", details);

                // test
                edgePersonalizationResponseHandler.createInAppMessage(consequence);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_createInAppMessage_NullDetails() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<InternalMessage> mockedConstruction = Mockito.mockConstruction(InternalMessage.class)) {
                RuleConsequence consequence = new RuleConsequence("123456789", MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, null);

                // test
                edgePersonalizationResponseHandler.createInAppMessage(consequence);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_createInAppMessage_NotCjmIamPayload() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<InternalMessage> mockedConstruction = Mockito.mockConstruction(InternalMessage.class)) {
                Map<String, Object> details = new HashMap<>();
                Map<String, Object> mobileParameters = new HashMap<>();

                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                RuleConsequence consequence = new RuleConsequence("123456789", "notCjmIam", details);

                // test
                edgePersonalizationResponseHandler.createInAppMessage(consequence);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_createInAppMessage_NullRuleConsequence() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<InternalMessage> mockedConstruction = Mockito.mockConstruction(InternalMessage.class)) {
                // test
                edgePersonalizationResponseHandler.createInAppMessage(null);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }
}