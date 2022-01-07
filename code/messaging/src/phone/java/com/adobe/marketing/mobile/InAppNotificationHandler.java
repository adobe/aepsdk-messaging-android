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

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InAppNotificationHandler {
    // private vars
    private final static String SELF_TAG = "InAppNotificationHandler";
    private final static String IMAGE_SRC_PATTERN = "(<img\\b|(?!^)\\G)[^>]*?\\b(src)=([\"']?)([^>]*?)\\3";
    // package private
    final MessagingInternal parent;
    private final ArrayList<String> imageAssetList = new ArrayList<>();
    private final Map<String, String> assetMap = new HashMap<>();
    private final Module messagingModule;
    private final MessagingCacheUtilities cacheUtilities;
    private String activityId;
    private String placementId;

    /**
     * Constructor
     *
     * @param parent {@link MessagingInternal} instance that is the parent of this {@code InAppNotificationHandler}
     */
    InAppNotificationHandler(final MessagingInternal parent, final MessagingCacheUtilities messagingCacheUtilities) {
        this.parent = parent;
        this.cacheUtilities = messagingCacheUtilities;
        // create a module to get access to the Core rules engine for adding ODE rules
        messagingModule = new Module("Messaging", MobileCore.getCore().eventHub) {
        };
        // load cached rules (if any) when InAppNotificationHandler is instantiated
        if (messagingCacheUtilities != null && messagingCacheUtilities.areMessagesCached()) {
            Map<String, Variant> cachedMessages = messagingCacheUtilities.getCachedMessages();
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                Log.trace(LOG_TAG, "%s - Retrieved cached messages, attempting to load them into the rules engine.", SELF_TAG);
                handleOfferNotificationPayload(cachedMessages, null);
            }
        }
    }

    /**
     * Generates and dispatches an event prompting the Optimize extension to fetch in-app messages.
     */
    void fetchMessages() {
        // activity and placement are both required for message definition retrieval
        getActivityAndPlacement(null);
        if (StringUtils.isNullOrEmpty(activityId) || StringUtils.isNullOrEmpty(placementId)) {
            Log.trace(LOG_TAG, "%s - Unable to retrieve message definitions - activity and placement ids are both required.", SELF_TAG);
            return;
        }
        // create event to be handled by the Optimize extension
        final HashMap<String, Object> optimizeData = new HashMap<>();
        final ArrayList<Map<String, Object>> decisionScopes = new ArrayList<>();
        final HashMap<String, Object> decisionScope = new HashMap<>();
        decisionScope.put(MessagingConstants.EventDataKeys.Optimize.NAME, getEncodedDecisionScope(activityId, placementId));
        decisionScopes.add(decisionScope);
        optimizeData.put(MessagingConstants.EventDataKeys.Optimize.REQUEST_TYPE, MessagingConstants.EventDataKeys.Values.Optimize.UPDATE_PROPOSITIONS);
        optimizeData.put(MessagingConstants.EventDataKeys.Optimize.DECISION_SCOPES, decisionScopes);

        final Event messageFetchEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_RETRIEVE_MESSAGE_DEFINITIONS, MessagingConstants.EventType.OPTIMIZE, MessagingConstants.EventSource.REQUEST_CONTENT)
                .setEventData(optimizeData)
                .build();

        // send event
        MobileCore.dispatchEvent(messageFetchEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.warning(LOG_TAG, "%s - Error in dispatching event for refreshing messages from Optimize.", SELF_TAG);
            }
        });
    }

    /**
     * Takes an activity and placement and returns an encoded string in the format expected
     * by the Optimize extension for retrieving offers
     *
     * @param activityId  {@code String} containing the activity id
     * @param placementId {@code String} containing the placement id
     * @return a base64 encoded JSON string to be used by the Optimize extension
     */
    private String getEncodedDecisionScope(final String activityId, final String placementId) {
        final byte[] decisionScopeBytes = String.format("{\"activityId\":\"%s\",\"placementId\":\"%s\",\"itemCount\":%s}", activityId, placementId, MessagingConstants.DefaultValues.Optimize.MAX_ITEM_COUNT).getBytes(StandardCharsets.UTF_8);

        final AndroidEncodingService androidEncodingService = (AndroidEncodingService) MobileCore.getCore().eventHub.getPlatformServices().getEncodingService();
        return new String(androidEncodingService.base64Encode(decisionScopeBytes));
    }

    /**
     * Converts the rules json present in the Edge response event payload into a list of rules then loads them in the {@link RulesEngine}.
     *
     * @param payload           An {@link Map<String, Variant>} containing the personalization decision payload retrieved from Offers
     * @param edgeResponseEvent The {@link Event} which contained the personalization decision payload. This can be null if cached
     *                          messages are being loaded.
     */
    void handleOfferNotificationPayload(final Map<String, Variant> payload, final Event edgeResponseEvent) {
        if (payload == null || payload.size() == 0) {
            Log.warning(LOG_TAG, "%s - Aborting handling of the Offers IAM payload because it is null or empty.", SELF_TAG);
            return;
        }
        getActivityAndPlacement(edgeResponseEvent);

        final Map<String, String> activity;
        final Map<String, String> placement;
        final Object activityMap = payload.get(MessagingConstants.EventDataKeys.Optimize.ACTIVITY);
        final Object placementMap = payload.get(MessagingConstants.EventDataKeys.Optimize.PLACEMENT);
        try {
            if (activityMap instanceof Variant) {
                activity = ((Variant) activityMap).getStringMap();
            } else {
                activity = (Map<String, String>) activityMap;
            }
            if (placementMap instanceof Variant) {
                placement = ((Variant) placementMap).getStringMap();
            } else {
                placement = (Map<String, String>) placementMap;
            }
        } catch (final VariantException e) {
            Log.warning(LOG_TAG, "%s - Exception occurred when converting a VariantMap to StringMap: %s", SELF_TAG, e.getMessage());
            return;
        }
        final String offerActivityId = activity.get(MessagingConstants.EventDataKeys.Optimize.ID);
        final String offerPlacementId = placement.get(MessagingConstants.EventDataKeys.Optimize.ID);
        Log.debug(LOG_TAG, "%s - Retrieved activity id is: (%s), retrieved placement id is: (%s)", SELF_TAG, offerActivityId, offerPlacementId);
        if (!offerActivityId.equals(activityId) || !offerPlacementId.equals(placementId)) {
            Log.debug(LOG_TAG, "%s - ignoring Offers IAM payload, the retrieved activity or placement id does not match the expected activity id: (%s) or expected placement id: (%s).", SELF_TAG, activityId, placementId);
            return;
        }

        List<HashMap<String, Variant>> items = new ArrayList<>();
        final Object itemsList = payload.get(MessagingConstants.EventDataKeys.Optimize.ITEMS);
        if (itemsList instanceof Variant) {
            final List<Variant> variantList = ((VectorVariant) itemsList).getVariantList();
            for (final Object element : variantList) {
                final MapVariant mapVariant = (MapVariant) element;
                items.add((HashMap<String, Variant>) mapVariant.getVariantMap());
            }
        } else {
            items = (List<HashMap<String, Variant>>) itemsList;
        }

        final ArrayList<JsonUtilityService.JSONObject> ruleJsons = new ArrayList<>();
        final ArrayList<Rule> parsedRules = new ArrayList<>();
        // Loop through each rule in the payload and collect the rule json's present.
        // Additionally, extract the image assets and build the asset list for asset caching.
        for (final Map<String, Variant> currentItem : items) {
            final Object dataObject = currentItem.get(MessagingConstants.EventDataKeys.Optimize.DATA);
            final Map<String, String> data;
            try {
                if (dataObject instanceof Variant) {
                    data = ((Variant) dataObject).getStringMap();
                } else {
                    data = (Map<String, String>) dataObject;
                }
            } catch (final VariantException e) {
                Log.warning(LOG_TAG, "%s - Exception occurred when converting s VariantMap to StringMap: %s", SELF_TAG, e.getMessage());
                return;
            }
            final String ruleJson = data.get(MessagingConstants.EventDataKeys.Optimize.CONTENT);
            final JsonUtilityService.JSONObject ruleJsonObject = MessagingUtils.getJsonUtilityService().createJSONObject(ruleJson);
            // we want to discard invalid jsons
            if (ruleJsonObject != null) {
                ruleJsons.add(ruleJsonObject);

                // Parse the "img src" from each html payload in the current rule being processed then
                // add the found assets to the imageAssetList so only current assets will be cached when the
                // RemoteDownloader is used to download the image assets.
                final String imageAsset = extractImageAssetFromJson(ruleJsonObject);
                if (cacheUtilities.assetIsDownloadable(imageAsset)) {
                    imageAssetList.add(imageAsset);
                }
            }
        }
        // download and cache image assets
        cacheUtilities.cacheImageAssets(imageAssetList);
        // create Rule objects from the rule jsons and load them into the RulesEngine
        for (final JsonUtilityService.JSONObject ruleJson : ruleJsons) {
            final Rule parsedRule = parseRuleFromJsonObject(ruleJson);
            if (parsedRule != null) {
                parsedRules.add(parsedRule);
            }
        }
        Log.debug(LOG_TAG, "%s - handleOfferNotification - registering %s rules", SELF_TAG, parsedRules.size());
        messagingModule.replaceRules(parsedRules);
    }

    /**
     * Create an in-app message object then attempt to display it.
     *
     * @param rulesEvent The Rules Engine {@link Event} containing an in-app message definition.
     */
    void createInAppMessage(final Event rulesEvent) {
        final Map triggeredConsequence = (Map) rulesEvent.getEventData().get(MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED);
        if (triggeredConsequence == null || triggeredConsequence.isEmpty()) {
            Log.warning(LOG_TAG,
                    "%s - Unable to create an in-app message, consequences are null or empty.", SELF_TAG);
            return;
        }

        try {
            final Map details = (Map) triggeredConsequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
            if (details == null || details.isEmpty()) {
                Log.warning(LOG_TAG,
                        "%s - Unable to create an in-app message, the consequence details are null or empty", SELF_TAG);
                return;
            }
            final Map mobileParameters = (Map) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS);
            // the asset map is populated when the edge response event containing messages is processed
            Message message = new Message(parent, triggeredConsequence, mobileParameters, cacheUtilities.getAssetMap());
            message.show();
        } catch (final MessageRequiredFieldMissingException exception) {
            Log.warning(LOG_TAG,
                    "%s - Unable to create an in-app message, an exception occurred during creation: %s", SELF_TAG, exception.getLocalizedMessage());
        }
    }

    /**
     * Extracts the image asset url as a {@code String} from the given in-app message payload.
     *
     * @param ruleJson A {@link JsonUtilityService.JSONObject} containing an in-app message payload.
     * @return {@code String} containing the image asset url.
     */
    private String extractImageAssetFromJson(final JsonUtilityService.JSONObject ruleJson) {
        if (ruleJson == null || ruleJson.length() <= 0) {
            Log.warning(LOG_TAG,
                    "%s - Unable to extract the image asset, the provided json is null or empty.");
            return null;
        }
        try {
            final JsonUtilityService.JSONArray rulesJsonArray = ruleJson.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY);
            final JsonUtilityService.JSONObject json = (JsonUtilityService.JSONObject) rulesJsonArray.get(0);
            final JsonUtilityService.JSONArray consequenceJsonArray = json.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY);
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
            final String imageAsset = matcher.group(4);
            Log.trace(LOG_TAG, "Found image asset in html: %s", imageAsset);
            return imageAsset;
        } catch (final JsonException jsonException) {
            Log.warning(LOG_TAG,
                    "%s - An exception occurred during image asset extraction: %s", SELF_TAG, jsonException.getMessage());
            return null;
        }
    }

    private void getActivityAndPlacement(final Event eventToProcess) {
        // placementId = package name
        placementId = App.getApplication().getPackageName();
        // activityId = IMS OrgID. If we have no event passed in to retrieve the activity id from, read activity id from the app manifest.
        if (eventToProcess != null) {
            final Map<String, Object> configSharedState = parent.getApi().getSharedEventState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME,
                    eventToProcess, null);
            if (configSharedState != null) {
                Object edgeResponseActivityId = configSharedState.get(MessagingConstants.SharedState.Configuration.ORG_ID);
                if (edgeResponseActivityId instanceof String) {
                    activityId = (String) edgeResponseActivityId;
                    Log.debug(LOG_TAG,
                            "%s - Got activity id (%s) from event.", SELF_TAG, activityId);
                }
            }
        } else {
            ApplicationInfo applicationInfo = null;
            try {
                final Application application = App.getApplication();
                applicationInfo = App.getApplication().getPackageManager().getApplicationInfo(application.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException exception) {
                Log.warning(LOG_TAG,
                        "%s - An exception occurred when retrieving the manifest metadata: %s", SELF_TAG, exception.getLocalizedMessage());
            }
            activityId = applicationInfo.metaData.getString("activityId");
        }
        // TODO: for manual testing, remove
        // activityId = "xcore:offer-activity:14090235e6b6757a";
        // placementId = "xcore:offer-placement:142be72cd583bd40";
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