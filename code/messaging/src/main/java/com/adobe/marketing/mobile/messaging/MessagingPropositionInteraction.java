package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;

import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MessagingPropositionInteraction {

    private static final String SELF_TAG = "MessagingPropositionInteraction";
    private final MessagingEdgeEventType eventType;
    private final String interaction;
    private final PropositionInfo propositionInfo;
    private final String itemId;

    MessagingPropositionInteraction(final MessagingEdgeEventType eventType, final String interaction,
                                    final PropositionInfo propositionInfo, final String itemId) {
        this.eventType = eventType;
        this.interaction = interaction;
        this.propositionInfo = propositionInfo;
        this.itemId = itemId;
    }

    Map<String, Object> getPropositionInteractionXDM() {
        if (propositionInfo == null) {
            Log.trace(LOG_TAG, SELF_TAG, "Unable to create proposition interaction data, PropositionInfo was not found for this message.");
            return null;
        }
        final Map<String, Object> propositionDetailsData= new HashMap<>();
        propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID, propositionInfo.id);
        propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE , propositionInfo.scope);
        propositionDetailsData.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCOPE_DETAILS,
                propositionInfo.scopeDetails == null ? new HashMap<>() : propositionInfo.scopeDetails);
        if (!StringUtils.isNullOrEmpty(itemId)) {
            List<Map<String, String>> items = new ArrayList<>();
            Map<String, String> item = new HashMap<>();
            item.put(MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID, itemId);
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
