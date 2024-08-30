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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SDKHelper;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.util.TestHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PushTokenSyncingTests {
    @Rule
    public RuleChain rule =
            RuleChain.outerRule(new TestHelper.SetupCoreRule())
                    .around(new TestHelper.RegisterMonitorExtensionRule());

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
                        put("someconfig", "someConfigValue");
                    }
                };

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
                    MobileCore.updateConfiguration(config);
                    // wait for configuration to be set
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        fail(interruptedException.getMessage());
                    }
                    latch.countDown();
                });

        latch.await(2, TimeUnit.SECONDS);
        resetTestExpectations();
    }

    @After
    public void tearDown() {
        SDKHelper.resetSDK();
    }

    // --------------------------------------------------------------------------------------------
    // Syncing push token
    // --------------------------------------------------------------------------------------------
    @Test
    public void testPushTokenSync() throws InterruptedException {
        // expected results
        String expectedEdgeEvent =
                "{data={pushNotificationDetails=[{denylisted=false,"
                        + " identity={namespace={code=ECID}, id=mockECID},"
                        + " appID=com.adobe.marketing.mobile.messaging.test, platform=fcm,"
                        + " token=mockPushToken}]}}";
        // test
        MobileCore.setPushIdentifier("mockPushToken");

        // verify messaging event
        List<Event> genericIdentityEvents =
                getDispatchedEventsWith(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT);
        assertEquals(1, genericIdentityEvents.size());

        // verify push profile sync edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(1, edgeRequestEvents.size());
        assertEquals(expectedEdgeEvent, edgeRequestEvents.get(0).getEventData().toString());

        // verify shared state is updated with the push token
        Map<String, String> sharedStateMap =
                MessagingTestUtils.flattenMap(
                        getSharedStateFor(MessagingTestConstants.EXTENSION_NAME, 1000));
        String pushToken =
                sharedStateMap.get(MessagingTestConstants.SharedState.Messaging.PUSH_IDENTIFIER);
        assertEquals("mockPushToken", pushToken);
    }

    @Test
    public void testPushTokenSync_pushTokenIsNull() throws InterruptedException {
        // test
        MobileCore.setPushIdentifier(null);

        // verify messaging event
        List<Event> genericIdentityEvents =
                getDispatchedEventsWith(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT);
        assertEquals(1, genericIdentityEvents.size());

        // verify no push profile sync edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(0, edgeRequestEvents.size());

        // verify shared state is not updated with the push token
        Map<String, String> sharedStateMap =
                MessagingTestUtils.flattenMap(
                        getSharedStateFor(MessagingTestConstants.EXTENSION_NAME, 1000));
        assertEquals(0, sharedStateMap.size());
    }

    @Test
    public void testPushTokenSync_pushTokenIsEmpty() throws InterruptedException {
        // test
        MobileCore.setPushIdentifier("");

        // verify messaging event
        List<Event> genericIdentityEvents =
                getDispatchedEventsWith(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT);
        assertEquals(1, genericIdentityEvents.size());

        // verify no push profile sync edge event
        List<Event> edgeRequestEvents =
                getDispatchedEventsWith(
                        MessagingTestConstants.EventType.EDGE, EventSource.REQUEST_CONTENT);
        assertEquals(0, edgeRequestEvents.size());

        // verify shared state is not updated with the push token
        Map<String, String> sharedStateMap =
                MessagingTestUtils.flattenMap(
                        getSharedStateFor(MessagingTestConstants.EXTENSION_NAME, 1000));
        String pushToken =
                sharedStateMap.get(MessagingTestConstants.SharedState.Messaging.PUSH_IDENTIFIER);
        assertNull(pushToken);
    }
}
