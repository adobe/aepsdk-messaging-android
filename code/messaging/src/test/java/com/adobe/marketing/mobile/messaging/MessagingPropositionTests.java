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
public class MessagingPropositionTests {

    Map<String, Object> characteristics = new HashMap<String, Object>() {{
        put("eventToken", "eventToken");
    }};
    Map<String, Object> activity = new HashMap<String, Object>() {{
        put("id", "activityId");
    }};
    Map<String, Object> scopeDetails = new HashMap<String, Object>() {{
        put("decisionProvider", "AJO");
        put("correlationID", "correlationID");
        put("characteristics", characteristics);
        put("activity", activity);
    }};

    Map<String, Object> propositionItemMap = new HashMap<>();
    Map<String, Object> propositionItemMap2 = new HashMap<>();
    Map<String, Object> eventDataMap = new HashMap<>();
    List<MessagingPropositionItem> messagingPropositionItems = new ArrayList<>();
    List<MessagingPropositionItem> messagingPropositionItems2 = new ArrayList<>();
    List<Map<String, Object>> propositionItemMaps = new ArrayList<>();

    @Before
    public void setup() throws JSONException {
        propositionItemMap = MessagingTestUtils.getMapFromFile("propositionItemFeed.json");
        propositionItemMap2 = MessagingTestUtils.getMapFromFile("propositionItemFeed2.json");
        MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(propositionItemMap);
        MessagingPropositionItem messagingPropositionItem2 = MessagingPropositionItem.fromEventData(propositionItemMap2);
        messagingPropositionItems.add(messagingPropositionItem);
        messagingPropositionItems2.add(messagingPropositionItem2);
        propositionItemMaps.add(propositionItemMap);
        eventDataMap.put("id", "uniqueId");
        eventDataMap.put("scope", "mobileapp://mockScope");
        eventDataMap.put("scopeDetails", scopeDetails);
        eventDataMap.put("items", propositionItemMaps);
    }

    @Test
    public void test_propositionConstructor() {
        // test
        MessagingProposition messagingProposition = new MessagingProposition("uniqueId", "mobileapp://mockScope", scopeDetails, messagingPropositionItems);
        // verify
        assertNotNull(messagingProposition);
        assertEquals("uniqueId", messagingProposition.getUniqueId());
        assertEquals("mobileapp://mockScope", messagingProposition.getScope());
        assertEquals(scopeDetails, messagingProposition.getScopeDetails());
        assertEquals(messagingPropositionItems, messagingProposition.getItems());
    }

    @Test
    public void test_createProposition_fromEventData() {
        // test
        MessagingProposition messagingProposition = MessagingProposition.fromEventData(eventDataMap);
        // verify
        assertNotNull(messagingProposition);
        assertEquals("uniqueId", messagingProposition.getUniqueId());
        assertEquals("mobileapp://mockScope", messagingProposition.getScope());
        assertEquals(scopeDetails, messagingProposition.getScopeDetails());
        // need to verify proposition items individually as proposition soft references will be different as the proposition is created from a map
        for (MessagingPropositionItem item : messagingProposition.getItems()) {
            assertEquals(messagingPropositionItems.get(0).getPropositionItemId(), item.getPropositionItemId());
            assertEquals(messagingPropositionItems.get(0).getData(), item.getData());
            assertEquals(messagingPropositionItems.get(0).getSchema(), item.getSchema());
            assertNotNull(item.getProposition());
        }
    }

    @Test
    public void test_createEventData_fromProposition() throws DataReaderException, JSONException {
        // test
        MessagingProposition messagingProposition = new MessagingProposition("uniqueId", "mobileapp://mockScope", scopeDetails, messagingPropositionItems);
        Map<String, Object> propositionMap = messagingProposition.toEventData();
        // verify
        assertNotNull(propositionMap);
        List<Map<String, Object>> itemList = DataReader.optTypedListOfMap(Object.class, propositionMap, "items", null);
        MessagingPropositionItem messagingPropositionItem = messagingPropositionItems.get(0);
        for (Map<String, Object> item : itemList) {
            Map<String, Object> data = DataReader.getTypedMap(Object.class, item, "data");
            Map<String, Object> content = DataReader.getTypedMap(Object.class, data, "content");
            assertEquals(messagingPropositionItem.getPropositionItemId(), item.get("id"));
            assertEquals(messagingPropositionItem.getSchema().toString(), item.get("schema"));
            Map<String, Object> expectedContent = JSONUtils.toMap(new JSONObject("{\"version\":1,\"rules\":[{\"consequences\":[{\"type\":\"schema\",\"id\":\"uniqueId\",\"detail\":{\"schema\":\"https://ns.adobe.com/personalization/message/feed-item\",\"data\":{\"expiryDate\":1717688797,\"publishedDate\":1717688797,\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp/feeds/testFeed\",\"feedName\":\"testFeed\",\"campaignName\":\"testCampaign\"},\"content\":{\"actionUrl\":\"actionUrl\",\"actionTitle\":\"actionTitle\",\"title\":\"title\",\"body\":\"body\",\"imageUrl\":\"imageUrl\"}},\"id\":\"uniqueId\"}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}]}"));
            assertEquals(expectedContent, content);
        }
    }

    @Test
    public void test_equals() {
        // test
        MessagingProposition messagingProposition1 = new MessagingProposition("uniqueId", "mobileapp://mockScope", scopeDetails, messagingPropositionItems);
        MessagingProposition messagingProposition2 = new MessagingProposition("uniqueId", "mobileapp://mockScope", scopeDetails, messagingPropositionItems);
        MessagingProposition messagingProposition3 = new MessagingProposition("uniqueId2", "mobileapp://mockScope2", scopeDetails, messagingPropositionItems2);

        Object notAMessagingProposition = new Object();
        // verify
        assertEquals(messagingProposition1, messagingProposition2);
        assertNotEquals(messagingProposition1, notAMessagingProposition);
        assertNotEquals(messagingProposition1, messagingProposition3);
    }
}
