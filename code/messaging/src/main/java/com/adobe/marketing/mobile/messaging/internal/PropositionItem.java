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

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_CONTENT;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_INBOUND_ITEM_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_METADATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.DATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.PayloadKeys.SCHEMA;

import com.adobe.marketing.mobile.PropositionEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A {@link PropositionItem} object contains the experience content delivered within an {@link Inbound} payload.
 */
class PropositionItem {
    private final static String SELF_TAG = "PropositionItem";
    // Unique proposition identifier
    private final String uniqueId;
    // PropositionItem schema string
    private final String schema;
    // PropositionItem data content e.g. html or plain-text string or string containing image URL, JSON string
    private final String content;
    // Weak reference to Proposition instance
    WeakReference<Proposition> proposition;

    PropositionItem(final Map<String, Object> item) throws DataReaderException {
        this.uniqueId = DataReader.getString(item, ID);
        this.schema = DataReader.getString(item, SCHEMA);
        final Map data = DataReader.getTypedMap(Object.class, item, DATA);
        this.content = DataReader.getString(data, CONTENT);
    }

    /**
     * Gets the {@code PropositionItem} identifier.
     *
     * @return {@link String} containing the {@link PropositionItem} identifier.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code PropositionItem} content schema.
     *
     * @return {@code String} containing the {@link PropositionItem} content schema.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Gets the {@code PropositionItem} content.
     *
     * @return {@link String} containing the {@link PropositionItem} content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the {@code Proposition} reference.
     *
     * @return {@link Proposition} instance {@link WeakReference}.
     */
    public Proposition getProposition() {
        return proposition.get();
    }

    // Track offer interaction
    void track(final PropositionEventType interaction) {
        // TODO
    }

    // Decode data content to generic inbound
    Inbound decodeContent() {
        Inbound inboundContent = null;
        try {
            final JSONObject ruleJson = new JSONObject(content);
            final JSONObject ruleConsequence = MessagingUtils.getConsequence(ruleJson);
            if (ruleConsequence != null) {
                final String uniqueId = ruleConsequence.getString(MESSAGE_CONSEQUENCE_ID);
                final InboundType inboundType = InboundType.getInboundTypeFromString(ruleConsequence.getString(MESSAGE_CONSEQUENCE_DETAIL_INBOUND_ITEM_TYPE));
                final JSONObject consequenceDetails = MessagingUtils.getConsequenceDetails(ruleJson);
                if (consequenceDetails != null) {
                    // content, content type, and expiry date are required
                    final String content = consequenceDetails.getString(MESSAGE_CONSEQUENCE_DETAIL_CONTENT);
                    final String contentType = consequenceDetails.getString(MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE);
                    final int expiryDate = consequenceDetails.getInt(MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE);
                    // published date and meta are optional
                    final int publishedDate = consequenceDetails.optInt(MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE);
                    final Map<String, Object> meta = JSONUtils.toMap(consequenceDetails.optJSONObject(MESSAGE_CONSEQUENCE_DETAIL_METADATA));
                    inboundContent = new Inbound(uniqueId, inboundType, content, contentType, publishedDate, expiryDate, meta);
                }
            }
        } catch (final JSONException e) {
            Log.trace(LOG_TAG, SELF_TAG, "JSONException caught while attempting to decode content: %s", e.getLocalizedMessage());
        }
        return inboundContent;
    }


}