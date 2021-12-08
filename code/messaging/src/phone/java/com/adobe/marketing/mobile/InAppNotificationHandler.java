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

import com.google.android.gms.common.util.ArrayUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InAppNotificationHandler {
    // private vars
    private final static String SELF_TAG = "InAppNotificationHandler";
    private final ArrayList<ImageAsset> imageAssetList = new ArrayList<>();
    private Module messagingModule;
    private MessagingCacheUtilities cacheUtilities;
    private String activityId;
    private String placementId;
    // package private
    final MessagingInternal parent;

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
     * Generates and dispatches an event prompting the Personalization extension to fetch in-app messages.
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
     * Converts the rules json present in the Edge response event payload into a
     * list of rules then loads them in the rules engine.
     *
     * @param payload           An {@link Map<String, Variant>} containing the personalization decision payload retrieved from Offers
     * @param edgeResponseEvent The {@link Event} which contained the personalization decision payload. This can be null if cached
     *                          messages are being loaded.
     */
    void handleOfferNotificationPayload(final Map<String, Variant> payload, final Event edgeResponseEvent) {
        if (payload == null || payload.size() == 0) {
            Log.warning(LOG_TAG, "handleOfferNotification - Aborting handling of the Offers IAM payload because it is null or empty.");
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
            Log.warning(LOG_TAG, "handleOfferNotification - Exception occurred when converting the VariantMap to StringMap: %s", e.getMessage());
            return;
        }
        final String offerActivityId = activity.get(MessagingConstants.EventDataKeys.Optimize.ID);
        final String offerPlacementId = placement.get(MessagingConstants.EventDataKeys.Optimize.ID);
        Log.debug(LOG_TAG, "handleOfferNotification - Retrieved activity id is: (%s), retrieved placement id is: (%s)", offerActivityId, offerPlacementId);
        if (!offerActivityId.equals(activityId) || !offerPlacementId.equals(placementId)) {
            Log.debug(LOG_TAG, "handleOfferNotification - ignoring Offers IAM payload, the retrieved activity or placement id does not match the expected activity id: (%s) or expected placement id: (%s).", activityId, placementId);
            return;
        }

        List<HashMap<String, Variant>> items = new ArrayList<>();
        Object itemsList = payload.get(MessagingConstants.EventDataKeys.Optimize.ITEMS);
        if (itemsList instanceof Variant) {
            List<Variant> variantList = ((VectorVariant) itemsList).getVariantList();
            for (Object element : variantList) {
                final MapVariant mapVariant = (MapVariant) element;
                items.add((HashMap<String, Variant>) mapVariant.getVariantMap());
            }
        } else {
            items = (List<HashMap<String, Variant>>) itemsList;
        }

        final ArrayList<JsonUtilityService.JSONObject> rawRuleJsons = new ArrayList<>();
        final ArrayList<Rule> parsedRules = new ArrayList<>();
        // collect image assets from the payload
        for (Map<String, Variant> currentItem : items) {
            final Object itemObject = currentItem.get(MessagingConstants.EventDataKeys.Optimize.DATA);
            final Map<String, String> data;
            try {
                if (itemObject instanceof Variant) {
                    data = ((Variant) itemObject).getStringMap();
                } else {
                    data = (Map<String, String>) itemObject;
                }
            } catch (final VariantException e) {
                Log.warning(LOG_TAG, "handleOfferNotification - Exception occurred when converting the VariantMap to StringMap: %s", e.getMessage());
                return;
            }
            final String ruleJson = data.get(MessagingConstants.EventDataKeys.Optimize.CONTENT);
            final JsonUtilityService.JSONObject ruleJsonObject = MessagingUtils.getJsonUtilityService().createJSONObject(ruleJson);
            rawRuleJsons.add(ruleJsonObject);

            // parse <img src="xxx" alt "xxx"> from each item in items.
            // if its not cached, cache it using MessagingUtils.cacheRetrievedImageAsset then replace it in the rules json.
            // if it is already cached, just replace it in the rules json.
            getImageAssetFromRule(ruleJsonObject);
        }
        // download and cache image assets
        cacheUtilities.cacheImageAssets(imageAssetList);
        // replace http url's in each rule with the cached asset
        for (final JsonUtilityService.JSONObject ruleJson : rawRuleJsons) {
            final JsonUtilityService.JSONObject rulesJsonObject = replaceHttpUrlWithCachedImageInRule(ruleJson);
            final Rule parsedRule = parseRuleFromJsonObject(rulesJsonObject);
            if (parsedRule != null) {
                parsedRules.add(parsedRule);
            }
        }
        Log.debug(LOG_TAG, "handleOfferNotification - registering %s rules", parsedRules.size());
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
            Log.warning(MessagingConstants.LOG_TAG,
                    "Unable to create an in-app message, consequences are null or empty.");
            return;
        }

        try {
            final Map details = (Map) triggeredConsequence.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
            if (details == null || details.isEmpty()) {
                Log.warning(MessagingConstants.LOG_TAG,
                        "Unable to create an in-app message, the consequence details are null or empty");
                return;
            }
            final Map mobileParameters = (Map) details.get(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS);
            Message message = new Message(parent, triggeredConsequence, mobileParameters);
            message.show();
        } catch (final MessageRequiredFieldMissingException exception) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "Unable to create an in-app message, an exception occurred during creation: %s", exception.getLocalizedMessage());
        }
    }

    private void getImageAssetFromRule(final JsonUtilityService.JSONObject ruleJson) {
        final ImageAsset imageAsset = extractImageAssetFromJson(ruleJson);
        if (cacheUtilities.assetIsDownloadable(imageAsset.getAssetUrl())) {
            imageAssetList.add(imageAsset);
        }
    }

    private JsonUtilityService.JSONObject replaceHttpUrlWithCachedImageInRule(final JsonUtilityService.JSONObject ruleJson) {
        final JsonUtilityService.JSONObject ruleJsonCopy = ruleJson;
        final ImageAsset imageAsset = extractImageAssetFromJson(ruleJson);
        // replace http url with cache url, TODO: handle "alt" images present for image asset
        final String cacheAssetUrl = cacheUtilities.getCachedImageAssetUrl(imageAsset.getAssetUrl()) + imageAsset.getAssetExtension();
        try {
            final JsonUtilityService.JSONArray rulesJsonArray = ruleJson.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY);
            final JsonUtilityService.JSONObject json = (JsonUtilityService.JSONObject) rulesJsonArray.get(0);
            final JsonUtilityService.JSONArray consequenceJsonArray = json.getJSONArray("consequences");
            final JsonUtilityService.JSONObject consequence = (JsonUtilityService.JSONObject) consequenceJsonArray.get(0);
            final JsonUtilityService.JSONObject detail = consequence.getJSONObject("detail");
            String html = detail.getString("html");
            html = html.substring(0, imageAsset.getBeginIndex()) + "\"" + cacheAssetUrl + "\"" + html.substring(imageAsset.getEndIndex(), html.length() - 1);
            // rebuild rulesJson using modified html
            detail.put("html", html);
            consequence.put("detail", detail);
            consequenceJsonArray.put(consequence);
            json.put("consequences", consequenceJsonArray);
            rulesJsonArray.put(json);
            ruleJsonCopy.put("rules", rulesJsonArray);
            return ruleJsonCopy;
        } catch (final JsonException jsonException) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "An exception occurred during http image asset replacement: %s", jsonException.getMessage());
            // return rules unchanged if any exception occurred during asset replacement
            return ruleJson;
        }
    }

    // parse <img src="xxx" alt "xxx"> from each item in items.
    private ImageAsset extractImageAssetFromJson(final JsonUtilityService.JSONObject ruleJson) {
        try {
            final JsonUtilityService.JSONArray rulesJsonArray = ruleJson.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY);
            final JsonUtilityService.JSONObject json = (JsonUtilityService.JSONObject) rulesJsonArray.get(0);
            final JsonUtilityService.JSONArray consequenceJsonArray = json.getJSONArray("consequences");
            final JsonUtilityService.JSONObject consequence = (JsonUtilityService.JSONObject) consequenceJsonArray.get(0);
            final JsonUtilityService.JSONObject detail = consequence.getJSONObject("detail");
            String html = detail.getString("html");
            final int beginIndex = html.lastIndexOf("<img src=") + 9;
            final int endIndex = html.lastIndexOf("alt=") + 6;
            final String imageAssetUrl = (html.substring(beginIndex, endIndex)).replace("\"", "");
            final String[] imageAssetTokens = imageAssetUrl.split(" ");
            return new ImageAsset(imageAssetTokens[0], beginIndex, endIndex);
        }  catch (final JsonException jsonException) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "An exception occurred during image asset extraction: %s", jsonException.getMessage());
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
                    Log.debug(MessagingConstants.LOG_TAG,
                            "Got activity id (%s) from event.", activityId);
                }
            }
        } else {
            ApplicationInfo applicationInfo = null;
            try {
                final Application application = App.getApplication();
                applicationInfo = App.getApplication().getPackageManager().getApplicationInfo(application.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException exception) {
                Log.warning(MessagingConstants.LOG_TAG,
                        "An exception occurred when retrieving the manifest metadata: %s", exception.getLocalizedMessage());
            }
            activityId = applicationInfo.metaData.getString("activityId");
        }
        // TODO: for testing, remove
        activityId = "xcore:offer-activity:14090235e6b6757a";
        placementId = "xcore:offer-placement:142be72cd583bd40";
    }

    /**
     * Parses rules from the provided {@code jsonObject} into a {@code Rule}.
     *
     * @param rulesJsonObject {@code JSONObject} containing a rule and consequences
     * @return a {@code Rule} object that was parsed from the input {@code jsonObject} or null if parsing failed.
     */
    private Rule parseRuleFromJsonObject(final JsonUtilityService.JSONObject rulesJsonObject) {
        if (rulesJsonObject == null) {
            Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to parse rules, input jsonObject is null.");
            return null;
        }

        JsonUtilityService.JSONArray rulesJsonArray;

        try {
            rulesJsonArray = rulesJsonObject.getJSONArray(MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY);
        } catch (final JsonException e) {
            Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to parse rules (%s)", e);
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
                Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to parse individual rule json (%s)", e.getLocalizedMessage());
            } catch (final UnsupportedConditionException e) {
                Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to parse individual rule conditions (%s)", e);
            } catch (final IllegalArgumentException e) {
                Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to create rule object (%s)", e);
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
                    "generateConsequenceEvents -  The passed in consequence array is null, so returning an empty consequence events list.");
            return parsedEvents;
        }

        final JsonUtilityService jsonUtilityService = MessagingUtils.getJsonUtilityService();

        if (jsonUtilityService == null) {
            Log.debug(LOG_TAG,
                    "generateConsequenceEvents -  JsonUtility service is not available, returning empty consequence events list.");
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
                Log.warning(MessagingConstants.LOG_TAG,
                        "Unable to convert consequence json object to a variant.");
                return null;
            }
        }

        return parsedEvents;
    }
}