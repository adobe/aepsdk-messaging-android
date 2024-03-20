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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import com.adobe.marketing.mobile.MessagingPushPayload;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingPushBuilderTests {

    private static final String NOTIFICATION_TITLE = "notification title";
    private static final String NOTIFICATION_BODY = "notification body";
    private static final int DEFAULT_ICON_RESOURCE_ID = 2;
    private static final int CUSTOM_ICON_RESOURCE_ID = 3;
    private static final String CUSTOM_ICON_RESOURCE_NAME = "newAppIcon";
    private static final int BADGE_COUNT = 10;
    private static final String LARGE_IMAGE_URL = "https://www.sampleimage.com";
    private static final int SAMPLE_PRIORITY = Notification.PRIORITY_HIGH;
    private static final String CUSTOM_SOUND_NAME = "customSound";
    private static final Uri CUSTOM_SOUND_URI =
            Uri.parse("android.resource://com.adobe.sample/raw/customSound");

    @Mock Context context;
    @Mock MessagingPushPayload payload;
    @Mock Notification notification;
    @Mock PackageManager packageManager;
    @Mock Intent launchIntent;
    @Mock Uri sampleUri;
    @Mock PendingIntent returnedPendingIntent;
    @Mock TaskStackBuilder taskStackBuilder;

    MessagingPushBuilder builder;
    MockedConstruction<NotificationCompat.Builder> mockBuilderConstructor;
    MockedConstruction<Intent> intentConstructor;
    MockedStatic<MessagingPushUtils> utils;
    ArgumentCaptor<Intent> launchIntentCaptor;
    MockedStatic<PendingIntent> staticMockPendingIntent;
    MockedStatic<TaskStackBuilder> staticMockTaskStackBuilder;
    MockedStatic<Uri> staticMockUri;
    ArgumentCaptor<String> mockUriStringCaptor;

    @Before
    public void before() {
        mockUriStringCaptor = ArgumentCaptor.forClass(String.class);
        launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);

        utils = mockStatic(MessagingPushUtils.class);
        staticMockPendingIntent = mockStatic(PendingIntent.class);
        staticMockTaskStackBuilder = mockStatic(TaskStackBuilder.class);
        staticMockUri = mockStatic(Uri.class);

        mockBuilderConstructor =
                mockConstruction(
                        NotificationCompat.Builder.class,
                        (mock, context) -> {
                            when(mock.build()).thenReturn(notification);
                        });

        intentConstructor = mockConstruction(Intent.class, (mock, context) -> {});

        when(payload.getTitle()).thenReturn(NOTIFICATION_TITLE);
        when(payload.getBody()).thenReturn(NOTIFICATION_BODY);
        when(payload.getBadgeCount()).thenReturn(BADGE_COUNT);
        when(payload.getImageUrl()).thenReturn(LARGE_IMAGE_URL);
        when(payload.getNotificationPriority()).thenReturn(SAMPLE_PRIORITY);
        when(payload.getIcon()).thenReturn(CUSTOM_ICON_RESOURCE_NAME);
        when(payload.getSound()).thenReturn(CUSTOM_SOUND_NAME);

        when(packageManager.getLaunchIntentForPackage("com.adobe.sample")).thenReturn(launchIntent);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getPackageName()).thenReturn("com.adobe.sample");

        utils.when(() -> MessagingPushUtils.getDefaultAppIcon(context))
                .thenReturn(DEFAULT_ICON_RESOURCE_ID);
        utils.when(
                        () ->
                                MessagingPushUtils.getSmallIconWithResourceName(
                                        CUSTOM_ICON_RESOURCE_NAME, context))
                .thenReturn(CUSTOM_ICON_RESOURCE_ID);
        utils.when(() -> MessagingPushUtils.getSoundUriForResourceName(CUSTOM_SOUND_NAME, context))
                .thenReturn(CUSTOM_SOUND_URI);

        when(taskStackBuilder.addNextIntentWithParentStack(any(Intent.class)))
                .thenReturn(taskStackBuilder);
        when(taskStackBuilder.getPendingIntent(anyInt(), anyInt()))
                .thenReturn(returnedPendingIntent);

        staticMockTaskStackBuilder
                .when(() -> TaskStackBuilder.create(any(Context.class)))
                .thenReturn(taskStackBuilder);
        staticMockPendingIntent
                .when(
                        () ->
                                PendingIntent.getActivity(
                                        any(Context.class),
                                        any(Integer.class),
                                        launchIntentCaptor.capture(),
                                        any(Integer.class)))
                .thenReturn(returnedPendingIntent);
        staticMockUri.when(() -> Uri.parse(mockUriStringCaptor.capture())).thenReturn(sampleUri);
    }

    @After
    public void after() {
        mockBuilderConstructor.close();
        intentConstructor.close();
        utils.close();
        staticMockPendingIntent.close();
        staticMockTaskStackBuilder.close();
        staticMockUri.close();
    }

    @Test
    public void test_notificationBuild() {
        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder =
                mockBuilderConstructor.constructed().get(0);

        // verify
        assertNotNull(notification);
        verify(mockNotificationBuilder, times(1)).setContentText(NOTIFICATION_BODY);
        verify(mockNotificationBuilder, times(1)).setContentTitle(NOTIFICATION_TITLE);
        verify(mockNotificationBuilder, times(1)).setPriority(SAMPLE_PRIORITY);
        verify(mockNotificationBuilder, times(1)).setSmallIcon(CUSTOM_ICON_RESOURCE_ID);
        verify(mockNotificationBuilder, times(1)).setNumber(BADGE_COUNT);
        verify(mockNotificationBuilder, times(1)).setSound(CUSTOM_SOUND_URI);
    }

    @Test
    public void test_notificationBuild_when_noSoundSetInPayload() {
        // setup
        when(payload.getSound()).thenReturn(null);

        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder =
                mockBuilderConstructor.constructed().get(0);

        // verify
        assertNotNull(notification);
        verify(mockNotificationBuilder, times(1))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        verify(mockNotificationBuilder, times(0)).setSound(any(Uri.class));
    }

    @Test
    public void test_notificationBuild_when_noIconSetInPayload() {
        // setup
        when(payload.getIcon()).thenReturn(null);

        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder =
                mockBuilderConstructor.constructed().get(0);

        // verify
        assertNotNull(notification);
        verify(mockNotificationBuilder, times(1)).setSmallIcon(DEFAULT_ICON_RESOURCE_ID);
    }

    @Test
    public void test_notificationBuild_when_noPrioritySetInPayload() {
        // setup
        when(payload.getNotificationPriority()).thenReturn(Notification.PRIORITY_DEFAULT);

        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder =
                mockBuilderConstructor.constructed().get(0);

        // verify
        assertNotNull(notification);
        verify(mockNotificationBuilder, times(1)).setPriority(Notification.PRIORITY_DEFAULT);
    }
}
