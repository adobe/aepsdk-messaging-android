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

import java.util.Map;

public class FeedItem {
    // Plain-text title for the feed item
    private final String title;
    // Plain-text body representing the content for the feed item
    private final String body;
    // String representing a URI that contains an image to be used for this feed item
    private final String imageUrl;
    // Contains a URL to be opened if the user interacts with the feed item
    private final String actionUrl;
    // Required if actionUrl is provided. Text to be used in title of button or link in feed item
    private final String actionTitle;
    // Represents when this feed item went live. Represented in seconds since January 1, 1970
    private final long publishedDate;
    // Represents when this feed item expires. Represented in seconds since January 1, 1970
    private final long expiryDate;
    // Contains additional key-value pairs associated with this feed item
    private final Map<String, Object> meta;

    private FeedItem(final String title, final String body, final String imageUrl, final String actionUrl, final String actionTitle, final long publishedDate, final long expiryDate, final Map<String, Object> meta) {
        this.title = title;
        this.body = body;
        this.imageUrl = imageUrl;
        this.actionUrl = actionUrl;
        this.actionTitle = actionTitle;
        this.publishedDate = publishedDate;
        this.expiryDate = expiryDate;
        this.meta = meta;
    }

    public static class Builder {
        // required parameters
        private final String title;
        private final String body;
        private final long publishedDate;
        private final long expiryDate;
        // optional parameters
        private String imageUrl;
        private String actionUrl;
        private String actionTitle;
        private Map<String, Object> meta;

        Builder(final String title, final String body, final long publishedDate, final long expiryDate) {
            this.title = title;
            this.body = body;
            this.publishedDate = publishedDate;
            this.expiryDate = expiryDate;
        }

        public Builder setImageUrl(final String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder setActionUrl(final String actionUrl) {
            this.actionUrl = actionUrl;
            return this;
        }

        public Builder setActionTitle(final String actionTitle) {
            this.actionTitle = actionTitle;
            return this;
        }

        public Builder setMeta(final Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public FeedItem build() {
            if (StringUtils.isNullOrEmpty(title) || StringUtils.isNullOrEmpty(body) || publishedDate <= 0 || expiryDate <= 0) {
                return null;
            }
            return new FeedItem(title, body, imageUrl, actionUrl, actionTitle, publishedDate, expiryDate, meta);
        }
    }

    String getTitle() {
        return title;
    }

    String getBody() {
        return body;
    }

    String getImageUrl() {
        return imageUrl;
    }

    String getActionUrl() {
        return actionUrl;
    }

    String getActionTitle() {
        return actionTitle;
    }

    long getPublishedDate() {
        return publishedDate;
    }

    long getExpiryDate() {
        return expiryDate;
    }

    Map<String, Object> getMeta() {
        return meta;
    }
}
