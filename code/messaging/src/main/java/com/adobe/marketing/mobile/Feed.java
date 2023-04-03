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
import com.google.android.gms.common.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public class Feed {
    // Identification for this feed, represented by the AJO Surface URI used to retrieve it
    private final String surfaceUri;

    // Friendly name for the feed, provided in the AJO UI
    private final String name;

    // List of FeedItem that are members of this Feed
    private final List<FeedItem> items;

    /**
     * Constructor.
     *
     * @param surfaceUri {@link String} containing the AJO Surface URI used to retrieve the feed
     * @param items {@link List<FeedItem>} that are members of this {@link Feed}
     */
    public Feed(final String surfaceUri, final List<FeedItem> items) {
        this.surfaceUri = surfaceUri;
        this.items = items;
        if (!CollectionUtils.isEmpty(items)) {
            final Map<String, Object> metaMap = items.get(0).getMeta();
            this.name = DataReader.optString(metaMap, "feedName", "");
        } else {
            name = "";
        }
    }

    /**
     * Gets the {@code Feed}'s surface uri.
     *
     * @return {@link String} containing the {@link Feed} surface uri.
     */
    public String getSurfaceUri() {
        return surfaceUri;
    }

    /**
     * Gets the {@code Feed}'s friendly name
     *
     * @return {@link String} containing the {@link Feed} friendly name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the {@code Feed}'s {@code FeedItem}s
     *
     * @return {@link List<FeedItem>} containing the {@link Feed}'s {@link FeedItem}s
     */
    public List<FeedItem> getItems() {
        return items;
    }
}
