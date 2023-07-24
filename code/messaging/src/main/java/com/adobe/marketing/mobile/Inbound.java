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

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link Inbound} object represents the common response (consequence) for AJO Campaigns targeting inbound channels.
 */
public class Inbound {
    // String representing a unique ID for this inbound item
    private final String uniqueId;
    // Enum representing the inbound item type
    private final InboundType inboundType;
    // Content for this inbound item e.g. inapp html string, or feed item JSON
    private final String content;
    // Contains mime type for this inbound item
    private final String contentType;
    // Represents when this inbound item went live. Represented in seconds since January 1, 1970
    private final int publishedDate;
    // Represents when this inbound item expires. Represented in seconds since January 1, 1970
    private final int expiryDate;
    // Contains additional key-value pairs associated with this inbound item
    private final Map<String, Object> meta;

    /**
     * Constructor.
     */
    public Inbound(final String uniqueId, final InboundType inboundType, final String content, final String contentType, final int publishedDate, final int expiryDate, final Map<String, Object> meta) {
        this.uniqueId = uniqueId != null ? uniqueId : "";
        this.inboundType = inboundType != null ? inboundType : InboundType.UNKNOWN;
        this.content = content != null ? content : "";
        this.contentType = contentType != null ? contentType : "";
        this.expiryDate = Math.max(expiryDate, 0);
        this.publishedDate = Math.max(publishedDate, 0);
        this.meta = meta != null ? meta : new HashMap<>();
    }

    /**
     * Gets the {@code Inbound} identifier.
     *
     * @return {@link String} containing the {@link Inbound} identifier.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code Inbound} type.
     *
     * @return {@code InboundType} describing the {@link Inbound}'s type.
     */
    public InboundType getInboundType() {
        return inboundType;
    }

    /**
     * Gets the {@code Inbound} content.
     *
     * @return {@link String} containing the {@link Inbound} content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the {@code Inbound} content type.
     *
     * @return {@link String} containing the {@link Inbound} content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the {@code Inbound} content's publish date.
     *
     * @return {@code int} describing when the {@link Inbound} content went live. Represented in seconds since January 1, 1970.
     */
    public int getPublishedDate() {
        return publishedDate;
    }

    /**
     * Gets the {@code Inbound} content's expiry date.
     *
     * @return {@code int} describing when the {@link Inbound} content will expire. Represented in seconds since January 1, 1970.
     */
    public int getExpiryDate() {
        return expiryDate;
    }

    /**
     * Gets the {@code Inbound} content meta.
     *
     * @return {@code Map<String, Object>} containing the {@link Inbound} content meta.
     */
    public Map<String, Object> getMeta() {
        return meta;
    }
}