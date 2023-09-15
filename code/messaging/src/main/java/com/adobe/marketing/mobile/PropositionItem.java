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
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link PropositionItem} object contains the experience content delivered within an {@link Inbound} payload.
 */
public class PropositionItem implements Serializable {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "PropositionItem";
    private static final String JSON_RULES_KEY = "rules";
    private static final String JSON_CONSEQUENCES_KEY = "consequences";
    private static final String MESSAGE_CONSEQUENCE_ID = "id";
    private static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_SCHEMA = "schema";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_CONTENT = "content";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE = "contentType";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE = "publishedDate";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE = "expiryDate";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_METADATA = "meta";
    private static final String PAYLOAD_ID = "id";
    private static final String PAYLOAD_DATA = "data";
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_SCHEMA = "schema";
    private static final String DATA_RULES_KEY = "rules";
    private static final String DATA_VERSION_KEY = "version";
    private static final String JSON_CONTENT_SCHEMA_VALUE = "https://ns.adobe.com/personalization/json-content-item";

    // Unique proposition identifier
    private String uniqueId;
    // PropositionItem schema string
    private String schema;
    // PropositionItem data content e.g. html or plain-text string or string containing image URL, JSON string
    private String content;
    // Soft reference to Proposition instance
    SoftReference<Proposition> propositionReference;

    public PropositionItem(final String uniqueId, final String schema, final String content) {
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
     * Gets the {@code Proposition} referenced by the Proposition {@code SoftReference}.
     *
     * @return {@link Proposition} referenced by the Proposition {@link SoftReference}.
     */
    public Proposition getProposition() {
        return propositionReference.get();
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
                final JSONObject consequenceDetails = getConsequenceDetails(ruleJson);
                if (consequenceDetails != null) {
                    final InboundType inboundType = InboundType.fromString(consequenceDetails.getString(MESSAGE_CONSEQUENCE_DETAIL_SCHEMA));
                    final String content = consequenceDetails.getJSONObject(MESSAGE_CONSEQUENCE_DETAIL_CONTENT).toString();
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
        PropositionItem propositionItem = null;
        try {
            final String uniqueId = DataReader.getString(eventData, PAYLOAD_ID);
            final String schema = DataReader.getString(eventData, PAYLOAD_SCHEMA);
            final Map<String, Object> contentMap = DataReader.getTypedMap(Object.class, eventData, PAYLOAD_DATA);

            String content = null;
            final Object data = contentMap.get(PAYLOAD_CONTENT);
            if (data instanceof String) {
                content = DataReader.getString(contentMap, PAYLOAD_CONTENT);
            } else if (data instanceof Map) {
                final JSONObject contentJSON = new JSONObject(DataReader.getTypedMap(Object.class, contentMap, PAYLOAD_CONTENT));
                if (contentJSON.length() > 0) {
                    content = contentJSON.toString();
                }
            }

            if (!StringUtils.isNullOrEmpty(content)) {
                propositionItem = new PropositionItem(uniqueId, schema, content);
            }
        } catch (final Exception exception) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception caught while attempting to create a PropositionItem from an event data map: %s", exception.getLocalizedMessage());
        }

        return propositionItem;
    }

    /**
     * Creates a {@code Map<String, Object>} object from this {@code PropositionItem}.
     *
     * @return {@link Map<String, Object>} object created from this {@link PropositionItem}.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> data = new HashMap<>();
        if (StringUtils.isNullOrEmpty(content)) {
            Log.trace(LOG_TAG, SELF_TAG, "PropositionItem content is null or empty, cannot create event data map.");
            return eventData;
        }
        eventData.put(PAYLOAD_ID, this.uniqueId);
        eventData.put(PAYLOAD_SCHEMA, this.schema);
        data.put(PAYLOAD_CONTENT, content);
        eventData.put(PAYLOAD_DATA, data);
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
            final JSONArray rules = ruleJson.getJSONArray(JSON_RULES_KEY);
            final JSONArray consequenceArray = rules.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            consequence = consequenceArray.getJSONObject(0);
        } catch (final JSONException jsonException) {
            Log.trace(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving rule consequence: %s", jsonException.getLocalizedMessage());
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
            Log.trace(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving consequence details: %s", jsonException.getLocalizedMessage());
        }
        return consequenceDetails;
    }

    private void readObject(final ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        uniqueId = objectInputStream.readUTF();
        schema = objectInputStream.readUTF();
        content = objectInputStream.readUTF();
        propositionReference = new SoftReference<>((Proposition) objectInputStream.readObject());
    }

    private void writeObject(final ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(uniqueId);
        objectOutputStream.writeUTF(schema);
        objectOutputStream.writeUTF(content);
        objectOutputStream.writeObject(propositionReference.get());
    }
}