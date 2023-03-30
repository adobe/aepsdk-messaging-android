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

package com.adobe.marketing.mobile.messaging.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
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
import com.adobe.marketing.mobile.util.JSONUtils;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class AJOPayloadHandlerTests {

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
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
    MessagingCacheUtilities mockMessagingCacheUtilities;

    private ArgumentCaptor<List<LaunchRule>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private File cacheDir;
    private AJOPayloadHandler AJOPayloadHandler;

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

        if (cacheDir.exists()) {
            cacheDir.delete();
        }
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getNetworkService()).thenReturn(mockNetworkService);

            when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(cacheDir);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mock_applicationId");

            AJOPayloadHandler = new AJOPayloadHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockMessagingCacheUtilities, "TESTING_ID");

            runnable.run();
        }
    }

    // ========================================================================================
    // fetchMessages
    // ========================================================================================
    @Test
    public void test_fetchMessages_appIdPresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"},\"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mock_applicationId\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            AJOPayloadHandler.fetchMessages(null);

            // verify extensionApi.dispatch called
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event data
            Event event = eventCaptor.getValue();
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_fetchMessages_emptyAppId() {
        runUsingMockedServiceProvider(() -> {
            // setup
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("");

            // test
            AJOPayloadHandler.fetchMessages(null);

            // verify extensionApi.dispatch not called
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
        });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided() {
        List<String> surfacePaths = new ArrayList<>();
        surfacePaths.add("promos/feed1");
        surfacePaths.add("promos/feed2");
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"},\"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mock_applicationId/promos/feed1\", \"mobileapp://mock_applicationId/promos/feed2\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            AJOPayloadHandler.fetchMessages(surfacePaths);

            // verify extensionApi.dispatch called
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event data contains the feed surface paths
            Event event = eventCaptor.getValue();
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_fetchMessages_SurfacePathsProvided_InvalidPathsDropped() {
        List<String> surfacePaths = new ArrayList<>();
        surfacePaths.add("promos/feed1");
        surfacePaths.add("");
        surfacePaths.add(null);
        surfacePaths.add("promos/feed2");
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"},\"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mock_applicationId/promos/feed1\", \"mobileapp://mock_applicationId/promos/feed2\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            AJOPayloadHandler.fetchMessages(surfacePaths);

            // verify extensionApi.dispatch called
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event data contains the feed surface paths
            Event event = eventCaptor.getValue();
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_fetchMessages_NoSurfacePathsProvided() {
        List<String> surfacePaths = new ArrayList<>();
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> expectedEventData = null;
            try {
                expectedEventData = JSONUtils.toMap(new JSONObject("{\"xdm\":{\"eventType\":\"personalization.request\"},\"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://mock_applicationId\"]}}}"));
            } catch (JSONException e) {
                fail(e.getMessage());
            }
            // test
            AJOPayloadHandler.fetchMessages(surfacePaths);

            // verify extensionApi.dispatch called
            ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
            verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

            // verify event data contains the application id only
            Event event = eventCaptor.getValue();
            assertEquals(expectedEventData, event.getEventData());
        });
    }

    @Test
    public void test_fetchMessages_NoValidSurfacePathsProvided() {
        List<String> surfacePaths = new ArrayList<>();
        surfacePaths.add(null);
        surfacePaths.add("");
        runUsingMockedServiceProvider(() -> {
            // test
            AJOPayloadHandler.fetchMessages(surfacePaths);

            // verify extensionApi.dispatch not called
            verifyNoInteractions(mockExtensionApi);
        });
    }

    // ========================================================================================
    // handleEdgePersonalizationNotification
    // ========================================================================================
    @Test
    public void test_handleEdgePersonalizationNotification_ValidIAMPayloadPresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                MessageTestConfig config = new MessageTestConfig();
                config.count = 1;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(1, listArgumentCaptor.getValue().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_MultiplePersonalizationRequestHandlesReceived_Then_AllValidRulesAddedToRulesEngine() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                MessageTestConfig config = new MessageTestConfig();
                config.count = 3;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(3)).cacheImageAssets(any(List.class));

                // verify rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(3, listArgumentCaptor.getValue().size());

                // mock a second personalization event containing the same requestId
                config.count = 4;
                payload = MessagingTestUtils.generateMessagePayload(config);
                eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify propositions cached again incrementing number of times by 1
                verify(mockMessagingCacheUtilities, times(2)).cachePropositions(any(List.class));

                // verify assets cached 4 additional times as 4 new propositions were received
                verify(mockMessagingCacheUtilities, times(7)).cacheImageAssets(any(List.class));

                // verify new rules were added and not replaced as the request event id is the same for both personalization events
                verify(mockMessagingRulesEngine, times(1)).addRules(listArgumentCaptor.capture());
                assertEquals(4, listArgumentCaptor.getValue().size());

                // verify 7 rules in total have been loaded
                assertEquals(7, AJOPayloadHandler.getRuleCount());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_MultipleValidIAMPayloadPresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                MessageTestConfig config = new MessageTestConfig();
                config.count = 3;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached for 3 rules
                verify(mockMessagingCacheUtilities, times(3)).cacheImageAssets(any(List.class));

                // verify rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(3, listArgumentCaptor.getValue().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_OneInvalidIAMPayloadPresent() {
        // setup
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

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
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached for 2 rules
                verify(mockMessagingCacheUtilities, times(2)).cacheImageAssets(any(List.class));

                // verify rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(3, listArgumentCaptor.getValue().size());

            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadMissingMessageId() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                MessageTestConfig config = new MessageTestConfig();
                config.count = 1;
                config.isMissingMessageId = true;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(1, listArgumentCaptor.getValue().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadMissingMessageType() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                MessageTestConfig config = new MessageTestConfig();
                config.count = 1;
                config.isMissingMessageType = true;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(1, listArgumentCaptor.getValue().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadMissingMessageDetail() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                MessageTestConfig config = new MessageTestConfig();
                config.count = 1;
                config.isMissingMessageDetail = true;
                List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("payload", payload);
                eventData.put("requestEventId", "TESTING_ID");
                Event mockEvent = mock(Event.class);
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition not cached
                verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

                // verify no assets cached
                verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

                // verify empty rules replaced
                verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
                assertEquals(0, listArgumentCaptor.getValue().size());
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadIsEmpty_Then_CachedPropositionsAndLoadedRulesCleared() {
        runUsingMockedServiceProvider(() -> {
            // setup
            MessageTestConfig config = new MessageTestConfig();
            config.count = 1;
            config.hasEmptyPayload = true;
            List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", payload);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify cached propositions cleared
            verify(mockMessagingCacheUtilities, times(1)).cachePropositions(eq(null));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify cache not cleared
            verify(mockMessagingCacheUtilities, times(0)).clearCachedData();

            // verify empty rules replaced
            verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
            assertEquals(0, listArgumentCaptor.getValue().size());
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadIsNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            ArgumentCaptor<List<LaunchRule>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", null);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify no proposition cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify cache not cleared
            verify(mockMessagingCacheUtilities, times(0)).clearCachedData();

            // verify empty rules replaced
            verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
            assertEquals(0, listArgumentCaptor.getValue().size());
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_PayloadContainsNonMatchingScope() {
        runUsingMockedServiceProvider(() -> {
            // setup
            MessageTestConfig config = new MessageTestConfig();
            config.count = 1;
            config.noValidAppSurfaceInPayload = true;
            config.nonMatchingAppSurfaceInPayload = true;
            List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", payload);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify proposition not cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify empty rules replaced
            verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
            assertEquals(0, listArgumentCaptor.getValue().size());
        });
    }

    @Test
    public void test_handlePersonalizationPayload_PayloadMissingAppSurface() {
        runUsingMockedServiceProvider(() -> {
            // setup
            MessageTestConfig config = new MessageTestConfig();
            config.count = 1;
            config.noValidAppSurfaceInPayload = true;
            List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", payload);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify no proposition cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify empty rules replaced
            verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
            assertEquals(0, listArgumentCaptor.getValue().size());
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_MissingPropositionInfo() {
        runUsingMockedServiceProvider(() -> {
            // setup
            MessageTestConfig config = new MessageTestConfig();
            config.count = 1;
            config.isMissingScopeDetails = true;
            List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", payload);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify no proposition cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify empty rules replaced
            verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
            assertEquals(0, listArgumentCaptor.getValue().size());
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_MissingScope() {
        runUsingMockedServiceProvider(() -> {
            // setup
            MessageTestConfig config = new MessageTestConfig();
            config.count = 1;
            config.isMissingScope = true;
            List<Map<String, Object>> payload = MessagingTestUtils.generateMessagePayload(config);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", payload);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            AJOPayloadHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify no proposition cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify empty rules replaced
            verify(mockMessagingRulesEngine, times(1)).replaceRules(listArgumentCaptor.capture());
            assertEquals(0, listArgumentCaptor.getValue().size());
        });
    }

    // ========================================================================================
    // inAppNotificationHandler load cached propositions on instantiation
    // ========================================================================================
    @Test
    public void test_cachedPropositions_cacheLoadedOnInAppNotificationHandlerConstruction() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedStatic<JSONRulesParser> ignored = Mockito.mockStatic(JSONRulesParser.class)) {
                when(mockMessagingCacheUtilities.arePropositionsCached()).thenReturn(true);
                List<LaunchRule> launchRules = new ArrayList<>();
                LaunchRule mockLaunchRule = mock(LaunchRule.class);
                launchRules.add(mockLaunchRule);
                when(JSONRulesParser.parse(anyString(), any(ExtensionApi.class))).thenReturn(launchRules);

                CacheService cacheService = new FileCacheService();
                when(mockServiceProvider.getCacheService()).thenReturn(cacheService);
                MessageTestConfig config = new MessageTestConfig();
                config.count = 5;
                List<PropositionPayload> payload = null;
                try {
                    payload = MessagingUtils.getPropositionPayloads(MessagingTestUtils.generateMessagePayload(config));
                } catch (Exception e) {
                    fail(e.getMessage());
                }
                when(mockMessagingCacheUtilities.getCachedPropositions()).thenReturn(payload);

                // test
                AJOPayloadHandler = new AJOPayloadHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockMessagingCacheUtilities, "TESTING_ID");

                // verify proposition not cached as we are loading cached propositions
                verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(5)).cacheImageAssets(any(List.class));

                // verify cached rules added
                verify(mockMessagingRulesEngine, times(1)).addRules(listArgumentCaptor.capture());
                assertEquals(5, listArgumentCaptor.getValue().size());
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
                Map<String, Object> mobileParameters = new HashMap<>();

                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                RuleConsequence consequence = new RuleConsequence("123456789", MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE, details);

                // test
                AJOPayloadHandler.createInAppMessage(consequence);

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
                AJOPayloadHandler.createInAppMessage(consequence);

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
                AJOPayloadHandler.createInAppMessage(consequence);

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
                AJOPayloadHandler.createInAppMessage(consequence);

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
                AJOPayloadHandler.createInAppMessage(null);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }
}
