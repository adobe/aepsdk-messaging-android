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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;

import com.adobe.marketing.mobile.MessagingEdgeEventType;

import org.junit.Test;

public class MessagingEdgeEventTypeTests {

    @Test
    public void test_MessagingEdgeEventType_valueOf() {
        assertEquals(MessagingEdgeEventType.IN_APP_DISMISS, MessagingEdgeEventType.valueOf("IN_APP_DISMISS"));
        assertEquals(MessagingEdgeEventType.IN_APP_INTERACT, MessagingEdgeEventType.valueOf("IN_APP_INTERACT"));
        assertEquals(MessagingEdgeEventType.IN_APP_TRIGGER, MessagingEdgeEventType.valueOf("IN_APP_TRIGGER"));
        assertEquals(MessagingEdgeEventType.IN_APP_DISPLAY, MessagingEdgeEventType.valueOf("IN_APP_DISPLAY"));
        assertEquals(MessagingEdgeEventType.PUSH_APPLICATION_OPENED, MessagingEdgeEventType.valueOf("PUSH_APPLICATION_OPENED"));
        assertEquals(MessagingEdgeEventType.PUSH_CUSTOM_ACTION, MessagingEdgeEventType.valueOf("PUSH_CUSTOM_ACTION"));
    }

    @Test
    public void test_MessagingEdgeEventType_getValue() {
        assertEquals(0, MessagingEdgeEventType.IN_APP_DISMISS.getValue());
        assertEquals(1, MessagingEdgeEventType.IN_APP_INTERACT.getValue());
        assertEquals(2, MessagingEdgeEventType.IN_APP_TRIGGER.getValue());
        assertEquals(3, MessagingEdgeEventType.IN_APP_DISPLAY.getValue());
        assertEquals(4, MessagingEdgeEventType.PUSH_APPLICATION_OPENED.getValue());
        assertEquals(5, MessagingEdgeEventType.PUSH_CUSTOM_ACTION.getValue());
    }

    @Test
    public void test_MessagingEdgeEventType_toString() {
        assertEquals(MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.DISMISS, MessagingEdgeEventType.IN_APP_DISMISS.toString());
        assertEquals(MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.INTERACT, MessagingEdgeEventType.IN_APP_INTERACT.toString());
        assertEquals(MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.TRIGGER, MessagingEdgeEventType.IN_APP_TRIGGER.toString());
        assertEquals(MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.DISPLAY, MessagingEdgeEventType.IN_APP_DISPLAY.toString());
        assertEquals(MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.EventType.OPENED, MessagingEdgeEventType.PUSH_APPLICATION_OPENED.toString());
        assertEquals(MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.EventType.CUSTOM_ACTION, MessagingEdgeEventType.PUSH_CUSTOM_ACTION.toString());
    }
}
