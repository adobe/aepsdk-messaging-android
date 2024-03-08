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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.CACHE_BASE_DIR;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.ENDING_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.RESPONSE_ERROR;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.REQUEST_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.JSON_RULES_KEY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.IMAGES_CACHE_SUBDIRECTORY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SchemaValues.SCHEMA_FEED_ITEM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SchemaValues.SCHEMA_IAM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.EdgeIdentity.ECID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.EdgeIdentity.ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SharedState.EdgeIdentity.IDENTITY_MAP;

import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InternalMessagingUtils {
    private final static String SELF_TAG = "InternalMessagingUtils";

    static List<Proposition> getPropositionsFromPayloads(final List<Map<String, Object>> payloads) {
        final List<Proposition> propositions = new ArrayList<>();
        for (final Map<String, Object> payload : payloads) {
            if (payload != null) {
                final Proposition proposition = Proposition.fromEventData(payload);
                if (proposition != null) {
                    propositions.add(proposition);
                }
            }
        }
        return propositions;
    }

    // ========================================================================================
    // Consequence data retrieval from a JSONObject
    // ========================================================================================
    /**
     * Retrieves the consequence detail {@code Map} from the passed in {@code JSONObject}.
     *
     * @param ruleJson A {@link JSONObject} containing an AJO rule payload
     * @return {@code JSONObject> containing the consequence details extracted from the rule json
     */
    static JSONObject getConsequenceDetails(final JSONObject ruleJson) {
        JSONObject consequenceDetails = null;
        try {
            consequenceDetails = getConsequence(ruleJson).getJSONObject(MESSAGE_CONSEQUENCE_DETAIL);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving consequence details: %s", jsonException.getLocalizedMessage());
        }
        return consequenceDetails;
    }

    /**
     * Retrieves the consequence {@code JSONObject} from the passed in {@code JSONObject}.
     *
     * @param ruleJson A {@link JSONObject} containing an AJO rule payload
     * @return {@code JSONObject> containing the consequence extracted from the rule json
     */
    static JSONObject getConsequence(final JSONObject ruleJson) {
        JSONObject consequence = null;
        try {
            final JSONArray rulesArray = ruleJson.getJSONArray(JSON_RULES_KEY);
            final JSONArray consequenceArray = rulesArray.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            consequence = consequenceArray.getJSONObject(0);
        } catch (final JSONException jsonException) {
            Log.debug(LOG_TAG, "getConsequenceDetails", "Exception occurred retrieving rule consequence: %s", jsonException.getLocalizedMessage());
        }
        return consequence;
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
     * Determines if the passed in {@code Event} is an messaging personalization complete event.
     *
     * @param event A Messaging Personalization Complete {@link Event}.
     * @return {@code boolean} indicating if the passed in event is an edge personalization complete event.
     */
    static boolean isPersonalizationRequestCompleteEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return MessagingConstants.EventType.MESSAGING.equalsIgnoreCase(event.getType()) &&
                EventSource.CONTENT_COMPLETE.equalsIgnoreCase(event.getSource());
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

    /**
     * Determines if the passed in {@code Event} is a get propositions event.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a get propositions event.
     */
    static boolean isGetPropositionsEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && DataReader.optBoolean(event.getEventData(), MessagingConstants.EventDataKeys.Messaging.GET_PROPOSITIONS, false);
    }

    /**
     * Determines if the passed in {@code Event} is a tracking proposition event
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code boolean} indicating if the passed in event is a track propositions event.
     */
    static boolean isTrackingPropositionsEvent(final Event event) {
        if (event == null || event.getEventData() == null) {
            return false;
        }

        return EventType.MESSAGING.equalsIgnoreCase(event.getType())
                && EventSource.REQUEST_CONTENT.equalsIgnoreCase(event.getSource())
                && DataReader.optBoolean(event.getEventData(), MessagingConstants.EventDataKeys.Messaging.TRACK_PROPOSITIONS, false);
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
        final Map<String, Object> eventData = event.getEventData();
        final List<Map<String, Object>> surfaces = DataReader.optTypedListOfMap(Object.class, eventData, MessagingConstants.EventDataKeys.Messaging.SURFACES, null);

        if (MessagingUtils.isNullOrEmpty(surfaces)) {
            Log.debug(LOG_TAG, SELF_TAG, "Surface URI's were not found in the provided event.");
            return null;
        }

        final List<Surface> retrievedSurfaces = new ArrayList<>();
        for (final Map<String, Object> surfaceData : surfaces) {
            final Surface surface = Surface.fromEventData(surfaceData);
            retrievedSurfaces.add(surface);
        }
        return retrievedSurfaces;
    }

    // ========================================================================================
    // Event id retrieval
    // ========================================================================================

    /**
     * Retrieves the request event id {@code String} from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code String} containing the request event id
     */
    static String getRequestEventId(final Event event) {
        String requestEventId = event.getParentID();
        if (StringUtils.isNullOrEmpty(requestEventId)) {
            requestEventId = DataReader.optString(event.getEventData(), REQUEST_EVENT_ID, null);
        }
        return requestEventId;
    }

    /**
     * Retrieves the ending event id {@code String} from the passed in {@code Event}'s event data.
     *
     * @param event A Messaging Request Content {@link Event}.
     * @return {@code String} containing the ending event id
     */
    static String getEndingEventId(final Event event) {
        return DataReader.optString(event.getEventData(), ENDING_EVENT_ID, null);
    }

    // ========================================================================================
    // feed item type verification using rule consequence object
    // ========================================================================================
    static boolean isFeedItem(final RuleConsequence ruleConsequence) {
        final Map<String, Object> ruleDetailMap = ruleConsequence.getDetail();
        final String schema = DataReader.optString(ruleDetailMap, MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
        return schema.equals(SCHEMA_FEED_ITEM);
    }

    // ========================================================================================
    // in app type verification using rule consequence object
    // ========================================================================================
    static boolean isInApp(final RuleConsequence ruleConsequence) {
        final Map<String, Object> ruleDetailMap = ruleConsequence.getDetail();
        final String schema = DataReader.optString(ruleDetailMap, MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
        final String consequenceType = ruleConsequence.getType();
        return schema.equals(SCHEMA_IAM) || consequenceType.equals(MESSAGE_CONSEQUENCE_CJM_VALUE);
    }

    // ========================================================================================
    // Error Event creation
    // ========================================================================================

    /**
     * Creates a response event with specified AdobeError type added in the Event data.
     *
     * @param event a {@link Event} to create a response event containing the specified {@link AdobeError}
     * @param error the {@code AdobeError} type
     * @return the created response event
     */
    static Event createErrorResponseEvent(final Event event, final AdobeError error) {
        final Map<String, Object> eventData = new HashMap<String, Object>() {{
            put(RESPONSE_ERROR, error.getErrorName());
        }};
        return new Event.Builder(MESSAGE_PROPOSITIONS_RESPONSE, EventType.MESSAGING, EventSource.RESPONSE_CONTENT)
                .inResponseToEvent(event)
                .setEventData(eventData)
                .build();
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

    /**
     * Sends a tracking status response event with the given parameters.
     *
     * @param status a {@link PushTrackingStatus} containing the status of the tracking request
     * @param extensionApi {@link ExtensionApi} to use for dispatching the event
     * @param requestEvent {@link Event} to be used as the request event
     */
    static void sendTrackingResponseEvent(final PushTrackingStatus status, final ExtensionApi extensionApi, final Event requestEvent) {
        final Map<String, Object> responseEventData = new HashMap<>();
        responseEventData.put(MessagingConstants.EventDataKeys.Messaging.PUSH_NOTIFICATION_TRACKING_STATUS, status.getValue());
        responseEventData.put(MessagingConstants.EventDataKeys.Messaging.PUSH_NOTIFICATION_TRACKING_MESSAGE, status.getDescription());
        final Event event = new Event.Builder(MessagingConstants.EventName.PUSH_TRACKING_STATUS_EVENT , EventType.MESSAGING, EventSource.RESPONSE_CONTENT)
                .setEventData(responseEventData)
                .inResponseToEvent(requestEvent)
                .build();
        extensionApi.dispatch(event);
    }

    // ========================================================================================
    // Shared State Helpers
    // ========================================================================================
    static String getSharedStateEcid(final Map<String, Object> edgeIdentityState) {
        final Map<String, Object> identityMap = DataReader.optTypedMap(Object.class, edgeIdentityState, IDENTITY_MAP, null);
        if (MapUtils.isNullOrEmpty(identityMap)) return null;

        final List<Map<String, Object>> ecids = DataReader.optTypedListOfMap(Object.class, identityMap, ECID, null);
        if (MessagingUtils.isNullOrEmpty(ecids)) return null;

        final Map<String, Object> ecidMap = ecids.get(0);
        if (MapUtils.isNullOrEmpty(ecidMap)) return null;

        return DataReader.optString(ecidMap, ID, null);
    }

    // ========================================================================================
    // Collection utils
    // ========================================================================================
    /**
     * Updates the provided {@code Map<Surface, List<LaunchRule>>} with the provided {@code Surface} and {@code List<LaunchRule>} objects.
     *
     * @param surface     A {@link Surface} key used to update a {@link List<LaunchRule>} value in the provided {@link Map<Surface, List<LaunchRule>>}
     * @param rulesToAdd  A {@link List<LaunchRule>} list to add in the provided {@code Map<Surface, List<LaunchRule>>}
     * @param mapToUpdate The {@code Map<Surface, List<LaunchRule>>} to be updated with the provided {@code Surface} and {@code List<LaunchRule>} objects
     * @return the updated {@link Map<Surface, List<LaunchRule>>} map
     */
    public static Map<Surface, List<LaunchRule>> updateRuleMapForSurface(final Surface surface, final List<LaunchRule> rulesToAdd, Map<Surface, List<LaunchRule>> mapToUpdate) {
        if (MessagingUtils.isNullOrEmpty(rulesToAdd)) {
            return mapToUpdate;
        }
        final Map<Surface, List<LaunchRule>> updatedMap = new HashMap<>(mapToUpdate);
        final List<LaunchRule> list = updatedMap.get(surface) != null ? updatedMap.get(surface) : MessagingUtils.createMutableList(rulesToAdd);
        if (updatedMap.get(surface) != null) {
            list.addAll(rulesToAdd);
        }
        updatedMap.put(surface, list);
        return updatedMap;
    }
}