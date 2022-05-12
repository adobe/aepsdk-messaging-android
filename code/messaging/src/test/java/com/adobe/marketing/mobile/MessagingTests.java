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

import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_GOOGLE_MESSAGE_ID;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataValues.EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED;
import static com.adobe.marketing.mobile.MessagingTestConstants.EventDataValues.EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class, Intent.class, App.class})
public class MessagingTests {
    Map<String, String> remoteMessageData = new HashMap<String, String>() {
        {
            put(MessagingTestConstants.PushNotificationPayload.ADB, "true");
            put(MessagingTestConstants.PushNotificationPayload.TITLE, "mockTitle");
            put(MessagingTestConstants.PushNotificationPayload.BODY, "mockBody");
            put(MessagingTestConstants.PushNotificationPayload.SOUND, "mockSound");
            put(MessagingTestConstants.PushNotificationPayload.CHANNEL_ID, "mockChannelId");
            put(MessagingTestConstants.PushNotificationPayload.ICON, "mockIcon");
            put(MessagingTestConstants.PushNotificationPayload.ACTION_URI, "mockAction");
            put(MessagingTestConstants.PushNotificationPayload.IMAGE_URL, "mockImageUrl");
            put(MessagingTestConstants.PushNotificationPayload.NOTIFICATION_COUNT, "10");
            put(MessagingTestConstants.PushNotificationPayload.NOTIFICATION_PRIORITY, "default");
            put(MessagingTestConstants.PushNotificationPayload.ACTION_TYPE, "CLICKED");
            put(MessagingTestConstants.PushNotificationPayload.ACTION_BUTTONS, "mockButtons");
        }
    };

    // interface implementations for testing
    class TestPushNotificationFactory implements IMessagingPushNotificationFactory {
        @Override
        public Notification create(Context context, MessagingPushPayload payload, String messageId, int notificationId, boolean shouldHandleTracking) {
            return null;
        }
    }

    class TestImageDownloader implements IMessagingImageDownloader {

        @Override
        public Bitmap getBitmapFromUrl(Context context, String imageUrl) {
            return null;
        }
    }

    @Mock
    Intent mockIntent;
    @Mock
    Context context;

    @Before
    public void before() {
        PowerMockito.mockStatic(MobileCore.class);
        PowerMockito.mockStatic(App.class);
        Whitebox.setInternalState(Messaging.class, "notificationFactory", (IMessagingPushNotificationFactory) null);
        Whitebox.setInternalState(Messaging.class, "imageDownloader", (IMessagingImageDownloader) null);
    }

    // ========================================================================================
    // extensionVersion
    // ========================================================================================

    @Test
    public void test_extensionVersionAPI() {
        // test
        String extensionVersion = Messaging.extensionVersion();
        Assert.assertEquals("The Extension version API returns the correct value", MessagingConstants.EXTENSION_VERSION,
                extensionVersion);
    }

    // ========================================================================================
    // registerExtension
    // ========================================================================================

    @Test
    public void test_registerExtensionAPI() {
        // test
        Messaging.registerExtension();
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // The monitor extension should register with core
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.registerExtension(ArgumentMatchers.eq(MessagingInternal.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
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
        Assert.assertFalse(done);
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
        verify(mockIntent, times(1)).putExtra(TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verify(mockIntent, times(1)).putExtra(TRACK_INFO_KEY_ADOBE_XDM, mockXDMData);
    }

    @Test
    public void test_addPushTrackingDetailsWithEmptyData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = new HashMap<>();

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetailsWithNullData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = null;

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertFalse(done);
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
        Assert.assertFalse(done);
    }

    @Test
    public void test_addPushTrackingDetails_NullXdmData() {
        String mockMessageId = "mockMessageId";
        Map<String, String> mockDataMap = new HashMap<>();
        mockDataMap.put(MessagingTestConstants.TrackingKeys._XDM, null);

        // test
        boolean done = Messaging.addPushTrackingDetails(mockIntent, mockMessageId, mockDataMap);

        // verify
        Assert.assertTrue(done);
        verify(mockIntent, times(1)).putExtra(TRACK_INFO_KEY_MESSAGE_ID, mockMessageId);
        verifyNoMoreInteractions(mockIntent);
    }

    // ========================================================================================
    // handleNotificationResponse
    // ========================================================================================
    @Test
    public void test_handleNotificationResponse_WhenParamsAreNull() {
        // test
        Messaging.handleNotificationResponse(null, false, null);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleNotificationResponse_NullMessageAndGoogleId() {
        String mockActionId = "mockActionId";
        String mockXdm = "mockXdm";

        try {
            PowerMockito.whenNew(Intent.class)
                    .withNoArguments().thenReturn(mockIntent);
        } catch (Exception e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "Intent exception");
        }

        when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);
        when(mockIntent.getStringExtra(TRACK_INFO_KEY_MESSAGE_ID)).thenReturn(null);
        when(mockIntent.getStringExtra(TRACK_INFO_KEY_GOOGLE_MESSAGE_ID)).thenReturn(null);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleNotificationResponse_NullMessageId_ThenGoogleMessageIdUsed() {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        String mockActionId = "mockActionId";
        String mockXdm = "mockXdm";
        String mockGoogleMessageId = "mockGoogleMessageId";

        try {
            PowerMockito.whenNew(Intent.class)
                    .withNoArguments().thenReturn(mockIntent);
        } catch (Exception e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "Intent exception");
        }

        when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);
        when(mockIntent.getStringExtra(TRACK_INFO_KEY_MESSAGE_ID)).thenReturn(null);
        when(mockIntent.getStringExtra(TRACK_INFO_KEY_GOOGLE_MESSAGE_ID)).thenReturn(mockGoogleMessageId);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

        // verify
        verify(mockIntent, times(3)).getStringExtra(anyString());

        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        EventData eventData = event.getData();
        assertNotNull(eventData);
        assertEquals(MessagingTestConstants.EventType.MESSAGING.toLowerCase(), event.getEventType().getName());
        try {
            assertEquals(mockGoogleMessageId, eventData.getString2(TRACK_INFO_KEY_MESSAGE_ID));
            assertEquals(mockActionId, eventData.getString2(TRACK_INFO_KEY_ACTION_ID));
            assertEquals(EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION, eventData.getString2(TRACK_INFO_KEY_EVENT_TYPE));
        } catch (VariantException e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "getString2 variant exception, error : %s", e.getMessage());
        }
    }

    @Test
    public void test_handleNotificationResponse_NullCustomActionId_ThenActionApplicationOpenedEventTypeUsed() {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        String mockXdm = "mockXdm";
        String mockMessageId = "mockMessageId";

        try {
            PowerMockito.whenNew(Intent.class)
                    .withNoArguments().thenReturn(mockIntent);
        } catch (Exception e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "Intent exception");
        }

        when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);
        when(mockIntent.getStringExtra(TRACK_INFO_KEY_MESSAGE_ID)).thenReturn(mockMessageId);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, null);

        // verify
        verify(mockIntent, times(2)).getStringExtra(anyString());

        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        EventData eventData = event.getData();
        assertNotNull(eventData);
        assertEquals(MessagingTestConstants.EventType.MESSAGING.toLowerCase(), event.getEventType().getName());
        try {
            assertEquals(TRACK_INFO_KEY_APPLICATION_OPENED, eventData.getString2(TRACK_INFO_KEY_ACTION_ID));
            assertEquals(EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED, eventData.getString2(TRACK_INFO_KEY_EVENT_TYPE));
            assertEquals(mockMessageId, eventData.getString2(TRACK_INFO_KEY_MESSAGE_ID));
        } catch (VariantException e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "getString2 variant exception, error : %s", e.getMessage());
        }
    }

    @Test
    public void test_handleNotificationResponse() {
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        String mockActionId = "mockActionId";
        String mockMessageId = "mockMessageId";
        String mockXdm = "mockXdm";

        try {
            PowerMockito.whenNew(Intent.class)
                    .withNoArguments().thenReturn(mockIntent);
        } catch (Exception e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "Intent exception");
        }

        when(mockIntent.getStringExtra(anyString())).thenReturn(mockXdm);
        when(mockIntent.getStringExtra(TRACK_INFO_KEY_MESSAGE_ID)).thenReturn(mockMessageId);

        // test
        Messaging.handleNotificationResponse(mockIntent, true, mockActionId);

        // verify
        verify(mockIntent, times(2)).getStringExtra(anyString());

        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify event
        Event event = eventCaptor.getValue();
        EventData eventData = event.getData();
        assertNotNull(eventData);
        assertEquals(MessagingTestConstants.EventType.MESSAGING.toLowerCase(), event.getEventType().getName());
        try {
            assertEquals(mockActionId, eventData.getString2(TRACK_INFO_KEY_ACTION_ID));
            assertEquals(EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION, eventData.getString2(TRACK_INFO_KEY_EVENT_TYPE));
            assertEquals(mockMessageId, eventData.getString2(TRACK_INFO_KEY_MESSAGE_ID));
        } catch (VariantException e) {
            com.adobe.marketing.mobile.Log.debug("MessagingTest", "getString2 variant exception, error : %s", e.getMessage());
        }
    }

    // ========================================================================================
    // handlePushNotificationWithRemoteMessage
    // ========================================================================================
    @Test
    public void test_handlePushNotificationWithRemoteMessage_WhenRemoteMessageIsNull() {
        // test
        boolean success = Messaging.handlePushNotificationWithRemoteMessage(null, false);
        // verify
        assertFalse(success);
    }

    @Test
    public void test_handlePushNotificationWithRemoteMessage_WhenMessagePayloadIsEmpty() {
        // setup
        App.setAppContext(context);
        RemoteMessage remoteMessage = new RemoteMessage.Builder("test").build();
        Whitebox.setInternalState(remoteMessage, "data", Collections.EMPTY_MAP);

        // test
        boolean success = Messaging.handlePushNotificationWithRemoteMessage(remoteMessage, false);

        // verify
        assertFalse(success);
    }

    @Test
    public void test_handlePushNotificationWithRemoteMessage() {
        // setup
        App.setAppContext(context);
        RemoteMessage remoteMessage = new RemoteMessage.Builder("test").build();
        Whitebox.setInternalState(remoteMessage, "data", remoteMessageData);

        // test
        boolean success = Messaging.handlePushNotificationWithRemoteMessage(remoteMessage, false);
        // verify
        assertTrue(success);
    }

    // ========================================================================================
    // set/getPushNotificationFactory
    // ========================================================================================
    @Test
    public void test_setPushNotificationFactory() {
        // setup
        IMessagingPushNotificationFactory factory = new TestPushNotificationFactory();
        // test
        Messaging.setPushNotificationFactory(factory);
        // verify
        assertEquals(factory, Messaging.getPushNotificationFactory());
    }

    @Test
    public void test_setNullPushNotificationFactory() {
        // test
        Messaging.setPushNotificationFactory(null);
        // verify
        assertNull(Messaging.getPushNotificationFactory());
    }

    // ========================================================================================
    // set/getPushImageDownloader
    // ========================================================================================
    @Test
    public void test_setPushImageDownloader() {
        // setup
        IMessagingImageDownloader downloader = new TestImageDownloader();
        // test
        Messaging.setPushImageDownloader(downloader);
        // verify
        assertEquals(downloader, Messaging.getImageDownloader());
    }

    @Test
    public void test_setNullPushImageDownloader() {
        // test
        Messaging.setPushImageDownloader(null);
        // verify
        assertNull(Messaging.getImageDownloader());
    }
}
