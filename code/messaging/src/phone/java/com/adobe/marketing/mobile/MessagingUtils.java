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

package com.adobe.marketing.mobile;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ui.UIService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class MessagingUtils {
    /* JSON - Map conversion helpers */

    /**
     * Converts provided {@link org.json.JSONObject} into {@link java.util.Map} for any number of levels, which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonObject to be converted
     * @return {@link java.util.Map} containing the elements from the provided json, null if {@code jsonObject} is null
     */
    static Map<String, Object> toMap(final JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        Map<String, Object> jsonAsMap = new HashMap<>();
        Iterator<String> keysIterator = jsonObject.keys();

        if (keysIterator == null) return null;

        while (keysIterator.hasNext()) {
            String nextKey = keysIterator.next();
            jsonAsMap.put(nextKey, fromJson(jsonObject.get(nextKey)));
        }

        return jsonAsMap;
    }

    /**
     * Converts provided {@link org.json.JSONObject} into a {@link Map<String, Variant>} for any number of levels, which can be used as event data.
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonObject to be converted
     * @return {@link Map<String, Variant>} containing the elements from the provided json, null if {@code jsonObject} is null
     */
    static Map<String, Variant> toVariantMap(final JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        final Map<String, Variant> jsonAsVariantMap = new HashMap<>();
        final Iterator<String> keysIterator = jsonObject.keys();

        while (keysIterator.hasNext()) {
            final String nextKey = keysIterator.next();
            final Object value = fromJson(jsonObject.get(nextKey));
            jsonAsVariantMap.put(nextKey, getVariantValue(value));
        }

        return jsonAsVariantMap;
    }

    /**
     * Converts provided {@link JSONArray} into {@link List} for any number of levels which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonArray to be converted
     * @return {@link List} containing the elements from the provided json, null if {@code jsonArray} is null
     */
    static List<Object> toList(final JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) {
            return null;
        }

        List<Object> jsonArrayAsList = new ArrayList<>();
        int size = jsonArray.length();

        for (int i = 0; i < size; i++) {
            jsonArrayAsList.add(fromJson(jsonArray.get(i)));
        }

        return jsonArrayAsList;
    }

    /**
     * Converts provided {@link JSONObject} to a {@link Map} or {@link JSONArray} into a {@link List}.
     *
     * @param json to be converted
     * @return {@link Object} converted from the provided json object.
     */
    private static Object fromJson(final Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return toList((JSONArray) json);
        } else {
            return json;
        }
    }

    /**
     * Converts the provided {@link Object} into a {@link Variant}.
     * This method is recursive if the passed in {@code Object} is a {@link Map} or {@link List}.
     *
     * @param value to be converted to a variant
     * @return {@code Variant} value of the passed in {@code Object}
     */
    private static Variant getVariantValue(final Object value) {
        final Variant convertedValue;
        if (value instanceof String) {
            convertedValue = StringVariant.fromString((String) value);
        } else if (value instanceof Double) {
            convertedValue = DoubleVariant.fromDouble((Double) value);
        } else if (value instanceof Integer) {
            convertedValue = IntegerVariant.fromInteger((int) value);
        } else if (value instanceof Boolean) {
            convertedValue = BooleanVariant.fromBoolean((boolean) value);
        } else if (value instanceof Long) {
            convertedValue = LongVariant.fromLong((long) value);
        } else if (value instanceof Map) {
            final HashMap<String, Variant> map = new HashMap<>();
            for (final Map.Entry entry : ((Map<String, Object>) value).entrySet()) {
                map.put((String) entry.getKey(), getVariantValue(entry.getValue()));
            }
            convertedValue = Variant.fromVariantMap((Map<String, Variant>) map);
        } else if (value instanceof List) {
            final ArrayList<Variant> list = new ArrayList<>();
            for (final Object element : (List) value) {
                list.add((Variant) getVariantValue(element));
            }
            convertedValue = Variant.fromVariantList(list);
        } else {
            convertedValue = (Variant) value;
        }
        return convertedValue;
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

        return EventType.GENERIC_IDENTITY.getName().equalsIgnoreCase(event.getType()) &&
                EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(event.getSource());
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
                EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(event.getSource());
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
                && EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(event.getSource())
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

        return EventType.RULES_ENGINE.getName().equalsIgnoreCase(event.getType())
                && EventSource.RESPONSE_CONTENT.getName().equalsIgnoreCase(event.getSource())
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

    // ========================================================================================
    // PlatformServices getters
    // ========================================================================================

    /**
     * Returns the {@link PlatformServices} instance.
     *
     * @return {@code PlatformServices} or null if {@code PlatformServices} are unavailable
     */
    static PlatformServices getPlatformServices() {
        final PlatformServices platformServices = MobileCore.getCore().eventHub.getPlatformServices();

        if (platformServices == null) {
            Log.debug(LOG_TAG,
                    "getPlatformServices - Platform services are not available.");
            return null;
        }

        return platformServices;
    }

    /**
     * Returns platform {@link JsonUtilityService} instance.
     *
     * @return {@code JsonUtilityService} or null if {@link PlatformServices} are unavailable
     */
    static JsonUtilityService getJsonUtilityService() {
        final PlatformServices platformServices = getPlatformServices();

        if (platformServices == null) {
            Log.debug(LOG_TAG,
                    "getJsonUtilityService -  Cannot get JsonUtility Service, Platform services are not available.");
            return null;
        }

        return platformServices.getJsonUtilityService();
    }

    static boolean isMapNullOrEmpty(final Map map) {
        if (map == null || map.isEmpty()) {
            return true;
        }
        return false;
    }
}
