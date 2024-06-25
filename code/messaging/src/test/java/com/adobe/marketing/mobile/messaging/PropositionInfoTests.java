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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionInfoTests {
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
                    put("correlationID", "correlationId");
                    put("characteristics", characteristics);
                    put("activity", activity);
                }
            };
    List<PropositionItem> propositionItems = new ArrayList<>();
    Map<String, Object> propositionItemMap = new HashMap<>();
    private PropositionInfo propositionInfo;

    @Before
    public void setup() throws JSONException {
        propositionItemMap = MessagingTestUtils.getMapFromFile("feedPropositionItem.json");
        PropositionItem propositionItem =
                PropositionItem.fromPropositionItemsMap(propositionItemMap);
        propositionItems.add(propositionItem);
    }

    @Test
    public void testCreatePropositionInfoFromProposition()
            throws MessageRequiredFieldMissingException {
        // setup
        Proposition proposition =
                new Proposition("id", "mobileapp://mockScope", scopeDetails, propositionItems);
        // test
        propositionInfo = PropositionInfo.createFromProposition(proposition);
        // verify
        assertNotNull(propositionInfo);
        assertEquals("id", propositionInfo.id);
        assertEquals("activityId", propositionInfo.activityId);
        assertEquals("correlationId", propositionInfo.correlationId);
        assertEquals("mobileapp://mockScope", propositionInfo.scope);
        assertEquals(scopeDetails, propositionInfo.scopeDetails);
    }

    @Test
    public void testCreatePropositionInfoFromProposition_nullProposition() {
        // test
        propositionInfo = PropositionInfo.createFromProposition(null);
        // verify
        assertNull(propositionInfo);
    }

    @Test
    public void testCreatePropositionInfoFromMap() {
        // setup
        Map<String, Object> propositionInfoMap = new HashMap<>();
        propositionInfoMap.put("id", "id");
        propositionInfoMap.put("scope", "mobileapp://mockScope");
        propositionInfoMap.put("scopeDetails", scopeDetails);
        // test
        propositionInfo = PropositionInfo.create(propositionInfoMap);
        // verify
        assertNotNull(propositionInfo);
        assertEquals("id", propositionInfo.id);
        assertEquals("activityId", propositionInfo.activityId);
        assertEquals("correlationId", propositionInfo.correlationId);
        assertEquals("mobileapp://mockScope", propositionInfo.scope);
        assertEquals(scopeDetails, propositionInfo.scopeDetails);
    }

    @Test
    public void testCreatePropositionInfoFromMap_nullId() {
        // setup
        Map<String, Object> propositionInfoMap = new HashMap<>();
        propositionInfoMap.put("id", null);
        propositionInfoMap.put("scope", "mobileapp://mockScope");
        propositionInfoMap.put("scopeDetails", scopeDetails);
        // test
        propositionInfo = PropositionInfo.create(propositionInfoMap);
        // verify
        assertNull(propositionInfo);
    }

    @Test
    public void testCreatePropositionInfoFromMap_nullScope() {
        // setup
        Map<String, Object> propositionInfoMap = new HashMap<>();
        propositionInfoMap.put("id", null);
        propositionInfoMap.put("scope", null);
        propositionInfoMap.put("scopeDetails", scopeDetails);
        // test
        propositionInfo = PropositionInfo.create(propositionInfoMap);
        // verify
        assertNull(propositionInfo);
    }

    @Test
    public void testCreatePropositionInfoFromMap_nullScopeDetails() {
        // setup
        Map<String, Object> propositionInfoMap = new HashMap<>();
        propositionInfoMap.put("id", "id");
        propositionInfoMap.put("scope", "mobileapp://mockScope");
        propositionInfoMap.put("scopeDetails", null);
        // test
        propositionInfo = PropositionInfo.create(propositionInfoMap);
        // verify
        assertNull(propositionInfo);
    }
}
