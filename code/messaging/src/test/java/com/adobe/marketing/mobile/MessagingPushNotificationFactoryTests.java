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

import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

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
@PrepareForTest({MessagingPushNotificationFactory.class, Notification.Builder.class})
public class MessagingPushNotificationFactoryTests {
    MessagingPushNotificationFactory messagingPushNotificationFactory;
    List<MessagingPushPayload.ActionButton> actionButtons = new ArrayList<MessagingPushPayload.ActionButton>(3) {
        {
            add(new MessagingPushPayload.ActionButton("button1", "https://www.adobe.com/1", "WEBURL"));
            add(new MessagingPushPayload.ActionButton("button2", "testapp://main", "DEEPLINK"));
            add(new MessagingPushPayload.ActionButton("button3", "https://www.adobe.com/2", "DISMISS"));
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

    @Before
    public void before() {
        PowerMockito.mockStatic(Notification.Builder.class);
        messagingPushNotificationFactory = MessagingPushNotificationFactory.getInstance();
    }

    @Test
    public void test_create(){
        // setup
        Mockito.when(context.getPackageName()).thenReturn("testPackage");
        Mockito.when(mockNotificationBuilder.build()).thenReturn(mockNotification);
        mockApplicationInfo.icon = 1;
        try {
            Mockito.when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            PowerMockito.whenNew(Notification.Builder.class).withAnyArguments().thenReturn(mockNotificationBuilder);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(context.getPackageManager()).thenReturn(mockPackageManager);

        // test
        Notification notification = messagingPushNotificationFactory.create(context, new MessagingPushPayload(remoteMessageData), "messageId", false);

        // verify
        assertEquals(mockNotification, notification);
    }

    @Test
    public void test_create_invalidSmallIconResId(){
        // setup
        Mockito.when(context.getPackageName()).thenReturn("testPackage");
        Mockito.when(mockNotificationBuilder.build()).thenReturn(mockNotification);
        // -1 is an invalid small icon resource id
        mockApplicationInfo.icon = -1;
        try {
            Mockito.when(mockPackageManager.getApplicationInfo(anyString(), anyInt())).thenReturn(mockApplicationInfo);
            PowerMockito.whenNew(Notification.Builder.class).withAnyArguments().thenReturn(mockNotificationBuilder);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        Mockito.when(context.getPackageManager()).thenReturn(mockPackageManager);

        // test
        Notification notification = messagingPushNotificationFactory.create(context, new MessagingPushPayload(remoteMessageData), "messageId", false);

        // verify
        assertNull(notification);
    }
}
