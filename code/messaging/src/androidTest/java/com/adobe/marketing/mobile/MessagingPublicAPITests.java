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

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.TestHelper.getSharedStateFor;
import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.messaging.BuildConfig;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class MessagingPublicAPITests {
    static {
        BuildConfig.IS_E2E_TEST.set(false);
        BuildConfig.IS_FUNCTIONAL_TEST.set(true);
    }

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        MessagingTestUtils.setEdgeIdentityPersistence(MessagingTestUtils.createIdentityMap("ECID", "mockECID"), TestHelper.defaultApplication);
        HashMap<String, Object> config = new HashMap<String, Object>() {
            {
                put("messaging.eventDataset", "somedatasetid");
            }
        };
        MobileCore.updateConfiguration(config);
        Messaging.registerExtension();
        com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(Object o) {
                latch.countDown();
            }
        });

        latch.await();
        resetTestExpectations();
    }

    // --------------------------------------------------------------------------------------------
    // Tests for GetExtensionVersion API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testGetExtensionVersionAPI() {
        Assert.assertEquals(MessagingConstants.EXTENSION_VERSION, Messaging.extensionVersion());
    }

    // --------------------------------------------------------------------------------------------
    // Tests for RegisterExtension API
    // --------------------------------------------------------------------------------------------

    @Test
    public void testRegisterExtensionAPI() throws InterruptedException {
        // test
        // Messaging.registerExtension() is called in the setup method

        // verify that the extension is registered with the correct version details
        Map<String, String> sharedStateMap = MessagingTestUtils.flattenMap(getSharedStateFor(MessagingTestConstants.SharedStateName.EVENT_HUB, 1000));
        assertEquals(MessagingConstants.EXTENSION_VERSION, sharedStateMap.get("extensions.com.adobe.messaging.version"));
    }

    // --------------------------------------------------------------------------------------------
    // Tests for Messaging.addPushTrackingDetails API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testAddPushTrackingDetails() {
        // Parameters
        Intent intent = new Intent();
        String messageId = "mock_message_id";
        Map<String, String> data = new HashMap<>();
        data.put(MessagingTestConstants.TrackingKeys._XDM, "xdmjson");

        // test
        boolean updated = Messaging.addPushTrackingDetails(intent, messageId, data);

        // verify intent is updated
        assertTrue(updated);

        // verify if the intent is updated with messageId and xdm data.
        String actualMsgId = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
        assertEquals(messageId, actualMsgId);
        assertEquals("xdmjson", actualXdmData);
    }

    @Test
    public void testAddPushTrackingDetails_dataIsNull() {
        // Parameters
        Intent intent = new Intent();
        String messageId = "mock_message_id";

        // test
        boolean updated = Messaging.addPushTrackingDetails(intent, messageId, null);

        // verify intent is updated
        assertFalse(updated);

        // verify if the intent is updated with messageId and xdm data.
        String actualMsgId = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
        assertNull(actualMsgId);
        assertNull(actualXdmData);
    }

    @Test
    public void testAddPushTrackingDetails_messageIdIsMissing() {
        // Parameters
        Intent intent = new Intent();
        Map<String, String> data = new HashMap<>();
        data.put(MessagingTestConstants.TrackingKeys._XDM, "xdmjson");

        // test
        boolean updated = Messaging.addPushTrackingDetails(intent, null, data);

        // verify intent is updated
        assertFalse(updated);

        // verify if the intent is updated with messageId and xdm data.
        String actualMsgId = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
        assertNull(actualMsgId);
        assertNull(actualXdmData);
    }

    @Test
    public void testAddPushTrackingDetails_xdmKeyIsMissing() {
        // Parameters
        Intent intent = new Intent();
        String messageId = "mock_message_id";
        Map<String, String> data = new HashMap<>();
        data.put("someRandomKey", "value");

        // test
        boolean updated = Messaging.addPushTrackingDetails(intent, messageId, data);

        // verify intent is updated
        assertTrue(updated);

        // verify if the intent is updated with messageId and xdm data.
        String actualMsgId = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData = intent.getStringExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
        assertEquals(messageId, actualMsgId);
        assertNull(actualXdmData);
    }

    @Test
    public void testAddPushTrackingDetails_intentIsMissing() {
        String messageId = "mock_message_id";
        Map<String, String> data = new HashMap<>();
        data.put(MessagingTestConstants.TrackingKeys._XDM, "xdmjson");

        // test
        boolean updated = Messaging.addPushTrackingDetails(null, messageId, data);

        // verify intent is updated
        assertFalse(updated);
    }

    // --------------------------------------------------------------------------------------------
    // Tests for Messaging.handleNotificationResponse API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testHandleNotificationResponse() throws InterruptedException {
        // Parameters
        Intent intent = getResponseIntent();
        String mockCustomActionId = "mockCustomActionId";
        String expectedMessagingEventData = "{\"messageId\":\"mockMessageId\",\"actionId\":\"mockCustomActionId\",\"eventType\":\"pushTracking.customAction\",\"applicationOpened\":true,\"adobe_xdm\":\"{\n" +
                "            \"cjm\" :{\n" +
                "              \"_experience\": {\n" +
                "                \"customerJourneyManagement\": {\n" +
                "                  \"messageExecution\": {\n" +
                "                    \"messageExecutionID\": \"16-Sept-postman\",\n" +
                "                    \"messageID\": \"567\",\n" +
                "                    \"journeyVersionID\": \"some-journeyVersionId\",\n" +
                "                    \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\"}";
        String expectedEdgeEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"customAction\":{\"actionID\":\"mockCustomActionId\"},\"pushProviderMessageID\":\"mockMessageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"pushTracking.customAction\",\"_experience\":{\"customerJourneyManagement\":{\"pushChannelContext\":{\"platform\":\"fcm\"},\"messageExecution\":{\"messageExecutionID\":\"16-Sept-postman\",\"journeyVersionInstanceID\":\"someJourneyVersionInstanceID\",\"messageID\":\"567\",\"journeyVersionID\":\"some-journeyVersionId\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/push\"}}}}},\"meta\":{\"collect\":{\"datasetId\":\"somedatasetid\"}}}";

        // test
        Messaging.handleNotificationResponse(intent, true, mockCustomActionId);

        // verify messaging event
        List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(expectedMessagingEventData, messagingRequestEvents.get(0).getData().toString());

        // verify edge events (2 events due to initial in-app message fetch request)
        List<Event> edgeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(2, edgeRequestEvents.size());
        assertEquals(expectedEdgeEventData, edgeRequestEvents.get(1).getData().toString());
    }

    @Test
    public void testHandleNotificationResponse_noCustomActionId() throws InterruptedException {
        // Parameters
        Intent intent = getResponseIntent();
        String expectedMessagingEventData = "{\"messageId\":\"mockMessageId\",\"eventType\":\"pushTracking.applicationOpened\",\"applicationOpened\":true,\"adobe_xdm\":\"{\n" +
                "            \"cjm\" :{\n" +
                "              \"_experience\": {\n" +
                "                \"customerJourneyManagement\": {\n" +
                "                  \"messageExecution\": {\n" +
                "                    \"messageExecutionID\": \"16-Sept-postman\",\n" +
                "                    \"messageID\": \"567\",\n" +
                "                    \"journeyVersionID\": \"some-journeyVersionId\",\n" +
                "                    \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\"}";
        String expectedEdgeEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"pushProviderMessageID\":\"mockMessageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"pushTracking.applicationOpened\",\"_experience\":{\"customerJourneyManagement\":{\"pushChannelContext\":{\"platform\":\"fcm\"},\"messageExecution\":{\"messageExecutionID\":\"16-Sept-postman\",\"journeyVersionInstanceID\":\"someJourneyVersionInstanceID\",\"messageID\":\"567\",\"journeyVersionID\":\"some-journeyVersionId\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/push\"}}}}},\"meta\":{\"collect\":{\"datasetId\":\"somedatasetid\"}}}";

        // test
        Messaging.handleNotificationResponse(intent, true, null);

        // verify messaging event
        List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(expectedMessagingEventData, messagingRequestEvents.get(0).getData().toString());

        // verify edge events (2 events due to initial in-app message fetch request)
        List<Event> edgeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(2, edgeRequestEvents.size());
        assertEquals(expectedEdgeEventData, edgeRequestEvents.get(1).getData().toString());
    }

    @Test
    public void testHandleNotificationResponse_noIntent() throws InterruptedException {
        // test
        Messaging.handleNotificationResponse(null, true, "customAction");

        // verify messaging event
        List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(0, messagingRequestEvents.size());

        // verify edge event
        List<Event> edgeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(0, edgeRequestEvents.size());
    }

    @Test
    public void testHandleNotificationResponse_noMessageId() throws InterruptedException {
        // test
        Messaging.handleNotificationResponse(new Intent(), true, "customAction");

        // verify messaging event
        List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(0, messagingRequestEvents.size());

        // verify edge event
        List<Event> edgeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(0, edgeRequestEvents.size());
    }

    @Test
    public void testHandleNotificationResponse_noXdmData() throws InterruptedException {
        // Parameters
        Intent intent = new Intent();
        intent.putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mockMessageId");

        // expected
        String expectedMessagingEventData = "{\"messageId\":\"mockMessageId\",\"eventType\":\"pushTracking.applicationOpened\",\"applicationOpened\":true,\"adobe_xdm\":null}";
        String expectedEdgeEventData = "{\"xdm\":{\"pushNotificationTracking\":{\"pushProviderMessageID\":\"mockMessageId\",\"pushProvider\":\"fcm\"},\"application\":{\"launches\":{\"value\":1}},\"eventType\":\"pushTracking.applicationOpened\",\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/push\"}}}}},\"meta\":{\"collect\":{\"datasetId\":\"somedatasetid\"}}}";

        // test
        Messaging.handleNotificationResponse(intent, true, null);

        // verify messaging event
        List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(expectedMessagingEventData, messagingRequestEvents.get(0).getData().toString());

        // verify edge events (2 events due to initial in-app message fetch request)
        List<Event> edgeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(2, edgeRequestEvents.size());
    }

    @Test
    public void testHandleNotificationResponse_emptyDatasetId() throws InterruptedException {
        // update config
        HashMap<String, Object> config = new HashMap<String, Object>() {
            {
                put("messaging.eventDataset", "");
            }
        };
        MobileCore.updateConfiguration(config);

        TestHelper.waitForThreads(1000);

        // Parameters
        Intent intent = getResponseIntent();
        String mockCustomActionId = "mockCustomActionId";
        String expectedMessagingEventData = "{\"messageId\":\"mockMessageId\",\"actionId\":\"mockCustomActionId\",\"eventType\":\"pushTracking.customAction\",\"applicationOpened\":true,\"adobe_xdm\":\"{\n" +
                "            \"cjm\" :{\n" +
                "              \"_experience\": {\n" +
                "                \"customerJourneyManagement\": {\n" +
                "                  \"messageExecution\": {\n" +
                "                    \"messageExecutionID\": \"16-Sept-postman\",\n" +
                "                    \"messageID\": \"567\",\n" +
                "                    \"journeyVersionID\": \"some-journeyVersionId\",\n" +
                "                    \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\"}";

        // test
        Messaging.handleNotificationResponse(intent, true, mockCustomActionId);

        // verify messaging event
        List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(expectedMessagingEventData, messagingRequestEvents.get(0).getData().toString());

        // verify edge event
        List<Event> edgeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(0, edgeRequestEvents.size());
    }

    // --------------------------------------------------------------------------------------------
    // Tests for Messaging.refreshInAppMessages API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testRefreshInAppMessages() throws InterruptedException {
        // setup
        final String expectedMessagingEventData = "{\"refreshmessages\":true}";
        final String expectedEdgePersonalizationEventData = "{\"xdm\":{\"eventType\":\"personalization.request\"},\"query\":{\"personalization\":{\"surfaces\":[\"mobileapp://com.adobe.marketing.mobile.messaging.test\"]}}}";
        // test
        Messaging.refreshInAppMessages();
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(expectedMessagingEventData, messagingRequestEvents.get(0).getData().toString());

        // verify edge request content events (initial offers fetch on app launch and offers fetch from refreshInAppMessages API)
        final List<Event> edgePersonalizationRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(2, edgePersonalizationRequestEvents.size());
        assertEquals(expectedEdgePersonalizationEventData.trim(), edgePersonalizationRequestEvents.get(1).getData().toString());
    }

    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------
    private Intent getResponseIntent() {
        Intent intent = new Intent();
        intent.putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "mockMessageId");
        intent.putExtra(MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, "{\n            \"cjm\" :{\n              \"_experience\": {\n                \"customerJourneyManagement\": {\n                  \"messageExecution\": {\n                    \"messageExecutionID\": \"16-Sept-postman\",\n                    \"messageID\": \"567\",\n                    \"journeyVersionID\": \"some-journeyVersionId\",\n                    \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n                  }\n                }\n              }\n            }\n          }");
        return intent;
    }
}
