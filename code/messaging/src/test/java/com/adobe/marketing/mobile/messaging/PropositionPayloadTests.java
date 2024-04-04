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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PropositionPayloadTests {

    @Test
    public void testCreatePropositionPayload() {
        // setup
        final Map<String, Object> testPayload =
                MessagingTestUtils.getMapFromFile("personalizationPayloadV1.json");
        final PropositionInfo propositionInfo = PropositionInfo.create(testPayload);

        // test
        final PropositionPayload propositionPayload =
                PropositionPayload.create(
                        propositionInfo, (List<Map<String, Object>>) testPayload.get("items"));

        // verify proposition info
        assertEquals("activityId", propositionPayload.propositionInfo.activityId);
        assertEquals("correlationID", propositionPayload.propositionInfo.correlationId);
        assertEquals("uniqueId", propositionPayload.propositionInfo.id);
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test",
                propositionPayload.propositionInfo.scope);
        // verify scope details
        Map<String, Object> scopeDetails = propositionPayload.propositionInfo.scopeDetails;
        Map<String, Object> characteristics =
                (Map<String, Object>) scopeDetails.get("characteristics");
        Map<String, Object> cjmEvent = (Map<String, Object>) characteristics.get("cjmEvent");
        Map<String, Object> messageExecution =
                (Map<String, Object>) cjmEvent.get("messageExecution");
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
        assertEquals("https://ns.adobe.com/personalization/ruleset-item", payloadItem.schema);
        // verify payload item data
        Map<String, Object> data = payloadItem.data;
        assertEquals("dataId", data.get("id"));
        assertEquals(
                "{\"version\":1,\"rules\":[{\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"definition\":{\"key\":\"foo\",\"matcher\":\"eq\",\"values\":[\"bar\"]},\"type\":\"matcher\"}],\"logic\":\"and\"}},\"consequences\":[{\"id\":\"ebdbd89e-3318-4720-afbc-d929890b28ae\",\"type\":\"schema\",\"detail\":{\"mobileParameters\":{\"verticalAlign\":\"center\",\"horizontalInset\":0,\"dismissAnimation\":\"bottom\",\"uiTakeover\":true,\"horizontalAlign\":\"center\",\"verticalInset\":0,\"displayAnimation\":\"bottom\",\"width\":100,\"height\":100,\"gestures\":{}},\"html\":\"<html><head></head><body>Hello"
                    + " from InApp campaign:"
                    + " [CIT]::inapp::LqhnZy7y1Vo4EEWciU5qK</body></html>\",\"remoteAssets\":[]}}]}]}",
                data.get("content"));
    }

    @Test
    public void testCreatePropositionPayload_NullPropositionInfo() {
        // test
        final PropositionPayload propositionPayload =
                PropositionPayload.create(null, new ArrayList<>());

        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }

    @Test
    public void testCreatePropositionPayload_NullItems() {
        // setup
        final Map<String, Object> testPayload =
                MessagingTestUtils.getMapFromFile("personalizationPayloadV1.json");
        final PropositionInfo propositionInfo = PropositionInfo.create(testPayload);

        // test
        final PropositionPayload propositionPayload =
                PropositionPayload.create(propositionInfo, null);

        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }

    @Test
    public void testCreatePropositionPayload_EmptyItems() {
        // setup
        final Map<String, Object> testPayload =
                MessagingTestUtils.getMapFromFile("personalizationPayloadV1.json");
        final PropositionInfo propositionInfo = PropositionInfo.create(testPayload);

        // test
        final PropositionPayload propositionPayload =
                PropositionPayload.create(propositionInfo, new ArrayList<>());

        // verify proposition payload failed to be created
        assertNull(propositionPayload);
    }

    @Test
    public void testCreatePropositionPayload_InvalidItems() {
        // setup
        final Map<String, Object> testPayload =
                MessagingTestUtils.getMapFromFile("personalizationPayloadV1InvalidItems.json");
        final PropositionInfo propositionInfo = PropositionInfo.create(testPayload);

        // test
        final PropositionPayload propositionPayload =
                PropositionPayload.create(
                        propositionInfo, (List<Map<String, Object>>) testPayload.get("items"));

        // verify proposition payload is created
        assertNotNull(propositionPayload);
        assertEquals(propositionInfo, propositionPayload.propositionInfo);
        assertTrue(propositionPayload.items.isEmpty());
    }
}
