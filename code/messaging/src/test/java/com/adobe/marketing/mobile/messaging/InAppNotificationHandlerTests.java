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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
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
public class InAppNotificationHandlerTests {

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

    private File cacheDir;
    private InAppNotificationHandler inAppNotificationHandler;

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
        reset(mockNetworkService);
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

            inAppNotificationHandler = new InAppNotificationHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockMessagingCacheUtilities, "TESTING_ID");

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
            inAppNotificationHandler.fetchMessages();

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
            inAppNotificationHandler.fetchMessages();

            // verify extensionApi.dispatch not called
            verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
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
                inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rules loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));
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
                inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached for 3 rules
                verify(mockMessagingCacheUtilities, times(3)).cacheImageAssets(any(List.class));

                // verify rules loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));
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
                inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached for 2 rules
                verify(mockMessagingCacheUtilities, times(2)).cacheImageAssets(any(List.class));

                // verify rules loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));

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
                inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rules loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));
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
                inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rules loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));
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
                inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

                // verify proposition cached
                verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

                // verify no assets cached
                verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

                // verify rules loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));
            }
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadIsEmpty() {
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
            inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify no proposition cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify no rules loaded
            verify(mockMessagingRulesEngine, times(0)).replaceRules(any(List.class));
        });
    }

    @Test
    public void test_handleEdgePersonalizationNotification_IAMPayloadIsNull() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("payload", null);
            eventData.put("requestEventId", "TESTING_ID");
            Event mockEvent = mock(Event.class);
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify no proposition cached
            verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify no rules loaded
            verify(mockMessagingRulesEngine, times(0)).replaceRules(any(List.class));
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
            inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify proposition cached
            verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify no rules loaded
            verify(mockMessagingRulesEngine, times(0)).replaceRules(any(List.class));
        });
    }

    @Test
    public void test_handlePersonalizationPayload_PayloadMissingScope() {
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
            inAppNotificationHandler.handleEdgePersonalizationNotification(mockEvent);

            // verify proposition cached
            verify(mockMessagingCacheUtilities, times(1)).cachePropositions(any(List.class));

            // verify no assets cached
            verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

            // verify no rules loaded
            verify(mockMessagingRulesEngine, times(0)).replaceRules(any(List.class));
        });
    }

    // ========================================================================================
    // inAppNotificationHandler load cached propositions on instantiation
    // ========================================================================================
    @Test
    public void test_cachedPropositions_ValidPayload() {
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
                config.count = 1;
                List<PropositionPayload> payload = MessagingUtils.getPropositionPayloads(MessagingTestUtils.generateMessagePayload(config));
                when(mockMessagingCacheUtilities.getCachedPropositions()).thenReturn(payload);

                // test
                inAppNotificationHandler = new InAppNotificationHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockMessagingCacheUtilities, "TESTING_ID");

                // verify proposition not cached as we are loading a cached proposition
                verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

                // verify assets cached
                verify(mockMessagingCacheUtilities, times(1)).cacheImageAssets(any(List.class));

                // verify rule loaded
                verify(mockMessagingRulesEngine, times(1)).replaceRules(any(List.class));
            }
        });
    }

    @Test
    public void test_cachedPropositions_nonMatchingScope() {
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
                config.count = 1;
                config.noValidAppSurfaceInPayload = true;
                config.nonMatchingAppSurfaceInPayload = true;
                List<PropositionPayload> payload = MessagingUtils.getPropositionPayloads(MessagingTestUtils.generateMessagePayload(config));
                when(mockMessagingCacheUtilities.getCachedPropositions()).thenReturn(payload);

                // test
                inAppNotificationHandler = new InAppNotificationHandler(mockMessagingExtension, mockExtensionApi, mockMessagingRulesEngine, mockMessagingCacheUtilities, "TESTING_ID");

                // verify proposition not cached as we are loading a cached proposition
                verify(mockMessagingCacheUtilities, times(0)).cachePropositions(any(List.class));

                // verify no assets cached
                verify(mockMessagingCacheUtilities, times(0)).cacheImageAssets(any(List.class));

                // verify no rules loaded
                verify(mockMessagingRulesEngine, times(0)).replaceRules(any(List.class));
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
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();
                Map<String, Object> details = new HashMap<>();
                Map<String, Object> mobileParameters = new HashMap<>();

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
                Message mockMessage = mockedConstruction.constructed().get(0);
                verify(mockMessage, times(1)).trigger();
                verify(mockMessage, times(1)).show();
            }
        });
    }

    @Test
    public void test_createInAppMessage_InvalidRuleConsequence() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();
                Map<String, Object> details = new HashMap<>();
                Map<String, Object> mobileParameters = new HashMap<>();

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

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }
}
