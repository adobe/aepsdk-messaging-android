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

package com.adobe.marketing.mobile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.adobe.marketing.mobile.util.TimeUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FeedTests {
    private static final int SECONDS_IN_A_DAY = 86400;
    private static final String TITLE = "testTitle";
    private static final String BODY = "testBody";
    private static final String IMAGE_URL = "testImageUrl";
    private static final String ACTION_URL = "testActionUrl";
    private static final String ACTION_TITLE = "testActionTitle";
    private static final String SURFACE_URI = "testSurfaceUri";
    private static final String FEED_NAME = "testFeedName";
    private static final long PUBLISHED_DATE = TimeUtils.getUnixTimeInSeconds();
    private static final long EXPIRY_DATE = PUBLISHED_DATE + SECONDS_IN_A_DAY;
    private final Map<String, Object> metaMap = new HashMap<String, Object>() {
        {
            put("stringKey", "value");
            put("key2", true);
            put("key3", 1000.1111);
        }
    };
    private final Map<String, Object> metaMap2 = new HashMap<String, Object>() {
        {
            put("key5", "value5");
            put("key6", 1.2345);
        }
    };
    private final List<FeedItem> feedItems = new ArrayList<>();

    @Before
    public void setup() {
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, PUBLISHED_DATE, EXPIRY_DATE)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();
        FeedItem feedItem2 = new FeedItem.Builder(TITLE + "2", BODY + "2", PUBLISHED_DATE, EXPIRY_DATE)
                .setImageUrl(IMAGE_URL + "2")
                .setActionUrl(ACTION_URL + "2")
                .setActionTitle(ACTION_TITLE + "2")
                .setMeta(metaMap2)
                .build();
        feedItems.add(feedItem);
        feedItems.add(feedItem2);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateFeed_AllParametersPresent() {
        // test
        Feed feed = new Feed(SURFACE_URI, FEED_NAME, feedItems);

        // verify
        assertNotNull(feed);
        assertEquals(SURFACE_URI, feed.getSurfaceUri());
        assertEquals(FEED_NAME, feed.getName());
        assertEquals(feedItems, feed.getItems());
    }

    @Test
    public void testCreateFeed_NoFeedName() {
        // test
        Feed feed = new Feed(SURFACE_URI, null, feedItems);

        // verify
        assertNotNull(feed);
        assertEquals(SURFACE_URI, feed.getSurfaceUri());
        assertNull(feed.getName());
        assertEquals(feedItems, feed.getItems());
    }

    @Test
    public void testCreateFeed_NoFeedItems() {
        // test
        Feed feed = new Feed(SURFACE_URI, FEED_NAME, null);

        // verify
        assertNotNull(feed);
        assertEquals(SURFACE_URI, feed.getSurfaceUri());
        assertEquals(FEED_NAME, feed.getName());
        assertNull(feed.getItems());
    }

    @Test
    public void testCreateFeed_NoSurfaceUri() {
        // test
        Feed feed = new Feed(null, FEED_NAME, feedItems);

        // verify
        assertNotNull(feed);
        assertNull(feed.getSurfaceUri());
        assertEquals(FEED_NAME, feed.getName());
        assertEquals(feedItems, feed.getItems());
    }
}
