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

package com.adobe.marketing.mobile.messaging.internal;

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.CACHE_BASE_DIR;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.ITEMS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.REQUEST_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SharedState.EdgeIdentity.ECID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SharedState.EdgeIdentity.ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Surface;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MessagingUtils {

    static List<PropositionPayload> getPropositionPayloads(final List<Map<String, Object>> payloads) throws Exception {
        final List<PropositionPayload> propositionPayloads = new ArrayList<>();
        for (final Map<String, Object> payload : payloads) {
            if (payload != null) {
                final PropositionInfo propositionInfo = PropositionInfo.create(payload);
                final PropositionPayload propositionPayload = PropositionPayload.create(propositionInfo, (List<Map<String, Object>>) payload.get(ITEMS));
                if (propositionPayload != null) {
                    propositionPayloads.add(propositionPayload);
                }
            }
        }
        return propositionPayloads;
    }

    // ========================================================================================
    // Cache Path helper
    // ========================================================================================

    static String getAssetCacheLocation() {
        final DeviceInforming deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
        String assetCacheLocation = null;
        if (deviceInfoService != null) {
            final File applicationCacheDir = deviceInfoService.getApplicationCacheDir();
            if (applicationCacheDir != null) {
                assetCacheLocation = applicationCacheDir + File.separator + CACHE_BASE_DIR + File.separator + IMAGES_CACHE_SUBDIRECTORY;
            }
        }
        return assetCacheLocation;
    }

    // ========================================================================================
    // Event Validation
    // ========================================================================================

    /**
     * Determines if the passed in {@code Event} is a generic identity request content event.
     *
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
     * Determines if the passed in {@code Event} is a messaging request content event.
     *
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
     * Determines if the passed in {@code Event} is a refresh messages event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a refresh messages event.
     */
    static boolean isRefreshMessagesEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && event.getEventData().containsKey(MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES);
    }

    /**
     * Determines if the passed in {@code Event} is an edge personalization decision event.
     *
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
     * Determines if the passed in {@code Event} is an update propositions event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is an update propositions event.
     */
    static boolean isUpdatePropositionsEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && DataReader.optBoolean(event.getEventData(), MessagingConstants.EventDataKeys.Messaging.UPDATE_PROPOSITIONS, false);
    }

    // ========================================================================================
    // Surfaces retrieval and validation
    // ========================================================================================

    /**
     * Retrieves the app surfaces from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code List<Surface>} containing the app surfaces to be used for retrieving propositions
     */
    static List<Surface> getSurfaces(final Event event) {
        if (event == null || event.getEventData() == null) {
            return null;
        }
        final List<String> surfaceStrings = DataReader.optTypedList(String.class, event.getEventData(), MessagingConstants.EventDataKeys.Messaging.SURFACES, null);
        final List<Surface> surfaces = new ArrayList<>();

        for (final String surfaceString : surfaceStrings) {
            surfaces.add(Surface.fromString(surfaceString));
        }
        return surfaces;
    }

    // ========================================================================================
    // Request event id retrieval
    // ========================================================================================

    /**
     * Retrieves the request event id {@code String} from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code List<String>} containing the app surfaces to be used for retrieving feeds
     */
    static String getRequestEventId(final Event event) {
        return DataReader.optString(event.getEventData(), REQUEST_EVENT_ID, null);
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
        if (MapUtils.isNullOrEmpty(identityMap)) return null;

        final List<Map<String, Object>> ecids = DataReader.optTypedListOfMap(Object.class, identityMap, ECID, null);
        if (ecids == null || ecids.isEmpty()) return null;

        final Map<String, Object> ecidMap = ecids.get(0);
        if (MapUtils.isNullOrEmpty(ecidMap)) return null;

        return DataReader.optString(ecidMap, ID, null);
    }
}