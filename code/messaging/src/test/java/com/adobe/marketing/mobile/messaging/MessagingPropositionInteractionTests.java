package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.adobe.marketing.mobile.MessagingEdgeEventType;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingPropositionInteractionTests {

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
        //setup
        MessagingPropositionInteraction messagingPropositionInteraction = new MessagingPropositionInteraction(null, "interaction", null, "mockItemId");

        //test
        Map<String, Object> result = messagingPropositionInteraction.getPropositionInteractionXDM();

        //verify
        assertNull(result);
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsValidMapWhenEventTypeIsInteract() {
        //setup
        String mockInteraction ="interaction";
        String mockItemId = "mockItemId";
        MessagingPropositionInteraction messagingPropositionInteraction = new MessagingPropositionInteraction(
                MessagingEdgeEventType.INTERACT, mockInteraction, mockPropositionInfo, mockItemId);

        //test
        Map<String, Object> xdm = messagingPropositionInteraction.getPropositionInteractionXDM();

        //verify
        assertNotNull(xdm);
        assertFalse(xdm.isEmpty());
        assertEquals(MessagingEdgeEventType.INTERACT.toString(), xdm.get(MessagingTestConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));
        Map<String, Object> experience = (Map<String, Object>) xdm.get(MessagingTestConstants.TrackingKeys.EXPERIENCE);
        assertNotNull(experience);

        Map<String, Object> decisioning = (Map<String, Object>) experience.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.DECISIONING);
        assertNotNull(decisioning);

        Map<String, Object> propositionEventType = (Map<String, Object>) decisioning.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_EVENT_TYPE);
        assertNotNull(propositionEventType);
        assertEquals(1, propositionEventType.get("interact"));

        List<Map<String, Object>> propositions = (List<Map<String, Object>>) decisioning.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS);
        assertNotNull(propositions);
        assertEquals(1, propositions.size());
        assertEquals(mockPropositionInfo.id, propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(mockPropositionInfo.scope, propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE));
        assertEquals(mockPropositionInfo.scopeDetails, propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE_DETAILS));

        List<Map<String, Object>> items = (List<Map<String, Object>>) propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(mockItemId, items.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));

        Map<String, Object> propositionAction = (Map<String, Object>) decisioning.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_ACTION);
        assertNotNull(propositionAction);
        assertEquals(2, propositionAction.size());
        assertEquals(mockInteraction, propositionAction.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(mockInteraction, propositionAction.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.LABEL));
    }

    @Test
    public void test_getPropositionInteractionXDM_returnsValidMapWhenEventTypeIsDisplay() {
        //setup
        String mockItemId = "mockItemId";
        MessagingPropositionInteraction messagingPropositionInteraction = new MessagingPropositionInteraction(MessagingEdgeEventType.DISPLAY, null, mockPropositionInfo, mockItemId);

        //test
        Map<String, Object> xdm = messagingPropositionInteraction.getPropositionInteractionXDM();

        //verify
        assertNotNull(xdm);
        assertFalse(xdm.isEmpty());
        assertEquals(MessagingEdgeEventType.DISPLAY.toString(), xdm.get(MessagingTestConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));
        Map<String, Object> experience = (Map<String, Object>) xdm.get(MessagingTestConstants.TrackingKeys.EXPERIENCE);
        assertNotNull(experience);

        Map<String, Object> decisioning = (Map<String, Object>) experience.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.DECISIONING);
        assertNotNull(decisioning);

        Map<String, Object> propositionEventType = (Map<String, Object>) decisioning.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_EVENT_TYPE);
        assertNotNull(propositionEventType);
        assertEquals(1, propositionEventType.get("display"));

        List<Map<String, Object>> propositions = (List<Map<String, Object>>) decisioning.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS);
        assertNotNull(propositions);
        assertEquals(1, propositions.size());
        assertEquals(mockPropositionInfo.id, propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));
        assertEquals(mockPropositionInfo.scope, propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE));
        assertEquals(mockPropositionInfo.scopeDetails, propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE_DETAILS));

        List<Map<String, Object>> items = (List<Map<String, Object>>) propositions.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ITEMS);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals(mockItemId, items.get(0).get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.ID));

        assertNull(decisioning.get(MessagingTestConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_ACTION));
    }
}
