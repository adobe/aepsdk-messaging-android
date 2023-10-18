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

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class InboundTests {
    String uniqueid = "uniqueid";
    InboundType inboundTypeFeed = InboundType.FEED;
    String content = "{\"version\":1,\"rules\":[{\"consequences\":[{\"type\":\"schema\",\"id\":\"uniqueId\",\"detail\":{\"id\":\"uniqueDetailId\",\"schema\":\"https://ns.adobe.com/personalization/message/feed-item\", \"data\":{\"expiryDate\":1717688797,\"publishedDate\":1717688797,\"contentType\":\"application/json\",\"meta\":{\"surface\":\"mobileapp://mockApp\",\"feedName\":\"apifeed\",\"campaignName\":\"mockCampaign\"},\"content\":{\"actionUrl\":\"https://adobe.com/\",\"actionTitle\":\"test action title\",\"title\":\"test title\",\"body\":\"test body\",\"imageUrl\":\"https://adobe.com/image.png\"}}}}],\"condition\":{\"type\":\"group\",\"definition\":{\"conditions\":[{\"type\":\"matcher\",\"definition\":{\"matcher\":\"ge\",\"key\":\"~timestampu\",\"values\":[1686066397]}},{\"type\":\"matcher\",\"definition\":{\"matcher\":\"le\",\"key\":\"~timestampu\",\"values\":[1717688797]}}],\"logic\":\"and\"}}}]}";
    String contentType = "contentType";
    int publishedDate = 12345678;
    int expiryDate = 12345678;
    Map<String, Object> meta = new HashMap<String, Object>() {{
        put("key", "value");
        put("key2", 1234);
    }};

    @Test
    public void testCreateInbound_AllParametersPresent() {
        // test
        Inbound inbound = new Inbound(uniqueid, inboundTypeFeed, content, contentType, publishedDate, expiryDate, meta);

        // verify
        assertNotNull(inbound);
        assertEquals(uniqueid, inbound.getUniqueId());
        assertEquals(InboundType.FEED, inbound.getInboundType());
        assertEquals(content, inbound.getContent());
        assertEquals(contentType, inbound.getContentType());
        assertEquals(publishedDate, inbound.getPublishedDate());
        assertEquals(expiryDate, inbound.getExpiryDate());
        assertEquals(meta, inbound.getMeta());
    }

    @Test
    public void testCreateInbound_DefaultValues() {
        // test
        Inbound inbound = new Inbound(null, null, null, null, -1, -1, null);

        // verify
        assertNotNull(inbound);
        assertEquals("", inbound.getUniqueId());
        assertEquals(InboundType.UNKNOWN, inbound.getInboundType());
        assertEquals("", inbound.getContent());
        assertEquals("", inbound.getContentType());
        assertEquals(0, inbound.getPublishedDate());
        assertEquals(0, inbound.getExpiryDate());
        assertEquals(new HashMap<>(), inbound.getMeta());
    }

    @Test
    public void testCreateFeedItemFromValidInbound() {
        // test
        List<Inbound> inboundList = MessagingTestUtils.createInboundList(1);
        FeedItem feedItem = inboundList.get(0).toFeedItem();

        // verify
        assertNotNull(feedItem);
        assertEquals("testActionTitle", feedItem.getActionTitle());
        assertEquals("https://someurl.com", feedItem.getActionUrl());
        assertEquals("testBody", feedItem.getBody());
        assertEquals("testTitle", feedItem.getTitle());
        assertEquals("https://someimage0.png", feedItem.getImageUrl());
    }

    @Test
    public void testCreateFeedItemFromInvalidInbound() {
        // test
        Inbound invalidInbound = new Inbound("id", InboundType.INAPP, "content", "feed", 1234, 1234, Collections.EMPTY_MAP);
        FeedItem feedItem = invalidInbound.toFeedItem();

        // verify
        assertNull(feedItem);
    }
}
