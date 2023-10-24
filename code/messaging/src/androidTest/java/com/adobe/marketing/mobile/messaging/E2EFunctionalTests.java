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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.AJO;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.INAPP_RESPONSE_FORMAT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.NAMESPACE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.Value.NEW_IAM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.QUERY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SURFACES;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.REQUEST;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.SEND_COMPLETION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.util.TestHelper.resetTestExpectations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SDKHelper;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.util.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class E2EFunctionalTests {
    static {
        BuildConfig.IS_E2E_TEST.set(true);
        BuildConfig.IS_FUNCTIONAL_TEST.set(false);
    }

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // Setup and teardown
    // --------------------------------------------------------------------------------------------
    @Before
    public void setup() throws Exception {
        MessagingTestUtils.cleanCache();
        MessagingTestUtils.setEdgeIdentityPersistence(MessagingTestUtils.createIdentityMap("ECID", "80195814545200720557089495418993853789"), TestHelper.getDefaultApplication());

        Messaging.registerExtension();
        Identity.registerExtension();
        Edge.registerExtension();

        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start((AdobeCallback) o -> {
            Map<String, Object> testConfig = MessagingTestUtils.getMapFromFile("functionalTestConfigStage.json");
            MobileCore.updateConfiguration(testConfig);
            // wait for configuration to be set
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                fail(interruptedException.getMessage());
            }
            latch.countDown();
        });

        latch.await();
        resetTestExpectations();
    }

    @After
    public void tearDown() {
        // clear loaded rules
        SDKHelper.resetSDK();

    }

    Map<String, Object> createExpectedEdgePersonalizationEventData() {
        final Map<String, Object> expectedEdgePersonalizationEventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();
        personalizationData.put(SURFACES, new ArrayList<String>() {{ add("mobileapp://com.adobe.marketing.mobile.messaging.test"); }});
        messageRequestData.put(PERSONALIZATION, personalizationData);
        expectedEdgePersonalizationEventData.put(QUERY, messageRequestData);

        final Map<String, Object> xdmData = new HashMap<String, Object>() {
            {
                put(EVENT_TYPE, PERSONALIZATION_REQUEST);
            }
        };
        expectedEdgePersonalizationEventData.put(XDM, xdmData);

        final Map<String, Object> data = new HashMap<>();
        final Map<String, Object> ajo = new HashMap<>();
        final Map<String, Object> inAppResponseFormat = new HashMap<>();
        inAppResponseFormat.put(INAPP_RESPONSE_FORMAT, NEW_IAM);
        ajo.put(AJO, inAppResponseFormat);
        data.put(NAMESPACE, ajo);
        expectedEdgePersonalizationEventData.put(DATA, data);

        final Map<String, Object> request = new HashMap<>();
        request.put(SEND_COMPLETION, true);
        expectedEdgePersonalizationEventData.put(REQUEST, request);

        return expectedEdgePersonalizationEventData;
    }

    @Test
    public void testGetInAppMessageDefinitionFromEdge() throws InterruptedException {
        // setup
        Map<String, Object> expectedEdgePersonalizationEventData = createExpectedEdgePersonalizationEventData();

        // test
        Messaging.refreshInAppMessages();

        // verify messaging request content event from refreshInAppMessages API call
        final List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT);
        assertEquals(1, messagingRequestEvents.size());
        final Event messagingRequestEvent = messagingRequestEvents.get(0);
        assertEquals(true, messagingRequestEvent.getEventData().get("refreshmessages"));

        // verify edge personalization request content event
        final List<Event> edgePersonalizationRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE,
                EventSource.REQUEST_CONTENT);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Event edgePersonalizationRequestEvent = edgePersonalizationRequestEvents.get(0);
        assertEquals(expectedEdgePersonalizationEventData, edgePersonalizationRequestEvent.getEventData());

        // TODO: restore after creating a valid prod in-app message campaign
        // verify edge personalization decision event
//        final List<Event> edgePersonalizationDecisionsEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE, MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS);
//        assertEquals(1, edgePersonalizationDecisionsEvents.size()); // refreshInAppMessage api
//        final Event edgePersonalizationDecisionEvent = edgePersonalizationDecisionsEvents.get(0);
//        assertNotNull(edgePersonalizationDecisionEvent.getEventData());
    }
}
