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

package com.adobe.marketing.mobile.messaging.internal;

import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_INBOUND_ITEM_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_METADATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID;

/**
 * An {@link Inbound} object represents the common response (consequence) for AJO Campaigns targeting inbound channels.
 */
class Inbound {
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
    Inbound(final String ruleString) throws JSONException {
        final JSONObject ruleJson = new JSONObject(ruleString);
        final JSONObject ruleConsequence = MessagingUtils.getConsequence(ruleJson);
        if (ruleConsequence != null) {
            this.uniqueId = ruleConsequence.getString(MESSAGE_CONSEQUENCE_ID);
            this.inboundType = InboundType.getInboundTypeFromString(ruleConsequence.getString(MESSAGE_CONSEQUENCE_DETAIL_INBOUND_ITEM_TYPE));
            final JSONObject consequenceDetails = MessagingUtils.getConsequenceDetails(ruleJson);
            if (consequenceDetails != null) {
                this.content = consequenceDetails.getString(MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
                this.publishedDate = consequenceDetails.optLong(MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE);
                this.expiryDate = consequenceDetails.optLong(MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE);
                this.contentType = consequenceDetails.optString(MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE);
                this.meta = JSONUtils.toMap(consequenceDetails.optJSONObject(MESSAGE_CONSEQUENCE_DETAIL_METADATA));
            }
        }
    }

    /**
     * Gets the {@code Inbound} unique id.
     *
     * @return {@link String} containing the {@link Inbound} unique id.
     */
    String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code InboundType}.
     *
     * @return {@link String} containing the {@link InboundType}.
     */
    InboundType getInboundType() {
        return inboundType;
    }

    /**
     * Gets the {@code Inbound} content.
     *
     * @return {@link String} containing the {@link Inbound} content.
     */
    String getContent() {
        return content;
    }

    /**
     * Gets the {@code Inbound} content type.
     *
     * @return {@link String} containing the {@link Inbound} content type.
     */
    String getContentType() {
        return contentType;
    }

    /**
     * Gets the {@code Inbound} published date.
     *
     * @return {@code long} containing the {@link Inbound} published date.
     */
    long getPublishedDate() {
        return publishedDate;
    }

    /**
     * Gets the {@code Inbound} expiry date.
     *
     * @return {@code long} containing the {@link Inbound} expiry date.
     */
    long getExpiryDate() {
        return expiryDate;
    }

    /**1
     * Gets the {@code Inbound} metadata.
     *
     * @return {@code Map<String, Object>} containing the {@link Inbound} metadata.
     */
    Map<String, Object> getMeta() {
        return meta;
    }
}