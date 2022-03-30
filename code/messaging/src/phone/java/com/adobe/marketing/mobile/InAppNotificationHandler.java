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
import static com.adobe.marketing.mobile.MessagingConstants.MESSAGES_CACHE_SUBDIRECTORY;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to handle the retrieval, processing, and display of AJO in-app messages.
 */
class InAppNotificationHandler {
    // private vars
    private final static String SELF_TAG = "InAppNotificationHandler";
    private final static String IMAGE_SRC_PATTERN = "(<img\\b|(?!^)\\G)[^>]*?\\b(src)=([\"']?)([^>]*?)\\3";
    private final ArrayList<String> imageAssetList = new ArrayList<>();
    private final Map<String, String> assetMap = new HashMap<>();
    private final Module messagingModule;
    private final MessagingCacheUtilities messagingCacheUtilities;
    // package private
    final MessagingInternal parent;
    final OffersConfig offersConfig = new OffersConfig();

    /**
     * Constructor
     *
     * @param parent {@link MessagingInternal} instance that is the parent of this {@code InAppNotificationHandler}
     */
    InAppNotificationHandler(final MessagingInternal parent, final MessagingCacheUtilities messagingCacheUtilities) {
        this.parent = parent;
        this.messagingCacheUtilities = messagingCacheUtilities;
        // create a module to get access to the Core rules engine for adding ODE rules
        messagingModule = new Module("Messaging", MobileCore.getCore().eventHub) {
        };
        // load cached rules (if any) when InAppNotificationHandler is instantiated
        if (messagingCacheUtilities != null && messagingCacheUtilities.areMessagesCached()) {
            Map<String, Variant> cachedMessages = messagingCacheUtilities.getCachedMessages();
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                Log.trace(LOG_TAG, "%s - Retrieved cached messages, attempting to load them into the rules engine.", SELF_TAG);
                handleOfferNotificationPayload(cachedMessages);
            }
        }
    }

    /**
     * Generates and dispatches an event prompting the Optimize extension to fetch in-app messages.
     */
    void fetchMessages() {
        final Map<String, String> decisionScope = new HashMap<>();
        // if we have an activity and placement id present in the manifest use the id's to retrieve offers
        if (!StringUtils.isNullOrEmpty(offersConfig.activityId) && !StringUtils.isNullOrEmpty(offersConfig.placementId)) {
            Log.trace(LOG_TAG, "%s - Activity id (%s) and placement id (%s) were found in the app manifest. Using these identifiers to retrieve offers.", SELF_TAG, offersConfig.activityId, offersConfig.placementId);
            decisionScope.put(MessagingConstants.EventDataKeys.Optimize.NAME, getEncodedDecisionScopeForActivityAndPlacement(offersConfig.activityId, offersConfig.placementId));
        } else { // otherwise use the application identifier
            Log.trace(LOG_TAG, "%s - Using the application identifier (%s) to retrieve offers.", SELF_TAG, offersConfig.applicationId);
            decisionScope.put(MessagingConstants.EventDataKeys.Optimize.NAME, getEncodedDecisionScopeForAppId(offersConfig.applicationId));
        }

        // create event to be handled by the Optimize extension
        final Map<String, Object> optimizeData = new HashMap<>();
        final List<Map<String, String>> decisionScopes = new ArrayList<>();
        decisionScopes.add(decisionScope);
        optimizeData.put(MessagingConstants.EventDataKeys.Optimize.REQUEST_TYPE, MessagingConstants.EventDataKeys.Values.Optimize.UPDATE_PROPOSITIONS);
        optimizeData.put(MessagingConstants.EventDataKeys.Optimize.DECISION_SCOPES, decisionScopes);

        // send event
        MessagingUtils.sendEvent(MessagingConstants.EventName.RETRIEVE_MESSAGE_DEFINITIONS_EVENT,
                MessagingConstants.EventType.OPTIMIZE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                optimizeData,
                MessagingConstants.EventDispatchErrors.OPTIMIZE_OFFER_RETRIEVAL_ERROR);
    }

    /**
     * Takes the retrieved activity and placement id's and returns a base54 encoded JSON string in the format expected
     * by the Optimize extension for retrieving offers
     *
     * @param activityId  a {@code String} containing the activity id
     * @param placementId a {@code String} containing the placement id
     * @return a base64 encoded JSON string to be used by the Optimize extension for retrieving offers
     */
    static String getEncodedDecisionScopeForActivityAndPlacement(final String activityId, final String placementId) {
        final byte[] decisionScopeBytes = String.format("{\"%s\":\"%s\",\"%s\":\"%s\",\"itemCount\":%s}",
                MessagingConstants.EventDataKeys.Optimize.ACTIVITY_ID,
                activityId,
                MessagingConstants.EventDataKeys.Optimize.PLACEMENT_ID,
                placementId,
                MessagingConstants.DefaultValues.Optimize.MAX_ITEM_COUNT)
                .getBytes(StandardCharsets.UTF_8);

        final EncodingService encodingService = MobileCore.getCore().eventHub.getPlatformServices().getEncodingService();
        return new String(encodingService.base64Encode(decisionScopeBytes));
    }

    /**
     * Takes the retrieved application identifier and returns a base64 encoded JSON string in the format expected
     * by the Optimize extension for retrieving offers
     *
     * @param applicationId a {@code String} containing the application id
     * @return a base64 encoded JSON string to be used by the Optimize extension for retrieving offers
     */
    static String getEncodedDecisionScopeForAppId(final String applicationId) {
        final byte[] decisionScopeBytes = String.format("{\"%s\":\"%s\"}", MessagingConstants.EventDataKeys.Optimize.XDM_NAME, applicationId).getBytes(StandardCharsets.UTF_8);

        final EncodingService encodingService = MobileCore.getCore().eventHub.getPlatformServices().getEncodingService();
        return new String(encodingService.base64Encode(decisionScopeBytes));
    }

    /**
     * Handles the Offers notification payload by extracting the rules json objects present in the Edge response event payload
     * into a list of {@code Map} objects. The valid rule json objects are then registered in the {@link RulesEngine}.
     *
     * @param payload A {@link Map<String, Variant>} containing the personalization decision payload retrieved via the Optimize extension.
     */
    void handleOfferNotificationPayload(final Map<String, Variant> payload) {
        if (MessagingUtils.isMapNullOrEmpty(payload)) {
            Log.warning(LOG_TAG, "%s - Empty content returned in call to retrieve in-app messages.", SELF_TAG);
            messagingCacheUtilities.clearCachedDataFromSubdirectory(MESSAGES_CACHE_SUBDIRECTORY);
            return;
        }

        // if we have an activity and placement id present in the manifest use these id's to validate retrieved offers
        if (!StringUtils.isNullOrEmpty(offersConfig.activityId) && !StringUtils.isNullOrEmpty(offersConfig.placementId)) {
            Log.trace(LOG_TAG, "%s - Activity id (%s) and placement id (%s) were found in the manifest. Using these identifiers to validate offers.", SELF_TAG, offersConfig.activityId, offersConfig.placementId);
            final Map<String, String> activity;
            final Map<String, String> placement;
            final Object activityMap = payload.get(MessagingConstants.EventDataKeys.Optimize.ACTIVITY);
            final Object placementMap = payload.get(MessagingConstants.EventDataKeys.Optimize.PLACEMENT);
            try { // need to convert the payload map depending on the source of the offers (optimize response event or previously cached offers)
                activity = new HashMap<>();
                if (activityMap instanceof Variant) {
                    activity.putAll(((Variant) activityMap).getStringMap());
                } else {
                    activity.putAll((Map<String, String>) activityMap);
                }
                placement = new HashMap<>();
                if (placementMap instanceof Variant) {
                    placement.putAll(((Variant) placementMap).getStringMap());
                } else {
                    placement.putAll((Map<String, String>) placementMap);
                }
            } catch (final VariantException e) {
                Log.warning(LOG_TAG, "%s - Exception occurred when converting a VariantMap to StringMap: %s", SELF_TAG, e.getMessage());
                return;
            }
            final String offerActivityId = activity.get(MessagingConstants.EventDataKeys.Optimize.ID);
            final String offerPlacementId = placement.get(MessagingConstants.EventDataKeys.Optimize.ID);
            Log.debug(LOG_TAG, "%s - Offers IAM payload contained activity id: (%s) and placement id: (%s)", SELF_TAG, offerActivityId, offerPlacementId);
            if (!offerActivityId.equals(offersConfig.activityId) || !offerPlacementId.equals(offersConfig.placementId)) {
                Log.debug(LOG_TAG, "%s - ignoring Offers IAM payload, the retrieved activity or placement id does not match the expected activity id: (%s) or expected placement id: (%s).", SELF_TAG, offersConfig.activityId, offersConfig.placementId);
                return;
            }
        } else { // otherwise use the application identifier to validate offers
            Log.trace(LOG_TAG, "%s - Using the application identifier (%s) to validate offers.", SELF_TAG, offersConfig.applicationId);
            final EncodingService encodingService = MobileCore.getCore().eventHub.getPlatformServices().getEncodingService();
            String decisionScope;
            try {
                Object rawScope = payload.get(MessagingConstants.EventDataKeys.Optimize.SCOPE);
                if (rawScope == null || StringUtils.isNullOrEmpty(rawScope.toString())) {
                    Log.warning(LOG_TAG, "%s - Unable to find a scope in the payload, payload will be discarded.", SELF_TAG);
                    return;
                }
                final byte[] decodedScope;
                if (rawScope instanceof Variant) { // need to convert the scope Json depending on the source of the offers (optimize response event or previously cached offers)
                    decodedScope = encodingService.base64Decode(payload.get(MessagingConstants.EventDataKeys.Optimize.SCOPE).convertToString());
                } else {
                    decodedScope = encodingService.base64Decode((String) rawScope);
                }
                final String decodedScopeString = new String(decodedScope);
                final JSONObject decisionScopeJson = new JSONObject(decodedScopeString);
                decisionScope = (String) decisionScopeJson.get(MessagingConstants.EventDataKeys.Optimize.XDM_NAME);
            } catch (final VariantException variantException) {
                Log.warning(LOG_TAG, "%s - Exception occurred when converting a VariantMap to StringMap: %s", SELF_TAG, variantException.getMessage());
                return;
            } catch (final JSONException jsonException) {
                Log.warning(LOG_TAG, "%s - Exception occurred when creating a JSON object from the encoded decision scope: %s", SELF_TAG, jsonException.getMessage());
                return;
            }

            Log.debug(LOG_TAG, "%s - Offers IAM payload contained application identifier: (%s)", SELF_TAG, decisionScope);
            if (!decisionScope.equals(offersConfig.applicationId)) {
                Log.debug(LOG_TAG, "%s - ignoring Offers IAM payload, the retrieved application identifier did not match the expected application identifier: (%s).", SELF_TAG, offersConfig.applicationId);
                return;
            }
        }

        // extract the items from the offers payload then attempt to register the contained rules
        List<Map<String, Variant>> items = new ArrayList<>();
        final Object itemsList = payload.get(MessagingConstants.EventDataKeys.Optimize.ITEMS);
        if (itemsList instanceof Variant) {
            final List<Variant> variantList = ((VectorVariant) itemsList).getVariantList();
            for (final Object element : variantList) {
                final MapVariant mapVariant = (MapVariant) element;
                items.add(mapVariant.getVariantMap());
            }
        } else {
            items = (List<Map<String, Variant>>) itemsList;
        }

        // save the payload to the messaging cache
        messagingCacheUtilities.cacheRetrievedMessages(payload);

        registerRules(items);
    }

    /**
     * Validates each {@link Rule} in the items {@code List} then adds all valid rules to the {@link RulesEngine}.
     * Any image assets found are cached using the {@link MessagingCacheUtilities}.
     *
     * @param items a {@link List<Map<String, Variant>>} containing a rule payload.
     */
    private void registerRules(final List<Map<String, Variant>> items) {
        final List<JsonUtilityService.JSONObject> ruleJsons = new ArrayList<>();
        final List<Rule> parsedRules = new ArrayList<>();
        // Loop through each rule in the payload and collect the rule json's present.
        // Additionally, extract the image assets and build the asset list for asset caching.
        for (final Map<String, Variant> currentItem : items) {
            final Object dataObject = currentItem.get(MessagingConstants.EventDataKeys.Optimize.DATA);
            final Map<String, String> data;
            try {
                data = new HashMap<>();
                if (dataObject instanceof Variant) {
                    data.putAll(((Variant) dataObject).getStringMap());
                } else {
                    data.putAll((Map<String, String>) dataObject);
                }
            } catch (final VariantException e) {
                Log.warning(LOG_TAG, "%s - Exception occurred when converting VariantMap to StringMap: %s", SELF_TAG, e.getMessage());
                return;
            }
            final String ruleJson = data.get(MessagingConstants.EventDataKeys.Optimize.CONTENT);
            final JsonUtilityService.JSONObject ruleJsonObject = MessagingUtils.getJsonUtilityService().createJSONObject(ruleJson);
            // we want to discard invalid jsons
            if (ruleJsonObject != null) {
                ruleJsons.add(ruleJsonObject);
                // cache any image assets present in the consequence details image assets array
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
    private void cacheImageAssetsFromPayload(JsonUtilityService.JSONObject ruleJsonObject) {
        List<String> remoteAssetsList = new ArrayList<>();
        try {
            final JSONArray rulesArray = new JSONArray(ruleJsonObject.getString(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY));
            final JSONArray consequence = rulesArray.getJSONObject(0).getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY);
            final JSONObject details = consequence.getJSONObject(0).getJSONObject(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
            final JSONArray remoteAssets = details.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
            if (remoteAssets.length() != 0 ) {
                for (final Object object : MessagingUtils.toList(remoteAssets)) {
                    remoteAssetsList.add(object.toString());
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
     * Extracts the image asset url as a {@code String} from the given in-app message payload.
     *
     * @param payloadJson A {@link JsonUtilityService.JSONObject} containing an in-app message payload.
     * @return {@code String} containing the image asset url.
     */
    private String extractImageAssetFromJson(final JsonUtilityService.JSONObject payloadJson) {
        if (payloadJson == null || payloadJson.length() <= 0) {
            Log.warning(LOG_TAG,
                    "%s - Unable to extract the image asset, the provided json is null or empty.");
            return null;
        }
        try {
            final JsonUtilityService.JSONArray rulesJsonArray = payloadJson.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY);
            final JsonUtilityService.JSONObject ruleJson = (JsonUtilityService.JSONObject) rulesJsonArray.get(0);
            final JsonUtilityService.JSONArray consequenceJsonArray = ruleJson.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY);
            final JsonUtilityService.JSONObject consequence = (JsonUtilityService.JSONObject) consequenceJsonArray.get(0);
            final JsonUtilityService.JSONObject detail = consequence.getJSONObject(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
            final String html = detail.getString(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML);
            final Pattern pattern = Pattern.compile(IMAGE_SRC_PATTERN);
            final Matcher matcher = pattern.matcher(html);
            matcher.find();
            // matcher.group(1) will contain "<img" if a match was found
            if (matcher.group(1).isEmpty()) {
                Log.trace(LOG_TAG, "No image asset found in html.");
                return null;
            }
            // matcher.group{4} will contain the image asset url
            final String imageAssetUrl = matcher.group(4);
            Log.trace(LOG_TAG, "Found image asset in html: %s", imageAssetUrl);
            return imageAssetUrl;
        } catch (final JsonException jsonException) {
            Log.warning(LOG_TAG,
                    "%s - An exception occurred during image asset extraction: %s", SELF_TAG, jsonException.getMessage());
            return null;
        }
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