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
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.QUERY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCHEMAS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SURFACES;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.REQUEST;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.SEND_COMPLETION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY;
import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SDKHelper;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class E2EFunctionalTests {
    static {
        BuildConfig.IS_E2E_TEST.set(true);
        BuildConfig.IS_FUNCTIONAL_TEST.set(false);
    }

    @Rule
    public RuleChain rule =
            RuleChain.outerRule(new TestHelper.SetupCoreRuleWithRealNetworkService())
                    .around(new TestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // Setup and teardown
    // --------------------------------------------------------------------------------------------
    @Before
    public void setup() throws Exception {
        MessagingTestUtils.cleanCache();
        MessagingTestUtils.setEdgeIdentityPersistence(
                MessagingTestUtils.createIdentityMap(
                        "ECID", "80195814545200720557089495418993853789"),
                TestHelper.getDefaultApplication());

        final CountDownLatch latch = new CountDownLatch(1);
        final List<Class<? extends Extension>> extensions =
                new ArrayList<Class<? extends Extension>>() {
                    {
                        add(Messaging.EXTENSION);
                        add(Identity.EXTENSION);
                        add(Edge.EXTENSION);
                    }
                };

        MobileCore.registerExtensions(
                extensions,
                o -> {
                    // tag: android messaging functional test app, org: AEM Assets Departmental, Prod VA7
                    MobileCore.configureWithAppID("3149c49c3910/473386a6e5b0/launch-6099493a8c97-development");
                    // wait for configuration to be set
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interruptedException) {
                        fail(interruptedException.getMessage());
                    }
                    latch.countDown();
                });

        latch.await();
    }

    @After
    public void tearDown() {
        // clear loaded rules
        SDKHelper.resetSDK();
        // clear received events
        MonitorExtension.reset();
    }

    Map<String, Object> createExpectedEdgePersonalizationEventData() {
        final Map<String, Object> expectedEdgePersonalizationEventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();
        personalizationData.put(
                SURFACES,
                new ArrayList<String>() {
                    {
                        add("mobileapp://com.adobe.marketing.mobile.messaging.test");
                    }
                });
        personalizationData.put(SCHEMAS,
                new ArrayList<String>() {
                    {
                        add(MessagingConstants.SchemaValues.SCHEMA_HTML_CONTENT);
                        add(MessagingConstants.SchemaValues.SCHEMA_JSON_CONTENT);
                        add(MessagingConstants.SchemaValues.SCHEMA_RULESET_ITEM);
                    }
                });
        messageRequestData.put(PERSONALIZATION, personalizationData);
        expectedEdgePersonalizationEventData.put(QUERY, messageRequestData);

        final Map<String, Object> xdmData =
                new HashMap<String, Object>() {
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
        // test
        verifyInAppPropositionsRetrievedFromEdge();

        // trigger an in-app message
        MobileCore.trackAction("functional", null);

        // verify show always rule consequence event is dispatched
        final Map<String, Object> expectedRulesConsequenceEventData = (Map<String, Object>) ((List) MessagingTestUtils.getMapFromFile("iam-show-always-consequence.json").get(JSON_CONSEQUENCES_KEY)).get(0);
        final List<Event> rulesConsequenceEvents = getDispatchedEventsWith(EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT);
        assertEquals(1, rulesConsequenceEvents.size());
        final Event rulesConsequenceEvent = rulesConsequenceEvents.get(0);
        final Map<String, Object> triggeredConsequenceEventData = (Map<String, Object>) rulesConsequenceEvent.getEventData().get(CONSEQUENCE_TRIGGERED);
        final Map<String, Object> expectedRuleConsequenceDetails = (Map<String, Object>) expectedRulesConsequenceEventData.get("detail");
        final Map<String, Object> triggeredRuleConsequenceDetails = (Map<String, Object>) triggeredConsequenceEventData.get("detail");
        assertEquals(expectedRuleConsequenceDetails.get("schema"), triggeredRuleConsequenceDetails.get("schema"));
        assertEquals(expectedRuleConsequenceDetails.get("data"), triggeredRuleConsequenceDetails.get("data"));
        assertEquals(expectedRulesConsequenceEventData.get("type"), triggeredConsequenceEventData.get("type"));
    }

    private void verifyInAppPropositionsRetrievedFromEdge() throws InterruptedException {
        final Map<String, Object> expectedEdgePersonalizationEventData =
                createExpectedEdgePersonalizationEventData();
        // verify message personalization request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(EventType.EDGE, EventSource.REQUEST_CONTENT, 5000);
        assertEquals(1, edgePersonalizationRequestEvents.size());
        final Event edgePersonalizationRequestEvent = edgePersonalizationRequestEvents.get(0);
        assertEquals(
                expectedEdgePersonalizationEventData,
                edgePersonalizationRequestEvent.getEventData());
        final String edgePersonalizationRequestEventID = edgePersonalizationRequestEvent.getUniqueIdentifier();

        // verify personalization decisions event containing two in-app propositions
        final List<Event> messagingPersonalizationEvents = getDispatchedEventsWith(EventType.EDGE, MessagingConstants.EventSource.PERSONALIZATION_DECISIONS, 3000);
        final Event messagingPersonalizationEvent = messagingPersonalizationEvents.get(0);
        assertEquals(2, ((List) messagingPersonalizationEvent.getEventData().get("payload")).size());
        final Map<String, Object> expectedInAppPayload = MessagingTestUtils.getMapFromFile("expectedInAppPayload.json");
        final Map<String, Object> inAppProposition1 = (Map<String, Object>) ((List) messagingPersonalizationEvent.getEventData().get("payload")).get(0);
        final Map<String, Object> inAppProposition2 = (Map<String, Object>) ((List) messagingPersonalizationEvent.getEventData().get("payload")).get(1);
        final Map<String, Object> expectedProposition1 = (Map<String, Object>) ((List) expectedInAppPayload.get("payload")).get(0);
        final Map<String, Object> expectedProposition2 = (Map<String, Object>) ((List) expectedInAppPayload.get("payload")).get(1);
        final Map<String, Object> expectedScopeDetails1 = (Map<String, Object>) expectedProposition1.get("scopeDetails");
        final Map<String, Object> expectedScopeDetails2 = (Map<String, Object>) expectedProposition2.get("scopeDetails");
        final Map<String, Object> scopeDetails1 = (Map<String, Object>) inAppProposition1.get("scopeDetails");
        final Map<String, Object> scopeDetails2 = (Map<String, Object>) inAppProposition2.get("scopeDetails");
        assertEquals(expectedScopeDetails1.get("activity"), scopeDetails1.get("activity"));
        assertEquals(expectedScopeDetails1.get("correlationID"), scopeDetails1.get("correlationID"));
        assertEquals(expectedScopeDetails1.get("decisionProvider"), scopeDetails1.get("decisionProvider"));
        assertEquals(expectedProposition1.get("scope"), inAppProposition1.get("scope"));
        assertEquals(expectedScopeDetails2.get("activity"), scopeDetails2.get("activity"));
        assertEquals(expectedScopeDetails2.get("correlationID"), scopeDetails2.get("correlationID"));
        assertEquals(expectedScopeDetails2.get("decisionProvider"), scopeDetails2.get("decisionProvider"));
        assertEquals(expectedProposition2.get("scope"), inAppProposition2.get("scope"));

        // verify edge content complete event
        final List<Event> edgeContentCompleteEvents = getDispatchedEventsWith(EventType.EDGE, EventSource.CONTENT_COMPLETE);
        assertEquals(1, edgeContentCompleteEvents.size());
        final Event edgeContentCompleteEvent = edgeContentCompleteEvents.get(0);
        assertEquals(edgePersonalizationRequestEventID, edgeContentCompleteEvent.getParentID());
    }
}
