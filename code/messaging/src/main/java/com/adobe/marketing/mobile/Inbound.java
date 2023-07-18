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

import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Map;

public class Inbound implements Serializable {
    // String representing a unique ID for this inbound item
    private String uniqueId;
    // Enum representing the inbound item type
    private InboundType inboundType;
    // Content for this inbound item e.g. inapp html string, or feed item JSON
    private String content;
    // Contains mime type for this inbound item
    private String contentType;
    // Represents when this feed item went live. Represented in seconds since January 1, 1970
    private long publishedDate;
    // Represents when this feed item expires. Represented in seconds since January 1, 1970
    private long expiryDate;
    // Contains additional key-value pairs associated with this feed item
    private Map<String, Object> meta;

    /**
     * Constructor.
     */
    public Inbound(final String content) throws JSONException {
        final JSONObject jsonObject = new JSONObject(content);
        this.uniqueId = jsonObject.getString("uniqueId");
        this.inboundType = InboundType.valueOf(jsonObject.getString("inboundType"));
        this.content = jsonObject.getString("content");
        this.contentType = jsonObject.getString("contentType");
        this.publishedDate = jsonObject.getLong("publishedDate");
        this.expiryDate = jsonObject.getLong("expiryDate");
        this.meta = JSONUtils.toMap(jsonObject.getJSONObject("meta"));
    }

    /**
     * Gets the {@code Inbound} unique id.
     *
     * @return {@link String} containing the {@link Inbound} unique id.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code InboundType}.
     *
     * @return {@link String} containing the {@link InboundType}.
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
     * Gets the {@code Inbound} published date.
     *
     * @return {@code long} containing the {@link Inbound} published date.
     */
    public long getPublishedDate() {
        return publishedDate;
    }

    /**
     * Gets the {@code Inbound} expiry date.
     *
     * @return {@code long} containing the {@link Inbound} expiry date.
     */
    public long getExpiryDate() {
        return expiryDate;
    }

    /**1
     * Gets the {@code Inbound} metadata.
     *
     * @return {@code Map<String, Object>} containing the {@link Inbound} metadata.
     */
    public Map<String, Object> getMeta() {
        return meta;
    }
}
