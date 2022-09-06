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

import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.SURFACE_BASE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.ITEMS;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.PERSONALIZATION;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.QUERY;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.SCOPE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.SURFACES;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.DATA;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.CONTENT;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Personalization.RULES;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstants.PROPOSITIONS_CACHE_SUBDIRECTORY;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to handle the retrieval, processing, and display of AJO in-app messages.
 */
class InAppNotificationHandler {
    private final static String SELF_TAG = "InAppNotificationHandler";
    final MessagingInternal parent;
    private final Module messagingModule;
    private final MessagingCacheUtilities messagingCacheUtilities;

    /**
     * Constructor
     *
     * @param parent                  {@link MessagingInternal} instance that is the parent of this {@code InAppNotificationHandler}
     * @param messagingCacheUtilities {@link MessagingCacheUtilities} instance to use for caching in-app message rule jsons and image assets
     */
    InAppNotificationHandler(final MessagingInternal parent, final MessagingCacheUtilities messagingCacheUtilities) {
        this.parent = parent;
        this.messagingCacheUtilities = messagingCacheUtilities;
        // create a module to get access to the Core rules engine for adding ODE rules
        messagingModule = new Module("Messaging", MobileCore.getCore().eventHub) {
        };
        // load cached propositions (if any) when InAppNotificationHandler is instantiated
        if (messagingCacheUtilities != null && messagingCacheUtilities.arePropositionsCached()) {
            Map<String, Object> cachedMessages = messagingCacheUtilities.getCachedPropositions();
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                Log.trace(LOG_TAG, "%s - Retrieved cached propositions, attempting to load in-app messages into the rules engine.", SELF_TAG);
                handlePersonalizationPayload(cachedMessages);
            }
        }
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to fetch in-app messages.
     * The app surface used in the request is generated using the application id of the app.
     * If the application id is unavailable, calling this method will do nothing.
     */
    void fetchMessages() {
        final String appSurface = App.getAppContext().getPackageName();
        if (StringUtils.isNullOrEmpty(appSurface)) {
            Log.warning(LOG_TAG, "%s - Unable to retrieve in-app messages - unable to retrieve the application id.", SELF_TAG);
            return;
        }

        // create event to be handled by the Edge extension
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();
        final List<String> surfaceData = new ArrayList<>();
        surfaceData.add(SURFACE_BASE + appSurface);
        personalizationData.put(SURFACES, surfaceData);
        messageRequestData.put(PERSONALIZATION, personalizationData);
        eventData.put(QUERY, messageRequestData);

        // add xdm request type
        final Map<String, Object> xdmData = new HashMap<String, Object>() {
            {
                put(EVENT_TYPE, PERSONALIZATION_REQUEST);
            }
        };
        eventData.put(XDM, xdmData);


        // send event
        Log.debug(LOG_TAG, "%s - Dispatching edge event to fetch in-app messages.", SELF_TAG);
        MessagingUtils.sendEvent(MessagingConstants.EventName.RETRIEVE_MESSAGE_DEFINITIONS_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                eventData,
                MessagingConstants.EventDispatchErrors.PERSONALIZATION_REQUEST_ERROR);
    }

    /**
     * Handles the notification payload by extracting the rules json objects present in the Edge response event
     * into a list of {@code Map} objects. The valid rule json objects are then registered in the {@link RulesEngine}.
     *
     * @param payload A {@link Map<String, Variant>} containing the personalization decision payload retrieved via the Edge extension.
     */
    void handlePersonalizationPayload(final Map<String, Object> payload) {
        if (MessagingUtils.isMapNullOrEmpty(payload)) {
            Log.warning(LOG_TAG, "%s - Empty content returned in call to retrieve in-app messages.", SELF_TAG);
            messagingCacheUtilities.clearCachedDataFromSubdirectory(PROPOSITIONS_CACHE_SUBDIRECTORY);
            return;
        }
        final String appSurface = App.getAppContext().getPackageName();
        Log.trace(LOG_TAG, "%s - Using the application identifier (%s) to validate the notification payload.", SELF_TAG, appSurface);

        final String scope = (String) payload.get(SCOPE);
        if (StringUtils.isNullOrEmpty(scope)) {
            Log.warning(LOG_TAG, "%s - Unable to find a scope in the payload, payload will be discarded.", SELF_TAG);
            return;
        }

        // check that app surface is present in the payload before processing any in-app message rules present
        Log.debug(LOG_TAG, "%s - IAM payload contained the app surface: (%s)", SELF_TAG, scope);
        if (!scope.equals(SURFACE_BASE + appSurface)) {
            Log.debug(LOG_TAG, "%s - the retrieved application identifier did not match the app surface present in the IAM payload: (%s).", SELF_TAG, appSurface);
            return;
        }

        // extract the items from the notification payload then attempt to register the contained rules
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get(ITEMS);

        // save the proposition payload if present to the messaging cache
        messagingCacheUtilities.cachePropositions(payload);

        registerRules(items);
    }

    /**
     * Validates each {@link Rule} in the items {@code List} then adds all valid rules to the {@link RulesEngine}.
     * Any image assets found are cached using the {@link MessagingCacheUtilities}.
     *
     * @param items a {@link List<Map<String, Object>>} containing a rule payload.
     */
    private void registerRules(final List<Map<String, Object>> items) {
        final List<JsonUtilityService.JSONObject> ruleJsons = new ArrayList<>();
        final List<Rule> parsedRules = new ArrayList<>();
        // Loop through each rule in the payload and collect the rule json's present.
        // Additionally, extract the image assets and build the asset list for asset caching.
        for (final Map<String, Object> currentItem : items) {
            final Map<String, String> data = (Map<String, String>) currentItem.get(DATA);
            final String ruleJson = data.get(CONTENT);
            final JsonUtilityService.JSONObject ruleJsonObject = MessagingUtils.getJsonUtilityService().createJSONObject(ruleJson);
            // we want to discard invalid jsons
            if (ruleJsonObject != null) {
                ruleJsons.add(ruleJsonObject);
                // cache any image assets present in the current rule json's image assets array
                cacheImageAssetsFromPayload(ruleJsonObject);
            }
        }

        // create Rule objects from the rule jsons and load them into the RulesEngine
        for (final JsonUtilityService.JSONObject ruleJson : ruleJsons) {
            final Rule parsedRule = parseRuleFromJsonObject(ruleJson);
            if (parsedRule != null) {
                parsedRules.add(parsedRule);
            }
        }
        Log.debug(LOG_TAG, "%s - handleOfferNotification - registering %d rules", SELF_TAG, parsedRules.size());
        messagingModule.replaceRules(parsedRules);
    }

    /**
     * Create an in-app message object then attempt to display it.
     *
     * @param rulesEvent The Rules Engine {@link Event} containing an in-app message definition.
     */
    void createInAppMessage(final Event rulesEvent) {
        final Map<String, Object> triggeredConsequence = (Map<String, Object>) rulesEvent.getEventData().get(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED);
        if (MessagingUtils.isMapNullOrEmpty(triggeredConsequence)) {
            Log.warning(LOG_TAG,
                    "%s - Unable to create an in-app message, consequences are null or empty.", SELF_TAG);
            return;
        }

        try {
            final Map<String, Object> details = (Map<String, Object>) triggeredConsequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
            if (MessagingUtils.isMapNullOrEmpty(details)) {
                Log.warning(LOG_TAG,
                        "%s - Unable to create an in-app message, the consequence details are null or empty", SELF_TAG);
                return;
            }
            final Map<String, Object> mobileParameters = (Map<String, Object>) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS);
            // the asset map is populated when the edge response event containing messages is processed
            final Message message = new Message(parent, triggeredConsequence, mobileParameters, messagingCacheUtilities.getAssetsMap());
            message.show();
        } catch (final MessageRequiredFieldMissingException exception) {
            Log.warning(LOG_TAG,
                    "%s - Unable to create an in-app message, an exception occurred during creation: %s", SELF_TAG, exception.getLocalizedMessage());
        }
    }

    /**
     * Cache any asset URL's present in the {@link RuleConsequence} detail {@link JSONObject}.
     *
     * @param ruleJsonObject A {@link Rule} JSON object containing an in-app message definition.
     */
    private void cacheImageAssetsFromPayload(final JsonUtilityService.JSONObject ruleJsonObject) {
        List<String> remoteAssetsList = new ArrayList<>();
        try {
            final JSONArray rulesArray = new JSONArray(ruleJsonObject.getString(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY));
            final JSONArray consequence = rulesArray.getJSONObject(0).getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY);
            final JSONObject details = consequence.getJSONObject(0).getJSONObject(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
            final JSONArray remoteAssets = details.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
            if (remoteAssets.length() != 0) {
                for (final Object object : MessagingUtils.toList(remoteAssets)) {
                    final String imageAssetUrl = object.toString();
                    if (StringUtils.stringIsUrl(imageAssetUrl)) {
                        Log.debug(LOG_TAG,
                                "%s - Image asset to be cached (%s) ", SELF_TAG, imageAssetUrl);
                        remoteAssetsList.add(imageAssetUrl);
                    }
                }
            }
        } catch (final JSONException | JsonException jsonException) {
            Log.warning(LOG_TAG,
                    "%s - An exception occurred retrieving the remoteAssets array from the rule json payload: %s", SELF_TAG, jsonException.getLocalizedMessage());
            return;
        }
        messagingCacheUtilities.cacheImageAssets(remoteAssetsList);
    }

    /**
     * Parses rules from the provided {@code jsonObject} into a {@code Rule}.
     *
     * @param rulesJsonObject {@code JSONObject} containing a rule and consequences
     * @return a {@code Rule} object that was parsed from the input {@code jsonObject} or null if parsing failed.
     */
    private Rule parseRuleFromJsonObject(final JsonUtilityService.JSONObject rulesJsonObject) {
        if (rulesJsonObject == null) {
            Log.debug(LOG_TAG, "%s - parseRulesFromJsonObject -  Unable to parse rules, input jsonObject is null.", SELF_TAG);
            return null;
        }

        JsonUtilityService.JSONArray rulesJsonArray;

        try {
            rulesJsonArray = rulesJsonObject.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY);
        } catch (final JsonException jsonException) {
            Log.debug(LOG_TAG, "%s - parseRulesFromJsonObject -  Unable to parse rules (%s)", SELF_TAG, jsonException);
            return null;
        }

        // loop through each rule definition
        Rule parsedRule = null;
        for (int i = 0; i < rulesJsonArray.length(); i++) {
            try {
                // get individual rule json object
                final JsonUtilityService.JSONObject ruleObject = rulesJsonArray.getJSONObject(i);
                // get rule condition
                final JsonUtilityService.JSONObject ruleConditionJsonObject = ruleObject.getJSONObject(
                        MessagingConstants.EventDataKeys.RulesEngine.JSON_CONDITION_KEY);
                final RuleCondition condition = RuleCondition.ruleConditionFromJson(ruleConditionJsonObject);
                // get consequences
                final JsonUtilityService.JSONArray consequenceJSONArray = ruleObject.getJSONArray(
                        MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY);
                final List<Event> consequences = generateConsequenceEvents(consequenceJSONArray);
                if (consequences != null) {
                    parsedRule = new Rule(condition, consequences);
                }
            } catch (final JsonException e) {
                Log.debug(LOG_TAG, "%s - parseRulesFromJsonObject -  Unable to parse individual rule json (%s)", SELF_TAG, e.getLocalizedMessage());
            } catch (final UnsupportedConditionException e) {
                Log.debug(LOG_TAG, "%s - parseRulesFromJsonObject -  Unable to parse individual rule conditions (%s)", SELF_TAG, e);
            } catch (final IllegalArgumentException e) {
                Log.debug(LOG_TAG, "%s - parseRulesFromJsonObject -  Unable to create rule object (%s)", SELF_TAG, e);
            }
        }

        return parsedRule;
    }

    /**
     * Parses {@code MessagingConsequence} objects from the given {@code consequenceJsonArray} and converts them
     * into a list of {@code Event}s.
     * <p>
     *
     * @param consequenceJsonArray {@link JsonUtilityService.JSONArray} object containing 1 or more rule consequence definitions
     * @return a {@code List} of consequence {@code Event} objects.
     * @throws JsonException if errors occur during parsing
     */
    private List<Event> generateConsequenceEvents(final JsonUtilityService.JSONArray consequenceJsonArray) throws
            JsonException {
        final List<Event> parsedEvents = new ArrayList<>();

        if (consequenceJsonArray == null || consequenceJsonArray.length() == 0) {
            Log.debug(LOG_TAG,
                    "%s - generateConsequenceEvents -  The passed in consequence array is null, so returning an empty consequence events list.", SELF_TAG);
            return parsedEvents;
        }

        final JsonUtilityService jsonUtilityService = MessagingUtils.getJsonUtilityService();

        if (jsonUtilityService == null) {
            Log.debug(LOG_TAG,
                    "%s - generateConsequenceEvents -  JsonUtility service is not available, returning empty consequence events list.", SELF_TAG);
            return parsedEvents;
        }

        JsonObjectVariantSerializer serializer = new JsonObjectVariantSerializer(jsonUtilityService);
        for (int i = 0; i < consequenceJsonArray.length(); i++) {
            try {
                final Variant consequenceAsVariant = Variant.fromTypedObject(consequenceJsonArray.getJSONObject(i), serializer);

                MessagingConsequence consequence = consequenceAsVariant.getTypedObject(new MessagingConsequenceSerializer());

                if (consequence != null) {
                    final Map<String, Variant> consequenceVariantMap = Variant.fromTypedObject(consequence,
                            new MessagingConsequenceSerializer()).getVariantMap();

                    EventData eventData = new EventData();
                    eventData.putVariantMap(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED,
                            consequenceVariantMap);

                    final Event event = new Event.Builder("Rules Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
                            .setData(eventData)
                            .build();

                    parsedEvents.add(event);
                }
            } catch (VariantException ex) {
                Log.warning(LOG_TAG,
                        "%s - Unable to convert consequence json object to a variant.", SELF_TAG);
                return null;
            }
        }

        return parsedEvents;
    }
}