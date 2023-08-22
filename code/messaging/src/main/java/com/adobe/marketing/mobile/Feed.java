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

import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Feed} object aggregates one or more {@link FeedItem}s.
 */
public class Feed {
    private static final String FEED_SURFACE_URI_KEY = "surfaceUri";
    private static final String FEED_ITEMS_KEY = "items";
    private static final String MAP_FEEDS_KEY = "feeds";
    private static final String MAP_ITEMS_KEY = "items";

    // Identification for this feed, represented by the AJO Surface URI used to retrieve it
    private final String surface;

    // List of FeedItem that are members of this Feed
    private final List<FeedItem> items;

    /**
     * Constructor.
     *
     * @param surfaceUri {@link String} containing the AJO Surface URI used to retrieve the feed
     * @param items {@link List<FeedItem>} that are members of this {@link Feed}
     */
    public Feed(final String surfaceUri, final List<FeedItem> items) {
        this.surface = surfaceUri;
        this.items = items;
    }

    /**
     * Gets the {@code Feed}'s surface uri.
     *
     * @return {@link String} containing the {@link Feed} surface uri.
     */
    public String getSurfaceUri() {
        return surface;
    }

    /**
     * Gets the {@code Feed}'s {@code FeedItem}s
     *
     * @return {@link List<FeedItem>} containing the {@link Feed}'s {@link FeedItem}s
     */
    public List<FeedItem> getItems() {
        return items;
    }

    /**
     * Converts the {@code Feed} into a {@code Map<String, Object>}.
     *
     * @return {@code Map<String, Object>} containing the {@link Feed} data.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> feedAsMap = new HashMap<>();
        feedAsMap.put(FEED_SURFACE_URI_KEY, getSurfaceUri());

        final List<Object> feedItemList = new ArrayList<>();
        for (final FeedItem feedItem : getItems()) {
            feedItemList.add(feedItem.toEventData());
        }
        feedAsMap.put(FEED_ITEMS_KEY, feedItemList);
        return feedAsMap;
    }

    /**
     * Creates a {@code Feed} from the event data {@code Map<String, Object>}.
     *
     * @return {@code Feed} created from the event data {@code Map<String, Object>}
     */
    public static Feed fromEventData(final Map<String, Object> eventData) {
        if (MapUtils.isNullOrEmpty(eventData)) {
            return null;
        }

        final Map.Entry<String, Object> feedEntry = eventData.entrySet().iterator().next();
        final Map<String, Object> feedMap = (Map<String, Object>) feedEntry.getValue();
        if (MapUtils.isNullOrEmpty(feedMap)) {
            return null;
        }
        final List<Map> feedItemObjects = DataReader.optTypedList(Map.class, feedMap, MAP_ITEMS_KEY, null);
        if (feedItemObjects == null || feedItemObjects.isEmpty()) {
            return null;
        }
        final List<FeedItem> feedItems = new ArrayList<>();
        for (final Map feedItemObject : feedItemObjects) {
            feedItems.add(FeedItem.fromEventData(feedItemObject));
        }
        return new Feed(DataReader.optString(feedMap, FEED_SURFACE_URI_KEY, ""), feedItems);
    }
}
