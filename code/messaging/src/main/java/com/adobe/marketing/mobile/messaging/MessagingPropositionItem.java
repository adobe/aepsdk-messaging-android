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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.CONTENT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.SCHEMA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;

import com.adobe.marketing.mobile.PropositionEventType;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;

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
 * A {@link MessagingPropositionItem} object represents a personalization JSON object returned by Konductor.
 * In its JSON form, it has the following properties: id, schema, and data.
 * The contents of data will be determined by the provided schema.
 */
public class MessagingPropositionItem implements Serializable {
    private static final String SELF_TAG = "MessagingPropositionItem";
    // proposition item identifier
    private String itemId;
    // SchemaType of this MessagingPropositionItem
    private SchemaType schema;
    // PropositionItem data map containing the JSON data
    private Map<String, Object> itemData;
    // Soft reference to Proposition instance
    SoftReference<MessagingProposition> propositionReference;

    public MessagingPropositionItem(final String itemId, final SchemaType schema, final Map<String, Object> itemData) {
        this.itemId = itemId;
        this.schema = schema;
        this.itemData = itemData;
    }

    /**
     * Gets the {@code MessagingPropositionItem} identifier.
     *
     * @return {@link String} containing the {@link MessagingPropositionItem} identifier.
     */
    public String getPropositionItemId() {
        return itemId;
    }

    /**
     * Gets the {@code MessagingPropositionItem} content schema.
     *
     * @return {@link SchemaType} containing the {@link MessagingPropositionItem} content schema.
     */
    public SchemaType getSchema() {
        return schema;
    }

    /**
     * Gets the {@code MessagingPropositionItem} data.
     *
     * @return {@licodenk Map<String, Object>} containing the {@link MessagingPropositionItem} data.
     */
    public Map<String, Object> getData() {
        return itemData;
    }

    /**
     * Gets the {@code MessagingProposition} referenced by the Proposition {@code SoftReference}.
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
     * Creates a {@code MessagingPropositionItem} object from the provided {@code RuleConsequence}.
     *
     * @return {@link MessagingPropositionItem} object created from the provided {@link RuleConsequence}.
     */
    public static MessagingPropositionItem fromRuleConsequence(final RuleConsequence consequence) {
        MessagingPropositionItem propositionItem = null;
        try {
            final Map<String, Object> details = consequence.getDetail();
            if (MapUtils.isNullOrEmpty(details)) {
                return null;
            }
            final String uniqueId = DataReader.getString(details, ID);
            final String schema = DataReader.getString(details, SCHEMA);
            final Map<String, Object> data = DataReader.getTypedMap(Object.class, details, DATA);
            if (MapUtils.isNullOrEmpty(data)) {
                return null;
            }
            propositionItem = new MessagingPropositionItem(uniqueId, SchemaType.fromString(schema), data);
        } catch (final DataReaderException dataReaderException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating MessagingPropositionItem from rule consequence: %s", dataReaderException.getLocalizedMessage());
        }

        return propositionItem;
    }

    /**
     * Returns this {@link MessagingPropositionItem}'s content as a json content {@code Map<String, Object>}.
     *
     * @return {@link Map<String, Object>} object containing the {@link MessagingPropositionItem}'s content.
     */
    public Map<String, Object> getJsonContentMap() {
        final JsonContentSchemaData schemaData = createSchemaData(SchemaType.JSON_CONTENT);
        return schemaData != null ? schemaData.getJsonObjectContent() : null;
    }

    /**
     * Returns this {@link MessagingPropositionItem}'s content as a json content {@code List<Map<String, Object>>}.
     *
     * @return {@link List<Map<String, Object>>} object containing the {@link MessagingPropositionItem}'s content.
     */
    public List<Map<String, Object>> getJsonArrayList() {
        final JsonContentSchemaData schemaData = createSchemaData(SchemaType.JSON_CONTENT);
        return schemaData != null ? schemaData.getJsonArrayContent() : null;
    }

    /**
     * Returns this {@link MessagingPropositionItem}'s content as a html content {@code String}.
     *
     * @return {@link String} containing the {@link MessagingPropositionItem}'s content.
     */
    public String getHtmlContent() {
        final HtmlContentSchemaData schemaData = createSchemaData(SchemaType.HTML_CONTENT);
        return schemaData != null ? schemaData.getContent().toString() : null;
    }

    /**
     * Returns this {@link MessagingPropositionItem}'s content as a {@code InAppSchemaData} object.
     *
     * @return {@link InAppSchemaData} object containing the {@link MessagingPropositionItem}'s content.
     */
    public InAppSchemaData getInAppSchemaData() {
        return createSchemaData(SchemaType.INAPP);
    }

    /**
     * Returns this {@link MessagingPropositionItem}'s content as a {@code FeedItemSchemaData} object.
     *
     * @return {@link FeedItemSchemaData} object containing the {@link MessagingPropositionItem}'s content.
     */
    public FeedItemSchemaData getFeedItemSchemaData() {
        return createSchemaData(SchemaType.FEED);
    }

    /**
     * Creates a schema data object from this {@code MessagingPropositionItem}'s content.
     *
     * @param schemaType {@link SchemaType} to be used when creating the {@link T} object.
     * @return {@code T} object created from the provided {@link MessagingPropositionItem}'s content.
     */
    private <T> T createSchemaData(final SchemaType schemaType) {
        if (MapUtils.isNullOrEmpty(itemData)) {
            Log.trace(LOG_TAG, SELF_TAG, "Cannot decode content, MessagingPropositionItem data is null or empty.");
            return null;
        }

        final JSONObject ruleJson = new JSONObject(itemData);
        switch (schemaType) {
            case UNKNOWN:
            case DEFAULT:
            case NATIVE_ALERT:
            case RULESET:
                break;
            case HTML_CONTENT:
                return (T) new HtmlContentSchemaData(ruleJson);
            case JSON_CONTENT:
                return (T) new JsonContentSchemaData(ruleJson);
            case INAPP:
                return (T) new InAppSchemaData(ruleJson);
            case FEED:
                return (T) new FeedItemSchemaData(ruleJson);
        }
        return null;
    }

    /**
     * Creates a {@code MessagingPropositionItem} object from the provided {@code Map<String, Object>}.
     *
     * @param eventData {@link Map<String, Object>} event data
     * @return {@link MessagingPropositionItem} object created from the provided {@code Map<String, Object>}.
     */
    public static MessagingPropositionItem fromEventData(final Map<String, Object> eventData) {
        MessagingPropositionItem propositionItem = null;
        try {
            final String uniqueId = DataReader.getString(eventData, ID);
            final SchemaType schema = SchemaType.fromString(DataReader.getString(eventData, SCHEMA));

            final Map<String, Object> dataMap = DataReader.getTypedMap(Object.class, eventData, DATA);
            if (MapUtils.isNullOrEmpty(dataMap)) {
                Log.trace(LOG_TAG, SELF_TAG, "Cannot create MessagingPropositionItem, event data is null or empty.");
                return null;
            }

            final Object content = dataMap.get(CONTENT);
            if (content == null) {
                Log.trace(LOG_TAG, SELF_TAG, "Cannot create MessagingPropositionItem, content is null or empty.");
                return null;
            }

            propositionItem = new MessagingPropositionItem(uniqueId, schema, dataMap);
        } catch (final DataReaderException exception) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception caught while attempting to create a MessagingPropositionItem from an event data map: %s", exception.getLocalizedMessage());
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
        if (MapUtils.isNullOrEmpty(itemData)) {
            Log.trace(LOG_TAG, SELF_TAG, "MessagingPropositionItem content is null or empty, cannot create event data map.");
            return eventData;
        }
        eventData.put(ID, this.itemId);
        eventData.put(SCHEMA, this.schema);
        eventData.put(DATA, itemData);

        return eventData;
    }

    private void readObject(final ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        itemId = objectInputStream.readUTF();
        schema = SchemaType.fromString(objectInputStream.readUTF());
        itemData = (Map<String, Object>) objectInputStream.readObject();
        propositionReference = new SoftReference<>((MessagingProposition) objectInputStream.readObject());
    }

    private void writeObject(final ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(itemId);
        objectOutputStream.writeUTF(schema.toString());
        objectOutputStream.writeObject(itemData);
        objectOutputStream.writeObject(propositionReference.get());
    }
}