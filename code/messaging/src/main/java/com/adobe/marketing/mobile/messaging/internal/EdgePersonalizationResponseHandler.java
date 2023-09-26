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

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.AJO;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.INAPP_RESPONSE_FORMAT;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.NAMESPACE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.Data.Key.DATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.Data.Value.NEW_IAM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PAYLOAD;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITIONS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.QUERY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SURFACES;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_NOTIFICATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        final List<Surface> requestedSurfaces = new ArrayList<>();
        Surface appSurface = null;
        if (surfaces != null && !surfaces.isEmpty()) {
            for (final Surface surface : surfaces) {
                if (surface.isValid()) {
                    requestedSurfaces.add(surface);
                }
            }

            if (requestedSurfaces.isEmpty()) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to update messages, no valid surfaces found.");
                return;
            }
        } else {
            appSurface = new Surface();
            if (appSurface.getUri().equals("unknown")) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to update messages, couldn't create a valid app surface.");
                return;
            }
            requestedSurfaces.add(appSurface);
        }

        // create list of strings from the validated requested surface list
        final List<String> validatedSurfaceUris = new ArrayList<>();
        for (final Surface surface : requestedSurfaces) {
            validatedSurfaceUris.add(surface.getUri());
        }

        // create event to be handled by the Edge extension
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();
        personalizationData.put(SURFACES, validatedSurfaceUris);
        messageRequestData.put(PERSONALIZATION, personalizationData);
        eventData.put(QUERY, messageRequestData);

        // add xdm request type
        final Map<String, Object> xdmData = new HashMap<String, Object>() {
            {
                put(EVENT_TYPE, PERSONALIZATION_REQUEST);
            }
        };
        eventData.put(XDM, xdmData);

        // request new in-app format
        final Map<String, Object> data = new HashMap<>();
        final Map<String, Object> ajo = new HashMap<>();
        final Map<String, Object> inAppResponseFormat = new HashMap<>();
        inAppResponseFormat.put(INAPP_RESPONSE_FORMAT, NEW_IAM);
        ajo.put(AJO, inAppResponseFormat);
        data.put(NAMESPACE, ajo);
        eventData.put(DATA, data);

        final Event event = new Event.Builder(MessagingConstants.EventName.REFRESH_MESSAGES_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                null)
                .setEventData(eventData)
                .build();

        // used for ensuring that the messaging extension is responding to the correct handle
        messagesRequestEventId = event.getUniqueIdentifier();
        requestedSurfacesForEventId.put(messagesRequestEventId, requestedSurfaces);

        // send event
        Log.debug(LOG_TAG, SELF_TAG, "Dispatching an edge event to retrieve in-app or feed message definitions.");
        extensionApi.dispatch(event);
    }

    /**
     * Retrieves the previously fetched (and cached) feeds content from the SDK for the provided surfaces.
     *
     * @param surfaces A {@code List<Surface>} of surfaces to use for retrieving cached content
     * @param event    The retrieve message {@link Event}
     */
    void retrieveMessages(final List<Surface> surfaces, final Event event) {
        final List<Surface> requestedSurfaces = new ArrayList<>();
        if (surfaces != null && !surfaces.isEmpty()) {
            for (final Surface surface : surfaces) {
                if (surface.isValid()) {
                    requestedSurfaces.add(surface);
                }
            }

            if (requestedSurfaces.isEmpty()) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to retrieve messages, no valid surfaces found.");
                return;
            }
        } else {
            final Surface appSurface = new Surface();
            if (appSurface.getUri().equals("unknown")) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to retrieve messages, couldn't create a valid app surface.");
                return;
            }
            requestedSurfaces.add(appSurface);
        }

        inboundMessages = feedRulesEngine.evaluate(event);
        if (!MapUtils.isNullOrEmpty(inboundMessages)) {
            updateInboundMessages(inboundMessages, requestedSurfaces);
        }

        final Map<Surface, List<Proposition>> requestedPropositions = retrievePropositions(requestedSurfaces);

        // dispatch an event with the cached feed propositions
        final Map<String, Object> eventData = new HashMap<>();
        final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
        for (final Map.Entry<Surface, List<Proposition>> propositionEntry : requestedPropositions.entrySet()) {
            for (final Proposition proposition : propositionEntry.getValue()) {
                convertedPropositions.add(proposition.toEventData());
            }
        }
        eventData.put(PROPOSITIONS, convertedPropositions);

        final Event responseEvent = new Event.Builder(MESSAGE_PROPOSITIONS_RESPONSE,
                EventType.MESSAGING, EventSource.RESPONSE_CONTENT)
                .setEventData(eventData)
                .inResponseToEvent(event)
                .build();

        extensionApi.dispatch(responseEvent);
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

        if (!messagesRequestEventId.equals(requestEventId) && !requestEventId.equals("TESTING_ID")) {
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

        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(messagesRequestEventId);
        final Map<InboundType, List<LaunchRule>> parsedRules = parsePropositions(propositions, requestedSurfaces, clearExistingRules, true);
        final List<LaunchRule> inAppRules = parsedRules.get(InboundType.INAPP);
        final List<LaunchRule> feedRules = parsedRules.get(InboundType.FEED);
        // TODO: remove this workaround once ajo payload is using new schema https://ns.adobe.com/personalization/message/feed-item
        // final List<LaunchRule> feedRules = parsedRules.get(InboundType.UNKNOWN);

        // handle in-app message rules
        if (!MessagingUtils.isNullOrEmpty(inAppRules)) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "The personalization:decisions response contains InApp message definitions.");
        }
        if (clearExistingRules) {
            Log.debug(LOG_TAG, SELF_TAG, "Loading %d rule(s) into the rules engine.", inAppRules != null ? inAppRules.size() : 0);
            launchRulesEngine.replaceRules(inAppRules);
        } else {
            Log.debug(LOG_TAG, SELF_TAG, "Added %d rule(s) into the rules engine.", inAppRules != null ? inAppRules.size() : 0);
            launchRulesEngine.addRules(inAppRules);
        }

        // handle feed rules
        if (!MessagingUtils.isNullOrEmpty(feedRules)) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "The personalization:decisions response contains feed message definitions.");
        }
        if (clearExistingRules) {
            Log.debug(LOG_TAG, SELF_TAG, "Loading %d rule(s) into the feed rules engine.", feedRules != null ? feedRules.size() : 0);
            feedRulesEngine.replaceRules(feedRules);
        } else {
            Log.debug(LOG_TAG, SELF_TAG, "Added %d rule(s) into the feed rules engine.", feedRules != null ? feedRules.size() : 0);
            feedRulesEngine.addRules(feedRules);
        }

        inboundMessages = feedRulesEngine.evaluate(edgeResponseEvent);
        if (!MapUtils.isNullOrEmpty(inboundMessages)) {
            updateInboundMessages(inboundMessages, requestedSurfaces);
        }

        final Map<Surface, List<Proposition>> requestedPropositions = retrievePropositions(requestedSurfaces);
        if (MapUtils.isNullOrEmpty(requestedPropositions)) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Not dispatching a notification event, personalization:decisions response does not contain propositions.");
            return;
        }

        // dispatch an event with the propositions received from the remote
        final Map<String, Object> eventData = new HashMap<>();
        final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
        for (final Map.Entry<Surface, List<Proposition>> propositionEntry : requestedPropositions.entrySet()) {
            for (final Proposition proposition : propositionEntry.getValue()) {
                convertedPropositions.add(proposition.toEventData());
            }
        }
        eventData.put(PROPOSITIONS, convertedPropositions);

        final Event event = new Event.Builder(MESSAGE_PROPOSITIONS_NOTIFICATION,
                EventType.MESSAGING, MessagingConstants.EventSource.NOTIFICATION)
                .setEventData(eventData)
                .build();

        extensionApi.dispatch(event);
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
     * Retrieves the previously fetched (and cached) feeds content for the provided surfaces.
     *
     * @param surfaces A {@link List<Surface>} of surfaces to retrieve feeds for
     * @return {@link Map<Surface, List<Proposition>>} containing previously fetched feeds content
     */
    private Map<Surface, List<Proposition>> retrievePropositions(final List<Surface> surfaces) {
        Map<Surface, List<Proposition>> propositionMap = new HashMap<>();
        for (final Surface surface : surfaces) {
            // add code-based propositions
            final List<Proposition> propositionsList = propositions.get(surface);
            if (!MessagingUtils.isNullOrEmpty(propositionsList)) {
                propositionMap.put(surface, propositionsList);
            }

            List<Inbound> inboundList = new ArrayList<>();
            if (!MapUtils.isNullOrEmpty(inboundMessages)) {
                inboundList = inboundMessages.get(surface);
            }

            if (MessagingUtils.isNullOrEmpty(inboundList)) {
                continue;
            }

            final List<Proposition> inboundPropositionList = new ArrayList<>();
            for (final Inbound message : inboundList) {
                final PropositionInfo propositionInfo = this.propositionInfo.get(message.getUniqueId());
                if (propositionInfo == null) {
                    continue;
                }

                final String content = message.getContent();
                final PropositionItem propositionItem = new PropositionItem(UUID.randomUUID().toString(), "https://ns.adobe.com/personalization/json-content-item", content);
                final List<PropositionItem> propositionItemList = new ArrayList<>();
                propositionItemList.add(propositionItem);
                final Proposition proposition = new Proposition(propositionInfo.id, propositionInfo.scope, propositionInfo.scopeDetails, propositionItemList);
                inboundPropositionList.add(proposition);
            }
            propositionMap = updatePropositionMapForSurface(surface, inboundPropositionList, propositionMap);
        }
        return propositionMap;
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
        Map<Surface, List<Proposition>> tempPropositions = new HashMap<>();
        Map<Surface, List<Proposition>> inAppPropositions = new HashMap<>();

        if (clearExisting) {
            clearSurfaces(expectedSurfaces);
        }

        if (MessagingUtils.isNullOrEmpty(propositions)) {
            return rules;
        }

        String inboundTypeString;
        InboundType inboundType = null;
        List<LaunchRule> parsedRules;
        for (final Proposition proposition : propositions) {
            final String scope = proposition.getScope();
            boolean found = false;
            for (final Surface surface : expectedSurfaces) {
                if (surface.getUri().equals(scope)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "processPropositions - Ignoring proposition where scope (%s) does not match one of the expected surfaces (%s).", scope, expectedSurfaces.toString());
                continue;
            }

            final Surface surface = Surface.fromUriString(scope);
            for (final PropositionItem propositionItem : proposition.getItems()) {
                parsedRules = JSONRulesParser.parse(propositionItem.getContent(), extensionApi);
                // iam and feed items will be wrapped in a valid rules engine rule - code-based experiences are not
                if (MessagingUtils.isNullOrEmpty(parsedRules)) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Proposition did not contain a rule, adding as a code-based experience.");
                    tempPropositions = updatePropositionMapForSurface(surface, proposition, tempPropositions);
                    continue;
                }

                final List<RuleConsequence> consequences = parsedRules.get(0).getConsequenceList();
                if (MessagingUtils.isNullOrEmpty(consequences)) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with null or empty consequences.");
                    continue;
                }
                for (final RuleConsequence consequence : consequences) {
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
                    if (isInAppConsequence) {
                        inAppPropositions = updatePropositionMapForSurface(surface, proposition, inAppPropositions);
                        // cache any in-app image assets present in the current rule json's image assets array
                        cacheImageAssetsFromPayload(consequence);
                    } else {
                        if (!MessagingUtils.isFeedItem(consequence)) {
                            tempPropositions = updatePropositionMapForSurface(surface, proposition, tempPropositions);
                        }
                    }

                    inboundTypeString = DataReader.optString(consequence.getDetail(), MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");
                    inboundType = isInAppConsequence ? InboundType.INAPP : InboundType.fromString(inboundTypeString);
                }

                List<LaunchRule> tempRules = rules.get(inboundType);
                if (!MessagingUtils.isNullOrEmpty(tempRules)) {
                    tempRules.addAll(new ArrayList<>(parsedRules));
                } else {
                    tempRules = new ArrayList<>(parsedRules);
                }

                rules.put(inboundType, tempRules);
            }
        }

        this.propositions.putAll(tempPropositions);
        this.propositionInfo.putAll(tempPropositionInfo);

        if (persistChanges && !inAppPropositions.isEmpty()) {
            messagingCacheUtilities.cachePropositions(inAppPropositions);
        }

        return rules;
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} propositions map with the provided {@code Surface} and {@code Proposition} objects.
     *
     * @param surface         A {@link Surface} key used to update a {@link List<Proposition>} value in the provided {@link Map<Surface, List<Proposition>>}
     * @param proposition     A {@link Proposition} value to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param propositionsMap The {@link Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code Proposition} objects
     * @return the updated {@link Map<Surface, List<Proposition>>} proposition maps
     */
    private Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final Proposition proposition, final Map<Surface, List<Proposition>> propositionsMap) {
        final Map<Surface, List<Proposition>> tempPropositionsMap = new HashMap<>(propositionsMap);
        final List<Proposition> propositionList = propositions.get(surface) != null ? propositions.get(surface) : MessagingUtils.createMutablePropositionList(proposition);
        if (tempPropositionsMap.get(surface) == null) {
            tempPropositionsMap.put(surface, propositionList);
        } else {
            tempPropositionsMap.get(surface).add(proposition);
        }
        return tempPropositionsMap;
    }

    /**
     * Updates the provided {@code Map<Surface, List<Proposition>>} propositions map with the provided {@code Surface} and {@code List<Proposition>} objects.
     *
     * @param surface         A {@link Surface} key used to update a {@link List<Proposition>} value in the provided {@link Map<Surface, List<Proposition>>}
     * @param newPropositions A {@link List<Proposition>} to add in the provided {@code Map<Surface, List<Proposition>>}
     * @param propositionsMap The {@link Map<Surface, List<Proposition>>} to be updated with the provided {@code Surface} and {@code Proposition} objects
     * @return the updated {@link Map<Surface, List<Proposition>>} proposition maps
     */
    private Map<Surface, List<Proposition>> updatePropositionMapForSurface(final Surface surface, final List<Proposition> newPropositions, final Map<Surface, List<Proposition>> propositionsMap) {
        if (MessagingUtils.isNullOrEmpty(newPropositions)) {
            return propositionsMap;
        }

        final Map<Surface, List<Proposition>> tempPropositionsMap = new HashMap<>(propositionsMap);
        final List<Proposition> propositionList = propositions.get(surface) != null ? propositions.get(surface) : MessagingUtils.createMutablePropositionList(newPropositions);
        if (tempPropositionsMap.get(surface) == null) {
            tempPropositionsMap.put(surface, propositionList);
        } else {
            tempPropositionsMap.get(surface).addAll(newPropositions);
        }
        return tempPropositionsMap;
    }

    /**
     * Clear in-memory data for each {@code Surface} in the provided {@code List<Surface>}
     *
     * @param surfaces A {@link List<Surface>} containing {@link Surface}s which need to have in-memory data cleared
     */
    private void clearSurfaces(final List<Surface> surfaces) {
        for (final Surface surface : surfaces) {
            if (!MapUtils.isNullOrEmpty(propositions)) {
                propositions.remove(surface);
            }
            if (!MapUtils.isNullOrEmpty(inboundMessages)) {
                inboundMessages.remove(surface);
            }
            final Map<String, PropositionInfo> tempPropositionInfo = new HashMap<>(propositionInfo);
            for (final Map.Entry<String, PropositionInfo> entry : tempPropositionInfo.entrySet()) {
                if (entry.getValue().scope.equals(surface.getUri())) {
                    propositionInfo.remove(entry.getKey());
                }
            }
        }

        // remove in-app message from cache
        removeCachedPropositions(surfaces);
    }

    /**
     * Clear cached data for each {@code Surface} in the provided {@code List<Surface>}
     *
     * @param surfaces A {@link List<Surface>} containing {@link Surface}s which need to have cached data cleared
     */
    private void removeCachedPropositions(final List<Surface> surfaces) {
        if (messagingCacheUtilities.arePropositionsCached()) {
            final Map<Surface, List<Proposition>> cachedPropositions = messagingCacheUtilities.getCachedPropositions();
            if (MapUtils.isNullOrEmpty(cachedPropositions)) {
                return;
            }

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
     * Cache any asset URL's present in the {@link com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence} detail.
     *
     * @param ruleConsequence A {@link RuleConsequence} containing an in-app message rule consequence.
     */
    private void cacheImageAssetsFromPayload(final RuleConsequence ruleConsequence) {
        final List<String> remoteAssetsList = new ArrayList<>();
        final Map<String, Object> details = ruleConsequence.getDetail();
        final List<String> remoteAssets = DataReader.optStringList(details, MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS, null);
        if (!MessagingUtils.isNullOrEmpty(remoteAssets)) {
            for (final String remoteAsset : remoteAssets) {
                if (UrlUtils.isValidUrl(remoteAsset)) {
                    Log.debug(LOG_TAG, SELF_TAG, "Image asset to be cached (%s) ", remoteAsset);
                    remoteAssetsList.add(remoteAsset);
                }
            }
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