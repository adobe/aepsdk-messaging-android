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

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionItemTests {

    String testJSONStringContent = "{\"version\":1,\"rules\":[{\"consequences\":[{\"type\":\"schema\",\"id\":\"uniqueId\",\"detail\":{\"id\":\"uniqueDetailId\",\"schema\":\"https://ns.adobe.com/personalization/message/feed-item\", \"expiryDate\":1717688797,\"publishedDate\":1717688797,\"type\":\"feed\",\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp\",\"feedName\":\"apifeed\",\"campaignName\":\"mockCampaign\"},\"content\":{\"actionUrl\":\"https://adobe.com/\",\"actionTitle\":\"test action title\",\"title\":\"test title\",\"body\":\"test body\",\"imageUrl\":\"https://adobe.com/image.png\"}}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}]}";
    String testContent = "some html string content";
    String testJSONSchema = "https://ns.adobe.com/personalization/json-content-item";
    String testHTMLSchema = "https://ns.adobe.com/personalization/html-content-item";
    String testId = "uniqueId";
    String expectedInboundContent = "{\"actionTitle\":\"test action title\",\"imageUrl\":\"https://adobe.com/image.png\",\"actionUrl\":\"https://adobe.com/\",\"title\":\"test title\",\"body\":\"test body\"}";
    String expectedContentType = "application/json";
    Map<String, Object> expectedInboundMetaMap = new HashMap<String, Object>() {{
        put("surface", "mobileapp://mockApp");
        put("feedName", "apifeed");
        put("campaignName", "mockCampaign");
    }};
    JSONObject ruleJSON;
    Map<String, Object> eventDataMapForJSON = new HashMap<>();
    Map<String, Object> eventDataMapForHTML = new HashMap<>();

    @Before
    public void setup() throws JSONException {
        ruleJSON = new JSONObject(testJSONStringContent);
        // setup event data for json content
        final Map<String, Object> jsonDataMap = new HashMap<>();
        jsonDataMap.put("content", ruleJSON.toString());
        eventDataMapForJSON.put("id", testId);
        eventDataMapForJSON.put("schema", testJSONSchema);
        eventDataMapForJSON.put("data", jsonDataMap);

        // setup event data for html content
        final Map<String, Object> htmlDataMap = new HashMap<>();
        htmlDataMap.put("content", testContent);
        eventDataMapForHTML.put("id", testId);
        eventDataMapForHTML.put("schema", testHTMLSchema);
        eventDataMapForHTML.put("data", htmlDataMap);
    }

    // json content tests
    @Test
    public void test_propositionItemConstructor_JSONContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testJSONSchema, testJSONStringContent);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getUniqueId());
        assertEquals(testJSONSchema, propositionItem.getSchema());
        assertEquals(testJSONStringContent, propositionItem.getContent());
    }

    @Test
    public void test_createPropositionItem_fromEventData_JSONContent() throws JSONException {
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getUniqueId());
        assertEquals(testJSONSchema, propositionItem.getSchema());
        JSONObject expectedJSON = new JSONObject(testJSONStringContent);
        JSONObject propositionItemJSON = new JSONObject(propositionItem.getContent());
        assertEquals(expectedJSON.toString().trim(), propositionItemJSON.toString().trim());
    }

    @Test
    public void test_createEventData_fromPropositionItem_JSONContent() throws DataReaderException {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testJSONSchema, testJSONStringContent);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        Map<String, Object> data = DataReader.getTypedMap(Object.class, propositionItemMap, "data");
        String content = DataReader.getString(data, "content");
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(testJSONSchema, propositionItemMap.get("schema"));
        assertEquals("{\"version\":1,\"rules\":[{\"consequences\":[{\"type\":\"schema\",\"id\":\"uniqueId\",\"detail\":{\"id\":\"uniqueDetailId\",\"schema\":\"https://ns.adobe.com/personalization/message/feed-item\", \"expiryDate\":1717688797,\"publishedDate\":1717688797,\"type\":\"feed\",\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp\",\"feedName\":\"apifeed\",\"campaignName\":\"mockCampaign\"},\"content\":{\"actionUrl\":\"https://adobe.com/\",\"actionTitle\":\"test action title\",\"title\":\"test title\",\"body\":\"test body\",\"imageUrl\":\"https://adobe.com/image.png\"}}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}]}", content);
    }

    @Test
    public void test_createInbound_fromPropositionItem_JSONContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testJSONSchema, testJSONStringContent);
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

    // string content tests
    @Test
    public void test_propositionItemConstructor_StringContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testHTMLSchema, testContent);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getUniqueId());
        assertEquals(testHTMLSchema, propositionItem.getSchema());
        assertEquals(testContent, propositionItem.getContent());
    }

    @Test
    public void test_createPropositionItem_fromEventData_StringContent() {
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForHTML);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getUniqueId());
        assertEquals(testHTMLSchema, propositionItem.getSchema());
        assertEquals(testContent, propositionItem.getContent());
    }

    @Test
    public void test_createEventData_fromPropositionItem_StringContent() throws DataReaderException {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testHTMLSchema, testContent);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(testHTMLSchema, propositionItemMap.get("schema"));
        Map<String, Object> data = DataReader.getTypedMap(Object.class, propositionItemMap, "data");
        String content = DataReader.getString(data, "content");
        assertEquals("some html string content", content);
    }

    // negative tests

    @Test
    public void test_createInbound_fromPropositionItem_whenJSONContentIsEmpty() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testJSONSchema, "");
        Inbound inbound = propositionItem.decodeContent();
        // verify
        assertNull(inbound);
    }

    @Test
    public void test_createInbound_fromPropositionItem_whenStringContentIsEmpty() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testHTMLSchema, "");
        Inbound inbound = propositionItem.decodeContent();
        // verify
        assertNull(inbound);
    }

    @Test
    public void test_createPropositionItem_fromEventData_NullJSONContent() {
        // setup
        final Map<String, Object> jsonDataMap = new HashMap<>();
        jsonDataMap.put("content", null);
        eventDataMapForJSON.put("data", jsonDataMap);
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromEventData_NullStringContent() {
        // setup
        final Map<String, Object> htmlDataMap = new HashMap<>();
        htmlDataMap.put("content", null);
        eventDataMapForHTML.put("data", htmlDataMap);
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForHTML);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createEventData_fromPropositionItem_NullContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, testJSONSchema, null);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        assertTrue(propositionItemMap.isEmpty());
    }
}
