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
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.util.TimeUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FeedItemTests {

    private static final int SECONDS_IN_A_DAY = 86400;
    private static final String TITLE = "testTile";
    private static final String BODY = "testBody";
    private static final String IMAGE_URL = "testImageUrl";
    private static final String ACTION_URL = "testActionUrl";
    private static final String ACTION_TITLE = "testActionTitle";
    private final Map<String, Object> metaMap = new HashMap<String, Object>() {
        {
            put("stringKey", "value");
            put("key2", true);
            put("key3", 1000.1111);
        }
    };

    @Before
    public void setup() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCreateFeedItem_AllParametersPresent() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertEquals(metaMap, feedItem.getMeta());
    }

    @Test
    public void testCreateFeedItem_RequiredParametersOnly() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate).build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals("", feedItem.getImageUrl());
        assertEquals("", feedItem.getActionUrl());
        assertEquals("", feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertTrue(feedItem.getMeta().isEmpty());
    }

    @Test
    public void testCreateFeedItem_ActionTitleNotRequired() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setMeta(metaMap)
                .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals("", feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertEquals(metaMap, feedItem.getMeta());
    }

    @Test
    public void testCreateFeedItem_ActionUrlNotRequired() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals("", feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertEquals(metaMap, feedItem.getMeta());
    }

    @Test
    public void testCreateFeedItem_MetaNotRequired() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertTrue(feedItem.getMeta().isEmpty());
    }

    @Test
    public void testCreateFeedItem_ImageUrlNotRequired() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals("", feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertEquals(metaMap, feedItem.getMeta());
    }

    @Test
    public void testCreateFeedItem_TitleRequired() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(null, BODY, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_BodyRequired() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, null, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_InvalidPublishedDate() {
        // setup
        long expiryDate = TimeUtils.getUnixTimeInSeconds() + SECONDS_IN_A_DAY;

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, 0, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_InvalidExpiryDate() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();

        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY, publishedDate, 0)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap)
                .build();

        // verify
        assertNull(feedItem);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateFeedItem_CanOnlyBuildOnce() {
        // setup
        long publishedDate = TimeUtils.getUnixTimeInSeconds();
        long expiryDate = publishedDate + SECONDS_IN_A_DAY;

        // test
        FeedItem.Builder builder = new FeedItem.Builder(TITLE, BODY, publishedDate, expiryDate)
                .setImageUrl(IMAGE_URL)
                .setActionUrl(ACTION_URL)
                .setActionTitle(ACTION_TITLE)
                .setMeta(metaMap);
        FeedItem feedItem = builder.build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
        assertEquals(publishedDate, feedItem.getPublishedDate());
        assertEquals(expiryDate, feedItem.getExpiryDate());
        assertEquals(metaMap, feedItem.getMeta());

        // test, throws UnsupportedOperationException
        builder.build();
    }
}
