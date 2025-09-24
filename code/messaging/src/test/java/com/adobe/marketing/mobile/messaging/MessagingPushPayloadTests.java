/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.content.Intent;
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MessagingPushPayloadTests {
    private final String mockTitle = "mockTitle";
    private final String mockBody = "mockBody";
    private final String mockSound = "mockSound";
    private final String mockBadgeCount = "1";
    private final String mockPriority = "PRIORITY_MAX";
    private final String mockChannelId = "mockChannelId";
    private final String mockIcon = "mockIcon";
    private final String mockImageUrl = "mockImageUrl";
    private final String mockActionType = "DEEPLINK";
    private final String mockActionUri = "mockActionUri";
    private final String mockActionButtons =
            "[\n"
                    + "            {\n"
                    + " \"label\" : \"deeplink\",\n"
                    + " \"uri\" : \"notificationapp://\",\n"
                    + " \"type\" : \"DEEPLINK\"\n"
                    + " },\n"
                    + " {\n"
                    + " \"label\" : \"weburl\",\n"
                    + " \"uri\" : \"https://www.yahoo.com\",\n"
                    + " \"type\" : \"WEBURL\"\n"
                    + "},\n"
                    + "{\n"
                    + "\"label\" : \"dismiss\",\n"
                    + "\"uri\" : \"\",\n"
                    + " \"type\" : \"DISMISS\"\n"
                    + "}\n"
                    + "]";
    private final String mockMalformedActionButtons =
            "[\n"
                    + "            {\n"
                    + " \"label\" : \"deeplink\",\n"
                    + " \"type\" : \"DEEPLINK\"\n"
                    + " },\n"
                    + " {\n"
                    + " \"label\" : \"weburl\",\n"
                    + " \"uri\" : \"https://www.yahoo.com\"},\n"
                    + "{\n"
                    + "\"label\" : \"dismiss\",\n"
                    + "\"uri\" : \"\",\n"
                    + " \"type\" : \"DISMISS\"\n"
                    + "}\n"
                    + "]";
    private MessagingPushPayload payload;
    private Map<String, String> mockData;

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor_with_RemoteMessage() {
        mockData = getMockData(false);
        final RemoteMessage mockMessage = Mockito.mock(RemoteMessage.class);
        when(mockMessage.getMessageId()).thenReturn("mockMessageId");
        when(mockMessage.getData()).thenReturn(mockData);
        payload = new MessagingPushPayload(mockMessage);
        Assert.assertNotNull(payload);
        Assert.assertEquals(3, payload.getActionButtons().size());
    }

    @Test
    public void test_Constructor_with_MapData() {
        mockData = getMockData(false);
        payload = new MessagingPushPayload(mockData);
        Assert.assertNotNull(payload);
        Assert.assertEquals(3, payload.getActionButtons().size());
    }

    @Test
    public void test_Constructor_with_RemoteMessage_MalformedActionButtons() {
        mockData = getMockData(true);
        final RemoteMessage mockMessage = Mockito.mock(RemoteMessage.class);
        when(mockMessage.getMessageId()).thenReturn("mockMessageId");
        when(mockMessage.getData()).thenReturn(mockData);
        payload = new MessagingPushPayload(mockMessage);
        Assert.assertNotNull(payload);
        // verify only 2 buttons created
        Assert.assertEquals(2, payload.getActionButtons().size());
    }

    @Test
    public void test_Constructor_with_MapData_MalformedActionButtons() {
        mockData = getMockData(true);
        payload = new MessagingPushPayload(mockData);
        Assert.assertNotNull(payload);
        // verify only 2 buttons created
        Assert.assertEquals(2, payload.getActionButtons().size());
    }

    @Test
    public void getNotificationVisibilityFromString_shouldReturnExpectedValues() throws Exception {
        MessagingPushPayload payload = new MessagingPushPayload(new HashMap<>());

        java.lang.reflect.Method method =
                MessagingPushPayload.class.getDeclaredMethod(
                        "getNotificationVisibilityFromString", String.class);
        method.setAccessible(true);

        // Valid values
        Assert.assertEquals(Notification.VISIBILITY_PRIVATE, method.invoke(payload, "PRIVATE"));
        Assert.assertEquals(Notification.VISIBILITY_PUBLIC, method.invoke(payload, "PUBLIC"));
        Assert.assertEquals(Notification.VISIBILITY_SECRET, method.invoke(payload, "SECRET"));

        // Null or invalid values
        Assert.assertEquals(Notification.VISIBILITY_PRIVATE, method.invoke(payload, ""));
        Assert.assertEquals(Notification.VISIBILITY_PRIVATE, method.invoke(payload, "INVALID"));
    }

    // ========================================================================================
    // public methods
    // ========================================================================================
    @Test
    public void test_PublicMethods() {
        mockData = getMockData(false);
        payload = new MessagingPushPayload(mockData);
        Assert.assertEquals(mockTitle, payload.getTitle());
        Assert.assertEquals(mockBody, payload.getBody());
        Assert.assertEquals(mockSound, payload.getSound());
        Assert.assertEquals(mockChannelId, payload.getChannelId());
        Assert.assertEquals(mockActionUri, payload.getActionUri());
        Assert.assertEquals(mockIcon, payload.getIcon());
        Assert.assertEquals(mockImageUrl, payload.getImageUrl());
        Assert.assertEquals(mockActionType, payload.getActionType().name());
        Assert.assertEquals(1, payload.getBadgeCount());
        Assert.assertEquals(2, payload.getNotificationPriority());
        Assert.assertEquals(3, payload.getActionButtons().size());
        Assert.assertEquals("deeplink", payload.getActionButtons().get(0).getLabel());
        Assert.assertEquals("notificationapp://", payload.getActionButtons().get(0).getLink());
        Assert.assertEquals("DEEPLINK", payload.getActionButtons().get(0).getType().name());
        Assert.assertEquals(mockData, payload.getData());
    }

    @Test
    public void putDataInExtras_withValidData() {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingConstants.Push.PayloadKeys.TITLE, "mockTitle");
        mockData.put(MessagingConstants.Push.PayloadKeys.BODY, "mockBody");
        mockData.put(MessagingConstants.Push.PayloadKeys.SOUND, "mockSound");
        mockData.put(MessagingConstants.Push.PayloadKeys.BADGE_NUMBER, "1");
        mockData.put(MessagingConstants.Push.PayloadKeys.NOTIFICATION_VISIBILITY, "PRIVATE");
        mockData.put(MessagingConstants.Push.PayloadKeys.NOTIFICATION_PRIORITY, "PRIORITY_MAX");
        mockData.put(MessagingConstants.Push.PayloadKeys.CHANNEL_ID, "mockChannelId");
        mockData.put(MessagingConstants.Push.PayloadKeys.ICON, "mockIcon");
        mockData.put(MessagingConstants.Push.PayloadKeys.IMAGE_URL, "mockImageUrl");
        mockData.put(MessagingConstants.Push.PayloadKeys.ACTION_TYPE, "DEEPLINK");
        mockData.put(MessagingConstants.Push.PayloadKeys.ACTION_URI, "mockActionUri");
        mockData.put(MessagingConstants.Push.PayloadKeys.ACTION_BUTTONS, "mockActionButtons");
        mockData.put(MessagingConstants.Push.PayloadKeys.INAPP_MESSAGE_ID, "mockInAppMessageId");
        MessagingPushPayload payload = new MessagingPushPayload(mockData);
        Intent mockIntent = mock(Intent.class);

        payload.putDataInExtras(mockIntent);

        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.TITLE, "mockTitle");
        verify(mockIntent, times(1)).putExtra(MessagingConstants.Push.PayloadKeys.BODY, "mockBody");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.SOUND, "mockSound");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.BADGE_NUMBER, "1");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.NOTIFICATION_VISIBILITY, "PRIVATE");
        verify(mockIntent, times(1))
                .putExtra(
                        MessagingConstants.Push.PayloadKeys.NOTIFICATION_PRIORITY, "PRIORITY_MAX");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.CHANNEL_ID, "mockChannelId");
        verify(mockIntent, times(1)).putExtra(MessagingConstants.Push.PayloadKeys.ICON, "mockIcon");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.IMAGE_URL, "mockImageUrl");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.ACTION_TYPE, "DEEPLINK");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.ACTION_URI, "mockActionUri");
        verify(mockIntent, times(1))
                .putExtra(MessagingConstants.Push.PayloadKeys.ACTION_BUTTONS, "mockActionButtons");
        verify(mockIntent, times(1))
                .putExtra(
                        MessagingConstants.Push.PayloadKeys.INAPP_MESSAGE_ID, "mockInAppMessageId");
    }

    @Test
    public void putDataInExtras_withEmptyData() {
        Map<String, String> mockData = new HashMap<>();
        MessagingPushPayload payload = new MessagingPushPayload(mockData);
        Intent mockIntent = mock(Intent.class);

        payload.putDataInExtras(mockIntent);

        verify(mockIntent, never()).putExtra(anyString(), anyString());
    }

    @Test
    public void putDataInExtras_withNullValue() {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingConstants.Push.PayloadKeys.TITLE, null);
        MessagingPushPayload payload = new MessagingPushPayload(mockData);
        Intent mockIntent = mock(Intent.class);

        payload.putDataInExtras(mockIntent);

        verify(mockIntent, never()).putExtra(anyString(), anyString());
    }

    @Test
    public void putDataInExtras_withEmptyValue() {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingConstants.Push.PayloadKeys.TITLE, "");
        MessagingPushPayload payload = new MessagingPushPayload(mockData);
        Intent mockIntent = mock(Intent.class);

        payload.putDataInExtras(mockIntent);

        verify(mockIntent, never()).putExtra(anyString(), anyString());
    }

    @Test
    public void putDataInExtras_withNullIntent_shouldNotThrowOrCallPutExtra() {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingConstants.Push.PayloadKeys.TITLE, "mockTitle");
        MessagingPushPayload payload = new MessagingPushPayload(mockData);

        // Should not throw or call putExtra
        payload.putDataInExtras(null);
    }

    @Test
    public void putDataInExtras_withNullData_shouldNotThrowOrCallPutExtra() {
        MessagingPushPayload payload =
                Mockito.mock(MessagingPushPayload.class, Mockito.CALLS_REAL_METHODS);
        Intent mockIntent = mock(Intent.class);

        // Set data to null
        Mockito.doReturn(null).when(payload).getData();

        payload.putDataInExtras(mockIntent);

        verify(mockIntent, never()).putExtra(anyString(), anyString());
    }

    // ========================================================================================
    // Helper
    // ========================================================================================
    private Map<String, String> getMockData(boolean testingMalformedButtonString) {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingTestConstants.Push.PayloadKeys.TITLE, mockTitle);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.BODY, mockBody);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.SOUND, mockSound);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.BADGE_NUMBER, mockBadgeCount);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.NOTIFICATION_PRIORITY, mockPriority);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.CHANNEL_ID, mockChannelId);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.ICON, mockIcon);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.IMAGE_URL, mockImageUrl);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.ACTION_TYPE, mockActionType);
        mockData.put(MessagingTestConstants.Push.PayloadKeys.ACTION_URI, mockActionUri);
        if (testingMalformedButtonString) {
            mockData.put(
                    MessagingTestConstants.Push.PayloadKeys.ACTION_BUTTONS,
                    mockMalformedActionButtons);
        } else {
            mockData.put(MessagingTestConstants.Push.PayloadKeys.ACTION_BUTTONS, mockActionButtons);
        }

        return mockData;
    }
}
