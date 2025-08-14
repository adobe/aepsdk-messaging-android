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
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.HashMap;
import java.util.Map;

/*
 * This class provides utility methods for generating XDM data maps for single and batched proposition interactions.
 */
class PropositionInteractionXdmUtils {

    /**
     * Generates a {@code Map} containing XDM data for proposition interactions, including the event
     * type and decisioning data. If the interaction is not null or empty, it adds the proposition
     * action details based on the event type.
     *
     * @param decisioningData {@link Map} containing decisioning data to be included in the XDM.
     * @param interaction {@link String} representing the interaction type.
     * @param eventType {@link MessagingEdgeEventType} representing the type of event.
     * @return {@code Map} containing the XDM data for the proposition interaction.
     */
    @NonNull static Map<String, Object> generateXdmMap(
            @NonNull final Map<String, Object> decisioningData,
            @Nullable final String interaction,
            @Nullable final MessagingEdgeEventType eventType) {
        // two use cases for including `propositionAction`:
        // 1. if this is an interact event, include `id` and `label`
        // 2. if this is a suppressDisplay event, include `reason`
        if (!StringUtils.isNullOrEmpty(interaction)) {
            final Map<String, String> propositionActionMap =
                    getPropositionActionMap(interaction, eventType);
            if (!propositionActionMap.isEmpty()) {
                decisioningData.put(
                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_ACTION,
                        propositionActionMap);
            }
        }

        final Map<String, Object> experience = new HashMap<>();
        experience.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.DECISIONING,
                decisioningData);

        final Map<String, Object> xdm = new HashMap<>();
        xdm.put(
                MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE,
                eventType.toString());
        xdm.put(MessagingConstants.TrackingKeys.EXPERIENCE, experience);

        return xdm;
    }

    @NonNull private static Map<String, String> getPropositionActionMap(
            @NonNull final String interaction, @Nullable final MessagingEdgeEventType eventType) {
        final Map<String, String> propositionActionMap = new HashMap<>();
        if (eventType == MessagingEdgeEventType.INTERACT) {
            propositionActionMap.put(
                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID, interaction);
            propositionActionMap.put(
                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key.LABEL, interaction);
        } else if (eventType == MessagingEdgeEventType.SUPPRESS_DISPLAY) {
            propositionActionMap.put(
                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key.REASON, interaction);
        }
        return propositionActionMap;
    }
}
