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

import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.util.TestHelper.getSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.resetTestExpectations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.TestRetryRule;
import com.adobe.marketing.mobile.util.TestableNetworkRequest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MessagingPublicAPITests {
    static {
        BuildConfig.IS_E2E_TEST.set(false);
        BuildConfig.IS_FUNCTIONAL_TEST.set(true);
    }

    @Rule
    public RuleChain rule =
            RuleChain.outerRule(new TestHelper.SetupCoreRule())
                    .around(new TestHelper.RegisterMonitorExtensionRule());

    // A test will be retried at most 3 times
    @Rule public TestRetryRule totalTestCount = new TestRetryRule(3);

    // --------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        MessagingTestUtils.setEdgeIdentityPersistence(
                MessagingTestUtils.createIdentityMap("ECID", "mockECID"),
                TestHelper.getDefaultApplication());
        HashMap<String, Object> config =
                new HashMap<String, Object>() {
                    {
                        put("messaging.eventDataset", "somedatasetid");
                        put("edge.configId", "someedgeconfigid");
                    }
                };
        MobileCore.updateConfiguration(config);

        final CountDownLatch latch = new CountDownLatch(1);
        final List<Class<? extends Extension>> extensions =
                new ArrayList<Class<? extends Extension>>() {
                    {
                        add(Messaging.EXTENSION);
                        add(Identity.EXTENSION);
                        add(Edge.EXTENSION);
                    }
                };

        MobileCore.registerExtensions(extensions, o -> latch.countDown());

        // wait for the initial edge personalization request to be made before resetting the monitor
        // extension
        List<Event> dispatchedEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.CONTENT_COMPLETE, 5000);
        assertEquals(1, dispatchedEvents.size());
        resetTestExpectations();
    }

    // --------------------------------------------------------------------------------------------
    // Tests for RegisterExtension API
    // --------------------------------------------------------------------------------------------

    @Test
    public void testRegisterExtensionAPI() throws InterruptedException {
        // test
        // Messaging.registerExtension() is called in the setup method

        // verify that the extension is registered with the correct version details
        Map<String, String> sharedStateMap =
                MessagingTestUtils.flattenMap(
                        getSharedStateFor(MessagingTestConstants.SharedStateName.EVENT_HUB, 1000));
        assertEquals(
                Messaging.extensionVersion(),
                sharedStateMap.get("extensions.com.adobe.messaging.version"));
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
        String actualMsgId =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
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
        String actualMsgId =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
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
        String actualMsgId =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
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
        String actualMsgId =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        String actualXdmData =
                intent.getStringExtra(
                        MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
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
        String expectedMessagingEventData =
                "{messageId=mockMessageId, actionId=mockCustomActionId,"
                    + " eventType=pushTracking.customAction, applicationOpened=true, adobe_xdm={\n"
                    + "            cjm ={\n"
                    + "              _experience= {\n"
                    + "                customerJourneyManagement= {\n"
                    + "                  messageExecution= {\n"
                    + "                    messageExecutionID= 16-Sept-postman, \n"
                    + "                    messageID= 567, \n"
                    + "                    journeyVersionID= some-journeyVersionId, \n"
                    + "                    journeyVersionInstanceID= someJourneyVersionInstanceID\n"
                    + "                  }\n"
                    + "                }\n"
                    + "              }\n"
                    + "            }\n"
                    + "          }}";
        String expectedEdgeEventData =
                "{xdm={pushNotificationTracking={customAction={actionID=mockCustomActionId},"
                    + " pushProviderMessageID=mockMessageId, pushProvider=fcm},"
                    + " application={launches={value=1}}, eventType=pushTracking.customAction,"
                    + " _experience={customerJourneyManagement={pushChannelContext={platform=fcm},"
                    + " messageExecution={messageExecutionID=16-Sept-postman,"
                    + " journeyVersionInstanceID=someJourneyVersionInstanceID, messageID=567,"
                    + " journeyVersionID=some-journeyVersionId},"
                    + " messageProfile={channel={_id=https://ns.adobe.com/xdm/channels/push}}}}},"
                    + " meta={collect={datasetId=somedatasetid}}}";

        // test
        Messaging.handleNotificationResponse(intent, true, mockCustomActionId);

        // verify messaging event
        List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(
                expectedMessagingEventData,
                messagingRequestEvents.get(0).getEventData().toString());

        // verify push tracking edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgeRequestEvents.size());
        assertEquals(expectedEdgeEventData, edgeRequestEvents.get(0).getEventData().toString());
    }

    @Test
    public void testHandleNotificationResponse_noCustomActionId() throws InterruptedException {
        // Parameters
        Intent intent = getResponseIntent();
        String expectedMessagingEventData =
                "{messageId=mockMessageId, eventType=pushTracking.applicationOpened,"
                    + " applicationOpened=true, adobe_xdm={\n"
                    + "            cjm ={\n"
                    + "              _experience= {\n"
                    + "                customerJourneyManagement= {\n"
                    + "                  messageExecution= {\n"
                    + "                    messageExecutionID= 16-Sept-postman, \n"
                    + "                    messageID= 567, \n"
                    + "                    journeyVersionID= some-journeyVersionId, \n"
                    + "                    journeyVersionInstanceID= someJourneyVersionInstanceID\n"
                    + "                  }\n"
                    + "                }\n"
                    + "              }\n"
                    + "            }\n"
                    + "          }}";
        String expectedEdgeEventData =
                "{xdm={pushNotificationTracking={pushProviderMessageID=mockMessageId,"
                    + " pushProvider=fcm}, application={launches={value=1}},"
                    + " eventType=pushTracking.applicationOpened,"
                    + " _experience={customerJourneyManagement={pushChannelContext={platform=fcm},"
                    + " messageExecution={messageExecutionID=16-Sept-postman,"
                    + " journeyVersionInstanceID=someJourneyVersionInstanceID, messageID=567,"
                    + " journeyVersionID=some-journeyVersionId},"
                    + " messageProfile={channel={_id=https://ns.adobe.com/xdm/channels/push}}}}},"
                    + " meta={collect={datasetId=somedatasetid}}}";

        // test
        Messaging.handleNotificationResponse(intent, true, null);

        // verify messaging event
        List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(
                expectedMessagingEventData,
                messagingRequestEvents.get(0).getEventData().toString());

        // verify push tracking edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgeRequestEvents.size());
        assertEquals(expectedEdgeEventData, edgeRequestEvents.get(0).getEventData().toString());
    }

    @Test
    public void testHandleNotificationResponse_noIntent() throws InterruptedException {
        // test
        Messaging.handleNotificationResponse(null, true, "customAction");

        // verify messaging event
        List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());

        // verify no push tracking edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(0, edgeRequestEvents.size());
    }

    @Test
    public void testHandleNotificationResponse_noMessageId() throws InterruptedException {
        // test
        Messaging.handleNotificationResponse(new Intent(), true, "customAction");

        // verify messaging event
        List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());

        // verify no push tracking edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(0, edgeRequestEvents.size());
    }

    @Test
    public void testHandleNotificationResponse_noXdmData() throws InterruptedException {
        // Parameters
        Intent intent = new Intent();
        intent.putExtra(
                MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID,
                "mockMessageId");

        // test
        CountDownLatch latch = new CountDownLatch(1);
        final PushTrackingStatus[] trackingStatus = new PushTrackingStatus[1];
        Messaging.handleNotificationResponse(
                intent,
                true,
                null,
                pushTrackingStatus -> {
                    trackingStatus[0] = pushTrackingStatus;
                    latch.countDown();
                });

        // verify callback called with error status
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(PushTrackingStatus.NO_TRACKING_DATA, trackingStatus[0]);

        // verify no messaging request event is sent
        List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());

        // verify no push tracking edge is sent
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(0, edgeRequestEvents.size());
    }

    @Test
    public void testHandleNotificationResponse_emptyDatasetId() throws InterruptedException {
        // update config
        HashMap<String, Object> config =
                new HashMap<String, Object>() {
                    {
                        put("messaging.eventDataset", "");
                    }
                };
        MobileCore.updateConfiguration(config);

        TestHelper.waitForThreads(1000);

        // Parameters
        Intent intent = getResponseIntent();
        String mockCustomActionId = "mockCustomActionId";
        String expectedMessagingEventData =
                "{messageId=mockMessageId, actionId=mockCustomActionId,"
                    + " eventType=pushTracking.customAction, applicationOpened=true, adobe_xdm={\n"
                    + "            cjm ={\n"
                    + "              _experience= {\n"
                    + "                customerJourneyManagement= {\n"
                    + "                  messageExecution= {\n"
                    + "                    messageExecutionID= 16-Sept-postman, \n"
                    + "                    messageID= 567, \n"
                    + "                    journeyVersionID= some-journeyVersionId, \n"
                    + "                    journeyVersionInstanceID= someJourneyVersionInstanceID\n"
                    + "                  }\n"
                    + "                }\n"
                    + "              }\n"
                    + "            }\n"
                    + "          }}";

        // test
        Messaging.handleNotificationResponse(intent, true, mockCustomActionId);

        // verify messaging event
        List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        assertEquals(
                expectedMessagingEventData,
                messagingRequestEvents.get(0).getEventData().toString());

        // verify no push tracking edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(0, edgeRequestEvents.size());
    }

    // --------------------------------------------------------------------------------------------
    // Tests for Messaging.refreshInAppMessages API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testRefreshInAppMessages() throws InterruptedException {
        // test
        Messaging.refreshInAppMessages();
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("refreshmessages"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(1, surfacesList.size());
        assertEquals("mobileapp://com.adobe.marketing.mobile.messaging.test", surfacesList.get(0));
    }

    // --------------------------------------------------------------------------------------------
    // Tests for Messaging.updatePropositionsForSurfacePaths API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testUpdatePropositionsForSurfacePaths() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(new Surface("promos/feed1"));
        surfacePaths.add(new Surface("promos/feed2"));
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // test
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));
    }

    @Test
    public void testUpdatePropositionsForSurfacePaths_somePathsInvalid()
            throws InterruptedException {
        // setup
        MonitorExtension.reset();
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(new Surface("promos/feed1"));
        surfacePaths.add(new Surface("##invalid##"));
        surfacePaths.add(new Surface("##alsoinvalid!"));
        surfacePaths.add(new Surface("promos/feed2"));
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // test
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));
    }

    @Test
    public void testUpdatePropositionsForSurfacePaths_emptyPaths() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        // test
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify no messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());
    }

    // --------------------------------------------------------------------------------------------
    // Tests for Messaging.getPropositionsForSurfaces API
    // --------------------------------------------------------------------------------------------
    @Test
    public void testGetPropositionsForSurfaces() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(Surface.fromUriString("mobileapp://mockPackageName/apifeed"));
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put("uri", "mobileapp://mockPackageName/apifeed");
                    }
                });

        // test
        final CountDownLatch latch = new CountDownLatch(1);
        final AdobeError[] responseError = new AdobeError[1];
        final List<Proposition>[] retrievedPropositions = new List[1];
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        responseError[0] = adobeError;
                        latch.countDown();
                    }

                    @Override
                    public void call(final Map<Surface, List<Proposition>> surfaceListMap) {
                        for (final List<Proposition> propositions : surfaceListMap.values()) {
                            retrievedPropositions[0] = propositions;
                            latch.countDown();
                        }
                    }
                });
        latch.await(1, TimeUnit.SECONDS);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("getpropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));
    }

    @Test
    public void testGetPropositionsForSurfaces_AdobeErrorOccurred() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(Surface.fromUriString("mobileapp://mockPackageName/apifeed"));
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put("uri", "mobileapp://mockPackageName/apifeed");
                    }
                });

        // test
        final CountDownLatch latch = new CountDownLatch(1);
        final AdobeError[] responseError = new AdobeError[1];
        final List<Proposition>[] retrievedPropositions = new List[1];
        final AdobeCallbackWithError callback =
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        responseError[0] = adobeError;
                        latch.countDown();
                    }

                    @Override
                    public void call(final Map<Surface, List<Proposition>> surfaceListMap) {
                        for (final List<Proposition> propositions : surfaceListMap.values()) {
                            retrievedPropositions[0] = propositions;
                            latch.countDown();
                        }
                    }
                };
        Messaging.getPropositionsForSurfaces(surfacePaths, callback);
        callback.fail(AdobeError.UNEXPECTED_ERROR);
        latch.await(1, TimeUnit.SECONDS);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("getpropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify adobe error returned
        assertNotNull(responseError[0]);
        assertEquals(AdobeError.UNEXPECTED_ERROR, responseError[0]);
    }

    @Test
    public void testGetPropositionsForSurfaces_someInvalidSurfaces() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(Surface.fromUriString("mobileapp://mockPackageName/apifeed"));
        surfacePaths.add(new Surface("newcontentcard"));
        surfacePaths.add(new Surface("##invalid##"));
        final List<Map<String, Object>> expectedSurfaces = getExpectedSurfaces();

        // test
        final CountDownLatch latch = new CountDownLatch(1);
        final AdobeError[] responseError = new AdobeError[1];
        final List<Proposition>[] retrievedPropositions = new List[1];
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        responseError[0] = adobeError;
                        latch.countDown();
                    }

                    @Override
                    public void call(final Map<Surface, List<Proposition>> surfaceListMap) {
                        for (final List<Proposition> propositions : surfaceListMap.values()) {
                            retrievedPropositions[0] = propositions;
                            latch.countDown();
                        }
                    }
                });
        latch.await(1, TimeUnit.SECONDS);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("getpropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));
    }

    @Test
    public void testGetPropositionsForSurfaces_noValidSurfaces() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(new Surface("##invalid##"));
        // test
        final CountDownLatch latch = new CountDownLatch(1);
        final AdobeError[] responseError = new AdobeError[1];
        final List<Proposition>[] retrievedPropositions = new List[1];
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        responseError[0] = adobeError;
                        latch.countDown();
                    }

                    @Override
                    public void call(final Map<Surface, List<Proposition>> surfaceListMap) {
                        for (final List<Proposition> propositions : surfaceListMap.values()) {
                            retrievedPropositions[0] = propositions;
                            latch.countDown();
                        }
                    }
                });
        latch.await(1, TimeUnit.SECONDS);

        // verify no messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());
    }

    @Test
    public void testGetPropositionsForSurfaces_emptySurfacesListProvided()
            throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        // test
        final CountDownLatch latch = new CountDownLatch(1);
        final AdobeError[] responseError = new AdobeError[1];
        final List<Proposition>[] retrievedPropositions = new List[1];
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        responseError[0] = adobeError;
                        latch.countDown();
                    }

                    @Override
                    public void call(final Map<Surface, List<Proposition>> surfaceListMap) {
                        for (final List<Proposition> propositions : surfaceListMap.values()) {
                            retrievedPropositions[0] = propositions;
                            latch.countDown();
                        }
                    }
                });
        latch.await(1, TimeUnit.SECONDS);

        // verify no messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());
    }

    @Test
    public void testGetPropositionsForSurfaces_nullCallback() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        surfacePaths.add(new Surface("newcontentcard"));
        // test
        final List<Proposition>[] retrievedPropositions = new List[1];
        Messaging.getPropositionsForSurfaces(surfacePaths, null);

        // verify no messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(0, messagingRequestEvents.size());
    }

    // --------------------------------------------------------------------------------------------
    // Content Card Tests
    // --------------------------------------------------------------------------------------------
    @Test
    public void testContentCard_Qualification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        List<TestableNetworkRequest> edgeRequestList = null;
                        try {
                            edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));

        // dispatch content card qualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        assertEquals(SchemaType.CONTENT_CARD, contentCardList.get(0).getItems().get(0).getSchema());
    }

    @Test
    public void testContentCard_Unqualification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        List<TestableNetworkRequest> edgeRequestList = null;
                        try {
                            edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));

        // dispatch content card qualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch1 = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch1.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch1.countDown();
                    }
                });
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        assertEquals(SchemaType.CONTENT_CARD, contentCardList.get(0).getItems().get(0).getSchema());

        // dispatch content card unqualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "exited");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch2 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch2.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch2.countDown();
                    }
                });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());
    }

    @Test
    public void testContentCard_Requalification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        List<TestableNetworkRequest> edgeRequestList = null;
                        try {
                            edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));

        // dispatch content card qualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch1 = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch1.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch1.countDown();
                    }
                });
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        assertEquals(SchemaType.CONTENT_CARD, contentCardList.get(0).getItems().get(0).getSchema());

        // dispatch content card unqualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "exited");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch2 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch2.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch2.countDown();
                    }
                });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());

        // dispatch content card qualification event again
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch3 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch3.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch3.countDown();
                    }
                });
        assertTrue(latch3.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardListNew = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardListNew);
        assertEquals(1, contentCardListNew.size());
        assertEquals(
                SchemaType.CONTENT_CARD, contentCardListNew.get(0).getItems().get(0).getSchema());
    }

    @Test
    public void testContentCard_Disqualification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        List<TestableNetworkRequest> edgeRequestList = null;
                        try {
                            edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));

        // dispatch content card qualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch1 = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch1.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch1.countDown();
                    }
                });
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        PropositionItem propositionItem = contentCardList.get(0).getItems().get(0);
        assertEquals(SchemaType.CONTENT_CARD, propositionItem.getSchema());

        // dispatch content card disqualification event
        propositionItem.track(MessagingEdgeEventType.DISMISS);
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch2 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch2.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch2.countDown();
                    }
                });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());

        // dispatch content card qualification event again
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "entered");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch3 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch3.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch3.countDown();
                    }
                });
        assertTrue(latch3.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());
    }

    @Test
    public void testContentCardWithoutTriggers_Qualification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        try {
                            List<TestableNetworkRequest> edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithoutTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content events
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(2, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));
        final Map<String, Object> edgeTriggerEventData =
                edgePersonalizationRequestEvents.get(1).getEventData();
        final Map<String, Object> xdmTriggerDataMap =
                DataReader.optTypedMap(Object.class, edgeTriggerEventData, "xdm", null);
        assertEquals(MessagingEdgeEventType.TRIGGER.toString(), xdmTriggerDataMap.get("eventType"));
        final Map<String, Object> experienceDataMap =
                DataReader.optTypedMap(Object.class, xdmTriggerDataMap, "_experience", null);
        final Map<String, Object> decisioningDataMap =
                DataReader.optTypedMap(Object.class, experienceDataMap, "decisioning", null);
        final List<Map<String, Object>> propositionsDataMap =
                DataReader.optTypedListOfMap(
                        Object.class, decisioningDataMap, "propositions", null);
        assertNotNull(propositionsDataMap);
        assertEquals(surface1.getUri(), propositionsDataMap.get(0).get("scope"));

        // retrieve qualified content cards
        CountDownLatch latch = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        assertEquals(SchemaType.CONTENT_CARD, contentCardList.get(0).getItems().get(0).getSchema());
    }

    @Test
    public void testContentCardWithoutTriggers_Unqualification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        try {
                            List<TestableNetworkRequest> edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithoutTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content events
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(2, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));
        final Map<String, Object> edgeTriggerEventData =
                edgePersonalizationRequestEvents.get(1).getEventData();
        final Map<String, Object> xdmTriggerDataMap =
                DataReader.optTypedMap(Object.class, edgeTriggerEventData, "xdm", null);
        assertEquals(MessagingEdgeEventType.TRIGGER.toString(), xdmTriggerDataMap.get("eventType"));
        final Map<String, Object> experienceDataMap =
                DataReader.optTypedMap(Object.class, xdmTriggerDataMap, "_experience", null);
        final Map<String, Object> decisioningDataMap =
                DataReader.optTypedMap(Object.class, experienceDataMap, "decisioning", null);
        final List<Map<String, Object>> propositionsDataMap =
                DataReader.optTypedListOfMap(
                        Object.class, decisioningDataMap, "propositions", null);
        assertNotNull(propositionsDataMap);
        assertEquals(surface1.getUri(), propositionsDataMap.get(0).get("scope"));

        // retrieve qualified content cards
        CountDownLatch latch = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        assertEquals(SchemaType.CONTENT_CARD, contentCardList.get(0).getItems().get(0).getSchema());

        // dispatch content card unqualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "exited");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch2 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch2.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch2.countDown();
                    }
                });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());
    }

    // TODO: Clarify behavior for this case
    @Test
    public void testContentCardWithoutTriggers_Requalification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        try {
                            List<TestableNetworkRequest> edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithoutTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content events
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(2, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));
        final Map<String, Object> edgeTriggerEventData =
                edgePersonalizationRequestEvents.get(1).getEventData();
        final Map<String, Object> xdmTriggerDataMap =
                DataReader.optTypedMap(Object.class, edgeTriggerEventData, "xdm", null);
        assertEquals(MessagingEdgeEventType.TRIGGER.toString(), xdmTriggerDataMap.get("eventType"));
        final Map<String, Object> experienceDataMap =
                DataReader.optTypedMap(Object.class, xdmTriggerDataMap, "_experience", null);
        final Map<String, Object> decisioningDataMap =
                DataReader.optTypedMap(Object.class, experienceDataMap, "decisioning", null);
        final List<Map<String, Object>> propositionsDataMap =
                DataReader.optTypedListOfMap(
                        Object.class, decisioningDataMap, "propositions", null);
        assertNotNull(propositionsDataMap);
        assertEquals(surface1.getUri(), propositionsDataMap.get(0).get("scope"));

        // retrieve qualified content cards
        CountDownLatch latch = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        assertEquals(SchemaType.CONTENT_CARD, contentCardList.get(0).getItems().get(0).getSchema());

        // dispatch content card unqualification event
        MobileCore.dispatchEvent(
                new Event.Builder(
                                "Places entry event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                        .setEventData(
                                new HashMap<String, Object>() {
                                    {
                                        put("regionEventType", "exited");
                                    }
                                })
                        .build());
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch2 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch2.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch2.countDown();
                    }
                });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());

        // dispatch any event again
        MobileCore.trackAction("dummyEvent", null);
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch3 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch3.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch3.countDown();
                    }
                });
        assertTrue(latch3.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardListNew = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardListNew);
        assertEquals(1, contentCardListNew.size());
        assertEquals(
                SchemaType.CONTENT_CARD, contentCardListNew.get(0).getItems().get(0).getSchema());
    }

    @Test
    public void testContentCardWithoutTriggers_Disqualification() throws InterruptedException {
        // setup
        final List<Surface> surfacePaths = new ArrayList<>();
        Surface surface1 = new Surface("promos/feed1");
        surfacePaths.add(surface1);
        Surface surface2 = new Surface("promos/feed2");
        surfacePaths.add(surface2);
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2");
                    }
                });

        // setup mock server response for content card propositions
        String edgeRequestUrl = "https://edge.adobedc.net/ee/v1/interact";
        TestHelper.setNetworkResponseFor(
                edgeRequestUrl,
                HttpMethod.POST,
                new HttpConnecting() {
                    @Override
                    public InputStream getInputStream() {
                        try {
                            List<TestableNetworkRequest> edgeRequestList =
                                    TestHelper.getNetworkRequestsWith(
                                            edgeRequestUrl, HttpMethod.POST);

                            String requestId = edgeRequestList.get(0).queryParam("requestId");
                            String response =
                                    MessagingTestUtils.loadStringFromFile(
                                            "contentCardWithoutTriggersNetworkResponse.json");
                            if (response != null) {
                                String replacedResponse =
                                        "\u0000"
                                                + response.replace("mockRequestId", requestId)
                                                        .replaceAll("[\\r\\n\\t]+", "");
                                return new ByteArrayInputStream(replacedResponse.getBytes());
                            } else {
                                return new ByteArrayInputStream("".getBytes());
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public InputStream getErrorStream() {
                        return null;
                    }

                    @Override
                    public int getResponseCode() {
                        return 200;
                    }

                    @Override
                    public String getResponseMessage() {
                        return "";
                    }

                    @Override
                    public String getResponsePropertyValue(String responsePropertyKey) {
                        return null;
                    }

                    @Override
                    public void close() {}
                });

        // test retrieving propositions from server
        Messaging.updatePropositionsForSurfaces(surfacePaths);
        TestHelper.sleep(500);

        // verify messaging request content event
        final List<Event> messagingRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Map<String, Object> messagingEventData = messagingRequestEvents.get(0).getEventData();
        assertEquals(true, messagingEventData.get("updatepropositions"));
        assertEquals(expectedSurfaces, messagingEventData.get("surfaces"));

        // verify edge request content events
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(2, edgePersonalizationRequestEvents.size());
        final Map<String, Object> edgeEventData =
                edgePersonalizationRequestEvents.get(0).getEventData();
        final Map<String, Object> xdmDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "xdm", null);
        final Map<String, Object> queryDataMap =
                DataReader.optTypedMap(Object.class, edgeEventData, "query", null);
        final Map<String, Object> personalizationDataMap =
                DataReader.optTypedMap(Object.class, queryDataMap, "personalization", null);
        final List<String> surfacesList =
                DataReader.optStringList(personalizationDataMap, "surfaces", null);
        assertEquals("personalization.request", xdmDataMap.get("eventType"));
        assertEquals(2, surfacesList.size());
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed1",
                surfacesList.get(0));
        assertEquals(
                "mobileapp://com.adobe.marketing.mobile.messaging.test/promos/feed2",
                surfacesList.get(1));
        final Map<String, Object> edgeTriggerEventData =
                edgePersonalizationRequestEvents.get(1).getEventData();
        final Map<String, Object> xdmTriggerDataMap =
                DataReader.optTypedMap(Object.class, edgeTriggerEventData, "xdm", null);
        assertEquals(MessagingEdgeEventType.TRIGGER.toString(), xdmTriggerDataMap.get("eventType"));
        final Map<String, Object> experienceDataMap =
                DataReader.optTypedMap(Object.class, xdmTriggerDataMap, "_experience", null);
        final Map<String, Object> decisioningDataMap =
                DataReader.optTypedMap(Object.class, experienceDataMap, "decisioning", null);
        final List<Map<String, Object>> propositionsDataMap =
                DataReader.optTypedListOfMap(
                        Object.class, decisioningDataMap, "propositions", null);
        assertNotNull(propositionsDataMap);
        assertEquals(surface1.getUri(), propositionsDataMap.get(0).get("scope"));

        // retrieve qualified content cards
        CountDownLatch latch = new CountDownLatch(1);
        final Map<Surface, List<Proposition>> qualifiedCardPropositions = new HashMap<>();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch.countDown();
                    }
                });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, qualifiedCardPropositions.size());
        List<Proposition> contentCardList = qualifiedCardPropositions.get(surface1);
        assertNotNull(contentCardList);
        assertEquals(1, contentCardList.size());
        PropositionItem propositionItem = contentCardList.get(0).getItems().get(0);
        assertEquals(SchemaType.CONTENT_CARD, propositionItem.getSchema());

        // dispatch content card disqualification event
        propositionItem.track(MessagingEdgeEventType.DISMISS);
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch2 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch2.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch2.countDown();
                    }
                });
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());

        // dispatch any event again
        MobileCore.trackAction("dummyEvent", null);
        TestHelper.sleep(500);

        // retrieve qualified content cards
        CountDownLatch latch3 = new CountDownLatch(1);
        qualifiedCardPropositions.clear();
        Messaging.getPropositionsForSurfaces(
                surfacePaths,
                new AdobeCallbackWithError<Map<Surface, List<Proposition>>>() {
                    @Override
                    public void fail(AdobeError adobeError) {
                        latch3.countDown();
                    }

                    @Override
                    public void call(Map<Surface, List<Proposition>> surfaceListMap) {
                        qualifiedCardPropositions.putAll(surfaceListMap);
                        latch3.countDown();
                    }
                });
        assertTrue(latch3.await(1, TimeUnit.SECONDS));
        assertEquals(0, qualifiedCardPropositions.size());
    }

    // --------------------------------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------------------------------
    private List<Map<String, Object>> getExpectedSurfaces() {
        final List<Map<String, Object>> expectedSurfaces = new ArrayList<>();
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put("uri", "mobileapp://mockPackageName/apifeed");
                    }
                });
        expectedSurfaces.add(
                new HashMap<String, Object>() {
                    {
                        put(
                                "uri",
                                "mobileapp://com.adobe.marketing.mobile.messaging.test/newcontentcard");
                    }
                });
        return expectedSurfaces;
    }

    private Intent getResponseIntent() {
        Intent intent = new Intent();
        intent.putExtra(
                MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID,
                "mockMessageId");
        intent.putExtra(
                MessagingTestConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM,
                "{\n"
                    + "            cjm ={\n"
                    + "              _experience= {\n"
                    + "                customerJourneyManagement= {\n"
                    + "                  messageExecution= {\n"
                    + "                    messageExecutionID= 16-Sept-postman, \n"
                    + "                    messageID= 567, \n"
                    + "                    journeyVersionID= some-journeyVersionId, \n"
                    + "                    journeyVersionInstanceID= someJourneyVersionInstanceID\n"
                    + "                  }\n"
                    + "                }\n"
                    + "              }\n"
                    + "            }\n"
                    + "          }");
        return intent;
    }
}
