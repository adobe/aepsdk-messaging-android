/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.ITEMS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.EdgeIdentity.ECID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.EdgeIdentity.ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP;

import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MessagingUtils {

    private static final String SELF_TAG = "MessagingUtils";

    static List<PropositionPayload> getPropositionPayloads(final List<Map<String, Object>> payloads) {
        if (payloads == null || payloads.size() == 0) {
            return null;
        }

        List<PropositionPayload> propositionPayloads = new ArrayList<>();
        for (final Map<String, Object> payload : payloads) {
            if (payload != null) {
                final PropositionInfo propositionInfo = PropositionInfo.create(payload);
                final PropositionPayload propositionPayload = PropositionPayload.create(propositionInfo, (List<Map<String, Object>>) payload.get(ITEMS));
                propositionPayloads.add(propositionPayload);
            }
        }
        return propositionPayloads;
    }

    // ========================================================================================
    // Event Validation
    // ========================================================================================

    /**
     * @param event A Generic Identity Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a generic identity request content event.
     */
    static boolean isGenericIdentityRequestEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.GENERIC_IDENTITY.equalsIgnoreCase(event.getType()) &&
                EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource());
    }

    /**
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a messaging request content event.
     */
    static boolean isMessagingRequestContentEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.MESSAGING.equalsIgnoreCase(event.getType()) &&
                EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource());
    }

    /**
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a message fetch event.
     */
    static boolean isFetchMessagesEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && event.getEventData().containsKey(MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES);
    }

    /**
     * @param event A Rules Response Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a messaging consequence event.
     */
    static boolean isMessagingConsequenceEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.RULES_ENGINE.equalsIgnoreCase(event.getType())
                && EventSource.RESPONSE_CONTENT.equalsIgnoreCase(event.getSource())
                && event.getEventData().containsKey(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED);
    }

    /**
     * @param event An Edge Personalization Decision {@link Event}.
     * @return {@code boolean} indicating if the passed in event is an edge personalization decision event.
     */
    static boolean isEdgePersonalizationDecisionEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.EDGE.equalsIgnoreCase(event.getType()) &&
                MessagingConstants.EventSource.PERSONALIZATION_DECISIONS.equalsIgnoreCase(event.getSource());
    }

    /**
     * @param map A {@link Map} of any type.
     * @return {@code boolean} indicating if the passed in {@code Map} is empty.
     */
    static boolean isMapNullOrEmpty(final Map map) {
        return map == null || map.isEmpty();
    }

    // ========================================================================================
    // Event Dispatching
    // ========================================================================================

    /**
     * Dispatches an event with the given parameters.
     *
     * @param eventName    a {@code String} containing the name of the event to be dispatched
     * @param eventType    a {@code String} containing the type of the event to be dispatched
     * @param eventSource  a {@code String} containing the source of the event to be dispatched
     * @param data         a {@link Map} containing the data of the event to be dispatched
     * @param mask         a {@link String[]} containing an optional event mask
     * @param extensionApi {@link ExtensionApi} to use for dispatching the event
     */
    static void sendEvent(final String eventName, final String eventType, final String eventSource, final Map<String, Object> data, final String[] mask, final ExtensionApi extensionApi) {
        final Event event = new Event.Builder(eventName, eventType, eventSource, mask)
                .setEventData(data)
                .build();
        extensionApi.dispatch(event);
    }

    /**
     * Dispatches an event with the given parameters.
     *
     * @param eventName    a {@code String} containing the name of the event to be dispatched
     * @param eventType    a {@code String} containing the type of the event to be dispatched
     * @param eventSource  a {@code String} containing the source of the event to be dispatched
     * @param data         a {@link Map} containing the data of the event to be dispatched
     * @param extensionApi {@link ExtensionApi} to use for dispatching the event
     */
    static void sendEvent(final String eventName, final String eventType, final String eventSource, final Map<String, Object> data, final ExtensionApi extensionApi) {
        sendEvent(eventName, eventType, eventSource, data, null, extensionApi);
    }

    // ========================================================================================
    // Shared State Helpers
    // ========================================================================================
    static String getSharedStateEcid(final Map<String, Object> edgeIdentityState) {
        final Map<String, Object> identityMap = DataReader.optTypedMap(Object.class, edgeIdentityState, IDENTITY_MAP, null);
        if (MessagingUtils.isMapNullOrEmpty(identityMap)) return null;

        final List<Map<String, Object>> ecids = DataReader.optTypedListOfMap(Object.class, identityMap, ECID, null);
        if (ecids == null || ecids.isEmpty()) return null;

        final Map<String, Object> ecidMap = ecids.get(0);
        if (MessagingUtils.isMapNullOrEmpty(ecidMap)) return null;

        return DataReader.optString(ecidMap, ID, null);
    }

    static String getShareStateMessagingEventDatasetId(final Map<String, Object> configState) {
        return DataReader.optString(configState, EXPERIENCE_EVENT_DATASET_ID, null);
    }
}
