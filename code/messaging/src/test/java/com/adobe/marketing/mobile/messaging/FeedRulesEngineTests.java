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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FeedRulesEngineTests {

    @Mock
    private ExtensionApi mockExtensionApi;

    private FeedRulesEngine feedRulesEngine;

    private Event defaultEvent = new Event.Builder("event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(new HashMap<String, Object>() {{
                put("action", "fullscreen");
            }}).build();

    @Before
    public void setup() {
        feedRulesEngine = new FeedRulesEngine("mockRulesEngine", mockExtensionApi);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_evaluate_WithNullEvent() {
        // test
        feedRulesEngine.evaluate(null);
    }

    @Test
    public void test_evaluate_WithNoConsequencesRules() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("ruleWithNoConsequence.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        feedRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface = feedRulesEngine.evaluate(defaultEvent);


        // verify
        Assert.assertNull(propositionItemsBySurface);
    }

    @Test
    public void test_evaluate_WithInAppV2Consequence() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        feedRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface = feedRulesEngine.evaluate(defaultEvent);


        // verify
        Assert.assertNotNull(propositionItemsBySurface);
        Assert.assertTrue(propositionItemsBySurface.isEmpty());
    }

    @Test
    public void test_evaluate_WithFeedConsequence() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("feedPropositionContent.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        feedRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface = feedRulesEngine.evaluate(defaultEvent);

        // verify
        Assert.assertNotNull(propositionItemsBySurface);
        Assert.assertEquals(1, propositionItemsBySurface.size());
        List<PropositionItem> inboundMessageList = propositionItemsBySurface.get(Surface.fromUriString("mobileapp://com.feeds.testing/feeds/apifeed"));
        Assert.assertNotNull(inboundMessageList);
        Assert.assertEquals(1, inboundMessageList.size());
        Assert.assertEquals(SchemaType.FEED, inboundMessageList.get(0).getSchema());
    }

    @Test
    public void test_evaluate_WithMultipleFeedItemConsequences() {
        // setup
        String rulesJson = MessagingTestUtils.loadStringFromFile("feedPropositionContentFeedItemConsequences.json");
        Assert.assertNotNull(rulesJson);
        List<LaunchRule> rules = JSONRulesParser.parse(rulesJson, mockExtensionApi);
        feedRulesEngine.replaceRules(rules);

        // test
        Map<Surface, List<PropositionItem>> propositionItemsBySurface = feedRulesEngine.evaluate(defaultEvent);


        // verify
        Assert.assertNotNull(propositionItemsBySurface);
        Assert.assertEquals(1, propositionItemsBySurface.size());
        List<PropositionItem> inboundMessageList = propositionItemsBySurface.get(Surface.fromUriString("mobileapp://com.feeds.testing/feeds/apifeed"));
        Assert.assertNotNull(inboundMessageList);
        Assert.assertEquals(2, inboundMessageList.size());
    }
}
