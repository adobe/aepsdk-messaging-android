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
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.CJM_XDM;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.SURFACE_BASE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.QUERY;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SURFACES;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PAYLOAD;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONDITION_KEY;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.REQUEST_EVENT_ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

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
    private final Map<String, PropositionInfo> propositionInfoMap = new HashMap<>();
    private String requestMessagesEventId;

    /**
     * Constructor
     *
     * @param parent                  {@link MessagingInternal} instance that is the parent of this {@code InAppNotificationHandler}
     * @param messagingCacheUtilities {@link MessagingCacheUtilities} instance to use for caching in-app message rule jsons and image assets
     */
    InAppNotificationHandler(final MessagingInternal parent, final MessagingCacheUtilities messagingCacheUtilities) {
        this.parent = parent;
        this.messagingCacheUtilities = messagingCacheUtilities;
        // create a module to get access to the Core rules engine for adding AJO in-app message rules
        messagingModule = new Module("Messaging", MobileCore.getCore().eventHub) {
        };
        // load cached propositions (if any) when InAppNotificationHandler is instantiated
        if (messagingCacheUtilities != null && messagingCacheUtilities.arePropositionsCached()) {
            List<PropositionPayload> cachedMessages = messagingCacheUtilities.getCachedPropositions();
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                Log.trace(LOG_TAG, "%s - Retrieved cached propositions, attempting to load in-app messages into the rules engine.", SELF_TAG);
                processPropositions(cachedMessages);
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

        final Event event = new Event.Builder(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                null)
                .setEventData(eventData)
                .build();

        // used for ensuring that the messaging extension is responding to the correct handle
        requestMessagesEventId = event.getUniqueIdentifier();

        // send event
        Log.debug(LOG_TAG, "%s - Dispatching edge event to fetch in-app messages.", SELF_TAG);
        MessagingUtils.sendEvent(event, MessagingConstants.EventDispatchErrors.PERSONALIZATION_REQUEST_ERROR);
    }

    /**
     * Validates that the edge response event is a response that we are waiting for. If the returned payload is empty then the Messaging cache
     * is cleared. Non empty payloads are converted into rules within {@link #processPropositions(List)}.
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        final String requestEventId = getRequestEventId(edgeResponseEvent);
        // "TESTING_ID" used in unit and functional testing
        if (!requestMessagesEventId.equals(requestEventId) && !requestEventId.equals("TESTING_ID")) {
            return;
        }
        final List<Map<String, Object>> payload = (ArrayList<Map<String, Object>>) edgeResponseEvent.getEventData().get(PAYLOAD);
        final List<PropositionPayload> propositions = MessagingUtils.getPropositionPayloads(payload);
        if (propositions == null || propositions.isEmpty()) {
            Log.trace(LOG_TAG, "%s - Payload for in-app messages was empty. Clearing local cache.", SELF_TAG);
            messagingCacheUtilities.clearCachedData();
            return;
        }
        // save the proposition payload to the messaging cache
        messagingCacheUtilities.cachePropositions(propositions);
        Log.trace(LOG_TAG, "%s - Loading in-app messages definitions from network response.", SELF_TAG);
        processPropositions(propositions);
    }

    /**
     * Attempts to load in-app message rules contained in the provided {@code List<PropositionPayload>}. Any valid rule {@link JsonUtilityService.JSONObject}s
     * found will be registered with the {@link RulesEngine}.
     *
     * @param propositions A {@link List<PropositionPayload>} containing in-app message definitions
     */
    private void processPropositions(final List<PropositionPayload> propositions) {
        final List<JsonUtilityService.JSONObject> foundRules = new ArrayList<>();
        for (final PropositionPayload proposition : propositions) {
            final String appSurface = App.getAppContext().getPackageName();
            Log.trace(LOG_TAG, "%s - Using the application identifier (%s) to validate the notification payload.", SELF_TAG, appSurface);

            final String scope = proposition.propositionInfo.scope;
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

            for (final PayloadItem payloadItem : proposition.items) {
                final JsonUtilityService.JSONObject ruleJson = payloadItem.data.getRuleJsonObject();
                if (ruleJson != null) {
                    foundRules.add(ruleJson);

                    // cache any image assets present in the current rule json's image assets array
                    cacheImageAssetsFromPayload(ruleJson);

                    // store reporting data for this payload for later use
                    storePropositionInfo(getMessageId(ruleJson), proposition);
                }
            }
        }

        registerRules(foundRules);
    }

    /**
     * Parses each item in the {@code List<JsonUtilityService.JSONObject>>} then adds all valid rules to the {@link RulesEngine}.
     *
     * @param rulePayload a {@link List<JsonUtilityService.JSONObject>>} containing a rule payload.
     */
    private void registerRules(final List<JsonUtilityService.JSONObject> rulePayload) {
        final List<Rule> parsedRules = new ArrayList<>();

        // create rule objects from the rule JSONs and load them into the rule engine
        for (final JsonUtilityService.JSONObject ruleJson : rulePayload) {
            final Rule parsedRule = parseRuleFromJsonObject(ruleJson);
            if (parsedRule != null) {
                parsedRules.add(parsedRule);
            }
        }
        Log.debug(LOG_TAG, "%s - registerRules - registering %d rules", SELF_TAG, parsedRules.size());
        messagingModule.replaceRules(parsedRules);
    }

    /**
     * Creates a mapping between the message id and the {@code PropositionInfo} to use for in-app message interaction tracking.
     *
     * @param messageId a {@code String} containing the rule consequence id
     * @param propositionPayload a {@link PropositionPayload} containing an in-app message payload
     */
    private void storePropositionInfo(final String messageId, final PropositionPayload propositionPayload) {
        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(LOG_TAG, "Unable to associate proposition information for in-app message. MessageId unavailable in rule consequence.");
            return;
        }
        propositionInfoMap.put(messageId, propositionPayload.propositionInfo);
    }

    /**
     * Returns a {@code PropositionInfo} object to use for in-app message interaction tracking.
     *
     * @param messageId a {@code String} containing the rule consequence id to use for retrieving a {@link PropositionInfo} object
     * @return a {@code PropositionInfo} containing XDM data necessary for tracking in-app interactions with Adobe Journey Optimizer
     */
    private PropositionInfo getPropositionInfoForMessageId(final String messageId) {
        return propositionInfoMap.get(messageId);
    }

    /**
     * Extracts the message id from the provided rule payload's consequence.
     *
     * @return a {@code String> containing the consequence id
     */
    private String getMessageId(final JsonUtilityService.JSONObject rulePayload) {
        final JsonUtilityService.JSONObject consequences;
        try {
            consequences = rulePayload.getJSONArray(JSON_KEY).getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY).getJSONObject(0);
            return consequences.getString(MESSAGE_CONSEQUENCE_ID);
        } catch (final JsonException exception) {
            Log.warning(LOG_TAG, "Exception occurred when retrieving MessageId from the rule consequence: %s.", exception.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Retrieves the request event id from the edge response event.
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    private String getRequestEventId(final Event edgeResponseEvent) {
        final Map<String, Object> eventData = edgeResponseEvent.getEventData();
        return (String) eventData.get(REQUEST_EVENT_ID);
    }

    /**
     * Create an in-app message object then attempt to display it.
     *
     * @param rulesEvent The Rules Engine {@link Event} containing an in-app message definition.
     */
    void createInAppMessage(final Event rulesEvent) {
        final Map<String, Object> triggeredConsequence = (Map<String, Object>) rulesEvent.getEventData().get(CONSEQUENCE_TRIGGERED);
        if (MessagingUtils.isMapNullOrEmpty(triggeredConsequence)) {
            Log.debug(LOG_TAG,
                    "%s - Unable to create an in-app message, consequences are null or empty.", SELF_TAG);
            return;
        }

        final String consequenceType = (String) triggeredConsequence.get(MESSAGE_CONSEQUENCE_TYPE);

        // ensure we have a CJM IAM payload before creating a message
        if (StringUtils.isNullOrEmpty(consequenceType)) {
            Log.debug(LOG_TAG,
                    "%s - Unable to create an in-app message, missing consequence type: %s.", SELF_TAG);
            return;
        }

        if (!consequenceType.equals(MESSAGE_CONSEQUENCE_CJM_VALUE)) {
            Log.debug(LOG_TAG,
                    "%s - Unable to create an in-app message, unknown message consequence type: %s.", SELF_TAG, consequenceType);
            return;
        }

        try {
            final Map<String, Object> details = (Map<String, Object>) triggeredConsequence.get(MESSAGE_CONSEQUENCE_DETAIL);
            if (MessagingUtils.isMapNullOrEmpty(details)) {
                Log.warning(LOG_TAG,
                        "%s - Unable to create an in-app message, the consequence details are null or empty", SELF_TAG);
                return;
            }
            final Map<String, Object> mobileParameters = (Map<String, Object>) details.get(MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS);
            final Message message = new Message(parent, triggeredConsequence, mobileParameters, messagingCacheUtilities.getAssetsMap());
            message.propositionInfo = getPropositionInfoForMessageId(message.id);
            message.trigger();
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
            final JsonUtilityService.JSONArray rulesArray = ruleJsonObject.getJSONArray(JSON_KEY);
            final JsonUtilityService.JSONArray consequence = rulesArray.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            final JsonUtilityService.JSONObject details = consequence.getJSONObject(0).getJSONObject(MESSAGE_CONSEQUENCE_DETAIL);
            final JsonUtilityService.JSONArray remoteAssets = details.getJSONArray(MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
            if (remoteAssets.length() != 0) {
                for (int index = 0; index < remoteAssets.length(); index++) {
                    final String imageAssetUrl = (String) remoteAssets.get(index);
                    if (StringUtils.stringIsUrl(imageAssetUrl)) {
                        Log.debug(LOG_TAG,
                                "%s - Image asset to be cached (%s) ", SELF_TAG, imageAssetUrl);
                        remoteAssetsList.add(imageAssetUrl);
                    }
                }
            }
        } catch (final JsonException jsonException) {
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
                final JsonUtilityService.JSONObject ruleConditionJsonObject = ruleObject.getJSONObject(JSON_CONDITION_KEY);
                final RuleCondition condition = RuleCondition.ruleConditionFromJson(ruleConditionJsonObject);
                // get consequences
                final JsonUtilityService.JSONArray consequenceJSONArray = ruleObject.getJSONArray(JSON_CONSEQUENCES_KEY);
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
                    eventData.putVariantMap(CONSEQUENCE_TRIGGERED,
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