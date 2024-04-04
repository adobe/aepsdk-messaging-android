/*
  Copyright 2023 Adobe. All rights reserved.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FeedTests {
    private static final String TITLE = "testTitle";
    private static final String BODY = "testBody";
    private static final String IMAGE_URL = "testImageUrl";
    private static final String ACTION_URL = "testActionUrl";
    private static final String ACTION_TITLE = "testActionTitle";
    private static final String SURFACE_URI = "mobileapp://com.app.appname/testSurfaceUri";
    private static final String FEED_NAME = "testFeedName";
    private FeedItem feedItem;
    private FeedItem feedItem2;

    private final List<FeedItem> feedItems = new ArrayList<>();

    @Mock ServiceProvider mockServiceProvider;
    @Mock DeviceInforming mockDeviceInfoService;

    void runUsingMockedServiceProvider(final Runnable runnable) {
        try (MockedStatic<ServiceProvider> serviceProviderMockedStatic =
                Mockito.mockStatic(ServiceProvider.class)) {
            serviceProviderMockedStatic
                    .when(ServiceProvider::getInstance)
                    .thenReturn(mockServiceProvider);
            when(mockServiceProvider.getDeviceInfoService()).thenReturn(mockDeviceInfoService);
            when(mockDeviceInfoService.getApplicationPackageName()).thenReturn("mockPackageName");

            runnable.run();
        }
    }

    @Before
    public void setup() {
        feedItem =
                new FeedItem.Builder(TITLE, BODY)
                        .setImageUrl(IMAGE_URL)
                        .setActionUrl(ACTION_URL)
                        .setActionTitle(ACTION_TITLE)
                        .build();
        feedItem2 =
                new FeedItem.Builder(TITLE + "2", BODY + "2")
                        .setImageUrl(IMAGE_URL + "2")
                        .setActionUrl(ACTION_URL + "2")
                        .setActionTitle(ACTION_TITLE + "2")
                        .build();
        feedItems.add(feedItem);
        feedItems.add(feedItem2);
    }

    @After
    public void tearDown() {
        reset(mockServiceProvider);
        reset(mockDeviceInfoService);
    }

    @Test
    public void testCreateFeed_AllParametersPresent() {
        // test
        runUsingMockedServiceProvider(
                () -> {
                    Feed feed = new Feed(FEED_NAME, Surface.fromUriString(SURFACE_URI), feedItems);

                    // verify
                    assertNotNull(feed);
                    assertEquals(SURFACE_URI, feed.getSurfaceUri());
                    assertEquals(feedItems, feed.getItems());
                });
    }

    @Test
    public void testCreateFeed_NoFeedItems() {
        // test
        runUsingMockedServiceProvider(
                () -> {
                    Feed feed = new Feed(FEED_NAME, Surface.fromUriString(SURFACE_URI), null);

                    // verify
                    assertNotNull(feed);
                    assertEquals(SURFACE_URI, feed.getSurfaceUri());
                    assertNull(feed.getItems());
                });
    }

    @Test
    public void testCreateFeed_NullSurfaceUri() {
        // test
        runUsingMockedServiceProvider(
                () -> {
                    Feed feed = new Feed(FEED_NAME, Surface.fromUriString(null), feedItems);

                    // verify
                    assertNotNull(feed);
                    assertEquals(null, feed.getSurfaceUri());
                    assertEquals(feedItems, feed.getItems());
                });
    }

    @Test
    public void testCreateFeed_NullSurface() {
        // test
        runUsingMockedServiceProvider(
                () -> {
                    Feed feed = new Feed(FEED_NAME, null, feedItems);

                    // verify
                    assertNotNull(feed);
                    assertEquals(null, feed.getSurfaceUri());
                    assertEquals(feedItems, feed.getItems());
                });
    }

    @Test
    public void testCreateFeed_EmptySurfaceUri() {
        // test
        runUsingMockedServiceProvider(
                () -> {
                    Feed feed = new Feed(FEED_NAME, Surface.fromUriString(""), feedItems);

                    // verify
                    assertNotNull(feed);
                    assertEquals(null, feed.getSurfaceUri());
                    assertEquals(feedItems, feed.getItems());
                });
    }

    @Test
    public void testFeedGetters() {
        // test
        runUsingMockedServiceProvider(
                () -> {
                    Feed feed = new Feed(FEED_NAME, Surface.fromUriString(SURFACE_URI), feedItems);

                    // verify
                    assertEquals(FEED_NAME, feed.getName());
                    assertEquals(feedItems, feed.getItems());
                    assertEquals(SURFACE_URI, feed.getSurfaceUri());
                });
    }
}
