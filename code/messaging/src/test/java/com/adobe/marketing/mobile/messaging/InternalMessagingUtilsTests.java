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

import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKey.REQUEST_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventDataKeys.Messaging.RESPONSE_ERROR;
import static com.adobe.marketing.mobile.messaging.MessagingTestConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class InternalMessagingUtilsTests {
    private final String mockJsonObj =
            "{\n"
                    + "   \"messageProfile\":{\n"
                    + "      \"channel\":{\n"
                    + "         \"_id\":\"https://ns.adobe.com/xdm/channels/push\"\n"
                    + "      }\n"
                    + "   },\n"
                    + "   \"pushChannelContext\":{\n"
                    + "      \"platform\":\"fcm\"\n"
                    + "   }\n"
                    + "}";
    private final String mockJsonArr =
            "[\n"
                    + "   {\n"
                    + "      \"channel\": {\n"
                    + "         \"_id\": \"https://ns.adobe.com/xdm/channels/push\"\n"
                    + "      }\n"
                    + "   },\n"
                    + "   {\n"
                    + "      \"platform\": \"fcm\"\n"
                    + "   }\n"
                    + "]";

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
            assertNull(JSONUtils.toMap(null));
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
            assertNull(JSONUtils.toList(null));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    // ========================================================================================
    // getPropositionsFromPayload
    // =======================================================================================
    @Test
    public void getPropositionsFromPayloads_returnsEmptyList_whenPayloadsIsNull() {
        // test
        List<Proposition> result = InternalMessagingUtils.getPropositionsFromPayloads(null);

        // verify
        assertTrue(result.isEmpty());
    }

    @Test
    public void getPropositionsFromPayloads_returnsEmptyList_whenPayloadsIsEmpty() {
        // test
        List<Proposition> result =
                InternalMessagingUtils.getPropositionsFromPayloads(new ArrayList<>());

        // verify
        assertTrue(result.isEmpty());
    }

    @Test
    public void getPropositionsFromPayloads_returnsEmptyList_whenPayloadsContainsNull() {
        // setup
        List<Map<String, Object>> payloads = new ArrayList<>();
        payloads.add(null);

        // test
        List<Proposition> result = InternalMessagingUtils.getPropositionsFromPayloads(payloads);

        // verify
        assertTrue(result.isEmpty());
    }

    @Test
    public void
            getPropositionsFromPayloads_returnsListWithProposition_whenPayloadsContainsValidData() {
        try (MockedStatic<Proposition> messagingPropositionMockedStatic =
                mockStatic(Proposition.class)) {
            // setup
            Proposition mockProposition = mock(Proposition.class);
            messagingPropositionMockedStatic
                    .when(() -> Proposition.fromEventData(anyMap()))
                    .thenReturn(mockProposition);
            List<Map<String, Object>> payloads = new ArrayList<>();
            Map<String, Object> payload = new HashMap<>();
            payload.put("key", "value");
            payloads.add(payload);

            // test
            List<Proposition> result = InternalMessagingUtils.getPropositionsFromPayloads(payloads);

            // verify
            assertEquals(1, result.size());
            assertEquals(mockProposition, result.get(0));
        }
    }

    @Test
    public void
            getPropositionsFromPayloads_returnsListWithProposition_whenPayloadsContainsNullPropositionItem() {
        try (MockedStatic<Proposition> messagingPropositionMockedStatic =
                mockStatic(Proposition.class)) {
            // setup
            Proposition mockProposition = mock(Proposition.class);
            messagingPropositionMockedStatic
                    .when(() -> Proposition.fromEventData(anyMap()))
                    .thenReturn(null);
            List<Map<String, Object>> payloads = new ArrayList<>();
            Map<String, Object> payload = new HashMap<>();
            payload.put("key", "value");
            payloads.add(payload);

            // test
            List<Proposition> result = InternalMessagingUtils.getPropositionsFromPayloads(payloads);

            // verify
            assertEquals(0, result.size());
        }
    }

    // ========================================================================================
    // Consequence data retrieval from a JSONObject
    // ========================================================================================
    @Test
    public void getConsequenceDetails_returnsNull_whenRuleJsonIsNull() {
        // verify
        assertNull(InternalMessagingUtils.getConsequenceDetails(null));
    }

    @Test
    public void getConsequenceDetails_returnsNull_whenRuleJsonDoesNotContainConsequences() {
        // setup
        JSONObject ruleJson = new JSONObject();

        // test
        JSONObject result = InternalMessagingUtils.getConsequenceDetails(ruleJson);

        // verify
        assertNull(result);
    }

    @Test
    public void getConsequenceDetails_returnsNull_whenRuleJsonDoesNotContainConsequence() {
        // setup
        JSONObject ruleJson = getRuleJsonWithEmptyConsequence();

        // test
        JSONObject result = InternalMessagingUtils.getConsequenceDetails(ruleJson);

        // verify
        assertNull(result);
    }

    @Test
    public void getConsequenceDetails_returnsNull_whenRuleJsonDoesNotContainConsequenceDetail() {
        // setup
        JSONObject ruleJson = getRuleJsonWithNoConsequenceDetail();

        // test
        JSONObject result = InternalMessagingUtils.getConsequenceDetails(ruleJson);

        // verify
        assertNull(result);
    }

    @Test
    public void
            getConsequenceDetails_returnsConsequenceDetails_whenRuleJsonContainsConsequenceDetails()
                    throws JSONException {
        // setup
        JSONObject ruleJson = getValidRuleJson();
        assertNotNull(ruleJson);

        // test
        JSONObject result = InternalMessagingUtils.getConsequenceDetails(ruleJson);

        // verify
        assertNotNull(result);
    }

    @Test
    public void getConsequence_returnsNull_whenRuleJsonIsNull() {
        // verify
        assertNull(InternalMessagingUtils.getConsequence(null));
    }

    @Test
    public void getConsequence_returnsNull_whenRuleJsonDoesNotContainRules() {
        // setup
        JSONObject ruleJson = new JSONObject();

        // test
        JSONObject result = InternalMessagingUtils.getConsequence(ruleJson);

        // verify
        assertNull(result);
    }

    @Test
    public void getConsequence_returnsConsequence_whenRuleJsonContainsValidConsequence()
            throws JSONException {
        // setup
        JSONObject ruleJson = getValidRuleJson();

        // test
        JSONObject result = InternalMessagingUtils.getConsequence(ruleJson);

        // verify
        assertNotNull(result);
    }

    @Test
    public void getConsequence_returnsConsequence_whenRuleJsonContainsEmptyConsequence()
            throws JSONException {
        // setup
        JSONObject ruleJson = getRuleJsonWithEmptyConsequence();

        // test
        JSONObject result = InternalMessagingUtils.getConsequence(ruleJson);

        // verify
        assertNull(result);
    }

    // ========================================================================================
    // Cache Path helper
    // ========================================================================================
    @Test
    public void getAssetCacheLocation_returnsNull_whenDeviceInfoServiceIsNull() {
        // setup
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                mockStatic(ServiceProvider.class)) {
            ServiceProvider mockServiceProvider = mock(ServiceProvider.class);
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            Mockito.when(mockServiceProvider.getDeviceInfoService()).thenReturn(null);

            // test
            String result = InternalMessagingUtils.getAssetCacheLocation();

            // verify
            assertNull(result);
        }
    }

    @Test
    public void getAssetCacheLocation_returnsNull_whenApplicationCacheDirIsNull() {
        // setup
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                mockStatic(ServiceProvider.class)) {
            ServiceProvider mockServiceProvider = mock(ServiceProvider.class);
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            DeviceInforming mockDeviceInfoService = mock(DeviceInforming.class);
            Mockito.when(mockServiceProvider.getDeviceInfoService())
                    .thenReturn(mockDeviceInfoService);
            Mockito.when(mockDeviceInfoService.getApplicationCacheDir()).thenReturn(null);

            // test
            String result = InternalMessagingUtils.getAssetCacheLocation();

            // verify
            assertNull(result);
        }
    }

    @Test
    public void getAssetCacheLocation_returnsCorrectPath_whenApplicationCacheDirIsNotNull() {
        // setup
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                mockStatic(ServiceProvider.class)) {
            ServiceProvider mockServiceProvider = mock(ServiceProvider.class);
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            DeviceInforming mockDeviceInfoService = mock(DeviceInforming.class);
            Mockito.when(mockServiceProvider.getDeviceInfoService())
                    .thenReturn(mockDeviceInfoService);
            Mockito.when(mockDeviceInfoService.getApplicationCacheDir())
                    .thenReturn(new File("/path/to/cache"));

            // test
            String result = InternalMessagingUtils.getAssetCacheLocation();

            // verify
            assertEquals(
                    "/path/to/cache"
                            + File.separator
                            + MessagingTestConstants.CACHE_BASE_DIR
                            + File.separator
                            + MessagingTestConstants.IMAGES_CACHE_SUBDIRECTORY,
                    result);
        }
    }

    // ========================================================================================
    // event validators
    // ========================================================================================
    @Test
    public void testIsGenericIdentityRequest_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "generic identity event",
                                EventType.GENERIC_IDENTITY,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertTrue(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsGenericIdentityRequest_returnFalse_eventTypeIsNotGenericIdentity() {
        Event event =
                new Event.Builder(
                                "generic identity event",
                                EventType.EDGE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsGenericIdentityRequest_returnFalse_eventSourceIsNotRequestContent() {
        Event event =
                new Event.Builder(
                                "generic identity event",
                                EventType.GENERIC_IDENTITY,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsGenericIdentityRequest_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "generic identity event",
                                EventType.GENERIC_IDENTITY,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsGenericIdentityRequest_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(null));
    }

    @Test
    public void testIsGenericIdentityRequest_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "generic identity event",
                                EventType.GENERIC_IDENTITY,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isGenericIdentityRequestEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "messaging request event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertTrue(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_returnTrue_eventTypeIsNotMessaging() {
        Event event =
                new Event.Builder(
                                "messaging request event",
                                EventType.EDGE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_returnTrue_eventSourceIsNotRequestContent() {
        Event event =
                new Event.Builder(
                                "messaging request event",
                                EventType.EDGE,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "messaging request event",
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsMessagingRequestContentEvent_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(null));
    }

    @Test
    public void testIsMessagingRequestContentEvent_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "messaging request event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isMessagingRequestContentEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "refresh messages event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("refreshmessages", true);
                                    }
                                })
                        .build();
        assertTrue(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_returnFalse_eventTypeIsNotMessaging() {
        Event event =
                new Event.Builder(
                                "refresh messages event",
                                EventType.EDGE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("refreshmessages", true);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_returnFalse_eventSourceIsNotRequestContent() {
        Event event =
                new Event.Builder(
                                "refresh messages event",
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("refreshmessages", true);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "refresh messages event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsRefreshMessagesEvent_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(null));
    }

    @Test
    public void testIsRefreshMessagesEvent_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "refresh messages event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isRefreshMessagesEvent(event));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "edge personalization event",
                                EventType.EDGE,
                                MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS)
                        .setEventData(new HashMap())
                        .build();
        assertTrue(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_returnFalse_eventTypeIsNotEdge() {
        Event event =
                new Event.Builder(
                                "edge personalization event",
                                EventType.MESSAGING,
                                MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS)
                        .setEventData(new HashMap())
                        .build();
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void
            testIsEdgePersonalizationDecisionEvent_returnFalse_eventSourceIsNotPersonalizationDecisions() {
        Event event =
                new Event.Builder(
                                "edge personalization event",
                                EventType.EDGE,
                                MessagingTestConstants.EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap())
                        .build();
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "edge personalization event",
                                EventType.EDGE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(null));
    }

    @Test
    public void testIsEdgePersonalizationDecisionEvent_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "edge personalization event",
                                EventType.EDGE,
                                MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isEdgePersonalizationDecisionEvent(event));
    }

    @Test
    public void testIsPersonalizationRequestCompleteEvent_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "messaging complete event",
                                EventType.MESSAGING,
                                EventSource.CONTENT_COMPLETE)
                        .setEventData(new HashMap())
                        .build();
        assertTrue(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void testIsPersonalizationRequestCompleteEvent_returnFalse_eventTypeIsNotMessaging() {
        Event event =
                new Event.Builder(
                                "messaging complete event",
                                EventType.EDGE,
                                EventSource.CONTENT_COMPLETE)
                        .setEventData(new HashMap())
                        .build();
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void
            testIsPersonalizationRequestCompleteEvent_returnFalse_eventSourceIsNotContentComplete() {
        Event event =
                new Event.Builder(
                                "messaging complete event",
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap())
                        .build();
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void testIsPersonalizationRequestCompleteEvent_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "messaging complete event",
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void testIsPersonalizationRequestCompleteEvent_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(null));
    }

    @Test
    public void testIsPersonalizationRequestCompleteEvent_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "messaging complete event",
                                EventType.MESSAGING,
                                EventSource.CONTENT_COMPLETE)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isPersonalizationRequestCompleteEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("updatepropositions", true);
                                    }
                                })
                        .build();
        assertTrue(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnFalse_eventTypeIsNotMessaging() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.EDGE,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("updatepropositions", true);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnFalse_eventSourceIsNotRequestContent() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("updatepropositions", true);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnFalse_updatePropositionsIsFalse() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("updatepropositions", false);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(null));
    }

    @Test
    public void testIsUpdatePropositionsEvent_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isUpdatePropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_returnTrue_validEvent() {
        Event event =
                new Event.Builder(
                                "get propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("getpropositions", true);
                                    }
                                })
                        .build();
        assertTrue(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_returnFalse_eventTypeIsNotMessaging() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.EDGE,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("getpropositions", true);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_returnFalse_eventSourceIsNotRequestContent() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("getpropositions", true);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_returnFalse_getPropositionsIsFalse() {
        Event event =
                new Event.Builder(
                                "update propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap() {
                                    {
                                        put("getpropositions", false);
                                    }
                                })
                        .build();
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_returnFalse_invalidEvent() {
        Event event =
                new Event.Builder(
                                "get propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsGetPropositionsEvent_returnFalse_nullEvent() {
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(null));
    }

    @Test
    public void testIsGetPropositionsEvent_returnFalse_nullEventData() {
        Event event =
                new Event.Builder(
                                "get propositions event",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();
        assertFalse(InternalMessagingUtils.isGetPropositionsEvent(event));
    }

    @Test
    public void testIsTrackingPropositionsEvent_returnFalse_nullEvent() {
        // setup
        Event event = null;

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertFalse(result);
    }

    @Test
    public void testIsTrackingPropositionsEvent_returnFalse_nullEventData() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertFalse(result);
    }

    @Test
    public void testIsTrackingPropositionsEvent_returnFalse_EventTypeIsNotMessaging() {
        // setup
        Event event =
                new Event.Builder("event", EventType.IDENTITY, EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertFalse(result);
    }

    @Test
    public void testIsTrackingPropositionsEvent_returnFalse_EventSourceIsNotRequestContent() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.RESPONSE_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertFalse(result);
    }

    @Test
    public void
            testIsTrackingPropositionsEvent_returnFalse_TrackPropositionsIsNotPresentInEventData() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertFalse(result);
    }

    @Test
    public void testIsTrackingPropositionsEvent_returnFalse_trackPropositionsIsFalse() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put(
                                                MessagingTestConstants.EventDataKeys.Messaging
                                                        .TRACK_PROPOSITIONS,
                                                false);
                                    }
                                })
                        .build();

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertFalse(result);
    }

    @Test
    public void testIsTrackingPropositionsEvent_returnTrue_whenAllConditionsAreMet() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put(
                                                MessagingTestConstants.EventDataKeys.Messaging
                                                        .TRACK_PROPOSITIONS,
                                                true);
                                    }
                                })
                        .build();

        // test
        boolean result = InternalMessagingUtils.isTrackingPropositionsEvent(event);

        // verify
        assertTrue(result);
    }

    // ========================================================================================
    // Surfaces retrieval and validation
    // ========================================================================================
    @Test
    public void getSurfaces_returnsNull_whenEventIsNull() {
        // setup
        Event event = null;

        // test
        List<Surface> result = InternalMessagingUtils.getSurfaces(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getSurfaces_returnsNull_whenEventDataIsNull() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();

        // test
        List<Surface> result = InternalMessagingUtils.getSurfaces(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getSurfaces_returnsNull_whenSurfacesNotFoundInEvent() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();

        // test
        List<Surface> result = InternalMessagingUtils.getSurfaces(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getSurfaces_returnsListOfSurfaces_whenSurfacesIsEmpty() {
        // setup
        List<Map<String, Object>> surfaces = new ArrayList<>();
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingTestConstants.EventDataKeys.Messaging.SURFACES, surfaces);
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // test
        List<Surface> result = InternalMessagingUtils.getSurfaces(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getSurfaces_returnsListOfSurfaces_whenSurfacesFoundInEvent() {
        // setup
        Map<String, Object> surfaceData1 = new HashMap<>();
        surfaceData1.put("uri", "mobileapp://surface/path1");
        Map<String, Object> surfaceData2 = new HashMap<>();
        surfaceData2.put("uri", "mobileapp://surface/path2");
        List<Map<String, Object>> surfaces = new ArrayList<>();
        surfaces.add(surfaceData1);
        surfaces.add(surfaceData2);
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingTestConstants.EventDataKeys.Messaging.SURFACES, surfaces);
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // test
        List<Surface> result = InternalMessagingUtils.getSurfaces(event);

        // verify
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("mobileapp://surface/path1", result.get(0).getUri());
        assertEquals("mobileapp://surface/path2", result.get(1).getUri());
    }

    // ========================================================================================
    // Event id retrieval
    // ========================================================================================
    @Test
    public void getRequestEventId_returnsParentId_whenEventHasParentId() {
        // setup
        Event event = mock(Event.class);
        Mockito.when(event.getParentID()).thenReturn("parent-id");
        Mockito.when(event.getEventData()).thenReturn(new HashMap<>());

        // test
        String result = InternalMessagingUtils.getRequestEventId(event);

        // verify
        assertEquals("parent-id", result);
    }

    @Test
    public void getRequestEventId_returnsRequestEventId_whenEventHasNoParentId() {
        // setup
        Event event = mock(Event.class);
        Mockito.when(event.getParentID()).thenReturn(null);
        Mockito.when(event.getEventData())
                .thenReturn(
                        new HashMap<String, Object>() {
                            {
                                put(REQUEST_EVENT_ID, "request-id");
                            }
                        });

        // test
        String result = InternalMessagingUtils.getRequestEventId(event);

        // verify
        assertEquals("request-id", result);
    }

    @Test
    public void getRequestEventId_returnsNull_whenEventHasNoParentIdAndNoRequestEventId() {
        // setup
        Event event = mock(Event.class);
        Mockito.when(event.getParentID()).thenReturn(null);
        Mockito.when(event.getEventData()).thenReturn(new HashMap<>());

        // test
        String result = InternalMessagingUtils.getRequestEventId(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getEndingEventId_returnsNull_whenEventIsNull() {
        // setup
        Event event = null;

        // test
        String result = InternalMessagingUtils.getEndingEventId(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getEndingEventId_returnsNull_whenEventDataIsNull() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(null)
                        .build();

        // test
        String result = InternalMessagingUtils.getEndingEventId(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getEndingEventId_returnsNull_whenEndingEventIdNotFoundInEvent() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();

        // test
        String result = InternalMessagingUtils.getEndingEventId(event);

        // verify
        assertNull(result);
    }

    @Test
    public void getEndingEventId_returnsEndingEventId_whenEndingEventIdFoundInEvent() {
        // setup
        Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                MessagingTestConstants.EventDataKeys.Messaging.ENDING_EVENT_ID, "ending-event-id");
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .build();

        // test
        String result = InternalMessagingUtils.getEndingEventId(event);

        // verify
        assertEquals("ending-event-id", result);
    }

    // ========================================================================================
    // Error Event creation
    // ========================================================================================
    @Test
    public void createErrorResponseEvent_returnsEvent_withErrorInEventData() {
        // setup
        Event event =
                new Event.Builder("event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();
        AdobeError error = AdobeError.UNEXPECTED_ERROR;

        // test
        Event result = InternalMessagingUtils.createErrorResponseEvent(event, error);

        // verify
        assertEquals(result.getResponseID(), event.getUniqueIdentifier());
        assertEquals(EventType.MESSAGING, result.getType());
        assertEquals(EventSource.RESPONSE_CONTENT, result.getSource());
        assertEquals(MESSAGE_PROPOSITIONS_RESPONSE, result.getName());
        assertEquals(error.getErrorName(), result.getEventData().get(RESPONSE_ERROR));
    }

    // ========================================================================================
    // Event Dispatching
    // ========================================================================================
    @Test
    public void sendEvent_dispatchesEvent_withProvidedParameters() {
        // setup
        String eventName = "event-name";
        String eventType = EventType.MESSAGING;
        String eventSource = EventSource.REQUEST_CONTENT;
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        String[] mask = new String[] {"key"};
        ExtensionApi extensionApi = mock(ExtensionApi.class);

        // test
        InternalMessagingUtils.sendEvent(
                eventName, eventType, eventSource, data, mask, extensionApi, null);

        // verify
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(extensionApi).dispatch(eventCaptor.capture());
        Event dispatchedEvent = eventCaptor.getValue();
        assertEquals(eventName, dispatchedEvent.getName());
        assertEquals(eventType, dispatchedEvent.getType());
        assertEquals(eventSource, dispatchedEvent.getSource());
        assertEquals(data, dispatchedEvent.getEventData());
        assertArrayEquals(mask, dispatchedEvent.getMask());
    }

    @Test
    public void sendEvent_dispatchesEvent_withProvidedParametersAndNullMask() {
        // setup
        String eventName = "event-name";
        String eventType = EventType.MESSAGING;
        String eventSource = EventSource.REQUEST_CONTENT;
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        ExtensionApi extensionApi = mock(ExtensionApi.class);

        // test
        InternalMessagingUtils.sendEvent(
                eventName, eventType, eventSource, data, extensionApi, null);

        // verify
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(extensionApi).dispatch(eventCaptor.capture());
        Event dispatchedEvent = eventCaptor.getValue();
        assertEquals(eventName, dispatchedEvent.getName());
        assertEquals(eventType, dispatchedEvent.getType());
        assertEquals(eventSource, dispatchedEvent.getSource());
        assertEquals(data, dispatchedEvent.getEventData());
        assertNull(dispatchedEvent.getMask());
    }

    @Test
    public void sendTrackingResponseEvent_dispatchesEvent_withProvidedParameters() {
        // setup
        PushTrackingStatus status = PushTrackingStatus.TRACKING_INITIATED;
        ExtensionApi extensionApi = mock(ExtensionApi.class);
        Event requestEvent =
                new Event.Builder("request-event", EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                        .setEventData(new HashMap<>())
                        .build();

        // test
        InternalMessagingUtils.sendTrackingResponseEvent(status, extensionApi, requestEvent);

        // verify
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(extensionApi).dispatch(eventCaptor.capture());
        Event dispatchedEvent = eventCaptor.getValue();
        assertEquals(
                MessagingTestConstants.EventName.PUSH_TRACKING_STATUS_EVENT,
                dispatchedEvent.getName());
        assertEquals(EventType.MESSAGING, dispatchedEvent.getType());
        assertEquals(EventSource.RESPONSE_CONTENT, dispatchedEvent.getSource());
        assertEquals(
                status.getValue(),
                dispatchedEvent
                        .getEventData()
                        .get(
                                MessagingTestConstants.EventDataKeys.Messaging
                                        .PUSH_NOTIFICATION_TRACKING_STATUS));
        assertEquals(
                status.getDescription(),
                dispatchedEvent
                        .getEventData()
                        .get(
                                MessagingTestConstants.EventDataKeys.Messaging
                                        .PUSH_NOTIFICATION_TRACKING_MESSAGE));
        assertEquals(requestEvent.getUniqueIdentifier(), dispatchedEvent.getResponseID());
    }

    // ========================================================================================
    // Shared State Helpers
    // ========================================================================================
    @Test
    public void getSharedStateEcid_returnsNull_whenEdgeIdentityStateIsNull() {
        // setup
        Map<String, Object> edgeIdentityState = null;

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsNull_whenIdentityMapIsMissing() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsNull_whenIdentityMapIsNull() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();
        edgeIdentityState.put(MessagingTestConstants.SharedState.EdgeIdentity.IDENTITY_MAP, null);

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsNull_whenIdentityMapIsEmpty() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();
        edgeIdentityState.put(
                MessagingTestConstants.SharedState.EdgeIdentity.IDENTITY_MAP, new HashMap<>());

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsNull_whenEcidListIsNull() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();
        Map<String, Object> identityMap = new HashMap<>();
        identityMap.put(MessagingTestConstants.SharedState.EdgeIdentity.ECID, null);
        edgeIdentityState.put(
                MessagingTestConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsNull_whenEcidListIsEmpty() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();
        Map<String, Object> identityMap = new HashMap<>();
        identityMap.put(MessagingTestConstants.SharedState.EdgeIdentity.ECID, new ArrayList<>());
        edgeIdentityState.put(
                MessagingTestConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsNull_whenEcidListIsHasEmptyEcidMap() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();
        Map<String, Object> identityMap = new HashMap<>();
        identityMap.put(
                MessagingTestConstants.SharedState.EdgeIdentity.ECID,
                Collections.singletonList(new HashMap<>()));
        edgeIdentityState.put(
                MessagingTestConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertNull(result);
    }

    @Test
    public void getSharedStateEcid_returnsEcid_whenAllConditionsAreMet() {
        // setup
        Map<String, Object> edgeIdentityState = new HashMap<>();
        Map<String, Object> identityMap = new HashMap<>();
        Map<String, Object> ecidMap = new HashMap<>();
        ecidMap.put(MessagingTestConstants.SharedState.EdgeIdentity.ID, "ecid-value");
        List<Map<String, Object>> ecids = new ArrayList<>();
        ecids.add(ecidMap);
        identityMap.put(MessagingTestConstants.SharedState.EdgeIdentity.ECID, ecids);
        edgeIdentityState.put(
                MessagingTestConstants.SharedState.EdgeIdentity.IDENTITY_MAP, identityMap);

        // test
        String result = InternalMessagingUtils.getSharedStateEcid(edgeIdentityState);

        // verify
        assertEquals("ecid-value", result);
    }

    // ========================================================================================
    // Collection utils
    // ========================================================================================
    @Test
    public void updateRuleMapForSurface_returnsOriginalMap_whenRulesToAddIsNull() {
        // setup
        Surface surface = mock(Surface.class);
        Map<Surface, List<LaunchRule>> originalMap = new HashMap<>();

        // test
        Map<Surface, List<LaunchRule>> result =
                InternalMessagingUtils.updateRuleMapForSurface(surface, null, originalMap);

        // verify
        assertSame(originalMap, result);
    }

    @Test
    public void updateRuleMapForSurface_returnsOriginalMap_whenRulesToAddIsEmpty() {
        // setup
        Surface surface = mock(Surface.class);
        Map<Surface, List<LaunchRule>> originalMap = new HashMap<>();

        // test
        Map<Surface, List<LaunchRule>> result =
                InternalMessagingUtils.updateRuleMapForSurface(
                        surface, new ArrayList<>(), originalMap);

        // verify
        assertSame(originalMap, result);
    }

    @Test
    public void updateRuleMapForSurface_addsNewSurfaceAndRules_whenOriginalMapIsEmpty() {
        // setup
        Surface surface = Surface.fromUriString("mobileapp://surface/path");
        List<LaunchRule> rulesToAdd = Collections.singletonList(mock(LaunchRule.class));
        Map<Surface, List<LaunchRule>> originalMap = new HashMap<>();

        // test
        Map<Surface, List<LaunchRule>> result =
                InternalMessagingUtils.updateRuleMapForSurface(surface, rulesToAdd, originalMap);

        // verify
        assertNotSame(originalMap, result);
        assertEquals(rulesToAdd, result.get(surface));
    }

    @Test
    public void updateRuleMapForSurface_appendsRules_whenSurfaceInOriginalMap() {
        // setup
        Surface surface = Surface.fromUriString("mobileapp://surface/path");
        LaunchRule mockedLaunchRule = mock(LaunchRule.class);
        List<LaunchRule> originalRules =
                new ArrayList<LaunchRule>() {
                    {
                        add(mockedLaunchRule);
                    }
                };
        List<LaunchRule> rulesToAdd =
                new ArrayList<LaunchRule>() {
                    {
                        add(mockedLaunchRule);
                    }
                };
        Map<Surface, List<LaunchRule>> originalMap = new HashMap<>();
        originalMap.put(surface, originalRules);

        // test
        Map<Surface, List<LaunchRule>> result =
                InternalMessagingUtils.updateRuleMapForSurface(surface, rulesToAdd, originalMap);

        // verify
        assertNotSame(originalMap, result);
        assertEquals(2, result.get(surface).size());
    }

    @Test
    public void updateRuleMapForSurface_appendsRules_whenSurfaceNotInOriginalMap() {
        // setup
        Surface surface = Surface.fromUriString("mobileapp://surface/path");
        List<LaunchRule> originalRules = Collections.singletonList(mock(LaunchRule.class));
        List<LaunchRule> rulesToAdd = Collections.singletonList(mock(LaunchRule.class));
        Map<Surface, List<LaunchRule>> originalMap = new HashMap<>();
        originalMap.put(surface, originalRules);
        Surface surfaceToAdd = Surface.fromUriString("mobileapp://surface/path2");

        // test
        Map<Surface, List<LaunchRule>> result =
                InternalMessagingUtils.updateRuleMapForSurface(
                        surfaceToAdd, rulesToAdd, originalMap);

        // verify
        assertNotSame(originalMap, result);
        assertEquals(2, result.size());
        assertEquals(1, result.get(surface).size());
        assertEquals(1, result.get(surfaceToAdd).size());
    }

    // ========================================================================================
    // Test utilities
    // ========================================================================================
    private JSONObject getValidRuleJson() {
        String rulesJson = MessagingTestUtils.loadStringFromFile("inappPropositionV2Content.json");
        try {
            return new JSONObject(rulesJson);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getRuleJsonWithEmptyConsequence() {
        String rulesJson = MessagingTestUtils.loadStringFromFile("ruleWithNoConsequence.json");
        try {
            return new JSONObject(rulesJson);
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject getRuleJsonWithNoConsequenceDetail() {
        String rulesJson =
                MessagingTestUtils.loadStringFromFile("ruleWithNoConsequenceDetail.json");
        try {
            return new JSONObject(rulesJson);
        } catch (JSONException e) {
            return null;
        }
    }
}
