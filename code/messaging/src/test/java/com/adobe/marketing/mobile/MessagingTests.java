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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


import android.content.Intent;

import com.adobe.marketing.mobile.messaging.PushTrackingStatus;
import com.adobe.marketing.mobile.messaging.internal.MessagingExtension;
import com.adobe.marketing.mobile.messaging.internal.MessagingTestConstants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingTests {
    @Mock
    Intent mockIntent;
    MockedStatic<MobileCore> mobileCore;
    ArgumentCaptor<AdobeCallbackWithError<Event>> callbackWithErrorArgumentCaptor;
    ArgumentCaptor<Event> dispatchEventCaptor;
    CountDownLatch latch;
    PushTrackingStatus[] capturedStatus = new PushTrackingStatus[1];
    @Before
    public void before() {
        latch = new CountDownLatch(1);
        capturedStatus = new PushTrackingStatus[1];

        // Mock MobileCore
        dispatchEventCaptor = ArgumentCaptor.forClass(Event.class);
        callbackWithErrorArgumentCaptor = ArgumentCaptor.forClass(AdobeCallbackWithError.class);

        mobileCore = mockStatic(MobileCore.class);

        mobileCore.when(() -> MobileCore.dispatchEventWithResponseCallback(dispatchEventCaptor.capture(), anyLong(),callbackWithErrorArgumentCaptor.capture())).thenCallRealMethod();
        mobileCore.when(() -> MobileCore.dispatchEvent(dispatchEventCaptor.capture())).thenCallRealMethod();

    }

    @After
    public void after() {
        mobileCore.close();
        dispatchEventCaptor = null;
        callbackWithErrorArgumentCaptor = null;
        capturedStatus = null;
        latch = null;
    }


    // ========================================================================================
    // extensionVersion
    // ========================================================================================

    @Test
    public void test_extensionVersionAPI() {
        // test
        String extensionVersion = Messaging.extensionVersion();
        Assert.assertEquals("The Extension version API returns the correct value", MessagingTestConstants.EXTENSION_VERSION,
                extensionVersion);
    }

    // ========================================================================================
    // registerExtension
    // ========================================================================================

    @Test
    public void test_registerExtensionAPI() {
        final ArgumentCaptor<ExtensionErrorCallback<ExtensionError>> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        Messaging.registerExtension();

        // The monitor extension should register with core
        mobileCore.verify(() -> MobileCore.registerExtension(any(), callbackCaptor.capture()));
        MobileCore.registerExtension(ArgumentMatchers.eq(MessagingExtension.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getAllValues().get(0);
        Assert.assertNotNull("The extension callback should not be null", extensionErrorCallback);

        // should not crash on calling the callback
        extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    // ========================================================================================
    // addPushTrackingDetails
    // ========================================================================================
    @Test
    public void test_addPushTrackingDetails_WhenParamsAreNull() {
        // test
        boolean done = Messaging.addPushTrackingDetails(null, null, null);

        // verify
        assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetails() {
        String mockMessageId = "mockMessageId";
        String mockXDMData = "mockXDMData";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, mockXDMData);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertTrue(done);
        verify(mockIntent, times(1)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verify(mockIntent, times(1)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, mockXDMData);
    }

    @Test
    public void test_addPushTrackingDetailsNoXdmData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, null);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertTrue(done);
        verify(mockIntent, times(1)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verify(mockIntent, times(0)).putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, "");
    }

    @Test
    public void test_addPushTrackingDetailsWithEmptyData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = new HashMap<>();

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetailsWithNullData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = null;

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetailsWithEmptyMessageId() {
        String mockMessageId = "";
        String mockXDMData = "mockXDMData";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, mockXDMData);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        assertFalse(done);
    }

    // ========================================================================================
    // handleNotificationResponse
    // ========================================================================================
    @Test
    public void test_handleNotificationResponse_WhenParamsAreNull() throws InterruptedException {
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final CountDownLatch latch = new CountDownLatch(1);
        final PushTrackingStatus[] capturedStatus = new PushTrackingStatus[1];
        // test
        Messaging.handleNotificationResponse(null, false, null, new AdobeCallback<PushTrackingStatus>() {
            @Override
            public void call(PushTrackingStatus trackingStatus) {
                latch.countDown();
                capturedStatus[0] = trackingStatus;
            }
        });

        // verify
        latch.await(2, TimeUnit.SECONDS);
        assertEquals(PushTrackingStatus.INVALID_INTENT, capturedStatus[0]);
        verifyNoInteractions(MobileCore.class);
    }

    @Test
    public void test_handleNotificationResponse() {
            String mockActionId = "mockActionId";
            String mockXdm = "mockXdm";

            when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);

            // test
            Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

            // verify
            verify(mockIntent, times(2)).getStringExtra(anyString());
            mobileCore.verify(() -> MobileCore.dispatchEventWithResponseCallback(any(),anyLong(),any()));

            // verify event
            Event event = dispatchEventCaptor.getValue();
            Map<String, Object> eventData = event.getEventData();
            assertNotNull(eventData);
            assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
            assertEquals(eventData.get("actionId"), mockActionId);
    }

    @Test
    public void test_handleNotificationResponseNoXdmData() throws Exception {
        String mockActionId = "mockActionId";
        String messageId = "messageId";
        when(mockIntent.getStringExtra(ArgumentMatchers.contains("messageId"))).thenReturn(messageId);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId, new AdobeCallback<PushTrackingStatus>() {
            @Override
            public void call(PushTrackingStatus trackingStatus) {
                latch.countDown();
                capturedStatus[0] = trackingStatus;
            }
        });

        // verify no event was sent
        latch.await(2, TimeUnit.SECONDS);
        assertEquals(PushTrackingStatus.NO_TRACKING_DATA, capturedStatus[0]);
        verifyNoInteractions(MobileCore.class);
    }

    @Test
    public void test_handleNotificationResponse_EventDispatchError() throws Exception{
        String mockActionId = "mockActionId";
        String mockXdm = "mockXdm";
        when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId, new AdobeCallback<PushTrackingStatus>() {
            @Override
            public void call(PushTrackingStatus trackingStatus) {
                latch.countDown();
                capturedStatus[0] = trackingStatus;
            }
        });

        // verify
        mobileCore.verify(() -> MobileCore.dispatchEventWithResponseCallback(any(),anyLong(),any()));

        // verify event
        Event event = dispatchEventCaptor.getAllValues().get(0);
        Map<String, Object> eventData = event.getEventData();
        assertNotNull(eventData);
        assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
        assertEquals(eventData.get("actionId"), mockActionId);

        // no exception should occur when triggering unexpected error callback
        callbackWithErrorArgumentCaptor.getAllValues().get(0).fail(AdobeError.UNEXPECTED_ERROR);

        // verify the return value
        latch.await(2, TimeUnit.SECONDS);
        assertEquals(PushTrackingStatus.UNKNOWN_ERROR, capturedStatus[0]);
    }

    @Test
    public void test_handleNotificationResponseWithEmptyMessageId() {
        String mockActionId = "mockActionId";
        String messageId = "";

        when(mockIntent.getStringExtra(anyString())).thenReturn(messageId);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId, new AdobeCallback<PushTrackingStatus>() {
            @Override
            public void call(PushTrackingStatus trackingStatus) {
                latch.countDown();
                capturedStatus[0] = trackingStatus;
            }
        });

        // verify
        assertEquals(PushTrackingStatus.INVALID_MESSAGE_ID, capturedStatus[0]);
        verify(mockIntent, times(2)).getStringExtra(anyString());
        verifyNoInteractions(MobileCore.class);
    }

    @Test
    public void test_handleNotificationResponseWithEmptyAction() {
        String mockActionId = "";
        String messageId = "mockXdm";

        when(mockIntent.getStringExtra(anyString())).thenReturn(messageId);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

        // verify
        verify(mockIntent, times(2)).getStringExtra(anyString());

        // verify event
        Event event = dispatchEventCaptor.getAllValues().get(0);
        Map<String, Object> eventData = event.getEventData();
        assertNotNull(eventData);
        assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
        assertEquals("", mockActionId);
    }

    // ========================================================================================
    // refreshInAppMessage
    // ========================================================================================
    @Test
    public void test_refreshInAppMessage() {
        // test
        Messaging.refreshInAppMessages();

        // verify event
        Event event = dispatchEventCaptor.getValue();
        Map<String, Object> eventData = event.getEventData();
        assertNotNull(eventData);
        assertEquals(MessagingTestConstants.EventType.MESSAGING, event.getType());
        assertEquals(MessagingTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        assertEquals(MessagingTestConstants.EventName.REFRESH_MESSAGES_EVENT, event.getName());
    }
}
