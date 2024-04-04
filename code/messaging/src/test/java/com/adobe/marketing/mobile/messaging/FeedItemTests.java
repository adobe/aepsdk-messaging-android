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

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FeedItemTests {

    private static final int SECONDS_IN_A_DAY = 86400;
    private static final String TITLE = "testTitle";
    private static final String BODY = "testBody";
    private static final String IMAGE_URL = "testImageUrl";
    private static final String ACTION_URL = "testActionUrl";
    private static final String ACTION_TITLE = "testActionTitle";
    private final Map<String, Object> metaMap =
            new HashMap<String, Object>() {
                {
                    put("stringKey", "value");
                    put("key2", true);
                    put("key3", 1000.1111);
                }
            };

    @Before
    public void setup() {}

    @After
    public void tearDown() {}

    @Test
    public void testCreateFeedItem_AllParametersPresent() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(TITLE, BODY)
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
    }

    @Test
    public void testCreateFeedItem_RequiredParametersOnly() {
        // test
        FeedItem feedItem = new FeedItem.Builder(TITLE, BODY).build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals("", feedItem.getImageUrl());
        assertEquals("", feedItem.getActionUrl());
        assertEquals("", feedItem.getActionTitle());
    }

    @Test
    public void testCreateFeedItem_ActionTitleNotRequired() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(TITLE, BODY)
                        .setImageUrl(IMAGE_URL)
                        .setActionUrl(ACTION_URL)
                        .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals("", feedItem.getActionTitle());
    }

    @Test
    public void testCreateFeedItem_ActionUrlNotRequired() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(TITLE, BODY)
                        .setImageUrl(IMAGE_URL)
                        .setActionTitle(ACTION_TITLE)
                        .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals("", feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
    }

    @Test
    public void testCreateFeedItem_MetaNotRequired() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(TITLE, BODY)
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
    }

    @Test
    public void testCreateFeedItem_ImageUrlNotRequired() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(TITLE, BODY)
                        .setActionUrl(ACTION_URL)
                        .setActionTitle(ACTION_TITLE)
                        .build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals("", feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());
    }

    @Test
    public void testCreateFeedItem_TitleRequired() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(null, BODY)
                        .setImageUrl(IMAGE_URL)
                        .setActionUrl(ACTION_URL)
                        .setActionTitle(ACTION_TITLE)
                        .build();

        // verify
        assertNull(feedItem);
    }

    @Test
    public void testCreateFeedItem_BodyRequired() {
        // test
        FeedItem feedItem =
                new FeedItem.Builder(TITLE, null)
                        .setImageUrl(IMAGE_URL)
                        .setActionUrl(ACTION_URL)
                        .setActionTitle(ACTION_TITLE)
                        .build();

        // verify
        assertNull(feedItem);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCreateFeedItem_CanOnlyBuildOnce() {
        // test
        FeedItem.Builder builder =
                new FeedItem.Builder(TITLE, BODY)
                        .setImageUrl(IMAGE_URL)
                        .setActionUrl(ACTION_URL)
                        .setActionTitle(ACTION_TITLE);
        FeedItem feedItem = builder.build();

        // verify
        assertNotNull(feedItem);
        assertEquals(TITLE, feedItem.getTitle());
        assertEquals(BODY, feedItem.getBody());
        assertEquals(IMAGE_URL, feedItem.getImageUrl());
        assertEquals(ACTION_URL, feedItem.getActionUrl());
        assertEquals(ACTION_TITLE, feedItem.getActionTitle());

        // test, throws UnsupportedOperationException
        builder.build();
    }
}
