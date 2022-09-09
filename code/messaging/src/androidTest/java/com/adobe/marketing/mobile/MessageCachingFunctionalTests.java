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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.messaging.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class MessageCachingFunctionalTests {
    static {
        BuildConfig.IS_E2E_TEST.set(false);
        BuildConfig.IS_FUNCTIONAL_TEST.set(true);
    }

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());
    MessagingCacheUtilities messagingCacheUtilities;

    // --------------------------------------------------------------------------------------------
    // Setup and teardown
    // --------------------------------------------------------------------------------------------
    @Before
    public void setup() throws Exception {
        MessagingTestUtils.setEdgeIdentityPersistence(MessagingTestUtils.createIdentityMap("ECID", "mockECID"), TestHelper.defaultApplication);
        Messaging.registerExtension();
        com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

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

        // setup messaging caching
        final SystemInfoService systemInfoService = MessagingUtils.getPlatformServices().getSystemInfoService();
        final NetworkService networkService = MessagingUtils.getPlatformServices().getNetworkService();
        final CacheManager cacheManager = new CacheManager(systemInfoService);
        messagingCacheUtilities = new MessagingCacheUtilities(systemInfoService, networkService, cacheManager);

        // ensure cache is cleared before testing
        MessagingTestUtils.cleanCache();

        // write an image file from resources to the image asset cache
        MessagingTestUtils.addImageAssetToCache();
    }

    @After
    public void tearDown() {
        // clear cache and loaded rules
        messagingCacheUtilities.clearCachedDataFromSubdirectory();
        MobileCore.getCore().eventHub.getModuleRuleAssociation().clear();
    }

    // --------------------------------------------------------------------------------------------
    // Caching received message payload
    // --------------------------------------------------------------------------------------------
    @Test
    public void testMessageCaching_ReceivedMessagePayload() {
        final String expectedCondition = "((foo EQUALS bar))";
        final String expectedConsequence = MessagingTestUtils.loadStringFromFile("expected_consequence_data.txt");
        // dispatch edge response event containing a messaging payload
        MessagingTestUtils.dispatchEdgePersonalizationEventWithMessagePayload("personalization_payload.json");
        // wait for event and rules processing
        TestHelper.sleep(1000);
        // verify rule payload was loaded into rules engine
        final ConcurrentHashMap moduleRules = MobileCore.getCore().eventHub.getModuleRuleAssociation();
        assertEquals(2, moduleRules.size()); // configuration + messaging
        final Iterator iterator = moduleRules.keySet().iterator();
        while (iterator.hasNext()) {
            Module module = (Module) iterator.next();
            if (module.getModuleName().equals("Messaging")) {
                ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule> messagingRules = (ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule>) moduleRules.get(module);
                assertEquals(1, messagingRules.size());
                com.adobe.marketing.mobile.Rule rule = messagingRules.element();
                assertEquals(expectedCondition, rule.condition.toString());
                assertEquals(expectedConsequence, rule.consequenceEvents.get(0).getData().toString());
            }
        }
        // verify message payload was cached
        assertTrue(messagingCacheUtilities.arePropositionsCached());
        final Map<String, Object> cachedPropositions = messagingCacheUtilities.getCachedPropositions();
        assertEquals(cachedPropositions, MessagingTestUtils.getMapFromFile("personalization_payload.json"));
    }

    @Test
    public void testMessageCaching_ReceivedInvalidMessagePayload() {
        // dispatch edge response event containing a messaging payload
        MessagingTestUtils.dispatchEdgePersonalizationEventWithMessagePayload("invalid.json");
        // wait for event and rules processing
        TestHelper.sleep(1000);
        // verify rule payload was not loaded into rules engine
        final ConcurrentHashMap moduleRules = MobileCore.getCore().eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size()); // configuration only
        // verify message payload was not cached
        assertFalse(messagingCacheUtilities.arePropositionsCached());
    }
}