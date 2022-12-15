/*
  Copyright 2022 Adobe. All rights reserved.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
public class PropositionPayloadTests {
    private PropositionPayload propositionPayload;

    @Test
    public void testCreatePropositionPayload() {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload.json"));
        // test
        propositionPayload = MessagingUtils.getPropositionPayloads(testPayload).get(0);
        // verify proposition info
        assertEquals("activityId", propositionPayload.propositionInfo.activityId);
        assertEquals("correlationID", propositionPayload.propositionInfo.correlationId);
        assertEquals("scopeId", propositionPayload.propositionInfo.id);
        assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test", propositionPayload.propositionInfo.scope);
        // verify scope details
        Map<String, Object> scopeDetails = propositionPayload.propositionInfo.scopeDetails;
        Map<String, Object> characteristics = (Map<String, Object>) scopeDetails.get("characteristics");
        Map<String, Object> cjmEvent = (Map<String, Object>) characteristics.get("cjmEvent");
        Map<String, Object> messageExecution = (Map<String, Object>) cjmEvent.get("messageExecution");
        Map<String, Object> messageProfile = (Map<String, Object>) cjmEvent.get("messageProfile");
        Map<String, Object> channel = (Map<String, Object>) messageProfile.get("channel");
        assertEquals("messageExecutionID", messageExecution.get("messageExecutionID"));
        assertEquals("messagePublicationID", messageExecution.get("messagePublicationID"));
        assertEquals("campaignVersionID", messageExecution.get("campaignVersionID"));
        assertEquals("campaignActionID", messageExecution.get("campaignActionID"));
        assertEquals("campaignID", messageExecution.get("campaignID"));
        assertEquals("messageID", messageExecution.get("messageID"));
        assertEquals("marketing", messageExecution.get("messageType"));
        assertEquals("https://ns.adobe.com/xdm/channels/inApp", channel.get("_id"));
        assertEquals("https://ns.adobe.com/xdm/channel-types/inApp", channel.get("_type"));
        assertEquals("messageProfileID", messageProfile.get("messageProfileID"));
        assertEquals("AJO", scopeDetails.get("decisionProvider"));
        // verify payload item
        PayloadItem payloadItem = propositionPayload.items.get(0);
        assertEquals("itemId", payloadItem.id);
        assertEquals("https://ns.adobe.com/personalization/json-content-item", payloadItem.schema);
        // verify payload item data
        ItemData itemData = payloadItem.data;
        assertEquals("dataId", itemData.id);
        assertEquals("{\"version\":1,\"rules\":[{\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"definition\":{\"key\":\"foo\",\"matcher\":\"eq\",\"values\":[\"bar\"]},\"type\":\"matcher\"}],\"logic\":\"and\"}},\"consequences\":[{\"id\":\"ebdbd89e-3318-4720-afbc-d929890b28ae\",\"type\":\"cjmiam\",\"detail\":{\"mobileParameters\":{\"verticalAlign\":\"center\",\"horizontalInset\":0,\"dismissAnimation\":\"bottom\",\"uiTakeover\":true,\"horizontalAlign\":\"center\",\"verticalInset\":0,\"displayAnimation\":\"bottom\",\"width\":100,\"height\":100,\"gestures\":{}},\"html\":\"<html><head></head><body>Hello from InApp campaign: [CIT]::inapp::LqhnZy7y1Vo4EEWciU5qK</body></html>\",\"remoteAssets\":[]}}]}]}", itemData.content);
    }

    @Test
    public void testCreatePropositionPayload_MissingScopeDetails() {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload_missing_scope_details.json"));
        // test
        propositionPayload = MessagingUtils.getPropositionPayloads(testPayload).get(0);
        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }

    @Test
    public void testCreatePropositionPayload_MissingScope() {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload_missing_scope.json"));
        // test
        propositionPayload = MessagingUtils.getPropositionPayloads(testPayload).get(0);
        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }

    @Test
    public void testCreatePropositionPayload_MissingItems() {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload_missing_items.json"));
        // test
        propositionPayload = MessagingUtils.getPropositionPayloads(testPayload).get(0);
        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }

    @Test
    public void testCreatePropositionPayload_MissingId() {
        // setup
        final List<Map<String, Object>> testPayload = new ArrayList<>();
        testPayload.add(MessagingTestUtils.getMapFromFile("personalization_payload_missing_id.json"));
        // test
        propositionPayload = MessagingUtils.getPropositionPayloads(testPayload).get(0);
        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }
}