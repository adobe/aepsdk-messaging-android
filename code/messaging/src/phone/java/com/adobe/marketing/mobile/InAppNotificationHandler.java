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

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

class InAppNotificationHandler {
    // private vars
    private final static String SELF_TAG = "InAppNotificationHandler";
    private final Module messagingModule;
    private String activityId;
    private String placementId;

    // package private
    final MessagingInternal parent;

    // message customization poc
    private Map<String, Object> rawMessageParameters = new HashMap<>();

    /**
     * Constructor
     *
     * @param parent {@link MessagingInternal} instance that is the parent of this {@code InAppNotificationHandler}
     */
    InAppNotificationHandler(MessagingInternal parent) {
        this.parent = parent;
        // create a module to get access to the Core rules engine for adding ODE rules
        messagingModule = new Module("Messaging", MobileCore.getCore().eventHub) {
        };
    }

    /**
     * Generates and dispatches an event prompting the Personalization extension to fetch in-app messages.
     */
    void fetchMessages() {
        // activity and placement are both required for message definition retrieval
        getActivityAndPlacement(null);
        if(StringUtils.isNullOrEmpty(activityId) || StringUtils.isNullOrEmpty(placementId)) {
            Log.trace(LOG_TAG, "%s - Unable to retrieve message definitions - activity and placement ids are both required.", SELF_TAG);
            return;
        }
        // create event to be handled by optimize
        final HashMap<String, Object> optimizeData = new HashMap<>();
        final HashMap<String, String> decisionScopes = new HashMap<>();
        decisionScopes.put(MessagingConstants.EventDataKeys.Optimize.NAME, getEncodedDecisionScope(activityId, placementId));
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
     * @param activityId {@code String} containing the activity id
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
     * @param edgeResponseEvent An Edge {@link Event} containing a personalization decision payload retrieved from Offers.
     */
    void handleOfferNotificationPayload(final Event edgeResponseEvent) {
        getActivityAndPlacement(edgeResponseEvent);
        // TODO: FOR TESTING ONLY, REMOVE WHEN OPTIMIZE EXTENSION ADDED ========================================================
        messagingModule.unregisterAllRules();
        // TODO: ===============================================================================================================
        final JSONObject payload = (JSONObject) edgeResponseEvent.getEventData().get(MessagingConstants.EventDataKeys.Offers.PAYLOAD);
        if (payload == null || payload.length() == 0) {
            Log.warning(LOG_TAG, "handleOfferNotification - Aborting handling of the Offers IAM payload because it is null or empty.");
            return;
        }

        try {
            final JSONObject activity = payload.getJSONObject(MessagingConstants.EventDataKeys.Optimize.ACTIVITY);
            final JSONObject placement = payload.getJSONObject(MessagingConstants.EventDataKeys.Optimize.PLACEMENT);
            final String offerActivityId = (String) activity.get(MessagingConstants.EventDataKeys.Optimize.ID);
            final String offerPlacementId = (String) placement.get(MessagingConstants.EventDataKeys.Optimize.ID);
            if (!offerActivityId.equals(activityId) || !offerPlacementId.equals(placementId)) {
                Log.warning(LOG_TAG, "handleOfferNotification - ignoring Offers IAM payload, the expected offer activity id or placement id is missing.");
                return;
            }
            final JSONArray items = payload.getJSONArray(MessagingConstants.EventDataKeys.Optimize.ITEMS);

            for (int index = 0; index < items.length(); index++) {
                final String ruleJson = items.getJSONObject(index).getJSONObject(MessagingConstants.EventDataKeys.Offers.DATA).getString(MessagingConstants.EventDataKeys.Offers.CONTENT);
                final JsonUtilityService.JSONObject rulesJsonObject = MessagingUtils.getJsonUtilityService().createJSONObject(ruleJson);
                final Rule parsedRule = parseRuleFromJsonObject(rulesJsonObject);
                if (parsedRule != null) {
                    Log.debug(LOG_TAG, "handleOfferNotification - registering rule: %s", parsedRule.toString());
                    messagingModule.registerRule(parsedRule);
                }
            }
        } catch (JSONException e) {
            Log.debug(LOG_TAG, "handleOfferNotification - JSON exception when attempting to retrieve rules json from the edge response event: %s", e.getLocalizedMessage());
        }
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
            Message message = new Message(parent, triggeredConsequence, rawMessageParameters);
            message.show();
        } catch (MessageRequiredFieldMissingException exception) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "Unable to create an in-app message, an exception occurred during creation: %s", exception.getLocalizedMessage());
            return;
        }
    }

    private void getActivityAndPlacement(final Event eventToProcess) {
        this.placementId = App.getApplication().getPackageName();
        if(eventToProcess != null) {
            final Map<String, Object> configSharedState = parent.getApi().getSharedEventState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME,
                    eventToProcess, null);
            if (configSharedState != null) {
                Object edgeResponseActivityId = configSharedState.get(MessagingConstants.SharedState.Configuration.ORG_ID);
                if (edgeResponseActivityId instanceof String) {
                    this.activityId = (String) edgeResponseActivityId;
                }
            }
        } else { // if we have no event passed in to retrieve the activity id from, read activity id from the app manifest
            ApplicationInfo applicationInfo;
            try {
                final Application application = App.getApplication();
                applicationInfo = App.getApplication().getPackageManager().getApplicationInfo(application.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException exception) {
                Log.warning(MessagingConstants.LOG_TAG,
                        "An exception occurred when retrieving the manifest metadata: %s", exception.getLocalizedMessage());
                return;
            }
            activityId = applicationInfo.metaData.getString("activityId");
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
                // message customization poc
                // get message settings
                final JsonUtilityService.JSONObject consequenceJson = (JsonUtilityService.JSONObject) consequenceJSONArray.get(0);
                final JsonUtilityService.JSONObject detailJson = consequenceJson.getJSONObject(MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL);
                final JsonUtilityService.JSONObject parametersJson = detailJson.getJSONObject(MessagingConstants.EventDataKeys.MobileParametersKeys.MOBILE_PARAMETERS);
                // convert JsonUtilityService.JSONObject to JSONObject
                try {
                    JSONObject convertedJSON = new JSONObject(parametersJson.toString());
                    rawMessageParameters = MessagingUtils.toMap(convertedJSON);
                } catch (JSONException jsonException) {
                    Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to convert from JsonUtilityService.JSONObject to JSONObject (%s)", jsonException.getLocalizedMessage());
                }

            } catch (final JsonException e) {
                Log.debug(LOG_TAG, "parseRulesFromJsonObject -  Unable to parse individual rule json (%s)", e);
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