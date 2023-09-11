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

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Feed} object aggregates one or more {@link FeedItem}s.
 */
public class Feed {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "Feed";
    private static final String FEED_SURFACE_URI_KEY = "surfaceUri";
    private static final String FEED_ITEMS_KEY = "items";
    private static final String FEED_NAME_KEY = "feedName";

    // Friendly name for the feed, provided in the AJO UI
    private final String name;

    // Identification for this feed, represented by the AJO Surface URI used to retrieve it
    private final Surface surface;

    // List of FeedItem that are members of this Feed
    private final List<FeedItem> items;

    /**
     * Constructor.
     *
     * @param name    {@link String} containing the friendly name for the feed, provided in the AJO UI
     * @param surface {@link String} containing the AJO Surface URI used to retrieve the feed
     * @param items   {@link List<FeedItem>} that are members of this {@link Feed}
     */
    public Feed(final String name, final Surface surface, final List<FeedItem> items) {
        this.name = name;
        this.surface = surface;
        this.items = items;
    }

    /**
     * Gets the {@code Feed}'s friendly name.
     *
     * @return {@link String} containing the friendly {@link Feed} name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the {@code Feed}'s surface uri.
     *
     * @return {@link String} containing the {@link Feed} surface uri.
     */
    public String getSurfaceUri() {
        return surface == null ? null : surface.getUri();
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

        final List<Map<String, Object>> feedItemEventDataList = new ArrayList<>();
        final List<FeedItem> feedItemList = getItems();
        if (feedItemList == null || feedItemList.isEmpty()) {
            return feedAsMap;
        }
        for (final FeedItem feedItem : feedItemList) {
            if (feedItem != null) {
                feedItemEventDataList.add(feedItem.toEventData());
            }
        }
        feedAsMap.put(FEED_ITEMS_KEY, feedItemEventDataList);
        feedAsMap.put(FEED_NAME_KEY, getName());
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
        final Surface surface = Surface.fromUriString(feedEntry.getKey());
        final List<Map<String, Object>> feedMaps = (List<Map<String, Object>>) feedEntry.getValue();
        if (feedMaps == null || feedMaps.isEmpty()) {
            return null;
        }
        final List<FeedItem> feedItems = new ArrayList<>();
        String feedName = null;
        for (final Map feedItemObject : feedMaps) {
            final Proposition proposition = Proposition.fromEventData(feedItemObject);
            final String ruleContentString = proposition.getItems().get(0).getContent();
            feedItems.add(FeedItem.fromEventData(getFeedItemData(ruleContentString)));
            feedName = getFeedName(ruleContentString);
        }

        return new Feed(feedName, surface, feedItems);
    }

    private static JSONObject getData(final String ruleContent) {
        JSONObject data = null;
        try {
            final JSONObject ruleContentJSON = new JSONObject(ruleContent);
            final JSONArray rules = ruleContentJSON.getJSONArray("rules");
            final JSONObject ruleJSON = rules.getJSONObject(0);
            final JSONArray consequenceArray = ruleJSON.getJSONArray("consequences");
            final JSONObject consequence = consequenceArray.getJSONObject(0);
            final JSONObject details = consequence.getJSONObject("detail");
            data = details.getJSONObject("data");
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, SELF_TAG, "Exception occurred retrieving rule consequence data: %s", jsonException.getLocalizedMessage());
        }
        return data;
    }

    private static String getFeedName(final String ruleContent) {
        try {
            final JSONObject data = getData(ruleContent);
            final JSONObject metadata = data.getJSONObject("meta");
            return metadata.getString("feedName");
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, SELF_TAG, "Exception occurred retrieving feed item data: %s", jsonException.getLocalizedMessage());
        }
        return null;
    }

    private static Map<String, Object> getFeedItemData(final String ruleContent) {
        JSONObject feedItemData;
        try {
            final JSONObject data = getData(ruleContent);
            feedItemData = data.getJSONObject("content");
            return JSONUtils.toMap(feedItemData);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, SELF_TAG, "Exception occurred retrieving feed item data: %s", jsonException.getLocalizedMessage());
        }
        return null;
    }
}
