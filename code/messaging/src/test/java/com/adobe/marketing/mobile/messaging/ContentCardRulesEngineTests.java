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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventHistoryRequest;
import com.adobe.marketing.mobile.EventHistoryResult;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.matchers.Any;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentCardRulesEngineTests {

    @Mock private ExtensionApi mockExtensionApi;

    private ContentCardRulesEngine contentCardRulesEngine;

    private Event defaultEvent =
            new Event.Builder("event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
                    .setEventData(
                            new HashMap<String, Object>() {
                                {
                                    put("action", "fullscreen");
                                }
                            })
                    .build();

    @Before
    public void setup() {
        contentCardRulesEngine = new ContentCardRulesEngine("mockRulesEngine", mockExtensionApi);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_evaluate_WithNullEvent() {
        // test
        contentCardRulesEngine.evaluate(null);
    }

    @Test
    public void test_evaluate_WithNoConsequencesRules() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("ruleWithNoConsequence.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify
        Assert.assertNull(propositionItemsBySurface);
    }

    @Test
    public void test_evaluate_WithInAppV2Consequence() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify
        Assert.assertNotNull(propositionItemsBySurface);
        Assert.assertTrue(propositionItemsBySurface.isEmpty());
    }

    @Test
    public void test_evaluate_WithContentCardConsequence_ForFirstTimeQualifyingEvent() {
        // setup
        // mock the getHistoricalEvents call to return 0 events
        doAnswer(invocation -> {
            AdobeCallbackWithError<EventHistoryResult[]> callback = invocation.getArgument(2);
            callback.call(new EventHistoryResult[]{new EventHistoryResult(0, null, null)});
            return null;
        }).when(mockExtensionApi).getHistoricalEvents(any(EventHistoryRequest[].class), anyBoolean(), any(AdobeCallbackWithError.class));

        String rulesJson = MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);
        Event qualifyingEvent =
                new Event.Builder(
                                "qualifyingEvent",
                                EventType.PLACES,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build();

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(qualifyingEvent);

        // verify that the content card consequence is returned
        Assert.assertNotNull(propositionItemsBySurface);
        assertEquals(1, propositionItemsBySurface.size());
        List<PropositionItem> inboundMessageList =
                propositionItemsBySurface.get(Surface.fromUriString("mobileapp://mockPackageName"));
        Assert.assertNotNull(inboundMessageList);
        assertEquals(1, inboundMessageList.size());
        assertEquals(SchemaType.CONTENT_CARD, inboundMessageList.get(0).getSchema());

        // verify the qualify content card consequence is written to event history
        ArgumentCaptor<Event> eventHistoryRecordCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi, times(1))
                .recordHistoricalEvent(eventHistoryRecordCaptor.capture(), any(AdobeCallbackWithError.class));
        assertEquals("qualify", eventHistoryRecordCaptor.getValue().getEventData().get("iam.eventType"));
    }

    @Test
    public void test_evaluate_WithContentCardConsequence_ForAlreadyQualifiedCard() {
        // setup
        // mock the getHistoricalEvents call
        doAnswer(invocation -> {
            EventHistoryRequest[] requestsArray = invocation.getArgument(0);
            EventHistoryResult[] resultsArray = new EventHistoryResult[requestsArray.length];
            AdobeCallbackWithError<EventHistoryResult[]> callback = invocation.getArgument(2);
            for (int i = 0; i < requestsArray.length; i++) {
                // hash for disqualify event is 2655746408L
                // hash for unqualify event is 2655746409L
                if (requestsArray[i].getMaskAsDecimalHash() == 2655746408L ||
                        requestsArray[i].getMaskAsDecimalHash() == 2479650165L) {
                    resultsArray[i] = new EventHistoryResult(0, null, null);
                } else {
                    // return found for qualify and trigger event
                    resultsArray[i] = new EventHistoryResult(1, 123L, 456L);
                }
            }
            callback.call(resultsArray);
            return null;
        }).when(mockExtensionApi).getHistoricalEvents(any(EventHistoryRequest[].class), anyBoolean(), any(AdobeCallbackWithError.class));

        String rulesJson = MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify that the content card consequence is returned
        Assert.assertNotNull(propositionItemsBySurface);
        assertEquals(1, propositionItemsBySurface.size());
        List<PropositionItem> inboundMessageList =
                propositionItemsBySurface.get(Surface.fromUriString("mobileapp://mockPackageName"));
        Assert.assertNotNull(inboundMessageList);
        assertEquals(1, inboundMessageList.size());
        assertEquals(SchemaType.CONTENT_CARD, inboundMessageList.get(0).getSchema());

        // verify the qualify content card consequence is not written to event history
        verify(mockExtensionApi, times(0))
                .recordHistoricalEvent(any(), any());
    }

    @Test
    public void test_evaluate_WithContentCardConsequence_ForFirstTimeUnqualifyingEvent() {
        // setup
        // mock the getHistoricalEvents call to return 0 events
        doAnswer(invocation -> {
            AdobeCallbackWithError<EventHistoryResult[]> callback = invocation.getArgument(2);
            callback.call(new EventHistoryResult[]{new EventHistoryResult(0, null, null)});
            return null;
        }).when(mockExtensionApi).getHistoricalEvents(any(EventHistoryRequest[].class), anyBoolean(), any(AdobeCallbackWithError.class));

        String rulesJson = MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);
        Event qualifyingEvent =
                new Event.Builder(
                        "qualifyingEvent",
                        EventType.PLACES,
                        EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "exited");
                                    }
                                })
                        .build();

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(qualifyingEvent);

        // verify that the content card consequence is not returned
        assertTrue(propositionItemsBySurface.isEmpty());

        // verify the unqualify content card consequence is written to event history
        ArgumentCaptor<Event> eventHistoryRecordCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi, times(1))
                .recordHistoricalEvent(eventHistoryRecordCaptor.capture(), any(AdobeCallbackWithError.class));
        assertEquals("unqualify", eventHistoryRecordCaptor.getValue().getEventData().get("iam.eventType"));
    }

    @Test
    public void test_evaluate_WithContentCardConsequence_ForAlreadyUnqualifiedCard() {
        // setup
        // mock the getHistoricalEvents call
        doAnswer(invocation -> {
            EventHistoryRequest[] requestsArray = invocation.getArgument(0);
            EventHistoryResult[] resultsArray = new EventHistoryResult[requestsArray.length];
            AdobeCallbackWithError<EventHistoryResult[]> callback = invocation.getArgument(2);
            for (int i = 0; i < requestsArray.length; i++) {
                // hash for unqualify event is 2479650165L
                // return found for unqualify event
                if (requestsArray[i].getMaskAsDecimalHash() == 2479650165L) {
                    resultsArray[i] = new EventHistoryResult(1, 123L, 456L);
                } else {
                    resultsArray[i] = new EventHistoryResult(0, null, null);
                }
            }
            callback.call(resultsArray);
            return null;
        }).when(mockExtensionApi).getHistoricalEvents(any(EventHistoryRequest[].class), anyBoolean(), any(AdobeCallbackWithError.class));

        String rulesJson = MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify that the content card consequence is not returned
        assertNull(propositionItemsBySurface);

        // verify the unqualify content card consequence is not written to event history
        verify(mockExtensionApi, times(0))
                .recordHistoricalEvent(any(), any());
    }

    @Test
    public void test_evaluate_WithContentCardConsequence_ForFirstTimeDisqualifyingEvent() {
        // setup
        // mock the getHistoricalEvents call to return 0 events
        doAnswer(invocation -> {
            AdobeCallbackWithError<EventHistoryResult[]> callback = invocation.getArgument(2);
            callback.call(new EventHistoryResult[]{new EventHistoryResult(0, null, null)});
            return null;
        }).when(mockExtensionApi).getHistoricalEvents(any(EventHistoryRequest[].class), anyBoolean(), any(AdobeCallbackWithError.class));

        String rulesJson = MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);
        Event disqualifyingEvent =
                new Event.Builder(
                        "card dismiss event",
                        EventType.EDGE,
                        EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("xdm", new HashMap<String, Object>() {
                                            {
                                                put("eventType", "decisioning.propositionDismiss");
                                                put("_experience", new HashMap<String, Object>() {
                                                    {
                                                        put("decisioning", new HashMap<String, Object>() {{
                                                            put("propositions", new ArrayList<Object>() {
                                                                {
                                                                    add(new HashMap<String, Object>() {{
                                                                        put("scopeDetails", new HashMap<String, Object>() {{
                                                                            put("activity", new HashMap<String, Object>() {{
                                                                                put("id", "a43122c4-bf19-499f-b507-087a028d1769#fa035681-15ce-488e-859e-200bb2ca90ac");
                                                                            }});
                                                                        }});
                                                                    }});
                                                                }
                                                            });
                                                        }});
                                                    }
                                                });
                                            }
                                        });
                                    }
                                })
                        .build();

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(disqualifyingEvent);

        // verify that the content card consequence is not returned
        assertTrue(propositionItemsBySurface.isEmpty());

        // verify the unqualify content card consequence is written to event history
        ArgumentCaptor<Event> eventHistoryRecordCaptor = ArgumentCaptor.forClass(Event.class);
        verify(mockExtensionApi, times(1))
                .recordHistoricalEvent(eventHistoryRecordCaptor.capture(), any(AdobeCallbackWithError.class));
        assertEquals("disqualify", eventHistoryRecordCaptor.getValue().getEventData().get("iam.eventType"));
    }

    @Test
    public void test_evaluate_WithContentCardConsequence_ForAlreadyDisqualifiedCard() {
        // setup
        // mock the getHistoricalEvents call
        doAnswer(invocation -> {
            EventHistoryRequest[] requestsArray = invocation.getArgument(0);
            EventHistoryResult[] resultsArray = new EventHistoryResult[requestsArray.length];
            AdobeCallbackWithError<EventHistoryResult[]> callback = invocation.getArgument(2);
            for (int i = 0; i < requestsArray.length; i++) {
                // return found for all events
                resultsArray[i] = new EventHistoryResult(1, 123L, 456L);
            }
            callback.call(resultsArray);
            return null;
        }).when(mockExtensionApi).getHistoricalEvents(any(EventHistoryRequest[].class), anyBoolean(), any(AdobeCallbackWithError.class));

        String rulesJson = MessagingTestUtils.loadStringFromFile("contentCardPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify that the content card consequence is not returned
        assertNull(propositionItemsBySurface);

        // verify the unqualify content card consequence is not written to event history
        verify(mockExtensionApi, times(0))
                .recordHistoricalEvent(any(), any());
    }

    @Test
    public void test_evaluate_WithMultipleFeedItemConsequences() {
        // setup
        String rulesJson =
                MessagingTestUtils.loadStringFromFile(
                        "feedPropositionContentFeedItemConsequences.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify
        Assert.assertNotNull(propositionItemsBySurface);
        assertEquals(1, propositionItemsBySurface.size());
        List<PropositionItem> inboundMessageList =
                propositionItemsBySurface.get(
                        Surface.fromUriString("mobileapp://com.feeds.testing/feeds/apifeed"));
        Assert.assertNotNull(inboundMessageList);
        assertEquals(2, inboundMessageList.size());
    }

    @Test
    public void test_evaluate_WithMissingDataInConsequencesDetail() {
        // setup
        String rulesJson =
                MessagingTestUtils.loadStringFromFile("feedPropositionContentMissingData.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify
        Assert.assertNull(propositionItemsBySurface);
    }

    @Test
    public void test_evaluate_WithMissingSurfaceInConsequencesDetailMetadata() {
        // setup
        String rulesJson =
                MessagingTestUtils.loadStringFromFile(
                        "feedPropositionContentMissingSurfaceMetadata.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        contentCardRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(defaultEvent);

        // verify
        Assert.assertNull(propositionItemsBySurface);
    }
}
