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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.ArrayList;
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
        MessagingTestUtils.setEdgeIdentityPersistence(MessagingTestUtils.createIdentityMap("ECID", "mockECID"), TestHelper.defaultApplication);
        Messaging.registerExtension();
        com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

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

        latch.await(2, TimeUnit.SECONDS);

        // ensure cache is cleared before testing
        MessagingTestUtils.cleanCache();

        // write an image file from resources to the image asset cache
        MessagingTestUtils.addImageAssetToCache();
    }

    @After
    public void tearDown() {
        // clear cache and loaded rules
        messagingCacheUtilities.clearCachedData();
    }

    // --------------------------------------------------------------------------------------------
    // Caching received message payload
    // --------------------------------------------------------------------------------------------
    @Test
    public void testMessageCaching_ReceivedMessagePayload() {
        // dispatch edge response event containing a messaging payload
        MessagingTestUtils.dispatchEdgePersonalizationEventWithMessagePayload("personalization_payload.json");
        // wait for event and rules processing
        TestHelper.sleep(1000);
        // verify message payload was cached
        assertTrue(messagingCacheUtilities.arePropositionsCached());
        final List<PropositionPayload> cachedPropositions = messagingCacheUtilities.getCachedPropositions();
        final List<Map<String, Object>> expectedPropositions = new ArrayList<>();
        expectedPropositions.add(MessagingTestUtils.getMapFromFile("personalization_payload.json"));
        final String expectedPropositionString = MessagingTestUtils.convertPayloadToString(MessagingUtils.getPropositionPayloads(expectedPropositions));
        assertEquals(expectedPropositionString, MessagingTestUtils.convertPayloadToString(cachedPropositions));
    }

    @Test
    public void testMessageCaching_ReceivedInvalidMessagePayload() {
        // dispatch edge response event containing a messaging payload
        MessagingTestUtils.dispatchEdgePersonalizationEventWithMessagePayload("invalid.json");
        // wait for event and rules processing
        TestHelper.sleep(1000);
        // verify message payload was not cached
        assertFalse(messagingCacheUtilities.arePropositionsCached());
    }
}