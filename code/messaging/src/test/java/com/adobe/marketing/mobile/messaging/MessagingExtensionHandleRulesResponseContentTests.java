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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.os.Looper;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingExtensionHandleRulesResponseContentTests {

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
    SharedStateResult mockConfigData;
    @Mock
    SharedStateResult mockEdgeIdentityData;
    @Mock
    Application mockApplication;
    @Mock
    AppContextService mockAppContextService;
    @Mock
    Looper mockLooper;

    private MessagingExtension messagingExtension;
    private final Map<String, Object> mobileParameters = new HashMap() {
        {
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.SCHEMA_VERSION, "version");
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.VERTICAL_ALIGN, "center");
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.HORIZONTAL_INSET, 0);
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.DISMISS_ANIMATION, "bottom");
            put(MessagingTestConstants.EventDataKeys.MobileParametersKeys.UI_TAKEOVER, true);
        }
    };

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
        reset(mockConfigData);
        reset(mockEdgeIdentityData);
        reset(mockDeviceInfoService);
        reset(mockApplication);
        reset(mockAppContextService);
        reset(mockLooper);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
            when(mockAppContextService.getApplication()).thenReturn(mockApplication);
            when(mockApplication.getMainLooper()).thenReturn(mockLooper);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");
            when(mockCacheService.get(any(), any())).thenReturn(null);
            when(mockConfigData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("messaging.eventDataset", "mock_datasetId");
            }});
            when(mockEdgeIdentityData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("key", "value");
            }});
            when(mockExtensionApi.getSharedState(eq(MessagingTestConstants.SharedState.Configuration.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(mockConfigData);
            when(mockExtensionApi.getSharedState(eq(MessagingTestConstants.SharedState.EdgeIdentity.EXTENSION_NAME), any(Event.class), anyBoolean(), any(SharedStateResolution.class))).thenReturn(mockEdgeIdentityData);

            messagingExtension = new MessagingExtension(mockExtensionApi, mockMessagingRulesEngine, null);

            runnable.run();
        }
    }

    // ========================================================================================
    // handling rules response events
    // ========================================================================================
    @Test
    public void test_handleRulesResponseEvent_ValidConsequencePresent() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();
                Map<String, Object> details = new HashMap<>();

                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify MessagingFullscreenMessage.show() and MessagingFullscreenMessage.trigger() called
                Message mockMessage = mockedConstruction.constructed().get(0);
                verify(mockMessage, times(1)).trigger();
                verify(mockMessage, times(1)).show();
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_UnsupportedConsequenceType() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();
                Map<String, Object> details = new HashMap<>();

                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, "unknownIamType");
                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_MissingConsequenceType() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();
                Map<String, Object> details = new HashMap<>();

                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
                details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_NullConsequences() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, null);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_EmptyConsequences() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> eventData = new HashMap<>();
                Map<String, Object> consequence = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_EmptyDetails() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();
                Map<String, Object> details = new HashMap<>();

                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_NullDetails() {
        runUsingMockedServiceProvider(() -> {
            // setup
            try (MockedConstruction<Message> mockedConstruction = Mockito.mockConstruction(Message.class)) {
                Map<String, Object> consequence = new HashMap<>();

                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, null);
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
                Map<String, Object> eventData = new HashMap<>();
                eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

                // Mocks
                Event mockEvent = mock(Event.class);

                // when mock event getType called return RULES_ENGINE
                when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

                // when mock event getSource called return RESPONSE_CONTENT
                when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

                // when get eventData called return event data containing a valid rules response content event with a triggered consequence
                when(mockEvent.getEventData()).thenReturn(eventData);

                // test
                messagingExtension.processEvent(mockEvent);

                // verify no message object created
                assertEquals(0, mockedConstruction.constructed().size());
            }
        });
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_EmptyHTML() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> consequence = new HashMap<>();
            Map<String, Object> details = new HashMap<>();

            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "");
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

            // Mocks
            Event mockEvent = mock(Event.class);

            // when mock event getType called return RULES_ENGINE
            when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

            // when mock event getSource called return RESPONSE_CONTENT
            when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

            // when get eventData called return event data containing a valid rules response content event with a triggered consequence
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify no message object created
            assertNull(messagingExtension.inAppNotificationHandler.getMessage());
        });
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_MissingHTML() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> consequence = new HashMap<>();
            Map<String, Object> details = new HashMap<>();

            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

            // Mocks
            Event mockEvent = mock(Event.class);

            // when mock event getType called return RULES_ENGINE
            when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

            // when mock event getSource called return RESPONSE_CONTENT
            when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

            // when get eventData called return event data containing a valid rules response content event with a triggered consequence
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify no message object created
            assertNull(messagingExtension.inAppNotificationHandler.getMessage());
        });
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_MissingMessageId() {
        runUsingMockedServiceProvider(() -> {
            // setup
            Map<String, Object> consequence = new HashMap<>();
            Map<String, Object> details = new HashMap<>();

            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, mobileParameters);
            details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
            consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
            Map<String, Object> eventData = new HashMap<>();
            eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

            // Mocks
            Event mockEvent = mock(Event.class);

            // when mock event getType called return RULES_ENGINE
            when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE);

            // when mock event getSource called return RESPONSE_CONTENT
            when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT);

            // when get eventData called return event data containing a valid rules response content event with a triggered consequence
            when(mockEvent.getEventData()).thenReturn(eventData);

            // test
            messagingExtension.processEvent(mockEvent);

            // verify no message object created
            assertNull(messagingExtension.inAppNotificationHandler.getMessage());
        });
    }
}
