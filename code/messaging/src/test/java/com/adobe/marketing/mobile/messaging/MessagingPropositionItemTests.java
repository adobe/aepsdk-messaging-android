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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.JSON_RULES_KEY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONArray;
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
public class MessagingPropositionItemTests {
    String testStringContent = "some html string content";
    String testId = "uniqueId";
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
        JSONObject inAppRuleJSON = new JSONObject(testJSONStringInAppContent);
        JSONObject inAppConsequenceDetails = getConsequenceDetails(inAppRuleJSON);
        Map<String, Object> inAppRuleJsonMap = (Map<String, Object>) JSONUtils.toMap(inAppConsequenceDetails).get("data");
        inAppPropositionItemData = inAppRuleJsonMap;
        // setup expected data
        expectedFeedMetadata = new HashMap<String, Object>(){{
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
        eventDataMapForJSON.put("data", jsonContentMap);
        eventDataMapForJSON.put("id", testId);
        eventDataMapForJSON.put("schema", SchemaType.FEED.toString());

        // setup event data for html content
        htmlContentMap.put("content", testStringContent);
        eventDataMapForHTML.put("id", testId);
        eventDataMapForHTML.put("schema", SchemaType.HTML_CONTENT.toString());
        eventDataMapForHTML.put("data", htmlContentMap);

        // setup expected feed data map
        String expectedFeedSchemaDataContent = "{\"actionTitle\":\"actionTitle\",\"imageUrl\":\"imageUrl\",\"actionUrl\":\"actionUrl\",\"title\":\"title\",\"body\":\"body\"}";
        feedContentMap = JSONUtils.toMap(new JSONObject(expectedFeedSchemaDataContent));
    }

    private JSONObject getConsequenceDetails(final JSONObject ruleJson) {
        JSONObject consequenceDetails = null;
        try {
            consequenceDetails = getConsequence(ruleJson).getJSONObject(MESSAGE_CONSEQUENCE_DETAIL);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving consequence details: %s", jsonException.getLocalizedMessage());
        }
        return consequenceDetails;
    }

    private JSONObject getConsequence(final JSONObject ruleJson) {
        JSONObject consequence = null;
        try {
            final JSONArray rulesArray = ruleJson.getJSONArray(JSON_RULES_KEY);
            final JSONArray consequenceArray = rulesArray.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            consequence = consequenceArray.getJSONObject(0);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving rule consequence: %s", jsonException.getLocalizedMessage());
        }
        return consequence;
    }

    @Test
    public void test_propositionItemConstructor_FeedJSONContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.FEED, feedPropositionItemData);
        // verify
        assertNotNull(messagingPropositionItem);
        assertEquals(testId, messagingPropositionItem.getPropositionItemId());
        assertEquals(SchemaType.FEED, messagingPropositionItem.getSchema());
        assertEquals(feedPropositionItemData, messagingPropositionItem.getData());
    }

    @Test
    public void test_createPropositionItem_fromEventData_JSONContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNotNull(messagingPropositionItem);
        assertEquals(testId, messagingPropositionItem.getPropositionItemId());
        assertEquals(SchemaType.FEED, messagingPropositionItem.getSchema());
        Map<String, Object> propositionItemData = messagingPropositionItem.getData();
        assertEquals(feedPropositionItemData, propositionItemData);
    }

    @Test
    public void test_createEventData_fromFeedPropositionItem_JSONContent() throws DataReaderException {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.FEED, feedPropositionItemData);
        Map<String, Object> propositionItemMap = messagingPropositionItem.toEventData();
        // verify
        Map<String, Object> data = DataReader.getTypedMap(Object.class, propositionItemMap, "data");
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(SchemaType.FEED, propositionItemMap.get("schema"));
        assertEquals(feedPropositionItemData, data);
    }

    @Test
    public void test_propositionItemConstructor_StringContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.HTML_CONTENT, eventDataMapForHTML);
        // verify
        assertNotNull(messagingPropositionItem);
        assertEquals(testId, messagingPropositionItem.getPropositionItemId());
        assertEquals(SchemaType.HTML_CONTENT, messagingPropositionItem.getSchema());
        assertEquals(eventDataMapForHTML, messagingPropositionItem.getData());
    }

    @Test
    public void test_createPropositionItem_fromEventData_StringContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(eventDataMapForHTML);
        // verify
        assertNotNull(messagingPropositionItem);
        assertEquals(testId, messagingPropositionItem.getPropositionItemId());
        assertEquals(SchemaType.HTML_CONTENT, messagingPropositionItem.getSchema());
        assertEquals(htmlContentMap, messagingPropositionItem.getData());
    }

    @Test
    public void test_createEventData_fromPropositionItem_StringContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.HTML_CONTENT, eventDataMapForHTML);
        Map<String, Object> propositionItemMap = messagingPropositionItem.toEventData();
        // verify
        assertNotNull(propositionItemMap);
        assertEquals(testId, propositionItemMap.get("id"));
        assertEquals(SchemaType.HTML_CONTENT, propositionItemMap.get("schema"));
        assertEquals(eventDataMapForHTML, propositionItemMap.get("data"));
    }

    @Test
    public void test_createPropositionItem_fromEventData_NullData() {
        // setup
        eventDataMapForJSON.put("data", null);
        // test
        MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNull(messagingPropositionItem);
    }

    @Test
    public void test_createPropositionItem_fromEventData_NullJSONContent() {
        // setup
        final Map<String, Object> jsonDataMap = new HashMap<>();
        jsonDataMap.put("content", null);
        eventDataMapForJSON.put("data", jsonDataMap);
        // test
        MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(eventDataMapForJSON);
        // verify
        assertNull(messagingPropositionItem);
    }

    @Test
    public void test_createPropositionItem_fromEventData_NullStringContent() {
        // setup
        final Map<String, Object> htmlDataMap = new HashMap<>();
        htmlDataMap.put("content", null);
        eventDataMapForHTML.put("data", htmlDataMap);
        // test
        MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(eventDataMapForHTML);
        // verify
        assertNull(messagingPropositionItem);
    }

    @Test
    public void test_createEventData_fromPropositionItem_NullContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.JSON_CONTENT, null);
        Map<String, Object> propositionItemMap = messagingPropositionItem.toEventData();
        // verify
        assertTrue(propositionItemMap.isEmpty());
    }

    // getInAppSchemaData tests
    @Test
    public void test_getInAppSchemaData() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.INAPP, inAppPropositionItemData);
        InAppSchemaData schemaData = messagingPropositionItem.getInAppSchemaData();
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
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.INAPP, Collections.emptyMap());
        InAppSchemaData schemaData = messagingPropositionItem.getInAppSchemaData();
        // verify
        assertNull(schemaData);
    }

    // getFeedItemSchemaData tests
    @Test
    public void test_getFeedItemSchemaData() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.FEED, feedPropositionItemContent);
        FeedItemSchemaData schemaData = messagingPropositionItem.getFeedItemSchemaData();
        // verify
        assertNotNull(schemaData);
        assertEquals(ContentType.APPLICATION_JSON, schemaData.getContentType());
        assertEquals(feedContentMap, schemaData.getContent());
        assertEquals(expectedFeedMetadata, schemaData.getMeta());
        assertEquals(1717688797, schemaData.getExpiryDate());
        assertEquals(1717688797, schemaData.getPublishedDate());
    }

    // getJsonContentMap tests
    @Test
    public void test_getJsonContentMap() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.JSON_CONTENT, feedPropositionItemData);
        Map<String, Object> jsonContentMap = messagingPropositionItem.getJsonContentMap();
        // verify
        assertNotNull(jsonContentMap);
        assertEquals(feedPropositionItemData.get("content"), jsonContentMap);
    }

    // getJsonArrayContentMap tests
    @Test
    public void test_getJsonArrayContentMap() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.JSON_CONTENT, codeBasedPropositionItemData);
        List<Map<String, Object>> jsonArrayContentMap = messagingPropositionItem.getJsonArrayMap();
        // verify
        assertNotNull(jsonArrayContentMap);
        assertEquals(codeBasedPropositionItemContent, jsonArrayContentMap);
    }

    // getHtmlContentMap tests
    @Test
    public void test_getHtmlContent() {
        // test
        MessagingPropositionItem messagingPropositionItem = new MessagingPropositionItem(testId, SchemaType.HTML_CONTENT, htmlContentMap);
        String htmlContent = messagingPropositionItem.getHtmlContent();
        // verify
        assertEquals(testStringContent, htmlContent);
    }
}
