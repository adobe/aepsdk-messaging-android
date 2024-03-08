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

package com.adobe.marketing.mobile.messaging;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.AJO;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.INAPP_RESPONSE_FORMAT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.NAMESPACE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.Key.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Data.Value.NEW_IAM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.ENDING_EVENT_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.PERSONALIZATION_REQUEST;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PAYLOAD;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PERSONALIZATION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.QUERY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCHEMAS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SURFACES;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.REQUEST;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.SEND_COMPLETION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.FINALIZE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_NOTIFICATION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventName.REFRESH_MESSAGES_EVENT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.RESPONSE_CALLBACK_TIMEOUT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.SchemaValues.SCHEMA_IAM;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.UrlUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to handle the retrieval and processing of AJO payloads containing in-app or feed messages. It is also
 * responsible for the display of AJO in-app messages.
 */
class EdgePersonalizationResponseHandler {
    private final static String SELF_TAG = "EdgePersonalizationResponseHandler";

    private final static List<String> SUPPORTED_SCHEMAS = new ArrayList<String>() {{
        add(MessagingConstants.SchemaValues.SCHEMA_HTML_CONTENT);
        add(MessagingConstants.SchemaValues.SCHEMA_JSON_CONTENT);
        add(MessagingConstants.SchemaValues.SCHEMA_RULESET_ITEM);
    }};
    final MessagingExtension parent;
    private final MessagingCacheUtilities messagingCacheUtilities;
    private final ExtensionApi extensionApi;
    private final LaunchRulesEngine launchRulesEngine;
    private final FeedRulesEngine feedRulesEngine;

    private Map<Surface, List<Proposition>> propositions = new HashMap<>();
    private Map<String, PropositionInfo> propositionInfo = new HashMap<>();
    // keeps a list of all surfaces requested per personalization request event by event id
    private final Map<String, List<Surface>> requestedSurfacesForEventId = new HashMap<>();
    // used while processing streaming payloads for a single request
    private Map<Surface, List<Proposition>> inProgressPropositions = new HashMap<>();
    private final Map<Surface, List<LaunchRule>> inAppRulesBySurface = new HashMap<>();
    private final Map<Surface, List<LaunchRule>> feedRulesBySurface = new HashMap<>();
    private SerialWorkDispatcher<Event> serialWorkDispatcher;

    /**
     * Constructor
     *
     * @param parent          {@link MessagingExtension} instance that is the parent of this {@code EdgePersonalizationResponseHandler}
     * @param extensionApi    {@link ExtensionApi} instance
     * @param rulesEngine     {@link LaunchRulesEngine} instance to use for loading in-app message rule payloads
     * @param feedRulesEngine {@link FeedRulesEngine} instance to use for loading message feed rule payloads
     */
    EdgePersonalizationResponseHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final FeedRulesEngine feedRulesEngine) {
        this(parent, extensionApi, rulesEngine, feedRulesEngine, null);
    }

    @VisibleForTesting
    EdgePersonalizationResponseHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final FeedRulesEngine feedRulesEngine, final MessagingCacheUtilities messagingCacheUtilities) {
        this.parent = parent;
        this.extensionApi = extensionApi;
        this.launchRulesEngine = rulesEngine;
        this.feedRulesEngine = feedRulesEngine;

        // load cached propositions (if any) when EdgePersonalizationResponseHandler is instantiated
        this.messagingCacheUtilities = messagingCacheUtilities != null ? messagingCacheUtilities : new MessagingCacheUtilities();
        if (this.messagingCacheUtilities.arePropositionsCached()) {
            final Map<Surface, List<Proposition>> cachedPropositions = this.messagingCacheUtilities.getCachedPropositions();
            if (cachedPropositions != null && !cachedPropositions.isEmpty()) {
                Log.trace(LOG_TAG, SELF_TAG, "Retrieved cached propositions, attempting to load the propositions into the rules engine.");
                propositions = cachedPropositions;
                final List<Surface> surfaces = new ArrayList<>();
                // get surfaces
                for (final Map.Entry<Surface, List<Proposition>> cacheEntry : cachedPropositions.entrySet()) {
                    surfaces.add(cacheEntry.getKey());
                }

                final ParsedPropositions parsedPropositions = new ParsedPropositions(cachedPropositions, surfaces, extensionApi);
                final Map<Surface, List<LaunchRule>> inAppRules = parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.INAPP);
                // register any in-app propositions which were previously cached
                if (inAppRules != null) {
                    final List<LaunchRule> rulesToReplace = new ArrayList<>();
                    for (final Map.Entry<Surface, List<LaunchRule>> entry: inAppRules.entrySet()){
                        rulesToReplace.addAll(entry.getValue());
                    }
                    if (!MessagingUtils.isNullOrEmpty(rulesToReplace)) {
                        launchRulesEngine.replaceRules(rulesToReplace);
                    }
                }
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
        personalizationData.put(SCHEMAS, SUPPORTED_SCHEMAS);
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

        final Event newEvent = new Event.Builder(REFRESH_MESSAGES_EVENT,
                EventType.EDGE, MessagingConstants.EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .chainToParentEvent(event)
                .build();

        // create entries in our local containers for managing streamed responses from edge
        beginRequestForSurfaces(newEvent, requestedSurfaces);

        // dispatch the event and handle the response callback
        MobileCore.dispatchEventWithResponseCallback(newEvent, RESPONSE_CALLBACK_TIMEOUT, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                // response event failed or timed out, need to remove this event from the queue
                requestedSurfacesForEventId.remove(newEvent.getUniqueIdentifier());
                serialWorkDispatcher.resume();
                Log.warning(LOG_TAG, SELF_TAG, "Unable to run completion logic for a personalization request event - error occurred: %s", adobeError.getErrorName());
            }

            // the callback is called by Edge extension when a request's stream has been closed
            @Override
            public void call(final Event responseCompleteEvent) {
                // dispatch an event signaling messaging extension needs to finalize this event
                // it must be dispatched to the event queue to avoid a race with the events containing propositions
                final String endingEventId = InternalMessagingUtils.getRequestEventId(responseCompleteEvent);
                final Map<String, Object> eventData = new HashMap<>();
                eventData.put(ENDING_EVENT_ID, endingEventId);
                final Event processCompletedEvent = new Event.Builder(FINALIZE_PROPOSITIONS_RESPONSE,
                        EventType.MESSAGING,
                        EventSource.CONTENT_COMPLETE)
                        .setEventData(eventData)
                        .chainToParentEvent(responseCompleteEvent)
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
        final String endingEventId = InternalMessagingUtils.getEndingEventId(event);
        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(endingEventId);
        if (StringUtils.isNullOrEmpty(endingEventId) || MessagingUtils.isNullOrEmpty(requestedSurfaces)) {
            // shouldn't ever get here, but if we do, we don't have anything to process so we should bail
            return;
        }

        Log.trace(LOG_TAG, SELF_TAG, "End of streaming response events for requesting event %s", endingEventId);
        endRequestForEventId(endingEventId);

        // dispatch notification event for request
        dispatchNotificationEventForSurfaces(requestedSurfaces);
        // resume processing the internal events queue after processing is completed for an update propositions request
        Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "handleProcessCompletedEvent - Starting serial work dispatcher.");
        serialWorkDispatcher.resume();
    }

    private void dispatchNotificationEventForSurfaces(final List<Surface> requestedSurfaces) {
        final Map<Surface, List<Proposition>> requestedPropositionsMap = retrieveCachedPropositions(requestedSurfaces);
        if (MapUtils.isNullOrEmpty(requestedPropositionsMap)) {
            Log.trace(LOG_TAG, SELF_TAG, "Not dispatching a notification event, personalization:decisions response does not contain propositions.");
            return;
        }

        // dispatch an event with the propositions received from the remote
        final Map<String, Object> eventData = new HashMap<>();
        final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
        for (final Map.Entry<Surface, List<Proposition>> propositionEntry : requestedPropositionsMap.entrySet()) {
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
        if (surfaces == null || surfaces.isEmpty()) {
            return;
        }

        for (final Surface surface : surfaces) {
            if (surface.isValid()) {
                requestedSurfaces.add(surface);
            }
        }

        if (requestedSurfaces.isEmpty()) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to retrieve messages, no valid surfaces found.");
            extensionApi.dispatch(InternalMessagingUtils.createErrorResponseEvent(event, AdobeErrorExt.INVALID_REQUEST));
            return;
        }

        final Map<Surface, List<Proposition>> ruleConsequencePropositions = getPropositionsFromFeedRulesEngine(event);
        Map<Surface, List<Proposition>> requestedPropositions = retrieveCachedPropositions(requestedSurfaces);

        for (final Map.Entry<Surface, List<Proposition>> entry: ruleConsequencePropositions.entrySet()) {
            requestedPropositions = MessagingUtils.updatePropositionMapForSurface(entry.getKey(), entry.getValue(), requestedPropositions);
        }

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
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        // validate this is one of our events
        final String requestEventId = InternalMessagingUtils.getRequestEventId(edgeResponseEvent);

        if (StringUtils.isNullOrEmpty(requestEventId) || (!requestedSurfacesForEventId.containsKey(requestEventId) && !requestEventId.equals("TESTING_ID"))) {
            return;
        }

        Log.trace(LOG_TAG, SELF_TAG, "Processing propositions from personalization:decisions network response for event %s.", requestEventId);
        updateInProgressPropositionsWithEvent(edgeResponseEvent);
    }

    private void updateInProgressPropositionsWithEvent(final Event event) {
        final String requestEventId = InternalMessagingUtils.getRequestEventId(event);
        if (StringUtils.isNullOrEmpty(requestEventId)) {
            Log.trace(LOG_TAG, SELF_TAG, "Ignoring personalization:decisions response with no requesting Event ID.");
            return;
        }

        // convert the payload into a list of Proposition(s)
        final List<Map<String, Object>> payloads = DataReader.optTypedListOfMap(Object.class, event.getEventData(), PAYLOAD, null);
        if(MessagingUtils.isNullOrEmpty(payloads)) {
            Log.trace(LOG_TAG, SELF_TAG, "Ignoring personalization:decisions response with no propositions.");
            return;
        }
        List<Proposition> propositions = null;
        try {
            propositions = InternalMessagingUtils.getPropositionsFromPayloads(payloads);
        } catch (final Exception exception) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Unable to create propositions from the AJO personalization payload, an exception occurred: %s.", exception.getLocalizedMessage());
        }
        if (MessagingUtils.isNullOrEmpty(propositions)) {
            Log.trace(LOG_TAG, SELF_TAG, "Ignoring personalization:decisions response with no propositions.");
            return;
        }

        // loop through propositions for this event and add them to existing proposition map by surface
        for (final Proposition proposition : propositions) {
            final Surface surface = Surface.fromUriString(proposition.getScope());
            inProgressPropositions = MessagingUtils.updatePropositionMapForSurface(surface, proposition, inProgressPropositions);
        }
    }

    private void beginRequestForSurfaces(final Event event, final List<Surface> surfaces) {
        requestedSurfacesForEventId.put(event.getUniqueIdentifier(), surfaces);

        // add the Edge request event to update propositions in the events queue.
        serialWorkDispatcher.offer(event);
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
        updateRulesEngines(parsedPropositions.surfaceRulesBySchemaType, requestedSurfaces);
    }

    private void updateRulesEngines(final Map<SchemaType, Map<Surface, List<LaunchRule>>> surfaceRulesBySchemaType, final List<Surface> requestedSurfaces) {
        for (final Map.Entry<SchemaType, Map<Surface, List<LaunchRule>>> newRules : surfaceRulesBySchemaType.entrySet()) {
            final Set<Surface> newSurfaces = newRules.getValue().keySet();
            final List<Surface> surfacesToRemove = new ArrayList<>(requestedSurfaces);
            surfacesToRemove.removeAll(newSurfaces);

            final SchemaType schemaType = newRules.getKey();
            final Map<Surface, List<LaunchRule>> rulesMaps = newRules.getValue();
            switch (schemaType) {
                case INAPP:
                    Log.trace(LOG_TAG, SELF_TAG, "Updating in-app message definitions for surfaces %s.", newSurfaces);

                    // replace rules for each in-app surface we got back
                    inAppRulesBySurface.putAll(rulesMaps);

                    // remove any surfaces that were requested but had no in-app content returned
                    for (final Surface surface : surfacesToRemove) {
                        inAppRulesBySurface.remove(surface);
                    }

                    // combine all our rules
                    final Collection<List<LaunchRule>> allInAppRules = inAppRulesBySurface.values();
                    final List<LaunchRule> collectedInAppRules = new ArrayList<>();
                    for (final List<LaunchRule> inAppRules : allInAppRules) {
                        collectedInAppRules.addAll(inAppRules);
                    }

                    // pre-fetch the assets for this message if there are any defined
                    final List<RuleConsequence> collectedConsequences = new ArrayList<>();
                    for (final LaunchRule rule : collectedInAppRules) {
                        collectedConsequences.addAll(rule.getConsequenceList());
                    }
                    cacheImageAssetsFromPayload(collectedConsequences);

                    // update rules in in-app engine
                    launchRulesEngine.replaceRules(collectedInAppRules);
                    break;
                case FEED:
                    Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Updating feed definitions for surfaces %s", newSurfaces);

                    // replace rules for each feed surface we got back
                    feedRulesBySurface.putAll(rulesMaps);

                    // remove any surfaces that were requested but had no feed content returned
                    for (final Surface surface : surfacesToRemove) {
                        feedRulesBySurface.remove(surface);
                    }

                    // combine all our rules
                    final Collection<List<LaunchRule>> allFeedRules = feedRulesBySurface.values();
                    final List<LaunchRule> collectedFeedRules = new ArrayList<>();
                    for (final List<LaunchRule> feedRules : allFeedRules) {
                        collectedFeedRules.addAll(feedRules);
                    }

                    // update rules in feed rules engine
                    feedRulesEngine.replaceRules(collectedFeedRules);
                    break;
                default:
                    // no-op
                    Log.trace(LOG_TAG, SELF_TAG, "No action will be taken updating messaging rules - the InboundType provided is not supported.");
                    break;
            }
        }
    }

    private void updatePropositionInfo(final Map<String, PropositionInfo> newPropositionInfo, final List<Surface> surfacesToRemove) {
        propositionInfo.putAll(newPropositionInfo);

        // currently, we can't remove entries that pre-exist by message id since they are not linked to surfaces
        // need to get surface uri from propositionInfo.scope and remove entry based on incoming surfaces
        if (MessagingUtils.isNullOrEmpty(surfacesToRemove)) {
            return;
        }

        final Map<String, PropositionInfo> tempPropositionInfoMap = new HashMap<>(propositionInfo);
        for (final Map.Entry<String, PropositionInfo> entry : tempPropositionInfoMap.entrySet()) {
            if (surfacesToRemove.contains(Surface.fromUriString(entry.getValue().scope))) {
                tempPropositionInfoMap.remove(entry.getKey());
            }
        }

        propositionInfo = tempPropositionInfoMap;
    }

    private Map<Surface, List<Proposition>> getPropositionsFromFeedRulesEngine(final Event event) {
        Map<Surface, List<Proposition>> surfacePropositions = new HashMap<>();
        final Map<Surface, List<PropositionItem>> propositionItemsBySurface = feedRulesEngine.evaluate(event);
        if (!MapUtils.isNullOrEmpty(propositionItemsBySurface)) {
            for (final Map.Entry<Surface, List<PropositionItem>> entry: propositionItemsBySurface.entrySet()){
                final List<Proposition> tempPropositions = new ArrayList<>();
                for (final PropositionItem propositionItem: entry.getValue()) {
                    final PropositionInfo propositionInfo = this.propositionInfo.get(propositionItem.getPropositionItemId());
                    if (propositionInfo == null) {
                        continue;
                    }

                    final Proposition proposition = new Proposition(
                            propositionInfo.id,
                            propositionInfo.scope,
                            propositionInfo.scopeDetails,
                            new ArrayList<PropositionItem>() {{ add(propositionItem); }});

                    // check to see if that proposition is already in the array (based on ID)
                    // if yes, append the propositionItem.  if not, create a new entry for the
                    // proposition with the new item.

                    Proposition existingProposition = null;
                    for (final Proposition messagingProposition: tempPropositions) {
                        if (messagingProposition.getUniqueId().equals(proposition.getUniqueId())) {
                            existingProposition = messagingProposition;
                            break;
                        }
                    }
                    if (existingProposition != null) {
                        propositionItem.propositionReference = new SoftReference<>(existingProposition);
                        existingProposition.getItems().add(propositionItem);
                    } else {
                        propositionItem.propositionReference = new SoftReference<>(proposition);
                        tempPropositions.add(proposition);
                    }
                }

                surfacePropositions = MessagingUtils.updatePropositionMapForSurface(entry.getKey(), tempPropositions, surfacePropositions);
            }
        }
        return surfacePropositions;
    }

    private void updatePropositions(final Map<Surface, List<Proposition>> newPropositions, final List<Surface> surfacesToRemove) {
        // add new surfaces or update replace existing surfaces
        Map<Surface, List<Proposition>> tempPropositionsMap = new HashMap<>(propositions);
        for (final Map.Entry<Surface, List<Proposition>> entry : newPropositions.entrySet()) {
            tempPropositionsMap = MessagingUtils.updatePropositionMapForSurface(entry.getKey(), entry.getValue(), tempPropositionsMap);
        }

        // remove any surfaces if necessary
        for (final Surface surface : surfacesToRemove) {
            tempPropositionsMap.remove(surface);
        }

        propositions = tempPropositionsMap;
    }

    /**
     * Returns propositions by surface from `propositions` matching the provided `surfaces`
     *
     * @param surfaces A {@link List<Surface>} of surfaces to retrieve feeds for
     * @return {@link Map<Surface, List<  Proposition >>} containing previously fetched propositions
     */
    private Map<Surface, List<Proposition>> retrieveCachedPropositions(final List<Surface> surfaces) {
        Map<Surface, List<Proposition>> propositionMap = new HashMap<>();
        for (final Surface surface : surfaces) {
            final List<Proposition> propositionsList = propositions.get(surface);
            if (!MessagingUtils.isNullOrEmpty(propositionsList)) {
                propositionMap.put(surface, propositionsList);
            }
        }
        return propositionMap;
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

        final Map<String, Object> consequenceDetails = triggeredConsequence.getDetail();
        if (MapUtils.isNullOrEmpty(consequenceDetails)) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to create an in-app message, the consequence details are null or empty");
            return;
        }

        final String consequenceType = DataReader.optString(consequenceDetails, MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA, "");

        // ensure we have a AJO IAM payload before creating a message
        if (StringUtils.isNullOrEmpty(consequenceType)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create an in-app message, missing consequence type.");
            return;
        }

        if (!consequenceType.equals(SCHEMA_IAM)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create an in-app message, unknown message consequence type: %s.", consequenceType);
            return;
        }

        try {
            final Map<String, Object> detailsDataMap = DataReader.optTypedMap(Object.class, consequenceDetails, MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, Collections.emptyMap());
            final Map<String, Object> mobileParameters = DataReader.optTypedMap(Object.class, detailsDataMap, MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS, Collections.emptyMap());
            final InternalMessage message = new InternalMessage(parent, triggeredConsequence, mobileParameters, messagingCacheUtilities.getAssetsMap());
            message.propositionInfo = propositionInfo.get(message.getId());
            message.trigger();
            message.show(true);
        } catch (final MessageRequiredFieldMissingException exception) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to create an in-app message, an exception occurred during creation: %s", exception.getLocalizedMessage());
        }
    }

    void setSerialWorkDispatcher (final SerialWorkDispatcher<Event> serialWorkDispatcher) {
        this.serialWorkDispatcher = serialWorkDispatcher;
    }

    Map<String, List<Surface>> getRequestedSurfacesForEventId() {
        return requestedSurfacesForEventId;
    }

    /**
     * Cache any asset URL's present in each {@code RuleConsequence} {@link com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence} detail.
     *
     * @param ruleConsequences A {@link List<RuleConsequence>} containing an in-app message rule consequences.
     */
    private void cacheImageAssetsFromPayload(final List<RuleConsequence> ruleConsequences) {
        final List<String> remoteAssetsList = new ArrayList<>();
        try {
            for (final RuleConsequence consequence : ruleConsequences) {
                final Map<String, Object> details = consequence.getDetail();
                if (MapUtils.isNullOrEmpty(details)) {
                    return;
                }
                final Map<String, Object> data = DataReader.getTypedMap(Object.class, details, DATA);
                final List<String> remoteAssets = DataReader.getStringList(data, MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
                if (!MessagingUtils.isNullOrEmpty(remoteAssets)) {
                    for (final String remoteAsset : remoteAssets) {
                        if (UrlUtils.isValidUrl(remoteAsset) && !remoteAssetsList.contains(remoteAsset)) {
                            Log.debug(LOG_TAG, SELF_TAG, "Image asset to be cached (%s) ", remoteAsset);
                            remoteAssetsList.add(remoteAsset);
                        }
                    }
                }
            }
            messagingCacheUtilities.cacheImageAssets(remoteAssetsList);
        } catch (final DataReaderException exception) {
            Log.warning(LOG_TAG, SELF_TAG, "Failed to cache image asset, exception occurred %s", exception.getLocalizedMessage());
        }
    }

    // for testing, the size of the proposition info map should always mirror the number of rules currently loaded
    @VisibleForTesting
    int getRuleCount() {
        return propositionInfo.size();
    }

    @VisibleForTesting
    void setMessagesRequestEventId(final String messagesRequestEventId) {
        requestedSurfacesForEventId.put(messagesRequestEventId, Collections.singletonList(new Surface()));
    }

    @VisibleForTesting
    Map<Surface, List<Proposition>> getInProgressPropositions () {
        return inProgressPropositions;
    }
}