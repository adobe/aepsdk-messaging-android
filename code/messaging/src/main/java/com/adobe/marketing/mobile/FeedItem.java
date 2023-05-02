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

import com.adobe.marketing.mobile.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class FeedItem {
    // Plain-text title for the feed item
    private String title;
    // Plain-text body representing the content for the feed item
    private String body;
    // String representing a URI that contains an image to be used for this feed item
    private String imageUrl;
    // Contains a URL to be opened if the user interacts with the feed item
    private String actionUrl;
    // Required if actionUrl is provided. Text to be used in title of button or link in feed item
    private String actionTitle;
    // Represents when this feed item went live. Represented in seconds since January 1, 1970
    private long publishedDate;
    // Represents when this feed item expires. Represented in seconds since January 1, 1970
    private long expiryDate;
    // Contains additional key-value pairs associated with this feed item
    private Map<String, Object> meta;

    /**
     * Private constructor.
     * <p>
     * Use {@link Builder} to create {@link FeedItem} object.
     */
    private FeedItem() {}

    /**
     * {@code FeedItem} Builder.
     */
    public static class Builder {
        private boolean didBuild;
        final private FeedItem feedItem;

        /**
         * Builder constructor with required {@code FeedItem} attributes as parameters.
         * <p>
         * It sets default values for the remaining {@link FeedItem} attributes.
         *
         * @param title required {@link String} plain-text title for the feed item
         * @param body required {@link String} plain-text body representing the content for the feed item
         * @param publishedDate required {@code long} represents when this feed item went live
         */
        public Builder(final String title, final String body, final long publishedDate) {
            feedItem = new FeedItem();
            feedItem.title = StringUtils.isNullOrEmpty(title) ? "" : title;
            feedItem.body = StringUtils.isNullOrEmpty(body) ? "" : body;
            feedItem.publishedDate = publishedDate <= 0 ? 0 : publishedDate;
            feedItem.expiryDate = 0;
            feedItem.imageUrl = "";
            feedItem.actionUrl = "";
            feedItem.actionTitle = "";
            feedItem.meta = new HashMap<>();
            didBuild = false;
        }

        /**
         * Sets the image url for this {@code FeedItem}.
         *
         * @param imageUrl {@link String} containing the {@link FeedItem} image url.
         * @return this FeedItem {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setImageUrl(final String imageUrl) {
            throwIfAlreadyBuilt();

            feedItem.imageUrl = imageUrl;
            return this;
        }

        /**
         * Sets the action url for this {@code FeedItem}.
         *
         * @param actionUrl {@link String} containing the {@link FeedItem} action url.
         * @return this FeedItem {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setActionUrl(final String actionUrl) {
            throwIfAlreadyBuilt();

            feedItem.actionUrl = actionUrl;
            return this;
        }

        /**
         * Sets the action title for this {@code FeedItem}.
         *
         * @param actionTitle {@link String} containing the {@link FeedItem} action title.
         * @return this FeedItem {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setActionTitle(final String actionTitle) {
            throwIfAlreadyBuilt();

            feedItem.actionTitle = actionTitle;
            return this;
        }

        /**
         * Sets the expiry date for this {@code FeedItem}.
         *
         * @param expiryDate {@link long} containing the {@link FeedItem}'s expiry date.
         * @return this FeedItem {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setExpiryDate(final long expiryDate) {
            throwIfAlreadyBuilt();

            feedItem.expiryDate = expiryDate;
            return this;
        }

        /**
         * Sets the metadata map for this {@code FeedItem}.
         *
         * @param meta {@code Map<String, Object>} containing {@link FeedItem} metadata.
         * @return this FeedItem {@link Builder}
         * @throws UnsupportedOperationException if this method is invoked after {@link Builder#build()}.
         */
        public Builder setMeta(final Map<String, Object> meta) {
            throwIfAlreadyBuilt();

            feedItem.meta = meta;
            return this;
        }

        /**
         * Builds and returns the {@code FeedItem} object.
         *
         * @return {@link FeedItem} object or null.
         */
        public FeedItem build() {
            // title, body, and published date are required
            if (StringUtils.isNullOrEmpty(feedItem.title) || StringUtils.isNullOrEmpty(feedItem.body) || feedItem.publishedDate <= 0) {
                return null;
            }

            throwIfAlreadyBuilt();
            didBuild = true;

            return feedItem;
        }

        private void throwIfAlreadyBuilt() {
            if (didBuild) {
                throw new UnsupportedOperationException("Attempted to call methods on FeedItem.Builder after build() was invoked.");
            }
        }
    }

    /**
     * Gets the {@code FeedItem} title.
     *
     * @return {@link String} containing the {@link FeedItem} title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the {@code FeedItem} body.
     *
     * @return {@link String} containing the {@link FeedItem} body.
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the {@code FeedItem} image url.
     *
     * @return {@link String} containing the {@link FeedItem} image url.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Gets the {@code FeedItem} action url.
     *
     * @return {@link String} containing the {@link FeedItem} action url.
     */
    public String getActionUrl() {
        return actionUrl;
    }

    /**
     * Gets the {@code FeedItem} action title.
     *
     * @return {@link String} containing the {@link FeedItem} action title.
     */
    public String getActionTitle() {
        return actionTitle;
    }

    /**
     * Gets the {@code FeedItem} published date.
     *
     * @return {@code long} containing the {@link FeedItem} published date.
     */
    public long getPublishedDate() {
        return publishedDate;
    }

    /**
     * Gets the {@code FeedItem} expiry date.
     *
     * @return {@code long} containing the {@link FeedItem} expiry date.
     */
    public long getExpiryDate() {
        return expiryDate;
    }

    /**
     * Gets the {@code FeedItem} metadata.
     *
     * @return {@code Map<String, Object>} containing the {@link FeedItem} metadata.
     */
    public Map<String, Object> getMeta() {
        return meta;
    }
}
