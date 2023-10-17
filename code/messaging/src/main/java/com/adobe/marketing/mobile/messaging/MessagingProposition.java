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
 * A {@link MessagingProposition} object encapsulates offers and the information needed for tracking offer interactions.
 */
public class MessagingProposition implements Serializable {
    private static final String LOG_TAG = "Messaging";
    private static final String SELF_TAG = "Proposition";
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
    private final List<MessagingPropositionItem> messagingPropositionItems;

    public MessagingProposition(final String uniqueId, final String scope, final Map<String, Object> scopeDetails, final List<MessagingPropositionItem> messagingPropositionItems) {
        this.uniqueId = uniqueId;
        this.scope = scope;
        this.scopeDetails = scopeDetails;
        this.messagingPropositionItems = messagingPropositionItems;
        for (final MessagingPropositionItem item : this.messagingPropositionItems) {
            if (item.propositionReference == null) {
                item.propositionReference = new SoftReference<>(this);
            }
        }
    }

    /**
     * Gets the {@code Proposition} identifier.
     *
     * @return {@link String} containing the {@link MessagingProposition} identifier.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code Proposition} items.
     *
     * @return {@code List<PropositionItem>} containing the {@link MessagingProposition} items.
     */
    public List<MessagingPropositionItem> getItems() {
        return messagingPropositionItems;
    }

    /**
     * Gets the {@code Proposition} scope.
     *
     * @return {@link String} containing the encoded {@link MessagingProposition} scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Gets the {@code Proposition} scope details.
     *
     * @return {@code Map<String, Object>} containing the {@link MessagingProposition} scope details.
     */
    public Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }

    /**
     * Creates a {@code Proposition} object from the provided {@code Map<String, Object>}.
     *
     * @return {@link MessagingProposition} object created from the provided {@link Map<String, Object>}.
     */
    public static MessagingProposition fromEventData(final Map<String, Object> eventData) {
        MessagingProposition messagingProposition = null;
        try {
            final String uniqueId = DataReader.getString(eventData, PAYLOAD_ID);
            final String scope = DataReader.getString(eventData, PAYLOAD_SCOPE);
            final Map<String, Object> scopeDetails = DataReader.getTypedMap(Object.class, eventData, PAYLOAD_SCOPE_DETAILS);
            final List<Map<String, Object>> items = DataReader.getTypedListOfMap(Object.class, eventData, PAYLOAD_ITEMS);
            final List<MessagingPropositionItem> messagingPropositionItems = new ArrayList<>();
            for (final Map<String, Object> item : items) {
                final MessagingPropositionItem messagingPropositionItem = MessagingPropositionItem.fromEventData(item);
                if (messagingPropositionItem != null) {
                    messagingPropositionItems.add(messagingPropositionItem);
                }
            }
            messagingProposition = new MessagingProposition(uniqueId, scope, scopeDetails, messagingPropositionItems);
        } catch (final DataReaderException dataReaderException) {
            Log.trace(LOG_TAG, SELF_TAG, "Exception occurred creating proposition from event data map: %s", dataReaderException.getLocalizedMessage());
        }

        return messagingProposition;
    }

    /**
     * Creates a {@code Map<String, Object>} object from this {@code Proposition}.
     *
     * @return {@link Map<String, Object>} object created from this {@link MessagingProposition}.
     */
    public Map<String, Object> toEventData() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(PAYLOAD_ID, this.uniqueId);
        eventData.put(PAYLOAD_SCOPE, this.scope);
        eventData.put(PAYLOAD_SCOPE_DETAILS, this.scopeDetails);
        final List<Map<String, Object>> items = new ArrayList<>();
        for (final MessagingPropositionItem messagingPropositionItem : this.messagingPropositionItems) {
            items.add(messagingPropositionItem.toEventData());
        }
        eventData.put(PAYLOAD_ITEMS, items);
        return eventData;
    }

    public boolean equals(final Object object){
        if (object instanceof MessagingProposition) {
            final MessagingProposition proposition = (MessagingProposition) object;
            final String newPropositionContent = proposition.getItems().get(0).getContent();
            final String propositionContent = this.getItems().get(0).getContent();
            return newPropositionContent.equals(propositionContent);
        } else {
            return false;
        }
    }
}