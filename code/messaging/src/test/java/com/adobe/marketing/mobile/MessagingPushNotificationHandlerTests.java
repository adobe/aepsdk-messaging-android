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
package com.adobe.marketing.mobile;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.core.app.NotificationManagerCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessagingPushNotificationHandler.class, NotificationManagerCompat.class, MessagingUtils.class})
public class MessagingPushNotificationHandlerTests {
    private static int NOTIFICATION_ID = 1;
    private static String MESSAGE_ID = "messageId";
    private static String PUSH_PAYLOAD = "pushPayload";
    private static String ACTIVITY_NAME = "testActivity";
    NotificationManagerCompat mockNotificationManagerCompat;

    List<MessagingPushPayload.ActionButton> actionButtons = new ArrayList<MessagingPushPayload.ActionButton>(3) {
        {
            add(new MessagingPushPayload.ActionButton("button1", "https://www.adobe.com/1", "WEBURL"));
            add(new MessagingPushPayload.ActionButton("button2", "testapp://main", "DEEPLINK"));
            add(new MessagingPushPayload.ActionButton("button3", "https://www.adobe.com/2", "DISMISS"));
        }
    };
    String actionButtonString = TestUtils.convertActionButtonListToJsonArray(actionButtons).toString();
    Map<String, String> normalNotificationMessageData = new HashMap<String, String>() {
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
            put(MessagingTestConstants.PushNotificationPayload.ACTION_BUTTONS, actionButtonString);
        }
    };
    Map<String, String> silentNotificationMessageData = new HashMap<String, String>() {
        {
            put(MessagingTestConstants.PushNotificationPayload.ADB, "true");
            put(MessagingTestConstants.PushNotificationPayload.TITLE, null);
            put(MessagingTestConstants.PushNotificationPayload.BODY, null);
            put(MessagingTestConstants.PushNotificationPayload.SOUND, "mockSound");
            put(MessagingTestConstants.PushNotificationPayload.CHANNEL_ID, "mockChannelId");
            put(MessagingTestConstants.PushNotificationPayload.ICON, "mockIcon");
            put(MessagingTestConstants.PushNotificationPayload.ACTION_URI, "mockAction");
            put(MessagingTestConstants.PushNotificationPayload.IMAGE_URL, "mockImageUrl");
            put(MessagingTestConstants.PushNotificationPayload.NOTIFICATION_COUNT, "10");
            put(MessagingTestConstants.PushNotificationPayload.NOTIFICATION_PRIORITY, "default");
            put(MessagingTestConstants.PushNotificationPayload.ACTION_TYPE, "DEEPLINK");
            put(MessagingTestConstants.PushNotificationPayload.ACTION_BUTTONS, null);
        }
    };
    ResolveInfo testResolveInfo = new ResolveInfo();
    List<ResolveInfo> receivers;

    @Mock
    Context context;
    @Mock
    PackageManager mockPackageManager;
    @Mock
    ApplicationInfo mockApplicationInfo;
    @Mock
    Notification mockNotification;
    @Mock
    MessagingPushNotificationFactory mockMessagingPushNotificationFactory;
    @Mock
    Intent mockLaunchIntent;
    @Mock
    Intent mockSendIntent;
    @Mock
    Intent mockBroadcastIntent;

    @Before
    public void before() {
        mockNotificationManagerCompat = PowerMockito.mock(NotificationManagerCompat.class);
        try {
            PowerMockito.whenNew(NotificationManagerCompat.class).withAnyArguments().thenReturn(mockNotificationManagerCompat);
            PowerMockito.whenNew(Intent.class).withNoArguments().thenReturn(mockSendIntent);
            PowerMockito.whenNew(Intent.class).withArguments(mockSendIntent).thenReturn(mockBroadcastIntent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(mockMessagingPushNotificationFactory.create(any(Context.class), any(MessagingPushPayload.class), anyString(), anyBoolean())).thenReturn(mockNotification);

        // setup broadcast receivers for testing
        testResolveInfo = new ResolveInfo();
        ActivityInfo testActivityInfo = new ActivityInfo();
        testActivityInfo.name = ACTIVITY_NAME;
        testResolveInfo.activityInfo = testActivityInfo;
        receivers = new ArrayList<ResolveInfo>() {
            {
                add(testResolveInfo);
            }
        };
    }

    @Test
    public void test_handlePushNotification_nullNotification(){
        // setup
        Mockito.when(mockMessagingPushNotificationFactory.create(any(Context.class), any(MessagingPushPayload.class), anyString(), anyBoolean())).thenReturn(null);
        Mockito.when(context.getPackageName()).thenReturn("testPackage");
        mockApplicationInfo.icon = 1;
        try {
            Mockito.when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            Mockito.when(mockPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mockLaunchIntent);
            Mockito.when(mockPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt())).thenReturn(receivers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(context.getPackageManager()).thenReturn(mockPackageManager);
        MessagingPushPayload pushPayload = new MessagingPushPayload(normalNotificationMessageData);

        // test
        MessagingPushNotificationHandler.handlePushNotification(context, NOTIFICATION_ID, MESSAGE_ID, pushPayload, mockMessagingPushNotificationFactory, false);

        // verify notification manager notify not called, launch intent not created, and the broadcast was not sent
        verify(mockNotificationManagerCompat, times(0)).notify(NOTIFICATION_ID, mockNotification);
        verify(mockLaunchIntent, times(0)).putExtra(MESSAGE_ID, MESSAGE_ID);
        verify(mockLaunchIntent, times(0)).putExtra(PUSH_PAYLOAD, pushPayload);
        verify(context, times(0)).sendBroadcast(mockBroadcastIntent);
    }

    @Test
    public void test_handlePushNotification_normalNotification(){
        // setup
        Mockito.when(context.getPackageName()).thenReturn("testPackage");
        mockApplicationInfo.icon = 1;
        try {
            Mockito.when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            Mockito.when(mockPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mockLaunchIntent);
            Mockito.when(mockPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt())).thenReturn(receivers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(context.getPackageManager()).thenReturn(mockPackageManager);
        MessagingPushPayload pushPayload = new MessagingPushPayload(normalNotificationMessageData);

        // test
        MessagingPushNotificationHandler.handlePushNotification(context, NOTIFICATION_ID, MESSAGE_ID, pushPayload, mockMessagingPushNotificationFactory, false);

        // verify notification manager notify called, launch intent does not contain the push payload, and the broadcast sent
        verify(mockNotificationManagerCompat, times(1)).notify(NOTIFICATION_ID, mockNotification);
        verify(mockLaunchIntent, times(0)).putExtra(MESSAGE_ID, MESSAGE_ID);
        verify(mockLaunchIntent, times(0)).putExtra(PUSH_PAYLOAD, pushPayload);
        verify(context, times(1)).sendBroadcast(mockBroadcastIntent);
    }

    @Test
    public void test_handlePushNotification_silentNotification(){
        // setup
        Mockito.when(context.getPackageName()).thenReturn("testPackage");
        mockApplicationInfo.icon = 1;
        try {
            Mockito.when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            Mockito.when(mockPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mockLaunchIntent);
            Mockito.when(mockPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt())).thenReturn(receivers);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(context.getPackageManager()).thenReturn(mockPackageManager);
        MessagingPushPayload pushPayload = new MessagingPushPayload(silentNotificationMessageData);

        // test
        MessagingPushNotificationHandler.handlePushNotification(context, NOTIFICATION_ID, MESSAGE_ID, pushPayload, mockMessagingPushNotificationFactory, false);

        // verify notification manager notify not called, launch intent does contain the push payload, and the broadcast sent
        verify(mockNotificationManagerCompat, times(0)).notify(NOTIFICATION_ID, mockNotification);
        verify(mockLaunchIntent, times(1)).putExtra(MESSAGE_ID, MESSAGE_ID);
        verify(mockLaunchIntent, times(1)).putExtra(PUSH_PAYLOAD, pushPayload);
        verify(context, times(1)).sendBroadcast(mockBroadcastIntent);
    }
}
