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

package com.adobe.marketing.mobile;

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
import com.google.firebase.messaging.RemoteMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.HashMap;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AdobeMessagingServiceTests {
    @Mock
    RemoteMessage remoteMessage;
    @Mock
    Context context;
    @Mock
    NotificationManagerCompat notificationManager;
    @Mock
    Notification notification;
    MockedStatic<MobileCore> mobileCore;
    MockedStatic<NotificationManagerCompat> notificationManagerCompat;
    MockedStatic<MessagingPushBuilder> pushBuilder;

    @Before
    public void before() {

        // Mock NotificationManager
        notificationManagerCompat = mockStatic(NotificationManagerCompat.class);
        notificationManagerCompat.when(() -> NotificationManagerCompat.from(any(Context.class))).thenReturn(notificationManager);
        doNothing().when(notificationManager).notify(anyInt(), any());

        // Mock MobileCore
        mobileCore = mockStatic(MobileCore.class);

        // Mock PushNotificationBuilder
        pushBuilder = mockStatic(MessagingPushBuilder.class);
        pushBuilder.when(() -> MessagingPushBuilder.build(any(MessagingPushPayload.class), any(Context.class))).thenReturn(notification);

        when(remoteMessage.getMessageId()).thenReturn("someMessageID");
    }

    @After
    public void clean() {
        mobileCore.close();
        pushBuilder.close();
        notificationManagerCompat.close();
    }


    @Test
    public void test_handleRemoteMessage_WhenPushNotificationFromAJO() {
        //setup
        when(remoteMessage.getData()).thenReturn(new HashMap<String, String>()
        {{
            put("_xdm", "somevalues");
            put("adb_title", "Sample Title");
            put("adb_content", "Sample Content");
        }});
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        boolean isHandled = AdobeMessagingService.handleRemoteMessage(context,remoteMessage);

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
        pushBuilder.verify(() -> MessagingPushBuilder.build(any(MessagingPushPayload.class), eq(context)));
    }

    @Test
    public void test_handleRemoteMessage_WhenNotificationFromAssurance() {
        //setup
        when(remoteMessage.getData()).thenReturn(new HashMap<String, String>()
        {{
            put("adb_title", "title");
        }});

        // test
        assertTrue(AdobeMessagingService.handleRemoteMessage(context,remoteMessage));
    }


    @Test
    public void test_handleRemoteMessage_whenNotAdobeGeneratedNotification() {
        //setup
        when(remoteMessage.getData()).thenReturn(new HashMap<String, String>()
        {{
            put("key", "value");
        }});

        // test
        boolean isHandled = AdobeMessagingService.handleRemoteMessage(context,remoteMessage);

        // verify
        assertFalse(isHandled);
    }

}
