/*
 Copyright 2021 Adobe. All rights reserved.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InternalMessagingUtilsTests {
    private final String mockJsonObj = "{\n" +
            "   \"messageProfile\":{\n" +
            "      \"channel\":{\n" +
            "         \"_id\":\"https://ns.adobe.com/xdm/channels/push\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"pushChannelContext\":{\n" +
            "      \"platform\":\"fcm\"\n" +
            "   }\n" +
            "}";
    private final String mockJsonArr = "[\n" +
            "   {\n" +
            "      \"channel\": {\n" +
            "         \"_id\": \"https://ns.adobe.com/xdm/channels/push\"\n" +
            "      }\n" +
            "   },\n" +
            "   {\n" +
            "      \"platform\": \"fcm\"\n" +
            "   }\n" +
            "]";

    // ========================================================================================
    // toMap
    // ========================================================================================
    @Test
    public void test_toMap() {
        try {
            // mock
            JSONObject json = new JSONObject(mockJsonObj);

            // test
            Map<String, Object> result = JSONUtils.toMap(json);

            if (result == null) {
                Assert.fail();
            }

            // verify
            Assert.assertTrue(result.containsKey("messageProfile"));
            Assert.assertTrue(result.containsKey("pushChannelContext"));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void test_toMap_when_nullJson() {
        try {
            Assert.assertNull(JSONUtils.toMap(null));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    // ========================================================================================
    // toList
    // ========================================================================================
    @Test
    public void test_toList() {
        try {
            // mock
            JSONArray json = new JSONArray(mockJsonArr);

            // test
            List<Object> result = JSONUtils.toList(json);

            // verify
            assertEquals(result.size(), 2);
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void test_toList_when_nullJson() {
        try {
            Assert.assertNull(JSONUtils.toList(null));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    // ========================================================================================
    // event validators
    // ========================================================================================
    @Test
    public void testIsGenericIdentityRequest_validEvent() {
       Event event = new Event.Builder("generic identity event", EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT)
               .setEventData(new HashMap<>())
               .build();
       assertTrue(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsGenericIdentityRequest_invalidEvent() {
        Event event = new Event.Builder("generic identity event", EventType.GENERIC_IDENTITY, EventSource.RESPONSE_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsGenericIdentityRequest_nullEvent() {
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(null));
    }

    @Test
    public void testIsMessagingRequestContentEvent_validEvent() {
        Event event = new Event.Builder("messaging request event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertTrue(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_invalidEvent() {
        Event event = new Event.Builder("messaging request event", EventType.MESSAGING, EventSource.RESPONSE_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_nullEvent() {
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(null));
    }

    @Test
    public void testIsRefreshMessagesEvent_validEvent() {
        Event event = new Event.Builder("refresh messages event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap<String, Object>() {{ put("refreshmessages", true); }})
                .build();
        assertTrue(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_invalidEvent() {
        Event event = new Event.Builder("refresh messages event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_nullEvent() {
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(null));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_validEvent() {
        Event event = new Event.Builder("edge personalization event", EventType.EDGE, MessagingConstants.EventSource.PERSONALIZATION_DECISIONS)
                .setEventData(new HashMap())
                .build();
        assertTrue(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_invalidEvent() {
        Event event = new Event.Builder("edge personalization event", EventType.EDGE, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_nullEvent() {
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(null));
    }

    @Test
    public void testIsContentCompleteEvent_validEvent() {
        Event event = new Event.Builder("messaging complete event", EventType.MESSAGING, EventSource.CONTENT_COMPLETE)
                .setEventData(new HashMap())
                .build();
        assertTrue(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void testIsContentCompleteEvent_invalidEvent() {
        Event event = new Event.Builder("messaging complete event", EventType.MESSAGING, EventSource.RESPONSE_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void testIsContentCompleteEvent_nullEvent() {
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(null));
    }

    @Test
    public void testIsUpdatePropositionsEvent_validEvent() {
        Event event = new Event.Builder("update propositions event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap() {{ put("updatepropositions", true); }})
                .build();
        assertTrue(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_invalidEvent() {
        Event event = new Event.Builder("update propositions event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_nullEvent() {
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(null));
    }

    @Test
    public void testIsGetPropositionsEvent_validEvent() {
        Event event = new Event.Builder("get propositions event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap() {{ put("getpropositions", true); }})
                .build();
        assertTrue(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_invalidEvent() {
        Event event = new Event.Builder("get propositions event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(new HashMap<>())
                .build();
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_nullEvent() {
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(null));
    }
}
