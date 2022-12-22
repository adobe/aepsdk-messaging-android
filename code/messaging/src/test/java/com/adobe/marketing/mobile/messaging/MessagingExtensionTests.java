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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.caching.CacheService;

import org.json.JSONException;
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    ConcurrentLinkedQueue<Event> mockEventQueue;
    @Mock
    LaunchRulesEngine mockMessagingRulesEngine;
    @Mock
    InAppNotificationHandler mockInAppNotificationHandler;


    private static final String html = "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>";
    private static final String mockAppId = "mock_applicationId";


//    @Mock
//    Map<String, Object> mockConfigData;
//    @Mock
//    Map<String, Object> mockEdgeIdentityData;
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
        reset(mockEventQueue);
        reset(mockMessagingRulesEngine);
        reset(mockInAppNotificationHandler);
    }

    void runUsingMockedServiceProvider(final Runnable runnable) {
        messagingExtension = new MessagingExtension(mockExtensionApi, mockEventQueue, mockMessagingRulesEngine, mockInAppNotificationHandler);

        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic = Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic.when(ServiceProvider::getInstance).thenReturn(mockServiceProvider);
            when(mockServiceProvider.getCacheService()).thenReturn(mockCacheService);
            when(mockCacheService.get(any(), any())).thenReturn(null);

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

            // verify 6 listeners are registered
            verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.HUB), eq(EventSource.SHARED_STATE), any());
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
    // queueEvent
    // ========================================================================================
    @Test
    public void test_QueueEvent() {
        runUsingMockedServiceProvider(() -> {
            // test 1
            assertNotNull("EventQueue instance is should never be null", messagingExtension.getEventQueue());

            // test 2
            Event sampleEvent = new Event.Builder("event 1", "eventType", "eventSource").build();
            messagingExtension.queueEvent(sampleEvent);
            assertEquals("The size of the eventQueue should be correct", 1, messagingExtension.getEventQueue().size());

            // test 3
            messagingExtension.queueEvent(null);
            assertEquals("The size of the eventQueue should be correct", 1, messagingExtension.getEventQueue().size());

            // test 4
            Event anotherEvent = new Event.Builder("event 2", "eventType", "eventSource").build();
            messagingExtension.queueEvent(anotherEvent);
            assertEquals("The size of the eventQueue should be correct", 2, messagingExtension.getEventQueue().size());
        });
    }

    // ========================================================================================
    // processHubSharedState
    // ========================================================================================
    @Test
    public void test_processHubSharedState() {
        runUsingMockedServiceProvider(() -> {
            //Mocks
            Map<String, Object> data = new HashMap<>();
            data.put(MessagingConstants.EventDataKeys.STATE_OWNER, MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME);
            Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").setEventData(data).build();

            // Test
            messagingExtension.processHubSharedState(mockEvent);

            // Verify
            verify(mockEventQueue, times(1)).isEmpty();
        });
    }

//    @Test
//    public void test_processHubSharedState_NoMatchingStateOwner() {
//        //Mocks
//       Map<String, Object> data = new EventData();
//        data.putString(MessagingConstants.EventDataKeys.STATE_OWNER, "somerandomstateowner");
//        Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").setData(data).build();
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "eventQueue", mockEventQueue);
//
//        // Test
//        messagingExtension.processHubSharedState(mockEvent);
//
//        // Verify
//        verify(mockEventQueue, times(0)).isEmpty();
//    }
//
//    // ========================================================================================
//    // processEvents
//    // ========================================================================================
//    @Test
//    public void test_processEvents_when_noEventInQueue() {
//        // Mocks
//        ExtensionErrorCallback<ExtensionError> mockCallback = new ExtensionErrorCallback<ExtensionError>() {
//            @Override
//            public void error(ExtensionError extensionError) {
//            }
//        };
//        Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").build();
//
//        // test
//        messagingExtension.processEvents();
//
//        // verify
//        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, mockEvent, mockCallback);
//        verify(mockExtensionApi, times(0)).getXDMSharedEventState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, mockEvent, mockCallback);
//    }
//
//    @Test
//    public void test_processEvents_whenSharedStates_present() {
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
//                any(ExtensionErrorCallback.class))).thenReturn(mockEdgeIdentityData);
//
//        // test
//        messagingExtension.queueEvent(mockEvent);
//        messagingExtension.processEvents();
//
//        // verify
//        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
//        verify(mockEvent, times(5)).getType();
//        assertEquals(0, messagingExtension.getEventQueue().size());
//    }
//
//    @Test
//    public void test_processEvents_with_genericIdentityEvent() {
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put("somekey", "somedata");
//
//        Event mockEvent = mock(Event.class);
//
//        // when mock event getType called return GENERIC_IDENTITY
//        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY.getName());
//
//        // when mock event getSource called return REQUEST_CONTENT
//        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());
//
//        when(mockEvent.getEventData()).thenReturn(eventData);
//
//        // when configState containsKey return true
//        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
//                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);
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
//        verify(mockEvent, times(2)).getType();
//        verify(mockEvent, times(1)).getSource();
//        verify(mockEvent, times(4)).getEventData();
//        assertEquals(0, messagingExtension.getEventQueue().size());
//    }
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
//    // ========================================================================================
//    // handlePushToken
//    // ========================================================================================
//    @Test
//    public void test_handlePushToken() {
//        // expected
//        final String expectedEventData = "{\"data\":{\"pushNotificationDetails\":[{\"denylisted\":false,\"identity\":{\"namespace\":{\"code\":\"ECID\"},\"id\":\"mock_ecid\"},\"appID\":\"mock_placement\",\"platform\":\"fcm\",\"token\":\"mock_push_token\"}]}}";
//
//        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
//
//        // Mocks
//        Map<String, Object> eventData = new HashMap<>();
//        eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
//        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
//        String mockECID = "mock_ecid";
//
//        // private mocks
//        Whitebox.setInternalState(messagingExtension, "messagingState", messagingState);
//
//        // when - then return mock
//        when(messagingState.getEcid()).thenReturn(mockECID);
//
//        // when App.getApplication().getPackageName() return mock packageName
//        when(App.getApplication()).thenReturn(mockApplication);
//        when(mockApplication.getPackageName()).thenReturn("mock_placement");
//
//        //test
//        messagingExtension.handlePushToken(mockEvent);
//
//        // verify
//        // 1 event dispatched: edge event with push profile data
//        PowerMockito.verifyStatic(MobileCore.class, times(1));
//        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
//
//        // verify event
//        Event event = eventCaptor.getValue();
//        assertNotNull(event.getData());
//        assertEquals(MessagingConstants.EventName.PUSH_PROFILE_EDGE_EVENT, event.getName());
//        assertEquals(MessagingConstants.EventType.EDGE.toLowerCase(), event.getEventType().getName());
//        assertEquals(EventSource.REQUEST_CONTENT.getName(), event.getSource());
//        assertEquals(expectedEventData, event.getData().toString());
//
//        // verify if the push token is stored in shared state
//        verify(mockExtensionApi, times(1)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
//    }
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
        Map<String, Object> expectedEventHistoryMap =  (Map<String, Object>) expected.get("iam");
        String expectedScope = (String) expected.get("scope");
        String expectedId = (String) expected.get("id");

        Map<String, Object> actualXdmMap = (Map<String, Object>) actual.get("xdm");
        Map<String, Object> actualEventHistoryMap =  (Map<String, Object>) actual.get("iam");
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