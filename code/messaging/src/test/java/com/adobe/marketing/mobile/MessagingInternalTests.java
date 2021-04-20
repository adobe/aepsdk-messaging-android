/*
  Copyright 2020 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import android.app.Application;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, LocalStorageService.class, App.class, Context.class})
public class MessagingInternalTests {

    private int EXECUTOR_TIMEOUT = 5;
    private MessagingInternal messagingInternal;

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
    @Mock
    ExtensionUnexpectedError mockExtensionUnexpectedError;
    @Mock
    MessagingState messagingState;
    @Mock
    Map<String, Object> mockConfigData;
    @Mock
    Map<String, Object> mockEdgeIdentityData;
    @Mock
    EventData mockEdgeIdentityEventData;
    @Mock
    ConcurrentLinkedQueue<Event> mockEventQueue;
    @Mock
    Application mockApplication;
    @Mock
    Context context;

    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        Mockito.when(App.getAppContext()).thenReturn(context);
        messagingInternal = new MessagingInternal(mockExtensionApi);
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor() {
        // verify 2 listeners are registered
        verify(mockExtensionApi, times(1)).registerEventListener(eq(MessagingConstant.EventType.MESSAGING),
                eq(EventSource.REQUEST_CONTENT.getName()), eq(ListenerMessagingRequestContent.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(1)).registerListener(eq(EventType.GENERIC_IDENTITY),
                eq(EventSource.REQUEST_CONTENT), eq(ListenerIdentityRequestContent.class));
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        // test
        String moduleName = messagingInternal.getName();
        assertEquals("getName should return the correct module name", MessagingConstant.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = messagingInternal.getVersion();
        assertEquals("getVesion should return the correct module version", MessagingConstant.EXTENSION_VERSION,
                moduleVersion);
    }

    // ========================================================================================
    // queueEvent
    // ========================================================================================
    @Test
    public void test_QueueEvent() {
        // test 1
        assertNotNull("EventQueue instance is should never be null", messagingInternal.getEventQueue());

        // test 2
        Event sampleEvent = new Event.Builder("event 1", "eventType", "eventSource").build();
        messagingInternal.queueEvent(sampleEvent);
        assertEquals("The size of the eventQueue should be correct", 1, messagingInternal.getEventQueue().size());

        // test 3
        messagingInternal.queueEvent(null);
        assertEquals("The size of the eventQueue should be correct", 1, messagingInternal.getEventQueue().size());

        // test 4
        Event anotherEvent = new Event.Builder("event 2", "eventType", "eventSource").build();
        messagingInternal.queueEvent(anotherEvent);
        assertEquals("The size of the eventQueue should be correct", 2, messagingInternal.getEventQueue().size());
    }

    // ========================================================================================
    // processEvents
    // ========================================================================================
    @Test
    public void test_processEvents_when_noEventInQueue() {
        // Mocks
        ExtensionErrorCallback<ExtensionError> mockCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) { }
        };
        Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").build();

        // test
        messagingInternal.processEvents();

        // verify
        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME, mockEvent, mockCallback);
        verify(mockExtensionApi, times(0)).getXDMSharedEventState(MessagingConstant.SharedState.EdgeIdentity.EXTENSION_NAME, mockEvent, mockCallback);
    }

    @Test
    public void test_processEvents_whenSharedStates_present() {
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("somekey", "somedata");

        Event mockEvent = mock(Event.class);

        // when getSharedEventState return mock config
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);

        when(mockExtensionApi.getXDMSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockEdgeIdentityData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify
        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockEvent, times(2)).getType();
        assertEquals(messagingInternal.getEventQueue().size(), 0);
    }

    @Test
    public void test_processEvents_with_genericIdentityEvent() {
        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return GENERIC_IDENTITY
        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY.getName());

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        when(mockEvent.getEventData()).thenReturn(null);

        // when configState containsKey return true
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);

        when(mockExtensionApi.getXDMSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockEdgeIdentityData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify
        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockEvent, times(1)).getType();
        verify(mockEvent, times(1)).getSource();
        verify(mockEvent, times(1)).getEventData();
        assertEquals(messagingInternal.getEventQueue().size(), 0);
    }

    @Test
    public void test_processEvents_with_messagingEventType() {
        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return MESSAGING
        when(mockEvent.getType()).thenReturn(MessagingConstant.EventType.MESSAGING);

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        when(mockEvent.getEventData()).thenReturn(null);

        // when configState containsKey return true
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockConfigData);
        when(mockConfigData.containsKey(anyString())).thenReturn(true);

        when(mockExtensionApi.getXDMSharedEventState(anyString(), any(Event.class),
                any(ExtensionErrorCallback.class))).thenReturn(mockEdgeIdentityData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify
        verify(mockExtensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(1)).getXDMSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockEvent, times(2)).getType();
        verify(mockEvent, times(1)).getSource();
        verify(mockEvent, times(1)).getData();
        assertEquals(messagingInternal.getEventQueue().size(), 0);
    }

    // ========================================================================================
    // handlePushToken
    // ========================================================================================
    @Test
    public void test_handlePushToken() {
        // expected
        final String expectedEventData = "{\"data\":{\"pushNotificationDetails\":[{\"denylisted\":false,\"identity\":{\"namespace\":{\"code\":\"ECID\",\"id\":\"mock_ecid\"}},\"appId\":\"mock_package\",\"platform\":\"fcm\",\"token\":\"mock_push_token\"}]}}";

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
        String mockECID = "mock_ecid";

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        // when App.getApplication().getPackageName() return mock packageName
        when(App.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getPackageName()).thenReturn("mock_package");

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        PowerMockito.verifyStatic(MobileCore.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        assertNotNull(event.getData());
        assertEquals(MessagingConstant.EventName.MESSAGING_PUSH_PROFILE_EDGE_EVENT, event.getName());
        assertEquals(MessagingConstant.EventType.EDGE.toLowerCase(), event.getEventType().getName());
        assertEquals(EventSource.REQUEST_CONTENT.getName(), event.getSource());
        assertEquals(expectedEventData, event.getData().toString());
    }


    @Test
    public void test_handlePushToken_when_eventDataIsNull() {
        // Mocks
        String mockECID = "mock_ecid";
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(null).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handlePushToken_when_tokenIsEmpty() {
        // Mocks
        String mockECID = "mock_ecid";
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handlePushToken_when_ecidIsNull() {
        // Mocks
        String mockECID = null;
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_token");
        Event mockEvent = mock(Event.class);

        // when
        when(mockEvent.getEventData()).thenReturn(eventData);

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when getLocalStorageService() return mockLocalStorageService
        LocalStorageService.DataStore mockDataStore = mock(LocalStorageService.DataStore.class);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // handleTrackingInfo
    // ========================================================================================
    @Test
    public void test_handleTrackingInfo_whenApplicationOpened_withCustomAction() {
        // expected
        final String expectedEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mock_actionId\"},\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"mock_eventType\"},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}";

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);
        Event mockEvent = new Event.Builder("event1", MessagingConstant.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify experience event dataset id
        verify(messagingState, times(1)).getExperienceEventDatasetId();

        // verify dispatch event is called
        PowerMockito.verifyStatic(MobileCore.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        assertNotNull(event.getData());
        assertEquals(expectedEventData, event.getData().toString());
    }

    @Test
    public void test_handleTrackingInfo_whenApplicationOpened_withoutCustomAction() {
        // expected
        final String expectedEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"mock_eventType\"},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}";

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);
        Event mockEvent = new Event.Builder("event1", MessagingConstant.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify experience event dataset id
        verify(messagingState, times(1)).getExperienceEventDatasetId();

        // verify dispatch event is called
        PowerMockito.verifyStatic(MobileCore.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        assertNotNull(event.getData());
        assertEquals(expectedEventData, event.getData().toString());
    }

    @Test
    public void test_handleTrackingInfo_when_mixinsData() {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final String expectedEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mock_actionId\"},\"pushProviderMessageID\":\"mock_messageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":0}},\"eventType\":\"mock_eventType\",\"_experience\":{\"customerJourneyManagement\":{\"pushChannelContext\":{\"platform\":\"fcm\"},\"messageExecution\":{\"messageExecutionID\":\"16-Sept-postman\",\"journeyVersionInstanceId\":\"someJourneyVersionInstanceId\",\"messageID\":\"567\",\"journeyVersionID\":\"some-journeyVersionId\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/push\"}}}}},\"meta\":{\"collect\":{\"datasetId\":\"mock_datasetId\"}}}";
        final String mockCJMData = "{\n" +
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

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);
        Event mockEvent = new Event.Builder("event1", MessagingConstant.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify
        verify(messagingState, times(1)).getExperienceEventDatasetId();

        PowerMockito.verifyStatic(MobileCore.class);
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        assertNotNull(event.getData());
        assertEquals(MessagingConstant.EventType.EDGE.toLowerCase(), event.getEventType().getName());
        // Verify _experience exist
        assertEquals(expectedEventData, event.getData().toString());
    }

    @Test
    public void test_handleTrackingInfo_when_EventDataNull() {
        // Mocks
        Event mockEvent = mock(Event.class);

        // when
        when(mockEvent.getEventData()).thenReturn(null);

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify
        verify(mockEvent, times(1)).getData();
        verify(messagingState, times(0)).getExperienceEventDatasetId();
    }

    @Test
    public void test_handleTrackingInfo_when_eventTypeIsNull() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify
        // verify
        verify(messagingState, times(0)).getExperienceEventDatasetId();
    }

    @Test
    public void test_handleTrackingInfo_when_MessageIdIsNull() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify
        verify(messagingState, times(0)).getExperienceEventDatasetId();
    }

    // ========================================================================================
    // getExecutor
    // ========================================================================================
    @Test
    public void test_getExecutor_NeverReturnsNull() {
        // test
        ExecutorService executorService = messagingInternal.getExecutor();
        assertNotNull("The executor should not return null", executorService);

        // verify
        assertEquals("Gets the same executor instance on the next get", executorService, messagingInternal.getExecutor());
    }
}