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
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_NOTIFICATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Inbound;
import com.adobe.marketing.mobile.InboundType;
import com.adobe.marketing.mobile.Proposition;
import com.adobe.marketing.mobile.PropositionItem;
import com.adobe.marketing.mobile.Surface;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.launch.rulesengine.json.JSONRulesParser;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.UrlUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class is used to handle the retrieval and processing of AJO payloads containing in-app or feed messages. It is also
 * responsible for the display of AJO in-app messages.
 */
class EdgePersonalizationResponseHandler {
    private final static String SELF_TAG = "EdgePersonalizationResponseHandler";
    final MessagingExtension parent;
    private final MessagingCacheUtilities messagingCacheUtilities;
    private final ExtensionApi extensionApi;
    private final LaunchRulesEngine launchRulesEngine;
    private final FeedRulesEngine feedRulesEngine;

    private Map<Surface, List<Proposition>> propositions = new HashMap<>();
    private final Map<String, PropositionInfo> propositionInfo = new HashMap<>();
    private Map<Surface, List<Inbound>> inboundMessages = new HashMap<>();
    private final Map<String, List<Surface>> requestedSurfacesForEventId = new HashMap<>();

    private String messagesRequestEventId;
    private String lastProcessedRequestEventId;

    /**
     * Constructor
     *
     * @param parent          {@link MessagingExtension} instance that is the parent of this {@code EdgePersonalizationResponseHandler}
     * @param extensionApi    {@link ExtensionApi} instance
     * @param rulesEngine     {@link LaunchRulesEngine} instance to use for loading in-app message rule payloads
     * @param feedRulesEngine {@link FeedRulesEngine} instance to use for loading message feed rule payloads
     */
    EdgePersonalizationResponseHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final FeedRulesEngine feedRulesEngine) {
        this(parent, extensionApi, rulesEngine, feedRulesEngine, null, null);
    }

    @VisibleForTesting
    EdgePersonalizationResponseHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final FeedRulesEngine feedRulesEngine, final MessagingCacheUtilities messagingCacheUtilities, final String messagesRequestEventId) {
        this.parent = parent;
        this.extensionApi = extensionApi;
        this.launchRulesEngine = rulesEngine;
        this.feedRulesEngine = feedRulesEngine;
        this.messagesRequestEventId = messagesRequestEventId;

        // load cached propositions (if any) when EdgePersonalizationResponseHandler is instantiated
        this.messagingCacheUtilities = messagingCacheUtilities != null ? messagingCacheUtilities : new MessagingCacheUtilities();
        if (this.messagingCacheUtilities.arePropositionsCached()) {
            final Map<Surface, List<Proposition>> cachedPropositions = this.messagingCacheUtilities.getCachedPropositions();
            if (cachedPropositions != null && !cachedPropositions.isEmpty()) {
                Log.trace(LOG_TAG, SELF_TAG, "Retrieved cached propositions, attempting to load the propositions into the rules engine.");
                propositions = cachedPropositions;
                final List<Proposition> propositions = new ArrayList<>();
                final List<Surface> surfaces = new ArrayList<>();
                // get surfaces
                for (final Map.Entry<Surface, List<Proposition>> cacheEntry : cachedPropositions.entrySet()) {
                    surfaces.add(cacheEntry.getKey());
                }
                // get propositions
                for (final List<Proposition> propositionList : cachedPropositions.values()) {
                    propositions.addAll(propositionList);
                }

                final Map<InboundType, List<LaunchRule>> parsedRules = parsePropositions(propositions, surfaces, false, false);
                final List<LaunchRule> inAppRules = parsedRules.get(InboundType.INAPP);
                // register any in-app propositions which were previously cached
                launchRulesEngine.replaceRules(inAppRules);
            }
        }
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to fetch in-app, feed messages, or code-based experiences.
     * The surface URI's used in the request are generated using the application id of the app.
     * If the application id is unavailable, calling this method will do nothing.
     *
     * @param surfaces A {@code List<Surface>} of surfaces for fetching propositions, if available.
     */
    void fetchMessages(final List<Surface> surfaces) {
        final List<String> requestedSurfaceUris = new ArrayList<>();
        if (surfaces != null && !surfaces.isEmpty()) {
            for (final Surface surface : surfaces) {
                if (surface.isValid()) {
                    requestedSurfaceUris.add(surface.getUri());
                }
            }

            if (requestedSurfaceUris.isEmpty()) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to update messages, no valid surfaces found.");
                return;
            }
        } else {
            final Surface appSurface = new Surface();
            if (appSurface.getUri().equals("unknown")) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to update messages, couldn't create a valid app surface.");
                return;
            }
            requestedSurfaceUris.add(appSurface.getUri());
        }

        // create event to be handled by the Edge extension
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();
        personalizationData.put(SURFACES, requestedSurfaceUris);
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
        final List<Surface> requestedSurfaces = new ArrayList<>();
        for (final String surfaceUri : requestedSurfaceUris) {
            requestedSurfaces.add(Surface.fromUriString(surfaceUri));
        }
        messagesRequestEventId = event.getUniqueIdentifier();
        requestedSurfacesForEventId.put(messagesRequestEventId, requestedSurfaces);

        // send event
        Log.debug(LOG_TAG, SELF_TAG, "Dispatching an edge event to retrieve in-app or feed message definitions.");
        extensionApi.dispatch(event);
    }

    /**
     * Validates that the edge response event is a response that we are waiting for. If the returned payload is empty then the Messaging cache
     * and any loaded rules in the Messaging extension's {@link LaunchRulesEngine} are cleared.
     * Non-empty payloads are converted into rules within {@link #parsePropositions(List, List, boolean, boolean)}.
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        final String requestEventId = MessagingUtils.getRequestEventId(edgeResponseEvent);
        final Surface appSurface = new Surface();

        if (!messagesRequestEventId.equals(requestEventId)) {
            return;
        }

        // if this is an event for a new request, purge cache and update lastProcessedRequestEventId
        boolean clearExistingRules = false;
        if (!requestEventId.equals(lastProcessedRequestEventId)) {
            clearExistingRules = true;
            lastProcessedRequestEventId = requestEventId;
        }

        final List<Map<String, Object>> payload = DataReader.optTypedListOfMap(Object.class, edgeResponseEvent.getEventData(), PAYLOAD, null);

        // convert the payload into a list of Proposition(s)
        List<Proposition> propositions = null;
        try {
            propositions = MessagingUtils.getPropositionsFromPayloads(payload);
        } catch (final Exception exception) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create propositions from the AJO personalization payload, an exception occurred: %s.", exception.getLocalizedMessage());
        }

        final Map<InboundType, List<LaunchRule>> parsedRules = parsePropositions(propositions, requestedSurfacesForEventId.get(messagesRequestEventId), clearExistingRules, true);
        if (!parsedRules.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG, "Loading %d rule(s) into the rules engine for surface %s.", parsedRules.size(), requestedSurfacesForEventId.get(messagesRequestEventId));
        }

        final List<LaunchRule> inAppRules = parsedRules.get(InboundType.INAPP);
        final List<LaunchRule> feedRules = parsedRules.get(InboundType.FEED);
        if (inAppRules != null && !inAppRules.isEmpty()) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "The personalization:decisions response contains InApp message definitions.");
            launchRulesEngine.replaceRules(inAppRules);
        } else if (feedRules != null && !feedRules.isEmpty()) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "The personalization:decisions response contains feed message definitions.");
            feedRulesEngine.replaceRules(feedRules);
            inboundMessages = feedRulesEngine.evaluate(edgeResponseEvent);
            if (!MapUtils.isNullOrEmpty(inboundMessages)) {
                updateInboundMessages(inboundMessages, Objects.requireNonNull(requestedSurfacesForEventId.get(lastProcessedRequestEventId)));
            }

            // dispatch event containing serialized Map<Surface, List<Proposition>>
            final Map<String, Object> eventData = new HashMap<>();
            final List<Map<String, Object>> propositionMaps = new ArrayList<>();
            for (final Proposition proposition : propositions) {
                propositionMaps.add(proposition.toEventData());
            }
            eventData.put(appSurface.getUri(), propositionMaps);

            final Event event = new Event.Builder(MESSAGE_PROPOSITIONS_NOTIFICATION,
                    EventType.MESSAGING, MessagingConstants.EventSource.NOTIFICATION)
                    .setEventData(eventData)
                    .build();

            extensionApi.dispatch(event);
        }
    }

    private void updateInboundMessages(final Map<Surface, List<Inbound>> newInboundMessages, final List<Surface> requestedSurfaces) {
        for (final Surface surface : requestedSurfaces) {
            final List<Inbound> inboundMessageList = newInboundMessages.get(surface);
            if (inboundMessageList != null && !inboundMessageList.isEmpty()) {
                inboundMessages.put(surface, inboundMessageList);
            } else {
                if (inboundMessages.get(surface) != null) {
                    inboundMessages.remove(surface);
                }
            }
        }
    }

    /**
     * Attempts to parse any in-app message or message feed rules contained in the provided {@code List<Proposition>}. Any valid rule {@code JSONObject}s
     * found will be returned in a {code List<LaunchRule>}.
     *
     * @param propositions     A {@link List<Proposition>} propositions to be processed
     * @param expectedSurfaces A {@link List<Surface>} containing the expected app surfaces
     * @param clearExisting    {@code boolean} if true the existing cached propositions are cleared and new message rules are replaced in the {@code LaunchRulesEngine}
     * @param persistChanges   {@code boolean} if true the passed in {@code List<Proposition>} are added to the cache
     * @return {@link Map<InboundType, List<LaunchRule>>} containing a mapping between {@link InboundType} and {@link LaunchRule}s parsed from the passed in propositions
     */
    private Map<InboundType, List<LaunchRule>> parsePropositions(final List<Proposition> propositions, final List<Surface> expectedSurfaces, final boolean clearExisting, final boolean persistChanges) {
        final Map<InboundType, List<LaunchRule>> rules = new HashMap<>();
        final Map<String, PropositionInfo> tempPropositionInfo = new HashMap<>();
        final Map<Surface, List<Proposition>> tempPropositions = new HashMap<>();
        final Map<Surface, List<Proposition>> inAppPropositions = new HashMap<>();

        if (clearExisting) {
            clearSurfaces(expectedSurfaces);
        }

        if (propositions == null || propositions.isEmpty()) {
            return rules;
        }

        for (final Proposition proposition : propositions) {
            final Surface surface = expectedSurfaces.get(0);
            final String scope = proposition.getScope();
            if (StringUtils.isNullOrEmpty(surface.getUri()) || !surface.getUri().equals(scope)) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "processPropositions - Ignoring proposition where scope (%s) does not match one of the expected surfaces (%s).", scope, expectedSurfaces.toString());
                continue;
            }

            for (final PropositionItem propositionItem : proposition.getItems()) {
                JSONObject ruleJson;
                try {
                    ruleJson = new JSONObject(propositionItem.getContent());
                } catch (final JSONException jsonException) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "processPropositions - Skipping proposition with invalid json content.");
                    continue;
                }

                final List<LaunchRule> parsedRule = JSONRulesParser.parse(propositionItem.getContent(), extensionApi);
                if (parsedRule == null) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with malformed rule json content.");
                    continue;
                }

                final RuleConsequence consequence = parsedRule.get(0).getConsequenceList().get(0);
                // store reporting data for this payload for later use
                final String messageId = consequence.getId();
                if (!StringUtils.isNullOrEmpty(messageId)) {
                    final PropositionInfo propositionInfo = PropositionInfo.createFromProposition(proposition);
                    if (propositionInfo == null) {
                        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with missing / invalid proposition info.");
                        continue;
                    }
                    tempPropositionInfo.put(messageId, PropositionInfo.createFromProposition(proposition));
                }

                final boolean isInAppConsequence = MessagingUtils.isInApp(consequence);
                List<Proposition> propositionsForSurface;
                if (isInAppConsequence) {
                    propositionsForSurface = inAppPropositions.get(surface) == null ? new ArrayList<>() : inAppPropositions.get(surface);
                    if (!propositionsForSurface.isEmpty()) {
                        inAppPropositions.get(surface).add(proposition);
                    } else {
                        propositionsForSurface.add(proposition);
                        inAppPropositions.put(surface, propositionsForSurface);
                    }
                    // cache any image assets present in the current rule json's image assets array
                    cacheImageAssetsFromPayload(ruleJson);
                } else {
                    if (!MessagingUtils.isFeedItem(consequence)) {
                        propositionsForSurface = tempPropositions.get(surface) == null ? new ArrayList<>() : tempPropositions.get(surface);
                        if (!propositionsForSurface.isEmpty()) {
                            tempPropositions.get(surface).add(proposition);
                        } else {
                            propositionsForSurface.add(proposition);
                            tempPropositions.put(surface, propositionsForSurface);
                        }
                    }
                }

                final String inboundTypeString = DataReader.optString(consequence.getDetail(), MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
                final InboundType inboundType = isInAppConsequence ? InboundType.INAPP : InboundType.fromString(inboundTypeString);
                final List<LaunchRule> parsedRules = rules.get(inboundType) != null ? rules.get(inboundType) : new ArrayList<>();
                parsedRules.addAll(parsedRule);
                rules.put(inboundType, parsedRules);
            }
        }

        this.propositions.putAll(tempPropositions);
        this.propositionInfo.putAll(tempPropositionInfo);

        if (persistChanges && !inAppPropositions.isEmpty()) {
            messagingCacheUtilities.cachePropositions(inAppPropositions);
        }

        return rules;
    }

    private void clearSurfaces(final List<Surface> surfaces) {
        for (final Surface surface : surfaces) {
            propositions.remove(surface);
            inboundMessages.remove(surface);
            for (final Map.Entry<String, PropositionInfo> entry : propositionInfo.entrySet()) {
                if (entry.getValue().scope.equals(surface.getUri())) {
                    propositionInfo.remove(entry.getKey());
                }
            }
        }

        // remove in-app message from cache
        removeCachedPropositions(surfaces);
    }

    private void removeCachedPropositions(final List<Surface> surfaces) {
        if (messagingCacheUtilities.arePropositionsCached()) {
            final Map<Surface, List<Proposition>> cachedPropositions = messagingCacheUtilities.getCachedPropositions();
            for (final Surface surface : surfaces) {
                cachedPropositions.remove(surface);
            }
            messagingCacheUtilities.cachePropositions(cachedPropositions);
        }
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

            final Map<String, Object> mobileParameters = DataReader.optTypedMap(Object.class, details, MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, Collections.emptyMap());
            final InternalMessage message = new InternalMessage(parent, triggeredConsequence, mobileParameters, messagingCacheUtilities.getAssetsMap());
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
            final JSONObject details = MessagingUtils.getConsequenceDetails(ruleJsonObject);
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

    @VisibleForTesting
    void setMessagesRequestEventId(final String messagesRequestEventId) {
        this.messagesRequestEventId = messagesRequestEventId;
        requestedSurfacesForEventId.put(messagesRequestEventId, Collections.singletonList(new Surface()));
    }
}