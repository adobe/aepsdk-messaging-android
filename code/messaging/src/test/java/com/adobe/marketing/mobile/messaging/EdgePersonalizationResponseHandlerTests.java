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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import android.net.ConnectivityManager;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.internal.util.NetworkUtils;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.AppContextService;
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
import java.lang.ref.SoftReference;
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
        ContentCardMapper.getInstance().clear();

        if (cacheDir.exists()) {
            cacheDir.delete();
        }
    }

    /**
     * Pre-populates ContentCardMapper with a mock ContentCardSchemaData entry keyed by the given
     * activity ID, so tests can verify removal behaviour. Uses a separate unique ID to ensure the
     * mapper is keyed by activityId, not uniqueId.
     */
    private void seedContentCardMapper(String activityId) {
        ContentCardSchemaData schemaData = mock(ContentCardSchemaData.class);
        PropositionItem propItem = mock(PropositionItem.class);
        Proposition prop = mock(Proposition.class);
        schemaData.parent = propItem;
        propItem.propositionReference = new SoftReference<>(prop);
        when(propItem.getProposition()).thenReturn(prop);
        when(prop.getUniqueId()).thenReturn("uniqueId_" + activityId);
        when(prop.getActivityId()).thenReturn(activityId);
        ContentCardMapper.getInstance().storeContentCardSchemaData(schemaData);
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
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
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

    // ========================================================================================
    // createPersonalizationRequestEventData (custom XDM / data merge)
    // ========================================================================================
    @Test
    public void createPersonalizationRequestEventData_baseStructure_whenNoCustomXdmOrData() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/surface\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(
                                            Collections.singletonList("mobileapp://test/surface"),
                                            null,
                                            null);

                    assertEquals(expected, actual);
                });
    }

    @Test
    public void createPersonalizationRequestEventData_withCustomXdm() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> customXdm = new HashMap<>();
                    customXdm.put("_chipotle", Collections.singletonMap("restaurantId", "6099"));

                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\","
                                                    + " \"_chipotle\":{\"restaurantId\":\"6099\"}},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/surface\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(
                                            Collections.singletonList("mobileapp://test/surface"),
                                            customXdm,
                                            null);

                    assertEquals(expected, actual);
                });
    }

    @Test
    public void createPersonalizationRequestEventData_withCustomData() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> customData = new HashMap<>();
                    customData.put("customKey", "customValue");

                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"customKey\":\"customValue\","
                                                    + " \"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/surface\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(
                                            Collections.singletonList("mobileapp://test/surface"),
                                            null,
                                            customData);

                    assertEquals(expected, actual);
                });
    }

    @Test
    public void createPersonalizationRequestEventData_customXdmCannotOverrideInternalEventType() {
        runUsingMockedServiceProvider(
                () -> {
                    // caller attempts to override the internal eventType
                    Map<String, Object> customXdm = new HashMap<>();
                    customXdm.put("eventType", "some.other.type");
                    customXdm.put("_chipotle", Collections.singletonMap("restaurantId", "6099"));

                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\","
                                                    + " \"_chipotle\":{\"restaurantId\":\"6099\"}},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/surface\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(
                                            Collections.singletonList("mobileapp://test/surface"),
                                            customXdm,
                                            null);

                    // internal eventType wins; custom sibling preserved
                    assertEquals(expected, actual);
                });
    }

    @Test
    public void
            createPersonalizationRequestEventData_customDataCannotOverrideInternalAdobeNamespace() {
        runUsingMockedServiceProvider(
                () -> {
                    // caller attempts to override the internal __adobe response-format namespace
                    Map<String, Object> customData = new HashMap<>();
                    customData.put(
                            "__adobe",
                            Collections.singletonMap(
                                    "ajo",
                                    Collections.singletonMap("in-app-response-format", 999)));
                    customData.put("customKey", "customValue");

                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"customKey\":\"customValue\","
                                                    + " \"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/surface\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(
                                            Collections.singletonList("mobileapp://test/surface"),
                                            null,
                                            customData);

                    // internal __adobe wins; custom sibling preserved
                    assertEquals(expected, actual);
                });
    }

    @Test
    public void createPersonalizationRequestEventData_withCustomXdmAndData() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> customXdm = new HashMap<>();
                    customXdm.put("_chipotle", Collections.singletonMap("restaurantId", "6099"));
                    Map<String, Object> customData = new HashMap<>();
                    customData.put("customKey", "customValue");

                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\","
                                                    + " \"_chipotle\":{\"restaurantId\":\"6099\"}},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"customKey\":\"customValue\","
                                                    + " \"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/surface\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(
                                            Collections.singletonList("mobileapp://test/surface"),
                                            customXdm,
                                            customData);

                    assertEquals(expected, actual);
                });
    }

    @Test
    public void createPersonalizationRequestEventData_multipleSurfaces() {
        runUsingMockedServiceProvider(
                () -> {
                    List<String> surfaceUris = new ArrayList<>();
                    surfaceUris.add("mobileapp://test/one");
                    surfaceUris.add("mobileapp://test/two");

                    Map<String, Object> expected = null;
                    try {
                        expected =
                                JSONUtils.toMap(
                                        new JSONObject(
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
                                                    + " \"request\":{\"sendCompletion\":true},"
                                                    + " \"data\":{\"__adobe\":{\"ajo\":{\"in-app-response-format\":2}}},"
                                                    + " \"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://test/one\",\"mobileapp://test/two\"],"
                                                    + " \"schemas\":[\"https://ns.adobe.com/personalization/html-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/json-content-item\","
                                                    + " \"https://ns.adobe.com/personalization/ruleset-item\"]}}}"));
                    } catch (JSONException e) {
                        fail(e.getMessage());
                    }

                    Map<String, Object> actual =
                            edgePersonalizationResponseHandler
                                    .createPersonalizationRequestEventData(surfaceUris, null, null);

                    assertEquals(expected, actual);
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
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
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
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
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

                    // verify serial work dispatcher is resumed when API fails
                    verify(mockSerialWorkDispatcher, times(1)).resume();
                });
    }

    @Test
    public void test_fetchMessages_AdobeErrorReceived_InvokesCompletionCallbackAndRemovesHandler() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup
                    String edgeRequestEventId = "mockEventId";
                    when(mockMessagingExtension.completionHandlerForEdgeRequestEventId(
                                    eq(edgeRequestEventId)))
                            .thenReturn(completionHandler);

                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);

                    Event edgeRequestEvent = eventArgumentCaptor.getValue();
                    assertEquals(
                            MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT,
                            edgeRequestEvent.getName());

                    // simulate API failure via AdobeCallbackWithError.fail()
                    adobeCallbackWithErrorArgumentCaptor.getValue().fail(mockAdobeError);

                    // verify completion callback invoked with false when API fails
                    verify(mockAdobeCallback, times(1)).call(false);

                    // verify completion handler was retrieved/removed from list for this edge
                    // request event id
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForEdgeRequestEventId(edgeRequestEventId);

                    // verify serial work dispatcher is resumed on failure
                    verify(mockSerialWorkDispatcher, times(1)).resume();
                });
    }

    @Test
    public void test_fetchMessages_AdobeErrorReceived_NoCompletionHandler_ResumesDispatcher() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup - no completion handler registered for edge request (e.g. already
                    // removed or never added)
                    when(mockMessagingExtension.completionHandlerForEdgeRequestEventId(anyString()))
                            .thenReturn(null);

                    // test
                    edgePersonalizationResponseHandler.fetchPropositions(mockEvent, null);
                    adobeCallbackWithErrorArgumentCaptor.getValue().fail(mockAdobeError);

                    // verify completion callback not invoked when no handler found
                    verifyNoInteractions(mockAdobeCallback);

                    // verify completion handler was still looked up for removal
                    verify(mockMessagingExtension, times(1))
                            .completionHandlerForEdgeRequestEventId(anyString());

                    // verify serial work dispatcher is resumed even when no handler
                    verify(mockSerialWorkDispatcher, times(1)).resume();
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
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
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
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
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
                                                "{\"xdm\":{\"eventType\":\"decisioning.propositionFetch\"},"
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
                        eventData.put("requestEventId", "TESTING_ID_1");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID_1",
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
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID_1");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // cache propositions initially
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // test : subsequent response does not contain previously cached in-app
                        // propositions
                        eventData = new HashMap<>();
                        eventData.put("payload", contentCardPayload);
                        eventData.put("requestEventId", "TESTING_ID_2");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID_2",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface);
                                    }
                                });

                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID_2");
                        when(mockEvent.getEventData()).thenReturn(eventData);

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
                        eventData.put("requestEventId", "TESTING_ID_1");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID_1",
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
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID_1");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // cache propositions initially
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // test : subsequent response does not contain two previously returned
                        // content card propositions
                        inAppPayload.add(contentCardPayload.get(0));
                        eventData = new HashMap<>();
                        eventData.put("payload", inAppPayload);
                        eventData.put("requestEventId", "TESTING_ID_2");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID_2",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                        add(contentCardSurface1);
                                        add(contentCardSurface2);
                                        add(contentCardSurface3);
                                    }
                                });

                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID_2");
                        when(mockEvent.getEventData()).thenReturn(eventData);

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
    public void test_handleProcessCompletedEvent_SeparateIAMAndContentCardRequests() {
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

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", payload);
                        eventData.put("requestEventId", "TESTING_ID_IAM");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID_IAM",
                                new ArrayList<Surface>() {
                                    {
                                        add(inappSurface);
                                    }
                                });

                        // set up in progress propositions
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID_IAM");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        // cache propositions initially
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // test : subsequent response does not contain previously cached in-app
                        // propositions
                        // setup in progress feed propositions and add them to the payload
                        config.count = 4;
                        List<Map<String, Object>> contentCardPayload =
                                MessagingTestUtils.generateContentCardPayload(config);
                        payload.addAll(contentCardPayload);

                        eventData = new HashMap<>();
                        eventData.put("payload", contentCardPayload);
                        eventData.put("requestEventId", "TESTING_ID_CONTENT_CARD");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID_CONTENT_CARD",
                                new ArrayList<Surface>() {
                                    {
                                        add(contentCardSurface);
                                    }
                                });

                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        // setup processing completed event
                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID_CONTENT_CARD");
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // verify parsed rules replaced in rules engine only for the first in-app
                        // response but event history rules replaced for both responses
                        verify(mockMessagingRulesEngine, times(2))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(3, rulesListCaptor.getAllValues().get(0).size());
                        assertEquals(15, rulesListCaptor.getAllValues().get(1).size());

                        // verify content card rules engine synced for both responses
                        verify(mockContentCardRulesEngine, times(2))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(0, contentCardRulesListCaptor.getAllValues().get(0).size());
                        assertEquals(4, contentCardRulesListCaptor.getAllValues().get(1).size());
                        List<LaunchRule> eventHistoryRules = new ArrayList<>();
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
                                    } else if (ruleType.equals(
                                            SchemaType.EVENT_HISTORY_OPERATION.toString())) {
                                        eventHistoryRules.add(launchRule);
                                    }
                                }
                            }
                        }
                        assertEquals(3, inAppRules.size());
                        assertEquals(12, eventHistoryRules.size());

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

                        // verify rules engines synced with empty rules (code-based propositions
                        // produce no rules)
                        verify(mockMessagingRulesEngine, times(1))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(0, rulesListCaptor.getValue().size());
                        verify(mockContentCardRulesEngine, times(1))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(0, contentCardRulesListCaptor.getValue().size());

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

                        // verify rules engines synced with empty rules
                        verify(mockMessagingRulesEngine, times(1))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(0, rulesListCaptor.getValue().size());
                        verify(mockContentCardRulesEngine, times(1))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(0, contentCardRulesListCaptor.getValue().size());

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

                        // verify rules engines synced with empty rules (invalid payload produces no
                        // parseable rules)
                        verify(mockMessagingRulesEngine, times(1))
                                .replaceRules(rulesListCaptor.capture());
                        assertEquals(0, rulesListCaptor.getValue().size());
                        verify(mockContentCardRulesEngine, times(1))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertEquals(0, contentCardRulesListCaptor.getValue().size());

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

    @Test
    public void
            test_retrieveMessages_doesNotStoreContentCardsInTheSameMapAsOtherInMemoryPropositions() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // setup a shared surface for inbox and content cards
                        Surface surface = new Surface("apifeed");
                        List<Surface> surfaces = Collections.singletonList(surface);

                        // build an inbox proposition payload to populate inMemoryPropositions
                        Map<String, Object> inboxItemData =
                                MessagingTestUtils.getMapFromFile("inboxPropositionContent.json");
                        Map<String, Object> inboxItem = new HashMap<>();
                        inboxItem.put("id", "inbox-item-id-001");
                        inboxItem.put("schema", SchemaType.INBOX.toString());
                        inboxItem.put("data", inboxItemData);

                        Map<String, Object> activityMap = new HashMap<>();
                        activityMap.put("id", "test-inbox-activity-id");
                        Map<String, Object> scopeDetails = new HashMap<>();
                        scopeDetails.put("activity", activityMap);
                        scopeDetails.put("decisionProvider", "AJO");

                        Map<String, Object> inboxPropositionMap = new HashMap<>();
                        inboxPropositionMap.put("id", "inbox-proposition-id-001");
                        inboxPropositionMap.put("scope", surface.getUri());
                        inboxPropositionMap.put("scopeDetails", scopeDetails);
                        inboxPropositionMap.put("items", Collections.singletonList(inboxItem));

                        Map<String, Object> eventData = new HashMap<>();
                        eventData.put("payload", Collections.singletonList(inboxPropositionMap));
                        eventData.put("requestEventId", "TESTING_ID");
                        Event mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "TESTING_ID", surfaces);
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                mockEvent);

                        eventData = new HashMap<>();
                        eventData.put(ENDING_EVENT_ID, "TESTING_ID");
                        mockEvent = mock(Event.class);
                        when(mockEvent.getEventData()).thenReturn(eventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(mockEvent);

                        // setup 2 qualified content cards in contentCardsBySurface
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 2;
                        Map<Surface, List<Proposition>> qualifiedContentCards = new HashMap<>();
                        qualifiedContentCards.put(
                                surface, MessagingTestUtils.generateQualifiedContentCards(config));
                        edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                                qualifiedContentCards);
                        // the qualified cards represent a network refresh for this surface, so mark
                        // it network-refreshed (get filters out non-network surfaces when the
                        // offline flag is off)
                        edgePersonalizationResponseHandler
                                .getNetworkRefreshedSurfaces()
                                .add(surface);

                        reset(mockExtensionApi);
                        ArgumentCaptor<Event> localCaptor = ArgumentCaptor.forClass(Event.class);

                        // first call — dispatches 1 inbox + 2 content cards = 3 propositions
                        edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                surfaces, mockEvent);

                        // clear content cards — simulates a scenario where they are no longer
                        // qualified
                        edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                                new HashMap<>());

                        // second call — should dispatch only the 1 inbox proposition
                        // without the fix, the 2 content cards leaked into inMemoryPropositions
                        // on the first call, so the second call would wrongly return 3
                        edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                surfaces, mockEvent);

                        verify(mockExtensionApi, times(2)).dispatch(localCaptor.capture());
                        List<Event> dispatchedEvents = localCaptor.getAllValues();

                        List<Map<String, Object>> firstCallPropositions =
                                DataReader.optTypedListOfMap(
                                        Object.class,
                                        dispatchedEvents.get(0).getEventData(),
                                        "propositions",
                                        null);
                        List<Map<String, Object>> secondCallPropositions =
                                DataReader.optTypedListOfMap(
                                        Object.class,
                                        dispatchedEvents.get(1).getEventData(),
                                        "propositions",
                                        null);

                        // verify first call has 1 inbox and 2 content card (ruleset) propositions
                        assertEquals(3, firstCallPropositions.size());
                        assertEquals(
                                SchemaType.INBOX,
                                Proposition.fromEventData(firstCallPropositions.get(0))
                                        .getItems()
                                        .get(0)
                                        .getSchema());
                        assertEquals(
                                SchemaType.RULESET,
                                Proposition.fromEventData(firstCallPropositions.get(1))
                                        .getItems()
                                        .get(0)
                                        .getSchema());
                        assertEquals(
                                SchemaType.RULESET,
                                Proposition.fromEventData(firstCallPropositions.get(2))
                                        .getItems()
                                        .get(0)
                                        .getSchema());

                        // verify second call contains only the inbox proposition — no leaked
                        // content cards
                        assertEquals(1, secondCallPropositions.size());
                        assertEquals(
                                SchemaType.INBOX,
                                Proposition.fromEventData(secondCallPropositions.get(0))
                                        .getItems()
                                        .get(0)
                                        .getSchema());
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
    public void test_handleEventHistoryRuleConsequence_Disqualify() {
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
                                MessagingTestConstants.EventMask.Mask.ACTIVITY_ID,
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
    public void test_handleEventHistoryRuleConsequence_Unqualify() {
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
                                MessagingConstants.EventHistoryOperationEventTypes.UNQUALIFY);
                        contentMap.put(
                                MessagingTestConstants.EventMask.Mask.ACTIVITY_ID,
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
    public void test_handleEventHistoryRuleConsequence_HasNoEventHistoryOperationConsequence() {
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

                        final Map<String, Object> eventHistoryRuleConsequenceDetail =
                                new HashMap<>();
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.DATA,
                                new HashMap<String, Object>() {
                                    {
                                        put("key", "value");
                                    }
                                });
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.ID, "mockConsequenceId");
                        eventHistoryRuleConsequenceDetail.put(
                                MessagingConstants.ConsequenceDetailKeys.SCHEMA, SchemaType.INAPP);
                        final PropositionItem propositionItem =
                                PropositionItem.fromRuleConsequenceDetail(
                                        eventHistoryRuleConsequenceDetail);

                        // test
                        edgePersonalizationResponseHandler.handleEventHistoryRuleConsequence(
                                propositionItem);

                        // verify qualified content cards matching the activity id are not removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
                        final List<Proposition> qualifiedContentCardsList =
                                qualifiedContentCardsBySurface.get(feedSurface);
                        assertEquals(propositions, qualifiedContentCardsList);
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

                        // verify qualified content cards matching the activity id are not removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
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
                                MessagingTestConstants.EventMask.Mask.ACTIVITY_ID,
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

                        // verify qualified content cards matching the activity id are not removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
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
                                MessagingTestConstants.EventMask.Mask.ACTIVITY_ID,
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

                        // verify qualified content cards matching the activity id are not removed
                        Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                                edgePersonalizationResponseHandler
                                        .getQualifiedContentCardsBySurface();
                        assertEquals(1, qualifiedContentCardsBySurface.size());
                        final List<Proposition> qualifiedContentCardsList =
                                qualifiedContentCardsBySurface.get(feedSurface);
                        assertEquals(propositions, qualifiedContentCardsList);
                    }
                });
    }

    // ========================================================================================
    // removeOrReplaceContentCards
    // ========================================================================================
    @Test
    public void test_removeOrReplaceContentCards_EvictsAllRequestedSurfaces_WhenNoQualifiedCards() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: pre-populate contentCardsBySurface with two surfaces
                    Surface surfaceA = new Surface("feed1");
                    Surface surfaceB = new Surface("feed2");

                    MessageTestConfig configA = new MessageTestConfig();
                    configA.count = 2;
                    List<Proposition> propositionsA =
                            MessagingTestUtils.generateQualifiedContentCards(configA);
                    MessageTestConfig configB = new MessageTestConfig();
                    configB.count = 1;
                    List<Proposition> propositionsB =
                            MessagingTestUtils.generateQualifiedContentCards(configB);

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, propositionsA);
                    contentCards.put(surfaceB, propositionsB);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);
                    assertEquals(
                            2,
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .size());

                    // mock evaluate to return null (no qualified content cards)
                    when(mockContentCardRulesEngine.evaluate(any(Event.class))).thenReturn(null);

                    Event testEvent = mock(Event.class);
                    List<Surface> requestedSurfaces = new ArrayList<>();
                    requestedSurfaces.add(surfaceA);
                    requestedSurfaces.add(surfaceB);

                    // test
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify all requested surfaces are evicted
                    Map<Surface, List<Proposition>> result =
                            edgePersonalizationResponseHandler.getQualifiedContentCardsBySurface();
                    assertEquals(0, result.size());
                });
    }

    @Test
    public void
            test_removeOrReplaceContentCards_OnlyEvictsRequestedSurfaces_PreservesUnrequested() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: pre-populate contentCardsBySurface with three surfaces
                    Surface surfaceA = new Surface("feed1");
                    Surface surfaceB = new Surface("feed2");
                    Surface surfaceC = new Surface("feed3");

                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    List<Proposition> propositionsA =
                            MessagingTestUtils.generateQualifiedContentCards(config);
                    List<Proposition> propositionsB =
                            MessagingTestUtils.generateQualifiedContentCards(config);
                    List<Proposition> propositionsC =
                            MessagingTestUtils.generateQualifiedContentCards(config);

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, propositionsA);
                    contentCards.put(surfaceB, propositionsB);
                    contentCards.put(surfaceC, propositionsC);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);
                    assertEquals(
                            3,
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .size());

                    // mock evaluate to return null (no qualified content cards)
                    when(mockContentCardRulesEngine.evaluate(any(Event.class))).thenReturn(null);

                    Event testEvent = mock(Event.class);
                    // request only A and B — not C
                    List<Surface> requestedSurfaces = new ArrayList<>();
                    requestedSurfaces.add(surfaceA);
                    requestedSurfaces.add(surfaceB);

                    // test
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify A and B are evicted, C is preserved
                    Map<Surface, List<Proposition>> result =
                            edgePersonalizationResponseHandler.getQualifiedContentCardsBySurface();
                    assertEquals(1, result.size());
                    assertNotNull(result.get(surfaceC));
                    assertNull(result.get(surfaceA));
                    assertNull(result.get(surfaceB));
                });
    }

    @Test
    public void
            test_removeOrReplaceContentCards_ReplacesPropositionsForQualifiedSurface_AndEvictsAbsentSurface() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: pre-populate contentCardsBySurface with two surfaces
                    Surface surfaceA = new Surface("feed1");
                    Surface surfaceB = new Surface("feed2");

                    MessageTestConfig configA = new MessageTestConfig();
                    configA.count = 2;
                    List<Proposition> oldPropositionsA =
                            MessagingTestUtils.generateQualifiedContentCards(configA);
                    MessageTestConfig configB = new MessageTestConfig();
                    configB.count = 1;
                    List<Proposition> oldPropositionsB =
                            MessagingTestUtils.generateQualifiedContentCards(configB);

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, oldPropositionsA);
                    contentCards.put(surfaceB, oldPropositionsB);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);
                    assertEquals(2, oldPropositionsA.size());
                    assertEquals(1, oldPropositionsB.size());

                    // populate propositionInfo via reflection so
                    // getPropositionsFromContentCardRulesEngine
                    // can reconstruct propositions for items returned by evaluate()
                    try {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("id", "newActivityId");
                        Map<String, Object> scopeDetails = new HashMap<>();
                        scopeDetails.put("activity", activity);
                        scopeDetails.put("correlationID", "testCorrelationId");

                        Map<String, Object> infoMap = new HashMap<>();
                        infoMap.put("id", "newPropositionId");
                        infoMap.put("scope", surfaceA.getUri());
                        infoMap.put("scopeDetails", scopeDetails);
                        PropositionInfo info = PropositionInfo.create(infoMap);

                        java.lang.reflect.Field propositionInfoField =
                                EdgePersonalizationResponseHandler.class.getDeclaredField(
                                        "propositionInfo");
                        propositionInfoField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<String, PropositionInfo> propositionInfoMap =
                                (Map<String, PropositionInfo>)
                                        propositionInfoField.get(
                                                edgePersonalizationResponseHandler);
                        propositionInfoMap.put("183639c4-cb37-458e-a8ef-4e130d767ebf", info);
                    } catch (Exception e) {
                        fail("Failed to set propositionInfo via reflection: " + e.getMessage());
                    }

                    // mock evaluate to return 1 qualified item for surfaceA only
                    Map<Surface, List<PropositionItem>> evaluateResult = new HashMap<>();
                    evaluateResult.put(
                            surfaceA, MessagingTestUtils.createMessagingPropositionItemList(1));
                    when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                            .thenReturn(evaluateResult);

                    Event testEvent = mock(Event.class);
                    List<Surface> requestedSurfaces = new ArrayList<>();
                    requestedSurfaces.add(surfaceA);
                    requestedSurfaces.add(surfaceB);

                    // test
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify surfaceA has new proposition (replaced, not merged)
                    Map<Surface, List<Proposition>> result =
                            edgePersonalizationResponseHandler.getQualifiedContentCardsBySurface();
                    assertEquals(1, result.size());

                    List<Proposition> resultA = result.get(surfaceA);
                    assertNotNull(resultA);
                    assertEquals(1, resultA.size());
                    assertEquals("newPropositionId", resultA.get(0).getUniqueId());

                    // verify surfaceB is evicted
                    assertNull(result.get(surfaceB));
                });
    }

    // ========================================================================================
    // addOrReplaceContentCards (incremental — should NOT evict)
    // ========================================================================================
    @Test
    public void test_addOrReplaceContentCards_DoesNotEvictSurfaces_WhenNoQualifiedCards() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: pre-populate contentCardsBySurface with two surfaces
                    Surface surfaceA = new Surface("feed1");
                    Surface surfaceB = new Surface("feed2");

                    MessageTestConfig configA = new MessageTestConfig();
                    configA.count = 2;
                    List<Proposition> propositionsA =
                            MessagingTestUtils.generateQualifiedContentCards(configA);
                    MessageTestConfig configB = new MessageTestConfig();
                    configB.count = 1;
                    List<Proposition> propositionsB =
                            MessagingTestUtils.generateQualifiedContentCards(configB);

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, propositionsA);
                    contentCards.put(surfaceB, propositionsB);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);
                    assertEquals(
                            2,
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .size());

                    // mock evaluate to return null (no qualified content cards)
                    when(mockContentCardRulesEngine.evaluate(any(Event.class))).thenReturn(null);

                    Event testEvent = mock(Event.class);

                    // test
                    edgePersonalizationResponseHandler.addOrReplaceContentCards(testEvent);

                    // verify all surfaces are preserved (additive behavior does not evict)
                    Map<Surface, List<Proposition>> result =
                            edgePersonalizationResponseHandler.getQualifiedContentCardsBySurface();
                    assertEquals(2, result.size());
                    assertEquals(propositionsA, result.get(surfaceA));
                    assertEquals(propositionsB, result.get(surfaceB));
                });
    }

    // ========================================================================================
    // removeOrReplaceContentCards — ContentCardMapper cleanup
    // ========================================================================================
    @Test
    public void
            test_removeOrReplaceContentCards_ClearsContentCardMapperEntries_WhenSurfaceEvicted() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surfaceA = new Surface("feed1");

                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    List<Proposition> propositionsA =
                            MessagingTestUtils.generateQualifiedContentCards(config);
                    String activityId = propositionsA.get(0).getActivityId();

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, propositionsA);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);

                    // pre-populate ContentCardMapper with an entry keyed by activityId
                    seedContentCardMapper(activityId);
                    assertNotNull(
                            ContentCardMapper.getInstance().getContentCardSchemaData(activityId));

                    when(mockContentCardRulesEngine.evaluate(any(Event.class))).thenReturn(null);

                    Event testEvent = mock(Event.class);
                    List<Surface> requestedSurfaces = new ArrayList<>();
                    requestedSurfaces.add(surfaceA);

                    // test
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify surface evicted from cache
                    assertNull(
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .get(surfaceA));
                    // verify ContentCardMapper entry was removed
                    assertNull(
                            ContentCardMapper.getInstance().getContentCardSchemaData(activityId));
                });
    }

    @Test
    public void
            test_removeOrReplaceContentCards_RemovesOldAndStoresNewContentCardMapperEntries_OnReplacement() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surfaceA = new Surface("feed1");

                    MessageTestConfig configOld = new MessageTestConfig();
                    configOld.count = 1;
                    List<Proposition> oldPropositions =
                            MessagingTestUtils.generateQualifiedContentCards(configOld);
                    String oldActivityId = oldPropositions.get(0).getActivityId();

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, oldPropositions);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);

                    // pre-populate ContentCardMapper with an entry keyed by activityId
                    seedContentCardMapper(oldActivityId);
                    assertNotNull(
                            ContentCardMapper.getInstance()
                                    .getContentCardSchemaData(oldActivityId));

                    // set up propositionInfo so getPropositionsFromContentCardRulesEngine can
                    // reconstruct propositions from evaluate() results
                    String newPropositionId = "newPropositionId";
                    String newActivityId = "newActivityId";
                    try {
                        Map<String, Object> activity = new HashMap<>();
                        activity.put("id", newActivityId);
                        Map<String, Object> scopeDetails = new HashMap<>();
                        scopeDetails.put("activity", activity);
                        scopeDetails.put("correlationID", "testCorrelationId");

                        Map<String, Object> infoMap = new HashMap<>();
                        infoMap.put("id", newPropositionId);
                        infoMap.put("scope", surfaceA.getUri());
                        infoMap.put("scopeDetails", scopeDetails);
                        PropositionInfo info = PropositionInfo.create(infoMap);

                        java.lang.reflect.Field propositionInfoField =
                                EdgePersonalizationResponseHandler.class.getDeclaredField(
                                        "propositionInfo");
                        propositionInfoField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<String, PropositionInfo> propositionInfoMap =
                                (Map<String, PropositionInfo>)
                                        propositionInfoField.get(
                                                edgePersonalizationResponseHandler);
                        propositionInfoMap.put("183639c4-cb37-458e-a8ef-4e130d767ebf", info);
                    } catch (Exception e) {
                        fail("Failed to set propositionInfo via reflection: " + e.getMessage());
                    }

                    Map<Surface, List<PropositionItem>> evaluateResult = new HashMap<>();
                    evaluateResult.put(
                            surfaceA, MessagingTestUtils.createMessagingPropositionItemList(1));
                    when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                            .thenReturn(evaluateResult);

                    Event testEvent = mock(Event.class);
                    List<Surface> requestedSurfaces = new ArrayList<>();
                    requestedSurfaces.add(surfaceA);

                    // test
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify old proposition's ContentCardMapper entry was removed
                    assertNull(
                            ContentCardMapper.getInstance()
                                    .getContentCardSchemaData(oldActivityId));
                    // verify new proposition is stored in ContentCardMapper (keyed by activityId)
                    assertNotNull(
                            ContentCardMapper.getInstance()
                                    .getContentCardSchemaData(newActivityId));
                });
    }

    // ========================================================================================
    // removeOrReplaceContentCards — edge cases
    // ========================================================================================
    @Test
    public void test_removeOrReplaceContentCards_NoOp_WhenRequestedSurfacesEmpty() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surfaceA = new Surface("feed1");

                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    List<Proposition> propositionsA =
                            MessagingTestUtils.generateQualifiedContentCards(config);

                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surfaceA, propositionsA);
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);

                    when(mockContentCardRulesEngine.evaluate(any(Event.class))).thenReturn(null);

                    Event testEvent = mock(Event.class);
                    List<Surface> requestedSurfaces = new ArrayList<>();

                    // test with empty requested surfaces
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify cache is unchanged
                    Map<Surface, List<Proposition>> result =
                            edgePersonalizationResponseHandler.getQualifiedContentCardsBySurface();
                    assertEquals(1, result.size());
                    assertNotNull(result.get(surfaceA));
                });
    }

    @Test
    public void test_removeOrReplaceContentCards_NoError_WhenRequestedSurfaceNeverCached() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surfaceA = new Surface("feed1");

                    when(mockContentCardRulesEngine.evaluate(any(Event.class))).thenReturn(null);

                    Event testEvent = mock(Event.class);
                    List<Surface> requestedSurfaces = new ArrayList<>();
                    requestedSurfaces.add(surfaceA);

                    // test — surfaceA was never in the cache
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            testEvent, requestedSurfaces);

                    // verify cache remains empty, no exceptions thrown
                    Map<Surface, List<Proposition>> result =
                            edgePersonalizationResponseHandler.getQualifiedContentCardsBySurface();
                    assertEquals(0, result.size());
                });
    }

    // ========================================================================================
    // Fix #1: enrichWithContentCardOrigin — XDM enrichment per-item
    // ========================================================================================
    @Test
    public void test_enrichWithContentCardOrigin_DiskOrigin_SetsServedFromPersistentCachePerItem() {
        runUsingMockedServiceProvider(
                () -> {
                    // seed a DISK-origin proposition into contentCardsBySurface
                    String propositionId = "prop-disk-1";
                    Map<String, Object> scopeDetails = new HashMap<>();
                    scopeDetails.put("decisionProvider", "AJO");
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "activityDisk1");
                    scopeDetails.put("activity", activity);
                    Map<String, Object> propData = new HashMap<>();
                    propData.put("id", propositionId);
                    propData.put("scope", "mobileapp://mockPackageName/apifeed");
                    propData.put("scopeDetails", scopeDetails);
                    propData.put("items", new ArrayList<>());
                    Proposition diskProp = Proposition.fromEventData(propData);
                    Surface ccSurface = new Surface("apifeed");
                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(ccSurface, Collections.singletonList(diskProp));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);

                    // build XDM matching the structure:
                    // _experience.decisioning.propositionEventType.display = 1
                    // _experience.decisioning.propositions[].items[].data.characteristics
                    Map<String, Object> item1 = new HashMap<>();
                    item1.put("id", "item-1");
                    Map<String, Object> item2 = new HashMap<>();
                    item2.put("id", "item-2");
                    List<Map<String, Object>> items = new ArrayList<>();
                    items.add(item1);
                    items.add(item2);

                    Map<String, Object> propositionMap = new HashMap<>();
                    propositionMap.put("id", propositionId);
                    propositionMap.put("items", items);
                    List<Map<String, Object>> propositions = new ArrayList<>();
                    propositions.add(propositionMap);

                    Map<String, Object> propositionEventType = new HashMap<>();
                    propositionEventType.put("display", 1);

                    Map<String, Object> decisioning = new HashMap<>();
                    decisioning.put("propositionEventType", propositionEventType);
                    decisioning.put("propositions", propositions);

                    Map<String, Object> experience = new HashMap<>();
                    experience.put("decisioning", decisioning);

                    Map<String, Object> xdm = new HashMap<>();
                    xdm.put("_experience", experience);

                    // test
                    Map<String, Object> enriched =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(xdm);

                    // verify each item has servedFromPersistentCache = true at
                    // items[].data.characteristics.servedFromPersistentCache
                    List<Map<String, Object>> enrichedPropositions =
                            (List<Map<String, Object>>)
                                    ((Map<String, Object>)
                                                    ((Map<String, Object>)
                                                                    enriched.get("_experience"))
                                                            .get("decisioning"))
                                            .get("propositions");
                    for (Map<String, Object> prop : enrichedPropositions) {
                        List<Map<String, Object>> enrichedItems =
                                (List<Map<String, Object>>) prop.get("items");
                        for (Map<String, Object> enrichedItem : enrichedItems) {
                            Map<String, Object> data =
                                    (Map<String, Object>) enrichedItem.get("data");
                            assertNotNull("item should have data after enrichment", data);
                            Map<String, Object> characteristics =
                                    (Map<String, Object>) data.get("characteristics");
                            assertNotNull(
                                    "item should have characteristics after enrichment",
                                    characteristics);
                            assertTrue(
                                    "servedFromPersistentCache should be true for DISK origin",
                                    (boolean) characteristics.get("servedFromPersistentCache"));
                        }
                    }
                });
    }

    @Test
    public void test_enrichWithContentCardOrigin_NetworkOrigin_DoesNotSetFlag() {
        runUsingMockedServiceProvider(
                () -> {
                    // proposition not in contentCardsBySurface → defaults to NETWORK origin
                    String propositionId = "prop-network-1";

                    // build XDM with DISPLAY event type
                    Map<String, Object> item1 = new HashMap<>();
                    item1.put("id", "item-1");
                    List<Map<String, Object>> items = new ArrayList<>();
                    items.add(item1);

                    Map<String, Object> propositionMap = new HashMap<>();
                    propositionMap.put("id", propositionId);
                    propositionMap.put("items", items);
                    List<Map<String, Object>> propositions = new ArrayList<>();
                    propositions.add(propositionMap);

                    Map<String, Object> propositionEventType = new HashMap<>();
                    propositionEventType.put("display", 1);

                    Map<String, Object> decisioning = new HashMap<>();
                    decisioning.put("propositionEventType", propositionEventType);
                    decisioning.put("propositions", propositions);

                    Map<String, Object> experience = new HashMap<>();
                    experience.put("decisioning", decisioning);

                    Map<String, Object> xdm = new HashMap<>();
                    xdm.put("_experience", experience);

                    // test
                    Map<String, Object> enriched =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(xdm);

                    // verify item does NOT have servedFromPersistentCache
                    List<Map<String, Object>> enrichedPropositions =
                            (List<Map<String, Object>>)
                                    ((Map<String, Object>)
                                                    ((Map<String, Object>)
                                                                    enriched.get("_experience"))
                                                            .get("decisioning"))
                                            .get("propositions");
                    Map<String, Object> prop = enrichedPropositions.get(0);
                    List<Map<String, Object>> enrichedItems =
                            (List<Map<String, Object>>) prop.get("items");
                    Map<String, Object> enrichedItem = enrichedItems.get(0);
                    // item should not have data.characteristics.servedFromPersistentCache
                    assertNull(
                            "NETWORK origin items should not be enriched",
                            enrichedItem.get("data"));
                });
    }

    // ========================================================================================
    // Fix #2: enrichWithContentCardOrigin — Only DISPLAY events enriched
    // ========================================================================================
    @Test
    public void test_enrichWithContentCardOrigin_InteractEvent_NotEnriched() {
        runUsingMockedServiceProvider(
                () -> {
                    // seed a DISK-origin proposition into contentCardsBySurface
                    String propositionId = "prop-disk-interact";
                    Map<String, Object> scopeDetailsI = new HashMap<>();
                    scopeDetailsI.put("decisionProvider", "AJO");
                    Map<String, Object> activityI = new HashMap<>();
                    activityI.put("id", "activityInteract");
                    scopeDetailsI.put("activity", activityI);
                    Map<String, Object> propDataI = new HashMap<>();
                    propDataI.put("id", propositionId);
                    propDataI.put("scope", "mobileapp://mockPackageName/apifeed");
                    propDataI.put("scopeDetails", scopeDetailsI);
                    propDataI.put("items", new ArrayList<>());
                    Proposition diskPropI = Proposition.fromEventData(propDataI);
                    Surface ccSurfaceI = new Surface("apifeed");
                    Map<Surface, List<Proposition>> contentCardsI = new HashMap<>();
                    contentCardsI.put(ccSurfaceI, Collections.singletonList(diskPropI));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCardsI);

                    // build XDM with INTERACT event type (not DISPLAY)
                    Map<String, Object> item1 = new HashMap<>();
                    item1.put("id", "item-1");
                    List<Map<String, Object>> items = new ArrayList<>();
                    items.add(item1);

                    Map<String, Object> propositionMap = new HashMap<>();
                    propositionMap.put("id", propositionId);
                    propositionMap.put("items", items);
                    List<Map<String, Object>> propositions = new ArrayList<>();
                    propositions.add(propositionMap);

                    Map<String, Object> propositionEventType = new HashMap<>();
                    propositionEventType.put("interact", 1);

                    Map<String, Object> decisioning = new HashMap<>();
                    decisioning.put("propositionEventType", propositionEventType);
                    decisioning.put("propositions", propositions);

                    Map<String, Object> experience = new HashMap<>();
                    experience.put("decisioning", decisioning);

                    Map<String, Object> xdm = new HashMap<>();
                    xdm.put("_experience", experience);

                    // test
                    Map<String, Object> enriched =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(xdm);

                    // verify item is NOT enriched for interact event
                    List<Map<String, Object>> enrichedPropositions =
                            (List<Map<String, Object>>)
                                    ((Map<String, Object>)
                                                    ((Map<String, Object>)
                                                                    enriched.get("_experience"))
                                                            .get("decisioning"))
                                            .get("propositions");
                    Map<String, Object> prop = enrichedPropositions.get(0);
                    List<Map<String, Object>> enrichedItems =
                            (List<Map<String, Object>>) prop.get("items");
                    assertNull(
                            "INTERACT events should not enrich items",
                            enrichedItems.get(0).get("data"));
                });
    }

    @Test
    public void test_enrichWithContentCardOrigin_DismissEvent_NotEnriched() {
        runUsingMockedServiceProvider(
                () -> {
                    // seed a DISK-origin proposition into contentCardsBySurface
                    String propositionId = "prop-disk-dismiss";
                    Map<String, Object> scopeDetailsD = new HashMap<>();
                    scopeDetailsD.put("decisionProvider", "AJO");
                    Map<String, Object> activityD = new HashMap<>();
                    activityD.put("id", "activityDismiss");
                    scopeDetailsD.put("activity", activityD);
                    Map<String, Object> propDataD = new HashMap<>();
                    propDataD.put("id", propositionId);
                    propDataD.put("scope", "mobileapp://mockPackageName/apifeed");
                    propDataD.put("scopeDetails", scopeDetailsD);
                    propDataD.put("items", new ArrayList<>());
                    Proposition diskPropD = Proposition.fromEventData(propDataD);
                    Surface ccSurfaceD = new Surface("apifeed");
                    Map<Surface, List<Proposition>> contentCardsD = new HashMap<>();
                    contentCardsD.put(ccSurfaceD, Collections.singletonList(diskPropD));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCardsD);

                    Map<String, Object> item1 = new HashMap<>();
                    item1.put("id", "item-1");
                    List<Map<String, Object>> items = new ArrayList<>();
                    items.add(item1);

                    Map<String, Object> propositionMap = new HashMap<>();
                    propositionMap.put("id", propositionId);
                    propositionMap.put("items", items);
                    List<Map<String, Object>> propositions = new ArrayList<>();
                    propositions.add(propositionMap);

                    Map<String, Object> propositionEventType = new HashMap<>();
                    propositionEventType.put("dismiss", 1);

                    Map<String, Object> decisioning = new HashMap<>();
                    decisioning.put("propositionEventType", propositionEventType);
                    decisioning.put("propositions", propositions);

                    Map<String, Object> experience = new HashMap<>();
                    experience.put("decisioning", decisioning);

                    Map<String, Object> xdm = new HashMap<>();
                    xdm.put("_experience", experience);

                    Map<String, Object> enriched =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(xdm);

                    List<Map<String, Object>> enrichedPropositions =
                            (List<Map<String, Object>>)
                                    ((Map<String, Object>)
                                                    ((Map<String, Object>)
                                                                    enriched.get("_experience"))
                                                            .get("decisioning"))
                                            .get("propositions");
                    assertNull(
                            "DISMISS events should not enrich items",
                            enrichedPropositions.get(0).get("items").getClass().equals(List.class)
                                    ? ((List<Map<String, Object>>)
                                                    enrichedPropositions.get(0).get("items"))
                                            .get(0)
                                            .get("data")
                                    : null);
                });
    }

    @Test
    public void test_enrichWithContentCardOrigin_NullXdm_ReturnsNull() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> result =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(null);
                    assertNull("null XDM should return null", result);
                });
    }

    @Test
    public void test_enrichWithContentCardOrigin_EmptyXdm_ReturnsSameMap() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> empty = new HashMap<>();
                    Map<String, Object> result =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(empty);
                    assertEquals("empty XDM should return same empty map", empty, result);
                });
    }

    // ========================================================================================
    // Fix #3: clearContentCards — Full wipe (in-memory + disk)
    // ========================================================================================
    @Test
    public void test_clearContentCards_ClearsInMemoryStateAndDiskCache() {
        runUsingMockedServiceProvider(
                () -> {
                    // seed in-memory state with a DISK-origin proposition
                    Surface surface = new Surface("testFeed");
                    Map<String, Object> scopeDetailsClear = new HashMap<>();
                    scopeDetailsClear.put("decisionProvider", "AJO");
                    Map<String, Object> activityClear = new HashMap<>();
                    activityClear.put("id", "activityClear");
                    scopeDetailsClear.put("activity", activityClear);
                    Map<String, Object> propDataClear = new HashMap<>();
                    propDataClear.put("id", "prop-1");
                    propDataClear.put("scope", "mobileapp://mockPackageName/testFeed");
                    propDataClear.put("scopeDetails", scopeDetailsClear);
                    propDataClear.put("items", new ArrayList<>());
                    Proposition diskPropClear = Proposition.fromEventData(propDataClear);
                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(surface, Collections.singletonList(diskPropClear));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);

                    // test
                    edgePersonalizationResponseHandler.clearContentCards();

                    // verify in-memory state cleared
                    assertTrue(
                            "contentCardsBySurface should be empty after clearContentCards",
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .isEmpty());

                    // verify content card rules engine cleared
                    verify(mockContentCardRulesEngine, times(1)).replaceRules(anyList());

                    // verify disk cache cleared
                    verify(mockMessagingCacheUtilities, times(1)).clearPersistedContentCardCache();
                });
    }

    // ========================================================================================
    // Fix #4: Completion handler returns false on non-recoverable edge error
    // ========================================================================================
    @Test
    public void
            test_handleProcessCompletedEvent_NonRecoverableError_CompletionHandlerCalledWithFalse() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // setup: register a surface request
                        Surface surface = new Surface("testFeed");
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "ERROR_EVENT_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });

                        // mock completion handler lookup for edge request event id
                        CompletionHandler errorHandler =
                                new CompletionHandler("mockParentId", mockAdobeCallback);
                        errorHandler.edgeRequestEventId = "ERROR_EVENT_ID";
                        when(mockMessagingExtension.completionHandlerForEdgeRequestEventId(
                                        "ERROR_EVENT_ID"))
                                .thenReturn(errorHandler);

                        // simulate a non-recoverable edge error (status 500)
                        Map<String, Object> errorEventData = new HashMap<>();
                        errorEventData.put("requestEventId", "ERROR_EVENT_ID");
                        errorEventData.put("status", 500);
                        Event errorEvent = mock(Event.class);
                        when(errorEvent.getEventData()).thenReturn(errorEventData);
                        edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                        // verify event id is in non-recoverable set
                        assertTrue(
                                "event ID should be in nonRecoverableErrorEventIds",
                                edgePersonalizationResponseHandler
                                        .getNonRecoverableErrorEventIds()
                                        .contains("ERROR_EVENT_ID"));

                        // setup process completed event
                        Map<String, Object> completedEventData = new HashMap<>();
                        completedEventData.put(ENDING_EVENT_ID, "ERROR_EVENT_ID");
                        Event completedEvent = mock(Event.class);
                        when(completedEvent.getEventData()).thenReturn(completedEventData);

                        // test
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(
                                completedEvent);

                        // verify completion handler called with false
                        verify(mockAdobeCallback, times(1)).call(false);

                        // verify non-recoverable event id is cleaned up
                        assertFalse(
                                "event ID should be removed from nonRecoverableErrorEventIds"
                                        + " after processing",
                                edgePersonalizationResponseHandler
                                        .getNonRecoverableErrorEventIds()
                                        .contains("ERROR_EVENT_ID"));
                    }
                });
    }

    @Test
    public void
            test_handleProcessCompletedEvent_RecoverableError_CompletionHandlerCalledWithTrue() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // setup: register a surface request
                        Surface surface = new Surface("testFeed");
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "RECOVERABLE_EVENT_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });

                        // mock completion handler lookup for edge request event id
                        CompletionHandler recoverableHandler =
                                new CompletionHandler("mockParentId", mockAdobeCallback);
                        recoverableHandler.edgeRequestEventId = "RECOVERABLE_EVENT_ID";
                        when(mockMessagingExtension.completionHandlerForEdgeRequestEventId(
                                        "RECOVERABLE_EVENT_ID"))
                                .thenReturn(recoverableHandler);

                        // simulate a recoverable edge error (status 503)
                        Map<String, Object> errorEventData = new HashMap<>();
                        errorEventData.put("requestEventId", "RECOVERABLE_EVENT_ID");
                        errorEventData.put("status", 503);
                        Event errorEvent = mock(Event.class);
                        when(errorEvent.getEventData()).thenReturn(errorEventData);
                        edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                        // verify event id is NOT in non-recoverable set
                        assertFalse(
                                "recoverable status should not add to"
                                        + " nonRecoverableErrorEventIds",
                                edgePersonalizationResponseHandler
                                        .getNonRecoverableErrorEventIds()
                                        .contains("RECOVERABLE_EVENT_ID"));

                        // setup process completed event
                        Map<String, Object> completedEventData = new HashMap<>();
                        completedEventData.put(ENDING_EVENT_ID, "RECOVERABLE_EVENT_ID");
                        Event completedEvent = mock(Event.class);
                        when(completedEvent.getEventData()).thenReturn(completedEventData);

                        // test
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(
                                completedEvent);

                        // verify completion handler called with true
                        verify(mockAdobeCallback, times(1)).call(true);
                    }
                });
    }

    // ========================================================================================
    // Fix #5: handleEdgeErrorResponse — Missing/zero status → non-recoverable
    // ========================================================================================
    @Test
    public void test_handleEdgeErrorResponse_MissingStatus_TreatedAsNonRecoverable() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: register a surface request
                    Surface surface = new Surface("testFeed");
                    edgePersonalizationResponseHandler.setMessagesRequestEventId(
                            "MISSING_STATUS_ID",
                            new ArrayList<Surface>() {
                                {
                                    add(surface);
                                }
                            });

                    // simulate edge error with NO status field
                    Map<String, Object> errorEventData = new HashMap<>();
                    errorEventData.put("requestEventId", "MISSING_STATUS_ID");
                    // deliberately no "status" key
                    Event errorEvent = mock(Event.class);
                    when(errorEvent.getEventData()).thenReturn(errorEventData);

                    // test
                    edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                    // verify event id is in non-recoverable set (missing = 0 = non-recoverable)
                    assertTrue(
                            "missing status should be treated as non-recoverable",
                            edgePersonalizationResponseHandler
                                    .getNonRecoverableErrorEventIds()
                                    .contains("MISSING_STATUS_ID"));
                });
    }

    @Test
    public void test_handleEdgeErrorResponse_ZeroStatus_TreatedAsNonRecoverable() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surface = new Surface("testFeed");
                    edgePersonalizationResponseHandler.setMessagesRequestEventId(
                            "ZERO_STATUS_ID",
                            new ArrayList<Surface>() {
                                {
                                    add(surface);
                                }
                            });

                    Map<String, Object> errorEventData = new HashMap<>();
                    errorEventData.put("requestEventId", "ZERO_STATUS_ID");
                    errorEventData.put("status", 0);
                    Event errorEvent = mock(Event.class);
                    when(errorEvent.getEventData()).thenReturn(errorEventData);

                    edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                    assertTrue(
                            "zero status should be treated as non-recoverable",
                            edgePersonalizationResponseHandler
                                    .getNonRecoverableErrorEventIds()
                                    .contains("ZERO_STATUS_ID"));
                });
    }

    @Test
    public void test_handleEdgeErrorResponse_RecoverableStatuses_NotMarkedNonRecoverable() {
        runUsingMockedServiceProvider(
                () -> {
                    int[] recoverableStatuses = {408, 429, 502, 503, 504, 507};
                    for (int status : recoverableStatuses) {
                        String eventId = "RECOVERABLE_" + status;
                        Surface surface = new Surface("testFeed");
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                eventId,
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });

                        Map<String, Object> errorEventData = new HashMap<>();
                        errorEventData.put("requestEventId", eventId);
                        errorEventData.put("status", status);
                        Event errorEvent = mock(Event.class);
                        when(errorEvent.getEventData()).thenReturn(errorEventData);

                        edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                        assertFalse(
                                "status " + status + " should be recoverable",
                                edgePersonalizationResponseHandler
                                        .getNonRecoverableErrorEventIds()
                                        .contains(eventId));
                    }
                });
    }

    @Test
    public void test_handleEdgeErrorResponse_NonRecoverableStatuses_MarkedNonRecoverable() {
        runUsingMockedServiceProvider(
                () -> {
                    int[] nonRecoverableStatuses = {400, 401, 403, 404, 500};
                    for (int status : nonRecoverableStatuses) {
                        String eventId = "NON_RECOVERABLE_" + status;
                        Surface surface = new Surface("testFeed");
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                eventId,
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });

                        Map<String, Object> errorEventData = new HashMap<>();
                        errorEventData.put("requestEventId", eventId);
                        errorEventData.put("status", status);
                        Event errorEvent = mock(Event.class);
                        when(errorEvent.getEventData()).thenReturn(errorEventData);

                        edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                        assertTrue(
                                "status " + status + " should be non-recoverable",
                                edgePersonalizationResponseHandler
                                        .getNonRecoverableErrorEventIds()
                                        .contains(eventId));
                    }
                });
    }

    @Test
    public void test_handleEdgeErrorResponse_UnknownRequestId_DoesNothing() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> errorEventData = new HashMap<>();
                    errorEventData.put("requestEventId", "UNKNOWN_EVENT_ID");
                    errorEventData.put("status", 500);
                    Event errorEvent = mock(Event.class);
                    when(errorEvent.getEventData()).thenReturn(errorEventData);

                    edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                    assertTrue(
                            "unknown request ID should not add to nonRecoverableErrorEventIds",
                            edgePersonalizationResponseHandler
                                    .getNonRecoverableErrorEventIds()
                                    .isEmpty());
                });
    }

    // ========================================================================================
    // Fix #6: isContentCardOfflineAvailable — Config-driven flag
    // ========================================================================================
    @Test
    public void test_isContentCardOfflineAvailable_DefaultsFalse_WhenNoConfig() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: return null shared state (no config)
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(null);

                    assertFalse(
                            "should default to false when config is unavailable",
                            edgePersonalizationResponseHandler.isContentCardOfflineAvailable());
                });
    }

    @Test
    public void test_isContentCardOfflineAvailable_DefaultsFalse_WhenConfigValueMissing() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup: config shared state exists but doesn't have the key
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("some.other.key", "value");
                    SharedStateResult result =
                            new SharedStateResult(SharedStateStatus.SET, configState);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(result);

                    assertFalse(
                            "should default to false when config key is missing",
                            edgePersonalizationResponseHandler.isContentCardOfflineAvailable());
                });
    }

    @Test
    public void test_isContentCardOfflineAvailable_ReturnsFalse_WhenConfigSetToFalse() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", false);
                    SharedStateResult result =
                            new SharedStateResult(SharedStateStatus.SET, configState);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(result);

                    assertFalse(
                            "should return false when config explicitly disables it",
                            edgePersonalizationResponseHandler.isContentCardOfflineAvailable());
                });
    }

    @Test
    public void test_isContentCardOfflineAvailable_ReturnsTrue_WhenConfigSetToTrue() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", true);
                    SharedStateResult result =
                            new SharedStateResult(SharedStateStatus.SET, configState);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(result);

                    assertTrue(
                            "should return true when config explicitly enables it",
                            edgePersonalizationResponseHandler.isContentCardOfflineAvailable());
                });
    }

    @Test
    public void test_isContentCardOfflineAvailable_DefaultsFalse_WhenExceptionThrown() {
        runUsingMockedServiceProvider(
                () -> {
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenThrow(new RuntimeException("test exception"));

                    assertFalse(
                            "should default to false when exception occurs",
                            edgePersonalizationResponseHandler.isContentCardOfflineAvailable());
                });
    }

    // ========================================================================================
    // isInternetAvailable — network gating for user-triggered fetches
    // ========================================================================================
    @Test
    public void test_isInternetAvailable_returnsFalse_whenNetworkUtilsReportsNoInternet() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<NetworkUtils> networkUtils =
                            Mockito.mockStatic(NetworkUtils.class)) {
                        AppContextService mockAppContextService = mock(AppContextService.class);
                        Context mockContext = mock(Context.class);
                        ConnectivityManager mockConnectivityManager =
                                mock(ConnectivityManager.class);
                        when(mockServiceProvider.getAppContextService())
                                .thenReturn(mockAppContextService);
                        when(mockAppContextService.getApplicationContext()).thenReturn(mockContext);
                        when(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                                .thenReturn(mockConnectivityManager);
                        networkUtils
                                .when(
                                        () ->
                                                NetworkUtils.isInternetAvailable(
                                                        mockConnectivityManager))
                                .thenReturn(false);

                        assertFalse(
                                "should report no internet when NetworkUtils says so",
                                edgePersonalizationResponseHandler.isInternetAvailable());
                    }
                });
    }

    @Test
    public void test_isInternetAvailable_failsOpen_whenApplicationContextUnavailable() {
        runUsingMockedServiceProvider(
                () -> {
                    // no app context service available — should fail open (assume available)
                    when(mockServiceProvider.getAppContextService()).thenReturn(null);

                    assertTrue(
                            "should fail open (return true) when connectivity is indeterminate",
                            edgePersonalizationResponseHandler.isInternetAvailable());
                });
    }

    // ========================================================================================
    // Fix #7: hydrateContentCardRulesEngineFromDisk — Event-history rules at boot
    // ========================================================================================
    @Test
    public void test_hydrateContentCardRulesEngineFromDisk_LoadsContentCardAndEventHistoryRules() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        Surface contentCardSurface = new Surface("apifeed");

                        // build cached content card propositions
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 2;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateContentCardPayload(config);

                        Map<Surface, List<Proposition>> cachedContentCards = new HashMap<>();
                        List<Proposition> propositions = new ArrayList<>();
                        for (Map<String, Object> p : payload) {
                            propositions.add(Proposition.fromEventData(p));
                        }
                        cachedContentCards.put(contentCardSurface, propositions);

                        when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                                .thenReturn(cachedContentCards);

                        // test
                        edgePersonalizationResponseHandler.hydrateContentCardRulesEngineFromDisk();

                        // verify content card rules engine was loaded
                        verify(mockContentCardRulesEngine, times(1))
                                .replaceRules(contentCardRulesListCaptor.capture());
                        assertFalse(
                                "content card rules should not be empty",
                                contentCardRulesListCaptor.getValue().isEmpty());

                        // verify main rules engine (IAM + event-history) was loaded
                        verify(mockMessagingRulesEngine, times(1))
                                .replaceRules(rulesListCaptor.capture());

                        // verify the hydrated surface was NOT marked network-refreshed, so its
                        // cards report servedFromPersistentCache = true until a live network
                        // refresh
                        assertFalse(
                                "disk-hydrated surface must not be in networkRefreshedSurfaces",
                                edgePersonalizationResponseHandler
                                        .getNetworkRefreshedSurfaces()
                                        .contains(contentCardSurface));
                    }
                });
    }

    @Test
    public void test_hydrateContentCardRulesEngineFromDisk_NoCachedCards_DoesNothing() {
        runUsingMockedServiceProvider(
                () -> {
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(null);

                    edgePersonalizationResponseHandler.hydrateContentCardRulesEngineFromDisk();

                    // verify no rules engine changes and qualified cache remains empty
                    verifyNoInteractions(mockContentCardRulesEngine);
                    assertTrue(
                            "qualifiedContentCardsBySurface should remain empty",
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .isEmpty());
                });
    }

    @Test
    public void test_hydrateContentCardRulesEngineFromDisk_EmptyCachedCards_DoesNothing() {
        runUsingMockedServiceProvider(
                () -> {
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(new HashMap<>());

                    edgePersonalizationResponseHandler.hydrateContentCardRulesEngineFromDisk();

                    // verify no rules engine changes
                    verifyNoInteractions(mockContentCardRulesEngine);
                });
    }

    @Test
    public void test_hydrateContentCardRulesEngineFromDisk_runsWhenFlagOff() {
        runUsingMockedServiceProvider(
                () -> {
                    // disable offline availability via config
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", false);
                    SharedStateResult configResult =
                            new SharedStateResult(SharedStateStatus.SET, configState);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(configResult);
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(null);

                    // hydration is now ungated — it should run even when the flag is off
                    edgePersonalizationResponseHandler.hydrateContentCardRulesEngineFromDisk();

                    // verify cache WAS read (no early return based on flag)
                    verify(mockMessagingCacheUtilities, times(1))
                            .getCachedContentCardPropositions();
                });
    }

    // ========================================================================================
    // hydrateContentCardRulesEngineFromDisk
    // ========================================================================================
    @Test
    public void test_hydrateContentCardRulesEngineFromDisk_readsContentCardCache() {
        runUsingMockedServiceProvider(
                () -> {
                    // setup - no cached data
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(null);

                    // test
                    edgePersonalizationResponseHandler.hydrateContentCardRulesEngineFromDisk();

                    // verify content card cache was read (inbox cache no longer persisted)
                    verify(mockMessagingCacheUtilities, times(1))
                            .getCachedContentCardPropositions();
                });
    }

    // ========================================================================================
    // New tests — networkRefreshedSurfaces provenance
    // ========================================================================================

    @Test
    public void
            test_enrichWithContentCardOrigin_NetworkRefreshedSurface_SetsServedFromCacheFalse() {
        runUsingMockedServiceProvider(
                () -> {
                    // seed a qualified content card and mark its surface network-refreshed
                    String propositionId = "prop-network-refreshed";
                    Map<String, Object> scopeDetails = new HashMap<>();
                    scopeDetails.put("decisionProvider", "AJO");
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "activityNetwork1");
                    scopeDetails.put("activity", activity);
                    Map<String, Object> propData = new HashMap<>();
                    propData.put("id", propositionId);
                    propData.put("scope", "mobileapp://mockPackageName/apifeed");
                    propData.put("scopeDetails", scopeDetails);
                    propData.put("items", new ArrayList<>());
                    Proposition prop = Proposition.fromEventData(propData);
                    Surface ccSurface = new Surface("apifeed");
                    Map<Surface, List<Proposition>> contentCards = new HashMap<>();
                    contentCards.put(ccSurface, Collections.singletonList(prop));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(
                            contentCards);
                    // mark the surface as refreshed from a live network response this session
                    edgePersonalizationResponseHandler.getNetworkRefreshedSurfaces().add(ccSurface);

                    // build DISPLAY-event XDM containing the proposition with one item
                    Map<String, Object> item1 = new HashMap<>();
                    item1.put("id", "item-1");
                    List<Map<String, Object>> items = new ArrayList<>();
                    items.add(item1);
                    Map<String, Object> propositionMap = new HashMap<>();
                    propositionMap.put("id", propositionId);
                    propositionMap.put("items", items);
                    List<Map<String, Object>> propositions = new ArrayList<>();
                    propositions.add(propositionMap);
                    Map<String, Object> propositionEventType = new HashMap<>();
                    propositionEventType.put("display", 1);
                    Map<String, Object> decisioning = new HashMap<>();
                    decisioning.put("propositionEventType", propositionEventType);
                    decisioning.put("propositions", propositions);
                    Map<String, Object> experience = new HashMap<>();
                    experience.put("decisioning", decisioning);
                    Map<String, Object> xdm = new HashMap<>();
                    xdm.put("_experience", experience);

                    // test
                    Map<String, Object> enriched =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(xdm);

                    // verify servedFromPersistentCache = false for a network-refreshed surface
                    List<Map<String, Object>> enrichedPropositions =
                            (List<Map<String, Object>>)
                                    ((Map<String, Object>)
                                                    ((Map<String, Object>)
                                                                    enriched.get("_experience"))
                                                            .get("decisioning"))
                                            .get("propositions");
                    Map<String, Object> data =
                            (Map<String, Object>)
                                    ((List<Map<String, Object>>)
                                                    enrichedPropositions.get(0).get("items"))
                                            .get(0)
                                            .get("data");
                    assertNotNull("item should have data after enrichment", data);
                    Map<String, Object> characteristics =
                            (Map<String, Object>) data.get("characteristics");
                    assertNotNull(characteristics);
                    assertFalse(
                            "servedFromPersistentCache should be false for a network-refreshed"
                                    + " surface",
                            (boolean) characteristics.get("servedFromPersistentCache"));
                });
    }

    // Recursively wraps maps/lists as unmodifiable to mimic the immutable event data delivered by
    // the Event hub.
    @SuppressWarnings("unchecked")
    private static Object deepUnmodifiable(final Object value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new HashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) value).entrySet()) {
                copy.put(e.getKey(), deepUnmodifiable(e.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        } else if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object element : (List<Object>) value) {
                copy.add(deepUnmodifiable(element));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    @Test
    public void test_enrichWithContentCardOrigin_realDisplayXdm_setsServedFromPersistentCache() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surface = new Surface("apifeed");
                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    List<Map<String, Object>> payload =
                            MessagingTestUtils.generateContentCardPayload(config);
                    Proposition prop = Proposition.fromEventData(payload.get(0));

                    // place the card in the qualified cache and mark its surface network-refreshed
                    Map<Surface, List<Proposition>> cards = new HashMap<>();
                    cards.put(surface, Collections.singletonList(prop));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(cards);
                    edgePersonalizationResponseHandler.getNetworkRefreshedSurfaces().add(surface);

                    // build the DISPLAY interaction XDM exactly the way production does
                    PropositionItem item = prop.getItems().get(0);
                    Map<String, Object> xdm =
                            item.generateInteractionXdm(
                                    com.adobe.marketing.mobile.MessagingEdgeEventType.DISPLAY);
                    assertNotNull("production display XDM should be generated", xdm);

                    // mimic the Event hub, which delivers deeply-immutable event data — enrichment
                    // must not throw when writing into the nested item maps
                    @SuppressWarnings("unchecked")
                    Map<String, Object> immutableXdm = (Map<String, Object>) deepUnmodifiable(xdm);

                    Map<String, Object> enriched =
                            edgePersonalizationResponseHandler.enrichWithContentCardOrigin(
                                    immutableXdm);

                    // dig into
                    // _experience.decisioning.propositions[0].items[0].data.characteristics
                    Map<String, Object> experience =
                            (Map<String, Object>) enriched.get("_experience");
                    Map<String, Object> decisioning =
                            (Map<String, Object>) experience.get("decisioning");
                    List<Map<String, Object>> props =
                            (List<Map<String, Object>>) decisioning.get("propositions");
                    List<Map<String, Object>> items =
                            (List<Map<String, Object>>) props.get(0).get("items");
                    assertNotNull("display XDM must contain items", items);
                    Map<String, Object> data = (Map<String, Object>) items.get(0).get("data");
                    assertNotNull("item should have data after enrichment", data);
                    Map<String, Object> characteristics =
                            (Map<String, Object>) data.get("characteristics");
                    assertNotNull("characteristics should be present", characteristics);
                    assertEquals(
                            "network-refreshed card should report servedFromPersistentCache=false",
                            false,
                            characteristics.get("servedFromPersistentCache"));
                });
    }

    @Test
    public void test_removeOrReplaceContentCards_marksSurfaceNetworkRefreshed() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surface = new Surface("apifeed");

                    // inject propositionInfo so the rules engine output can be reconstructed
                    try {
                        Map<String, Object> infoActivity = new HashMap<>();
                        infoActivity.put("id", "newActivityId");
                        Map<String, Object> infoScopeDetails = new HashMap<>();
                        infoScopeDetails.put("activity", infoActivity);
                        infoScopeDetails.put("correlationID", "testCorrelationId");
                        Map<String, Object> infoMap = new HashMap<>();
                        infoMap.put("id", "newPropositionId");
                        infoMap.put("scope", surface.getUri());
                        infoMap.put("scopeDetails", infoScopeDetails);
                        PropositionInfo info = PropositionInfo.create(infoMap);
                        java.lang.reflect.Field propositionInfoField =
                                EdgePersonalizationResponseHandler.class.getDeclaredField(
                                        "propositionInfo");
                        propositionInfoField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<String, PropositionInfo> propositionInfoMap =
                                (Map<String, PropositionInfo>)
                                        propositionInfoField.get(
                                                edgePersonalizationResponseHandler);
                        propositionInfoMap.put("183639c4-cb37-458e-a8ef-4e130d767ebf", info);
                    } catch (Exception e) {
                        fail("Failed to set propositionInfo via reflection: " + e.getMessage());
                    }

                    // the network response delivered content card rules for the surface
                    try {
                        java.lang.reflect.Field ccRulesField =
                                EdgePersonalizationResponseHandler.class.getDeclaredField(
                                        "contentCardRulesBySurface");
                        ccRulesField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Map<Surface, List<LaunchRule>> ccRules =
                                (Map<Surface, List<LaunchRule>>)
                                        ccRulesField.get(edgePersonalizationResponseHandler);
                        ccRules.put(surface, Collections.singletonList(mock(LaunchRule.class)));
                    } catch (Exception e) {
                        fail(
                                "Failed to set contentCardRulesBySurface via reflection: "
                                        + e.getMessage());
                    }

                    // rules engine returns a qualifying content card for the surface
                    Map<Surface, List<PropositionItem>> evaluateResult = new HashMap<>();
                    evaluateResult.put(
                            surface, MessagingTestUtils.createMessagingPropositionItemList(1));
                    when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                            .thenReturn(evaluateResult);

                    // test — a requested surface whose rules were delivered is network-refreshed
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            mock(Event.class), Collections.singletonList(surface));

                    assertTrue(
                            "surface returning network content cards must be network-refreshed",
                            edgePersonalizationResponseHandler
                                    .getNetworkRefreshedSurfaces()
                                    .contains(surface));
                    assertFalse(
                            "surface should have qualified content cards",
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .get(surface)
                                    .isEmpty());
                });
    }

    @Test
    public void test_removeOrReplaceContentCards_emptyResponseEvictsAndUnmarksSurface() {
        runUsingMockedServiceProvider(
                () -> {
                    Surface surface = new Surface("apifeed");

                    // pre-seed as if previously network-refreshed with a card
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("id", "existingActivity");
                    Map<String, Object> scopeDetails = new HashMap<>();
                    scopeDetails.put("activity", activity);
                    Map<String, Object> propData = new HashMap<>();
                    propData.put("id", "existingProp");
                    propData.put("scope", surface.getUri());
                    propData.put("scopeDetails", scopeDetails);
                    propData.put("items", new ArrayList<>());
                    Proposition existing = Proposition.fromEventData(propData);
                    Map<Surface, List<Proposition>> seeded = new HashMap<>();
                    seeded.put(surface, new ArrayList<>(Collections.singletonList(existing)));
                    edgePersonalizationResponseHandler.setQualifiedContentCardsBySurface(seeded);
                    edgePersonalizationResponseHandler.getNetworkRefreshedSurfaces().add(surface);

                    // rules engine now returns nothing for the surface (campaign ended server-side)
                    when(mockContentCardRulesEngine.evaluate(any(Event.class)))
                            .thenReturn(new HashMap<>());

                    // test
                    edgePersonalizationResponseHandler.removeOrReplaceContentCards(
                            mock(Event.class), Collections.singletonList(surface));

                    assertFalse(
                            "evicted surface must be removed from networkRefreshedSurfaces",
                            edgePersonalizationResponseHandler
                                    .getNetworkRefreshedSurfaces()
                                    .contains(surface));
                    assertNull(
                            "evicted surface must be removed from the qualified cache",
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .get(surface));
                });
    }

    @Test
    public void test_clearContentCards_clearsNetworkRefreshedSurfaces() {
        runUsingMockedServiceProvider(
                () -> {
                    edgePersonalizationResponseHandler
                            .getNetworkRefreshedSurfaces()
                            .add(new Surface("apifeed"));

                    edgePersonalizationResponseHandler.clearContentCards();

                    assertTrue(
                            "networkRefreshedSurfaces should be empty after clearContentCards",
                            edgePersonalizationResponseHandler
                                    .getNetworkRefreshedSurfaces()
                                    .isEmpty());
                });
    }

    // ========================================================================================
    // applyPropositionChange — offline flag gating
    // ========================================================================================

    @Test
    public void test_applyPropositionChange_flagOn_persistsContentCardsToDisk() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // enable offline availability
                        Map<String, Object> configState = new HashMap<>();
                        configState.put("messaging.contentCardOfflineAvailable", true);
                        SharedStateResult configResult =
                                new SharedStateResult(SharedStateStatus.SET, configState);
                        when(mockExtensionApi.getSharedState(
                                        eq("com.adobe.module.configuration"),
                                        any(),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(configResult);

                        // simulate a network response returning 6 content cards for the surface
                        Surface surface = new Surface("apifeed");
                        MessageTestConfig config = new MessageTestConfig();
                        config.count = 6;
                        List<Map<String, Object>> payload =
                                MessagingTestUtils.generateContentCardPayload(config);
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("payload", payload);
                        notificationData.put("requestEventId", "PERSIST_ON_EVENT_ID");
                        Event notificationEvent = mock(Event.class);
                        when(notificationEvent.getEventData()).thenReturn(notificationData);

                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "PERSIST_ON_EVENT_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });
                        edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                                notificationEvent);

                        // process completed (success path)
                        Map<String, Object> completedEventData = new HashMap<>();
                        completedEventData.put(ENDING_EVENT_ID, "PERSIST_ON_EVENT_ID");
                        Event completedEvent = mock(Event.class);
                        when(completedEvent.getEventData()).thenReturn(completedEventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(
                                completedEvent);

                        // verify all 6 content cards were written to the disk cache and nothing was
                        // cleared
                        ArgumentCaptor<Map<Surface, List<Proposition>>> captor =
                                ArgumentCaptor.forClass(Map.class);
                        verify(mockMessagingCacheUtilities, times(1))
                                .cacheContentCardPropositions(captor.capture(), any());
                        int totalPersisted = 0;
                        for (List<Proposition> props : captor.getValue().values()) {
                            totalPersisted += props.size();
                        }
                        assertEquals(6, totalPersisted);
                        verify(mockMessagingCacheUtilities, times(0))
                                .clearPersistedContentCardCache();
                    }
                });
    }

    @Test
    public void test_applyPropositionChange_flagOff_requestSucceeded_clearsDisk() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // disable offline availability
                        Map<String, Object> configState = new HashMap<>();
                        configState.put("messaging.contentCardOfflineAvailable", false);
                        SharedStateResult configResult =
                                new SharedStateResult(SharedStateStatus.SET, configState);
                        when(mockExtensionApi.getSharedState(
                                        eq("com.adobe.module.configuration"),
                                        any(),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(configResult);

                        // simulate a successful network response (no error)
                        Surface surface = new Surface("apifeed");
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "FLAG_OFF_EVENT_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });

                        // process completed (no error → success path)
                        Map<String, Object> completedEventData = new HashMap<>();
                        completedEventData.put(ENDING_EVENT_ID, "FLAG_OFF_EVENT_ID");
                        Event completedEvent = mock(Event.class);
                        when(completedEvent.getEventData()).thenReturn(completedEventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(
                                completedEvent);

                        // verify clearPersistedContentCardCache() was called
                        verify(mockMessagingCacheUtilities, times(1))
                                .clearPersistedContentCardCache();
                    }
                });
    }

    @Test
    public void test_applyPropositionChange_flagOff_requestFailed_doesNotClearDisk() {
        runUsingMockedServiceProvider(
                () -> {
                    try (MockedStatic<JSONRulesParser> ignored =
                            Mockito.mockStatic(JSONRulesParser.class)) {
                        when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class)))
                                .thenCallRealMethod();

                        // disable offline availability
                        Map<String, Object> configState = new HashMap<>();
                        configState.put("messaging.contentCardOfflineAvailable", false);
                        SharedStateResult configResult =
                                new SharedStateResult(SharedStateStatus.SET, configState);
                        when(mockExtensionApi.getSharedState(
                                        eq("com.adobe.module.configuration"),
                                        any(),
                                        eq(false),
                                        eq(SharedStateResolution.LAST_SET)))
                                .thenReturn(configResult);

                        Surface surface = new Surface("apifeed");
                        edgePersonalizationResponseHandler.setMessagesRequestEventId(
                                "FLAG_OFF_FAIL_EVENT_ID",
                                new ArrayList<Surface>() {
                                    {
                                        add(surface);
                                    }
                                });

                        // inject a non-recoverable edge error first so request is marked as failed
                        Map<String, Object> errorEventData = new HashMap<>();
                        errorEventData.put("requestEventId", "FLAG_OFF_FAIL_EVENT_ID");
                        errorEventData.put("status", 500);
                        Event errorEvent = mock(Event.class);
                        when(errorEvent.getEventData()).thenReturn(errorEventData);
                        edgePersonalizationResponseHandler.handleEdgeErrorResponse(errorEvent);

                        // process completed — request failed path skips
                        // applyPropositionChangeForEventId
                        Map<String, Object> completedEventData = new HashMap<>();
                        completedEventData.put(ENDING_EVENT_ID, "FLAG_OFF_FAIL_EVENT_ID");
                        Event completedEvent = mock(Event.class);
                        when(completedEvent.getEventData()).thenReturn(completedEventData);
                        edgePersonalizationResponseHandler.handleProcessCompletedEvent(
                                completedEvent);

                        // verify clearPersistedContentCardCache() was NOT called
                        verify(mockMessagingCacheUtilities, times(0))
                                .clearPersistedContentCardCache();
                    }
                });
    }

    // ========================================================================================
    // retrieveInMemoryPropositions (getPropositions) — offline flag gating
    // ========================================================================================

    @Test
    public void
            test_retrieveInMemoryPropositions_flagOff_withPersistedCards_clearsPersistedCache() {
        runUsingMockedServiceProvider(
                () -> {
                    // offline availability disabled
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", false);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(new SharedStateResult(SharedStateStatus.SET, configState));

                    // stale content card data still exists on disk
                    Surface surface = new Surface("apifeed");
                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    List<Map<String, Object>> payload =
                            MessagingTestUtils.generateContentCardPayload(config);
                    Map<Surface, List<Proposition>> cachedCC = new HashMap<>();
                    cachedCC.put(
                            surface,
                            Collections.singletonList(Proposition.fromEventData(payload.get(0))));
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(cachedCC);

                    // test — get path
                    edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                            Collections.singletonList(surface), mock(Event.class));

                    // verify the persisted content card cache was cleared
                    verify(mockMessagingCacheUtilities, times(1)).clearPersistedContentCardCache();
                });
    }

    @Test
    public void test_retrieveInMemoryPropositions_flagOff_noPersistedCards_doesNotClear() {
        runUsingMockedServiceProvider(
                () -> {
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", false);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(new SharedStateResult(SharedStateStatus.SET, configState));

                    // no content card data on disk — nothing to clear
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(null);

                    edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                            Collections.singletonList(new Surface("apifeed")), mock(Event.class));

                    verify(mockMessagingCacheUtilities, times(0)).clearPersistedContentCardCache();
                });
    }

    @Test
    public void test_retrieveInMemoryPropositions_flagOn_doesNotClear() {
        runUsingMockedServiceProvider(
                () -> {
                    // offline availability enabled — must never clear on get
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", true);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(new SharedStateResult(SharedStateStatus.SET, configState));

                    // even with data on disk
                    Surface surface = new Surface("apifeed");
                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    List<Map<String, Object>> payload =
                            MessagingTestUtils.generateContentCardPayload(config);
                    Map<Surface, List<Proposition>> cachedCC = new HashMap<>();
                    cachedCC.put(
                            surface,
                            Collections.singletonList(Proposition.fromEventData(payload.get(0))));
                    when(mockMessagingCacheUtilities.getCachedContentCardPropositions())
                            .thenReturn(cachedCC);

                    edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                            Collections.singletonList(surface), mock(Event.class));

                    verify(mockMessagingCacheUtilities, times(0)).clearPersistedContentCardCache();
                });
    }

    @Test
    public void
            test_retrieveInMemoryPropositions_flagOff_servesOnlyNetworkRefreshed_withoutMutatingCache() {
        runUsingMockedServiceProvider(
                () -> {
                    // offline availability disabled
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", false);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(new SharedStateResult(SharedStateStatus.SET, configState));

                    // seed the qualified cache with one network-origin surface and one disk-origin
                    // surface
                    Surface networkSurface = new Surface("network");
                    Surface diskSurface = new Surface("disk");
                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    Proposition networkProp =
                            Proposition.fromEventData(
                                    MessagingTestUtils.generateContentCardPayload(config).get(0));
                    Proposition diskProp =
                            Proposition.fromEventData(
                                    MessagingTestUtils.generateContentCardPayload(config).get(0));
                    edgePersonalizationResponseHandler
                            .getQualifiedContentCardsBySurface()
                            .put(networkSurface, Collections.singletonList(networkProp));
                    edgePersonalizationResponseHandler
                            .getQualifiedContentCardsBySurface()
                            .put(diskSurface, Collections.singletonList(diskProp));

                    // only the network surface was refreshed from Edge this session
                    edgePersonalizationResponseHandler
                            .getNetworkRefreshedSurfaces()
                            .add(networkSurface);

                    List<Surface> requested = new ArrayList<>();
                    requested.add(networkSurface);
                    requested.add(diskSurface);

                    // test — get path
                    edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                            requested, mock(Event.class));

                    // verify only the network-refreshed surface's card is served; the disk-origin
                    // surface is excluded
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    Map<String, Object> eventData = eventArgumentCaptor.getValue().getEventData();
                    List<Map<String, Object>> propositions =
                            DataReader.optTypedListOfMap(
                                    Object.class, eventData, "propositions", null);
                    assertEquals(1, propositions.size());

                    // the in-memory qualified cache must NOT be mutated by the get path — both the
                    // network and disk surfaces remain; only the response copy is filtered
                    assertTrue(
                            "network-refreshed surface must remain in the qualified cache",
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .containsKey(networkSurface));
                    assertTrue(
                            "disk-origin surface must remain in the qualified cache (source not"
                                    + " mutated)",
                            edgePersonalizationResponseHandler
                                    .getQualifiedContentCardsBySurface()
                                    .containsKey(diskSurface));
                });
    }

    @Test
    public void test_retrieveInMemoryPropositions_flagOn_servesAllRequestedSurfaces() {
        runUsingMockedServiceProvider(
                () -> {
                    // offline availability enabled
                    Map<String, Object> configState = new HashMap<>();
                    configState.put("messaging.contentCardOfflineAvailable", true);
                    when(mockExtensionApi.getSharedState(
                                    eq("com.adobe.module.configuration"),
                                    any(),
                                    eq(false),
                                    eq(SharedStateResolution.LAST_SET)))
                            .thenReturn(new SharedStateResult(SharedStateStatus.SET, configState));

                    // seed the qualified cache with one network-origin surface and one disk-origin
                    // surface
                    Surface networkSurface = new Surface("network");
                    Surface diskSurface = new Surface("disk");
                    MessageTestConfig config = new MessageTestConfig();
                    config.count = 1;
                    Proposition networkProp =
                            Proposition.fromEventData(
                                    MessagingTestUtils.generateContentCardPayload(config).get(0));
                    Proposition diskProp =
                            Proposition.fromEventData(
                                    MessagingTestUtils.generateContentCardPayload(config).get(0));
                    edgePersonalizationResponseHandler
                            .getQualifiedContentCardsBySurface()
                            .put(networkSurface, Collections.singletonList(networkProp));
                    edgePersonalizationResponseHandler
                            .getQualifiedContentCardsBySurface()
                            .put(diskSurface, Collections.singletonList(diskProp));

                    // only the network surface was refreshed this session; the disk surface is not
                    edgePersonalizationResponseHandler
                            .getNetworkRefreshedSurfaces()
                            .add(networkSurface);

                    List<Surface> requested = new ArrayList<>();
                    requested.add(networkSurface);
                    requested.add(diskSurface);

                    // test — get path
                    edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                            requested, mock(Event.class));

                    // verify both surfaces are served regardless of network-refresh state
                    verify(mockExtensionApi, times(1)).dispatch(eventArgumentCaptor.capture());
                    Map<String, Object> eventData = eventArgumentCaptor.getValue().getEventData();
                    List<Map<String, Object>> propositions =
                            DataReader.optTypedListOfMap(
                                    Object.class, eventData, "propositions", null);
                    assertEquals(2, propositions.size());
                });
    }
}
