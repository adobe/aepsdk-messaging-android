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

import static org.mockito.Mockito.when;

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

    // ========================================================================================
    // Helper
    // ========================================================================================
    private Map<String, String> getMockData(boolean testingMalformedButtonString) {
        Map<String, String> mockData = new HashMap<>();
        mockData.put(MessagingConstants.Push.PayloadKeys.TITLE, mockTitle);
        mockData.put(MessagingConstants.Push.PayloadKeys.BODY, mockBody);
        mockData.put(MessagingConstants.Push.PayloadKeys.SOUND, mockSound);
        mockData.put(MessagingConstants.Push.PayloadKeys.BADGE_NUMBER, mockBadgeCount);
        mockData.put(MessagingConstants.Push.PayloadKeys.NOTIFICATION_PRIORITY, mockPriority);
        mockData.put(MessagingConstants.Push.PayloadKeys.CHANNEL_ID, mockChannelId);
        mockData.put(MessagingConstants.Push.PayloadKeys.ICON, mockIcon);
        mockData.put(MessagingConstants.Push.PayloadKeys.IMAGE_URL, mockImageUrl);
        mockData.put(MessagingConstants.Push.PayloadKeys.ACTION_TYPE, mockActionType);
        mockData.put(MessagingConstants.Push.PayloadKeys.ACTION_URI, mockActionUri);
        if (testingMalformedButtonString) {
            mockData.put(
                    MessagingConstants.Push.PayloadKeys.ACTION_BUTTONS, mockMalformedActionButtons);
        } else {
            mockData.put(MessagingConstants.Push.PayloadKeys.ACTION_BUTTONS, mockActionButtons);
        }

        return mockData;
    }
}
