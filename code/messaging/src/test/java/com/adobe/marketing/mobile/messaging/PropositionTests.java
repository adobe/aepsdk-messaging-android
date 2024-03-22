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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionTests {

    Map<String, Object> characteristics =
            new HashMap<String, Object>() {
                {
                    put("eventToken", "eventToken");
                }
            };
    Map<String, Object> activity =
            new HashMap<String, Object>() {
                {
                    put("id", "activityId");
                }
            };
    Map<String, Object> scopeDetails =
            new HashMap<String, Object>() {
                {
                    put("decisionProvider", "AJO");
                    put("correlationID", "correlationID");
                    put("characteristics", characteristics);
                    put("activity", activity);
                }
            };

    Map<String, Object> propositionItemMap = new HashMap<>();
    Map<String, Object> propositionItemMap2 = new HashMap<>();
    Map<String, Object> eventDataMap = new HashMap<>();
    List<PropositionItem> propositionItems = new ArrayList<>();
    List<PropositionItem> propositionItems2 = new ArrayList<>();
    List<Map<String, Object>> propositionItemMaps = new ArrayList<>();

    @Before
    public void setup() throws JSONException {
        propositionItemMap = MessagingTestUtils.getMapFromFile("propositionItemFeed.json");
        propositionItemMap2 = MessagingTestUtils.getMapFromFile("propositionItemFeed2.json");
        PropositionItem propositionItem = PropositionItem.fromEventData(propositionItemMap);
        PropositionItem propositionItem2 = PropositionItem.fromEventData(propositionItemMap2);
        propositionItems.add(propositionItem);
        propositionItems2.add(propositionItem2);
        propositionItemMaps.add(propositionItemMap);
        eventDataMap.put("id", "uniqueId");
        eventDataMap.put("scope", "mobileapp://mockScope");
        eventDataMap.put("scopeDetails", scopeDetails);
        eventDataMap.put("items", propositionItemMaps);
    }

    @Test
    public void test_propositionConstructor() throws MessageRequiredFieldMissingException {
        // test
        Proposition proposition =
                new Proposition(
                        "uniqueId", "mobileapp://mockScope", scopeDetails, propositionItems);
        // verify
        assertNotNull(proposition);
        assertEquals("uniqueId", proposition.getUniqueId());
        assertEquals("mobileapp://mockScope", proposition.getScope());
        assertEquals(scopeDetails, proposition.getScopeDetails());
        assertEquals(propositionItems, proposition.getItems());
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_MissingId()
            throws MessageRequiredFieldMissingException {
        // test
        new Proposition(null, "mobileapp://mockScope", scopeDetails, propositionItems);
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_MissingScope()
            throws MessageRequiredFieldMissingException {
        // test
        new Proposition("uniqueId", null, scopeDetails, propositionItems);
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_MissingScopeDetails()
            throws MessageRequiredFieldMissingException {
        // test
        new Proposition("uniqueId", "mobileapp://mockScope", null, propositionItems);
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_EmptyId() throws MessageRequiredFieldMissingException {
        // test
        new Proposition("", "mobileapp://mockScope", scopeDetails, propositionItems);
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_EmptyScope()
            throws MessageRequiredFieldMissingException {
        // test
        new Proposition("uniqueId", "", scopeDetails, propositionItems);
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_EmptyScopeDetails()
            throws MessageRequiredFieldMissingException {
        // test
        new Proposition("uniqueId", "mobileapp://mockScope", new HashMap<>(), propositionItems);
    }

    @Test(expected = MessageRequiredFieldMissingException.class)
    public void test_propositionConstructor_MissingPropositionItems()
            throws MessageRequiredFieldMissingException {
        // test
        Proposition proposition =
                new Proposition("uniqueId", "mobileapp://mockScope", scopeDetails, null);
        // verify
        assertNotNull(proposition);
        assertEquals("uniqueId", proposition.getUniqueId());
        assertEquals("mobileapp://mockScope", proposition.getScope());
        assertEquals(scopeDetails, proposition.getScopeDetails());
        assertEquals(0, proposition.getItems().size());
    }

    @Test
    public void test_createProposition_fromEventData() {
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNotNull(proposition);
        assertEquals("uniqueId", proposition.getUniqueId());
        assertEquals("mobileapp://mockScope", proposition.getScope());
        assertEquals(scopeDetails, proposition.getScopeDetails());
        // need to verify proposition items individually as proposition soft references will be
        // different as the proposition is created from a map
        for (PropositionItem item : proposition.getItems()) {
            assertEquals(propositionItems.get(0).getItemId(), item.getItemId());
            assertEquals(propositionItems.get(0).getData(), item.getData());
            assertEquals(propositionItems.get(0).getSchema(), item.getSchema());
            assertNotNull(item.getProposition());
        }
    }

    @Test
    public void test_createProposition_fromEventData_MissingId() {
        // test
        eventDataMap.remove("id");
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNull(proposition);
    }

    @Test
    public void test_createProposition_fromEventData_MissingScope() {
        // test
        eventDataMap.remove("scope");
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNull(proposition);
    }

    @Test
    public void test_createProposition_fromEventData_MissingScopeDetails() {
        // test
        eventDataMap.remove("scopeDetails");
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNull(proposition);
    }

    @Test
    public void test_createProposition_fromEventData_EmptyScopeDetails() {
        // test
        eventDataMap.put("scopeDetails", new HashMap<>());
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNull(proposition);
    }

    @Test
    public void test_createProposition_fromEventData_MissingPropositionItems() {
        // test
        eventDataMap.remove("items");
        // test
        Proposition proposition = Proposition.fromEventData(eventDataMap);
        // verify
        assertNotNull(proposition);
        assertEquals("uniqueId", proposition.getUniqueId());
        assertEquals("mobileapp://mockScope", proposition.getScope());
        assertEquals(scopeDetails, proposition.getScopeDetails());
        assertEquals(0, proposition.getItems().size());
    }

    @Test
    public void test_createEventData_fromProposition()
            throws DataReaderException, JSONException, MessageRequiredFieldMissingException {
        // test
        Proposition proposition =
                new Proposition(
                        "uniqueId", "mobileapp://mockScope", scopeDetails, propositionItems);
        Map<String, Object> propositionMap = proposition.toEventData();
        // verify
        assertNotNull(propositionMap);
        List<Map<String, Object>> itemList =
                DataReader.optTypedListOfMap(Object.class, propositionMap, "items", null);
        PropositionItem propositionItem = propositionItems.get(0);
        for (Map<String, Object> item : itemList) {
            Map<String, Object> data = DataReader.getTypedMap(Object.class, item, "data");
            Map<String, Object> content = DataReader.getTypedMap(Object.class, data, "content");
            assertEquals(propositionItem.getItemId(), item.get("id"));
            assertEquals(propositionItem.getSchema().toString(), item.get("schema"));
            Map<String, Object> expectedContent =
                    JSONUtils.toMap(
                            new JSONObject(
                                    "{\"version\":1,\"rules\":[{\"consequences\":[{\"type\":\"schema\",\"id\":\"uniqueId\",\"detail\":{\"schema\":\"https://ns.adobe.com/personalization/message/feed-item\",\"data\":{\"expiryDate\":1717688797,\"publishedDate\":1717688797,\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp/feeds/testFeed\",\"feedName\":\"testFeed\",\"campaignName\":\"testCampaign\"},\"content\":{\"actionUrl\":\"actionUrl\",\"actionTitle\":\"actionTitle\",\"title\":\"title\",\"body\":\"body\",\"imageUrl\":\"imageUrl\"}},\"id\":\"uniqueId\"}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}]}"));
            assertEquals(expectedContent, content);
        }
    }

    @Test
    public void test_equals() throws MessageRequiredFieldMissingException {
        // test
        Proposition proposition1 =
                new Proposition(
                        "uniqueId", "mobileapp://mockScope", scopeDetails, propositionItems);
        Proposition proposition2 =
                new Proposition(
                        "uniqueId", "mobileapp://mockScope", scopeDetails, propositionItems);
        Proposition proposition3 =
                new Proposition(
                        "uniqueId2", "mobileapp://mockScope2", scopeDetails, propositionItems2);

        Object notAMessagingProposition = new Object();
        // verify
        assertEquals(proposition1, proposition2);
        assertNotEquals(proposition1, notAMessagingProposition);
        assertNotEquals(proposition1, proposition3);
    }
}
