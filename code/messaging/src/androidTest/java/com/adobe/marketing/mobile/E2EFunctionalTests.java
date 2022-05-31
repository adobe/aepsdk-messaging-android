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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.messaging.BuildConfig;
import com.adobe.marketing.mobile.optimize.Optimize;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        MessagingTestUtils.setEdgeIdentityPersistence(MessagingTestUtils.createIdentityMap("ECID", "80195814545200720557089495418993853789"), TestHelper.defaultApplication);

        Messaging.registerExtension();
        Optimize.registerExtension();
        Identity.registerExtension();
        Edge.registerExtension();

        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(Object o) {
                Map<String, Object> testConfig = MessagingTestUtils.getMapFromFile("functionalTestConfigStage.json");
                MobileCore.updateConfiguration(testConfig);
                // wait for configuration to be set
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interruptedException) {
                    fail(interruptedException.getMessage());
                }
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
        final String expectedOptimizeEventData = "{\"requesttype\":\"updatepropositions\",\"decisionscopes\":[{\"name\":\"eyJhY3Rpdml0eUlkIjoieGNvcmU6b2ZmZXItYWN0aXZpdHk6MTRiNTU2YzExZDRjMjQzMyIsInBsYWNlbWVudElkIjoieGNvcmU6b2ZmZXItcGxhY2VtZW50OjE0MmFlMTBkMWQyZmQ4ODMiLCJpdGVtQ291bnQiOjMwfQ==\"}]}";
        // test
        Messaging.refreshInAppMessages();
        // wait for configuration + messaging rules to load
        assertTrue(MonitorExtension.waitForRulesToBeLoaded(2, "Messaging"));

        // verify messaging request content event from refreshInAppMessages API call
        final List<Event> messagingRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.MESSAGING,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(1, messagingRequestEvents.size());
        final Event messagingRequestEvent = messagingRequestEvents.get(0);
        assertEquals(expectedMessagingEventData, messagingRequestEvent.getData().toString());

        // verify optimize request content event, 2 events expected due to initial offers fetch + refreshInAppMessages api call
        final List<Event> optimizeRequestEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.OPTIMIZE,
                EventSource.REQUEST_CONTENT.getName());
        assertEquals(2, optimizeRequestEvents.size());
        final Event optimizeRequestEvent = optimizeRequestEvents.get(0);
        assertEquals(expectedOptimizeEventData, optimizeRequestEvent.getData().toString());

        // verify edge personalization decision event, 2 events expected due to initial offers fetch + refreshInAppMessages api call
        final List<Event> edgePersonalizationDecisionsEvents = getDispatchedEventsWith(MessagingTestConstants.EventType.EDGE, MessagingTestConstants.EventSource.PERSONALIZATION_DECISIONS);
        assertEquals(2, edgePersonalizationDecisionsEvents.size()); // initial messages fetch + refreshInAppMessage api
        final Event edgePersonalizationDecisionEvent = edgePersonalizationDecisionsEvents.get(0);
        assertNotNull(edgePersonalizationDecisionEvent.getData());

        // verify "test EQUALS e2e" messaging rule loaded into rules engine
        final ConcurrentHashMap moduleRules = MobileCore.getCore().eventHub.getModuleRuleAssociation();
        assertEquals(2, moduleRules.size());
        final Iterator iterator = moduleRules.keySet().iterator();
        while (iterator.hasNext()) {
            Module module = (Module) iterator.next();
            if (module.getModuleName().equals("Messaging")) {
                ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule> messagingRules = (ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule>) moduleRules.get(module);
                assertEquals(1, messagingRules.size());
                com.adobe.marketing.mobile.Rule rule = messagingRules.element();
                assertEquals("(test EQUALS e2e)", rule.condition.toString());
                assertNotNull(rule.consequenceEvents.get(0).getData());
            }
        }
    }
}
