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

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.Inbound.PropositionEventType.*;
import static org.junit.Assert.assertEquals;

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import org.junit.Test;

public class MessagingEdgeEventTypeTests {

    @Test
    public void test_MessagingEdgeEventType_valueOf() {
        assertEquals(MessagingEdgeEventType.DISMISS, MessagingEdgeEventType.valueOf("DISMISS"));
        assertEquals(MessagingEdgeEventType.INTERACT, MessagingEdgeEventType.valueOf("INTERACT"));
        assertEquals(MessagingEdgeEventType.TRIGGER, MessagingEdgeEventType.valueOf("TRIGGER"));
        assertEquals(MessagingEdgeEventType.DISPLAY, MessagingEdgeEventType.valueOf("DISPLAY"));
        assertEquals(
                MessagingEdgeEventType.DISQUALIFY, MessagingEdgeEventType.valueOf("DISQUALIFY"));
        assertEquals(
                MessagingEdgeEventType.SUPPRESS_DISPLAY,
                MessagingEdgeEventType.valueOf("SUPPRESS_DISPLAY"));
        assertEquals(
                MessagingEdgeEventType.PUSH_APPLICATION_OPENED,
                MessagingEdgeEventType.valueOf("PUSH_APPLICATION_OPENED"));
        assertEquals(
                MessagingEdgeEventType.PUSH_CUSTOM_ACTION,
                MessagingEdgeEventType.valueOf("PUSH_CUSTOM_ACTION"));
    }

    @Test
    public void test_MessagingEdgeEventType_getValue() {
        assertEquals(6, MessagingEdgeEventType.DISMISS.getValue());
        assertEquals(7, MessagingEdgeEventType.INTERACT.getValue());
        assertEquals(8, MessagingEdgeEventType.TRIGGER.getValue());
        assertEquals(9, MessagingEdgeEventType.DISPLAY.getValue());
        assertEquals(10, MessagingEdgeEventType.DISQUALIFY.getValue());
        assertEquals(11, MessagingEdgeEventType.SUPPRESS_DISPLAY.getValue());
        assertEquals(4, MessagingEdgeEventType.PUSH_APPLICATION_OPENED.getValue());
        assertEquals(5, MessagingEdgeEventType.PUSH_CUSTOM_ACTION.getValue());
    }

    @Test
    public void test_MessagingEdgeEventType_getPropositionEventType() {
        assertEquals(
                PROPOSITION_EVENT_TYPE_DISMISS,
                MessagingEdgeEventType.DISMISS.getPropositionEventType());
        assertEquals(
                PROPOSITION_EVENT_TYPE_INTERACT,
                MessagingEdgeEventType.INTERACT.getPropositionEventType());
        assertEquals(
                PROPOSITION_EVENT_TYPE_TRIGGER,
                MessagingEdgeEventType.TRIGGER.getPropositionEventType());
        assertEquals(
                PROPOSITION_EVENT_TYPE_DISPLAY,
                MessagingEdgeEventType.DISPLAY.getPropositionEventType());
        assertEquals(
                PROPOSITION_EVENT_TYPE_DISQUALIFY,
                MessagingEdgeEventType.DISQUALIFY.getPropositionEventType());
        assertEquals(
                PROPOSITION_EVENT_TYPE_SUPPRESS_DISPLAY,
                MessagingEdgeEventType.SUPPRESS_DISPLAY.getPropositionEventType());
    }

    @Test
    public void test_MessagingEdgeEventType_toString() {
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.DISMISS,
                MessagingEdgeEventType.DISMISS.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.INTERACT,
                MessagingEdgeEventType.INTERACT.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.TRIGGER,
                MessagingEdgeEventType.TRIGGER.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.DISPLAY,
                MessagingEdgeEventType.DISPLAY.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.DISQUALIFY,
                MessagingEdgeEventType.DISQUALIFY.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys
                        .EventType.OPENED,
                MessagingEdgeEventType.PUSH_APPLICATION_OPENED.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys
                        .EventType.CUSTOM_ACTION,
                MessagingEdgeEventType.PUSH_CUSTOM_ACTION.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.SUPPRESS_DISPLAY,
                MessagingEdgeEventType.SUPPRESS_DISPLAY.toString());
    }
}
