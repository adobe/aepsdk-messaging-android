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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;

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
    private static final Uri CUSTOM_SOUND_URI = Uri.parse("android.resource://com.adobe.sample/raw/customSound");

    @Mock
    Context context;
    @Mock
    MessagingPushPayload payload;
    @Mock
    Notification notification;
    @Mock
    PackageManager packageManager;
    @Mock
    Intent launchIntent;
    @Mock
    Uri sampleUri;
    @Mock
    PendingIntent returnedPendingIntent;
    MessagingPushBuilder builder;
    MockedConstruction<NotificationCompat.Builder> mockBuilderConstructor;
    MockedConstruction<Intent> intentConstructor;
    MockedStatic<MessagingPushUtils> utils;
    ArgumentCaptor<Intent> launchIntentCaptor;
    MockedStatic<PendingIntent> staticMockPendingIntent;
    MockedStatic<Uri> staticMockUri;
    ArgumentCaptor<String> mockUriStringCaptor;

    @Before
    public void before() {
        mockUriStringCaptor = ArgumentCaptor.forClass(String.class);
        launchIntentCaptor = ArgumentCaptor.forClass(Intent.class);

        utils = mockStatic(MessagingPushUtils.class);
        staticMockPendingIntent = mockStatic(PendingIntent.class);
        staticMockUri = mockStatic(Uri.class);

        mockBuilderConstructor = mockConstruction(NotificationCompat.Builder.class, (mock, context) -> {
            when(mock.build()).thenReturn(notification);
        });

        intentConstructor = mockConstruction(Intent.class, (mock, context) -> {
        });

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

        utils.when(() -> MessagingPushUtils.getDefaultAppIcon(context)).thenReturn(DEFAULT_ICON_RESOURCE_ID);
        utils.when(() -> MessagingPushUtils.getSmallIconWithResourceName(CUSTOM_ICON_RESOURCE_NAME,context)).thenReturn(CUSTOM_ICON_RESOURCE_ID);
        utils.when(() -> MessagingPushUtils.getSoundUriForResourceName(CUSTOM_SOUND_NAME ,context)).thenReturn(CUSTOM_SOUND_URI);

        staticMockPendingIntent.when(() -> PendingIntent.getActivity(any(Context.class), any(Integer.class), launchIntentCaptor.capture(), any(Integer.class))).thenReturn(returnedPendingIntent);
        staticMockUri.when(() -> Uri.parse(mockUriStringCaptor.capture())).thenReturn(sampleUri);
    }

    @After
    public void after() {
        mockBuilderConstructor.close();
        intentConstructor.close();
        utils.close();
        staticMockPendingIntent.close();
        staticMockUri.close();
    }

    @Test
    public void test_notificationBuild() {
        // test
        Notification notification = builder.build(payload, context);
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
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        verify(mockNotificationBuilder,times(0)).setSound(any(Uri.class));
    }

    @Test
    public void test_notificationBuild_when_noIconSetInPayload() {
        // setup
        when(payload.getIcon()).thenReturn(null);

        // test
        Notification notification = builder.build(payload, context);
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
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setPriority(Notification.PRIORITY_DEFAULT);
    }

    @Test
    public void test_notificationBuild_when_OpenAppOnNotificationClick() {
        // setup
        final ArgumentCaptor<PendingIntent> pendingIntentCaptor = ArgumentCaptor.forClass(PendingIntent.class);
        when(payload.getActionType()).thenReturn(MessagingPushPayload.ActionType.OPENAPP);

        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setContentIntent(pendingIntentCaptor.capture());
        verify(mockNotificationBuilder,times(1)).setAutoCancel(true);

        // verify pending Intent created to open app
        assertEquals(returnedPendingIntent, pendingIntentCaptor.getValue());

        // verify launch intent created to open app
        verify(launchIntent,times(1)).putExtra("applicationOpened", true);
        verify((launchIntent), times(1)).putExtra(eq("eventType"), eq("pushTracking.applicationOpened"));
        verify((launchIntent), times(1)).setFlags(anyInt());
    }

    @Test
    public void test_notificationBuild_when_DeeplinkOnNotificationClick() {
        // setup
        final ArgumentCaptor<PendingIntent> pendingIntentCaptor = ArgumentCaptor.forClass(PendingIntent.class);
        when(payload.getActionType()).thenReturn(MessagingPushPayload.ActionType.DEEPLINK);
        when(payload.getActionUri()).thenReturn("sampleapp://test.com");

        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);
        Intent deeplinkIntent = intentConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(1)).setContentIntent(pendingIntentCaptor.capture());
        verify(mockNotificationBuilder,times(1)).setAutoCancel(true);

        // verify pending Intent created
        assertEquals(returnedPendingIntent, pendingIntentCaptor.getValue());

        // verify deeplink intent created
        verify(deeplinkIntent,times(1)).putExtra("applicationOpened", true);
        verify((deeplinkIntent), times(1)).putExtra(eq("eventType"), eq("pushTracking.applicationOpened"));
        verify((deeplinkIntent), times(1)).setData(sampleUri);
        assertEquals("sampleapp://test.com",mockUriStringCaptor.getValue());
    }

    @Test
    public void test_notificationBuild_with_notificationButtons() {
        // setup
        ArgumentCaptor<Integer> buttonIconCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> buttonLabelCaptor = ArgumentCaptor.forClass(String.class);

        final ArgumentCaptor<PendingIntent> pendingIntentCaptor = ArgumentCaptor.forClass(PendingIntent.class);
        when(payload.getActionType()).thenReturn(MessagingPushPayload.ActionType.OPENAPP);
        when(payload.getActionButtons()).thenReturn(new ArrayList<MessagingPushPayload.ActionButton>() {{
        }});

        // test
        Notification notification = builder.build(payload, context);
        NotificationCompat.Builder mockNotificationBuilder = mockBuilderConstructor.constructed().get(0);
        Intent deeplinkIntent = intentConstructor.constructed().get(0);

        //verify
        assertNotNull(notification);
        verify(mockNotificationBuilder,times(3)).addAction(buttonIconCaptor.capture(), buttonLabelCaptor.capture(), pendingIntentCaptor.capture());

        //verify Button 1
        assertEquals("Button 1", buttonLabelCaptor.getAllValues().get(0));

    }
}
