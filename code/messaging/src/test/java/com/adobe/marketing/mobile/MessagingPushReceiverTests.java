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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Messaging.class, MessagingUtils.class, MessagingPushReceiver.class, PendingIntent.class})
public class MessagingPushReceiverTests {
    private static final String MESSAGE_ID = "messageId";
    private static final String XDM = "someXdmData";
    private static final String PACKAGE_NAME = "testPackage";
    private static final String ACTIVITY_NAME = "testActivity";
    private static final String CUSTOM_ACTION = "someAction";
    private static final boolean NOTIFICATION_INTERACTED = true;
    private static final boolean NOTIFICATION_DELETED = false;
    MessagingPushReceiver messagingPushReceiver = new MessagingPushReceiver();
    ResolveInfo testResolveInfo = new ResolveInfo();
    List<ResolveInfo> receivers;
    ComponentName component;

    @Mock
    Context context;
    @Mock
    Bundle mockExtras;
    @Mock
    Intent mockSendIntent;
    @Mock
    Intent mockBroadcastIntent;
    @Mock
    Intent mockLaunchIntent;
    @Mock
    PendingIntent mockPendingIntent;
    @Mock
    PackageManager mockPackageManager;
    @Mock
    Thread mockThread;

    @Before
    public void before() {
        PowerMockito.mockStatic(Messaging.class);
        PowerMockito.mockStatic(PendingIntent.class);
        try {
            PowerMockito.whenNew(Thread.class).withAnyArguments().thenReturn(mockThread);
            PowerMockito.whenNew(Intent.class).withNoArguments().thenReturn(mockSendIntent);
            PowerMockito.whenNew(Intent.class).withArguments(mockSendIntent).thenReturn(mockBroadcastIntent);
            PowerMockito.when(PendingIntent.getActivity(any(Context.class), anyInt(), any(Intent.class), anyInt())).thenReturn(mockPendingIntent);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        // Run the PushActionHandlingRunnable - mocking the Thread.start()
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = new MessagingPushReceiver.PushActionHandlingRunnable(context, mockBroadcastIntent);
                r.run();
                return null;
            }
        }).when(mockThread).start();
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
        Mockito.when(context.getPackageName()).thenReturn(PACKAGE_NAME);
        Mockito.when(context.getPackageManager()).thenReturn(mockPackageManager);
        Mockito.when(mockPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt())).thenReturn(receivers);
        Mockito.when(mockPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(mockLaunchIntent);
        // setup test intents
        Mockito.when(mockBroadcastIntent.getAction()).thenReturn(CUSTOM_ACTION);
        Mockito.when(mockBroadcastIntent.getExtras()).thenReturn(mockExtras);
        Mockito.when(mockSendIntent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID)).thenReturn(MESSAGE_ID);
        Mockito.when(mockSendIntent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM)).thenReturn(XDM);
        // setup component
        component = new ComponentName(PACKAGE_NAME, ACTIVITY_NAME);
        try {
            PowerMockito.whenNew(ComponentName.class).withAnyArguments().thenReturn(component);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_onReceive_shouldHandleTrackingIsTrue() throws PendingIntent.CanceledException {
        // setup
        String action = String.format("%s_%s", PACKAGE_NAME, CUSTOM_ACTION);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(true);

        // test
        messagingPushReceiver.onReceive(context, mockBroadcastIntent);

        // verify broadcast with the action is sent
        verify(mockSendIntent, times(1)).setAction(action);
        verify(mockBroadcastIntent, times(1)).setComponent(component);
        verify(context, times(1)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is called with the broadcast intent and matching action, and that the pending intent was sent
        PowerMockito.verifyStatic(Messaging.class, times(1));
        Messaging.handleNotificationResponse(eq(mockBroadcastIntent), eq(NOTIFICATION_INTERACTED), eq(CUSTOM_ACTION));
        PowerMockito.verifyStatic(PendingIntent.class, times(1));
        PendingIntent.getActivity(eq(context), eq(0), eq(mockLaunchIntent), eq(PendingIntent.FLAG_ONE_SHOT));
        verify(mockPendingIntent, times(1)).send();
    }

    @Test
    public void test_onReceive_shouldHandleTrackingIsFalse() throws PendingIntent.CanceledException {
        // setup
        String action = String.format("%s_%s", PACKAGE_NAME, CUSTOM_ACTION);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(false);

        // test
        messagingPushReceiver.onReceive(context, mockBroadcastIntent);

        // verify broadcast with the action is sent
        verify(mockSendIntent, times(1)).setAction(action);
        verify(mockBroadcastIntent, times(1)).setComponent(component);
        verify(context, times(1)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is not called and pending intent not sent
        PowerMockito.verifyStatic(Messaging.class, times(0));
        Messaging.handleNotificationResponse(eq(mockBroadcastIntent), eq(NOTIFICATION_INTERACTED), eq(CUSTOM_ACTION));
        PowerMockito.verifyStatic(PendingIntent.class, times(0));
        PendingIntent.getActivity(eq(context), eq(0), eq(mockLaunchIntent), eq(PendingIntent.FLAG_ONE_SHOT));
        verify(mockPendingIntent, times(0)).send();
    }

    @Test
    public void test_onReceive_nullContext() throws PendingIntent.CanceledException {
        // setup
        String action = String.format("%s_%s", PACKAGE_NAME, CUSTOM_ACTION);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(true);

        // test
        messagingPushReceiver.onReceive(null, mockBroadcastIntent);

        // verify broadcast with the action is not sent
        verify(mockSendIntent, times(0)).setAction(action);
        verify(mockBroadcastIntent, times(0)).setComponent(component);
        verify(context, times(0)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is not called and pending intent not sent
        PowerMockito.verifyStatic(Messaging.class, times(0));
        Messaging.handleNotificationResponse(eq(mockBroadcastIntent), eq(NOTIFICATION_INTERACTED), eq(CUSTOM_ACTION));
        PowerMockito.verifyStatic(PendingIntent.class, times(0));
        PendingIntent.getActivity(eq(context), eq(0), eq(mockLaunchIntent), eq(PendingIntent.FLAG_ONE_SHOT));
        verify(mockPendingIntent, times(0)).send();
    }

    @Test
    public void test_onReceive_nullIntent() throws PendingIntent.CanceledException {
        // setup
        String action = String.format("%s_%s", PACKAGE_NAME, CUSTOM_ACTION);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(true);

        // test
        messagingPushReceiver.onReceive(context, null);

        // verify broadcast with the action is not sent
        verify(mockSendIntent, times(0)).setAction(action);
        verify(mockBroadcastIntent, times(0)).setComponent(component);
        verify(context, times(0)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is not called and pending intent not sent
        PowerMockito.verifyStatic(Messaging.class, times(0));
        Messaging.handleNotificationResponse(eq(mockBroadcastIntent), eq(NOTIFICATION_INTERACTED), eq(CUSTOM_ACTION));
        PowerMockito.verifyStatic(PendingIntent.class, times(0));
        PendingIntent.getActivity(eq(context), eq(0), eq(mockLaunchIntent), eq(PendingIntent.FLAG_ONE_SHOT));
        verify(mockPendingIntent, times(0)).send();
    }

    @Test
    public void test_onReceive_nullAction() throws PendingIntent.CanceledException {
        // setup
        Mockito.when(mockBroadcastIntent.getAction()).thenReturn(null);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(true);

        // test
        messagingPushReceiver.onReceive(context, mockBroadcastIntent);

        // verify broadcast with the action is not sent
        verify(mockSendIntent, times(0)).setAction(anyString());
        verify(mockBroadcastIntent, times(0)).setComponent(component);
        verify(context, times(0)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is not called and pending intent not sent
        PowerMockito.verifyStatic(Messaging.class, times(0));
        Messaging.handleNotificationResponse(eq(mockBroadcastIntent), eq(NOTIFICATION_INTERACTED), anyString());
        PowerMockito.verifyStatic(PendingIntent.class, times(0));
        PendingIntent.getActivity(eq(context), eq(0), eq(mockLaunchIntent), eq(PendingIntent.FLAG_ONE_SHOT));
        verify(mockPendingIntent, times(0)).send();
    }

    @Test
    public void test_onReceive_NotificationDeleted_shouldHandleTrackingIsTrue() throws PendingIntent.CanceledException {
        // setup
        String action = String.format("%s_%s", PACKAGE_NAME, MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED);
        Mockito.when(mockBroadcastIntent.getAction()).thenReturn(MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(true);

        // test
        messagingPushReceiver.onReceive(context, mockBroadcastIntent);

        // verify broadcast with the action is sent
        verify(mockSendIntent, times(1)).setAction(action);
        verify(mockBroadcastIntent, times(1)).setComponent(component);
        verify(context, times(1)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is called with the broadcast intent and matching action, and that the
        // pending intent was not sent as this was a deletion
        PowerMockito.verifyStatic(Messaging.class, times(1));
        Messaging.handleNotificationResponse(eq(mockBroadcastIntent), eq(NOTIFICATION_DELETED), eq(MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED));
        PowerMockito.verifyStatic(PendingIntent.class, times(0));
        PendingIntent.getActivity(ArgumentMatchers.<Context>any(), anyInt(), ArgumentMatchers.<Intent>any(), anyInt());
        verify(mockPendingIntent, times(0)).send();
    }

    @Test
    public void test_onReceive_NotificationDeleted_shouldHandleTrackingIsFalse() throws PendingIntent.CanceledException {
        // setup
        String action = String.format("%s_%s", PACKAGE_NAME, MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED);
        Mockito.when(mockBroadcastIntent.getAction()).thenReturn(MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED);
        Mockito.when(mockExtras.getBoolean(MessagingTestConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)).thenReturn(false);

        // test
        messagingPushReceiver.onReceive(context, mockBroadcastIntent);

        // verify broadcast with the action is sent
        verify(mockSendIntent, times(1)).setAction(action);
        verify(mockBroadcastIntent, times(1)).setComponent(component);
        verify(context, times(1)).sendBroadcast(mockBroadcastIntent);
        // verify handle notification response is not called
        PowerMockito.verifyStatic(Messaging.class, times(0));
        Messaging.handleNotificationResponse(ArgumentMatchers.<Intent>any(), anyBoolean(), anyString());
        PowerMockito.verifyStatic(PendingIntent.class, times(0));
        PendingIntent.getActivity(ArgumentMatchers.<Context>any(), anyInt(), ArgumentMatchers.<Intent>any(), anyInt());
        verify(mockPendingIntent, times(0)).send();
    }
}
