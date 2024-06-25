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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Proposition} object encapsulates offers and the information needed for tracking offer
 * interactions.
 */
public class Proposition implements Serializable {
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
    private final List<PropositionItem> propositionItems = new ArrayList<>();

    public Proposition(
            @NonNull final String uniqueId,
            @NonNull final String scope,
            @NonNull final Map<String, Object> scopeDetails,
            @NonNull final List<PropositionItem> propositionItems)
            throws MessageRequiredFieldMissingException {
        if (StringUtils.isNullOrEmpty(uniqueId)
                || StringUtils.isNullOrEmpty(scope)
                || MapUtils.isNullOrEmpty(scopeDetails)
                || propositionItems == null) {
            throw new MessageRequiredFieldMissingException("Id, scope or scope details is missing");
        }
        this.uniqueId = uniqueId;
        this.scope = scope;
        this.scopeDetails = scopeDetails;
        this.propositionItems.addAll(propositionItems);
        for (final PropositionItem item : this.propositionItems) {
            if (item.propositionReference == null) {
                item.propositionReference = new SoftReference<>(this);
            }
        }
    }

    /**
     * Gets the {@code Proposition} identifier.
     *
     * @return {@link String} containing the {@link Proposition} identifier.
     */
    @NonNull public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Gets the {@code PropositionItem} list.
     *
     * @return {@code List<PropositionItem>} containing the {@link PropositionItem}s.
     */
    @NonNull public List<PropositionItem> getItems() {
        return propositionItems;
    }

    /**
     * Gets the {@code Proposition} scope.
     *
     * @return {@link String} containing the encoded {@link Proposition} scope.
     */
    @NonNull public String getScope() {
        return scope;
    }

    /**
     * Gets the {@code Proposition} scope details.
     *
     * @return {@code Map<String, Object>} containing the {@link Proposition} scope details.
     */
    @NonNull Map<String, Object> getScopeDetails() {
        return scopeDetails;
    }

    String getActivityId() {
        // return early if we have no "scopeDetails"
        if (MapUtils.isNullOrEmpty(scopeDetails)) {
            return "";
        }

        final Map<String, Object> activity =
                DataReader.optTypedMap(
                        Object.class, scopeDetails, MessagingConstants.PayloadKeys.ACTIVITY, null);

        // return early if we don't have an "activity" map in "scopeDetails"
        if (MapUtils.isNullOrEmpty(activity)) {
            return "";
        }

        return DataReader.optString(activity, MessagingConstants.PayloadKeys.ID, "");
    }

    /**
     * Creates a {@code Proposition} object from the provided {@code Map<String, Object>}.
     *
     * @return {@link Proposition} object created from the provided {@link Map<String, Object>}.
     */
    @Nullable public static Proposition fromEventData(final Map<String, Object> eventData) {
        Proposition proposition = null;
        try {
            final String uniqueId = DataReader.getString(eventData, PAYLOAD_ID);
            final String scope = DataReader.getString(eventData, PAYLOAD_SCOPE);
            final Map<String, Object> scopeDetails =
                    DataReader.getTypedMap(Object.class, eventData, PAYLOAD_SCOPE_DETAILS);
            final List<Map<String, Object>> items =
                    DataReader.optTypedListOfMap(
                            Object.class, eventData, PAYLOAD_ITEMS, new ArrayList<>());
            final List<PropositionItem> propositionItems = new ArrayList<>();
            for (final Map<String, Object> item : items) {
                final PropositionItem propositionItem =
                        PropositionItem.fromPropositionItemsMap(item);
                if (propositionItem != null) {
                    propositionItems.add(propositionItem);
                }
            }
            proposition = new Proposition(uniqueId, scope, scopeDetails, propositionItems);
        } catch (final DataReaderException | MessageRequiredFieldMissingException exception) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Exception occurred creating Proposition from event data map: %s",
                    exception.getLocalizedMessage());
        }

        return proposition;
    }

    /**
     * Creates a {@code Map<String, Object>} object from this {@code Proposition}.
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

    /**
     * Two propositions are equal if their {@code decisionScope.activity.id} values are the same.
     *
     * @param object the other {@link Proposition} object to be checked against.
     * @return {@code true} if both {@code Proposition}s share the same activityId.
     */
    public boolean equals(final Object object) {
        if (!(object instanceof Proposition)) {
            return false;
        }

        final Proposition proposition = (Proposition) object;
        return proposition.getActivityId().equals(getActivityId());
    }
}
