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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PushNotificationBuilderTests {

    private static String NOTIFICATION_TITLE = "notification title";
    private static String NOTIFICATION_BODY = "notification body";
    private static int DEFAULT_ICON_RESOURCE_ID = 2;
    private static int CUSTOM_ICON_RESOURCE_ID = 3;

    @Mock
    Context context;
    @Mock
    PushNotificationBuilderUtils utils;
    @Mock
    MessagingPushPayload payload;
    @Mock
    Notification notification;

    NotificationCompat.Builder mockNotificationBuilder;
    PushNotificationBuilder builder;
    MockedConstruction<NotificationCompat.Builder> mockBuilderConstructor;

    @Before
    public void before() {
        builder = new PushNotificationBuilder(payload, context, utils);
        mockBuilderConstructor = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
            when(mock.build()).thenReturn(notification);
        });

        when(payload.getTitle()).thenReturn(NOTIFICATION_TITLE);
        when(payload.getBody()).thenReturn(NOTIFICATION_BODY);
        when(utils.getDefaultAppIcon()).thenReturn(DEFAULT_ICON_RESOURCE_ID);
        when(utils.getSmallIconWithResourceName(anyString())).thenReturn(CUSTOM_ICON_RESOURCE_ID);
    }

    @Test
    public void test_notificationBuild() {
        // setup


        // test
        Notification notification = builder.build();
        mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
    }
}
