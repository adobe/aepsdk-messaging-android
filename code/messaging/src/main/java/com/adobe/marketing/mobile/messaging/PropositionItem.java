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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.ConsequenceDetailKeys.SCHEMA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
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
 * A {@link PropositionItem} object represents a personalization JSON object returned by Konductor.
 * In its JSON form, it has the following properties: id, schema, and data.
 * The contents of data will be determined by the provided schema.
 * This class provides helper access to get strongly typed content - e.g. `getHtmlContent`
 */
public class PropositionItem implements Serializable {
    private static final String SELF_TAG = "MessagingPropositionItem";
    /// Unique identifier for this `PropositionItem`
    /// contains value for `id` in JSON
    private String itemId;

    /// `PropositionItem` schema string
    /// contains value for `schema` in JSON
    private SchemaType schema;

    // PropositionItem data map containing the JSON data
    private Map<String, Object> itemData;

    // Soft reference to Proposition instance
    SoftReference<Proposition> propositionReference;

    public PropositionItem(final String itemId, final SchemaType schema, final Map<String, Object> itemData) {
        this.itemId = itemId;
        this.schema = schema;
        this.itemData = itemData;
    }

    /**
     * Gets the {@code MessagingPropositionItem} identifier.
     *
     * @return {@link String} containing the {@link PropositionItem} identifier.
     */
    public String getPropositionItemId() {
        return itemId;
    }

    /**
     * Gets the {@code MessagingPropositionItem} content schema.
     *
     * @return {@link SchemaType} containing the {@link PropositionItem} content schema.
     */
    public SchemaType getSchema() {
        return schema;
    }

    /**
     * Gets the {@code MessagingPropositionItem} data.
     *
     * @return {@licodenk Map<String, Object>} containing the {@link PropositionItem} data.
     */
    public Map<String, Object> getData() {
        return itemData;
    }

    /**
     * Gets the {@code MessagingProposition} referenced by the Proposition {@code SoftReference}.
     *
     * @return {@link Proposition} referenced by the Proposition {@link SoftReference}.
     */
    public Proposition getProposition() {
        return propositionReference.get();
    }

    /**
     * Tracks interaction with the given proposition item.
     *
     * @param eventType enum of type {@link MessagingEdgeEventType} specifying event type for the interaction
     */
    public void track(@NonNull final MessagingEdgeEventType eventType) {
        final Map<String, Object> propositionInteractionXdm = generateInteractionXdm(eventType);
        if (propositionInteractionXdm == null) {
            Log.debug(LOG_TAG, SELF_TAG, "Cannot track proposition interaction for item (%s), could not generate interactions XDM.", itemId);
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_PROPOSITIONS, true);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.PROPOSITION_INTERACTION, propositionInteractionXdm);

        final Event trackingPropositionsEvent = new Event.Builder(MessagingConstants.EventName.TRACK_PROPOSITIONS,
                MessagingConstants.EventType.MESSAGING,
                MessagingConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
        MobileCore.dispatchEvent(trackingPropositionsEvent);
    }

    /**
     * Tracks interaction with the given proposition item.
     *
     * @param interaction {@link String} describing the interaction.
     * @param eventType enum of type {@link MessagingEdgeEventType} specifying event type for the interaction
     * @param tokens {@link List<String>} containing the sub-item tokens for recording interaction.
     */
    void track(final String interaction, @NonNull final MessagingEdgeEventType eventType, final List<String> tokens) {
        final Map<String, Object> propositionInteractionXdm = generateInteractionXdm(interaction, eventType, tokens);
        if (propositionInteractionXdm == null) {
            Log.debug(LOG_TAG, SELF_TAG, "Cannot track proposition interaction for item (%s), could not generate interactions XDM.", itemId);
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Messaging.TRACK_PROPOSITIONS, true);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.PROPOSITION_INTERACTION, propositionInteractionXdm);

        final Event trackingPropositionsEvent = new Event.Builder(MessagingConstants.EventName.TRACK_PROPOSITIONS,
                MessagingConstants.EventType.MESSAGING,
                MessagingConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();
        MobileCore.dispatchEvent(trackingPropositionsEvent);
    }

    /**
     * Creates a {@code Map<String, Object} containing XDM data for interaction with the given proposition item, for the provided event type.
     * If the proposition reference within the item is released and no longer valid, the method returns null.
     *
     * @param eventType an enum of type {@link MessagingEdgeEventType} specifying event type for the interaction.
     * @return {code Map<String, Object} containing XDM data for the proposition interaction.
     */
    public Map<String, Object> generateInteractionXdm(@NonNull final MessagingEdgeEventType eventType) {
        if (propositionReference == null) {
            Log.debug(LOG_TAG, SELF_TAG,  "Cannot generate interaction XDM for item (%s), proposition reference is not available.", itemId);
            return null;
        }
        final PropositionInteraction propositionInteraction = new PropositionInteraction(eventType, null,
                PropositionInfo.createFromProposition(getProposition()), itemId, null);
        return propositionInteraction.getPropositionInteractionXDM();
    }

    /**
     * Creates a {@code Map<String, Object} containing XDM data for interaction with the given proposition item, for the provided event type.
     * If the proposition reference within the item is released and no longer valid, the method returns null.
     *
     * @param interaction custom {@link String} describing the interaction.
     * @param eventType an enum of type {@link MessagingEdgeEventType} specifying event type for the interaction.
     * @param tokens {@link List<String>} containing the sub-item tokens for recording interaction.
     * @return {code Map<String, Object} containing XDM data for the proposition interaction.
     */
    public Map<String, Object> generateInteractionXdm(final String interaction, @NonNull final MessagingEdgeEventType eventType, final List<String> tokens) {
        if (propositionReference == null) {
            Log.debug(LOG_TAG, SELF_TAG,  "Cannot generate interaction XDM for item (%s), proposition reference is not available.", itemId);
            return null;
        }
        final PropositionInteraction propositionInteraction = new PropositionInteraction(eventType, interaction,
                PropositionInfo.createFromProposition(getProposition()), itemId, tokens);
        return propositionInteraction.getPropositionInteractionXDM();
    }

    /**
     * Creates a {@code MessagingPropositionItem} object from the provided {@code RuleConsequence}.
     *
     * @return {@link PropositionItem} object created from the provided {@link RuleConsequence}.
     */
    public static PropositionItem fromRuleConsequence(final RuleConsequence consequence) {
        PropositionItem propositionItem = null;
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
            propositionItem = new PropositionItem(uniqueId, SchemaType.fromString(schema), data);
        } catch (final DataReaderException dataReaderException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating MessagingPropositionItem from rule consequence: %s", dataReaderException.getLocalizedMessage());
        }

        return propositionItem;
    }

    /**
     * Returns this {@link PropositionItem}'s content as a json content {@code Map<String, Object>}.
     *
     * @return {@link Map<String, Object>} object containing the {@link PropositionItem}'s content.
     */
    public Map<String, Object> getJsonContentMap() {
        final JsonContentSchemaData schemaData = (JsonContentSchemaData) createSchemaData(SchemaType.JSON_CONTENT);
        return schemaData != null ? schemaData.getJsonObjectContent() : null;
    }

    /**
     * Returns this {@link PropositionItem}'s content as a json content {@code List<Map<String, Object>>}.
     *
     *
     * @return {@link List<Map<String, Object>>} object containing the {@link PropositionItem}'s content.
     */
    public List<Map<String, Object>> getJsonArrayList() {
        final JsonContentSchemaData schemaData = (JsonContentSchemaData) createSchemaData(SchemaType.JSON_CONTENT);
        return schemaData != null ? schemaData.getJsonArrayContent() : null;
    }

    /**
     * Returns this {@link PropositionItem}'s content as a html content {@code String}.
     *
     * @return {@link String} containing the {@link PropositionItem}'s content.
     */
    public String getHtmlContent() {
        final HtmlContentSchemaData schemaData = (HtmlContentSchemaData) createSchemaData(SchemaType.HTML_CONTENT);
        return schemaData != null ? schemaData.getContent() : null;
    }

    /**
     * Returns this {@link PropositionItem}'s content as a {@code InAppSchemaData} object.
     *
     * @return {@link InAppSchemaData} object containing the {@link PropositionItem}'s content.
     */
    public InAppSchemaData getInAppSchemaData() {
        return (InAppSchemaData) createSchemaData(SchemaType.INAPP);
    }

    /**
     * Returns this {@link PropositionItem}'s content as a {@code FeedItemSchemaData} object.
     *
     * @return {@link FeedItemSchemaData} object containing the {@link PropositionItem}'s content.
     */
    public FeedItemSchemaData getFeedItemSchemaData() {
        return (FeedItemSchemaData) createSchemaData(SchemaType.FEED);
    }

    /**
     * Creates a schema data object from this {@code MessagingPropositionItem}'s content.
     *
     * @param schemaType {@link SchemaType} to be used when creating the {@link SchemaData} object.
     * @return {@code SchemaData} object created from the provided {@link PropositionItem}'s content.
     */
    private SchemaData createSchemaData(final SchemaType schemaType) {
        if (MapUtils.isNullOrEmpty(itemData)) {
            Log.trace(LOG_TAG, SELF_TAG, "Cannot decode content, MessagingPropositionItem data is null or empty.");
            return null;
        }

        final JSONObject ruleJson = new JSONObject(itemData);
        switch (schemaType) {
            case UNKNOWN:
            case DEFAULT_CONTENT:
            case NATIVE_ALERT:
            case RULESET:
                break;
            case HTML_CONTENT:
                return new HtmlContentSchemaData(ruleJson);
            case JSON_CONTENT:
                return new JsonContentSchemaData(ruleJson);
            case INAPP:
                return new InAppSchemaData(ruleJson);
            case FEED:
                return new FeedItemSchemaData(ruleJson);
        }
        return null;
    }

    /**
     * Creates a {@code MessagingPropositionItem} object from the provided {@code Map<String, Object>}.
     *
     * @param eventData {@link Map<String, Object>} event data
     * @return {@link PropositionItem} object created from the provided {@code Map<String, Object>}.
     */
    public static PropositionItem fromEventData(final Map<String, Object> eventData) {
        PropositionItem propositionItem = null;
        try {
            final String uniqueId = DataReader.getString(eventData, ID);
            final SchemaType schema = SchemaType.fromString(DataReader.getString(eventData, SCHEMA));

            final Map<String, Object> dataMap = DataReader.getTypedMap(Object.class, eventData, DATA);
            if (MapUtils.isNullOrEmpty(dataMap)) {
                Log.trace(LOG_TAG, SELF_TAG, "Cannot create MessagingPropositionItem, event data is null or empty.");
                return null;
            }
            propositionItem = new PropositionItem(uniqueId, schema, dataMap);
        } catch (final DataReaderException exception) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception caught while attempting to create a MessagingPropositionItem from an event data map: %s", exception.getLocalizedMessage());
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
        if (MapUtils.isNullOrEmpty(itemData)) {
            Log.trace(LOG_TAG, SELF_TAG, "MessagingPropositionItem content is null or empty, cannot create event data map.");
            return eventData;
        }

        eventData.put(ID, this.itemId);
        eventData.put(SCHEMA, this.schema.toString());
        eventData.put(DATA, itemData);

        return eventData;
    }

    private void readObject(final ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        itemId = objectInputStream.readUTF();
        schema = SchemaType.fromString(objectInputStream.readUTF());
        itemData = (Map<String, Object>) objectInputStream.readObject();
        propositionReference = new SoftReference<>((Proposition) objectInputStream.readObject());
    }

    private void writeObject(final ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeUTF(itemId);
        objectOutputStream.writeUTF(schema.toString());
        objectOutputStream.writeObject(itemData);
        objectOutputStream.writeObject(propositionReference.get());
    }
}