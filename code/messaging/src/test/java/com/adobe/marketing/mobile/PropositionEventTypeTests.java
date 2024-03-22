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

import com.adobe.marketing.mobile.messaging.MessagingTestConstants;
import org.junit.Test;

public class PropositionEventTypeTests {

    @Test
    public void test_PropositionEventType_valueOf() {
        assertEquals(
                PropositionEventType.IN_APP_DISMISS,
                PropositionEventType.valueOf("IN_APP_DISMISS"));
        assertEquals(
                PropositionEventType.IN_APP_INTERACT,
                PropositionEventType.valueOf("IN_APP_INTERACT"));
        assertEquals(
                PropositionEventType.IN_APP_TRIGGER,
                PropositionEventType.valueOf("IN_APP_TRIGGER"));
        assertEquals(
                PropositionEventType.IN_APP_DISPLAY,
                PropositionEventType.valueOf("IN_APP_DISPLAY"));
    }

    @Test
    public void test_PropositionEventType_getValue() {
        assertEquals(0, PropositionEventType.IN_APP_DISMISS.getValue());
        assertEquals(1, PropositionEventType.IN_APP_INTERACT.getValue());
        assertEquals(2, PropositionEventType.IN_APP_TRIGGER.getValue());
        assertEquals(3, PropositionEventType.IN_APP_DISPLAY.getValue());
    }

    @Test
    public void test_PropositionEventType_toString() {
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.DISMISS,
                PropositionEventType.IN_APP_DISMISS.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.INTERACT,
                PropositionEventType.IN_APP_INTERACT.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.TRIGGER,
                PropositionEventType.IN_APP_TRIGGER.toString());
        assertEquals(
                MessagingTestConstants.EventDataKeys.Messaging.Inbound.EventType.DISPLAY,
                PropositionEventType.IN_APP_DISPLAY.toString());
    }

    @Test
    public void test_PropositionEventType_getPropositionEventType() {
        assertEquals(
                PropositionEventType.PROPOSITION_EVENT_TYPE_DISMISS,
                PropositionEventType.IN_APP_DISMISS.getPropositionEventType());
        assertEquals(
                PropositionEventType.PROPOSITION_EVENT_TYPE_INTERACT,
                PropositionEventType.IN_APP_INTERACT.getPropositionEventType());
        assertEquals(
                PropositionEventType.PROPOSITION_EVENT_TYPE_TRIGGER,
                PropositionEventType.IN_APP_TRIGGER.getPropositionEventType());
        assertEquals(
                PropositionEventType.PROPOSITION_EVENT_TYPE_DISPLAY,
                PropositionEventType.IN_APP_DISPLAY.getPropositionEventType());
    }
}
