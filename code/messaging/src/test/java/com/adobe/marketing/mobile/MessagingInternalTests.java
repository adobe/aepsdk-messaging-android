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

package com.adobe.marketing.mobile;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.json.JSONObject;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class, ExtensionUnexpectedError.class, MessagingState.class, App.class, Context.class})
public class MessagingInternalTests {

    private MessagingInternal messagingInternal;
    private AndroidPlatformServices platformServices;
    private JsonUtilityService jsonUtilityService;
    private EventHub eventHub;
    private Map<String, Object> mockConfigState = new HashMap<>();

    // Mocks
    @Mock
    ExtensionApi mockExtensionApi;
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
    @Mock
    Core mockCore;
    @Mock
    Activity mockActivity;
    @Mock
    MessagingFullscreenMessage mockFullscreenMessage;
    @Mock
    AndroidPlatformServices mockPlatformServices;
    @Mock
    UIService mockUIService;
    @Mock
    PackageManager packageManager;
    @Mock
    ApplicationInfo applicationInfo;
    @Mock
    Bundle bundle;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(Event.class);
        PowerMockito.mockStatic(App.class);
        platformServices = new AndroidPlatformServices();
        jsonUtilityService = platformServices.getJsonUtilityService();
        eventHub = new EventHub("testEventHub", mockPlatformServices);
        mockCore.eventHub = eventHub;
        when(MobileCore.getCore()).thenReturn(mockCore);
        when(App.getCurrentActivity()).thenReturn(mockActivity);
        when(mockApplication.getApplicationContext()).thenReturn(context);
        when(context.getPackageName()).thenReturn("mock_placement");
        // mocks for getting activity id from manifest
        when(App.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(applicationInfo);
        Whitebox.setInternalState(applicationInfo, "metaData", bundle);
        when(bundle.getString(anyString())).thenReturn("mock_activity");

        when(mockApplication.getPackageName()).thenReturn("mock_placement");
        when(mockPlatformServices.getJsonUtilityService()).thenReturn(jsonUtilityService);
        when(mockPlatformServices.getUIService()).thenReturn(mockUIService);
        when(mockExtensionApi.getSharedEventState(anyString(), any(Event.class), nullable(ExtensionErrorCallback.class))).thenReturn(mockConfigState);
        messagingInternal = new MessagingInternal(mockExtensionApi);
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        mockConfigState.put(MessagingConstants.SharedState.Configuration.ORG_ID, "mock_activity");
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor() {
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // verify 5 listeners are registered
        verify(mockExtensionApi, times(1)).registerEventListener(eq(MessagingConstants.EventType.MESSAGING),
                eq(EventSource.REQUEST_CONTENT.getName()), eq(ListenerMessagingRequestContent.class), any(ExtensionErrorCallback.class));

        verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.GENERIC_IDENTITY.getName()),
                eq(EventSource.REQUEST_CONTENT.getName()), eq(ListenerIdentityRequestContent.class), any(ExtensionErrorCallback.class));

        verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.HUB.getName()),
                eq(EventSource.SHARED_STATE.getName()), eq(ListenerHubSharedState.class), any(ExtensionErrorCallback.class));

        verify(mockExtensionApi, times(1)).registerEventListener(eq(MessagingConstants.EventType.EDGE),
                eq(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS), eq(ListenerOffersPersonalizationDecisions.class), any(ExtensionErrorCallback.class));

        verify(mockExtensionApi, times(1)).registerEventListener(eq(EventType.RULES_ENGINE.getName()),
                eq(EventSource.RESPONSE_CONTENT.getName()), eq(ListenerRulesEngineResponseContent.class), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        // test
        String moduleName = messagingInternal.getName();
        assertEquals("getName should return the correct module name", MessagingConstants.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = messagingInternal.getVersion();
        assertEquals("getVesion should return the correct module version", MessagingConstants.EXTENSION_VERSION,
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
    // processHubSharedState
    // ========================================================================================
    @Test
    public void test_processHubSharedState() {
        //Mocks
        EventData data = new EventData();
        data.putString(MessagingConstants.EventDataKeys.STATE_OWNER, MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME);
        Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").setData(data).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "eventQueue", mockEventQueue);

        // Test
        messagingInternal.processHubSharedState(mockEvent);
        
        // Verify
        verify(mockEventQueue, times(1)).isEmpty();
    }
    
    @Test
    public void test_processHubSharedState_NoMatchingStateOwner() {
        //Mocks
        EventData data = new EventData();
        data.putString(MessagingConstants.EventDataKeys.STATE_OWNER, "somerandomstateowner");
        Event mockEvent = new Event.Builder("event 2", "eventType", "eventSource").setData(data).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "eventQueue", mockEventQueue);

        // Test
        messagingInternal.processHubSharedState(mockEvent);

        // Verify
        verify(mockEventQueue, times(0)).isEmpty();
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
        verify(mockExtensionApi, times(0)).getSharedEventState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, mockEvent, mockCallback);
        verify(mockExtensionApi, times(0)).getXDMSharedEventState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, mockEvent, mockCallback);
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
        verify(mockEvent, times(5)).getType();
        assertEquals(0, messagingInternal.getEventQueue().size());
    }

    @Test
    public void test_processEvents_with_genericIdentityEvent() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("somekey", "somedata");

        Event mockEvent = mock(Event.class);

        // when mock event getType called return GENERIC_IDENTITY
        when(mockEvent.getType()).thenReturn(EventType.GENERIC_IDENTITY.getName());

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        when(mockEvent.getEventData()).thenReturn(eventData);

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
        verify(mockEvent, times(2)).getType();
        verify(mockEvent, times(1)).getSource();
        verify(mockEvent, times(4)).getEventData();
        assertEquals(0, messagingInternal.getEventQueue().size());
    }

    @Test
    public void test_processEvents_with_messagingEventType() {
        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("somekey", "somedata");

        Event mockEvent = mock(Event.class);

        // when mock event getType called return MESSAGING
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        when(mockEvent.getEventData()).thenReturn(eventData);

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
        verify(mockEvent, times(3)).getType();
        verify(mockEvent, times(2)).getSource();
        verify(mockEvent, times(1)).getData();
        assertEquals(0, messagingInternal.getEventQueue().size());
    }

    // ========================================================================================
    // handlePushToken
    // ========================================================================================
    @Test
    public void test_handlePushToken() {
        // expected
        final String expectedEventData = "{\"data\":{\"pushNotificationDetails\":[{\"denylisted\":false,\"identity\":{\"namespace\":{\"code\":\"ECID\"},\"id\":\"mock_ecid\"},\"appID\":\"mock_placement\",\"platform\":\"fcm\",\"token\":\"mock_push_token\"}]}}";

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // Mocks
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_push_token");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();
        String mockECID = "mock_ecid";

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        // when App.getApplication().getPackageName() return mock packageName
        when(App.getApplication()).thenReturn(mockApplication);
        when(mockApplication.getPackageName()).thenReturn("mock_placement");

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        // 2 events dispatched: Offers iam fetch event when extension is registered + edge event with push profile data
        PowerMockito.verifyStatic(MobileCore.class, times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        assertNotNull(event.getData());
        assertEquals(MessagingConstants.EventName.MESSAGING_PUSH_PROFILE_EDGE_EVENT, event.getName());
        assertEquals(MessagingConstants.EventType.EDGE.toLowerCase(), event.getEventType().getName());
        assertEquals(EventSource.REQUEST_CONTENT.getName(), event.getSource());
        assertEquals(expectedEventData, event.getData().toString());

        // verify if the push token is stored in shared state
        verify(mockExtensionApi, times(1)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
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
        // 1 event dispatched: Offers iam fetch event when extension is registered
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handlePushToken_when_tokenIsEmpty() {
        // Mocks
        String mockECID = "mock_ecid";
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "");
        Event mockEvent = new Event.Builder("event1", EventType.GENERIC_DATA.getName(), EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        // 1 event dispatched: Offers iam fetch event when extension is registered
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handlePushToken_when_ecidIsNull() {
        // Mocks
        String mockECID = null;
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, "mock_token");
        Event mockEvent = mock(Event.class);

        // when
        when(mockEvent.getEventData()).thenReturn(eventData);

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        // when - then return mock
        when(messagingState.getEcid()).thenReturn(mockECID);

        //test
        messagingInternal.handlePushToken(mockEvent);

        // verify
        // 1 event dispatched: Offers iam fetch event when extension is registered
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(0)).setSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
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
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);
        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify experience event dataset id
        verify(messagingState, times(1)).getExperienceEventDatasetId();

        // verify dispatch event is called
        // 2 events dispatched: Offers iam fetch event when extension is registered + edge event with tracking info
        PowerMockito.verifyStatic(MobileCore.class, times(2));
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
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, true);
        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify experience event dataset id
        verify(messagingState, times(1)).getExperienceEventDatasetId();

        // verify dispatch event is called
        // 2 events dispatched: Offers iam fetch event when extension is registered + edge event with tracking info
        PowerMockito.verifyStatic(MobileCore.class, times(2));
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
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mock_messageId");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, "mock_actionId");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, "mock_application_opened");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockCJMData);
        Event mockEvent = new Event.Builder("event1", MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName()).setEventData(eventData).build();

        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);

        when(messagingState.getExperienceEventDatasetId()).thenReturn("mock_datasetId");

        //test
        messagingInternal.handleTrackingInfo(mockEvent);

        // verify
        verify(messagingState, times(1)).getExperienceEventDatasetId();

        // 2 events dispatched: Offers iam fetch event when extension is registered + edge event with tracking info
        PowerMockito.verifyStatic(MobileCore.class, times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        assertNotNull(event.getData());
        assertEquals(MessagingConstants.EventType.EDGE.toLowerCase(), event.getEventType().getName());
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
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
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
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "mock_eventType");
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
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

    // ========================================================================================
    // IAM rules retrieval from Offers
    // ========================================================================================
    @Test
    public void test_fetchMessages_CalledOnExtensionStart() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // expected dispatched event data
        String expectedOffersEventData = "{\"decisionscopes\":[{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}],\"type\":\"prefetch\"}";

        // verify dispatch event is called
        // 1 event dispatched: Offers iam fetch event when extension is registered
        PowerMockito.verifyStatic(MobileCore.class, times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify events
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
    }

    @Test
    public void test_refreshInAppMessages_Invoked() {
        // setup
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        // trigger event
        EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);
        // expected dispatched event data
        String expectedOffersEventData = "{\"decisionscopes\":[{\"activityId\":\"mock_activity\",\"placementId\":\"mock_placement\",\"itemCount\":30}],\"type\":\"prefetch\"}";

        // Mocks
        Event mockEvent = mock(Event.class);
        // when mock event getType called return MESSAGING
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.MESSAGING);

        // when mock event getSource called return REQUEST_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.REQUEST_CONTENT.getName());

        // when get eventData called return data with "REFRESH_MESSAGES, true"
        when(mockEvent.getEventData()).thenReturn(eventData.toObjectMap());

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify dispatch event is called
        // 2 events dispatched: Offers iam fetch event when extension is registered + Offers iam fetch event when refresh in app messages event is received
        PowerMockito.verifyStatic(MobileCore.class, times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify events
        Event event = eventCaptor.getAllValues().get(0);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
        event = eventCaptor.getAllValues().get(1);
        assertNotNull(event.getData());
        assertEquals(expectedOffersEventData, event.getData().toString());
    }

    // ========================================================================================
    // Offers rules payload processing
    // ========================================================================================
    @Test
    public void test_handleEdgeResponseEvent_ValidOffersIAMPayloadPresent() throws Exception {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");
        // expected messaging consequence payload
        String expectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing a valid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        Map.Entry<Module, ConcurrentLinkedQueue<Rule>> entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        Rule loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(expectedIAMPayload));
    }

    @Test
    public void test_handleEdgeResponseEvent_MultipleValidOffersIAMPayloadPresent() throws Exception {
        // setup
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage2\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me2<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");
        // expected messaging consequence payloads
        String expectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        String secondExpectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me2</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing multiple valid offers iam payloads
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        Map.Entry<Module, ConcurrentLinkedQueue<Rule>> entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        assertEquals(2, entry.getValue().size());
        Rule loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(expectedIAMPayload));
        entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(secondExpectedIAMPayload));
    }

    @Test
    public void test_handleEdgeResponseEvent_OneInvalidIAMPayloadPresent() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage2\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me2<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");
        // expected messaging consequence payloads
        String expectedIAMPayload = "{\n" +
                "        \"triggeredconsequence\" : {\n" +
                "            \"id\" : \"341800180\",\n" +
                "            \"detail\" : {\n" +
                "                \"template\" : \"fullscreen\",\n" +
                "                \"html\" : \"<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING MESSAGE 2 POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me2</a></h1></body></html>\",\n" +
                "                \"remoteAssets\" : [ ]\n" +
                "            },\n" +
                "            \"type\" : \"cjmiam\"\n" +
                "        }\n" +
                "    }";

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing one valid and one invalid offers iam payloads
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify rule loaded
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        Map.Entry<Module, ConcurrentLinkedQueue<Rule>> entry = (Map.Entry) loadedRules.entrySet().iterator().next();
        assertEquals(1, entry.getValue().size());
        Rule loadedRule = entry.getValue().remove();
        assertTrue(loadedRule.toString().contains(expectedIAMPayload));
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageId() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"type\\\": \\\"cjmiam\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rules loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageType() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"detail\\\": { \\\"remoteAssets\\\": [], \\\"html\\\": \\\"<html><head><\\/head><body bgcolor=\\\\\\\"black\\\\\\\"><br \\/><br \\/><br \\/><br \\/><br \\/><br \\/><h1 align=\\\\\\\"center\\\\\\\" style=\\\\\\\"color: white;\\\\\\\">IN-APP MESSAGING POWERED BY <br \\/>OFFER DECISIONING<\\/h1><h1 align=\\\\\\\"center\\\\\\\"><a style=\\\\\\\"color: white;\\\\\\\" href=\\\\\\\"adbinapp:\\/\\/cancel\\\\\\\" >dismiss me<\\/a><\\/h1><\\/body><\\/html>\\\", \\\"template\\\": \\\"fullscreen\\\" } } ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadMissingMessageDetail() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadIsEmpty() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadHasInvalidActivityId() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"invalid\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"mock_placement\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }

    @Test
    public void test_handleEdgeResponseEvent_OffersIAMPayloadHasInvalidPlacementId() throws Exception {
        // setup
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        // trigger event
        HashMap<String, Object> eventData = new HashMap<>();
        eventData.put("type", "personalization:decisions");
        eventData.put("requestEventId", "2E964037-E319-4D14-98B8-0682374E547B");
        JSONObject payload = new JSONObject("    {\n" +
                "      \"activity\" : {\n" +
                "        \"id\" : \"mock_activity\",\n" +
                "        \"etag\" : \"2\"\n" +
                "      },\n" +
                "      \"scope\" : \"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTMyM2RiZTk0ZjJlZWY5MyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjEzMjNkOWViNDNhYWNhZGEiLCJpdGVtQ291bnQiOjMwfQ==\",\n" +
                "      \"placement\" : {\n" +
                "        \"id\" : \"invalid\",\n" +
                "        \"etag\" : \"1\"\n" +
                "      },\n" +
                "      \"items\" : [\n" +
                "        {\n" +
                "          \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "          \"data\" : {\n" +
                "            \"id\" : \"xcore:fallback-offer:1323dbbc1c6eef91\",\n" +
                "            \"format\" : \"application\\/json\",\n" +
                "            \"content\" : \"{ \\\"version\\\": 1, \\\"rules\\\": [ { \\\"condition\\\": { \\\"type\\\": \\\"group\\\", \\\"definition\\\": { \\\"logic\\\": \\\"and\\\", \\\"conditions\\\": [ { \\\"definition\\\": { \\\"key\\\": \\\"contextdata.testShowMessage\\\", \\\"matcher\\\": \\\"eq\\\", \\\"values\\\": [ \\\"true\\\" ] }, \\\"type\\\": \\\"matcher\\\" } ] } }, \\\"consequences\\\": [ { \\\"id\\\": \\\"341800180\\\", \\\"type\\\": \\\"cjmiam\\\"} ] } ] }\"\n" +
                "          },\n" +
                "          \"etag\" : \"1\",\n" +
                "          \"schema\" : \"https:\\/\\/ns.adobe.com\\/experience\\/offer-management\\/content-component-json\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"id\" : \"cb25ecb0-d085-44ac-b73d-797a3265d37c\"\n" +
                "    }\n" +
                "  ]");
        eventData.put("payload", payload);
        eventData.put("requestId", "D158979E-0506-4968-8031-17A6A8A87DA8");

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return EDGE
        when(mockEvent.getType()).thenReturn(MessagingConstants.EventType.EDGE);

        // when mock event getSource called return PERSONALIZATION_DECISIONS
        when(mockEvent.getSource()).thenReturn(MessagingConstants.EventSource.PERSONALIZATION_DECISIONS);

        // when get eventData called return event data containing an invalid offers iam payload
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify no rule loaded for messaging extension
        ConcurrentHashMap loadedRules = mockCore.eventHub.getModuleRuleAssociation();
        assertEquals(0, loadedRules.size());
    }
    // ========================================================================================
    // handling rules response events
    // ========================================================================================
    @Test
    public void test_handleRulesResponseEvent_ValidConsequencePresent() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
        Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Object.class))).thenReturn(mockFullscreenMessage);
        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a valid rules response content event with a triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is called
        verify(mockFullscreenMessage, times(1)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_UnsupportedConsequenceType() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, "unknownIamType");
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_MissingConsequenceType() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    public void test_handleRulesResponseEvent_NullConsequences() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, null);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_EmptyConsequences() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_EmptyDetails() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_NullDetails() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, null);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_TemplateUnsupported() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "alert");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_TemplateMissing() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "<html><head></head><body bgcolor=\"black\"><br /><br /><br /><br /><br /><br /><h1 align=\"center\" style=\"color: white;\">IN-APP MESSAGING POWERED BY <br />OFFER DECISIONING</h1><h1 align=\"center\"><a style=\"color: white;\" href=\"adbinapp://cancel\" >dismiss me</a></h1></body></html>");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_EmptyHTML() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML, "");
                consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_MissingHTML() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "123456789");
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    @Test
    public void test_handleRulesResponseEvent_InvalidConsequenceDetails_MissingMessageId() {
        // setup
        MobileCore.setApplication(App.getApplication());
        // private mocks
        Whitebox.setInternalState(messagingInternal, "messagingState", messagingState);
                Mockito.when(mockUIService.createFullscreenMessage(any(String.class), any(UIService.FullscreenMessageDelegate.class), any(boolean.class), any(Extension.class))).thenReturn(mockFullscreenMessage);

        // trigger event
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE, "fullscreen");
        details.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, new ArrayList<String>());
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, details);
        consequence.put(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);

        // Mocks
        Event mockEvent = mock(Event.class);

        // when mock event getType called return RULES_ENGINE
        when(mockEvent.getType()).thenReturn(EventType.RULES_ENGINE.getName());

        // when mock event getSource called return RESPONSE_CONTENT
        when(mockEvent.getSource()).thenReturn(EventSource.RESPONSE_CONTENT.getName());

        // when get eventData called return event data containing a rules response content event with an invalid triggered consequence
        when(mockEvent.getEventData()).thenReturn(eventData);

        // test
        messagingInternal.queueEvent(mockEvent);
        messagingInternal.processEvents();

        // verify MessagingFullscreenMessage.show() is not called
        verify(mockFullscreenMessage, times(0)).show();
    }

    // ========================================================================================
    // Helpers
    // ========================================================================================
    private void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            fail("thread sleep error: " + e.getLocalizedMessage());
        }
    }
}