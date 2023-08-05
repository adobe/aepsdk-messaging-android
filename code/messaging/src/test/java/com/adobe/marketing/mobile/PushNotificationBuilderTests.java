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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Context;
import android.net.Uri;

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

    private static final String NOTIFICATION_TITLE = "notification title";
    private static final String NOTIFICATION_BODY = "notification body";
    private static final int DEFAULT_ICON_RESOURCE_ID = 2;
    private static final int CUSTOM_ICON_RESOURCE_ID = 3;
    private static final String CUSTOM_ICON_RESOURCE_NAME = "newAppIcon";
    private static final int BADGE_COUNT = 10;
    private static final String LARGE_IMAGE_URL = "https://www.sampleimage.com";
    private static final int SAMPLE_PRIORITY = Notification.PRIORITY_HIGH;

    private static final String CUSTOM_SOUND_NAME = "customSound";
    private static final Uri CUSTOM_SOUND_URI = Uri.parse("android.resource://com.adobe.sample/raw/customSound");

    @Mock
    Context context;
    @Mock
    PushNotificationBuilderUtils utils;
    @Mock
    MessagingPushPayload payload;
    @Mock
    Notification notification;

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
        when(payload.getBadgeCount()).thenReturn(BADGE_COUNT);
        when(payload.getImageUrl()).thenReturn(LARGE_IMAGE_URL);
        when(payload.getNotificationPriority()).thenReturn(SAMPLE_PRIORITY);
        when(payload.getIcon()).thenReturn(CUSTOM_ICON_RESOURCE_NAME);
        when(payload.getSound()).thenReturn(CUSTOM_SOUND_NAME);

        when(utils.getDefaultAppIcon()).thenReturn(DEFAULT_ICON_RESOURCE_ID);
        when(utils.getSmallIconWithResourceName(CUSTOM_ICON_RESOURCE_NAME)).thenReturn(CUSTOM_ICON_RESOURCE_ID);
        when(utils.getSoundUriForResourceName(CUSTOM_SOUND_NAME)).thenReturn(CUSTOM_SOUND_URI);
    }

    @Test
    public void test_notificationBuild() {

        // test
        Notification notification = builder.build();
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setContentText(NOTIFICATION_BODY);
        verify(mockNotificationBuilder,times(1)).setContentTitle(NOTIFICATION_TITLE);
        verify(mockNotificationBuilder,times(1)).setPriority(SAMPLE_PRIORITY);
        verify(mockNotificationBuilder,times(1)).setSmallIcon(CUSTOM_ICON_RESOURCE_ID);
        verify(mockNotificationBuilder,times(1)).setNumber(BADGE_COUNT);
        verify(mockNotificationBuilder,times(1)).setSound(CUSTOM_SOUND_URI);
    }

    @Test
    public void test_notificationBuild_when_noSoundSetInPayload() {
        // setup
        when(payload.getSound()).thenReturn(null);

        // test
        Notification notification = builder.build();
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setDefaults(Notification.DEFAULT_ALL);
        verify(mockNotificationBuilder,times(0)).setSound(any(Uri.class));
    }

    @Test
    public void test_notificationBuild_when_noIconSetInPayload() {
        // setup
        when(payload.getIcon()).thenReturn(null);

        // test
        Notification notification = builder.build();
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setSmallIcon(DEFAULT_ICON_RESOURCE_ID);
    }

    @Test
    public void test_notificationBuild_when_noPrioritySetInPayload() {
        // setup
        when(payload.getNotificationPriority()).thenReturn(Notification.PRIORITY_DEFAULT);

        // test
        Notification notification = builder.build();
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setPriority(Notification.PRIORITY_DEFAULT);
    }


}
