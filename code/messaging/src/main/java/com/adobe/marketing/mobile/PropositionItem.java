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
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PropositionItem} object contains the experience content delivered within an {@link Inbound} payload.
 */
class PropositionItem {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "PropositionItem";
    private static final String JSON_KEY = "rules";
    private static final String JSON_CONSEQUENCES_KEY = "consequences";
    private static final String MESSAGE_CONSEQUENCE_ID = "id";
    private static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_INBOUND_ITEM_TYPE = "type";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_CONTENT = "content";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE = "contentType";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE = "publishedDate";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE = "expiryDate";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_METADATA = "meta";
    private static final String PAYLOAD_ID = "id";
    private static final String PAYLOAD_DATA = "data";
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_SCHEMA = "schema";

    // Unique proposition identifier
    private final String uniqueId;
    // PropositionItem schema string
    private final String schema;
    // PropositionItem data content e.g. html or plain-text string or string containing image URL, JSON string
    private final String content;
    // Weak reference to Proposition instance
    WeakReference<Proposition> proposition;

    PropositionItem(final String uniqueId, final String schema, final String content) {
        this.uniqueId = uniqueId;
        this.schema = schema;
        this.content = content;
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

    /**
     * Creates an {@code Inbound} object from this {@code PropositionItem}'s content.
     *
     * @return {@link Inbound} object created from this {@link PropositionItem}'s content.
     */
    public Inbound decodeContent() {
        Inbound inboundContent = null;
        try {
            final JSONObject ruleJson = new JSONObject(content);
            final JSONObject ruleConsequence = getConsequence(ruleJson);
            if (ruleConsequence != null) {
                final String uniqueId = ruleConsequence.getString(MESSAGE_CONSEQUENCE_ID);
                final InboundType inboundType = InboundType.fromString(ruleConsequence.getString(MESSAGE_CONSEQUENCE_DETAIL_INBOUND_ITEM_TYPE));
                final JSONObject consequenceDetails = getConsequenceDetails(ruleJson);
                if (consequenceDetails != null) {
                    final String content = consequenceDetails.getString(MESSAGE_CONSEQUENCE_DETAIL_CONTENT);
                    final String contentType = consequenceDetails.getString(MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE);
                    final int expiryDate = consequenceDetails.getInt(MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE);
                    final int publishedDate = consequenceDetails.getInt(MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE);
                    final Map<String, Object> meta = JSONUtils.toMap(consequenceDetails.getJSONObject(MESSAGE_CONSEQUENCE_DETAIL_METADATA));
                    inboundContent = new Inbound(uniqueId, inboundType, content, contentType, publishedDate, expiryDate, meta);
                }
            }
        } catch (final JSONException e) {
            Log.trace(LOG_TAG, SELF_TAG, "JSONException caught while attempting to decode content: %s", e.getLocalizedMessage());
        }
        return inboundContent;
    }

    /**
     * Creates a {@code PropositionItem} object from the provided {@code Map<String, Object>}.
     *
     * @param eventData {@link Map<String, Object>} event data
     * @return {@link PropositionItem} object created from the provided {@code Map<String, Object>}.
     */
    public static PropositionItem fromEventData(final Map<String, Object> eventData) {
        final String uniqueId = DataReader.optString(eventData, PAYLOAD_ID, "");
        final String schema = DataReader.optString(eventData, PAYLOAD_SCHEMA, "");
        final Map<String, Object> data = DataReader.optTypedMap(Object.class, eventData, PAYLOAD_DATA, null);
        final String content = DataReader.optString(data, PAYLOAD_CONTENT, "");
        return new PropositionItem(uniqueId, schema, content);
    }

    /**
     * Creates a {@code Map<String, Object>} object from this {@code PropositionItem}.
     *
     * @return {@link Map<String, Object>} object created from this {@link PropositionItem}.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(PAYLOAD_ID, this.uniqueId);
        eventData.put(PAYLOAD_SCHEMA, this.schema);
        eventData.put(PAYLOAD_CONTENT, this.content);
        return eventData;
    }

    /**
     * Retrieves the consequence {@code JSONObject} from the passed in {@code JSONObject}.
     *
     * @param ruleJson A {@link JSONObject} containing an AJO rule payload
     * @return {@code JSONObject> containing the consequence extracted from the rule json
     */
    private JSONObject getConsequence(final JSONObject ruleJson) {
        JSONObject consequence = null;
        try {
            final JSONArray rulesArray = ruleJson.getJSONArray(JSON_KEY);
            final JSONArray consequenceArray = rulesArray.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            consequence = consequenceArray.getJSONObject(0);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving rule consequence: %s", jsonException.getLocalizedMessage());
        }
        return consequence;
    }

    /**
     * Retrieves the consequence detail {@code Map} from the passed in {@code JSONObject}.
     *
     * @param ruleJson A {@link JSONObject} containing an AJO rule payload
     * @return {@code JSONObject> containing the consequence details extracted from the rule json
     */
    private JSONObject getConsequenceDetails(final JSONObject ruleJson) {
        JSONObject consequenceDetails = null;
        try {
            final JSONObject consequence = getConsequence(ruleJson);
            if (consequence != null) {
                consequenceDetails = consequence.getJSONObject(MESSAGE_CONSEQUENCE_DETAIL);
            }
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving consequence details: %s", jsonException.getLocalizedMessage());
        }
        return consequenceDetails;
    }
}