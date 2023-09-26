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
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.ENDING_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PAYLOAD;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITIONS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.QUERY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SURFACES;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.REQUEST;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.SEND_COMPLETION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.FINALIZE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_NOTIFICATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.RESPONSE_CALLBACK_TIMEOUT;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Inbound;
import com.adobe.marketing.mobile.InboundType;
import com.adobe.marketing.mobile.MobileCore;
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
import java.util.Set;
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
    private Map<String, PropositionInfo> propositionInfo = new HashMap<>();
    private Map<Surface, List<Inbound>> inboundMessages = new HashMap<>();
    // keeps a list of all surfaces requested per personalization request event by event id
    private final Map<String, List<Surface>> requestedSurfacesForEventId = new HashMap<>();
    // used while processing streaming payloads for a single request
    private final Map<Surface, List<Proposition>> inProgressPropositions = new HashMap<>();
    private final Map<Surface, List<LaunchRule>> inAppRulesBySurface = new HashMap<>();
    private final Map<Surface, List<LaunchRule>> feedRulesBySurface = new HashMap<>();

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

                final ParsedPropositions parsedPropositions = new ParsedPropositions(cachedPropositions, surfaces, extensionApi);
                final Map<Surface, List<LaunchRule>> inAppRules = parsedPropositions.surfaceRulesByInboundType.get(InboundType.INAPP);
                // register any in-app propositions which were previously cached
                final List<LaunchRule> rulesToReplace = inAppRules.get(new Surface());
                launchRulesEngine.replaceRules(rulesToReplace);
            }
        }
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to fetch in-app, feed messages, or code-based experiences.
     * The surface URI's used in the request are generated using the application id of the app.
     * If the application id is unavailable, calling this method will do nothing.
     *
     * @param event    The fetch message {@link Event}
     * @param surfaces A {@code List<Surface>} of surfaces for fetching propositions, if available.
     */
    void fetchMessages(final Event event, final List<Surface> surfaces) {
        final List<Surface> requestedSurfaces = new ArrayList<>();
        Surface appSurface = null;
        // if surfaces are provided, use them - otherwise assume the request is for base surface (mobileapp://{application package name})
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

        // begin construction of event data
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();

        // add query parameters containing supported schemas and requested surfaces
        personalizationData.put(SURFACES, validatedSurfaceUris);
        messageRequestData.put(PERSONALIZATION, personalizationData);
        eventData.put(QUERY, messageRequestData);

        // add xdm with an event type of personalization.request
        final Map<String, Object> xdmData = new HashMap<String, Object>() {
            {
                put(EVENT_TYPE, PERSONALIZATION_REQUEST);
            }
        };
        eventData.put(XDM, xdmData);

        // add a data object to the request specifying the format desired in the response from XAS
        final Map<String, Object> data = new HashMap<>();
        final Map<String, Object> ajo = new HashMap<>();
        final Map<String, Object> inAppResponseFormat = new HashMap<>();
        inAppResponseFormat.put(INAPP_RESPONSE_FORMAT, NEW_IAM);
        ajo.put(AJO, inAppResponseFormat);
        data.put(NAMESPACE, ajo);
        eventData.put(DATA, data);

        // add a request object so we get a response event from edge when the propositions stream is closed for this event
        final Map<String, Object> request = new HashMap<>();
        request.put(SEND_COMPLETION, true);
        eventData.put(REQUEST, request);
        // end construction of event data

        final Event newEvent = new Event.Builder(MESSAGE_PROPOSITIONS_NOTIFICATION,
                EventType.MESSAGING, MessagingConstants.EventSource.NOTIFICATION)
                .setEventData(eventData)
                .chainToParentEvent(event)
                .build();

        // create entries in our local containers for managing streamed responses from edge
        beginRequestForSurfaces(newEvent, requestedSurfaces);

        // dispatch the event and handle the response callback
        MobileCore.dispatchEventWithResponseCallback(event, RESPONSE_CALLBACK_TIMEOUT, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                // response event failed or timed out, need to remove this event from the queue
                requestedSurfacesForEventId.remove(newEvent.getUniqueIdentifier());
                Log.warning(LOG_TAG, SELF_TAG, "Unable to run completion logic for a personalization request event - error occurred: %s", adobeError.getErrorName());
            }

            // the callback is called by Edge extension when a request's stream has been closed
            @Override
            public void call(final Event responseEvent) {
                // dispatch an event signaling messaging extension needs to finalize this event
                // it must be dispatched to the event queue to avoid a race with the events containing propositions
                final String endingEventId = responseEvent.getResponseID();
                final Map<String, Object> eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, endingEventId);
                final Event processCompletedEvent = new Event.Builder(FINALIZE_PROPOSITIONS_RESPONSE, EventType.MESSAGING, EventSource.CONTENT_COMPLETE)
                        .setEventData(eventData)
                        .chainToParentEvent(responseEvent)
                        .build();
                extensionApi.dispatch(processCompletedEvent);
            }
        });
    }

    /**
     * Process the event containing the finalized edge response personalization notification data.
     *
     * @param event A {@link Event} containing the personalization notification complete edge response event.
     */
    void handleProcessCompletedEvent(final Event event) {
        final String endingEventId = MessagingUtils.getEndingEventId(event);
        if (StringUtils.isNullOrEmpty(endingEventId) || requestedSurfacesForEventId.get(endingEventId) == null) {
            // shouldn't ever get here, but if we do, we don't have anything to process so we should bail
            return;
        }

        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(endingEventId);
        Log.trace(LOG_TAG, SELF_TAG, "End of streaming response events for requesting event %s", endingEventId);
        endRequestForEventId(endingEventId);

        // check for new inbound messages from recently updated rules engine
        final Map<Surface, List<Inbound>> inboundMessages = feedRulesEngine.evaluate(event);
        if (!MapUtils.isNullOrEmpty(inboundMessages)) {
            updateInboundMessages(inboundMessages, requestedSurfaces);
        }

        // dispatch notification event for request
        dispatchNotificationEventForSurfaces(requestedSurfaces);
    }

    private void dispatchNotificationEventForSurfaces(final List<Surface> requestedSurfaces) {
        final Map<Surface, List<Proposition>> requestedPropositions = retrievePropositions(requestedSurfaces);
        if (MapUtils.isNullOrEmpty(requestedPropositions)) {
            Log.trace(LOG_TAG, SELF_TAG, "Not dispatching a notification event, personalization:decisions response does not contain propositions.");
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
        final Map<String, Object> requestedPropositionsMap = new HashMap<>();
        for (final Map.Entry<Surface, List<Proposition>> propositionEntry : requestedPropositions.entrySet()) {
            final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
            for (final Proposition proposition : propositionEntry.getValue()) {
                convertedPropositions.add(proposition.toEventData());
            }
            requestedPropositionsMap.put(propositionEntry.getKey().getUri(), convertedPropositions);
        }
        eventData.put(PROPOSITIONS, requestedPropositionsMap);

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
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        // validate this is one of our events
        final String requestEventId = MessagingUtils.getRequestEventId(edgeResponseEvent);
        if (!messagesRequestEventId.equals(requestEventId) && !requestEventId.equals("TESTING_ID")) {
            return;
        }

        Log.trace(LOG_TAG, SELF_TAG, "Processing propositions from personalization:decisions network response for event %s.", requestEventId);
        updateInProgressPropositionsWithEvent(edgeResponseEvent);
//        final String requestEventId = MessagingUtils.getRequestEventId(edgeResponseEvent);
//
//        if (!messagesRequestEventId.equals(requestEventId) && !requestEventId.equals("TESTING_ID")) {
//            return;
//        }
//
//        // if this is an event for a new request, purge cache and update lastProcessedRequestEventId
//        boolean clearExistingRules = false;
//        if (!requestEventId.equals(lastProcessedRequestEventId)) {
//            clearExistingRules = true;
//            lastProcessedRequestEventId = requestEventId;
//        }
//
//        final List<Map<String, Object>> payload = DataReader.optTypedListOfMap(Object.class, edgeResponseEvent.getEventData(), PAYLOAD, null);
//
//        // convert the payload into a list of Proposition(s)
//        List<Proposition> propositions = null;
//        try {
//            propositions = MessagingUtils.getPropositionsFromPayloads(payload);
//        } catch (final Exception exception) {
//            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create propositions from the AJO personalization payload, an exception occurred: %s.", exception.getLocalizedMessage());
//        }
//
//        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(messagesRequestEventId);
//        final Map<InboundType, List<LaunchRule>> parsedRules = parsePropositions(propositions, requestedSurfaces, clearExistingRules, true);
//        final List<LaunchRule> inAppRules = parsedRules.get(InboundType.INAPP);
//        final List<LaunchRule> feedRules = parsedRules.get(InboundType.FEED);
//        // TODO: remove this workaround once ajo payload is using new schema https://ns.adobe.com/personalization/message/feed-item
//        // final List<LaunchRule> feedRules = parsedRules.get(InboundType.UNKNOWN);
//
//        // handle in-app message rules
//        if (!MessagingUtils.isNullOrEmpty(inAppRules)) {
//            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "The personalization:decisions response contains InApp message definitions.");
//        }
//        if (clearExistingRules) {
//            Log.debug(LOG_TAG, SELF_TAG, "Loading %d rule(s) into the rules engine.", inAppRules != null ? inAppRules.size() : 0);
//            launchRulesEngine.replaceRules(inAppRules);
//        } else {
//            Log.debug(LOG_TAG, SELF_TAG, "Added %d rule(s) into the rules engine.", inAppRules != null ? inAppRules.size() : 0);
//            launchRulesEngine.addRules(inAppRules);
//        }
//
//        // handle feed rules
//        if (!MessagingUtils.isNullOrEmpty(feedRules)) {
//            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "The personalization:decisions response contains feed message definitions.");
//        }
//        if (clearExistingRules) {
//            Log.debug(LOG_TAG, SELF_TAG, "Loading %d rule(s) into the feed rules engine.", feedRules != null ? feedRules.size() : 0);
//            feedRulesEngine.replaceRules(feedRules);
//        } else {
//            Log.debug(LOG_TAG, SELF_TAG, "Added %d rule(s) into the feed rules engine.", feedRules != null ? feedRules.size() : 0);
//            feedRulesEngine.addRules(feedRules);
//        }
//
//        inboundMessages = feedRulesEngine.evaluate(edgeResponseEvent);
//        if (!MapUtils.isNullOrEmpty(inboundMessages)) {
//            updateInboundMessages(inboundMessages, requestedSurfaces);
//        }
//
//        final Map<Surface, List<Proposition>> requestedPropositions = retrievePropositions(requestedSurfaces);
//        if (MapUtils.isNullOrEmpty(requestedPropositions)) {
//            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Not dispatching a notification event, personalization:decisions response does not contain propositions.");
//            return;
//        }
//
//        // dispatch an event with the propositions received from the remote
//        final Map<String, Object> eventData = new HashMap<>();
//        final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
//        for (final Map.Entry<Surface, List<Proposition>> propositionEntry : requestedPropositions.entrySet()) {
//            for (final Proposition proposition : propositionEntry.getValue()) {
//                convertedPropositions.add(proposition.toEventData());
//            }
//        }
//        eventData.put(PROPOSITIONS, convertedPropositions);
//
//        final Event event = new Event.Builder(MESSAGE_PROPOSITIONS_NOTIFICATION,
//                EventType.MESSAGING, MessagingConstants.EventSource.NOTIFICATION)
//                .setEventData(eventData)
//                .build();
//
//        extensionApi.dispatch(event);
    }

    private void updateInProgressPropositionsWithEvent(final Event event) {
        final String requestEventId = MessagingUtils.getRequestEventId(event);
        if (StringUtils.isNullOrEmpty(requestEventId)) {
            Log.trace(LOG_TAG, SELF_TAG, "Ignoring personalization:decisions response with no requesting Event ID.");
            return;
        }

        // convert the payload into a list of Proposition(s)
        final List<Map<String, Object>> payload = DataReader.optTypedListOfMap(Object.class, event.getEventData(), PAYLOAD, null);
        List<Proposition> propositions = null;
        try {
            propositions = MessagingUtils.getPropositionsFromPayloads(payload);
        } catch (final Exception exception) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create propositions from the AJO personalization payload, an exception occurred: %s.", exception.getLocalizedMessage());
        }
        if (MessagingUtils.isNullOrEmpty(propositions)) {
            Log.trace(LOG_TAG, SELF_TAG, "Ignoring personalization:decisions response with no propositions.");
            return;
        }

        // loop through propositions for this event and add them to existing props by surface
        for (final Proposition proposition : propositions) {
            final Surface surface = Surface.fromUriString(proposition.getScope());
            MessagingUtils.updatePropositionMapForSurface(surface, proposition, inProgressPropositions);
        }
    }

    private void beginRequestForSurfaces(final Event event, final List<Surface> surfaces) {
        requestedSurfacesForEventId.put(event.getUniqueIdentifier(), surfaces);
    }

    private void endRequestForEventId(final String eventId) {
        // update in memory propositions
        applyPropositionChangeForEventId(eventId);

        // remove event from surfaces dictionary
        requestedSurfacesForEventId.remove(eventId);

        // clear pending propositions
        inProgressPropositions.clear();
    }

    private void applyPropositionChangeForEventId(final String eventId) {
        // get the list of requested surfaces for this event
        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(eventId);
        if (MessagingUtils.isNullOrEmpty(requestedSurfaces)) {
            return;
        }

        final ParsedPropositions parsedPropositions = new ParsedPropositions(inProgressPropositions, requestedSurfaces, extensionApi);

        // we need to preserve cache for any surfaces that were not a part of this request
        // any requested surface that is absent from the response needs to be removed from cache and persistence
        final Set<Surface> returnedSurfaces = inProgressPropositions.keySet();
        final List<Surface> surfacesToRemove = new ArrayList<>();
        for (final Surface surface : returnedSurfaces) {
            if (!requestedSurfaces.contains(surface)) {
                surfacesToRemove.add(surface);
            }
        }

        // update persistence, reporting data cache, and finally rules engine for in-app messages
        // order matters here because the rules engine must be a full replace, and when we update
        // persistence we will be removing empty surfaces and making sure unrequested surfaces
        // continue to have their rules active
        updatePropositions(parsedPropositions.propositionsToCache, surfacesToRemove);
        updatePropositionInfo(parsedPropositions.propositionInfoToCache, surfacesToRemove);
        messagingCacheUtilities.cachePropositions(parsedPropositions.propositionsToPersist, surfacesToRemove);

        // apply rules
        updateRulesEngines(parsedPropositions.surfaceRulesByInboundType, requestedSurfaces)
    }

    private void updateRulesEngines(final Map<InboundType, Map<Surface, List<LaunchRule>>> rules, final List<Surface> requestedSurfaces) {
       // TODO
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

    private void updatePropositionInfo(final Map<String, PropositionInfo> newPropositionInfo, final List<Surface> surfaces) {
        propositionInfo.putAll(newPropositionInfo);

        // currently, we can't remove entries that pre-exist by message id since they are not linked to surfaces
        // need to get surface uri from propositionInfo.scope and remove entry based on incoming `surfaces`
        if (MessagingUtils.isNullOrEmpty(surfaces)) {
            return;
        }

        final Map<String, PropositionInfo> tempPropositionInfo = new HashMap<>(propositionInfo);
        for (final Map.Entry<String, PropositionInfo> entry : tempPropositionInfo.entrySet()) {
            if (!surfaces.contains(Surface.fromUriString(entry.getValue().scope))) {
                tempPropositionInfo.remove(entry.getKey());
            }
        }

        propositionInfo = tempPropositionInfo;
    }

    private void updatePropositions(final Map<Surface, List<Proposition>> newPropositions, final List<Surface> surfacesToRemove) {
        // add new surfaces or update replace existing surfaces
        Map<Surface, List<Proposition>> tempPropositions = new HashMap<>(newPropositions);
        for (final Map.Entry<Surface, List<Proposition>> entry : tempPropositions.entrySet()) {
            MessagingUtils.updatePropositionMapForSurface(entry.getKey(), entry.getValue(), tempPropositions);
        }

        // remove any surfaces if necessary
        for (final Surface surface : surfacesToRemove) {
            tempPropositions.remove(surface);
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

            final List<Inbound> inboundList = inboundMessages.get(surface);
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
            propositionMap = MessagingUtils.updatePropositionMapForSurface(surface, inboundPropositionList, propositionMap);
        }
        return propositionMap;
    }

    /**
     * Clear in-memory data for each {@code Surface} in the provided {@code List<Surface>}
     *
     * @param surfaces A {@link List<Surface>} containing {@link Surface}s which need to have in-memory data cleared
     */
    private void clearSurfaces(final List<Surface> surfaces) {
        for (final Surface surface : surfaces) {
            propositions.remove(surface);
            inboundMessages.remove(surface);
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
            for (final Surface surface : surfaces) {
                cachedPropositions.remove(surface);
            }
            messagingCacheUtilities.cachePropositions(cachedPropositions, surfaces);
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