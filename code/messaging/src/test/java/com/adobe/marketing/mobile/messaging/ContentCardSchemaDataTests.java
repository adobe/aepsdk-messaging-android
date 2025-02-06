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

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.CONTENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.EXPIRY_DATE;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.METADATA;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.ConsequenceDetailDataKeys.PUBLISHED_DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContentCardSchemaDataTests {

    @Test
    public void constructor_setsAllFieldsCorrectly_whenContentTypeIsApplicationJson()
            throws JSONException {
        // test
        ContentCardSchemaData contentCardSchemaData =
                new ContentCardSchemaData(createValidContentCardSchemaObject("json"));

        // verify
        assertEquals(ContentType.APPLICATION_JSON, contentCardSchemaData.getContentType());
        assertTrue(contentCardSchemaData.getContent() instanceof Map);
        assertEquals("value", ((Map) contentCardSchemaData.getContent()).get("key"));
        assertEquals(123456789, contentCardSchemaData.getPublishedDate());
        assertEquals(987654321, contentCardSchemaData.getExpiryDate());
        assertEquals("metaValue", contentCardSchemaData.getMeta().get("metaKey"));
    }

    @Test
    public void constructor_setsAllFieldsCorrectly_whenContentTypeIsString() throws JSONException {
        // test
        ContentCardSchemaData contentCardSchemaData =
                new ContentCardSchemaData(createValidContentCardSchemaObject("string"));

        // verify
        assertEquals(ContentType.TEXT_PLAIN, contentCardSchemaData.getContentType());
        assertTrue(contentCardSchemaData.getContent() instanceof String);
        assertEquals("content", contentCardSchemaData.getContent());
        assertEquals(123456789, contentCardSchemaData.getPublishedDate());
        assertEquals(987654321, contentCardSchemaData.getExpiryDate());
        assertEquals("metaValue", contentCardSchemaData.getMeta().get("metaKey"));
    }

    @Test
    public void constructor_handlesJSONException_whenBadJsonObjectInput() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
            schemaData.put(CONTENT, "invalidJson");

            // test
            ContentCardSchemaData contentCardSchemaData = new ContentCardSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, contentCardSchemaData.getContentType());
            assertNull(contentCardSchemaData.getContent());
            assertEquals(0, contentCardSchemaData.getPublishedDate());
            assertEquals(0, contentCardSchemaData.getExpiryDate());
            assertNull(contentCardSchemaData.getMeta());
        }
    }

    @Test
    public void constructor_handlesJSONException_whenContentIsMissing() throws JSONException {
        try (MockedStatic<Log> logMockedStatic = Mockito.mockStatic(Log.class)) {
            // setup
            JSONObject schemaData = new JSONObject();
            schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);

            // test
            ContentCardSchemaData contentCardSchemaData = new ContentCardSchemaData(schemaData);

            // verify
            logMockedStatic.verify(
                    () ->
                            Log.trace(
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString(),
                                    ArgumentMatchers.anyString()));
            assertEquals(ContentType.APPLICATION_JSON, contentCardSchemaData.getContentType());
            assertNull(contentCardSchemaData.getContent());
            assertEquals(0, contentCardSchemaData.getPublishedDate());
            assertEquals(0, contentCardSchemaData.getExpiryDate());
            assertNull(contentCardSchemaData.getMeta());
        }
    }

    @Test
    public void constructor_setsAllFieldsCorrectly_whenPublishedDateExpiryDateMetaAreMissing()
            throws JSONException {
        // setup
        JSONObject schemaData = new JSONObject();
        schemaData.put(CONTENT_TYPE, MessagingTestConstants.ContentTypes.APPLICATION_JSON);
        schemaData.put(CONTENT, new JSONObject().put("key", "value"));

        // test
        ContentCardSchemaData contentCardSchemaData = new ContentCardSchemaData(schemaData);

        // verify
        assertEquals(ContentType.APPLICATION_JSON, contentCardSchemaData.getContentType());
        assertTrue(contentCardSchemaData.getContent() instanceof Map);
        assertEquals("value", ((Map) contentCardSchemaData.getContent()).get("key"));
        assertEquals(0, contentCardSchemaData.getPublishedDate());
        assertEquals(0, contentCardSchemaData.getExpiryDate());
        assertNull(contentCardSchemaData.getMeta());
    }

    @Test
    public void test_track_interactionEvent()
            throws MessageRequiredFieldMissingException, JSONException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };

        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<PropositionHistory> propositionHistoryMockedStatic =
                        Mockito.mockStatic(PropositionHistory.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {

            // setup
            Proposition mockProposition = Mockito.mock(Proposition.class);
            when(mockProposition.getActivityId()).thenReturn("testPropositionId");
            PropositionItem propositionItem =
                    new PropositionItem(
                            "testPropositionItemId", SchemaType.JSON_CONTENT, new HashMap<>());
            propositionItem.propositionReference = new SoftReference<>(mockProposition);

            ContentCardSchemaData contentCardSchemaData =
                    new ContentCardSchemaData(createValidContentCardSchemaObject("json"));
            PropositionItem spyPropositionItem = Mockito.spy(propositionItem);
            contentCardSchemaData.parent = spyPropositionItem;

            // test
            contentCardSchemaData.track("mockInteraction", MessagingEdgeEventType.INTERACT);

            // verify tracking event
            verify(spyPropositionItem)
                    .generateInteractionXdm(
                            "mockInteraction", MessagingEdgeEventType.INTERACT, null);
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

            // verify event history record interact event
            ArgumentCaptor<String> propositionIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor =
                    ArgumentCaptor.forClass(MessagingEdgeEventType.class);
            ArgumentCaptor<String> interactionTokenCaptor = ArgumentCaptor.forClass(String.class);

            // expect one record call
            propositionHistoryMockedStatic.verify(
                    () ->
                            PropositionHistory.record(
                                    propositionIdCaptor.capture(),
                                    messagingEdgeEventTypeArgumentCaptor.capture(),
                                    interactionTokenCaptor.capture()),
                    times(1));
            assertEquals("testPropositionId", propositionIdCaptor.getValue());
            assertEquals(
                    MessagingEdgeEventType.INTERACT,
                    messagingEdgeEventTypeArgumentCaptor.getValue());
            assertEquals("mockInteraction", interactionTokenCaptor.getValue());
        }
    }

    @Test
    public void test_track_dismissEvent()
            throws MessageRequiredFieldMissingException, JSONException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };

        try (MockedStatic<MobileCore> mobileCoreMockedStatic =
                        Mockito.mockStatic(MobileCore.class);
                MockedStatic<PropositionHistory> propositionHistoryMockedStatic =
                        Mockito.mockStatic(PropositionHistory.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {

            // setup
            Proposition mockProposition = Mockito.mock(Proposition.class);
            when(mockProposition.getActivityId()).thenReturn("testPropositionId");
            PropositionItem propositionItem =
                    new PropositionItem(
                            "testPropositionItemId", SchemaType.JSON_CONTENT, new HashMap<>());
            propositionItem.propositionReference = new SoftReference<>(mockProposition);

            ContentCardSchemaData contentCardSchemaData =
                    new ContentCardSchemaData(createValidContentCardSchemaObject("json"));
            PropositionItem spyPropositionItem = Mockito.spy(propositionItem);
            contentCardSchemaData.parent = spyPropositionItem;

            // test
            contentCardSchemaData.track(null, MessagingEdgeEventType.DISMISS);

            // verify tracking event
            verify(spyPropositionItem)
                    .generateInteractionXdm(null, MessagingEdgeEventType.DISMISS, null);
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

            // verify event history record events
            ArgumentCaptor<String> propositionIdCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<MessagingEdgeEventType> messagingEdgeEventTypeArgumentCaptor =
                    ArgumentCaptor.forClass(MessagingEdgeEventType.class);
            ArgumentCaptor<String> interactionTokenCaptor = ArgumentCaptor.forClass(String.class);

            // expect two record calls
            propositionHistoryMockedStatic.verify(
                    () ->
                            PropositionHistory.record(
                                    propositionIdCaptor.capture(),
                                    messagingEdgeEventTypeArgumentCaptor.capture(),
                                    interactionTokenCaptor.capture()),
                    times(2));
            // verify event history record dismiss event
            assertEquals("testPropositionId", propositionIdCaptor.getAllValues().get(0));
            assertEquals(
                    MessagingEdgeEventType.DISMISS,
                    messagingEdgeEventTypeArgumentCaptor.getAllValues().get(0));
            assertEquals(null, interactionTokenCaptor.getAllValues().get(0));
            // verify event history record disqualify event
            assertEquals("testPropositionId", propositionIdCaptor.getAllValues().get(1));
            assertEquals(
                    MessagingEdgeEventType.DISQUALIFY,
                    messagingEdgeEventTypeArgumentCaptor.getAllValues().get(1));
            assertEquals(null, interactionTokenCaptor.getAllValues().get(1));
        }
    }

    @Test
    public void test_track_nullParent() throws JSONException {
        Map<String, Object> mockInteractionData =
                new HashMap<String, Object>() {
                    {
                        put("someKey", "someValue");
                    }
                };

        try (MockedStatic<PropositionHistory> propositionHistoryMockedStatic =
                        Mockito.mockStatic(PropositionHistory.class);
                MockedConstruction<PropositionInteraction>
                        propositionInteractionMockedConstruction =
                                Mockito.mockConstruction(
                                        PropositionInteraction.class,
                                        (mock, context) -> {
                                            when(mock.getPropositionInteractionXDM())
                                                    .thenReturn(mockInteractionData);
                                        })) {

            // setup
            ContentCardSchemaData contentCardSchemaData =
                    new ContentCardSchemaData(createValidContentCardSchemaObject("json"));
            contentCardSchemaData.parent = null;

            // test
            contentCardSchemaData.track("mockInteraction", MessagingEdgeEventType.INTERACT);

            // verify no proposition interaction constructed
            assertEquals(0, propositionInteractionMockedConstruction.constructed().size());

            // verify no event history interact event
            propositionHistoryMockedStatic.verify(
                    () ->
                            PropositionHistory.record(
                                    anyString(), any(MessagingEdgeEventType.class), anyString()),
                    times(0));
        }
    }

    private JSONObject createValidContentCardSchemaObject(final String type) throws JSONException {
        return new JSONObject()
                .put(
                        CONTENT_TYPE,
                        type.equals("json")
                                ? MessagingTestConstants.ContentTypes.APPLICATION_JSON
                                : MessagingTestConstants.ContentTypes.TEXT_PLAIN)
                .put(
                        CONTENT,
                        type.equals("json") ? new JSONObject().put("key", "value") : "content")
                .put(PUBLISHED_DATE, 123456789)
                .put(EXPIRY_DATE, 987654321)
                .put(METADATA, new JSONObject().put("metaKey", "metaValue"));
    }
}
