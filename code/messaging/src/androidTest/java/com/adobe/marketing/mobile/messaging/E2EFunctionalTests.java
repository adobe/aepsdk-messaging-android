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
import com.adobe.marketing.mobile.util.TestRetryRule;
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

    // A test will be retried at most 3 times
    @Rule public TestRetryRule totalTestCount = new TestRetryRule(3);

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
                    MobileCore.configureWithAppID(Environment.getAppId());
                    MobileCore.updateConfiguration(Environment.configurationUpdates());
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
        personalizationData.put(
                SCHEMAS,
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
    public void testIamTriggerShowAlways() throws InterruptedException {
        // test
        verifyInAppPropositionsRetrievedFromEdge();

        // trigger a show always in-app message
        MobileCore.trackAction("always", null);

        // verify show always rule consequence event is dispatched
        final Map<String, Object> expectedRulesConsequenceEventData =
                (Map<String, Object>) (getExpectedRulesConsequenceDataForEnvironment(false).get(0));
        List<Event> rulesConsequenceEvents =
                getDispatchedEventsWith(EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT);
        assertEquals(
                "show always rule consequence failed to be dispatched.",
                1,
                rulesConsequenceEvents.size());
        Event rulesConsequenceEvent = rulesConsequenceEvents.get(0);
        Map<String, Object> triggeredConsequenceEventData =
                (Map<String, Object>)
                        rulesConsequenceEvent.getEventData().get(CONSEQUENCE_TRIGGERED);
        final Map<String, Object> expectedRuleConsequenceDetails =
                (Map<String, Object>) expectedRulesConsequenceEventData.get("detail");
        Map<String, Object> triggeredRuleConsequenceDetails =
                (Map<String, Object>) triggeredConsequenceEventData.get("detail");
        assertEquals(
                expectedRuleConsequenceDetails.get("schema"),
                triggeredRuleConsequenceDetails.get("schema"));
        assertEquals(
                expectedRuleConsequenceDetails.get("data"),
                triggeredRuleConsequenceDetails.get("data"));
        assertEquals(
                expectedRulesConsequenceEventData.get("type"),
                triggeredConsequenceEventData.get("type"));

        // clear received events
        MonitorExtension.reset();

        // trigger the show always in-app message again
        MobileCore.trackAction("always", null);

        // verify rule consequence event is dispatched
        rulesConsequenceEvents =
                getDispatchedEventsWith(EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT);
        assertEquals(
                "show always rule consequence should be dispatched again.",
                1,
                rulesConsequenceEvents.size());
        rulesConsequenceEvent = rulesConsequenceEvents.get(0);
        triggeredConsequenceEventData =
                (Map<String, Object>)
                        rulesConsequenceEvent.getEventData().get(CONSEQUENCE_TRIGGERED);
        triggeredRuleConsequenceDetails =
                (Map<String, Object>) triggeredConsequenceEventData.get("detail");
        assertEquals(
                expectedRuleConsequenceDetails.get("schema"),
                triggeredRuleConsequenceDetails.get("schema"));
        assertEquals(
                expectedRuleConsequenceDetails.get("data"),
                triggeredRuleConsequenceDetails.get("data"));
        assertEquals(
                expectedRulesConsequenceEventData.get("type"),
                triggeredConsequenceEventData.get("type"));
    }

    @Test
    public void testIamTriggerShowOnce() throws InterruptedException {
        // test
        verifyInAppPropositionsRetrievedFromEdge();

        // trigger a show once in-app message
        MobileCore.trackAction("once", null);

        // verify show once rule consequence event is dispatched
        final Map<String, Object> expectedRulesConsequenceEventData =
                (Map<String, Object>) (getExpectedRulesConsequenceDataForEnvironment(true).get(0));
        List<Event> rulesConsequenceEvents =
                getDispatchedEventsWith(EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT);
        assertEquals(
                "show once rule consequence failed to be dispatched.",
                1,
                rulesConsequenceEvents.size());
        final Event rulesConsequenceEvent = rulesConsequenceEvents.get(0);
        final Map<String, Object> triggeredConsequenceEventData =
                (Map<String, Object>)
                        rulesConsequenceEvent.getEventData().get(CONSEQUENCE_TRIGGERED);
        final Map<String, Object> expectedRuleConsequenceDetails =
                (Map<String, Object>) expectedRulesConsequenceEventData.get("detail");
        final Map<String, Object> triggeredRuleConsequenceDetails =
                (Map<String, Object>) triggeredConsequenceEventData.get("detail");
        assertEquals(
                expectedRuleConsequenceDetails.get("schema"),
                triggeredRuleConsequenceDetails.get("schema"));
        assertEquals(
                expectedRuleConsequenceDetails.get("data"),
                triggeredRuleConsequenceDetails.get("data"));
        assertEquals(
                expectedRulesConsequenceEventData.get("type"),
                triggeredConsequenceEventData.get("type"));

        // workaround as the e2e test dispatches an IAM triggered event but not a display event
        final Map<String, String> mockHistoryMap = new HashMap<>();
        mockHistoryMap.put(MessagingConstants.EventMask.Keys.EVENT_TYPE, "display");
        mockHistoryMap.put(
                MessagingConstants.EventMask.Keys.MESSAGE_ID, Environment.getShowOnceMessageId());
        mockHistoryMap.put(MessagingConstants.EventMask.Keys.TRACKING_ACTION, "");
        final Map<String, Object> eventHistoryData = new HashMap<>();
        eventHistoryData.put(MessagingConstants.EventDataKeys.IAM_HISTORY, mockHistoryMap);

        final String[] mask = {
            MessagingConstants.EventMask.Mask.EVENT_TYPE,
            MessagingConstants.EventMask.Mask.MESSAGE_ID,
            MessagingConstants.EventMask.Mask.TRACKING_ACTION
        };
        final Event event =
                new Event.Builder(
                                MessagingConstants.EventName.EVENT_HISTORY_WRITE,
                                MessagingConstants.EventType.MESSAGING,
                                MessagingConstants.EventSource.EVENT_HISTORY_WRITE,
                                mask)
                        .setEventData(eventHistoryData)
                        .build();
        MobileCore.dispatchEvent(event);

        // clear received events
        MonitorExtension.reset();

        // trigger the show once in-app message again
        MobileCore.trackAction("once", null);

        // verify no rule consequence event is dispatched
        rulesConsequenceEvents =
                getDispatchedEventsWith(EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT);
        assertEquals(
                "show once rule consequence shouldn't be dispatched again.",
                0,
                rulesConsequenceEvents.size());
    }

    private void verifyInAppPropositionsRetrievedFromEdge() throws InterruptedException {
        final Map<String, Object> expectedEdgePersonalizationEventData =
                createExpectedEdgePersonalizationEventData();
        // verify message personalization request content event
        final List<Event> edgePersonalizationRequestEvents =
                getDispatchedEventsWith(EventType.EDGE, EventSource.REQUEST_CONTENT, 5000);
        assertEquals(
                "edge personalization request event not received after 5 seconds.",
                1,
                edgePersonalizationRequestEvents.size());
        final Event edgePersonalizationRequestEvent = edgePersonalizationRequestEvents.get(0);
        assertEquals(
                expectedEdgePersonalizationEventData,
                edgePersonalizationRequestEvent.getEventData());
        final String edgePersonalizationRequestEventID =
                edgePersonalizationRequestEvent.getUniqueIdentifier();

        // verify edge content complete event
        final List<Event> edgeContentCompleteEvents =
                getDispatchedEventsWith(EventType.EDGE, EventSource.CONTENT_COMPLETE, 5000);
        assertEquals(
                "edge content complete event not received after 5 seconds.",
                1,
                edgeContentCompleteEvents.size());
        final Event edgeContentCompleteEvent = edgeContentCompleteEvents.get(0);
        assertEquals(edgePersonalizationRequestEventID, edgeContentCompleteEvent.getParentID());
    }

    private List getExpectedRulesConsequenceDataForEnvironment(final boolean isShowOnce) {
        final String environment = Environment.Companion.getBuildConfigEnvironment();
        switch (environment) {
            case "stageVA7":
                return isShowOnce
                        ? (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-once-consequence-stageVA7.json")
                                        .get(JSON_CONSEQUENCES_KEY)
                        : (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-always-consequence-stageVA7.json")
                                        .get(JSON_CONSEQUENCES_KEY);
            case "prodNLD2":
                return isShowOnce
                        ? (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-once-consequence-prodNLD2.json")
                                        .get(JSON_CONSEQUENCES_KEY)
                        : (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-always-consequence-prodNLD2.json")
                                        .get(JSON_CONSEQUENCES_KEY);
            case "prodAUS5":
                return isShowOnce
                        ? (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-once-consequence-prodAUS5.json")
                                        .get(JSON_CONSEQUENCES_KEY)
                        : (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-always-consequence-prodAUS5.json")
                                        .get(JSON_CONSEQUENCES_KEY);
            default:
                return isShowOnce
                        ? (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-once-consequence-prodVA7.json")
                                        .get(JSON_CONSEQUENCES_KEY)
                        : (List)
                                MessagingTestUtils.getMapFromFile(
                                                "iam-show-always-consequence-prodVA7.json")
                                        .get(JSON_CONSEQUENCES_KEY);
        }
    }
}
