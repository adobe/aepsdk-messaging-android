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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionItemTests {

    String testContent = "{\"consequences\":[{\"type\":\"ajoInbound\",\"id\":\"uniqueId\",\"detail\":{\"expiryDate\":1717688797,\"publishedDate\":1717688797,\"type\":\"feed\",\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp\",\"feedName\":\"apifeed\",\"campaignName\":\"mockCampaign\"},\"content\":{\"actionUrl\":\"https://adobe.com/\",\"actionTitle\":\"test action title\",\"title\":\"test title\",\"body\":\"test body\",\"imageUrl\":\"https://adobe.com/image.png\"}}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}";
    String testSchema = "https://ns.adobe.com/personalization/json-content-item";
    String testId = "uniqueId";
    String expectedInboundContent = "{\"actionTitle\":\"test action title\",\"imageUrl\":\"https://adobe.com/image.png\",\"actionUrl\":\"https://adobe.com/\",\"title\":\"test title\",\"body\":\"test body\"}";
    String expectedContentType = "application/json";
    Map<String, Object> expectedInboundMetaMap = new HashMap<String, Object>() {{
        put("surface", "mobileapp://mockApp");
        put("feedName", "apifeed");
        put("campaignName", "mockCampaign");
    }};
    JSONObject ruleJSON;
    Map<String, Object> eventDataMap = new HashMap<>();

    @Before
    public void setup() throws JSONException {
        ruleJSON = new JSONObject(testContent);
        final Map<String, Object> dataMap = new HashMap<>();
        final Map<String, Object> contentMap = new HashMap<>();
        final List<Map<String, Object>> ruleMapList = new ArrayList<>();
        ruleMapList.add(JSONUtils.toMap(ruleJSON));
        contentMap.put("rules", ruleMapList);
        contentMap.put("version", 1);
        dataMap.put("content", contentMap);
        eventDataMap.put("id", testId);
        eventDataMap.put("schema", testSchema);
        eventDataMap.put("data", dataMap);
    }

    @Test
    public void test_propositionItemConstructor() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testSchema, testContent);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getUniqueId());
        assertEquals(testSchema, propositionItem.getSchema());
        assertEquals(testContent, propositionItem.getContent());
    }

    @Test
    public void test_createPropositionItem_fromEventData() {
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMap);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getUniqueId());
        assertEquals(testSchema, propositionItem.getSchema());
        assertEquals(testContent, propositionItem.getContent());
    }

    @Test
    public void test_createEventData_fromPropositionItem() throws DataReaderException {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testSchema, testContent);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        Map<String, Object> contentMap = DataReader.getTypedMap(Object.class, propositionItemMap, "content");
        JSONObject ruleJSON = new JSONObject(DataReader.getTypedListOfMap(Object.class, contentMap, "rules").get(0));
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(testSchema, propositionItemMap.get("schema"));
        assertEquals(testContent, ruleJSON.toString());
    }

    @Test
    public void test_createInbound_fromPropositionItem() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testSchema, testContent);
        Inbound inbound = propositionItem.decodeContent();
        // verify
        assertNotNull(inbound);
        assertEquals(testId, inbound.getUniqueId());
        assertEquals(InboundType.FEED, inbound.getInboundType());
        assertEquals(expectedInboundContent, inbound.getContent());
        assertEquals(expectedContentType, inbound.getContentType());
        assertEquals(expectedInboundMetaMap, inbound.getMeta());
        assertEquals(1717688797, inbound.getExpiryDate());
        assertEquals(1717688797, inbound.getPublishedDate());
    }

    @Test
    public void test_createInbound_fromPropositionItem_whenContentIsEmpty() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testSchema, "");
        Inbound inbound = propositionItem.decodeContent();
        // verify
        assertNull(inbound);
    }
}
