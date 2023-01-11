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
import static org.junit.Assert.assertNotNull;
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
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
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
    InAppNotificationHandler mockInAppNotificationHandler;
    @Mock
    SharedStateResult mockConfigData;
    @Mock
    SharedStateResult mockEdgeIdentityData;
    @Mock
    Application mockApplication;


    private static final String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private static final String mockAppId = "mock_applicationId";


//
//    @Mock
//    Application mockApplication;
//    @Mock
//    Context context;
//    @Mock
//    Core mockCore;
//    @Mock
//    AndroidPlatformServices mockPlatformServices;
//    @Mock
//    AndroidSystemInfoService mockAndroidSystemInfoService;
//    @Mock
//    AndroidNetworkService mockAndroidNetworkService;
//    @Mock
//    AndroidJsonUtility mockAndroidJsonUtility;
//    @Mock
//    PackageManager packageManager;
//    @Mock
//    Message mockMessage;


    private MessagingExtension messagingExtension;

//    private EventHub eventHub;

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
        reset(mockInAppNotificationHandler);
        reset(mockConfigData);
        reset(mockEdgeIdentityData);
        reset(mockDeviceInfoService);
        reset(mockApplication);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");
            when(mockCacheService.get(any(), any())).thenReturn(null);
            when(mockConfigData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("key", "value");
            }});
            when(mockEdgeIdentityData.getValue()).thenReturn(new HashMap<String, Object>() {{
                put("key", "value");
            }});

            messagingExtension = new MessagingExtension(mockExtensionApi, mockMessagingRulesEngine, mockInAppNotificationHandler);

            runnable.run();
        }
    }


//    void setupApplicationIdMocks() {
////        when(App.getApplication()).thenReturn(mockApplication);
//        when(mockApplication.getPackageManager()).thenReturn(packageManager);
//        when(mockApplication.getPackageName()).thenReturn(mockAppId);
//    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor() {
        runUsingMockedServiceProvider(() -> {
            assertNotNull(messagingExtension.messagingRulesEngine);
            assertNotNull(messagingExtension.inAppNotificationHandler);
        });
    }

    @Test
    public void test_onRegistered() {
        runUsingMockedServiceProvider(() -> {
            // test
            messagingExtension.onRegistered();

            // verify 5 listeners are registered
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.GENERIC_IDENTITY), eq(EventSource.REQUEST_CONTENT), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.EDGE), eq(MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.RULES_ENGINE), eq(EventSource.RESPONSE_CONTENT), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.WILDCARD), eq(EventSource.WILDCARD), any());
            verify(mockExtensionApi, times(1)).registerEventListener(eq(MessagingTestConstants.EventType.MESSAGING), eq(EventSource.REQUEST_CONTENT), any());
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

    //
//    @Test
//    public void test_processEvents_with_messagingEventType() {
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put("somekey", "somedata");
//
//        Event mockEvent = mock(Event.class);
//
//        // when mock event getType called return MESSAGING
//        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);
//
//        // when mock event getSource called return REQUEST_CONTENT
//        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());
//
//        when(mockEvent.getEventData()).thenReturn(eventData);
//
//        // when configState containsKey return true
//        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);
//        when(mockConfigData.containsKey(anyString())).thenReturn(true);
//
//        when(mockExtensionApi.getXDMSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(mockEdgeIdentityData);
//
//        // test
//        messagingExtension.queueEvent(mockEvent);
//        messagingExtension.processEvents();
//
//        // verify
//        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockEvent, times(3)).getType();
//        verify(mockEvent, times(2)).getSource();
//        verify(mockEvent, times(1)).getData();
//        assertEquals(0, messagingExtension.getEventQueue().size());
//    }
//
//    @Test
//    public void test_processEvents_whenConfigSharedState_notPresent() {
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put("somekey", "somedata");
//
//        Event mockEvent = mock(Event.class);
//
//        // when getSharedEventState return mock config
//        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(Collections.EMPTY_MAP);
//
//        when(mockExtensionApi.getXDMSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(mockEdgeIdentityData);
//
//        // test
//        messagingExtension.queueEvent(mockEvent);
//        messagingExtension.processEvents();
//
//        // verify event not processed
//        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockEvent, times(0)).getType();
//        assertEquals(1, messagingExtension.getEventQueue().size());
//    }
//
//    public void test_processEvents_whenIdentitySharedState_notPresent() {
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put("somekey", "somedata");
//
//        Event mockEvent = mock(Event.class);
//
//        // when getSharedEventState return mock config
//        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);
//
//        when(mockExtensionApi.getXDMSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(Collections.EMPTY_MAP);
//
//        // test
//        messagingExtension.queueEvent(mockEvent);
//        messagingExtension.processEvents();
//
//        // verify event not processed
//        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockEvent, times(0)).getType();
//        assertEquals(1, messagingExtension.getEventQueue().size());
//    }
//
    // ========================================================================================
    // handlePushToken
    // ========================================================================================
    @Test
    public void test_handlePushToken() {
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
                when(mockExtensionApi.getSharedState(eq(MessagingTestConstants.SharedState.Configuration.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockConfigData);
                when(mockExtensionApi.getXDMSharedState(eq(MessagingTestConstants.SharedState.EdgeIdentity.EXTENSION_NAME), eq(mockEvent), eq(false), eq(SharedStateResolution.LAST_SET))).thenReturn(mockEdgeIdentityData);

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
//
//    @Test
//    public void test_handlePushToken_when_eventIsNull() {
//        // Mocks
//        String mockECID = "mock_ecid";
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // when - then return mock
//        when(messagingState.getEcid()).thenReturn(mockECID);
//
//        //test
//        messagingExtension.handlePushToken(null);
//
//        // verify
//        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
//    }
//
//
//    @Test
//    public void test_handlePushToken_when_eventDataIsNull() {
//        // Mocks
//        String mockECID = "mock_ecid";
//        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(null).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // when - then return mock
//        when(messagingState.getEcid()).thenReturn(mockECID);
//
//        //test
//        messagingExtension.handlePushToken(mockEvent);
//
//        // verify
//        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
//    }
//
//    @Test
//    public void test_handlePushToken_when_tokenIsEmpty() {
//        // Mocks
//        String mockECID = "mock_ecid";
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "");
//        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // when - then return mock
//        when(messagingState.getEcid()).thenReturn(mockECID);
//
//        //test
//        messagingExtension.handlePushToken(mockEvent);
//
//        // verify
//        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
//    }
//
//    @Test
//    public void test_handlePushToken_when_ecidIsNull() {
//        // Mocks
//        String mockECID = null;
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_token");
//        Event mockEvent = mock(Event.class);
//
//        // when
//        when(mockEvent.getEventData()).thenReturn(eventData);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // when - then return mock
//        when(messagingState.getEcid()).thenReturn(mockECID);
//
//        //test
//        messagingExtension.handlePushToken(mockEvent);
//
//        // verify
//        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
//    }
//
//    // ========================================================================================
//    // handleTrackingInfo
//    // ========================================================================================
//    @Test
//    public void test_handleTrackingInfo_whenApplicationOpened_withCustomAction() {
//        // expected
//        final String expectedEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mock_actionId\"},\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"mock_eventType\"},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}";
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);
//        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify experience event dataset id
//        verify(messagingState, times(1)).getExperienceEventDatasetId();
//
//        // verify dispatch event is called
//        // 1 event dispatched: edge event with tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertEquals(expectedEventData, event.getData().toString());
//    }
//
//    @Test
//    public void test_handleTrackingInfo_whenApplicationOpened_withoutCustomAction() {
//        // expected
//        final String expectedEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"mock_eventType\"},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}";
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);
//        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify experience event dataset id
//        verify(messagingState, times(1)).getExperienceEventDatasetId();
//
//        // verify dispatch event is called
//        // 1 event dispatched: edge event with tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertEquals(expectedEventData, event.getData().toString());
//    }
//
//    @Test
//    public void test_handleTrackingInfo_when_mixinsDataPresent() {
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//        final String expectedEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mock_actionId\"},\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":0}},\"eventType\":\"mock_eventType\",\"_experience\":{\"customerJourneyManagement\":{\"pushChannelContext\":{\"platform\":\"fcm\"},\"messageExecution\":{\"messageExecutionID\":\"16-Sept-postman\",\"journeyVersionInstanceId\":\"someJourneyVersionInstanceId\",\"messageID\":\"567\",\"journeyVersionID\":\"some-journeyVersionId\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/push\"}}}}},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}";
//        final String mockCJMData = "{\n" +
//                "        \"mixins\" :{\n" +
//                "          \"_experience\": {\n" +
//                "            \"customerJourneyManagement\": {\n" +
//                "              \"messageExecution\": {\n" +
//                "                \"messageExecutionID\": \"16-Sept-postman\",\n" +
//                "                \"messageID\": \"567\",\n" +
//                "                \"journeyVersionID\": \"some-journeyVersionId\",\n" +
//                "                \"journeyVersionInstanceId\": \"someJourneyVersionInstanceId\"\n" +
//                "              }\n" +
//                "            }\n" +
//                "          }\n" +
//                "        }\n" +
//                "      }";
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);
//        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify
//        verify(messagingState, times(1)).getExperienceEventDatasetId();
//
//        // 1 event dispatched: edge event with tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertEquals(MessagingConstants.EventType.EDGE.toLowerCase(), event.getEventType().getName());
//        // Verify _experience exist
//        assertEquals(expectedEventData, event.getData().toString());
//    }
//
//    @Test
//    public void test_handleTrackingInfo_when_EventDataNull() {
//        // Mocks
//        Event mockEvent = mock(Event.class);
//
//        // when
//        when(mockEvent.getEventData()).thenReturn(null);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify
//        verify(mockEvent, times(1)).getData();
//        verify(messagingState, times(0)).getExperienceEventDatasetId();
//    }
//
//    @Test
//    public void test_handleTrackingInfo_when_eventTypeIsNull() {
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
//        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify
//        verify(messagingState, times(0)).getExperienceEventDatasetId();
//    }
//
//    @Test
//    public void test_handleTrackingInfo_when_MessageIdIsNull() {
//        final String mockCJMData = "{\n" +
//                "        \"mixins\" :{\n" +
//                "          \"_experience\": {\n" +
//                "            \"customerJourneyManagement\": {\n" +
//                "              \"messageExecution\": {\n" +
//                "                \"messageExecutionID\": \"16-Sept-postman\",\n" +
//                "                \"messageID\": \"567\",\n" +
//                "                \"journeyVersionID\": \"some-journeyVersionId\",\n" +
//                "                \"journeyVersionInstanceId\": \"someJourneyVersionInstanceId\"\n" +
//                "              }\n" +
//                "            }\n" +
//                "          }\n" +
//                "        }\n" +
//                "      }";
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);
//        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify
//        verify(messagingState, times(0)).getExperienceEventDatasetId();
//    }
//
//    @Test
//    public void test_handleTrackingInfo_when_ExperienceEventDatasetIsEmpty() {
//        // setup
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("");
//        // Mocks
//        final String mockCJMData = "{\n" +
//                "        \"mixins\" :{\n" +
//                "          \"_experience\": {\n" +
//                "            \"customerJourneyManagement\": {\n" +
//                "              \"messageExecution\": {\n" +
//                "                \"messageExecutionID\": \"16-Sept-postman\",\n" +
//                "                \"messageID\": \"567\",\n" +
//                "                \"journeyVersionID\": \"some-journeyVersionId\",\n" +
//                "                \"journeyVersionInstanceId\": \"someJourneyVersionInstanceId\"\n" +
//                "              }\n" +
//                "            }\n" +
//                "          }\n" +
//                "        }\n" +
//                "      }";
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
//        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);
//        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        //test
//        messagingExtension.handleTrackingInfo(mockEvent);
//
//        // verify
//        verify(messagingState, times(1)).getExperienceEventDatasetId();
//
//        // 0 events dispatched: edge event with tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(0));
//        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
//    }
//
//    // ========================================================================================
//    // sendPropositionInteraction
//    // ========================================================================================
//    @Test
//    public void test_sendPropositionInteraction_InAppInteractTracking() throws JSONException {
//        // setup
//        mockMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
//        // expected
//        final JSONObject expectedEventData = new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionInteract\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"interact\":1},\"propositionAction\":{\"id\":\"confirm\",\"label\":\"confirm\"},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"confirm\",\"eventType\":\"interact\"}}");
//        final Map<String, Object> expectedEventDataMap = MessagingTestUtils.toMap(expectedEventData);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        // Mocks
//        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");
//
//        //test
//        messagingExtension.sendPropositionInteraction("confirm", MessagingEdgeEventType.IN_APP_INTERACT, mockMessage);
//
//        // verify dispatch event is called
//        // 1 event dispatched: edge event with in app interact event tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertTrue(arePropositionPayloadsEqual(expectedEventDataMap, event.getEventData()));
//    }
//
//    @Test
//    public void test_sendPropositionInteraction_InAppInteractTracking_WhenScopeDetailsNull() {
//        // setup
//        mockMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(true);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // Mocks
//        when(messagingState.getExperienceEventDatasetId()).thenReturn(null);
//
//        //test
//        messagingExtension.sendPropositionInteraction("confirm", MessagingEdgeEventType.IN_APP_INTERACT, mockMessage);
//
//        // verify dispatch event is not called
//        PowerMockito.verifyStatic(MobileCore.class, times(0));
//        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
//    }
//
//    @Test
//    public void test_sendPropositionInteraction_InAppDismissTracking() throws JSONException {
//        // setup
//        mockMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
//        // expected
//        final JSONObject expectedEventData = new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionDismiss\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"dismiss\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"dismiss\"}}");
//        final Map<String, Object> expectedEventDataMap = MessagingTestUtils.toMap(expectedEventData);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        //test
//        messagingExtension.sendPropositionInteraction(null, MessagingEdgeEventType.IN_APP_DISMISS, mockMessage);
//
//        // verify dispatch event is called
//        // 1 event dispatched: edge event with in app dismiss event tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertTrue(arePropositionPayloadsEqual(expectedEventDataMap, event.getEventData()));
//    }
//
//    @Test
//    public void test_sendPropositionInteraction_InAppDisplayTracking() throws JSONException {
//        // setup
//        mockMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
//        // expected
//        final JSONObject expectedEventData = new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionDisplay\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"display\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"display\"}}");
//        final Map<String, Object> expectedEventDataMap = MessagingTestUtils.toMap(expectedEventData);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        //test
//        messagingExtension.sendPropositionInteraction(null, MessagingEdgeEventType.IN_APP_DISPLAY, mockMessage);
//
//        // verify dispatch event is called
//        // 1 event dispatched: edge event with in app display event tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertTrue(arePropositionPayloadsEqual(expectedEventDataMap, event.getEventData()));
//    }
//
//    @Test
//    public void test_sendPropositionInteraction_InAppTriggeredTracking() throws JSONException {
//        // setup
//        mockMessage.propositionInfo = MessagingTestUtils.generatePropositionInfo(false);
//        // expected
//        final JSONObject expectedEventData = new JSONObject("{\"xdm\":{\"eventType\":\"decisioning.propositionTrigger\",\"_experience\":{\"decisioning\":{\"propositionEventType\":{\"trigger\":1},\"propositions\":[{\"scopeDetails\":{\"scopeDetails\":{\"cjmEvent\":{\"messageExecution\":{\"messageExecutionID\":\"testExecutionId\"}}}},\"scope\":\"mobileapp://mock_applicationId\",\"id\":\"testResponseId\"}]}}},\"iam\":{\"id\":\"\",\"action\":\"\",\"eventType\":\"trigger\"}}");
//        final Map<String, Object> expectedEventDataMap = MessagingTestUtils.toMap(expectedEventData);
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        //test
//        messagingExtension.sendPropositionInteraction(null, MessagingEdgeEventType.IN_APP_TRIGGER, mockMessage);
//
//        // verify dispatch event is called
//        // 1 event dispatched: edge event with in app trigger event tracking info
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertTrue(arePropositionPayloadsEqual(expectedEventDataMap, event.getEventData()));
//    }
//
//    // ========================================================================================
//    // getExecutor
//    // ========================================================================================
//    @Test
//    public void test_getExecutor_NeverReturnsNull() {
//        // test
//        ExecutorService executorService = messagingExtension.getExecutor();
//        assertNotNull("The executor should not return null", executorService);
//
//        // verify
//        assertEquals("Gets the same executor instance on the next get", executorService, messagingExtension.getExecutor());
//    }

    boolean arePropositionPayloadsEqual(Map<String, Object> expected, Map<String, Object> actual) {
        Map<String, Object> expectedXdmMap = (Map<String, Object>) expected.get("xdm");
        Map<String, Object> expectedEventHistoryMap = (Map<String, Object>) expected.get("iam");
        String expectedScope = (String) expected.get("scope");
        String expectedId = (String) expected.get("id");

        Map<String, Object> actualXdmMap = (Map<String, Object>) actual.get("xdm");
        Map<String, Object> actualEventHistoryMap = (Map<String, Object>) actual.get("iam");
        String actualScope = (String) actual.get("scope");
        String actualId = (String) actual.get("id");

        if (expectedId != actualId) {
            return false;
        }

        if (expectedScope != actualScope) {
            return false;
        }

        return expectedXdmMap.equals(actualXdmMap) && expectedEventHistoryMap.equals(actualEventHistoryMap);
    }
}