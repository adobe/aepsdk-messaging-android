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

import java.util.List;

/** A {@link Feed} object aggregates one or more {@link FeedItem}s. */
public class Feed {
    // Friendly name for the feed, provided in the AJO UI
    private final String name;

    // Identification for this feed, represented by the AJO Surface URI used to retrieve it
    private final Surface surface;

    // List of FeedItem that are members of this Feed
    private final List<FeedItem> items;

    /**
     * Constructor.
     *
     * @param name {@link String} containing the friendly name for the feed, provided in the AJO UI
     * @param surface {@link String} containing the AJO Surface URI used to retrieve the feed
     * @param items {@link List<FeedItem>} that are members of this {@link Feed}
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
}
