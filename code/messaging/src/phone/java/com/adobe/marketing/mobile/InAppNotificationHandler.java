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
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID;

class InAppNotificationHandler {
    // private vars
    private final static String SELF_TAG = "InAppNotificationHandler";
    private final Module messagingModule;
    private String activityId;
    private String placementId;

    // package private
    final MessagingInternal parent;

    // testing vars
    private final static int MAX_ITEM_COUNT = 30;

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
        // create event to be handled by offers
        final ArrayList<Object> decisionScopes = new ArrayList<>();
        final HashMap<String, Object> offersIdentifiers = new HashMap<>();
        offersIdentifiers.put(MessagingConstants.EventDataKeys.Offers.ITEM_COUNT, Variant.fromInteger(MAX_ITEM_COUNT));
        offersIdentifiers.put(MessagingConstants.EventDataKeys.Offers.ACTIVITY_ID, Variant.fromString(activityId));
        offersIdentifiers.put(MessagingConstants.EventDataKeys.Offers.PLACEMENT_ID, Variant.fromString(placementId));
        decisionScopes.add(offersIdentifiers);

        final HashMap<String, Object> eventData = new HashMap<>();
        eventData.put(MessagingConstants.EventDataKeys.Offers.TYPE, MessagingConstants.EventDataKeys.Offers.PREFETCH);
        eventData.put(MessagingConstants.EventDataKeys.Offers.DECISION_SCOPES, decisionScopes);

        final Event messageFetchEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_REFRESH_IAM, MessagingConstants.EventType.OFFERS, MessagingConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        // send event
        MobileCore.dispatchEvent(messageFetchEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.warning(LOG_TAG, "%s - Error in dispatching event for fetching messages from Offers.", SELF_TAG);
            }
        });
    }

    /**
     * Converts the rules json present in the Edge response event payload into a
     * list of rules then loads them in the rules engine.
     *
     * @param edgeResponseEvent An Edge {@link Event} containing a personalization decision payload retrieved from Offers.
     */
    void handleOfferNotificationPayload(final Event edgeResponseEvent) {
        getActivityAndPlacement(edgeResponseEvent);
        final JSONObject payload = (JSONObject) edgeResponseEvent.getEventData().get(MessagingConstants.EventDataKeys.Offers.PAYLOAD);
        if (payload == null || payload.length() == 0) {
            Log.warning(LOG_TAG, "handleOfferNotification - Aborting handling of the Offers IAM payload because it is null or empty.");
            return;
        }

        try {
            final JSONObject activity = payload.getJSONObject(MessagingConstants.EventDataKeys.Offers.ACTIVITY);
            final JSONObject placement = payload.getJSONObject(MessagingConstants.EventDataKeys.Offers.PLACEMENT);
            final String offerActivityId = (String) activity.get(MessagingConstants.EventDataKeys.Offers.ID);
            final String offerPlacementId = (String) placement.get(MessagingConstants.EventDataKeys.Offers.ID);
            if (!offerActivityId.equals(activityId) || !offerPlacementId.equals(placementId)) {
                Log.warning(LOG_TAG, "handleOfferNotification - ignoring Offers IAM payload, the expected offer activity id or placement id is missing.");
                return;
            }
            final JSONArray items = payload.getJSONArray(MessagingConstants.EventDataKeys.Offers.ITEMS);
            for (int index = 0; index < items.length(); index++) {
                final String ruleJson = items.getJSONObject(index).getJSONObject(MessagingConstants.EventDataKeys.Offers.DATA).getString(MessagingConstants.EventDataKeys.Offers.CONTENT);
                final JsonUtilityService.JSONObject rulesJsonObject = MessagingUtils.getJsonUtilityService().createJSONObject(ruleJson);
                final Rule parsedRule = parseRuleFromJsonObject(rulesJsonObject);
                if (parsedRule != null) {
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
            Message message = new Message(parent, triggeredConsequence);
            message.show();
        } catch (MessageRequiredFieldMissingException exception) {
            Log.warning(MessagingConstants.LOG_TAG,
                    "Unable to create an in-app message, an exception occurred during creation: %s", exception.getLocalizedMessage());
            return;
        }
    }

    private void getActivityAndPlacement(final Event eventToProcess) {
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
                final Context context = application.getApplicationContext();
                applicationInfo = application.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException exception) {
                Log.warning(MessagingConstants.LOG_TAG,
                        "An exception occurred when retrieving the manifest metadata: %s", exception.getLocalizedMessage());
                return;
            }
            activityId = applicationInfo.metaData.getString("activityId");
        }
        this.placementId = App.getApplication().getPackageName();
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
                final List<Event> consequences = generateConsequenceEvents(ruleObject.getJSONArray(
                        MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY));
                if (consequences != null) {
                    parsedRule = new Rule(condition, consequences);
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