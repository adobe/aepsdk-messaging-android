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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MessagingPushNotificationFactory.class, Notification.Builder.class, Build.class, MessagingUtils.class, AudioAttributes.class})
public class MessagingPushNotificationFactoryTests {
    private static final String MESSAGE_ID = "messageId";
    private static final String PACKAGE_NAME = "testPackage";
    MessagingPushNotificationFactory messagingPushNotificationFactory;
    List<MessagingPushPayload.ActionButton> actionButtons = new ArrayList<MessagingPushPayload.ActionButton>(3) {
        {
            add(new MessagingPushPayload.ActionButton("button1", "https://www.adobe.com/1", "WEBURL"));
            add(new MessagingPushPayload.ActionButton("button2", "testapp://main", "DEEPLINK"));
            add(new MessagingPushPayload.ActionButton("button3", null, "OPENAPP"));
        }
    };
    String actionButtonString = TestUtils.convertActionButtonListToJsonArray(actionButtons).toString();
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
            put(MessagingTestConstants.PushNotificationPayload.ACTION_BUTTONS, actionButtonString);
        }
    };
    NotificationChannel defaultNotificationChannel = new NotificationChannel(MessagingTestConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID,
            MessagingTestConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT);

    @Mock
    Context context;
    @Mock
    PackageManager mockPackageManager;
    @Mock
    ApplicationInfo mockApplicationInfo;
    @Mock
    Notification.Builder mockNotificationBuilder;
    @Mock
    Notification mockNotification;
    @Mock
    NotificationManager mockNotificationManager;

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(Notification.Builder.class);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(mockNotificationManager);
        PowerMockito.whenNew(NotificationChannel.class).withAnyArguments().thenReturn(defaultNotificationChannel);
        messagingPushNotificationFactory = MessagingPushNotificationFactory.getInstance();
    }

    @Test
    public void test_create() {
        // setup
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mockNotificationBuilder.build()).thenReturn(mockNotification);
        mockApplicationInfo.icon = 1;
        try {
            when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            PowerMockito.whenNew(Notification.Builder.class).withAnyArguments().thenReturn(mockNotificationBuilder);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(context.getPackageManager()).thenReturn(mockPackageManager);

        // test
        Notification notification = messagingPushNotificationFactory.create(context, new MessagingPushPayload(remoteMessageData), MESSAGE_ID, 1, false);

        // verify
        assertEquals(mockNotification, notification);
    }

    @Test
    public void test_create_invalidSmallIconResId() {
        // setup
        when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mockNotificationBuilder.build()).thenReturn(mockNotification);
        // -1 is an invalid small icon resource id
        mockApplicationInfo.icon = -1;
        try {
            when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            PowerMockito.whenNew(Notification.Builder.class).withAnyArguments().thenReturn(mockNotificationBuilder);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(context.getPackageManager()).thenReturn(mockPackageManager);

        // test
        Notification notification = messagingPushNotificationFactory.create(context, new MessagingPushPayload(remoteMessageData), MESSAGE_ID, 1, false);

        // verify
        assertNull(notification);
    }
}
