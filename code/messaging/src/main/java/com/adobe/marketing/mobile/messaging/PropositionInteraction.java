package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;

import androidx.annotation.NonNull;

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// `PropositionInteraction` is a container for tracking related information needed to dispatch a
// `decisioning.propositionDisplay` or `decisioning.propositionInteract` event to the Experience Edge.
class PropositionInteraction {

    private static final String SELF_TAG = "PropositionInteraction";

    // Edge event type represented by MessagingEdgeEventType
    private final MessagingEdgeEventType eventType;

    // Interaction string to identify interaction type with the proposition item
    private final String interaction;

    // `PropositionInfo` instance to encapsulate proposition related information
    private final PropositionInfo propositionInfo;

    // Item ID string to identity the proposition item interacted with
    private final String itemId;

    // Sub-item tokens array to track interactions with proposition sub-items
    private final List<String> tokens;

    PropositionInteraction(@NonNull final MessagingEdgeEventType eventType, final String interaction,
                           @NonNull final PropositionInfo propositionInfo, final String itemId, final List<String> tokens) {
        this.eventType = eventType;
        this.interaction = interaction;
        this.propositionInfo = propositionInfo;
        this.itemId = itemId;
        this.tokens = tokens;
    }

    // Returns proposition interaction XDM
    Map<String, Object> getPropositionInteractionXDM() {
        if (propositionInfo == null) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to create proposition interaction data, PropositionInfo was not found for this message.");
            return null;
        }
        if (eventType == null) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to create proposition interaction data, MessagingEdgeEventType was not found for this message.");
            return null;
        }
        final Map<String, Object> propositionDetailsData= new HashMap<>();
        propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID, propositionInfo.id);
        propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE , propositionInfo.scope);
        propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE_DETAILS,
                propositionInfo.scopeDetails == null ? new HashMap<>() : propositionInfo.scopeDetails);
        if (!StringUtils.isNullOrEmpty(itemId)) {
            final Map<String, Object> item = new HashMap<>();
            item.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID, itemId);

            if (!MessagingUtils.isNullOrEmpty(tokens)) {
                final Map<String, Object> characteristics = new HashMap<>();
                characteristics.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.TOKENS, String.join(",", tokens));
                item.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.CHARACTERISTICS, characteristics);
            }
            final List<Map<String, Object>> items = new ArrayList<>();
            items.add(item);
            propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ITEMS, items);
        }

        final Map<String, Object> propositionEventType = new HashMap<>();
        propositionEventType.put(eventType.getPropositionEventType(), 1);

        final Map<String, Object> decisioning = new HashMap<>();
        decisioning.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_EVENT_TYPE, propositionEventType);
        decisioning.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS, Collections.singletonList(propositionDetailsData));

        // only add `propositionAction` data if this is an interact event and the interaction is not empty
        if ((eventType == MessagingEdgeEventType.INTERACT || eventType == MessagingEdgeEventType.IN_APP_INTERACT)
                && !StringUtils.isNullOrEmpty(interaction)) {
            final Map<String, String> propositionAction = new HashMap<>();
            propositionAction.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID, interaction);
            propositionAction.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.LABEL, interaction);
            decisioning.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_ACTION, propositionAction);
        }

        final Map<String, Object> experience = new HashMap<>();
        experience.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.DECISIONING, decisioning);

        final Map<String, Object> xdm = new HashMap<>();
        xdm.put(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE, eventType.toString());
        xdm.put(MessagingConstants.TrackingKeys.EXPERIENCE, experience);

        return xdm;
    }
}
