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

package com.adobe.marketing.mobile.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionItemTests {

    @Mock ExtensionApi mockExtensionApi;

    String testId = "uniqueId";
    // test event data maps
    Map<String, Object> eventDataMapForJSON;
    Map<String, Object> eventDataMapForHTML;
    // expected schema data content
    Map<String, Object> htmlContentMap = new HashMap<>();
    Map<String, Object> jsonContentMap = new HashMap<>();

    @Before
    public void setup() throws JSONException {
        // setup event data for json content
        jsonContentMap = MessagingTestUtils.getMapFromFile("codeBasedPropositionJsonContent.json");
        Map<String, Object> codebasePropositionJson =
                MessagingTestUtils.getMapFromFile("codeBasedPropositionJson.json");
        List<Map<String, Object>> jsonItemsList =
                (List<Map<String, Object>>) codebasePropositionJson.get("items");
        eventDataMapForJSON = jsonItemsList.get(0);

        // setup event data for html content
        htmlContentMap = MessagingTestUtils.getMapFromFile("codeBasedPropositionHtmlContent.json");
        Map<String, Object> codebasePropositionHTML =
                MessagingTestUtils.getMapFromFile("codeBasedPropositionHtml.json");
        List<Map<String, Object>> htmlItemsList =
                (List<Map<String, Object>>) codebasePropositionHTML.get("items");
        eventDataMapForHTML = htmlItemsList.get(0);
    }

    @After
    public void tearDown() {
        reset(mockExtensionApi);
    }

    @Test
    public void test_constructor_validData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.HTML_CONTENT, htmlContentMap);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertEquals(htmlContentMap, propositionItem.getItemData());
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_constructor_nullItemId() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(null, SchemaType.HTML_CONTENT, htmlContentMap);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertNull(propositionItem.getItemData());
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_constructor_emptyItemId() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem("", SchemaType.HTML_CONTENT, htmlContentMap);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertNull(propositionItem.getItemData());
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_constructor_nullSchema() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, null, htmlContentMap);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertNull(propositionItem.getItemData());
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_constructor_nullItemData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.HTML_CONTENT, null);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertNull(propositionItem.getItemData());
    }

    @Test
    public void test_track_validEventType() throws MessageRequiredFieldMissingException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            propositionItem.propositionReference =
                    new SoftReference<>(Mockito.mock(Proposition.class));

            // test
            PropositionItem spyPropositionItem = Mockito.spy(propositionItem);
            spyPropositionItem.track(MessagingEdgeEventType.DISPLAY);

            // verify
            verify(spyPropositionItem)
                    .generateInteractionXdm(null, MessagingEdgeEventType.DISPLAY, null);
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());
            ArgumentCaptor<Event> trackingEventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(
                    () -> MobileCore.dispatchEvent(trackingEventCaptor.capture()));
            Event capturedTrackingEvent = trackingEventCaptor.getValue();
            assertEquals(
                    MessagingTestConstants.EventName.TRACK_PROPOSITIONS,
                    capturedTrackingEvent.getName());
            assertEquals(
                    MessagingTestConstants.EventType.MESSAGING, capturedTrackingEvent.getType());
            assertEquals(
                    MessagingTestConstants.EventSource.REQUEST_CONTENT,
                    capturedTrackingEvent.getSource());
            assertTrue(
                    (boolean)
                            capturedTrackingEvent
                                    .getEventData()
                                    .get(
                                            MessagingTestConstants.EventDataKeys.Messaging
                                                    .TRACK_PROPOSITIONS));
            assertEquals(
                    mockInteractionData,
                    capturedTrackingEvent
                            .getEventData()
                            .get(
                                    MessagingTestConstants.EventDataKeys.Messaging
                                            .PROPOSITION_INTERACTION));
        }
    }

    @Test
    public void test_track_nullPropositionReference() throws MessageRequiredFieldMissingException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            propositionItem.propositionReference = null;

            // test
            PropositionItem spyPropositionItem = Mockito.spy(propositionItem);
            spyPropositionItem.track(MessagingEdgeEventType.DISPLAY);

            // verify
            verify(spyPropositionItem)
                    .generateInteractionXdm(null, MessagingEdgeEventType.DISPLAY, null);
            assertEquals(0, propositionInteractionMockedConstruction.constructed().size());
            mobileCoreMockedStatic.verifyNoInteractions();
        }
    }

    @Test
    public void test_track_validEventTypeInteractionTokens()
            throws MessageRequiredFieldMissingException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            propositionItem.propositionReference =
                    new SoftReference<>(Mockito.mock(Proposition.class));

            // test
            PropositionItem spyPropositionItem = Mockito.spy(propositionItem);
            spyPropositionItem.track(
                    "mockInteraction",
                    MessagingEdgeEventType.INTERACT,
                    Arrays.asList("token1", "token2"));

            // verify
            verify(spyPropositionItem)
                    .generateInteractionXdm(
                            "mockInteraction",
                            MessagingEdgeEventType.INTERACT,
                            Arrays.asList("token1", "token2"));
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());
            ArgumentCaptor<Event> trackingEventCaptor = ArgumentCaptor.forClass(Event.class);
            mobileCoreMockedStatic.verify(
                    () -> MobileCore.dispatchEvent(trackingEventCaptor.capture()));
            Event capturedTrackingEvent = trackingEventCaptor.getValue();
            assertEquals(
                    MessagingTestConstants.EventName.TRACK_PROPOSITIONS,
                    capturedTrackingEvent.getName());
            assertEquals(
                    MessagingTestConstants.EventType.MESSAGING, capturedTrackingEvent.getType());
            assertEquals(
                    MessagingTestConstants.EventSource.REQUEST_CONTENT,
                    capturedTrackingEvent.getSource());
            assertTrue(
                    (boolean)
                            capturedTrackingEvent
                                    .getEventData()
                                    .get(
                                            MessagingTestConstants.EventDataKeys.Messaging
                                                    .TRACK_PROPOSITIONS));
            assertEquals(
                    mockInteractionData,
                    capturedTrackingEvent
                            .getEventData()
                            .get(
                                    MessagingTestConstants.EventDataKeys.Messaging
                                            .PROPOSITION_INTERACTION));
        }
    }

    @Test
    public void test_track_nullPropositionReferenceWithInteractionAndTokens()
            throws MessageRequiredFieldMissingException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };
        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            propositionItem.propositionReference = null;

            // test
            PropositionItem spyPropositionItem = Mockito.spy(propositionItem);
            spyPropositionItem.track(
                    "mockInteraction",
                    MessagingEdgeEventType.INTERACT,
                    Arrays.asList("token1", "token2"));

            // verify
            verify(spyPropositionItem)
                    .generateInteractionXdm(
                            "mockInteraction",
                            MessagingEdgeEventType.INTERACT,
                            Arrays.asList("token1", "token2"));
            assertEquals(0, propositionInteractionMockedConstruction.constructed().size());
            mobileCoreMockedStatic.verifyNoInteractions();
        }
    }

    @Test
    public void test_generateInteractionXdm_validEventType()
            throws MessageRequiredFieldMissingException {
        final Map<PropositionInteraction, List<Object>> constructorArgs = new HashMap<>();
        try (MockedConstruction<PropositionInteraction> propositionInteractionMockedConstruction =
                Mockito.mockConstruction(
                        PropositionInteraction.class,
                        (mock, context) -> {
                            constructorArgs.put(mock, new ArrayList<>(context.arguments()));
                            when(mock.getPropositionInteractionXDM())
                                    .thenReturn(
                                            new HashMap<String, Object>() {
                                                {
                                                    put("someKey", "someValue");
                                                }
                                            });
                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            Proposition propositionMock = Mockito.mock(Proposition.class);
            when(propositionMock.getUniqueId()).thenReturn("propositionId");
            propositionItem.propositionReference = new SoftReference<>(propositionMock);

            // test
            Map<String, Object> result =
                    propositionItem.generateInteractionXdm(MessagingEdgeEventType.DISPLAY);

            // verify
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());
            PropositionInteraction mockedPropositionInteraction =
                    propositionInteractionMockedConstruction.constructed().get(0);
            List<Object> propositionInteractionConstructorArgs =
                    constructorArgs.get(mockedPropositionInteraction);
            assertEquals(5, propositionInteractionConstructorArgs.size());
            assertEquals(
                    MessagingEdgeEventType.DISPLAY, propositionInteractionConstructorArgs.get(0));
            assertNull(propositionInteractionConstructorArgs.get(1));
            assertTrue(propositionInteractionConstructorArgs.get(2) instanceof PropositionInfo);
            assertEquals(
                    "propositionId",
                    ((PropositionInfo) propositionInteractionConstructorArgs.get(2)).id);
            assertEquals(testId, propositionInteractionConstructorArgs.get(3));
            assertNull(propositionInteractionConstructorArgs.get(4));
            assertEquals("someValue", result.get("someKey"));
        }
    }

    @Test
    public void test_generateInteractionXdm_nullPropositionReference()
            throws MessageRequiredFieldMissingException {
        try (MockedConstruction<PropositionInteraction> propositionInteractionMockedConstruction =
                Mockito.mockConstruction(
                        PropositionInteraction.class,
                        (mock, context) -> {
                            when(mock.getPropositionInteractionXDM())
                                    .thenReturn(
                                            new HashMap<String, Object>() {
                                                {
                                                    put("someKey", "someValue");
                                                }
                                            });
                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            propositionItem.propositionReference = null;

            // test
            Map<String, Object> result =
                    propositionItem.generateInteractionXdm(MessagingEdgeEventType.DISPLAY);

            // verify
            assertNull(result);
            assertEquals(0, propositionInteractionMockedConstruction.constructed().size());
        }
    }

    @Test
    public void test_generateInteractionXdm_validEventTypeInteractionTokens()
            throws MessageRequiredFieldMissingException {
        final Map<PropositionInteraction, List<Object>> constructorArgs = new HashMap<>();
        try (MockedConstruction<PropositionInteraction> propositionInteractionMockedConstruction =
                Mockito.mockConstruction(
                        PropositionInteraction.class,
                        (mock, context) -> {
                            constructorArgs.put(mock, new ArrayList<>(context.arguments()));
                            when(mock.getPropositionInteractionXDM())
                                    .thenReturn(
                                            new HashMap<String, Object>() {
                                                {
                                                    put("someKey", "someValue");
                                                }
                                            });
                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            Proposition propositionMock = Mockito.mock(Proposition.class);
            when(propositionMock.getUniqueId()).thenReturn("propositionId");
            propositionItem.propositionReference = new SoftReference<>(propositionMock);

            // test
            Map<String, Object> result =
                    propositionItem.generateInteractionXdm(
                            "mockInteraction",
                            MessagingEdgeEventType.INTERACT,
                            Arrays.asList("token1", "token2"));

            // verify
            assertEquals(1, propositionInteractionMockedConstruction.constructed().size());
            PropositionInteraction mockedPropositionInteraction =
                    propositionInteractionMockedConstruction.constructed().get(0);
            List<Object> propositionInteractionConstructorArgs =
                    constructorArgs.get(mockedPropositionInteraction);
            assertEquals(5, propositionInteractionConstructorArgs.size());
            assertEquals(
                    MessagingEdgeEventType.INTERACT, propositionInteractionConstructorArgs.get(0));
            assertEquals("mockInteraction", propositionInteractionConstructorArgs.get(1));
            assertTrue(propositionInteractionConstructorArgs.get(2) instanceof PropositionInfo);
            assertEquals(
                    "propositionId",
                    ((PropositionInfo) propositionInteractionConstructorArgs.get(2)).id);
            assertEquals(testId, propositionInteractionConstructorArgs.get(3));
            assertEquals(
                    Arrays.asList("token1", "token2"),
                    propositionInteractionConstructorArgs.get(4));
            assertEquals("someValue", result.get("someKey"));
        }
    }

    @Test
    public void test_generateInteractionXdm_nullPropositionReferenceWithInteractionAndTokens()
            throws MessageRequiredFieldMissingException {
        try (MockedConstruction<PropositionInteraction> propositionInteractionMockedConstruction =
                Mockito.mockConstruction(
                        PropositionInteraction.class,
                        (mock, context) -> {
                            when(mock.getPropositionInteractionXDM())
                                    .thenReturn(
                                            new HashMap<String, Object>() {
                                                {
                                                    put("someKey", "someValue");
                                                }
                                            });
                        })) {
            // setup
            PropositionItem propositionItem =
                    new PropositionItem(testId, SchemaType.HTML_CONTENT, new HashMap<>());
            propositionItem.propositionReference = null;

            // test
            Map<String, Object> result =
                    propositionItem.generateInteractionXdm(
                            "mockInteraction",
                            MessagingEdgeEventType.DISPLAY,
                            Arrays.asList("token1", "token2"));

            // verify
            assertNull(result);
            assertEquals(0, propositionInteractionMockedConstruction.constructed().size());
        }
    }

    // toEventData
    @Test
    public void test_toEventData_ValidItemData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, jsonContentMap);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();

        // verify
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(SchemaType.JSON_CONTENT.toString(), propositionItemMap.get("schema"));
        assertEquals(jsonContentMap, propositionItemMap.get("data"));
    }

    @Test
    public void test_toEventData_EmptyItemData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, Collections.emptyMap());
        Map<String, Object> propositionItemMap = propositionItem.toEventData();

        // verify
        assertTrue(propositionItemMap.isEmpty());
    }

    // fromRuleConsequenceDetail
    @Test
    public void test_createPropositionItem_fromRuleConsequenceDetail_ValidContent() {
        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(eventDataMapForJSON);
        // verify
        assertNotNull(propositionItem);
        assertEquals(
                eventDataMapForJSON.get(MessagingTestConstants.ConsequenceDetailKeys.ID),
                propositionItem.getItemId());
        assertEquals(SchemaType.JSON_CONTENT, propositionItem.getSchema());
        assertEquals(jsonContentMap, propositionItem.getItemData());
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequenceDetail_InvalidIdType() {
        // setup
        eventDataMapForJSON.put("id", new HashMap<>());
        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequenceDetail_InvalidSchemaType() {
        // setup
        eventDataMapForJSON.put("schema", new HashMap<>());
        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequenceDetail_NullData() {
        // setup
        eventDataMapForJSON.put("data", null);

        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequenceDetail_EmptyData() {
        // setup
        eventDataMapForJSON.put("data", new HashMap<>());

        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequenceDetail_InvalidData() {
        // setup
        eventDataMapForJSON.put("data", "invalidData");

        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequenceDetail(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    // fromRuleConsequence
    @Test
    public void test_createPropositionItem_fromRuleConsequence() throws JSONException {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        RuleConsequence ruleConsequence = parseRuleConsequence(rulesJson).get(0);

        // test
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(ruleConsequence);

        // verify
        assertNotNull(propositionItem);
        assertEquals(ruleConsequence.getDetail().get("id"), propositionItem.getItemId());
        assertEquals(SchemaType.INAPP, propositionItem.getSchema());
        assertEquals(ruleConsequence.getDetail().get("data"), propositionItem.getItemData());
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_NullConsequence() {
        // test
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(null);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_EmptyDetails() {
        // test
        RuleConsequence consequence = new RuleConsequence(testId, "cjmiam", Collections.emptyMap());
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(consequence);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_InvalidIdType() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        RuleConsequence ruleConsequence = parseRuleConsequence(rulesJson).get(0);
        ruleConsequence.getDetail().put("id", new HashMap<>());

        // test
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(ruleConsequence);

        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_InvalidSchema()
            throws JSONException {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        RuleConsequence ruleConsequence = parseRuleConsequence(rulesJson).get(0);
        ruleConsequence.getDetail().put("schema", new HashMap<>());

        // test
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(ruleConsequence);

        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_InvalidData() throws JSONException {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        RuleConsequence ruleConsequence = parseRuleConsequence(rulesJson).get(0);
        ruleConsequence.getDetail().put("data", "someInvalidData");

        // test
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(ruleConsequence);

        // verify
        assertNull(propositionItem);
    }

    @Test
    public void fromSchemaConsequenceEvent_returnsNull_whenEventDataIsNull() {
        // setup
        Event event = mock(Event.class);
        when(event.getEventData()).thenReturn(null);

        // test
        PropositionItem result = PropositionItem.fromSchemaConsequenceEvent(event);

        // verify
        assertNull(result);
    }

    @Test
    public void fromSchemaConsequenceEvent_returnsNull_whenConsequenceMapIsNull() {
        // setup
        Event event = mock(Event.class);
        when(event.getEventData()).thenReturn(new HashMap<>());

        // test
        PropositionItem result = PropositionItem.fromSchemaConsequenceEvent(event);

        // verify
        assertNull(result);
    }

    @Test
    public void fromSchemaConsequenceEvent_returnsNull_whenConsequenceTypeIsNotSchema() {
        // setup
        Event event = mock(Event.class);
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        consequence.put(
                MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE,
                "not-schema");
        eventData.put(
                MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);
        when(event.getEventData()).thenReturn(eventData);

        // test
        PropositionItem result = PropositionItem.fromSchemaConsequenceEvent(event);

        // verify
        assertNull(result);
    }

    @Test
    public void fromSchemaConsequenceEvent_returnsNull_whenDetailIsNull() {
        // setup
        Event event = mock(Event.class);
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        consequence.put(
                MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE,
                MessagingConstants.ConsequenceDetailKeys.SCHEMA);
        eventData.put(
                MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);
        when(event.getEventData()).thenReturn(eventData);

        // test
        PropositionItem result = PropositionItem.fromSchemaConsequenceEvent(event);

        // verify
        assertNull(result);
    }

    @Test
    public void fromSchemaConsequenceEvent_returnsPropositionItem_whenAllConditionsAreMet() {
        // setup
        Event event = mock(Event.class);
        Map<String, Object> eventData = new HashMap<>();
        Map<String, Object> consequence = new HashMap<>();
        Map<String, Object> consequenceDetail =
                MessagingTestUtils.createFeedConsequenceList(1).get(0).getDetail();
        consequence.put(
                MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE,
                MessagingConstants.ConsequenceDetailKeys.SCHEMA);
        consequence.put(
                MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL,
                consequenceDetail);
        eventData.put(
                MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, consequence);
        when(event.getEventData()).thenReturn(eventData);

        // test
        PropositionItem propositionItem = PropositionItem.fromSchemaConsequenceEvent(event);

        // verify
        assertNotNull(propositionItem);
        assertEquals(consequenceDetail.get("id"), propositionItem.getItemId());
        assertEquals(SchemaType.CONTENT_CARD, propositionItem.getSchema());
        assertEquals(consequenceDetail.get("data"), propositionItem.getItemData());
    }

    // getInAppSchemaData tests
    @Test
    public void test_getInAppSchemaData() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        List<RuleConsequence> ruleConsequences = parseRuleConsequence(rulesJson);

        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequence(ruleConsequences.get(0));

        // verify
        assertNotNull(propositionItem);
        InAppSchemaData schemaData = propositionItem.getInAppSchemaData();
        assertNotNull(schemaData);
        Map<String, Object> inAppPropositionItemData =
                (Map<String, Object>) ruleConsequences.get(0).getDetail().get("data");
        assertEquals(ContentType.TEXT_HTML, schemaData.getContentType());
        assertEquals("<html>message here</html>", schemaData.getContent());
        assertEquals(
                (int)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.EXPIRY_DATE),
                schemaData.getExpiryDate());
        assertEquals(
                (int)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE),
                schemaData.getPublishedDate());
        assertEquals(
                (Map<String, Object>)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.METADATA),
                schemaData.getMeta());
        assertEquals(
                (Map<String, Object>)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.MOBILE_PARAMETERS),
                schemaData.getMobileParameters());
        assertEquals(
                (Map<String, Object>)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.WEB_PARAMETERS),
                schemaData.getWebParameters());
        assertEquals(
                (List<String>)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.REMOTE_ASSETS),
                schemaData.getRemoteAssets());
    }

    @Test
    public void test_getInAppSchemaData_emptyData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.INAPP, Collections.emptyMap());
        InAppSchemaData schemaData = propositionItem.getInAppSchemaData();
        // verify
        assertNull(schemaData);
    }

    @Test
    public void test_getInAppSchemaData_invalidSchemaType()
            throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, jsonContentMap);

        InAppSchemaData schemaData = propositionItem.getInAppSchemaData();
        // verify
        assertNull(schemaData);
    }

    // getFeedItemSchemaData tests
    @Test
    public void test_getFeedItemSchemaData() throws MessageRequiredFieldMissingException {
        // setup
        String rulesJson =
                MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        List<RuleConsequence> ruleConsequences = parseRuleConsequence(rulesJson);

        // test
        PropositionItem propositionItem =
                PropositionItem.fromRuleConsequence(ruleConsequences.get(0));

        // verify
        assertNotNull(propositionItem);
        ContentCardSchemaData schemaData = propositionItem.getContentCardSchemaData();
        assertNotNull(schemaData);
        Map<String, Object> inAppPropositionItemData =
                (Map<String, Object>) ruleConsequences.get(0).getDetail().get("data");
        assertEquals(ContentType.APPLICATION_JSON, schemaData.getContentType());
        assertEquals(
                (Map<String, Object>)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT),
                schemaData.getContent());
        assertEquals(
                (int)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.EXPIRY_DATE),
                schemaData.getExpiryDate());
        assertEquals(
                (int)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE),
                schemaData.getPublishedDate());
        assertEquals(
                (Map<String, Object>)
                        inAppPropositionItemData.get(
                                MessagingTestConstants.ConsequenceDetailDataKeys.METADATA),
                schemaData.getMeta());
    }

    @Test
    public void test_getFeedSchemaData_emptyData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.FEED, Collections.emptyMap());
        FeedItemSchemaData schemaData = propositionItem.getFeedItemSchemaData();
        // verify
        assertNull(schemaData);
    }

    @Test
    public void test_getFeedItemSchemaData_invalidSchemaType()
            throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, jsonContentMap);

        FeedItemSchemaData schemaData = propositionItem.getFeedItemSchemaData();
        // verify
        assertNull(schemaData);
    }

    // getJsonContentMap tests
    @Test
    public void test_getJsonContentMap() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, jsonContentMap);
        Map<String, Object> jsonContent = propositionItem.getJsonContentMap();

        // verify
        assertNotNull(jsonContent);
        assertEquals(jsonContentMap.get("content"), jsonContent);
    }

    @Test
    public void test_getJsonContentMap_emptyItemData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, Collections.emptyMap());
        Map<String, Object> jsonContentMap = propositionItem.getJsonContentMap();
        // verify
        assertNull(jsonContentMap);
    }

    @Test
    public void test_getJsonContentMap_invalidSchemaType()
            throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.HTML_CONTENT, htmlContentMap);
        Map<String, Object> jsonContentMap = propositionItem.getJsonContentMap();
        // verify
        assertNull(jsonContentMap);
    }

    // getJsonArrayContent tests
    @Test
    public void test_getJsonArrayContent() throws MessageRequiredFieldMissingException {
        // setup
        Map<String, Object> jsonContentArrayList =
                MessagingTestUtils.getMapFromFile("codeBasedPropositionJsonContentArray.json");

        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, jsonContentArrayList);
        List<Map<String, Object>> resultContent = propositionItem.getJsonContentArrayList();

        // verify
        assertEquals(jsonContentArrayList.get("content"), resultContent);
    }

    @Test
    public void test_getJsonArrayContent_emptyData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, Collections.emptyMap());
        // test
        List<Map<String, Object>> jsonArrayList = propositionItem.getJsonContentArrayList();
        // verify
        assertNull(jsonArrayList);
    }

    @Test
    public void test_getJsonArrayContent_invalidSchemaType()
            throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.HTML_CONTENT, htmlContentMap);
        List<Map<String, Object>> jsonArrayList = propositionItem.getJsonContentArrayList();
        // verify
        assertNull(jsonArrayList);
    }

    // getHtmlContent tests
    @Test
    public void test_getHtmlContent() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.HTML_CONTENT, htmlContentMap);
        String htmlContent = propositionItem.getHtmlContent();

        // verify
        assertEquals(
                htmlContentMap.get(MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT),
                htmlContent);
    }

    @Test
    public void test_getHtmlContent_emptyItemData() throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.HTML_CONTENT, Collections.emptyMap());
        String htmlContent = propositionItem.getHtmlContent();
        // verify
        assertNull(htmlContent);
    }

    @Test
    public void test_getHtmlContent_invalidSchemaType()
            throws MessageRequiredFieldMissingException {
        // test
        PropositionItem propositionItem =
                new PropositionItem(testId, SchemaType.JSON_CONTENT, htmlContentMap);
        String htmlContent = propositionItem.getHtmlContent();

        // verify
        assertNull(htmlContent);
    }

    private List<RuleConsequence> parseRuleConsequence(String rulesJson) {
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        return rules.get(0).getConsequenceList();
    }
}
