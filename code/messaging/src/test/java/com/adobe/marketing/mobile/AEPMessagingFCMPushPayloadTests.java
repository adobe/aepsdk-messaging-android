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

package com.adobe.marketing.mobile;

import android.os.Bundle;

import com.google.firebase.messaging.RemoteMessage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
public class AEPMessagingFCMPushPayloadTests {
    private AEPMessagingFCMPushPayload payload;

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
    private final String mockActionButtons = "[\n            {\n \"label\" : \"deeplink\",\n \"uri\" : \"notificationapp://\",\n \"type\" : \"DEEPLINK\"\n },\n {\n \"label\" : \"weburl\",\n \"uri\" : \"https://www.yahoo.com\",\n \"type\" : \"WEBURL\"\n},\n{\n\"label\" : \"dismiss\",\n\"uri\" : \"\",\n \"type\" : \"DISMISS\"\n}\n]";
    private Map<String, String> mockData;

    @Before
    public void setup() {
        mockData = getMockData();
        payload = new AEPMessagingFCMPushPayload(mockData);
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor_with_RemoteMessage() {
        Bundle bundle = Mockito.mock(Bundle.class);
        RemoteMessage message = new RemoteMessage(bundle);
        payload = new AEPMessagingFCMPushPayload(message);
        Assert.assertNotNull(payload);
    }

    @Test
    public void test_Constructor_with_MapData() {
        Map<String, String> data = new HashMap<>();
        payload = new AEPMessagingFCMPushPayload(data);
        Assert.assertNotNull(payload);
    }

    // ========================================================================================
    // public methods
    // ========================================================================================
    @Test
    public void test_PublicMethods() {
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
    }

    // ========================================================================================
    // Helper
    // ========================================================================================
    private Map<String, String> getMockData() {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingConstant.PushNotificationPayload.TITLE, mockTitle);
        mockData.put(MessagingConstant.PushNotificationPayload.BODY, mockBody);
        mockData.put(MessagingConstant.PushNotificationPayload.SOUND, mockSound);
        mockData.put(MessagingConstant.PushNotificationPayload.NOTIFICATION_COUNT, mockBadgeCount);
        mockData.put(MessagingConstant.PushNotificationPayload.NOTIFICATION_PRIORITY, mockPriority);
        mockData.put(MessagingConstant.PushNotificationPayload.CHANNEL_ID, mockChannelId);
        mockData.put(MessagingConstant.PushNotificationPayload.ICON, mockIcon);
        mockData.put(MessagingConstant.PushNotificationPayload.IMAGE_URL, mockImageUrl);
        mockData.put(MessagingConstant.PushNotificationPayload.ACTION_TYPE, mockActionType);
        mockData.put(MessagingConstant.PushNotificationPayload.ACTION_URI, mockActionUri);
        mockData.put(MessagingConstant.PushNotificationPayload.ACTION_BUTTONS, mockActionButtons);
        return mockData;
    }
}
