/*
  Copyright 2025 Adobe. All rights reserved.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionInteractionBatcherTests {

    private static final String MOCK_INTERACTION = "test_interaction";
    private static final String MOCK_ITEM_ID_1 = "item_id_1";
    private static final String MOCK_ITEM_ID_2 = "item_id_2";
    private static final String MOCK_PROPOSITION_ID = "proposition_id";
    private static final String MOCK_SCOPE = "mobileapp://test/scope";

    private MessagingEdgeEventType mockEventType;
    private PropositionItem mockPropositionItem1;
    private PropositionItem mockPropositionItem2;
    private Proposition mockProposition;
    private PropositionInfo mockPropositionInfo;
    private List<PropositionItem> propositionItems;

    @Before
    public void setup() {
        mockEventType = MessagingEdgeEventType.DISPLAY;

        // Setup mock proposition
        mockProposition = mock(Proposition.class);
        when(mockProposition.getUniqueId()).thenReturn(MOCK_PROPOSITION_ID);
        when(mockProposition.getScope()).thenReturn(MOCK_SCOPE);
        when(mockProposition.getScopeDetails()).thenReturn(new HashMap<>());

        // Setup mock proposition info
        mockPropositionInfo = PropositionInfo.createFromProposition(mockProposition);

        // Setup mock proposition items
        mockPropositionItem1 = mock(PropositionItem.class);
        when(mockPropositionItem1.getItemId()).thenReturn(MOCK_ITEM_ID_1);
        when(mockPropositionItem1.getProposition()).thenReturn(mockProposition);

        mockPropositionItem2 = mock(PropositionItem.class);
        when(mockPropositionItem2.getItemId()).thenReturn(MOCK_ITEM_ID_2);
        when(mockPropositionItem2.getProposition()).thenReturn(mockProposition);

        propositionItems = new ArrayList<>();
        propositionItems.add(mockPropositionItem1);
        propositionItems.add(mockPropositionItem2);
    }

    @Test
    public void test_generateBatchedXdmMap_ValidPropositionItems() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem2.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType, MOCK_INTERACTION, propositionItems);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            assertFalse(xdm.isEmpty());

            // Verify XDM structure
            assertEquals(
                    mockEventType.toString(),
                    xdm.get(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));

            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            Map<String, Object> propositionEventType =
                    (Map<String, Object>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_EVENT_TYPE);
            assertEquals(1, propositionEventType.get("display"));

            List<Map<String, Object>> propositions =
                    (List<Map<String, Object>>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITIONS);
            assertEquals(2, propositions.size());

            // Verify each proposition detail contains the expected test data
            for (Map<String, Object> propositionDetail : propositions) {
                assertEquals("testValue", propositionDetail.get("testKey"));
            }

            // Verify PropositionInfo.createFromProposition was called for each item
            propositionInfoMockedStatic.verify(
                    () -> PropositionInfo.createFromProposition(mockProposition), Mockito.times(2));

            // Verify PropositionInteraction was constructed for each item
            assertEquals(2, propositionInteractionMockedConstruction.constructed().size());
        }
    }

    @Test
    public void test_generateBatchedXdmMap_NullPropositionItems() {
        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(mockEventType, MOCK_INTERACTION, null);
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_EmptyPropositionItems() {
        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(
                        mockEventType, MOCK_INTERACTION, new ArrayList<>());
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_NullPropositionItem() {
        // setup
        List<PropositionItem> itemsWithNull = new ArrayList<>();
        itemsWithNull.add(null);
        itemsWithNull.add(mockPropositionItem1);

        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType, MOCK_INTERACTION, itemsWithNull);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            Map<String, Object> propositionEventType =
                    (Map<String, Object>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_EVENT_TYPE);
            assertEquals(1, propositionEventType.get("display"));

            List<Map<String, Object>> propositions =
                    (List<Map<String, Object>>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITIONS);
            assertEquals(1, propositions.size());
            Map<String, Object> propositionDetail = propositions.get(0);

            // Verify proposition detail contains the expected test data
            assertEquals("testValue", propositionDetail.get("testKey"));

            // Should only process the non-null item
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());
        }
    }

    @Test
    public void test_generateBatchedXdmMap_NullPropositionReference() {
        // setup
        mockPropositionItem1.propositionReference = null;
        mockPropositionItem2.propositionReference = null;

        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(
                        mockEventType, MOCK_INTERACTION, propositionItems);
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_ReleasedPropositionReference() {
        // setup
        SoftReference<Proposition> releasedReference = new SoftReference<>(null);
        mockPropositionItem1.propositionReference = releasedReference;
        mockPropositionItem2.propositionReference = releasedReference;

        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(
                        mockEventType, MOCK_INTERACTION, propositionItems);
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_PropositionInfoCreationFails() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                mockStatic(PropositionInfo.class)) {
            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(null);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType, MOCK_INTERACTION, propositionItems);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNull(xdm);
        }
    }

    @Test
    public void test_generateBatchedXdmMap_MixedValidAndInvalidItems() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem2.propositionReference = new SoftReference<>(null);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType, MOCK_INTERACTION, propositionItems);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);

            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            Map<String, Object> propositionEventType =
                    (Map<String, Object>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_EVENT_TYPE);
            assertEquals(1, propositionEventType.get("display"));

            List<Map<String, Object>> propositions =
                    (List<Map<String, Object>>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITIONS);
            assertEquals(1, propositions.size());
            Map<String, Object> propositionDetail = propositions.get(0);

            // Verify proposition detail contains the expected test data
            assertEquals("testValue", propositionDetail.get("testKey"));

            // Should only process the valid item
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());
        }
    }

    @Test
    public void test_generateBatchedXdmMap_WithDifferentEventTypes() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);

        List<PropositionItem> singleItem = Collections.singletonList(mockPropositionItem1);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // Test with INTERACT event type
            PropositionInteractionBatcher interactBatcher =
                    new PropositionInteractionBatcher(
                            MessagingEdgeEventType.INTERACT, MOCK_INTERACTION, singleItem);
            Map<String, Object> xdm = interactBatcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());

            // Verify XDM structure for INTERACT event type
            assertEquals(
                    MessagingEdgeEventType.INTERACT.toString(),
                    xdm.get(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));

            // Verify propositionAction is included for INTERACT event type
            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            Map<String, String> propositionAction =
                    (Map<String, String>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_ACTION);
            assertEquals(
                    MOCK_INTERACTION,
                    propositionAction.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID));
            assertEquals(
                    MOCK_INTERACTION,
                    propositionAction.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key.LABEL));

            // Verify the values in propositionAction
            assertEquals("test_interaction", propositionAction.get("id"));
            assertEquals("test_interaction", propositionAction.get("label"));
            assertEquals(2, propositionAction.size()); // Should only have id and label
        }
    }

    @Test
    public void test_generateBatchedXdmMap_returnsNull_WhenPropositionItemsNull() {
        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(mockEventType, MOCK_INTERACTION, null);
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_returnsNull_WhenPropositionReferenceNotAvailable() {
        // setup
        mockPropositionItem1.propositionReference = null;

        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(
                        mockEventType,
                        MOCK_INTERACTION,
                        Collections.singletonList(mockPropositionItem1));
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_returnsNull_WhenPropositionInfoCreationFails() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem2.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                mockStatic(PropositionInfo.class)) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(null);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType,
                            MOCK_INTERACTION,
                            Arrays.asList(mockPropositionItem1, mockPropositionItem2));
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNull(xdm);
        }
    }

    @Test
    public void test_generateBatchedXdmMap_returnsNull_WhenNoValidItems() {
        // setup
        mockPropositionItem1.propositionReference = null;
        mockPropositionItem2.propositionReference = null;

        // test
        PropositionInteractionBatcher batcher =
                new PropositionInteractionBatcher(
                        mockEventType, MOCK_INTERACTION, propositionItems);
        Map<String, Object> xdm = batcher.generateBatchedXdmMap();

        // verify
        assertNull(xdm);
    }

    @Test
    public void test_generateBatchedXdmMap_WithMultipleValidItems() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem2.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType, MOCK_INTERACTION, propositionItems);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            assertFalse(xdm.isEmpty());

            // Verify XDM structure for multiple items
            assertEquals(
                    mockEventType.toString(),
                    xdm.get(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));

            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            Map<String, Object> propositionEventType =
                    (Map<String, Object>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_EVENT_TYPE);
            assertEquals(1, propositionEventType.get(mockEventType.getPropositionEventType()));

            List<Map<String, Object>> propositions =
                    (List<Map<String, Object>>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITIONS);
            assertEquals(2, propositions.size());

            // Verify each proposition detail contains the expected test data
            for (Map<String, Object> propositionDetail : propositions) {
                assertEquals("testValue", propositionDetail.get("testKey"));
            }

            // Verify the event type in the XDM structure
            assertEquals("decisioning.propositionDisplay", xdm.get("eventType"));

            // Verify PropositionInteraction was constructed for both items
            assertEquals(2, propositionInteractionMockedConstruction.constructed().size());

            // Verify the constructor arguments for each PropositionInteraction
            List<PropositionInteraction> constructedInteractions =
                    propositionInteractionMockedConstruction.constructed();
            PropositionInteraction interaction1 = constructedInteractions.get(0);
            PropositionInteraction interaction2 = constructedInteractions.get(1);
            assertNotNull(interaction1);
            assertNotNull(interaction2);
        }
    }

    @Test
    public void test_generateBatchedXdmMap_WithEmptyPropositionDetailsList() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem2.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            // Return null to simulate empty proposition details
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(null);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            mockEventType,
                            MOCK_INTERACTION,
                            Arrays.asList(mockPropositionItem1, mockPropositionItem2));
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            assertTrue(xdm.isEmpty());
        }
    }

    @Test
    public void test_generateBatchedXdmMap_WithNullEventType() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        mockPropositionItem2.propositionReference = new SoftReference<>(mockProposition);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                mockStatic(PropositionInfo.class)) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            null,
                            MOCK_INTERACTION,
                            Arrays.asList(mockPropositionItem1, mockPropositionItem2));
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNull(xdm);
        }
    }

    @Test
    public void test_generateBatchedXdmMap_WithSuppressDisplayEventType() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        List<PropositionItem> singleItem = Collections.singletonList(mockPropositionItem1);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            MessagingEdgeEventType.SUPPRESS_DISPLAY, MOCK_INTERACTION, singleItem);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());

            // Verify XDM structure for SUPPRESS_DISPLAY event type
            assertEquals(
                    MessagingEdgeEventType.SUPPRESS_DISPLAY.toString(),
                    xdm.get(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));

            // Verify propositionAction is included for SUPPRESS_DISPLAY event type with "reason"
            // field
            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            Map<String, String> propositionAction =
                    (Map<String, String>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_ACTION);
            assertEquals(
                    MOCK_INTERACTION,
                    propositionAction.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key.REASON));

            // Verify "id" and "label" are not present for SUPPRESS_DISPLAY
            assertNull(
                    propositionAction.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID));
            assertNull(
                    propositionAction.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key.LABEL));

            // Verify the values in propositionAction
            assertEquals("test_interaction", propositionAction.get("reason"));
            assertEquals(1, propositionAction.size()); // Should only have reason
        }
    }

    @Test
    public void test_generateBatchedXdmMap_WithDisplayEventType() {
        // setup
        mockPropositionItem1.propositionReference = new SoftReference<>(mockProposition);
        List<PropositionItem> singleItem = Collections.singletonList(mockPropositionItem1);

        try (MockedStatic<PropositionInfo> propositionInfoMockedStatic =
                        mockStatic(PropositionInfo.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            Map<String, Object> mockXdmMap = new HashMap<>();
                                            mockXdmMap.put("testKey", "testValue");
                                            when(mock.generatePropositionDetails())
                                                    .thenReturn(mockXdmMap);
                                        })) {

            propositionInfoMockedStatic
                    .when(() -> PropositionInfo.createFromProposition(any(Proposition.class)))
                    .thenReturn(mockPropositionInfo);

            // test
            PropositionInteractionBatcher batcher =
                    new PropositionInteractionBatcher(
                            MessagingEdgeEventType.DISPLAY, MOCK_INTERACTION, singleItem);
            Map<String, Object> xdm = batcher.generateBatchedXdmMap();

            // verify
            assertNotNull(xdm);
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());

            // Verify XDM structure for DISPLAY event type
            assertEquals(
                    MessagingEdgeEventType.DISPLAY.toString(),
                    xdm.get(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE));

            // Verify propositionAction is not included for DISPLAY event type (not INTERACT or
            // SUPPRESS_DISPLAY)
            Map<String, Object> experience =
                    (Map<String, Object>) xdm.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            assertNull(
                    decisioning.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                    .PROPOSITION_ACTION));

            // Verify the event type in the XDM structure
            assertEquals("decisioning.propositionDisplay", xdm.get("eventType"));
        }
    }
}
