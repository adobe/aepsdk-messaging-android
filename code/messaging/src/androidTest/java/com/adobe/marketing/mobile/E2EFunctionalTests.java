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

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.messaging.BuildConfig;
import com.adobe.marketing.mobile.optimize.Optimize;

public class E2EFunctionalTests {
    static {
        BuildConfig.IS_E2E_TEST.set(true);
    }
    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());

    private static String activityIdBeingTested = "xcore:offer-activity:143614fd23c501cf";
    private static String placementIdBeingTested = "xcore:offer-placement:143f66555f80e367";

    // --------------------------------------------------------------------------------------------
    // Setup and teardown
    // --------------------------------------------------------------------------------------------
    @Before
    public void setup() throws Exception {
        MessagingFunctionalTestUtils.cleanCache();
        MessagingFunctionalTestUtils.setEdgeIdentityPersistence(MessagingFunctionalTestUtils.createIdentityMap("ECID", "mockECID"));
        Map<String, Object> testConfig = MessagingFunctionalTestUtils.getMapFromFile("functionalTestConfigStage.json");
        MobileCore.updateConfiguration(testConfig);

        Messaging.registerExtension();
        Optimize.registerExtension();
        Identity.registerExtension();
        Edge.registerExtension();

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

    @After
    public void tearDown() {
        // clear loaded rules
        MobileCore.getCore().eventHub.getModuleRuleAssociation().clear();
    }

    @Test
    public void testGetMessageDefinitionFromOptimize() throws InterruptedException {
        // setup
        final String expectedMessagingEventData = "{\"refreshmessages\":true}";
        final String expectedOptimizeEventData = "{\"requesttype\":\"updatepropositions\",\"decisionscopes\":[{\"name\":\"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTQzNjE0ZmQyM2M1MDFjZiIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjE0M2Y2NjU1NWY4MGUzNjciLCJpdGVtQ291bnQiOjMwfQ==\"}]}";
        // test
        Messaging.refreshInAppMessages();
        // wait for configuration + messaging rules to load
        assertTrue(MonitorExtension.waitForRulesToBeLoaded(2, "Messaging"));

        // verify messaging request content event
        final List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        final Event messagingRequestEvent = messagingRequestEvents.get(0);
        assertEquals(expectedMessagingEventData, messagingRequestEvent.getData().toString());

        // verify optimize request content event
        final List<Event> optimizeRequestEvents = getDispatchedEventsWith(MessagingConstants.EventType.OPTIMIZE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(2, optimizeRequestEvents.size()); // initial IAM fetch + refreshInAppMessages call
        final Event optimizeRequestEvent = optimizeRequestEvents.get(0);
        assertEquals(expectedOptimizeEventData, optimizeRequestEvent.getData().toString());

        // verify edge response content event
        final List<Event> edgeResponseEvents = getDispatchedEventsWith(MessagingConstants.EventType.EDGE,
                EventSource.RESPONSE_CONTENT.getName());
        assertEquals(1, edgeResponseEvents.size());
        final Event edgeResponseEvent = edgeResponseEvents.get(0);
        assertEquals(expectedMessagingEventData, messagingRequestEvent.getData().toString());

        // verify rules loaded into rules engine
        final ConcurrentHashMap moduleRules = MobileCore.getCore().eventHub.getModuleRuleAssociation();
        assertEquals(2, moduleRules.size());
        final Iterator iterator = moduleRules.keySet().iterator();
        while(iterator.hasNext()){
            Module module = (Module) iterator.next();
            if(module.getModuleName().equals("Messaging")) {
                ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule> messagingRules = (ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule>) moduleRules.get(module);
                assertEquals(1, messagingRules.size());
                com.adobe.marketing.mobile.Rule rule = messagingRules.element();
                assertEquals("", rule.condition.toString());
                assertEquals("", rule.consequenceEvents.get(0).getData().toString());
            }
        }
    }
}
