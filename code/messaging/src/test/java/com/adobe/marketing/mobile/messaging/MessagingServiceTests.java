/*
  Copyright 2023 Adobe. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Context;
import androidx.core.app.NotificationManagerCompat;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.PushNotificationListener;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingServiceTests {
    @Mock RemoteMessage remoteMessage;
    @Mock Context context;
    @Mock NotificationManagerCompat notificationManager;
    @Mock Notification notification;
    MockedStatic<MobileCore> mobileCore;
    MockedStatic<NotificationManagerCompat> notificationManagerCompat;
    MockedStatic<MessagingPushBuilder> pushBuilder;

    @Before
    public void before() {

        // Mock NotificationManager
        notificationManagerCompat = mockStatic(NotificationManagerCompat.class);
        notificationManagerCompat
                .when(() -> NotificationManagerCompat.from(any(Context.class)))
                .thenReturn(notificationManager);
        doNothing().when(notificationManager).notify(anyInt(), any());

        // Mock MobileCore
        mobileCore = mockStatic(MobileCore.class);

        // Mock PushNotificationBuilder
        pushBuilder = mockStatic(MessagingPushBuilder.class);
        pushBuilder
                .when(
                        () ->
                                MessagingPushBuilder.build(
                                        any(MessagingPushPayload.class), any(Context.class)))
                .thenReturn(notification);

        when(remoteMessage.getMessageId()).thenReturn("someMessageID");
    }

    @After
    public void clean() {
        mobileCore.close();
        pushBuilder.close();
        notificationManagerCompat.close();
    }

    @Test
    public void test_onNewToken_SetsPushIdentifierWhenTokenIsValid() {
        // setup
        String validToken = "valid_token";
        MessagingService messagingService = new MessagingService();

        // test
        messagingService.onNewToken(validToken);

        // verify
        mobileCore.verify(() -> MobileCore.setPushIdentifier(validToken));
    }

    @Test
    public void test_onMessageReceived_HandlesRemoteMessage() {
        try (MockedStatic<MessagingService> messagingServiceMockedStatic =
                Mockito.mockStatic(MessagingService.class)) {
            // setup
            messagingServiceMockedStatic
                    .when(
                            () ->
                                    MessagingService.handleRemoteMessage(
                                            any(Context.class), any(RemoteMessage.class)))
                    .thenReturn(true);
            MessagingService messagingService = new MessagingService();

            // test
            messagingService.onMessageReceived(remoteMessage);

            // verify
            messagingServiceMockedStatic.verify(
                    () ->
                            MessagingService.handleRemoteMessage(
                                    any(Context.class), eq(remoteMessage)));
        }
    }

    @Test
    public void test_handleRemoteMessage_WhenPushNotificationFromAJO() {
        // setup
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("_xdm", "somevalues");
                                put("adb_title", "Sample Title");
                                put("adb_content", "Sample Content");
                            }
                        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        boolean isHandled = MessagingService.handleRemoteMessage(context, remoteMessage);

        // verify
        assertTrue(isHandled);

        // verify event dispatched
        mobileCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
        final Event event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(event.getName(), "Push Notification Displayed");
        assertEquals(event.getType(), EventType.MESSAGING);
        assertEquals(event.getSource(), EventSource.RESPONSE_CONTENT);
        assertEquals(event.getEventData(), remoteMessage.getData());

        // verify notification created from push notification builder is displayed
        verify(notificationManager, times(1)).notify(anyInt(), eq(notification));
        pushBuilder.verify(
                () -> MessagingPushBuilder.build(any(MessagingPushPayload.class), eq(context)));
    }

    @Test
    public void test_handleRemoteMessage_WhenNotificationFromAssurance() {
        // setup
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("adb_title", "title");
                            }
                        });

        // test
        assertTrue(MessagingService.handleRemoteMessage(context, remoteMessage));
    }

    @Test
    public void test_handleRemoteMessage_whenNotAdobeGeneratedNotification() {
        // setup
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("key", "value");
                            }
                        });

        // test
        boolean isHandled = MessagingService.handleRemoteMessage(context, remoteMessage);

        // verify
        assertFalse(isHandled);
    }

    // ========================================================================================
    // PushNotificationListener - onNotificationReceived
    // ========================================================================================

    @Test
    public void test_handleRemoteMessage_callsOnNotificationReceived() {
        // setup
        final PushNotificationListener listener = Mockito.mock(PushNotificationListener.class);
        PushNotificationEventManager.setListener(listener);
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("_xdm", "somevalues");
                                put("adb_title", "Test Title");
                                put("custom_key", "custom_value");
                            }
                        });

        // test
        boolean isHandled = MessagingService.handleRemoteMessage(context, remoteMessage);

        // verify
        assertTrue(isHandled);
        ArgumentCaptor<MessagingPushPayload> payloadCaptor =
                ArgumentCaptor.forClass(MessagingPushPayload.class);
        verify(listener, times(1)).onNotificationReceived(payloadCaptor.capture());

        MessagingPushPayload payload = payloadCaptor.getValue();
        assertNotNull(payload);
        assertEquals("Test Title", payload.getTitle());
        assertEquals("custom_value", payload.getData().get("custom_key"));

        // cleanup
        PushNotificationEventManager.setListener(null);
    }

    @Test
    public void test_handleRemoteMessage_noListenerNoCrash() {
        // setup
        PushNotificationEventManager.setListener(null);
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("_xdm", "somevalues");
                                put("adb_title", "Test Title");
                            }
                        });

        // test — should not throw
        boolean isHandled = MessagingService.handleRemoteMessage(context, remoteMessage);

        // verify
        assertTrue(isHandled);
    }

    @Test
    public void test_handleRemoteMessage_listenerExceptionDoesNotPreventEvent() {
        // setup
        final PushNotificationListener listener = Mockito.mock(PushNotificationListener.class);
        Mockito.doThrow(new RuntimeException("listener crash"))
                .when(listener)
                .onNotificationReceived(any());
        PushNotificationEventManager.setListener(listener);
        when(remoteMessage.getData())
                .thenReturn(
                        new HashMap<String, String>() {
                            {
                                put("_xdm", "somevalues");
                                put("adb_title", "Test Title");
                            }
                        });
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test — should not throw despite listener crashing
        boolean isHandled = MessagingService.handleRemoteMessage(context, remoteMessage);

        // verify notification was still displayed and event dispatched
        assertTrue(isHandled);
        mobileCore.verify(() -> MobileCore.dispatchEvent(eventCaptor.capture()));
        assertNotNull(eventCaptor.getValue());
        assertEquals("Push Notification Displayed", eventCaptor.getValue().getName());

        // cleanup
        PushNotificationEventManager.setListener(null);
    }
}
