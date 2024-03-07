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

import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionItemTests {
    String testStringContent = "some html string content";
    String testId = "uniqueId";
    JSONObject inAppRuleJSON;
    Map<String, Object> codeBasedPropositionItemData = new HashMap<>();
    Map<String, Object> feedPropositionItemData = new HashMap<>();
    List<Map<String, Object>> codeBasedPropositionItemContent = new ArrayList<>();
    Map<String, Object> feedPropositionItemContent = new HashMap<>();
    Map<String, Object> inAppPropositionItemData = new HashMap<>();
    Map<String, Object> expectedFeedMetadata = new HashMap<>();
    Map<String, Object> expectedInAppMetadata = new HashMap<>();
    Map<String, Object> expectedMobileParameters = new HashMap<>();
    Map<String, Object> expectedWebParameters = new HashMap<>();
    List<String> expectedRemoteAssets = new ArrayList<>();
    // test event data maps
    Map<String, Object> eventDataMapForJSON = new HashMap<>();
    Map<String, Object> eventDataMapForHTML = new HashMap<>();
    Map<String, Object> eventDataMapForInApp = new HashMap<>();
    // expected schema data content
    Map<String, Object> feedContentMap = new HashMap<>();
    Map<String, Object> htmlContentMap = new HashMap<>();
    Map<String, Object> jsonContentMap = new HashMap<>();
    Map<String, Object> inAppContentMap = new HashMap<>();

    @Before
    public void setup() throws JSONException {
        // setup feed content map
        String testJSONStringFeedContent = MessagingTestUtils.loadStringFromFile("propositionItemFeed.json");
        JSONObject feedRuleJSON = new JSONObject(testJSONStringFeedContent);
        Map<String, Object> feedRuleJsonMap = JSONUtils.toMap(feedRuleJSON);
        feedPropositionItemData = (Map<String, Object>) feedRuleJsonMap.get("data");
        feedPropositionItemContent = (Map<String, Object>) feedPropositionItemData.get("content");
        // setup feed content list
        String testCodeBasedListString = MessagingTestUtils.loadStringFromFile("codeBasedList.json");
        JSONObject codeBasedRuleJson = new JSONObject(testCodeBasedListString);
        Map<String, Object> codeBasedJsonMap = JSONUtils.toMap(codeBasedRuleJson);
        codeBasedPropositionItemData = (Map<String, Object>) codeBasedJsonMap.get("data");
        codeBasedPropositionItemContent = (List<Map<String, Object>>) codeBasedPropositionItemData.get("content");
        // setup in app content
        String testJSONStringInAppContent = MessagingTestUtils.loadStringFromFile("inappPropositionAllDataPresent.json");
        inAppRuleJSON = new JSONObject(testJSONStringInAppContent);
        JSONObject inAppConsequenceDetails = InternalMessagingUtils.getConsequenceDetails(inAppRuleJSON);
        Map<String, Object> inAppRuleJsonMap = (Map<String, Object>) JSONUtils.toMap(inAppConsequenceDetails).get("data");
        inAppPropositionItemData = inAppRuleJsonMap;
        // setup expected data
        expectedFeedMetadata = new HashMap<String, Object>() {{
            put("surface", "mobileapp://mockApp/feeds/testFeed");
            put("feedName", "testFeed");
            put("campaignName", "testCampaign");
        }};
        expectedMobileParameters = (Map<String, Object>) inAppPropositionItemData.get("mobileParameters");
        expectedWebParameters = (Map<String, Object>) inAppPropositionItemData.get("webParameters");
        expectedInAppMetadata = (Map<String, Object>) inAppPropositionItemData.get("meta");
        expectedRemoteAssets = (List<String>) inAppPropositionItemData.get("remoteAssets");

        // setup event data for inapp content
        inAppContentMap.put("content", inAppRuleJsonMap);
        eventDataMapForInApp.put("data", inAppContentMap);
        eventDataMapForInApp.put("id", testId);
        eventDataMapForInApp.put("schema", SchemaType.INAPP.toString());

        // setup event data for json/feed content
        jsonContentMap.put("content", feedPropositionItemContent);
        eventDataMapForJSON.put("id", testId);
        eventDataMapForJSON.put("schema", SchemaType.FEED.toString());
        eventDataMapForJSON.put("data", jsonContentMap);

        // setup event data for html content
        htmlContentMap.put("content", testStringContent);
        eventDataMapForHTML.put("id", testId);
        eventDataMapForHTML.put("schema", SchemaType.HTML_CONTENT.toString());
        eventDataMapForHTML.put("data", htmlContentMap);

        // setup expected feed data map
        String expectedFeedSchemaDataContent = "{\"actionTitle\":\"actionTitle\",\"imageUrl\":\"imageUrl\",\"actionUrl\":\"actionUrl\",\"title\":\"title\",\"body\":\"body\"}";
        feedContentMap = JSONUtils.toMap(new JSONObject(expectedFeedSchemaDataContent));
    }

    // test constructor
    @Test
    public void test_propositionItemConstructor_FeedJSONContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.FEED, feedPropositionItemData);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getPropositionItemId());
        assertEquals(SchemaType.FEED, propositionItem.getSchema());
        assertEquals(feedPropositionItemData, propositionItem.getData());
    }

    @Test
    public void test_propositionItemConstructor_StringContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.HTML_CONTENT, eventDataMapForHTML);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getPropositionItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertEquals(eventDataMapForHTML, propositionItem.getData());
    }

    // toEventData
    @Test
    public void test_toEventData_JsonContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.JSON_CONTENT, (Map<String, Object>) eventDataMapForJSON.get("data"));
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(SchemaType.JSON_CONTENT.toString(), propositionItemMap.get("schema"));
        assertEquals(eventDataMapForJSON.get("data"), propositionItemMap.get("data"));
    }

    @Test
    public void test_toEventData_StringContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.HTML_CONTENT, (Map<String, Object>) eventDataMapForHTML.get("data"));
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(SchemaType.HTML_CONTENT.toString(), propositionItemMap.get("schema"));
        assertEquals(eventDataMapForHTML.get("data"), propositionItemMap.get("data"));
    }

    @Test
    public void test_toEventData_FeedContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.FEED, feedPropositionItemData);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(SchemaType.FEED.toString(), propositionItemMap.get("schema"));
        assertEquals(feedPropositionItemData, propositionItemMap.get("data"));
    }

    @Test
    public void test_toEventData_NullContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.JSON_CONTENT, null);
        Map<String, Object> propositionItemMap = propositionItem.toEventData();
        // verify
        assertTrue(propositionItemMap.isEmpty());
    }

    // fromEventData
    @Test
    public void test_createPropositionItem_fromEventData_JSONContent() {
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getPropositionItemId());
        assertEquals(SchemaType.FEED, propositionItem.getSchema());
        Map<String, Object> propositionItemData = propositionItem.getData();
        assertEquals(feedPropositionItemData, propositionItemData);
    }

    @Test
    public void test_createPropositionItem_fromEventData_StringContent() {
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForHTML);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getPropositionItemId());
        assertEquals(SchemaType.HTML_CONTENT, propositionItem.getSchema());
        assertEquals(htmlContentMap, propositionItem.getData());
    }

    @Test
    public void test_createPropositionItem_fromEventData_NullData() {
        // setup
        eventDataMapForJSON.put("data", null);
        // test
        PropositionItem propositionItem = PropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNull(propositionItem);
    }

    // fromRuleConsequence
    @Test
    public void test_createPropositionItem_fromRuleConsequence() throws JSONException {
        // test
        Map<String, Object> consequenceDetails = JSONUtils.toMap(InternalMessagingUtils.getConsequenceDetails(inAppRuleJSON));
        RuleConsequence consequence = new RuleConsequence("testId", "cjmiam", consequenceDetails);
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(consequence);
        // verify
        assertNotNull(propositionItem);
        assertEquals(testId, propositionItem.getPropositionItemId());
        assertEquals(SchemaType.INAPP, propositionItem.getSchema());
        Map<String, Object> propositionItemData = propositionItem.getData();
        assertEquals(consequenceDetails.get("data"), propositionItemData);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_EmptyDetails() {
        // test
        RuleConsequence consequence = new RuleConsequence("testId", "cjmiam", Collections.emptyMap());
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(consequence);
        // verify
        assertNull(propositionItem);
    }

    @Test
    public void test_createPropositionItem_fromRuleConsequence_EmptyData() throws JSONException {
        // test
        Map<String, Object> consequenceDetails = JSONUtils.toMap(InternalMessagingUtils.getConsequenceDetails(inAppRuleJSON));
        consequenceDetails.remove("data");
        RuleConsequence consequence = new RuleConsequence("testId", "cjmiam", consequenceDetails);
        PropositionItem propositionItem = PropositionItem.fromRuleConsequence(consequence);
        // verify
        assertNull(propositionItem);
    }

    // getInAppSchemaData tests
    @Test
    public void test_getInAppSchemaData() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.INAPP, inAppPropositionItemData);
        InAppSchemaData schemaData = propositionItem.getInAppSchemaData();
        // verify
        assertNotNull(schemaData);
        assertEquals(ContentType.TEXT_HTML, schemaData.getContentType());
        assertEquals("<html>message here</html>", schemaData.getContent());
        assertEquals(expectedInAppMetadata, schemaData.getMeta());
        assertEquals(1712190456, schemaData.getExpiryDate());
        assertEquals(1701538942, schemaData.getPublishedDate());
        assertEquals(expectedMobileParameters, schemaData.getMobileParameters());
        assertEquals(expectedWebParameters, schemaData.getWebParameters());
        assertEquals(expectedRemoteAssets, schemaData.getRemoteAssets());
    }

    @Test
    public void test_getInAppSchemaData_emptyData() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.INAPP, Collections.emptyMap());
        InAppSchemaData schemaData = propositionItem.getInAppSchemaData();
        // verify
        assertNull(schemaData);
    }

    // getFeedItemSchemaData tests
    @Test
    public void test_getFeedItemSchemaData() {
        // setup
        ArrayList<Map<String, Object>> rules = (ArrayList) feedPropositionItemContent.get("rules");
        Map<String, Object> rule = rules.get(0);
        ArrayList<Map<String, Object>> consequences = (ArrayList<Map<String, Object>>) rule.get("consequences");
        Map<String, Object> consequence = consequences.get(0);
        Map<String, Object> consequenceDetail = (Map<String, Object>) consequence.get("detail");
        Map<String, Object> itemData = (Map<String, Object>) consequenceDetail.get("data");
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.FEED, itemData);
        FeedItemSchemaData schemaData = propositionItem.getFeedItemSchemaData();
        // verify
        assertNotNull(schemaData);
        assertEquals(ContentType.APPLICATION_JSON, schemaData.getContentType());
        assertEquals(feedContentMap, schemaData.getContent());
        assertEquals(expectedFeedMetadata, schemaData.getMeta());
        assertEquals(1717688797, schemaData.getExpiryDate());
        assertEquals(1717688797, schemaData.getPublishedDate());
    }

    @Test
    public void test_getFeedSchemaData_emptyData() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.FEED, Collections.emptyMap());
        FeedItemSchemaData schemaData = propositionItem.getFeedItemSchemaData();
        // verify
        assertNull(schemaData);
    }

    // getJsonContentMap tests
    @Test
    public void test_getJsonContentMap() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.JSON_CONTENT, feedPropositionItemData);
        Map<String, Object> jsonContentMap = propositionItem.getJsonContentMap();
        // verify
        assertNotNull(jsonContentMap);
        assertEquals(feedPropositionItemData.get("content"), jsonContentMap);
    }

    @Test
    public void test_getJsonContentMap_emptyData() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.JSON_CONTENT, Collections.emptyMap());
        Map<String, Object> jsonContentMap = propositionItem.getJsonContentMap();
        // verify
        assertNull(jsonContentMap);
    }

    // getJsonArrayContent tests
    @Test
    public void test_getJsonArrayContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.JSON_CONTENT, codeBasedPropositionItemData);
        List<Map<String, Object>> jsonArrayList = propositionItem.getJsonArrayList();
        // verify
        assertNotNull(jsonArrayList);
        assertEquals(codeBasedPropositionItemContent, jsonArrayList);
    }

    @Test
    public void test_getJsonArrayContent_emptyData() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.JSON_CONTENT, Collections.emptyMap());
        List<Map<String, Object>> jsonArrayList = propositionItem.getJsonArrayList();
        // verify
        assertNull(jsonArrayList);
    }

    // getHtmlContent tests
    @Test
    public void test_getHtmlContent() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.HTML_CONTENT, htmlContentMap);
        String htmlContent = propositionItem.getHtmlContent();
        // verify
        assertEquals(testStringContent, htmlContent);
    }

    @Test
    public void test_getHtmlContent_EmptyData() {
        // test
        PropositionItem propositionItem = new PropositionItem(testId, SchemaType.HTML_CONTENT, Collections.emptyMap());
        String htmlContent = propositionItem.getHtmlContent();
        // verify
        assertNull(htmlContent);
    }
}
