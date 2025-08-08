/*
  Copyright 2024 Adobe. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class PropositionInteractionTests {

    private PropositionInfo mockPropositionInfo;

    @Before
    public void before() throws Exception {
        Map<String, Object> scopeDetails = new HashMap<>();
        scopeDetails.put("key", "value");
        Map<String, Object> propositionInfoMap = new HashMap<>();
        propositionInfoMap.put("id", "mockPropositionId");
        propositionInfoMap.put("scope", "mockScope");
        propositionInfoMap.put("scopeDetails", scopeDetails);
        mockPropositionInfo = PropositionInfo.create(propositionInfoMap);
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsNullWhenPropositionInfoIsNull() {
        // setup
        PropositionInteraction messagingPropositionInteraction =
                new PropositionInteraction(
                        MessagingEdgeEventType.DISMISS, "interaction", null, "mockItemId", null);

        // test
        Map<String, Object> result = messagingPropositionInteraction.getPropositionInteractionXDM();

        // verify
        assertNull(result);
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsNullWhenEventTypeIsNull() {
        // setup
        PropositionInteraction messagingPropositionInteraction =
                new PropositionInteraction(
                        null, "interaction", mockPropositionInfo, "mockItemId", null);

        // test
        Map<String, Object> result = messagingPropositionInteraction.getPropositionInteractionXDM();

        // verify
        assertNull(result);
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsValidMapWhenEventTypeIsInteract() {
        // setup
        String mockInteraction = "interaction";
        String mockItemId = "mockItemId";
        List<String> mockTokens = new ArrayList<>();
        mockTokens.add("token1");
        mockTokens.add("token2");
        PropositionInteraction messagingPropositionInteraction =
                new PropositionInteraction(
                        MessagingEdgeEventType.INTERACT,
                        mockInteraction,
                        mockPropositionInfo,
                        mockItemId,
                        mockTokens);

        // test
        Map<String, Object> xdm = messagingPropositionInteraction.getPropositionInteractionXDM();

        // verify
        assertNotNull(xdm);
        assertFalse(xdm.isEmpty());
        assertEquals(
                MessagingEdgeEventType.INTERACT.toString(),
                xdm.get(MessagingTestConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));
        Map<String, Object> experience =
                (Map<String, Object>) xdm.get(MessagingTestConstants.TrackingKeys.EXPERIENCE);
        assertNotNull(experience);

        Map<String, Object> decisioning =
                (Map<String, Object>)
                        experience.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .DECISIONING);
        assertNotNull(decisioning);

        Map<String, Object> propositionEventType =
                (Map<String, Object>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITION_EVENT_TYPE);
        assertNotNull(propositionEventType);
        assertEquals(1, propositionEventType.get("interact"));

        List<Map<String, Object>> propositions =
                (List<Map<String, Object>>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITIONS);
        assertNotNull(propositions);
        assertEquals(1, propositions.size());
        assertEquals(
                mockPropositionInfo.id,
                propositions
                        .get(0)
                        .get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(
                mockPropositionInfo.scope,
                propositions
                        .get(0)
                        .get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE));
        assertEquals(
                mockPropositionInfo.scopeDetails,
                propositions
                        .get(0)
                        .get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .SCOPE_DETAILS));

        List<Map<String, Object>> items =
                (List<Map<String, Object>>)
                        propositions
                                .get(0)
                                .get(
                                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                                .ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(
                mockItemId,
                items.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        Map<String, Object> characteristics =
                (Map<String, Object>)
                        items.get(0)
                                .get(
                                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                                .CHARACTERISTICS);
        assertNotNull(characteristics);
        String tokens =
                (String)
                        characteristics.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.TOKENS);
        assertEquals("token1,token2", tokens);

        Map<String, Object> propositionAction =
                (Map<String, Object>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITION_ACTION);
        assertNotNull(propositionAction);
        assertEquals(2, propositionAction.size());
        assertEquals(
                mockInteraction,
                propositionAction.get(
                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(
                mockInteraction,
                propositionAction.get(
                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.LABEL));
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsValidMapWhenEventTypeIsDisplay() {
        // setup
        String mockItemId = "mockItemId";
        PropositionInteraction messagingPropositionInteraction =
                new PropositionInteraction(
                        MessagingEdgeEventType.DISPLAY,
                        null,
                        mockPropositionInfo,
                        mockItemId,
                        null);

        // test
        Map<String, Object> xdm = messagingPropositionInteraction.getPropositionInteractionXDM();

        // verify
        assertNotNull(xdm);
        assertFalse(xdm.isEmpty());
        assertEquals(
                MessagingEdgeEventType.DISPLAY.toString(),
                xdm.get(MessagingTestConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));
        Map<String, Object> experience =
                (Map<String, Object>) xdm.get(MessagingTestConstants.TrackingKeys.EXPERIENCE);
        assertNotNull(experience);

        Map<String, Object> decisioning =
                (Map<String, Object>)
                        experience.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .DECISIONING);
        assertNotNull(decisioning);

        Map<String, Object> propositionEventType =
                (Map<String, Object>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITION_EVENT_TYPE);
        assertNotNull(propositionEventType);
        assertEquals(1, propositionEventType.get("display"));

        List<Map<String, Object>> propositions =
                (List<Map<String, Object>>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITIONS);
        assertNotNull(propositions);
        assertEquals(1, propositions.size());
        assertEquals(
                mockPropositionInfo.id,
                propositions
                        .get(0)
                        .get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(
                mockPropositionInfo.scope,
                propositions
                        .get(0)
                        .get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE));
        assertEquals(
                mockPropositionInfo.scopeDetails,
                propositions
                        .get(0)
                        .get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .SCOPE_DETAILS));

        List<Map<String, Object>> items =
                (List<Map<String, Object>>)
                        propositions
                                .get(0)
                                .get(
                                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                                .ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(
                mockItemId,
                items.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));

        assertNull(
                decisioning.get(
                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                .PROPOSITION_ACTION));
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsValidMapWhenEventTypeIsSuppressDisplay() {
        // setup
        String mockItemId = "mockItemId";
        String mockInteraction = "mockInteraction";
        PropositionInteraction messagingPropositionInteraction =
                new PropositionInteraction(
                        MessagingEdgeEventType.SUPPRESS_DISPLAY,
                        mockInteraction,
                        mockPropositionInfo,
                        mockItemId,
                        null);

        // test
        Map<String, Object> xdm = messagingPropositionInteraction.getPropositionInteractionXDM();

        // verify
        assertNotNull(xdm);
        assertFalse(xdm.isEmpty());
        assertEquals(
                MessagingEdgeEventType.SUPPRESS_DISPLAY.toString(),
                xdm.get(MessagingTestConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));
        Map<String, Object> experience =
                (Map<String, Object>) xdm.get(MessagingTestConstants.TrackingKeys.EXPERIENCE);
        assertNotNull(experience);

        Map<String, Object> decisioning =
                (Map<String, Object>)
                        experience.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .DECISIONING);
        assertNotNull(decisioning);

        Map<String, Object> propositionEventType =
                (Map<String, Object>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITION_EVENT_TYPE);
        assertNotNull(propositionEventType);

        List<Map<String, Object>> propositions =
                (List<Map<String, Object>>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITIONS);
        assertNotNull(propositions);
        assertEquals(1, propositions.size());
        assertEquals(
                mockPropositionInfo.id,
                propositions
                        .get(0)
                        .get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(
                mockPropositionInfo.scope,
                propositions
                        .get(0)
                        .get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE));
        assertEquals(
                mockPropositionInfo.scopeDetails,
                propositions
                        .get(0)
                        .get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .SCOPE_DETAILS));

        List<Map<String, Object>> items =
                (List<Map<String, Object>>)
                        propositions
                                .get(0)
                                .get(
                                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                                .ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(
                mockItemId,
                items.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));

        Map<String, Object> propositionAction =
                (Map<String, Object>)
                        decisioning.get(
                                MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key
                                        .PROPOSITION_ACTION);
        assertNotNull(propositionAction);
        assertEquals(1, propositionAction.size());
        assertEquals(
                mockInteraction,
                propositionAction.get(
                        MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.REASON));
    }
}
