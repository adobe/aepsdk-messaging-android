/*
  Copyright 2025 Adobe. All rights reserved.
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
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** This class handles batching multiple proposition interactions into a single XDM data map. */
class PropositionInteractionBatcher {

    private static final String SELF_TAG = "BatchedPropositionInteraction";

    // Enum containing the Event Type to be used for the ensuing Edge Event
    private final MessagingEdgeEventType eventType;

    // Interaction string to identify interaction type with the proposition items
    private final String interaction;

    // List of PropositionItem objects to be batched into a single interaction
    private final List<PropositionItem> propositionItems;

    PropositionInteractionBatcher(
            @NonNull final MessagingEdgeEventType eventType,
            final String interaction,
            @NonNull final List<PropositionItem> propositionItems) {
        this.eventType = eventType;
        this.interaction = interaction;
        this.propositionItems = propositionItems;
    }

    /**
     * Creates a {@code Map} containing XDM data for batched interactions with the given proposition
     * items, for the provided event type. If any proposition reference within the items is released
     * and no longer valid, those items will be skipped.
     *
     * @return {@link Map} containing XDM data for the batched proposition interactions, or null if
     *     no valid items
     */
    @Nullable Map<String, Object> generateBatchedXdmMap() {
        if (propositionItems == null || propositionItems.isEmpty()) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot generate batched interaction XDM, proposition items collection is null"
                            + " or empty.");
            return null;
        }

        if (eventType == null) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot generate batched interaction XDM, MessagingEdgeEventType was not found"
                            + " for this message.");
            return null;
        }

        final List<PropositionInfo> propositionInfos = new ArrayList<>();
        final List<String> itemIds = new ArrayList<>();

        for (final PropositionItem propositionItem : propositionItems) {
            if (propositionItem == null) {
                continue;
            }

            if (propositionItem.propositionReference == null
                    || propositionItem.propositionReference.get() == null) {
                Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Cannot include proposition item (%s) in batched interaction, proposition"
                                + " reference is not available.",
                        propositionItem.getItemId());
                continue;
            }

            final PropositionInfo propositionInfo =
                    PropositionInfo.createFromProposition(propositionItem.getProposition());
            if (propositionInfo == null) {
                Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Cannot include proposition item (%s) in batched interaction, could not"
                                + " create PropositionInfo.",
                        propositionItem.getItemId());
                continue;
            }

            propositionInfos.add(propositionInfo);
            itemIds.add(propositionItem.getItemId());
        }

        if (propositionInfos.isEmpty()) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot generate batched interaction XDM, no valid proposition items found.");
            return null;
        }

        return getPropositionInteractionXDM(propositionInfos, itemIds);
    }

    /**
     * Creates a {@code Map} containing the XDM data for batched proposition interactions.
     *
     * @param propositionInfos {@link List} of {@link PropositionInfo} objects encapsulating
     *     proposition related information
     * @param itemIds {@code List} of item ID strings to identity the proposition item interacted
     *     with
     * @return {@link Map} containing the XDM data for batched proposition interactions
     */
    @NonNull private Map<String, Object> getPropositionInteractionXDM(
            @NonNull final List<PropositionInfo> propositionInfos,
            @NonNull final List<String> itemIds) {
        final List<PropositionInteraction> propositionInteractions = new ArrayList<>();
        for (int i = 0; i < propositionInfos.size(); i++) {
            final PropositionInfo propositionInfo = propositionInfos.get(i);
            final String itemId = (i < itemIds.size()) ? itemIds.get(i) : null;

            propositionInteractions.add(
                    new PropositionInteraction(
                            eventType, interaction, propositionInfo, itemId, null));
        }

        // Return empty map if no valid propositions
        final List<Map<String, Object>> propositionDetailsList =
                generatePropositionDetailsList(propositionInteractions);
        if (MessagingUtils.isNullOrEmpty(propositionDetailsList)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to create batched proposition interaction data, no valid proposition"
                            + " details found.");
            return Collections.emptyMap();
        }

        final Map<String, Object> propositionEventType = new HashMap<>();
        propositionEventType.put(eventType.getPropositionEventType(), 1);

        final Map<String, Object> decisioning = new HashMap<>();
        decisioning.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_EVENT_TYPE,
                propositionEventType);
        decisioning.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS,
                propositionDetailsList);

        return PropositionInteractionXdmUtils.generateXdmMap(decisioning, interaction, eventType);
    }

    @NonNull private List<Map<String, Object>> generatePropositionDetailsList(
            final List<PropositionInteraction> propositionInteractions) {
        final List<Map<String, Object>> propositionDetails = new ArrayList<>();
        for (final PropositionInteraction propositionInteraction : propositionInteractions) {
            final Map<String, Object> detailMap =
                    propositionInteraction.generatePropositionDetails();
            if (detailMap == null) {
                Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Invalid PropositionInteraction, unable to create proposition details.");
                continue;
            }
            propositionDetails.add(detailMap);
        }

        return propositionDetails;
    }
}
