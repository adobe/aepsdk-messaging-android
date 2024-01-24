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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.SchemaValues.SCHEMA_JSON_CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SchemaValues.SCHEMA_RULESET_ITEM;

import com.adobe.marketing.mobile.PropositionEventType;
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
import java.util.List;
import java.util.Map;

/**
 * A {@link MessagingPropositionItem} object contains the experience content delivered within an {@link Inbound} payload.
 */
public class MessagingPropositionItem implements Serializable {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "PropositionItem";
    private static final String JSON_RULES_KEY = "rules";
    private static final String JSON_CONSEQUENCES_KEY = "consequences";
    private static final String MESSAGE_CONSEQUENCE_ID = "id";
    private static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_SCHEMA = "schema";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_CONTENT = "content";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_DATA = "data";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE = "contentType";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE = "publishedDate";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE = "expiryDate";
    private static final String MESSAGE_CONSEQUENCE_DETAIL_METADATA = "meta";
    private static final String PAYLOAD_ID = "id";
    private static final String PAYLOAD_DATA = "data";
    private static final String PAYLOAD_CONTENT = "content";
    private static final String PAYLOAD_SCHEMA = "schema";

    // Unique proposition identifier
    private String uniqueId;
    // PropositionItem schema string
    private String schema;
    // PropositionItem data content e.g. html or plain-text string or string containing image URL, JSON string
    private String content;
    // Soft reference to Proposition instance
    SoftReference<MessagingProposition> propositionReference;

    public MessagingPropositionItem(final String uniqueId, final String schema, final String content) {
        this.uniqueId = uniqueId;
        this.schema = schema;
        this.content = content;
    }

    /**
     * Gets the {@code PropositionItem} identifier.
     *
     * @return {@link String} containing the {@link MessagingPropositionItem} identifier.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code PropositionItem} content schema.
     *
     * @return {@code String} containing the {@link MessagingPropositionItem} content schema.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Gets the {@code PropositionItem} content.
     *
     * @return {@link String} containing the {@link MessagingPropositionItem} content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the {@code Proposition} referenced by the Proposition {@code SoftReference}.
     *
     * @return {@link MessagingProposition} referenced by the Proposition {@link SoftReference}.
     */
    public MessagingProposition getProposition() {
        return propositionReference.get();
    }

    // Track offer interaction
    void track(final PropositionEventType interaction) {
        // TODO
    }

    /**
     * Creates an {@code Inbound} object from this {@code PropositionItem}'s content.
     *
     * @return {@link Inbound} object created from this {@link MessagingPropositionItem}'s content.
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
                    final JSONObject data = consequenceDetails.getJSONObject(MESSAGE_CONSEQUENCE_DETAIL_DATA);
                    final String content = data.getJSONObject(MESSAGE_CONSEQUENCE_DETAIL_CONTENT).toString();
                    final String contentType = data.getString(MESSAGE_CONSEQUENCE_DETAIL_CONTENT_TYPE);
                    final int expiryDate = data.getInt(MESSAGE_CONSEQUENCE_DETAIL_EXPIRY_DATE);
                    final int publishedDate = data.getInt(MESSAGE_CONSEQUENCE_DETAIL_PUBLISHED_DATE);
                    final Map<String, Object> meta = JSONUtils.toMap(data.getJSONObject(MESSAGE_CONSEQUENCE_DETAIL_METADATA));
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
     * @return {@link MessagingPropositionItem} object created from the provided {@code Map<String, Object>}.
     */
    public static MessagingPropositionItem fromEventData(final Map<String, Object> eventData) {
        MessagingPropositionItem propositionItem = null;
        try {
            final String uniqueId = DataReader.getString(eventData, PAYLOAD_ID);
            final String schema = DataReader.getString(eventData, PAYLOAD_SCHEMA);
            final Map<String, Object> contentMap = DataReader.getTypedMap(Object.class, eventData, PAYLOAD_DATA);

            JSONObject jsonContent = null;
            JSONArray jsonArray = null;
            String content = null;
            if (schema.equals(SCHEMA_RULESET_ITEM)) {
                // in-app content
                jsonContent = new JSONObject(contentMap);
            } else if (schema.equals(SCHEMA_JSON_CONTENT)) {
                // feed or code based json content
                final Object contentObject = contentMap.get(PAYLOAD_CONTENT);
                if (contentObject != null) {
                    // create a json array for list content. Json objects are created for map or string content.
                    if (contentObject instanceof Map) {
                        jsonContent = new JSONObject((Map) contentObject);
                    } else if (contentObject instanceof List) {
                        jsonArray = new JSONArray((List) contentObject);
                    } else {
                        jsonContent = new JSONObject(contentObject.toString());
                    }
                }
            } else { // html or text content
                content = DataReader.getString(contentMap, PAYLOAD_CONTENT);
            }

            if (jsonContent != null && jsonContent.length() > 0) {
                content = jsonContent.toString();
            } else if (jsonArray != null && jsonArray.length() > 0) {
                content = jsonArray.toString();
            }

            if (!StringUtils.isNullOrEmpty(content)) {
                propositionItem = new MessagingPropositionItem(uniqueId, schema, content);
            }
        } catch (final Exception exception) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception caught while attempting to create a PropositionItem from an event data map: %s", exception.getLocalizedMessage());
        }

        return propositionItem;
    }

    /**
     * Creates a {@code Map<String, Object>} object from this {@code PropositionItem}.
     *
     * @return {@link Map<String, Object>} object created from this {@link MessagingPropositionItem}.
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

        try {
            if (schema.equals(SCHEMA_RULESET_ITEM) || schema.equals(SCHEMA_JSON_CONTENT)) { // in-app, feed, or code based content
                if (content.startsWith("[")) { // we have a json array
                    final JSONArray jsonArray = new JSONArray(content);
                    data.put(PAYLOAD_CONTENT, JSONUtils.toList(jsonArray));
                } else { // handle it as a json object
                    final JSONObject jsonContent = new JSONObject(content);
                    data.put(PAYLOAD_CONTENT, JSONUtils.toMap(jsonContent));
                }
            } else { // html or text content
                data.put(PAYLOAD_CONTENT, content);
            }
        } catch (final JSONException jsonException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception caught while attempting to create event data from a Proposition Item: %s", jsonException.getLocalizedMessage());
        }

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
        propositionReference = new SoftReference<>((MessagingProposition) objectInputStream.readObject());
    }

    private void writeObject(final ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(uniqueId);
        objectOutputStream.writeUTF(schema);
        objectOutputStream.writeUTF(content);
        objectOutputStream.writeObject(propositionReference.get());
    }
}