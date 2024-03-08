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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Proposition} object encapsulates offers and the information needed for tracking offer interactions.
 */
public class Proposition implements Serializable {
    private static final String SELF_TAG = "MessagingProposition";
    private static final String PAYLOAD_ID = "id";
    private static final String PAYLOAD_ITEMS = "items";
    private static final String PAYLOAD_SCOPE = "scope";
    private static final String PAYLOAD_SCOPE_DETAILS = "scopeDetails";

    // Unique proposition identifier
    private final String uniqueId;
    // Scope string
    private final String scope;
    // Scope details map
    private final Map<String, Object> scopeDetails;
    // List containing proposition decision items
    private final List<PropositionItem> propositionItems;

    public Proposition(final String uniqueId, final String scope, final Map<String, Object> scopeDetails, final List<PropositionItem> propositionItems) {
        this.uniqueId = uniqueId;
        this.scope = scope;
        this.scopeDetails = scopeDetails;
        this.propositionItems = propositionItems;
        for (final PropositionItem item : this.propositionItems) {
            if (item.propositionReference == null) {
                item.propositionReference = new SoftReference<>(this);
            }
        }
    }

    /**
     * Gets the {@code MessagingProposition} identifier.
     *
     * @return {@link String} containing the {@link Proposition} identifier.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code MessagingPropositionItem} list.
     *
     * @return {@code List<MessagingPropositionItem>} containing the {@link PropositionItem}s.
     */
    public List<PropositionItem> getItems() {
        return propositionItems;
    }

    /**
     * Gets the {@code MessagingProposition} scope.
     *
     * @return {@link String} containing the encoded {@link Proposition} scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the {@code MessagingProposition} scope details.
     *
     * @return {@code Map<String, Object>} containing the {@link Proposition} scope details.
     */
    public Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }

    /**
     * Creates a {@code MessagingProposition} object from the provided {@code Map<String, Object>}.
     *
     * @return {@link Proposition} object created from the provided {@link Map<String, Object>}.
     */
    public static Proposition fromEventData(final Map<String, Object> eventData) {
        Proposition proposition = null;
        try {
            final String uniqueId = DataReader.getString(eventData, PAYLOAD_ID);
            final String scope = DataReader.getString(eventData, PAYLOAD_SCOPE);
            final Map<String, Object> scopeDetails = DataReader.getTypedMap(Object.class, eventData, PAYLOAD_SCOPE_DETAILS);
            final List<Map<String, Object>> items = DataReader.getTypedListOfMap(Object.class, eventData, PAYLOAD_ITEMS);
            final List<PropositionItem> propositionItems = new ArrayList<>();
            for (final Map<String, Object> item : items) {
                final PropositionItem propositionItem = PropositionItem.fromEventData(item);
                if (propositionItem != null) {
                    propositionItems.add(propositionItem);
                }
            }
            proposition = new Proposition(uniqueId, scope, scopeDetails, propositionItems);
        } catch (final DataReaderException dataReaderException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating MessagingProposition from event data map: %s", dataReaderException.getLocalizedMessage());
        }

        return proposition;
    }

    /**
     * Creates a {@code Map<String, Object>} object from this {@code MessagingProposition}.
     *
     * @return {@link Map<String, Object>} object created from this {@link Proposition}.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(PAYLOAD_ID, this.uniqueId);
        eventData.put(PAYLOAD_SCOPE, this.scope);
        eventData.put(PAYLOAD_SCOPE_DETAILS, this.scopeDetails);
        final List<Map<String, Object>> items = new ArrayList<>();
        for (final PropositionItem propositionItem : this.propositionItems) {
            items.add(propositionItem.toEventData());
        }
        eventData.put(PAYLOAD_ITEMS, items);
        return eventData;
    }

    public boolean equals(final Object object) {
        if (object instanceof Proposition) {
            final Proposition proposition = (Proposition) object;
            final Map<String, Object> newPropositionContent = proposition.getItems().get(0).getData();
            final Map<String, Object> propositionContent = this.getItems().get(0).getData();
            return newPropositionContent.equals(propositionContent);
        } else {
            return false;
        }
    }
}