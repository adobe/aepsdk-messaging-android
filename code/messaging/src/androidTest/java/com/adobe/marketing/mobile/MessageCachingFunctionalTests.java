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

import static com.adobe.marketing.mobile.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.messaging.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class MessageCachingFunctionalTests {
    static {
        BuildConfig.IS_E2E_TEST.set(false);
    }
    private final static String REMOTE_URL = "https://www.adobe.com/adobe.png";
    private CacheManager cacheManager;

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());
    MessagingCacheUtilities messagingCacheUtilities;

    // --------------------------------------------------------------------------------------------
    // Setup and teardown
    // --------------------------------------------------------------------------------------------
    @Before
    public void setup() throws Exception {
        MessagingFunctionalTestUtils.setEdgeIdentityPersistence(MessagingFunctionalTestUtils.createIdentityMap("ECID", "mockECID"));
        HashMap<String, Object> config = new HashMap<String, Object>() {
            {
                put("experienceCloud.org", "xcore:offer-activity:13c2593fcbcfacbd");
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

        // setup messaging caching
        final SystemInfoService systemInfoService = MessagingUtils.getPlatformServices().getSystemInfoService();
        final NetworkService networkService = MessagingUtils.getPlatformServices().getNetworkService();
        messagingCacheUtilities = new MessagingCacheUtilities(systemInfoService, networkService);

        cacheManager = new CacheManager(systemInfoService);
        // ensure cache is cleared before testing
        MessagingFunctionalTestUtils.cleanCache();

        // write a image file from resources to the image asset cache
        final File mockCachedImage = cacheManager.createNewCacheFile(REMOTE_URL, IMAGES_CACHE_SUBDIRECTORY, new Date(System.currentTimeMillis()));
        MessagingFunctionalTestUtils.readInputStreamIntoFile(mockCachedImage, MessagingFunctionalTestUtils.convertResourceFileToInputStream("adobe", ".png"), false);
    }

    @After
    public void tearDown() {
        // clear cache and loaded rules
        messagingCacheUtilities.clearCachedDataFromSubdirectory(MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY);
        MobileCore.getCore().eventHub.getModuleRuleAssociation().clear();
    }

    // --------------------------------------------------------------------------------------------
    // Caching received message payload
    // --------------------------------------------------------------------------------------------
    @Test
    public void testMessageCaching_ReceivedMessagePayload() {
        final String expectedCondition = "((foo EQUALS bar))";
        final String expectedConsequence = MessagingFunctionalTestUtils.loadStringFromFile("expected_consequence_data.txt");
        // dispatch edge response event containing a messaging payload
        MessagingFunctionalTestUtils.dispatchEdgePersonalizationEventWithMessagePayload("optimize_payload.json");
        // wait for event and rules processing
        TestHelper.sleep(500);
        // verify rule payload was loaded into rules engine
        final ConcurrentHashMap moduleRules = MobileCore.getCore().eventHub.getModuleRuleAssociation();
        assertEquals(2, moduleRules.size()); // configuration + messaging
        final Iterator iterator = moduleRules.keySet().iterator();
        while(iterator.hasNext()){
            Module module = (Module) iterator.next();
            if(module.getModuleName().equals("Messaging")) {
                ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule> messagingRules = (ConcurrentLinkedQueue<com.adobe.marketing.mobile.Rule>) moduleRules.get(module);
                assertEquals(1, messagingRules.size());
                com.adobe.marketing.mobile.Rule rule = messagingRules.element();
                assertEquals(expectedCondition, rule.condition.toString());
                assertEquals(expectedConsequence, rule.consequenceEvents.get(0).getData().toString());
            }
        }
        // verify message payload was cached
        assertTrue(messagingCacheUtilities.areMessagesCached());
        final Map<String, Variant> cachedMessages = messagingCacheUtilities.getCachedMessages();
        assertEquals(cachedMessages, MessagingFunctionalTestUtils.getVariantMapFromFile("optimize_payload.json"));
    }

    @Test
    public void testMessageCaching_ReceivedInvalidMessagePayload() {
        // dispatch edge response event containing a messaging payload
        MessagingFunctionalTestUtils.dispatchEdgePersonalizationEventWithMessagePayload("invalid.json");
        // wait for event and rules processing
        TestHelper.sleep(500);
        // verify rule payload was not loaded into rules engine
        final ConcurrentHashMap moduleRules = MobileCore.getCore().eventHub.getModuleRuleAssociation();
        assertEquals(1, moduleRules.size()); // configuration only
        // verify message payload was not cached
        assertFalse(messagingCacheUtilities.areMessagesCached());
    }
}