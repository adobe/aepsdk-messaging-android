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

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PAYLOAD;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.QUERY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SURFACES;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.SURFACE_BASE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.JSON_CONSEQUENCES_KEY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.JSON_KEY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.UrlUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to handle the retrieval and processing of AJO payloads containing in-app or feed messages. It is also
 * responsible for the display of AJO in-app messages.
 */
class AJOPayloadHandler {
    private final static String SELF_TAG = "InAppNotificationHandler";
    final MessagingExtension parent;
    private final MessagingCacheUtilities messagingCacheUtilities;
    private final ExtensionApi extensionApi;
    private final LaunchRulesEngine launchRulesEngine;
    private Map<String, PropositionInfo> propositionInfo = new HashMap<>();
    private List<PropositionPayload> inMemoryPropositions = new ArrayList<>();
    private String messagesRequestEventId;
    private String lastProcessedRequestEventId;
    private InternalMessage message;

    /**
     * Constructor
     *
     * @param parent       {@link MessagingExtension} instance that is the parent of this {@code InAppNotificationHandler}
     * @param extensionApi {@link ExtensionApi} instance
     * @param rulesEngine  {@link LaunchRulesEngine} instance to use for loading in-app message rule payloads
     */
    AJOPayloadHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine) {
        this(parent, extensionApi, rulesEngine, null, null);
    }

    @VisibleForTesting
    AJOPayloadHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final MessagingCacheUtilities messagingCacheUtilities, final String messagesRequestEventId) {
        this.parent = parent;
        this.extensionApi = extensionApi;
        this.launchRulesEngine = rulesEngine;
        this.messagesRequestEventId = messagesRequestEventId;

        // load cached propositions (if any) when InAppNotificationHandler is instantiated
        this.messagingCacheUtilities = messagingCacheUtilities != null ? messagingCacheUtilities : new MessagingCacheUtilities();
        if (this.messagingCacheUtilities.arePropositionsCached()) {
            List<PropositionPayload> cachedMessages = this.messagingCacheUtilities.getCachedPropositions();
            if (cachedMessages != null && !cachedMessages.isEmpty()) {
                Log.trace(LOG_TAG, SELF_TAG, "Retrieved cached propositions, attempting to load in-app messages into the rules engine.");
                inMemoryPropositions = cachedMessages;
                processPropositions(cachedMessages, false, false, getAppSurface());
            }
        }
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to fetch in-app or feed messages.
     * The app surface used in the request is generated using the application id of the app.
     * If the application id is unavailable, calling this method will do nothing.
     *
     * @param surfacePaths A {@code List<String>} of surface path strings for fetching feed messages, if available.
     */
    void fetchMessages(final List<String> surfacePaths) {
        final String appSurface = getAppSurface();
        if (appSurface.equals("unknown")) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to retrieve in-app or feed messages, cannot read the bundle identifier.");
            return;
        }

        final List<String> surfaceUri = new ArrayList<>();
        if (surfacePaths != null && !surfacePaths.isEmpty()) {
            for (final String surfacePath : surfacePaths) {
                if (!StringUtils.isNullOrEmpty(surfacePath)) {
                    surfaceUri.add(appSurface + File.separator + surfacePath);
                }
            }

            if (surfaceUri.isEmpty()) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to retrieve feed messages, no valid surface paths found.");
                return;
            }
        } else {
            surfaceUri.add(appSurface);
        }

        // create event to be handled by the Edge extension
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();
        personalizationData.put(SURFACES, surfaceUri);
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
        messagesRequestEventId = event.getUniqueIdentifier();

        // send event
        Log.debug(LOG_TAG, SELF_TAG, "Dispatching edge event to retrieve AJO message payloads.");
        extensionApi.dispatch(event);
    }

    /**
     * Validates that the edge response event is a response that we are waiting for. If the returned payload is empty then the Messaging cache
     * and any loaded rules in the Messaging extension's {@link LaunchRulesEngine} are cleared.
     * Non-empty payloads are converted into rules within {@link #processPropositions(List, boolean, boolean, String)} )}.
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        final String requestEventId = MessagingUtils.getRequestEventId(edgeResponseEvent);

        // "TESTING_ID" used in unit and functional testing
        if (!messagesRequestEventId.equals(requestEventId) && !requestEventId.equals("TESTING_ID")) {
            return;
        }

        // if this is an event for a new request, purge cache and update lastProcessedRequestEventId
        boolean clearExistingRules = false;
        if (lastProcessedRequestEventId != requestEventId) {
            clearExistingRules = true;
            lastProcessedRequestEventId = requestEventId;
        }

        final List<Map<String, Object>> payload = DataReader.optTypedListOfMap(Object.class, edgeResponseEvent.getEventData(), PAYLOAD, null);

        // convert the payload into a list of PropositionPayload(s)
        List<PropositionPayload> propositions = null;
        try {
            propositions = MessagingUtils.getPropositionPayloads(payload);
        } catch (final Exception exception) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create PropositionPayload(s), an exception occurred: %s.", exception.getLocalizedMessage());
        }

        final String appSurface = getAppSurface();
        Log.trace(LOG_TAG, SELF_TAG, "Loading in-app message definitions from personalization:decisions network response.");
        processPropositions(propositions, clearExistingRules, true, appSurface);
    }

    /**
     * Attempts to load in-app message rules contained in the provided {@code List<PropositionPayload>}. Any valid rule {@link JSONObject}s
     * found will be registered with the {@link LaunchRulesEngine}.
     *
     * @param propositions       A {@link List<PropositionPayload>} containing in-app message definitions
     * @param clearExistingRules {@code boolean} if true the existing cached propositions are cleared and new message rules are replaced in the {@code LaunchRulesEngine}
     * @param persistChanges     {@code boolean} if true the passed in {@code List<PropositionPayload>} are added to the cache
     * @param expectedScope      {@code String} containing the app surface present in the {@code List<PropositionPayload>}
     */
    private void processPropositions(final List<PropositionPayload> propositions, final boolean clearExistingRules, final boolean persistChanges, final String expectedScope) {
        final List<LaunchRule> parsedRules = new ArrayList<>();
        final Map<String, PropositionInfo> tempPropositionInfo = new HashMap<>();

        if (propositions != null && !propositions.isEmpty()) {
            for (final PropositionPayload proposition : propositions) {
                if (proposition.propositionInfo != null && !expectedScope.equals(proposition.propositionInfo.scope)) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "processPropositions - Ignoring proposition where scope (%s) does not match expected scope (%s).", proposition.propositionInfo.scope, expectedScope);
                    continue;
                }

                for (final PayloadItem payloadItem : proposition.items) {
                    final JSONObject ruleJson = payloadItem.data.getRuleJsonObject();
                    if (ruleJson == null) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "processPropositions - Skipping proposition with no in-app message content.");
                        continue;
                    }

                    final List<LaunchRule> parsedRule = JSONRulesParser.parse(ruleJson.toString(), extensionApi);
                    if (parsedRule == null) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with malformed in-app message content.");
                        continue;
                    }

                    // cache any image assets present in the current rule json's image assets array
                    cacheImageAssetsFromPayload(ruleJson);

                    // store reporting data for this payload for later use
                    tempPropositionInfo.put(getMessageId(ruleJson), proposition.propositionInfo);

                    parsedRules.add(parsedRule.get(0));
                }
            }
        }

        if (clearExistingRules) {
            inMemoryPropositions.clear();
            messagingCacheUtilities.cachePropositions(null);
            propositionInfo = tempPropositionInfo;
            launchRulesEngine.replaceRules(parsedRules);
            Log.debug(LOG_TAG, SELF_TAG, "processPropositions - Successfully loaded %d message(s) into the rules engine for scope %s.", parsedRules.size(), expectedScope);
        } else if (!parsedRules.isEmpty()) {
            propositionInfo.putAll(tempPropositionInfo);
            launchRulesEngine.addRules(parsedRules);
            Log.debug(LOG_TAG, SELF_TAG, "processPropositions - Successfully added %d message(s) into the rules engine for scope %s.", parsedRules.size(), expectedScope);
        } else {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "processPropositions - Ignoring request to load in-app messages for scope %s. The propositions parameter provided was empty.", expectedScope);
        }

        if (persistChanges) {
            // save the proposition payload to the messaging cache
            if (!parsedRules.isEmpty()) {
                inMemoryPropositions.addAll(propositions);
                messagingCacheUtilities.cachePropositions(inMemoryPropositions);
            }
        } else {
            inMemoryPropositions.addAll(propositions);
        }
    }

    /**
     * Extracts the message id from the provided rule payload's consequence.
     *
     * @return a {@code String> containing the consequence id
     */
    private String getMessageId(final JSONObject rulePayload) {
        final JSONObject consequences;
        try {
            consequences = rulePayload.getJSONArray(JSON_KEY).getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY).getJSONObject(0);
            return consequences.getString(MESSAGE_CONSEQUENCE_ID);
        } catch (final JSONException exception) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception occurred when retrieving MessageId from the rule consequence: %s.", exception.getLocalizedMessage());
            return null;
        }
    }

    private String getAppSurface() {
        final String packageName = ServiceProvider.getInstance().getDeviceInfoService().getApplicationPackageName();
        return StringUtils.isNullOrEmpty(packageName) ? "unknown" : SURFACE_BASE + packageName;
    }

    /**
     * Creates an in-app message object then attempts to display it.
     *
     * @param triggeredConsequence A {@link RuleConsequence} containing an in-app message definition.
     */
    void createInAppMessage(final RuleConsequence triggeredConsequence) {
        if (triggeredConsequence == null) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create an in-app message, consequences are null.");
            return;
        }

        final String consequenceType = triggeredConsequence.getType();

        // ensure we have a CJM IAM payload before creating a message
        if (StringUtils.isNullOrEmpty(consequenceType)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create an in-app message, missing consequence type.");
            return;
        }

        if (!consequenceType.equals(MESSAGE_CONSEQUENCE_CJM_VALUE)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create an in-app message, unknown message consequence type: %s.", consequenceType);
            return;
        }

        try {
            final Map<String, Object> details = triggeredConsequence.getDetail();
            if (MapUtils.isNullOrEmpty(details)) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to create an in-app message, the consequence details are null or empty");
                return;
            }

            final Map<String, Object> mobileParameters = (Map<String, Object>) details.get(MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS);
            message = new InternalMessage(parent, triggeredConsequence, mobileParameters, messagingCacheUtilities.getAssetsMap());
            message.propositionInfo = propositionInfo.get(message.getId());
            message.trigger();
            message.show(true);
        } catch (final MessageRequiredFieldMissingException exception) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to create an in-app message, an exception occurred during creation: %s", exception.getLocalizedMessage());
        }
    }

    /**
     * Cache any asset URL's present in the {@link com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence} detail {@link JSONObject}.
     *
     * @param ruleJsonObject A {@link JSONObject} containing an in-app message definition.
     */
    private void cacheImageAssetsFromPayload(final JSONObject ruleJsonObject) {
        List<String> remoteAssetsList = new ArrayList<>();
        try {
            final JSONArray rulesArray = ruleJsonObject.getJSONArray(JSON_KEY);
            final JSONArray consequence = rulesArray.getJSONObject(0).getJSONArray(JSON_CONSEQUENCES_KEY);
            final JSONObject details = consequence.getJSONObject(0).getJSONObject(MESSAGE_CONSEQUENCE_DETAIL);
            final JSONArray remoteAssets = details.getJSONArray(MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
            if (remoteAssets.length() != 0) {
                for (int index = 0; index < remoteAssets.length(); index++) {
                    final String imageAssetUrl = (String) remoteAssets.get(index);
                    if (UrlUtils.isValidUrl(imageAssetUrl)) {
                        Log.debug(LOG_TAG, SELF_TAG, "Image asset to be cached (%s) ", imageAssetUrl);
                        remoteAssetsList.add(imageAssetUrl);
                    }
                }
            }
        } catch (final JSONException jsonException) {
            Log.warning(LOG_TAG, SELF_TAG, "An exception occurred retrieving the remoteAssets array from the rule json payload: %s", jsonException.getLocalizedMessage());
            return;
        }
        messagingCacheUtilities.cacheImageAssets(remoteAssetsList);
    }

    // for testing, the size of the proposition info map should always mirror the number of rules currently loaded
    @VisibleForTesting
    int getRuleCount() {
        return propositionInfo.size();
    }
}