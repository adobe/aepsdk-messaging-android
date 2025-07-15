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

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.ENDING_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.RESPONSE_ERROR;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventName.FINALIZE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.app.Application;
import android.content.Context;
import com.adobe.marketing.mobile.AdobeCallback;
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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EdgePersonalizationResponseHandlerTests {

    private final ArgumentCaptor<List<LaunchRule>> listArgumentCaptor =
            ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);
    private final ArgumentCaptor<AdobeCallbackWithError> adobeCallbackWithErrorArgumentCaptor =
            ArgumentCaptor.forClass(AdobeCallbackWithError.class);
    private final ArgumentCaptor<List<LaunchRule>> rulesListCaptor =
            ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<List<LaunchRule>> contentCardRulesListCaptor =
            ArgumentCaptor.forClass(List.class);
    private CompletionHandler completionHandler;

    // Mocks
    @Mock ExtensionApi mockExtensionApi;
    @Mock Event mockEvent;
    @Mock Event mockResponseEvent;
    @Mock AdobeError mockAdobeError;
    @Mock Application mockApplication;
    @Mock Context mockContext;
    @Mock ServiceProvider mockServiceProvider;
    @Mock DeviceInforming mockDeviceInfoService;
    @Mock Networking mockNetworkService;
    @Mock CacheService mockCacheService;
    @Mock CacheResult mockCacheResult;
    @Mock MessagingExtension mockMessagingExtension;
    @Mock LaunchRulesEngine mockMessagingRulesEngine;
    @Mock ContentCardRulesEngine mockContentCardRulesEngine;
    @Mock MessagingCacheUtilities mockMessagingCacheUtilities;
    @Mock SerialWorkDispatcher<Event> mockSerialWorkDispatcher;
    @Mock PresentableMessageMapper mockPresentableMessageMapper;
    @Mock PresentableMessageMapper.InternalMessage mockInternalMessage;
    @Mock AdobeCallback mockAdobeCallback;

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
        reset(mockContentCardRulesEngine);
        reset(mockSerialWorkDispatcher);
        reset(mockPresentableMessageMapper);
        reset(mockInternalMessage);
        reset(mockAdobeCallback);

        if (cacheDir.exists()) {
            cacheDir.delete();
        }
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                        Mockito.mockStatic(ServiceProvider.class);
                MockedStatic<MobileCore> mobileCoreStatic = Mockito.mockStatic(MobileCore.class);
                MockedConstruction<Event> ignored =
                        Mockito.mockConstruction(
                                Event.class,
                                withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS),
                                (mock, context) ->
                                        when(mock.getUniqueIdentifier())
                                                .thenReturn("mockEventId"))) {
            when(mockEvent.getUniqueIdentifier()).thenReturn("mockParentId");
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            mobileCoreStatic
                    .when(
                            () ->
                                    MobileCore.dispatchEventWithResponseCallback(
                                            eventArgumentCaptor.capture(),
                                            anyLong(),
                                            adobeCallbackWithErrorArgumentCaptor.capture()))
                    .thenCallRealMethod();
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);

            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");

            completionHandler = new CompletionHandler("mockParentId", mockAdobeCallback);
            when(mockMessagingExtension.completionHandlerForOriginatingEventId(anyString()))
                    .thenReturn(completionHandler);

            edgePersonalizationResponseHandler =
                    new EdgePersonalizationResponseHandler(
                            mockMessagingExtension,
                            mockExtensionApi,
                            mockMessagingRulesEngine,
                            mockContentCardRulesEngine,
                            mockMessagingCacheUtilities);
            edgePersonalizationResponseHandler.setMessagesRequestEventId(
                    "TESTING_ID", Collections.singletonList(new Surface()));
            edgePersonalizationResponseHandler.setSerialWorkDispatcher(mockSerialWorkDispatcher);

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
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"personalization.request\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }
                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);

                    // verify completion handler added for edge request event id
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForOriginatingEventId("mockParentId");

                    // verify edge request event dispatched
                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.EDGE, edgeRequestEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());
                    assertEquals(expectedEventData, edgeRequestEvent.getEventData());

                    // answer adobe callback with a response event
                    adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

                    // verify finalize proposition event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

                    Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
                    assertEquals(
                            FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
                    assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
                    assertEquals(
                            EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
                    Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
                    assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
                });
    }

    @Test
    public void
            test_fetchMessages_ValidApplicationPackageNamePresent_NullOriginatingEventCompletionHandler() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessagingExtension.completionHandlerForOriginatingEventId(anyString()))
                            .thenReturn(null);
                    edgePersonalizationResponseHandler =
                            new EdgePersonalizationResponseHandler(
                                    mockMessagingExtension,
                                    mockExtensionApi,
                                    mockMessagingRulesEngine,
                                    mockContentCardRulesEngine,
                                    mockMessagingCacheUtilities);
                    edgePersonalizationResponseHandler.setMessagesRequestEventId(
                            "TESTING_ID", Collections.singletonList(new Surface()));
                    edgePersonalizationResponseHandler.setSerialWorkDispatcher(
                            mockSerialWorkDispatcher);

                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"personalization.request\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }
                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);

                    // verify completion handler callback not invoked as the handler is null
                    verifyNoInteractions(mockAdobeCallback);

                    // verify edge request event dispatched
                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.EDGE, edgeRequestEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());
                    assertEquals(expectedEventData, edgeRequestEvent.getEventData());

                    // answer adobe callback with a response event
                    adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

                    // verify finalize proposition event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

                    Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
                    assertEquals(
                            FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
                    assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
                    assertEquals(
                            EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
                    Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
                    assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
                });
    }

    @Test
    public void test_fetchMessages_ValidApplicationPackageNamePresent_AdobeErrorReceived() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"personalization.request\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }
                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);

                    // verify completion handler added for edge request event id
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForOriginatingEventId("mockParentId");

                    // verify edge request event dispatched
                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.EDGE, edgeRequestEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());
                    assertEquals(expectedEventData, edgeRequestEvent.getEventData());

                    // answer adobe callback with an adobe error
                    adobeCallbackWithErrorArgumentCaptor.getValue().fail(mockAdobeError);

                    // verify finalize proposition event not dispatched
                    verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());

                    // verify requested surfaces does not contain fetch messages event id as it is
                    // removed when an adobe error is returned
                    String fetchMessagesEventId =
                            eventArgumentCaptor.getValue().getUniqueIdentifier();
                    Map<String, List<Surface>> requestedSurfaces =
                            edgePersonalizationResponseHandler.getRequestedSurfacesForEventId();
                    assertNull(requestedSurfaces.get(fetchMessagesEventId));
                });
    }

    @Test
    public void test_fetchMessages_InvalidApplicationPackageNamePresent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("");

                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);

                    // verify completion handler called with false
                    verify(mockAdobeCallback, times(1)).call(false);

                    // verify edge request event not dispatched
                    assertEquals(0, eventArgumentCaptor.getAllValues().size());

                    // verify finalize proposition event not dispatched
                    verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());
                });
    }

    @Test
    public void
            test_fetchMessages_InvalidApplicationPackageNamePresent_NullOriginatingEventCompletionHandler() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    when(mockMessagingExtension.completionHandlerForOriginatingEventId(anyString()))
                            .thenReturn(null);
                    when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("");

                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);

                    // verify completion handler callback not invoked as the handler is null
                    verifyNoInteractions(mockAdobeCallback);

                    // verify edge request event not dispatched
                    assertEquals(0, eventArgumentCaptor.getAllValues().size());

                    // verify finalize proposition event not dispatched
                    verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());
                });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    List<Surface> surfacePaths = new ArrayList<>();
                    surfacePaths.add(new Surface("promos/feed1"));
                    surfacePaths.add(new Surface("promos/feed2"));
                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"personalization.request\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName/promos/feed1\","
                                                    + " \"mobileapp://mockPackageName/promos/feed2\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, surfacePaths);

                    // verify completion handler added for edge request event id
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForOriginatingEventId("mockParentId");

                    // verify edge request event dispatched
                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.EDGE, edgeRequestEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());
                    assertEquals(expectedEventData, edgeRequestEvent.getEventData());

                    // answer adobe callback with a response event
                    adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

                    // verify finalize proposition event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

                    Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
                    assertEquals(
                            FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
                    assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
                    assertEquals(
                            EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
                    Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
                    assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
                });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided_InvalidPathsDropped() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    List<Surface> surfacePaths = new ArrayList<>();
                    surfacePaths.add(new Surface("promos/feed1"));
                    surfacePaths.add(new Surface("##invalid"));
                    surfacePaths.add(new Surface("alsoinvalid##"));
                    surfacePaths.add(new Surface("promos/feed2"));
                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"personalization.request\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName/promos/feed1\","
                                                    + " \"mobileapp://mockPackageName/promos/feed2\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }
                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, surfacePaths);

                    // verify completion handler added for edge request event id
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForOriginatingEventId("mockParentId");

                    // verify edge request event dispatched
                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.EDGE, edgeRequestEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());
                    assertEquals(expectedEventData, edgeRequestEvent.getEventData());

                    // answer adobe callback with a response event
                    adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

                    // verify finalize proposition event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

                    Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
                    assertEquals(
                            FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
                    assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
                    assertEquals(
                            EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
                    Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
                    assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
                });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided_InvalidPathsOnly() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    List<Surface> surfacePaths = new ArrayList<>();
                    surfacePaths.add(new Surface("##invalid"));
                    surfacePaths.add(new Surface("alsoinvalid##"));

                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, surfacePaths);

                    // verify completion handler called with false
                    verify(mockAdobeCallback, times(1)).call(false);

                    // verify edge request event not dispatched
                    assertEquals(0, eventArgumentCaptor.getAllValues().size());

                    // verify finalize proposition event not dispatched
                    verify(mockExtensionApi, times(0)).dispatch(eventArgumentCaptor.capture());
                });
    }

    @Test
    public void test_fetchMessages_EmptySurfacePathsProvided() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    List<Surface> surfacePaths = new ArrayList<>();
                    Map<String, Object> expectedEventData = null;
                    try {
                        expectedEventData =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"personalization.request\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mockPackageName\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }
                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, surfacePaths);

                    // verify completion handler added for edge request event id
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForOriginatingEventId("mockParentId");

                    // verify edge request event dispatched
                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(EventType.EDGE, edgeRequestEvent.getType());
                    assertEquals(EventSource.REQUEST_CONTENT, edgeRequestEvent.getSource());
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());
                    assertEquals(expectedEventData, edgeRequestEvent.getEventData());

                    // answer adobe callback with a response event
                    adobeCallbackWithErrorArgumentCaptor.getValue().call(mockResponseEvent);

                    // verify finalize proposition event dispatched
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());

                    Event finalizePersonalizationEvent = eventArgumentCaptor.getValue();
                    assertEquals(
                            FINALIZE_PROPOSITIONS_RESPONSE, finalizePersonalizationEvent.getName());
                    assertEquals(EventType.MESSAGING, finalizePersonalizationEvent.getType());
                    assertEquals(
                            EventSource.CONTENT_COMPLETE, finalizePersonalizationEvent.getSource());
                    Map<String, Object> eventData = finalizePersonalizationEvent.getEventData();
                    assertEquals("mockParentResponseId", eventData.get(ENDING_EVENT_ID));
                });
    }

    // ========================================================================================
    // handleEdgePersonalizationNotification
    // ========================================================================================
    @Test
    public void test_handleEdgePersonalizationNotification_ValidPayloadPresent() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 1;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(1, inProgressPropositions.size());
                        Surface surface =
                                inProgressPropositions.keySet().stream().findFirst().get();
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
    public void
            test_handleEdgePersonalizationNotification_MultiplePersonalizationRequestHandlesReceived_Then_AllValidPropositionsAddedToInProgressPropositions() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 3;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(1, inProgressPropositions.size());
                        Surface surface =
                                inProgressPropositions.keySet().stream().findFirst().get();
                        assertEquals("mobileapp://mockPackageName", surface.getUri());
                        List<Proposition> propositions = inProgressPropositions.get(surface);
                        assertEquals(3, propositions.size());

                        // mock a second personalization event containing the same requestId
                        config.count = 1;
                        payload = MessagingTestUtils.generateInAppPayload(config);
                        eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map updated with propositions from second
                        // personalization event
                        inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(1, inProgressPropositions.size());
                        surface = inProgressPropositions.keySet().stream().findFirst().get();
                        assertEquals("mobileapp://mockPackageName", surface.getUri());
                        propositions = inProgressPropositions.get(surface);
                        assertEquals(4, propositions.size());
                    }
                });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_NullPayload() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", null);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map not updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(0, inProgressPropositions.size());
                    }
                });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_EmptyPayload() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", Collections.emptyList());
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map not updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(0, inProgressPropositions.size());
                    }
                });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_InvalidPayload() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 1;
                        config.isMissingMessageId = true;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map not updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(0, inProgressPropositions.size());
                    }
                });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_NonMatchingRequestEventId() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", Collections.emptyList());
                        eventData.put("requestEventId", "NON_MATCHING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map not updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(0, inProgressPropositions.size());
                    }
                });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_EmptyRequestEventId() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // need to add an empty request event id for testing purposes
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "", Collections.singletonList(new Surface()));
                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", Collections.emptyList());
                        eventData.put("requestEventId", "");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // verify in progress propositions map not updated
                        Map<Surface, List<Proposition>> inProgressPropositions =
                                edgePersonalizationResponseHandler.getInProgressPropositions();
                        assertEquals(0, inProgressPropositions.size());
                    }
                });
    }

    // ========================================================================================
    // edgePersonalizationResponseHandler handleProcessCompletedEvent
    // ========================================================================================
    @Test
    public void test_handleProcessCompletedEvent_InAppAndFeedRulesCompleted() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        Surface inappSurface = new Surface();
                        Surface contentCardSurface = new Surface("apifeed");
                        Surface mockSurface = new Surface("mockSurface");
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // setup in progress in-app propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 3;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);

                        // setup in progress content card propositions and add them to the payload
                        config.count = 4;
                        payload.addAll(MessagingTestUtils.generateContentCardPayload(config));

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // add another dummy surface for the same event id
                        // to ensure it gets removed from cache when the response does not have it
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface);
                                        add(mockSurface);
                                    }
                                });

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // verify parsed rules replaced in rules engine for in-app and content card
                        // event history
                        verify(mockMessagingRulesEngine, times(1))
                                .replaceRules(rulesListCaptor.capture());
                        List<LaunchRule> inAppRules = new ArrayList<>();
                        List<LaunchRule> eventHistoryRules = new ArrayList<>();
                        for (LaunchRule launchRule : rulesListCaptor.getValue()) {
                            for (RuleConsequence ruleConsequence :
                                    launchRule.getConsequenceList()) {
                                String ruleType =
                                        (String)
                                                ruleConsequence
                                                        .getDetail()
                                                        .get(
                                                                MessagingTestConstants.EventDataKeys
                                                                        .RulesEngine
                                                                        .MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA);
                                if (ruleType != null) {
                                    if (ruleType.equals(SchemaType.INAPP.toString())) {
                                        inAppRules.add(launchRule);
                                    } else if (ruleType.equals(
                                            SchemaType.EVENT_HISTORY_OPERATION.toString())) {
                                        eventHistoryRules.add(launchRule);
                                    }
                                }
                            }
                        }
                        // verify one rule replaced in rules engine for each in-app
                        assertEquals(3, inAppRules.size());
                        // verify three event history rules replaced in rules engine for each
                        // content card
                        assertEquals(12, eventHistoryRules.size());

                        // verify in-app rules are in priority order
                        MessagingTestUtils.verifyInAppRulesOrdering(inAppRules);

                        // verify parsed rules replaced in feed rules engine
                        verify(mockContentCardRulesEngine, times(1))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(4, contentCardRulesListCaptor.getValue().size());

                        // verify in-app propositions are cached
                        ArgumentCaptor<Map<Surface, List<Proposition>>> cachedPropositionsCaptor =
                                ArgumentCaptor.forClass(Map.class);
                        ArgumentCaptor<List<Surface>> surfacesToRemoveCaptor =
                                ArgumentCaptor.forClass(List.class);
                        verify(mockMessagingCacheUtilities, times(1))
                                .cachePropositions(
                                        cachedPropositionsCaptor.capture(),
                                        surfacesToRemoveCaptor.capture());
                        Map<Surface, List<Proposition>> cachedPropositions =
                                cachedPropositionsCaptor.getValue();
                        assertEquals(1, cachedPropositions.size());
                        assertEquals(3, cachedPropositions.get(inappSurface).size());
                        List<Surface> surfacesToRemove = surfacesToRemoveCaptor.getValue();
                        assertEquals(1, surfacesToRemove.size());
                        assertEquals(mockSurface, surfacesToRemove.get(0));

                        // verify received propositions event not dispatched
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                    }
                });
    }

    @Test
    public void test_handleProcessCompletedEvent_IAMPropositionsNotReturnedInSubsequentResponse() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        Surface inappSurface = new Surface();
                        Surface contentCardSurface = new Surface("apifeed");

                        // setup in progress in-app propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 3;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);

                        // setup in progress feed propositions and add them to the payload
                        config.count = 4;
                        List<Map<String, Object>> contentCardPayload =
                                MessagingTestUtils.generateContentCardPayload(config);
                        payload.addAll(contentCardPayload);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface);
                                    }
                                });

                        // set up in progress propositions
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // cache propositions initially
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // test : subsequent response does not contain previously cached in-app
                        // propositions
                        Map<String, Object> secondEventData = new HashMap<>();
                        secondEventData.put("payload", contentCardPayload);
                        secondEventData.put("requestEventId", "TESTING_ID");
                        Event secondMockEvent = mock(Event.class);
                        when(secondMockEvent.getEventData()).thenReturn(secondEventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface);
                                    }
                                });

                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                secondMockEvent);

                        // setup processing completed event
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // verify parsed rules replaced in rules engine only for the first in-app
                        // response but event history rules replaced for both responses
                        verify(mockMessagingRulesEngine, times(2))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(15, rulesListCaptor.getAllValues().get(0).size());
                        assertEquals(12, rulesListCaptor.getAllValues().get(1).size());

                        // verify parsed rules replaced in feed rules engine for both responses
                        verify(mockContentCardRulesEngine, times(2))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(4, contentCardRulesListCaptor.getAllValues().get(0).size());
                        assertEquals(4, contentCardRulesListCaptor.getAllValues().get(1).size());
                        List<LaunchRule> inAppRules = new ArrayList<>();
                        for (LaunchRule launchRule : rulesListCaptor.getAllValues().get(1)) {
                            for (RuleConsequence ruleConsequence :
                                    launchRule.getConsequenceList()) {
                                String ruleType =
                                        (String)
                                                ruleConsequence
                                                        .getDetail()
                                                        .get(
                                                                MessagingTestConstants.EventDataKeys
                                                                        .RulesEngine
                                                                        .MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA);
                                if (ruleType != null) {
                                    if (ruleType.equals(SchemaType.INAPP.toString())) {
                                        inAppRules.add(launchRule);
                                    }
                                }
                            }
                        }
                        assertEquals(0, inAppRules.size());

                        // verify in-app propositions are cached for first response
                        ArgumentCaptor<Map<Surface, List<Proposition>>> cachedPropositionsCaptor =
                                ArgumentCaptor.forClass(Map.class);
                        ArgumentCaptor<List<Surface>> surfacesToRemoveCaptor =
                                ArgumentCaptor.forClass(List.class);
                        verify(mockMessagingCacheUtilities, times(2))
                                .cachePropositions(
                                        cachedPropositionsCaptor.capture(),
                                        surfacesToRemoveCaptor.capture());
                        Map<Surface, List<Proposition>> firstResponseCachedPropositions =
                                cachedPropositionsCaptor.getAllValues().get(0);
                        assertEquals(1, firstResponseCachedPropositions.size());
                        assertEquals(3, firstResponseCachedPropositions.get(inappSurface).size());
                        assertEquals(0, surfacesToRemoveCaptor.getAllValues().get(0).size());

                        // verify in-app propositions are not cached for second response
                        Map<Surface, List<Proposition>> secondResponseCachedPropositions =
                                cachedPropositionsCaptor.getAllValues().get(1);
                        assertEquals(0, secondResponseCachedPropositions.size());
                        assertEquals(
                                inappSurface, surfacesToRemoveCaptor.getAllValues().get(1).get(0));

                        // verify received propositions event not dispatched
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                    }
                });
    }

    @Test
    public void
            test_handleProcessCompletedEvent_SomeContentCardPropositionsNotReturnedInSubsequentResponse() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        Surface inappSurface = new Surface();
                        Surface contentCardSurface1 = new Surface("apifeed1");
                        Surface contentCardSurface2 = new Surface("apifeed2");
                        Surface contentCardSurface3 = new Surface("apifeed3");

                        // setup in progress in-app propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 3;
                        List<Map<String, Object>> inAppPayload =
                                MessagingTestUtils.generateInAppPayload(config);
                        List<Map<String, Object>> payload = new ArrayList<>(inAppPayload);

                        // setup in progress feed propositions and add them to the payload
                        config.count = 3;
                        List<Map<String, Object>> contentCardPayload =
                                MessagingTestUtils.generateContentCardPayload(config);
                        contentCardPayload.get(0).put("scope", contentCardSurface1.getUri());
                        contentCardPayload.get(1).put("scope", contentCardSurface2.getUri());
                        contentCardPayload.get(2).put("scope", contentCardSurface3.getUri());
                        payload.addAll(contentCardPayload);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface1);
                                        add(contentCardSurface2);
                                        add(contentCardSurface3);
                                    }
                                });

                        // set up in progress propositions
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // cache propositions initially
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // test : subsequent response does not contain two previously returned
                        // content card propositions
                        inAppPayload.add(contentCardPayload.get(0));
                        Map<String, Object> secondEventData = new HashMap<>();
                        secondEventData.put("payload", inAppPayload);
                        secondEventData.put("requestEventId", "TESTING_ID");
                        Event secondMockEvent = mock(Event.class);
                        when(secondMockEvent.getEventData()).thenReturn(secondEventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface1);
                                        add(contentCardSurface2);
                                        add(contentCardSurface3);
                                    }
                                });

                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                secondMockEvent);

                        // setup processing completed event
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // verify parsed rules replaced in rules engine for in-app and first
                        // content card event history for both responses
                        // but other two event history rules are only replaced for the first
                        // response
                        verify(mockMessagingRulesEngine, times(2))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(12, rulesListCaptor.getAllValues().get(0).size());
                        assertEquals(6, rulesListCaptor.getAllValues().get(1).size());
                        List<LaunchRule> eventHistoryRules = new ArrayList<>();
                        for (LaunchRule launchRule : rulesListCaptor.getValue()) {
                            for (RuleConsequence ruleConsequence :
                                    launchRule.getConsequenceList()) {
                                String ruleType =
                                        (String)
                                                ruleConsequence
                                                        .getDetail()
                                                        .get(
                                                                MessagingTestConstants.EventDataKeys
                                                                        .RulesEngine
                                                                        .MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA);
                                if (ruleType != null) {
                                    if (ruleType.equals(
                                            SchemaType.EVENT_HISTORY_OPERATION.toString())) {
                                        eventHistoryRules.add(launchRule);
                                    }
                                }
                            }
                        }
                        assertEquals(3, eventHistoryRules.size());

                        // verify parsed rules replaced in content card rules engine for both
                        // responses
                        // for the first card and only the first response for the other two
                        verify(mockContentCardRulesEngine, times(2))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(3, contentCardRulesListCaptor.getAllValues().get(0).size());
                        assertEquals(1, contentCardRulesListCaptor.getAllValues().get(1).size());

                        // verify received propositions event not dispatched
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                    }
                });
    }

    @Test
    public void test_handleProcessCompletedEvent_CodeBasedPropositions() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        Surface codeBasedSurface = new Surface("cbeHtml");

                        // setup in progress code propositions
                        Map<String, Object> codeBasedProposition =
                                MessagingTestUtils.getMapFromFile("codeBasedPropositionHtml.json");
                        List<Map<String, Object>> payload =
                                Collections.singletonList(codeBasedProposition);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // add another dummy surface for the same event id
                        // to ensure it gets removed from cache when the response does not have it
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID", Collections.singletonList(codeBasedSurface));

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // verify in-app rules engine not called
                        verifyNoInteractions(mockMessagingRulesEngine);

                        // verify feed rules engine not called
                        verifyNoInteractions(mockContentCardRulesEngine);

                        // verify received propositions event is dispatched
                        ArgumentCaptor<Event> dispatchEventArgumentCaptor =
                                ArgumentCaptor.forClass(Event.class);
                        verify(mockExtensionApi, times(1))
                                .dispatch(dispatchEventArgumentCaptor.capture());
                        Event dispatchedEvent = dispatchEventArgumentCaptor.getValue();
                        assertEquals(
                                MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_NOTIFICATION,
                                dispatchedEvent.getName());
                        assertEquals(EventType.MESSAGING, dispatchedEvent.getType());
                        assertEquals(
                                MessagingTestConstants.EventSource.NOTIFICATION,
                                dispatchedEvent.getSource());
                        List<Map<String, Object>> dispatchedPayload =
                                (List<Map<String, Object>>)
                                        dispatchedEvent
                                                .getEventData()
                                                .get(
                                                        MessagingTestConstants.EventDataKeys
                                                                .Messaging.Inbound.Key
                                                                .PROPOSITIONS);
                        assertEquals(1, dispatchedPayload.size());
                        assertEquals(codeBasedProposition, dispatchedPayload.get(0));
                    }
                });
    }

    @Test
    public void test_handleProcessCompletedEvent_EmptyPayload() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup an empty payload
                        List<Map<String, Object>> payload = new ArrayList<>();

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

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
                        verify(mockContentCardRulesEngine, times(0)).replaceRules(anyList());

                        // verify received propositions event not dispatched
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                    }
                });
    }

    @Test
    public void test_handleProcessCompletedEvent_ProcessCompletedEventMissingRequestId() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        Surface surface = new Surface();
                        Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                        matchedFeedRules.put(
                                surface, MessagingTestUtils.createMessagingPropositionItemList(4));
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();
                        when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                                .thenReturn(matchedFeedRules);

                        // setup in progress in-app propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 3;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);

                        // setup in progress feed propositions and add them to the payload
                        config = new MessageTestConfig();
                        config.count = 4;
                        payload.addAll(MessagingTestUtils.generateContentCardPayload(config));

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
                        verify(mockContentCardRulesEngine, times(0)).replaceRules(anyList());

                        // verify received propositions event not dispatched
                        verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
                    }
                });
    }

    @Test
    public void test_handleProcessCompletedEvent_NoValidRulesInPayload() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup in progress invalid in-app propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.isMissingRulesKey = true;
                        config.count = 1;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateInAppPayload(config);

                        // setup in progress invalid feed propositions and add them to the payload
                        config = new MessageTestConfig();
                        config.isMissingRulesKey = true;
                        config.count = 1;
                        payload.addAll(MessagingTestUtils.generateContentCardPayload(config));

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // test
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

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
                        verify(mockContentCardRulesEngine, times(0)).replaceRules(anyList());

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
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup valid surfaces
                        Surface inappSurface = new Surface();
                        Surface feedSurface = new Surface("apifeed");
                        List<Surface> surfaces =
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(feedSurface);
                                    }
                                };

                        // setup feed rules engine
                        Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                        matchedFeedRules.put(
                                feedSurface,
                                MessagingTestUtils.createMessagingPropositionItemList(3));
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();
                        when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                                .thenReturn(matchedFeedRules);

                        // setup in progress feed propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateContentCardPayload(config);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID", surfaces);
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);
                        reset(mockExtensionApi);

                        // test retrieveMessages
                        edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                surfaces, mockEvent);

                        // verify message propositions response event dispatched with 1 feed
                        // proposition
                        verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                        Event propositionsResponseEvent = eventArgumentCaptor.getValue();
                        assertEquals(
                                MESSAGE_PROPOSITIONS_RESPONSE, propositionsResponseEvent.getName());
                        assertEquals(EventType.MESSAGING, propositionsResponseEvent.getType());
                        assertEquals(
                                EventSource.RESPONSE_CONTENT,
                                propositionsResponseEvent.getSource());
                        eventData = propositionsResponseEvent.getEventData();
                        assertEquals("propositions", eventData.keySet().stream().findFirst().get());
                        List<Map<String, Object>> propositions =
                                DataReader.optTypedListOfMap(
                                        Object.class, eventData, "propositions", null);
                        assertEquals(1, propositions.size());
                    }
                });
    }

    @Test
    public void test_retrieveMessages_WithCachedPropositions() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup cached propositions
                        when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);

                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        CacheService cacheService = new FileCacheService();
                        when(mockServiceProvider.getCacheService()).thenReturn(cacheService);
                        Map<Surface, List<Proposition>> cachedPayload = new HashMap<>();
                        try {
                            MessageTestConfig config = new MessageTestConfig();
                            config.count = 1;
                            cachedPayload.put(
                                    new Surface(),
                                    InternalMessagingUtils.getPropositionsFromPayloads(
                                            MessagingTestUtils.generateInAppPayload(config)));
                        } catch (Exception e) {
                            fail(e.getMessage());
                        }
                        when(mockMessagingCacheUtilities.getCachedPropositions())
                                .thenReturn(cachedPayload);
                        edgePersonalizationResponseHandler =
                                new EdgePersonalizationResponseHandler(
                                        mockMessagingExtension,
                                        mockExtensionApi,
                                        mockMessagingRulesEngine,
                                        mockContentCardRulesEngine,
                                        mockMessagingCacheUtilities);
                        edgePersonalizationResponseHandler.setSerialWorkDispatcher(
                                mockSerialWorkDispatcher);

                        // setup valid surfaces
                        Surface inappSurface = new Surface();
                        Surface feedSurface = new Surface("apifeed");
                        List<Surface> surfaces =
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(feedSurface);
                                    }
                                };

                        // setup feed rules engine
                        Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                        matchedFeedRules.put(
                                feedSurface,
                                MessagingTestUtils.createMessagingPropositionItemList(3));
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();
                        when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                                .thenReturn(matchedFeedRules);

                        // setup in progress feed propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateContentCardPayload(config);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID", surfaces);
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        reset(mockExtensionApi);

                        // test retrieveMessages
                        edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                surfaces, mockEvent);

                        // verify message propositions response event dispatched with 1 feed
                        // proposition
                        verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                        Event propositionsResponseEvent = eventArgumentCaptor.getValue();
                        assertEquals(
                                MESSAGE_PROPOSITIONS_RESPONSE, propositionsResponseEvent.getName());
                        assertEquals(EventType.MESSAGING, propositionsResponseEvent.getType());
                        assertEquals(
                                EventSource.RESPONSE_CONTENT,
                                propositionsResponseEvent.getSource());
                        eventData = propositionsResponseEvent.getEventData();
                        assertEquals("propositions", eventData.keySet().stream().findFirst().get());
                        List<Map<String, Object>> propositions =
                                DataReader.optTypedListOfMap(
                                        Object.class, eventData, "propositions", null);
                        assertEquals(1, propositions.size());
                    }
                });
    }

    @Test
    public void test_retrieveMessages_invalidSurfacesProvided() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup invalid surfaces
                        List<Surface> surfaces = new ArrayList<>();
                        surfaces.add(new Surface("##invalid"));
                        surfaces.add(new Surface("alsoinvalid##"));

                        // setup feed rules engine
                        Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                        matchedFeedRules.put(
                                new Surface(),
                                MessagingTestUtils.createMessagingPropositionItemList(3));
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();
                        when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                                .thenReturn(matchedFeedRules);

                        // setup in progress feed propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateContentCardPayload(config);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);
                        reset(mockExtensionApi);

                        // test retrieveMessages
                        edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                surfaces, mockEvent);

                        // verify error response event dispatched
                        verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                        Event propositionsResponseEvent = eventArgumentCaptor.getValue();
                        assertEquals(
                                MESSAGE_PROPOSITIONS_RESPONSE, propositionsResponseEvent.getName());
                        assertEquals(EventType.MESSAGING, propositionsResponseEvent.getType());
                        assertEquals(
                                EventSource.RESPONSE_CONTENT,
                                propositionsResponseEvent.getSource());
                        eventData = propositionsResponseEvent.getEventData();
                        assertEquals(RESPONSE_ERROR, eventData.keySet().stream().findFirst().get());
                        assertEquals(
                                AdobeErrorExt.INVALID_REQUEST.getErrorName(),
                                eventData.get(RESPONSE_ERROR));
                    }
                });
    }

    @Test
    public void test_retrieveMessages_emptySurfacesProvided() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup empty surfaces
                        List<Surface> surfaces = new ArrayList<>();

                        // setup feed rules engine
                        Map<Surface, List<PropositionItem>> matchedFeedRules = new HashMap<>();
                        matchedFeedRules.put(
                                new Surface(),
                                MessagingTestUtils.createMessagingPropositionItemList(3));
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();
                        when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                                .thenReturn(matchedFeedRules);

                        // setup in progress feed propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateContentCardPayload(config);

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);
                        reset(mockExtensionApi);

                        // test retrieveMessages
                        edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                surfaces, mockEvent);

                        // verify one response event dispatched
                        verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                        Event propositionsResponseEvent = eventArgumentCaptor.getValue();
                        assertEquals(
                                MESSAGE_PROPOSITIONS_RESPONSE, propositionsResponseEvent.getName());
                        assertEquals(EventType.MESSAGING, propositionsResponseEvent.getType());
                        assertEquals(
                                EventSource.RESPONSE_CONTENT,
                                propositionsResponseEvent.getSource());
                        eventData = propositionsResponseEvent.getEventData();
                        assertEquals(RESPONSE_ERROR, eventData.keySet().stream().findFirst().get());
                        assertEquals(
                                AdobeErrorExt.INVALID_REQUEST.getErrorName(),
                                eventData.get(RESPONSE_ERROR));
                    }
                });
    }

    // ========================================================================================
    // edgePersonalizationResponseHandler load cached propositions on instantiation
    // ========================================================================================
    @Test
    public void
            test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);

                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        CacheService cacheService = new FileCacheService();
                        when(mockServiceProvider.getCacheService()).thenReturn(cacheService);
                        Map<Surface, List<Proposition>> payload = new HashMap<>();
                        try {
                            MessageTestConfig config = new MessageTestConfig();
                            config.count = 5;
                            List<Map<String, Object>> payloadList = new ArrayList<>();
                            payloadList.addAll(MessagingTestUtils.generateInAppPayload(config));
                            payload.put(
                                    new Surface(),
                                    InternalMessagingUtils.getPropositionsFromPayloads(
                                            payloadList));
                        } catch (Exception e) {
                            fail(e.getMessage());
                        }
                        when(mockMessagingCacheUtilities.getCachedPropositions())
                                .thenReturn(payload);

                        // test
                        edgePersonalizationResponseHandler =
                                new EdgePersonalizationResponseHandler(
                                        mockMessagingExtension,
                                        mockExtensionApi,
                                        mockMessagingRulesEngine,
                                        mockContentCardRulesEngine,
                                        mockMessagingCacheUtilities);

                        // verify cached rules replaced in rules engine
                        verify(mockMessagingRulesEngine, times(1))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(5, rulesListCaptor.getValue().size());

                        // verify in-app rules are in priority order after being loaded from the
                        // cache
                        MessagingTestUtils.verifyInAppRulesOrdering(rulesListCaptor.getValue());
                    }
                });
    }

    @Test
    public void
            test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction_whenPropositionsNotCached() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(false);
                        ;

                        // test
                        edgePersonalizationResponseHandler =
                                new EdgePersonalizationResponseHandler(
                                        mockMessagingExtension,
                                        mockExtensionApi,
                                        mockMessagingRulesEngine,
                                        mockContentCardRulesEngine,
                                        mockMessagingCacheUtilities);

                        // verify cached rules not replaced in rules engine
                        verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());
                    }
                });
    }

    @Test
    public void
            test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction_whenCachePropositionsAreNull() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);
                        when(mockMessagingCacheUtilities.getCachedPropositions()).thenReturn(null);

                        // test
                        edgePersonalizationResponseHandler =
                                new EdgePersonalizationResponseHandler(
                                        mockMessagingExtension,
                                        mockExtensionApi,
                                        mockMessagingRulesEngine,
                                        mockContentCardRulesEngine,
                                        mockMessagingCacheUtilities);

                        // verify cached rules not replaced in rules engine
                        verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());
                    }
                });
    }

    @Test
    public void
            test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction_whenCachePropositionsAreEmpty() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);
                        when(mockMessagingCacheUtilities.getCachedPropositions())
                                .thenReturn(new HashMap<>());

                        // test
                        edgePersonalizationResponseHandler =
                                new EdgePersonalizationResponseHandler(
                                        mockMessagingExtension,
                                        mockExtensionApi,
                                        mockMessagingRulesEngine,
                                        mockContentCardRulesEngine,
                                        mockMessagingCacheUtilities);

                        // verify cached rules not replaced in rules engine
                        verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());
                    }
                });
    }

    @Test
    public void
            test_cachedPropositions_cacheLoadedOnEdgePersonalizationResponseHandlerConstruction_whenCachePropositionsSchemaIsNotInApp() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);

                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        CacheService cacheService = new FileCacheService();
                        when(mockServiceProvider.getCacheService()).thenReturn(cacheService);
                        Map<Surface, List<Proposition>> payload = new HashMap<>();
                        try {
                            payload.put(
                                    new Surface(),
                                    InternalMessagingUtils.getPropositionsFromPayloads(
                                            new ArrayList<Map<String, Object>>() {
                                                {
                                                    add(
                                                            MessagingTestUtils.getMapFromFile(
                                                                    "contentCardProposition.json"));
                                                }
                                            }));
                        } catch (Exception e) {
                            fail(e.getMessage());
                        }
                        when(mockMessagingCacheUtilities.getCachedPropositions())
                                .thenReturn(payload);

                        // test
                        edgePersonalizationResponseHandler =
                                new EdgePersonalizationResponseHandler(
                                        mockMessagingExtension,
                                        mockExtensionApi,
                                        mockMessagingRulesEngine,
                                        mockContentCardRulesEngine,
                                        mockMessagingCacheUtilities);

                        // verify cached rules replaced in rules engine
                        verify(mockMessagingRulesEngine, times(0)).replaceRules(anyList());
                    }
                });
    }

    // ========================================================================================
    // createInAppMessage
    // ========================================================================================
    @Test
    public void test_createInAppMessage() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<PresentableMessageMapper>
                            presentableMessageMapperMockedStatic =
                                    Mockito.mockStatic(PresentableMessageMapper.class)) {
                        // setup
                        presentableMessageMapperMockedStatic
                                .when(PresentableMessageMapper::getInstance)
                                .thenReturn(mockPresentableMessageMapper);
                        try {
                            when(mockPresentableMessageMapper.createMessage(
                                            any(), any(), any(), any()))
                                    .thenReturn(mockInternalMessage);

                            Map<String, Object> data = new HashMap<>();
                            Map<String, Object> mobileParameters = new HashMap<>();
                            data.put(
                                    MessagingTestConstants.ConsequenceDetailDataKeys.REMOTE_ASSETS,
                                    new ArrayList<String>());
                            data.put(
                                    MessagingTestConstants.ConsequenceDetailDataKeys
                                            .MOBILE_PARAMETERS,
                                    mobileParameters);
                            data.put(
                                    MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT,
                                    "<html><head></head><body bgcolor=\"black\"><br /><br /><br"
                                        + " /><br /><br /><br /><h1 align=\"center\" style=\"color:"
                                        + " white;\">IN-APP MESSAGING POWERED BY <br />OFFER"
                                        + " DECISIONING</h1><h1 align=\"center\"><a style=\"color:"
                                        + " white;\" href=\"adbinapp://cancel\" >dismiss"
                                        + " me</a></h1></body></html>");
                            PropositionItem propositionItem =
                                    new PropositionItem("123456789", SchemaType.INAPP, data);

                            // test
                            edgePersonalizationResponseHandler.createInAppMessage(propositionItem);

                            // verify MessagingFullscreenMessage.trigger() then
                            // MessagingFullscreenMessage.show() called
                            verify(mockPresentableMessageMapper, times(1))
                                    .createMessage(any(), eq(propositionItem), any(), any());
                            verify(mockInternalMessage, times(1)).trigger();
                            verify(mockInternalMessage, times(1)).show();
                        } catch (MessageRequiredFieldMissingException e) {
                            fail();
                        }
                    }
                });
    }

    @Test
    public void test_createInAppMessage_NullPropositionItem() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<PresentableMessageMapper>
                            presentableMessageMapperMockedStatic =
                                    Mockito.mockStatic(PresentableMessageMapper.class)) {
                        // setup
                        presentableMessageMapperMockedStatic
                                .when(PresentableMessageMapper::getInstance)
                                .thenReturn(mockPresentableMessageMapper);
                        when(mockPresentableMessageMapper.getMessageFromPresentableId(anyString()))
                                .thenReturn(mockInternalMessage);

                        // test
                        edgePersonalizationResponseHandler.createInAppMessage(null);

                        // verify no message object created
                        verifyNoInteractions(mockPresentableMessageMapper);
                        verifyNoInteractions(mockInternalMessage);
                    }
                });
    }

    @Test
    public void test_createInAppMessage_MissingRequiredField() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<PresentableMessageMapper>
                            presentableMessageMapperMockedStatic =
                                    Mockito.mockStatic(PresentableMessageMapper.class)) {
                        // setup
                        presentableMessageMapperMockedStatic
                                .when(PresentableMessageMapper::getInstance)
                                .thenReturn(mockPresentableMessageMapper);
                        try {
                            when(mockPresentableMessageMapper.createMessage(
                                            any(), any(), any(), any()))
                                    .thenThrow(new MessageRequiredFieldMissingException(""));

                            Map<String, Object> data = new HashMap<>();
                            Map<String, Object> mobileParameters = new HashMap<>();
                            data.put(
                                    MessagingTestConstants.ConsequenceDetailDataKeys.REMOTE_ASSETS,
                                    new ArrayList<String>());
                            data.put(
                                    MessagingTestConstants.ConsequenceDetailDataKeys
                                            .MOBILE_PARAMETERS,
                                    mobileParameters);
                            data.put(
                                    MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT,
                                    "<html><head></head><body bgcolor=\"black\"><br /><br /><br"
                                        + " /><br /><br /><br /><h1 align=\"center\" style=\"color:"
                                        + " white;\">IN-APP MESSAGING POWERED BY <br />OFFER"
                                        + " DECISIONING</h1><h1 align=\"center\"><a style=\"color:"
                                        + " white;\" href=\"adbinapp://cancel\" >dismiss"
                                        + " me</a></h1></body></html>");
                            PropositionItem propositionItem =
                                    new PropositionItem("123456789", SchemaType.INAPP, data);

                            // test
                            edgePersonalizationResponseHandler.createInAppMessage(propositionItem);

                            // verify no message object created
                            verify(mockPresentableMessageMapper, times(1))
                                    .createMessage(any(), eq(propositionItem), any(), any());
                            verify(mockInternalMessage, times(0)).trigger();
                            verify(mockInternalMessage, times(0)).show();
                            verifyNoInteractions(mockInternalMessage);
                        } catch (MessageRequiredFieldMissingException e) {
                            fail();
                        }
                    }
                });
    }

    // ========================================================================================
    // edgePersonalizationResponseHandler handleEventHistoryDisqualifyEvent
    // ========================================================================================
    @Test
    public void test_handleEventHistoryRuleConsequence() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup valid surface
                        Surface feedSurface = new Surface("apifeed");

                        // setup in-memory qualified content cards
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Proposition> propositions =
                                MessagingTestUtils.generateQualifiedContentCards(config);
                        Map<Surface, List<Proposition>> qualifiedContentCards =
                                new HashMap<Surface, List<Proposition>>() {
                                    {
                                        put(feedSurface, propositions);
                                    }
                                };
                        edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                                qualifiedContentCards);

                        // setup PropositionItem of type EventHistoryOperationSchemaData
                        final Map<String, String> contentMap = new HashMap<>();
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.EVENT_TYPE,
                                MessagingConstants.EventHistoryOperationEventTypes.DISQUALIFY);
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.MESSAGE_ID,
                                "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300");
                        final Map<String, Object> eventHistoryData = new HashMap<>();
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.CONTENT, contentMap);
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.OPERATION,
                                "insertIfNotExists");
                        final Map<String, Object> eventHistoryRuleConsequenceDetail =
                                new HashMap<>();
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.DATA, eventHistoryData);
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.ID, "mockConsequenceId");
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.SCHEMA,
                                SchemaType.EVENT_HISTORY_OPERATION.toString());
                        final PropositionItem propositionItem =
                                PropositionItem.fromRuleConsequenceDetail(
                                        eventHistoryRuleConsequenceDetail);

                        // test
                        edgePersonalizationResponseHandler.handleEventHistoryRuleConsequence(
                                propositionItem);

                        // verify qualified content cards matching the activity id are removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
                        // verify there are now 3 qualified content cards after removing one with
                        // activity id
                        // "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300"
                        final List<Proposition> qualifiedContentCardsList =
                                qualifiedContentCardsBySurface.get(feedSurface);
                        assertEquals(3, qualifiedContentCardsList.size());
                        assertEquals(
                                "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44301",
                                qualifiedContentCardsList.get(0).getActivityId());
                        assertEquals(
                                "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44302",
                                qualifiedContentCardsList.get(1).getActivityId());
                        assertEquals(
                                "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44303",
                                qualifiedContentCardsList.get(2).getActivityId());
                    }
                });
    }

    @Test
    public void test_handleEventHistoryRuleConsequence_HasNoActivityId() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup valid surface
                        Surface feedSurface = new Surface("apifeed");

                        // setup in-memory qualified content cards
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Proposition> propositions =
                                MessagingTestUtils.generateQualifiedContentCards(config);
                        Map<Surface, List<Proposition>> qualifiedContentCards =
                                new HashMap<Surface, List<Proposition>>() {
                                    {
                                        put(feedSurface, propositions);
                                    }
                                };
                        edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                                qualifiedContentCards);

                        // setup PropositionItem of type EventHistoryOperationSchemaData
                        final Map<String, String> contentMap = new HashMap<>();
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.EVENT_TYPE,
                                MessagingConstants.EventHistoryOperationEventTypes.DISQUALIFY);
                        final Map<String, Object> eventHistoryData = new HashMap<>();
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.CONTENT, contentMap);
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.OPERATION,
                                "insertIfNotExists");
                        final Map<String, Object> eventHistoryRuleConsequenceDetail =
                                new HashMap<>();
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.DATA, eventHistoryData);
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.ID, "mockConsequenceId");
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.SCHEMA,
                                SchemaType.EVENT_HISTORY_OPERATION.toString());
                        final PropositionItem propositionItem =
                                PropositionItem.fromRuleConsequenceDetail(
                                        eventHistoryRuleConsequenceDetail);

                        // test
                        edgePersonalizationResponseHandler.handleEventHistoryRuleConsequence(
                                propositionItem);

                        // verify qualified content cards matching the activity id are removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
                        // verify there are now 3 qualified content cards after removing one with
                        // activity id
                        // "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300"
                        final List<Proposition> qualifiedContentCardsList =
                                qualifiedContentCardsBySurface.get(feedSurface);
                        assertEquals(propositions, qualifiedContentCardsList);
                    }
                });
    }

    @Test
    public void test_handleEventHistoryRuleConsequence_HasNoEventType() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup valid surface
                        Surface feedSurface = new Surface("apifeed");

                        // setup in-memory qualified content cards
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Proposition> propositions =
                                MessagingTestUtils.generateQualifiedContentCards(config);
                        Map<Surface, List<Proposition>> qualifiedContentCards =
                                new HashMap<Surface, List<Proposition>>() {
                                    {
                                        put(feedSurface, propositions);
                                    }
                                };
                        edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                                qualifiedContentCards);

                        // setup PropositionItem of type EventHistoryOperationSchemaData
                        final Map<String, String> contentMap = new HashMap<>();
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.MESSAGE_ID,
                                "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300");
                        final Map<String, Object> eventHistoryData = new HashMap<>();
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.CONTENT, contentMap);
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.OPERATION,
                                "insertIfNotExists");
                        final Map<String, Object> eventHistoryRuleConsequenceDetail =
                                new HashMap<>();
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.DATA, eventHistoryData);
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.ID, "mockConsequenceId");
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.SCHEMA,
                                SchemaType.EVENT_HISTORY_OPERATION.toString());
                        final PropositionItem propositionItem =
                                PropositionItem.fromRuleConsequenceDetail(
                                        eventHistoryRuleConsequenceDetail);

                        // test
                        edgePersonalizationResponseHandler.handleEventHistoryRuleConsequence(
                                propositionItem);

                        // verify qualified content cards matching the activity id are removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
                        // verify there are now 3 qualified content cards after removing one with
                        // activity id
                        // "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300"
                        final List<Proposition> qualifiedContentCardsList =
                                qualifiedContentCardsBySurface.get(feedSurface);
                        assertEquals(propositions, qualifiedContentCardsList);
                    }
                });
    }

    @Test
    public void test_handleEventHistoryRuleConsequence_HasQualifyEventType() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        // setup valid surface
                        Surface feedSurface = new Surface("apifeed");

                        // setup in-memory qualified content cards
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 4;
                        List<Proposition> propositions =
                                MessagingTestUtils.generateQualifiedContentCards(config);
                        Map<Surface, List<Proposition>> qualifiedContentCards =
                                new HashMap<Surface, List<Proposition>>() {
                                    {
                                        put(feedSurface, propositions);
                                    }
                                };
                        edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                                qualifiedContentCards);

                        // setup PropositionItem of type EventHistoryOperationSchemaData
                        final Map<String, String> contentMap = new HashMap<>();
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.EVENT_TYPE,
                                MessagingConstants.EventHistoryOperationEventTypes.QUALIFY);
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.MESSAGE_ID,
                                "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300");
                        final Map<String, Object> eventHistoryData = new HashMap<>();
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.CONTENT, contentMap);
                        eventHistoryData.put(
                                MessagingConstants.ConsequenceDetailDataKeys.OPERATION,
                                "insertIfNotExists");
                        final Map<String, Object> eventHistoryRuleConsequenceDetail =
                                new HashMap<>();
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.DATA, eventHistoryData);
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.ID, "mockConsequenceId");
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.SCHEMA,
                                SchemaType.EVENT_HISTORY_OPERATION.toString());
                        final PropositionItem propositionItem =
                                PropositionItem.fromRuleConsequenceDetail(
                                        eventHistoryRuleConsequenceDetail);

                        // test
                        edgePersonalizationResponseHandler.handleEventHistoryRuleConsequence(
                                propositionItem);

                        // verify qualified content cards matching the activity id are removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
                        // verify there are now 3 qualified content cards after removing one with
                        // activity id
                        // "9c8ec035-6b3b-470e-8ae5-e539c7123809#c7c1497e-e5a3-4499-ae37-ba76e1e44300"
                        final List<Proposition> qualifiedContentCardsList =
                                qualifiedContentCardsBySurface.get(feedSurface);
                        assertEquals(propositions, qualifiedContentCardsList);
                    }
                });
    }
}
