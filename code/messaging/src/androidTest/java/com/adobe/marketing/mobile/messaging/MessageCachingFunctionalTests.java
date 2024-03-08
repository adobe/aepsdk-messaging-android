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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.util.TestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class MessageCachingFunctionalTests {
    static {
        BuildConfig.IS_E2E_TEST.set(false);
        BuildConfig.IS_FUNCTIONAL_TEST.set(true);
    }

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());
    MessagingCacheUtilities messagingCacheUtilities = new MessagingCacheUtilities();

    // --------------------------------------------------------------------------------------------
    // Setup and teardown
    // --------------------------------------------------------------------------------------------
    @Before
    public void setup() throws Exception {
        MessagingTestUtils.setEdgeIdentityPersistence(MessagingTestUtils.createIdentityMap("ECID", "mockECID"), TestHelper.getDefaultApplication());

        final CountDownLatch latch = new CountDownLatch(1);
        final List<Class<? extends Extension>> extensions = new ArrayList<Class<? extends Extension>>() {{
            add(Messaging.EXTENSION);
            add(Identity.EXTENSION);
            add(Edge.EXTENSION);
        }};

        MobileCore.registerExtensions(extensions, o -> {
            Map<String, Object> testConfig = MessagingTestUtils.getMapFromFile("functionalTestConfig.json");
            MobileCore.updateConfiguration(testConfig);
            // wait for configuration to be set
            try {
                Thread.sleep(1000);
            } catch (InterruptedException interruptedException) {
                fail(interruptedException.getMessage());
            }
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);

        // ensure cache is cleared before testing
        MessagingTestUtils.cleanCache();

        // write an image file from resources to the image asset cache
        MessagingTestUtils.addImageAssetToCache();
    }

    @After
    public void tearDown() {
        messagingCacheUtilities.clearCachedData();
    }

    @Test
    public void testMessageCaching_CachePropositions() {
        final Surface surface = new Surface();
        final Map<Surface, List<Proposition>> propositions = new HashMap<>();
        final List<Proposition> propositionList = new ArrayList<>();
        propositionList.add(Proposition.fromEventData(MessagingTestUtils.getMapFromFile("personalization_payload.json")));
        propositions.put(surface, propositionList);
        // add a messaging payload to the cache
        messagingCacheUtilities.cachePropositions(propositions, Collections.EMPTY_LIST);
        // wait for event and rules processing
        TestHelper.sleep(1000);
        // verify message payload was cached
        assertTrue(messagingCacheUtilities.arePropositionsCached());
        final Map<Surface, List<Proposition>> cachedPropositions = messagingCacheUtilities.getCachedPropositions();
        final List<Map<String, Object>> expectedPropositions = new ArrayList<>();
        expectedPropositions.add(MessagingTestUtils.getMapFromFile("personalization_payload.json"));
        final String expectedPropositionString = MessagingTestUtils.convertPropositionsToString(InternalMessagingUtils.getPropositionsFromPayloads(expectedPropositions));
        assertEquals(expectedPropositionString, MessagingTestUtils.convertPropositionsToString(cachedPropositions.get(surface)));
    }
}