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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MessagingEdgeEventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.UrlUtils;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to handle the retrieval and processing of AJO payloads containing in-app,
 * content card, or code based messages. It is also responsible for the display of AJO in-app
 * messages.
 */
class EdgePersonalizationResponseHandler {
    private static final String SELF_TAG = "EdgePersonalizationResponseHandler";

    private static final List<String> SUPPORTED_SCHEMAS =
            new ArrayList<String>() {
                {
                    add(MessagingConstants.SchemaValues.SCHEMA_HTML_CONTENT);
                    add(MessagingConstants.SchemaValues.SCHEMA_JSON_CONTENT);
                    add(MessagingConstants.SchemaValues.SCHEMA_RULESET_ITEM);
                }
            };
    final MessagingExtension parent;
    private final MessagingCacheUtilities messagingCacheUtilities;
    private final ExtensionApi extensionApi;
    private final LaunchRulesEngine launchRulesEngine;
    private final ContentCardRulesEngine contentCardRulesEngine;

    private Map<Surface, List<Proposition>> inMemoryPropositions = new HashMap<>();
    private Map<String, PropositionInfo> propositionInfo = new HashMap<>();

    // keeps a list of all surfaces requested per personalization request event by event id
    private final Map<String, List<Surface>> requestedSurfacesForEventId = new HashMap<>();

    // used while processing streaming payloads for a single request
    private Map<Surface, List<Proposition>> inProgressPropositions = new HashMap<>();

    // used to manage in app rules between multiple surfaces and multiple requests
    private final Map<Surface, List<LaunchRule>> inAppRulesBySurface = new HashMap<>();

    // used to manage content card rules between multiple surfaces and multiple requests
    private final Map<Surface, List<LaunchRule>> contentCardRulesBySurface = new HashMap<>();

    // used to manage content card rules between multiple surfaces and multiple requests
    private final Map<Surface, List<LaunchRule>> eventHistoryRulesBySurface = new HashMap<>();

    // holds content cards that the user has qualified for
    private Map<Surface, List<Proposition>> contentCardsBySurface = new HashMap<>();

    // Surfaces that have been refreshed from a live network response this session. In-memory only
    // (never persisted) and empty on every launch, so a card served after a cold start — before any
    // successful network refresh — is correctly reported as served from the persisted cache.
    // Maintained
    // in exactly one place on the network path (removeOrReplaceContentCards: insert on refresh,
    // remove
    // on eviction) and cleared with the content card state. Disk hydration deliberately does NOT
    // add to
    // it. Used to derive servedFromPersistentCache at interaction time.
    private final java.util.Set<Surface> networkRefreshedSurfaces = new java.util.HashSet<>();

    // tracks event ids where a non-recoverable edge error was received
    private final java.util.Set<String> nonRecoverableErrorEventIds = new java.util.HashSet<>();

    private SerialWorkDispatcher<Event> serialWorkDispatcher;

    /**
     * Constructor
     *
     * @param parent {@link MessagingExtension} instance that is the parent of this {@code
     *     EdgePersonalizationResponseHandler}
     * @param extensionApi {@link ExtensionApi} instance
     * @param rulesEngine {@link LaunchRulesEngine} instance to use for loading in-app message rule
     *     payloads
     * @param contentCardRulesEngine {@link ContentCardRulesEngine} instance to use for loading
     *     content card rule payloads
     */
    EdgePersonalizationResponseHandler(
            final MessagingExtension parent,
            final ExtensionApi extensionApi,
            final LaunchRulesEngine rulesEngine,
            final ContentCardRulesEngine contentCardRulesEngine) {
        this(parent, extensionApi, rulesEngine, contentCardRulesEngine, null);
    }

    @SuppressWarnings("NestedIfDepth")
    @VisibleForTesting
    EdgePersonalizationResponseHandler(
            final MessagingExtension parent,
            final ExtensionApi extensionApi,
            final LaunchRulesEngine rulesEngine,
            final ContentCardRulesEngine contentCardRulesEngine,
            final MessagingCacheUtilities messagingCacheUtilities) {
        this.parent = parent;
        this.extensionApi = extensionApi;
        this.launchRulesEngine = rulesEngine;
        this.contentCardRulesEngine = contentCardRulesEngine;

        // load cached propositions (if any) when EdgePersonalizationResponseHandler is instantiated
        this.messagingCacheUtilities =
                messagingCacheUtilities != null
                        ? messagingCacheUtilities
                        : new MessagingCacheUtilities();
        if (this.messagingCacheUtilities.arePropositionsCached()) {
            final Map<Surface, List<Proposition>> cachedPropositions =
                    this.messagingCacheUtilities.getCachedPropositions();
            if (cachedPropositions != null && !cachedPropositions.isEmpty()) {
                Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Retrieved cached propositions, attempting to load the propositions into"
                                + " the rules engine.");
                inMemoryPropositions = cachedPropositions;
                final List<Surface> surfaces = new ArrayList<>();
                // get surfaces
                for (final Map.Entry<Surface, List<Proposition>> cacheEntry :
                        cachedPropositions.entrySet()) {
                    surfaces.add(cacheEntry.getKey());
                }

                final ParsedPropositions parsedPropositions =
                        new ParsedPropositions(
                                cachedPropositions,
                                surfaces,
                                extensionApi,
                                // this constructor only loads cached IAM rules; content card
                                // persistence is handled separately, so the offline flag is
                                // irrelevant here (matches iOS which passes false).
                                false);
                final Map<Surface, List<LaunchRule>> inAppRules =
                        parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.INAPP);
                // register any in-app propositions which were previously cached
                if (inAppRules != null) {
                    final List<LaunchRule> rulesToReplace = new ArrayList<>();
                    for (final Map.Entry<Surface, List<LaunchRule>> entry : inAppRules.entrySet()) {
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
     * Reads the {@code messaging.contentCardOfflineAvailable} flag from Configuration shared state.
     * Defaults to {@code false} when the key is absent — disk persistence is opt-in. Apps must
     * explicitly set {@code messaging.contentCardOfflineAvailable = true} in their configuration to
     * enable content card persistence across sessions.
     *
     * @return {@code true} if offline content card availability is enabled
     */
    boolean isContentCardOfflineAvailable() {
        try {
            final SharedStateResult result =
                    extensionApi.getSharedState(
                            MessagingConstants.SharedState.Configuration.EXTENSION_NAME,
                            null,
                            false,
                            SharedStateResolution.LAST_SET);
            if (result == null || result.getValue() == null) {
                return false;
            }
            return DataReader.optBoolean(
                    result.getValue(),
                    MessagingConstants.SharedState.Configuration.CONTENT_CARD_OFFLINE_AVAILABLE,
                    false);
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Determines whether the device currently has internet connectivity, used to short-circuit
     * user-triggered proposition fetches when offline. Fails open (returns {@code true}) when
     * connectivity cannot be determined, so a fetch is never wrongly suppressed.
     *
     * @return {@code true} if internet is available or connectivity is indeterminate
     */
    boolean isInternetAvailable() {
        try {
            final android.content.Context context =
                    ServiceProvider.getInstance().getAppContextService().getApplicationContext();
            if (context == null) {
                return true;
            }
            final android.net.ConnectivityManager connectivityManager =
                    (android.net.ConnectivityManager)
                            context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return true;
            }
            return com.adobe.marketing.mobile.internal.util.NetworkUtils.isInternetAvailable(
                    connectivityManager);
        } catch (final Exception e) {
            return true;
        }
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to fetch propositions
     * (currently in-app messages, content cards, or code-based experiences). The surface URIs used
     * in the request are generated using the application id of the app. If the application id is
     * unavailable, calling this method will do nothing.
     *
     * @param event The fetch propositions {@link Event}
     * @param surfaces A {@code List<Surface>} of surfaces for fetching propositions, if available.
     */
    void fetchPropositions(final Event event, final List<Surface> surfaces) {
        fetchPropositions(event, surfaces, null, null);
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to fetch propositions
     * (currently in-app messages, content cards, or code-based experiences), attaching any
     * caller-provided custom XDM and/or free-form data to the personalization request. The surface
     * URIs used in the request are generated using the application id of the app. If the
     * application id is unavailable, calling this method will do nothing.
     *
     * @param event The fetch propositions {@link Event}
     * @param surfaces A {@code List<Surface>} of surfaces for fetching propositions, if available.
     * @param customXdm An optional {@code Map<String, Object>} of custom XDM to merge into the
     *     personalization request XDM.
     * @param customData An optional {@code Map<String, Object>} of custom data to merge into the
     *     personalization request data.
     */
    @SuppressWarnings("NestedIfDepth")
    void fetchPropositions(
            final Event event,
            final List<Surface> surfaces,
            final Map<String, Object> customXdm,
            final Map<String, Object> customData) {
        // get a completion handler for requesting event if one exists
        final CompletionHandler handler =
                parent.completionHandlerForOriginatingEventId(event.getUniqueIdentifier());

        final List<Surface> requestedSurfaces = new ArrayList<>();
        Surface appSurface = null;
        // if surfaces are provided, use them - otherwise assume the request is for base surface
        // (mobileapp://{application package name})
        if (surfaces != null && !surfaces.isEmpty()) {
            for (final Surface surface : surfaces) {
                if (surface.isValid()) {
                    requestedSurfaces.add(surface);
                }
            }

            if (requestedSurfaces.isEmpty()) {
                Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to update messages, no valid surfaces found.");
                if (handler != null) {
                    handler.handle.call(false);
                }
                return;
            }
        } else {
            appSurface = new Surface();
            if (appSurface.getUri().equals("unknown")) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to update messages, couldn't create a valid app surface.");
                if (handler != null) {
                    handler.handle.call(false);
                }
                return;
            }
            requestedSurfaces.add(appSurface);
        }

        // create list of strings from the validated requested surface list
        final List<String> validatedSurfaceUris = new ArrayList<>();
        for (final Surface surface : requestedSurfaces) {
            validatedSurfaceUris.add(surface.getUri());
        }

        // build the personalization request event data, merging any caller-provided custom XDM/data
        final Map<String, Object> eventData =
                createPersonalizationRequestEventData(validatedSurfaceUris, customXdm, customData);

        final Event newEvent =
                new Event.Builder(
                                MessagingConstants.EventName.REFRESH_MESSAGES_EVENT,
                                EventType.EDGE,
                                MessagingConstants.EventSource.REQUEST_CONTENT)
                        .setEventData(eventData)
                        .chainToParentEvent(event)
                        .build();

        // create entries in our local containers for managing streamed responses from edge
        beginRequestForSurfaces(newEvent, requestedSurfaces);

        // if we have a handler, update the edge request event id and put it back in the list
        if (handler != null) {
            handler.edgeRequestEventId = newEvent.getUniqueIdentifier();
            MessagingExtension.addCompletionHandler(handler);
        }

        // dispatch the event and handle the response callback
        MobileCore.dispatchEventWithResponseCallback(
                newEvent,
                MessagingConstants.RESPONSE_CALLBACK_TIMEOUT,
                new AdobeCallbackWithError<Event>() {
                    @Override
                    public void fail(final AdobeError adobeError) {
                        // response event failed or timed out, need to remove this event from the
                        // queue
                        String eventId = newEvent.getUniqueIdentifier();
                        requestedSurfacesForEventId.remove(eventId);
                        CompletionHandler completionHandler =
                                parent.completionHandlerForEdgeRequestEventId(eventId);
                        if (completionHandler != null) {
                            completionHandler.handle.call(false);
                        }
                        serialWorkDispatcher.resume();
                        Log.warning(
                                MessagingConstants.LOG_TAG,
                                SELF_TAG,
                                "Unable to run completion logic for a personalization request event"
                                        + " - error occurred: %s",
                                adobeError.getErrorName());
                    }

                    // the callback is called by Edge extension when a request's stream has been
                    // closed
                    @Override
                    public void call(final Event responseCompleteEvent) {
                        // dispatch an event signaling messaging extension needs to finalize this
                        // event
                        // it must be dispatched to the event queue to avoid a race with the events
                        // containing propositions
                        final String endingEventId =
                                InternalMessagingUtils.getRequestEventId(responseCompleteEvent);
                        final Map<String, Object> eventData = new HashMap<>();
                        eventData.put(
                                MessagingConstants.EventDataKeys.Messaging.ENDING_EVENT_ID,
                                endingEventId);
                        final Event processCompletedEvent =
                                new Event.Builder(
                                                MessagingConstants.EventName
                                                        .FINALIZE_PROPOSITIONS_RESPONSE,
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
     * Builds the event data for a {@code decisioning.propositionFetch} edge event for the provided
     * surface URIs.
     *
     * <p>Any {@code customXdm} is merged into the request XDM and any {@code customData} is merged
     * into the request data. Internal keys required by the SDK — the personalization request {@code
     * eventType} in XDM and the {@code __adobe} in-app response format in data — always take
     * precedence and cannot be overwritten by the caller.
     *
     * @param validatedSurfaceUris the validated surface URI strings to request propositions for.
     * @param customXdm optional custom XDM to merge into the request XDM.
     * @param customData optional custom data to merge into the request data.
     * @return the event data {@code Map} for the chained edge request event.
     */
    Map<String, Object> createPersonalizationRequestEventData(
            final List<String> validatedSurfaceUris,
            final Map<String, Object> customXdm,
            final Map<String, Object> customData) {
        final Map<String, Object> eventData = new HashMap<>();
        final Map<String, Object> messageRequestData = new HashMap<>();
        final Map<String, Object> personalizationData = new HashMap<>();

        // add query parameters containing supported schemas and requested surfaces
        personalizationData.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SCHEMAS, SUPPORTED_SCHEMAS);
        personalizationData.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.SURFACES,
                validatedSurfaceUris);
        messageRequestData.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PERSONALIZATION,
                personalizationData);
        eventData.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.QUERY, messageRequestData);

        // add xdm with an event type of decisioning.propositionFetch, merging any caller-provided
        // XDM.
        // the internal eventType is required and always wins over a caller-provided value.
        final Map<String, Object> xdmData = new HashMap<>();
        if (customXdm != null && !customXdm.isEmpty()) {
            xdmData.putAll(customXdm);
        }
        xdmData.put(
                MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.EVENT_TYPE,
                MessagingConstants.EventDataKeys.Messaging.Inbound.EventType.PROPOSITION_FETCH);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.XDM, xdmData);

        // add a data object specifying the response format desired from XAS, merging any
        // caller-provided data. the internal __adobe namespace is required and always wins.
        final Map<String, Object> data = new HashMap<>();
        if (customData != null && !customData.isEmpty()) {
            data.putAll(customData);
        }
        final Map<String, Object> ajo = new HashMap<>();
        final Map<String, Object> inAppResponseFormat = new HashMap<>();
        inAppResponseFormat.put(
                MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.INAPP_RESPONSE_FORMAT,
                MessagingConstants.EventDataKeys.Messaging.Data.Value.NEW_IAM);
        ajo.put(MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.AJO, inAppResponseFormat);
        data.put(MessagingConstants.EventDataKeys.Messaging.Data.AdobeKeys.NAMESPACE, ajo);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.Data.Key.DATA, data);

        // add a request object so we get a response event from edge when the propositions stream is
        // closed for this event
        final Map<String, Object> request = new HashMap<>();
        request.put(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.SEND_COMPLETION, true);
        eventData.put(MessagingConstants.EventDataKeys.Messaging.XDMDataKeys.REQUEST, request);

        return eventData;
    }

    /**
     * Process the event containing the finalized edge response personalization notification data.
     *
     * @param event A {@link Event} containing the personalization notification complete edge
     *     response event.
     */
    void handleProcessCompletedEvent(final Event event) {
        final String endingEventId = InternalMessagingUtils.getEndingEventId(event);
        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(endingEventId);
        if (StringUtils.isNullOrEmpty(endingEventId)
                || MessagingUtils.isNullOrEmpty(requestedSurfaces)) {
            // shouldn't ever get here, but if we do, we don't have anything to process so we should
            // bail
            return;
        }

        Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "End of streaming response events for requesting event %s",
                endingEventId);
        endRequestForEventId(endingEventId);

        // dispatch notification event for request
        dispatchNotificationEventForSurfaces(requestedSurfaces);
        // resume processing the internal events queue after processing is completed for an update
        // propositions request
        Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "handleProcessCompletedEvent - Starting serial work dispatcher.");
        serialWorkDispatcher.resume();
    }

    /**
     * Process the event history rule consequence by removing the content card activity from the
     * in-memory cache using the proposition activity id.
     *
     * @param propositionItem A {@link PropositionItem} of type {@link
     *     EventHistoryOperationSchemaData} which contains the activity id of the content card to be
     *     removed.
     */
    void handleEventHistoryRuleConsequence(final PropositionItem propositionItem) {
        if (propositionItem == null) {
            return;
        }
        final EventHistoryOperationSchemaData eventHistorySchemaData =
                propositionItem.getEventHistoryOperationSchemaData();
        if (eventHistorySchemaData == null) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Ignoring event history rule consequence with id %s, not in expected format.",
                    propositionItem.getItemId());
            return;
        }
        final String activityId = eventHistorySchemaData.getActivityId();
        final String eventType = eventHistorySchemaData.getEventType();
        if (StringUtils.isNullOrEmpty(activityId) || StringUtils.isNullOrEmpty(eventType)) {
            // if the activity id or event type is empty, we do nothing
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Ignoring event history rule consequence with id %s, activity id or event type"
                            + " is empty.",
                    propositionItem.getItemId());
            return;
        }

        if (eventType.equals(MessagingConstants.EventHistoryOperationEventTypes.UNQUALIFY)
                || eventType.equals(
                        MessagingConstants.EventHistoryOperationEventTypes.DISQUALIFY)) {
            // remove the content card from the in-memory cache using the activity id
            for (final Map.Entry<Surface, List<Proposition>> contentCardEntry :
                    contentCardsBySurface.entrySet()) {
                final Surface surface = contentCardEntry.getKey();
                final List<Proposition> propositions = contentCardEntry.getValue();
                final List<Proposition> updatedPropositions = new ArrayList<>(propositions);
                for (final Proposition proposition : propositions) {
                    if (activityId.equals(proposition.getActivityId())) {
                        Log.debug(
                                MessagingConstants.LOG_TAG,
                                SELF_TAG,
                                "Removing content card proposition with activity id %s for"
                                        + " surface %s from in-memory cache.",
                                activityId,
                                surface.getUri());
                        updatedPropositions.remove(proposition);
                        // remove the content card schema data from the ContentCardMapper as well
                        ContentCardMapper.getInstance()
                                .removeContentCardSchemaData(proposition.getActivityId());
                    }
                }
                contentCardsBySurface.put(surface, updatedPropositions);
            }
        }
    }

    private void dispatchNotificationEventForSurfaces(final List<Surface> requestedSurfaces) {
        final Map<Surface, List<Proposition>> requestedPropositionsMap =
                retrieveCachedPropositions(requestedSurfaces);
        if (MapUtils.isNullOrEmpty(requestedPropositionsMap)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Not dispatching a notification event, personalization:decisions response does"
                            + " not contain propositions.");
            return;
        }

        // dispatch an event with the propositions received from the remote
        final Map<String, Object> eventData = new HashMap<>();
        final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
        for (final Map.Entry<Surface, List<Proposition>> propositionEntry :
                requestedPropositionsMap.entrySet()) {
            for (final Proposition proposition : propositionEntry.getValue()) {
                convertedPropositions.add(proposition.toEventData());
            }
        }
        eventData.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS,
                convertedPropositions);

        final Event event =
                new Event.Builder(
                                MessagingConstants.EventName.MESSAGE_PROPOSITIONS_NOTIFICATION,
                                EventType.MESSAGING,
                                MessagingConstants.EventSource.NOTIFICATION)
                        .setEventData(eventData)
                        .build();

        extensionApi.dispatch(event);
    }

    /**
     * Dispatches an event with previously cached content cards and code based experiences from the
     * SDK for the provided surfaces.
     *
     * @param surfaces A {@code List<Surface>} of surfaces to use for retrieving cached content
     * @param event The retrieve message {@link Event}
     */
    void retrieveInMemoryPropositions(final List<Surface> surfaces, final Event event) {
        final List<Surface> requestedSurfaces = new ArrayList<>();
        if (MessagingUtils.isNullOrEmpty(surfaces)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to retrieve messages, no surfaces were requested.");
            extensionApi.dispatch(
                    InternalMessagingUtils.createErrorResponseEvent(
                            event, AdobeErrorExt.INVALID_REQUEST));
            return;
        }

        for (final Surface surface : surfaces) {
            if (surface.isValid()) {
                requestedSurfaces.add(surface);
            }
        }

        if (requestedSurfaces.isEmpty()) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to retrieve messages, no valid surfaces found.");
            extensionApi.dispatch(
                    InternalMessagingUtils.createErrorResponseEvent(
                            event, AdobeErrorExt.INVALID_REQUEST));
            return;
        }

        // get a copy of qualified content cards and filter by requested surfaces
        final Map<Surface, List<Proposition>> requestedContentCards =
                new HashMap<>(contentCardsBySurface);
        requestedContentCards.keySet().retainAll(requestedSurfaces);

        // get a copy of in memory propositions (cbe)
        Map<Surface, List<Proposition>> requestedPropositions =
                retrieveCachedPropositions(requestedSurfaces);

        // merge their entries
        for (final Map.Entry<Surface, List<Proposition>> entry : requestedContentCards.entrySet()) {
            requestedPropositions =
                    MessagingUtils.updatePropositionMapForSurface(
                            entry.getKey(), entry.getValue(), requestedPropositions);
        }

        // dispatch an event with the cached content card propositions
        final Map<String, Object> eventData = new HashMap<>();
        final List<Map<String, Object>> convertedPropositions = new ArrayList<>();
        for (final Map.Entry<Surface, List<Proposition>> propositionEntry :
                requestedPropositions.entrySet()) {
            for (final Proposition proposition : propositionEntry.getValue()) {
                convertedPropositions.add(proposition.toEventData());
            }
        }
        eventData.put(
                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITIONS,
                convertedPropositions);

        final Event responseEvent =
                new Event.Builder(
                                MessagingConstants.EventName.MESSAGE_PROPOSITIONS_RESPONSE,
                                EventType.MESSAGING,
                                EventSource.RESPONSE_CONTENT)
                        .setEventData(eventData)
                        .inResponseToEvent(event)
                        .build();

        extensionApi.dispatch(responseEvent);
    }

    /**
     * Validates that the edge response event is a response that we are waiting for. If the returned
     * payload is empty then the Messaging cache and any loaded rules in the Messaging extension's
     * {@link LaunchRulesEngine} are cleared.
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved
     *     via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        // validate this is one of our events
        final String requestEventId = InternalMessagingUtils.getRequestEventId(edgeResponseEvent);

        if (StringUtils.isNullOrEmpty(requestEventId)
                || (!requestedSurfacesForEventId.containsKey(requestEventId)
                        && !"TESTING_ID".equals(requestEventId))) {
            return;
        }

        Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Processing propositions from personalization:decisions network response for event"
                        + " %s.",
                requestEventId);

        // update in progress propositions
        // convert the payload into a list of Proposition(s)
        final List<Map<String, Object>> payloads =
                DataReader.optTypedListOfMap(
                        Object.class,
                        edgeResponseEvent.getEventData(),
                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PAYLOAD,
                        null);
        if (MessagingUtils.isNullOrEmpty(payloads)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Ignoring personalization:decisions response with no propositions.");
            return;
        }
        List<Proposition> propositions =
                InternalMessagingUtils.getPropositionsFromPayloads(payloads);

        if (MessagingUtils.isNullOrEmpty(propositions)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Ignoring personalization:decisions response with no propositions.");
            return;
        }

        // loop through propositions for this event and add them to existing proposition map by
        // surface
        for (final Proposition proposition : propositions) {
            final Surface surface = Surface.fromUriString(proposition.getScope());
            inProgressPropositions =
                    MessagingUtils.updatePropositionMapForSurface(
                            surface, proposition, inProgressPropositions);
        }
    }

    private void beginRequestForSurfaces(final Event event, final List<Surface> surfaces) {
        requestedSurfacesForEventId.put(event.getUniqueIdentifier(), surfaces);

        // add the Edge request event to update propositions in the events queue.
        serialWorkDispatcher.offer(event);
    }

    private void endRequestForEventId(final String eventId) {
        // if a non-recoverable edge error was received for this event,
        // skip applying proposition changes to preserve last-known-good state
        boolean requestFailed = false;
        if (nonRecoverableErrorEventIds.remove(eventId)) {
            requestFailed = true;
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Non-recoverable edge error received for event %s, preserving"
                            + " last-known-good content card state.",
                    eventId);
        } else {
            // update in memory propositions
            applyPropositionChangeForEventId(eventId);
        }

        // remove event from surfaces dictionary
        requestedSurfacesForEventId.remove(eventId);

        // clear pending propositions
        inProgressPropositions.clear();

        // call the handler if we have one, passing false on failure (matching iOS behavior)
        final CompletionHandler handler = parent.completionHandlerForEdgeRequestEventId(eventId);
        if (handler != null) {
            handler.handle.call(!requestFailed);
        }
    }

    private void applyPropositionChangeForEventId(final String eventId) {
        // get the list of requested surfaces for this event
        final List<Surface> requestedSurfaces = requestedSurfacesForEventId.get(eventId);
        if (MessagingUtils.isNullOrEmpty(requestedSurfaces)) {
            return;
        }

        final boolean offlineAvailable = isContentCardOfflineAvailable();
        final ParsedPropositions parsedPropositions =
                new ParsedPropositions(
                        inProgressPropositions, requestedSurfaces, extensionApi, offlineAvailable);

        // we need to preserve cache for any surfaces that were not a part of this request
        // any requested surface that is absent from the response needs to be removed from cache and
        // persistence
        final Set<Surface> returnedSurfaces = inProgressPropositions.keySet();
        final List<Surface> surfacesToRemove = new ArrayList<>(requestedSurfaces);
        surfacesToRemove.removeAll(returnedSurfaces);

        // update persistence, reporting data cache, and finally rules engine for in-app messages
        // order matters here because the rules engine must be a full replace, and when we update
        // persistence we will be removing empty surfaces and making sure unrequested surfaces
        // continue to have their rules active
        updatePropositions(parsedPropositions.propositionsToCache, surfacesToRemove);
        updatePropositionInfo(parsedPropositions.propositionInfoToCache, surfacesToRemove);
        messagingCacheUtilities.cachePropositions(
                parsedPropositions.propositionsToPersist, surfacesToRemove);

        // disk-first write strategy: persist content card propositions to disk
        // before updating in-memory state, ensuring crash consistency.
        // Always pass even when empty with surfaces to remove — ensures old entries get cleaned up.
        if (offlineAvailable) {
            messagingCacheUtilities.cacheContentCardPropositions(
                    parsedPropositions.contentCardPropositionsToPersist, surfacesToRemove);
        } else {
            // Feature disabled — clear stale persisted data. This method is only called on
            // success (non-recoverable errors skip applyPropositionChangeForEventId entirely),
            // so it is always safe to wipe the cache here.
            messagingCacheUtilities.clearPersistedContentCardCache();
        }

        // apply rules. Content card provenance is tracked per-surface in networkRefreshedSurfaces,
        // maintained by removeOrReplaceContentCards (invoked via updateRulesEngines below); no
        // per-proposition tagging is needed on the network path.
        updateRulesEngines(parsedPropositions.surfaceRulesBySchemaType, requestedSurfaces);
    }

    private void updateRulesEngines(
            @NonNull final Map<SchemaType, Map<Surface, List<LaunchRule>>> surfaceRulesBySchemaType,
            @NonNull final List<Surface> requestedSurfaces) {
        final List<Surface> ccRequestedSurfaces = requestedSurfaces;

        // process rules from response
        processRulesForSchemaType(
                SchemaType.INAPP, surfaceRulesBySchemaType, requestedSurfaces, inAppRulesBySurface);
        processRulesForSchemaType(
                SchemaType.CONTENT_CARD,
                surfaceRulesBySchemaType,
                ccRequestedSurfaces,
                contentCardRulesBySurface);
        processRulesForSchemaType(
                SchemaType.EVENT_HISTORY_OPERATION,
                surfaceRulesBySchemaType,
                requestedSurfaces,
                eventHistoryRulesBySurface);

        // Always sync the content card rules engine and refresh the qualified cache.
        // processRulesForSchemaType clears contentCardRulesBySurface for requested surfaces
        // when CONTENT_CARD is absent from the response (e.g. all campaigns removed
        // server-side); we must still replaceRules and call removeOrReplaceContentCards in
        // that case — not only when the key is present.
        final List<LaunchRule> collectedContentCardRules =
                collectRulesFrom(contentCardRulesBySurface);
        contentCardRulesEngine.replaceRules(collectedContentCardRules);

        final Event contentCardSeedEvent =
                new Event.Builder(
                                "Seed content cards",
                                EventType.MESSAGING,
                                EventSource.REQUEST_CONTENT)
                        .build();
        removeOrReplaceContentCards(contentCardSeedEvent, ccRequestedSurfaces);

        // Always sync the in-app + event history rules engine, for the same reason as
        // content cards above: processRulesForSchemaType already cleared stale entries from
        // inAppRulesBySurface / eventHistoryRulesBySurface, and the engine must reflect that.
        final List<LaunchRule> collectedInAppRules = collectRulesFrom(inAppRulesBySurface);

        // Pre-fetch assets only when the response actually contained new in-app rules
        if (surfaceRulesBySchemaType.get(SchemaType.INAPP) != null) {
            final List<RuleConsequence> collectedInAppConsequences = new ArrayList<>();
            for (final LaunchRule rule : collectedInAppRules) {
                collectedInAppConsequences.addAll(rule.getConsequenceList());
            }
            cacheImageAssetsFromPayload(collectedInAppConsequences);
        }

        final List<LaunchRule> collectedInAppAndEventHistoryRules =
                new ArrayList<>(collectedInAppRules);
        collectedInAppAndEventHistoryRules.addAll(collectRulesFrom(eventHistoryRulesBySurface));
        launchRulesEngine.replaceRules(collectedInAppAndEventHistoryRules);
    }

    private void processRulesForSchemaType(
            final SchemaType schemaType,
            final Map<SchemaType, Map<Surface, List<LaunchRule>>> surfaceRulesBySchemaType,
            final List<Surface> requestedSurfaces,
            final Map<Surface, List<LaunchRule>> rulesBySurface) {
        final Map<Surface, List<LaunchRule>> newRules = surfaceRulesBySchemaType.get(schemaType);
        if (newRules != null) {
            final Set<Surface> newSurfaces = newRules.keySet();
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Updating definitions for surfaces %s with schema type %s.",
                    newSurfaces.toString(),
                    schemaType.toString());

            // replace rules for each surface we got back
            rulesBySurface.putAll(newRules);

            // remove any surfaces that were requested but had no content returned
            final List<Surface> surfacesToRemove = new ArrayList<>(requestedSurfaces);
            surfacesToRemove.removeAll(newSurfaces);
            for (final Surface surface : surfacesToRemove) {
                Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Removing definitions for surface %s with schema type %s.",
                        surface.getUri(),
                        schemaType.toString());
                rulesBySurface.remove(surface);
            }
        } else {
            // no rules of this schema type in the response, clear any existing rules for the
            // requested surfaces
            for (final Surface surface : requestedSurfaces) {
                Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Removing definitions for surface %s with schema type %s.",
                        surface.getUri(),
                        schemaType.toString());
                rulesBySurface.remove(surface);
            }
        }
    }

    private List<LaunchRule> collectRulesFrom(final Map<Surface, List<LaunchRule>> rulesBySurface) {
        final Collection<List<LaunchRule>> allRules = rulesBySurface.values();
        final List<LaunchRule> collectedRules = new ArrayList<>();
        for (final List<LaunchRule> rules : allRules) {
            collectedRules.addAll(rules);
        }
        return collectedRules;
    }

    /**
     * Incrementally updates the content card cache for surfaces qualified by the rules engine.
     *
     * <p>Called during rules engine processing (e.g. lifecycle or generic events). For each surface
     * qualified by the provided {@link Event}:
     *
     * <ul>
     *   <li>If a proposition already exists in the cache, it is removed and re-added (replaced) so
     *       the latest data is reflected without duplicates.
     *   <li>If a proposition is new (not previously in the cache), a {@code TRIGGER} event is sent
     *       to Edge Network and the proposition is added to the cache.
     * </ul>
     *
     * <p>This method is additive — it does not remove surfaces that return no results. Use {@link
     * #removeOrReplaceContentCards(Event, List)} for authoritative refreshes that should clear
     * stale surfaces.
     *
     * @param event the rules engine {@link Event} that may result in content card qualification.
     */
    @SuppressWarnings("NestedIfDepth")
    void addOrReplaceContentCards(final Event event) {
        final Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                getPropositionsFromContentCardRulesEngine(event);
        for (final Map.Entry<Surface, List<Proposition>> entry :
                qualifiedContentCardsBySurface.entrySet()) {
            final List<Proposition> propositions = entry.getValue();
            final Surface surface = entry.getKey();
            List<Proposition> existingPropositionsArray = contentCardsBySurface.get(surface);
            if (existingPropositionsArray == null) {
                existingPropositionsArray = new ArrayList<>();
            }

            int startingCount = existingPropositionsArray.size();
            // Track proposition items that are new so we can fire TRIGGER events for them
            List<PropositionItem> newPropositionItems = new ArrayList<>();

            for (final Proposition proposition : propositions) {
                if (existingPropositionsArray.contains(proposition)) {
                    // Remove the stale entry so the updated proposition is added at the end
                    existingPropositionsArray.remove(proposition);
                } else {
                    // Proposition is new — collect its first item for a batched TRIGGER event
                    final List<PropositionItem> propItems = proposition.getItems();
                    if (!propItems.isEmpty()) {
                        newPropositionItems.add(propItems.get(0));
                    }
                }
                existingPropositionsArray.add(proposition);
                storeContentCardInMapper(proposition);
            }

            contentCardsBySurface.put(surface, existingPropositionsArray);
            sendTriggersForNewPropositions(newPropositionItems);
            logContentCardCountChange(surface, startingCount, existingPropositionsArray.size());
        }
    }

    /**
     * Authoritatively refreshes the content card cache using the response from a personalization
     * network request.
     *
     * <p>Called after a personalization response stream completes. Treats the response as the
     * source of truth for the requested surfaces:
     *
     * <ul>
     *   <li>Any requested surface that returned no qualified propositions is removed from the
     *       cache, preventing stale cards from persisting after a server-side change.
     *   <li>For surfaces that did return propositions, the cached list is fully replaced (not
     *       merged) with the new results.
     *   <li>Propositions that are new (not previously in the cache) generate a {@code TRIGGER}
     *       event sent to Edge Network.
     * </ul>
     *
     * @param event the personalization response {@link Event} used to query the rules engine.
     * @param requestedSurfaces the list of {@link Surface}s that were included in the originating
     *     personalization request; used to identify surfaces that should be cleared if absent from
     *     the response.
     */
    void removeOrReplaceContentCards(final Event event, final List<Surface> requestedSurfaces) {
        final Map<Surface, List<Proposition>> qualifiedContentCardsBySurface =
                getPropositionsFromContentCardRulesEngine(event);

        // Only touch surfaces explicitly part of this network request. The content card rules
        // engine
        // re-evaluates ALL loaded rules on every call, so the qualified map may include surfaces
        // that
        // were hydrated from disk for a different request; leaving those untouched keeps them out
        // of
        // networkRefreshedSurfaces so their cards keep reporting servedFromPersistentCache = true.
        //
        // This is also the single place that maintains networkRefreshedSurfaces: a requested
        // surface
        // that returned content cards is marked network-refreshed for this session; a requested
        // surface
        // that returned nothing is evicted and removed from the set.
        for (final Surface surface : requestedSurfaces) {
            final List<Proposition> propositions = qualifiedContentCardsBySurface.get(surface);
            if (propositions == null) {
                // Requested surface returned no content cards — evict it (campaign ended
                // server-side).
                final List<Proposition> evictedPropositions = contentCardsBySurface.remove(surface);
                if (evictedPropositions != null) {
                    for (final Proposition proposition : evictedPropositions) {
                        ContentCardMapper.getInstance()
                                .removeContentCardSchemaData(proposition.getActivityId());
                    }
                }
                networkRefreshedSurfaces.remove(surface);
                continue;
            }

            List<Proposition> existingPropositionsArray = contentCardsBySurface.get(surface);
            if (existingPropositionsArray == null) {
                existingPropositionsArray = new ArrayList<>();
            }

            int startingCount = existingPropositionsArray.size();
            // Build a fresh list from the response rather than mutating the existing one
            List<Proposition> newPropositionsArray = new ArrayList<>();
            // Track proposition items that are genuinely new so we can fire TRIGGER events
            List<PropositionItem> newPropositionItems = new ArrayList<>();

            for (final Proposition proposition : propositions) {
                if (!existingPropositionsArray.contains(proposition)) {
                    // Proposition is new — collect its first item for a batched TRIGGER event
                    final List<PropositionItem> propItems = proposition.getItems();
                    if (!propItems.isEmpty()) {
                        newPropositionItems.add(propItems.get(0));
                    }
                }
                newPropositionsArray.add(proposition);
                storeContentCardInMapper(proposition);
            }

            // Remove ContentCardMapper entries for old propositions no longer in the fresh response
            for (final Proposition oldProposition : existingPropositionsArray) {
                if (!newPropositionsArray.contains(oldProposition)) {
                    ContentCardMapper.getInstance()
                            .removeContentCardSchemaData(oldProposition.getActivityId());
                }
            }

            contentCardsBySurface.put(surface, newPropositionsArray);
            // Mark this surface as refreshed from a live network response this session.
            networkRefreshedSurfaces.add(surface);
            sendTriggersForNewPropositions(newPropositionItems);
            logContentCardCountChange(surface, startingCount, newPropositionsArray.size());
        }
    }

    /**
     * Stores the first {@link PropositionItem}'s {@link ContentCardSchemaData} in the {@link
     * ContentCardMapper} for later use by the UI layer. No-op if the proposition has no items.
     *
     * @param proposition the {@link Proposition} whose first item should be stored.
     */
    private void storeContentCardInMapper(final Proposition proposition) {
        final List<PropositionItem> items = proposition.getItems();
        if (!items.isEmpty()) {
            final ContentCardSchemaData schemaData = items.get(0).getContentCardSchemaData();
            ContentCardMapper.getInstance().storeContentCardSchemaData(schemaData);
        }
    }

    /**
     * Sends batched {@code TRIGGER} events for the given proposition items. No-op if the list is
     * empty.
     *
     * @param newPropositionItems {@link List} of newly qualified {@link PropositionItem}s.
     */
    private void sendTriggersForNewPropositions(final List<PropositionItem> newPropositionItems) {
        if (!newPropositionItems.isEmpty()) {
            sendBatchedPropositionInteraction(
                    newPropositionItems, null, MessagingEdgeEventType.TRIGGER);
        }
    }

    /**
     * Logs a trace message when the number of qualified content cards for a surface changes.
     *
     * @param surface the {@link Surface} whose count changed.
     * @param oldCount the previous number of qualified propositions.
     * @param newCount the current number of qualified propositions.
     */
    private void logContentCardCountChange(
            final Surface surface, final int oldCount, final int newCount) {
        if (oldCount != newCount) {
            final Locale locale =
                    ServiceProvider.getInstance().getDeviceInfoService().getActiveLocale();
            String message =
                    newCount > 0
                            ? String.format(
                                    locale,
                                    "User has qualified for %d content card(s) for surface %s",
                                    newCount,
                                    surface.getUri())
                            : String.format(
                                    locale,
                                    "User has not qualified for any content card(s) for surface %s",
                                    surface.getUri());
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, message);
        }
    }

    /**
     * Sends a batched proposition interaction to the customer's experience event dataset.
     *
     * @param propositionItems {@link List} of {@link PropositionItem} instances to batch
     * @param interaction custom {@code String} describing the interaction
     * @param eventType {@link MessagingEdgeEventType} specifying event type for the interaction
     */
    private void sendBatchedPropositionInteraction(
            @NonNull final List<PropositionItem> propositionItems,
            @Nullable final String interaction,
            @NonNull final MessagingEdgeEventType eventType) {

        final PropositionInteractionBatcher propositionInteractionBatcher =
                new PropositionInteractionBatcher(eventType, interaction, propositionItems);
        final Map<String, Object> batchedPropositionInteractionXdm =
                propositionInteractionBatcher.generateBatchedXdmMap();

        if (batchedPropositionInteractionXdm == null) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot send batched proposition interaction, could not generate XDM data.");
            return;
        }

        // Record individual events in event history for each proposition item
        for (final PropositionItem propositionItem : propositionItems) {
            if (propositionItem.propositionReference != null
                    && propositionItem.propositionReference.get() != null) {
                PropositionHistory.record(
                        propositionItem.getProposition().getActivityId(), eventType, interaction);
            }
        }

        // Send the batched interaction event
        parent.sendPropositionInteraction(batchedPropositionInteractionXdm);
    }

    private void updatePropositionInfo(
            final Map<String, PropositionInfo> newPropositionInfo,
            final List<Surface> surfacesToRemove) {
        propositionInfo.putAll(newPropositionInfo);

        // currently, we can't remove entries that pre-exist by message id since they are not linked
        // to surfaces
        // need to get surface uri from propositionInfo.scope and remove entry based on incoming
        // surfaces
        if (MessagingUtils.isNullOrEmpty(surfacesToRemove)) {
            return;
        }

        final Map<String, PropositionInfo> tempPropositionInfoMap = new HashMap<>(propositionInfo);
        for (final Map.Entry<String, PropositionInfo> entry : tempPropositionInfoMap.entrySet()) {
            if (surfacesToRemove.contains(Surface.fromUriString(entry.getValue().scope))) {
                propositionInfo.remove(entry.getKey());
            }
        }
    }

    @SuppressWarnings("NestedForDepth")
    private Map<Surface, List<Proposition>> getPropositionsFromContentCardRulesEngine(
            final Event event) {
        Map<Surface, List<Proposition>> surfacePropositions = new HashMap<>();
        final Map<Surface, List<PropositionItem>> propositionItemsBySurface =
                contentCardRulesEngine.evaluate(event);
        if (!MapUtils.isNullOrEmpty(propositionItemsBySurface)) {
            for (final Map.Entry<Surface, List<PropositionItem>> entry :
                    propositionItemsBySurface.entrySet()) {
                final List<Proposition> tempPropositions = new ArrayList<>();
                for (final PropositionItem propositionItem : entry.getValue()) {
                    final PropositionInfo propositionInfo =
                            this.propositionInfo.get(propositionItem.getItemId());
                    if (propositionInfo == null) {
                        continue;
                    }

                    final Proposition proposition;
                    try {
                        proposition =
                                new Proposition(
                                        propositionInfo.id,
                                        propositionInfo.scope,
                                        propositionInfo.scopeDetails,
                                        new ArrayList<PropositionItem>() {
                                            {
                                                add(propositionItem);
                                            }
                                        });
                    } catch (MessageRequiredFieldMissingException e) {
                        continue;
                    }

                    // check to see if that proposition is already in the array (based on ID)
                    // if yes, append the propositionItem.  if not, create a new entry for the
                    // proposition with the new item.

                    Proposition existingProposition = null;
                    for (final Proposition messagingProposition : tempPropositions) {
                        if (messagingProposition.getUniqueId().equals(proposition.getUniqueId())) {
                            existingProposition = messagingProposition;
                            break;
                        }
                    }
                    if (existingProposition != null) {
                        propositionItem.propositionReference =
                                new SoftReference<>(existingProposition);
                        existingProposition.getItems().add(propositionItem);
                    } else {
                        propositionItem.propositionReference = new SoftReference<>(proposition);
                        tempPropositions.add(proposition);
                    }
                }

                surfacePropositions =
                        MessagingUtils.updatePropositionMapForSurface(
                                entry.getKey(), tempPropositions, surfacePropositions);
            }
        }
        return surfacePropositions;
    }

    private void updatePropositions(
            final Map<Surface, List<Proposition>> newPropositions,
            final List<Surface> surfacesToRemove) {
        // add new surfaces or replace existing surfaces
        Map<Surface, List<Proposition>> tempPropositionsMap = new HashMap<>(inMemoryPropositions);
        tempPropositionsMap.putAll(newPropositions);

        // remove any surfaces if necessary
        for (final Surface surface : surfacesToRemove) {
            tempPropositionsMap.remove(surface);
        }

        inMemoryPropositions = tempPropositionsMap;
    }

    /**
     * Returns propositions by surface from `propositions` matching the provided `surfaces`
     *
     * @param surfaces A {@link List<Surface>} of surfaces to retrieve feeds for
     * @return {@link Map<Surface, List< Proposition >>} containing previously fetched propositions
     */
    private Map<Surface, List<Proposition>> retrieveCachedPropositions(
            final List<Surface> surfaces) {
        Map<Surface, List<Proposition>> propositionMap = new HashMap<>();
        for (final Surface surface : surfaces) {
            final List<Proposition> propositionsList = inMemoryPropositions.get(surface);
            if (!MessagingUtils.isNullOrEmpty(propositionsList)) {
                propositionMap.put(surface, new ArrayList<>(propositionsList));
            }
        }
        return propositionMap;
    }

    /**
     * Creates an in-app message object then attempts to display it.
     *
     * @param propositionItem A {@link PropositionItem} containing an in-app message item data.
     */
    void createInAppMessage(final PropositionItem propositionItem) {
        if (propositionItem == null) {
            return;
        }
        try {
            final PresentableMessageMapper.InternalMessage message =
                    (PresentableMessageMapper.InternalMessage)
                            PresentableMessageMapper.getInstance()
                                    .createMessage(
                                            parent,
                                            propositionItem,
                                            messagingCacheUtilities.getAssetsMap(),
                                            propositionInfo.get(propositionItem.getItemId()));
            message.trigger();
            message.show();
        } catch (final MessageRequiredFieldMissingException | IllegalStateException exception) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to create an in-app message, an exception occurred during creation: %s",
                    exception.getLocalizedMessage());
        }
    }

    /**
     * Handles an edge error response event. If the error status code is non-recoverable, the event
     * id is recorded so that {@link #endRequestForEventId} will skip applying proposition changes,
     * preserving the last-known-good content card state.
     *
     * @param event the edge error response {@link Event}
     */
    void handleEdgeErrorResponse(final Event event) {
        final String requestEventId = InternalMessagingUtils.getRequestEventId(event);
        if (StringUtils.isNullOrEmpty(requestEventId)
                || !requestedSurfacesForEventId.containsKey(requestEventId)) {
            return;
        }

        // Treat the error as non-recoverable unless the status code is explicitly in the
        // recoverable set. Missing or zero status is conservatively treated as non-recoverable
        // (matching iOS behavior) to preserve the last-known-good state.
        final int status =
                DataReader.optInt(
                        event.getEventData(), MessagingConstants.EventDataKeys.EdgeError.STATUS, 0);
        if (!MessagingConstants.RECOVERABLE_EDGE_ERROR_STATUS_CODES.contains(status)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Received non-recoverable edge error (status %d) for event %s."
                            + " Content card state will be preserved.",
                    status,
                    requestEventId);
            nonRecoverableErrorEventIds.add(requestEventId);
        }
    }

    /**
     * Hydrates content card rules engine from persisted disk cache. Called at boot time before the
     * initial network fetch to provide offline content card availability.
     *
     * <p>Also loads event-history rules (disqualify/dismiss) into the main rules engine so that
     * dismiss/disqualify operations work for disk-loaded content cards without requiring a network
     * response first.
     */
    void hydrateContentCardRulesEngineFromDisk() {
        final Map<Surface, List<Proposition>> cachedContentCards =
                messagingCacheUtilities.getCachedContentCardPropositions();
        if (MapUtils.isNullOrEmpty(cachedContentCards)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "No persisted content card propositions found for hydration.");
            return;
        }

        Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Hydrating content card rules engine from %d persisted surface(s).",
                cachedContentCards.size());

        final List<Surface> surfaces = new ArrayList<>(cachedContentCards.keySet());
        final ParsedPropositions parsedPropositions =
                new ParsedPropositions(cachedContentCards, surfaces, extensionApi, true);

        // load proposition info for tracking
        propositionInfo.putAll(parsedPropositions.propositionInfoToCache);

        // load content card rules
        final Map<Surface, List<LaunchRule>> ccRules =
                parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.CONTENT_CARD);
        if (ccRules != null) {
            contentCardRulesBySurface.putAll(ccRules);
            final List<LaunchRule> allCCRules = collectRulesFrom(contentCardRulesBySurface);
            contentCardRulesEngine.replaceRules(allCCRules);

            // Seed the qualified cache directly from disk. We deliberately do NOT go through
            // removeOrReplaceContentCards here: disk-hydrated surfaces must stay out of
            // networkRefreshedSurfaces (so their cards report servedFromPersistentCache = true),
            // and
            // boot-seeded cards must not fire spurious TRIGGER analytics events.
            final Event seedEvent =
                    new Event.Builder(
                                    "Hydrate content cards from disk",
                                    EventType.MESSAGING,
                                    EventSource.REQUEST_CONTENT)
                            .build();
            final Map<Surface, List<Proposition>> diskQualified =
                    getPropositionsFromContentCardRulesEngine(seedEvent);
            for (final Map.Entry<Surface, List<Proposition>> entry : diskQualified.entrySet()) {
                if (!surfaces.contains(entry.getKey())) {
                    continue;
                }
                contentCardsBySurface.put(entry.getKey(), entry.getValue());
                for (final Proposition proposition : entry.getValue()) {
                    storeContentCardInMapper(proposition);
                }
            }
        }

        // load event-history rules (disqualify/dismiss) into the main rules engine
        // so that dismiss/disqualify operations work for disk-loaded content cards
        final Map<Surface, List<LaunchRule>> eventHistoryRules =
                parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.EVENT_HISTORY_OPERATION);
        if (eventHistoryRules != null) {
            eventHistoryRulesBySurface.putAll(eventHistoryRules);
        }

        // also preserve any IAM rules from persisted propositions to prevent
        // a later rebuildMainRulesEngine from clobbering them
        final Map<Surface, List<LaunchRule>> iamRules =
                parsedPropositions.surfaceRulesBySchemaType.get(SchemaType.INAPP);
        if (iamRules != null) {
            inAppRulesBySurface.putAll(iamRules);
        }

        // rebuild the main rules engine with combined IAM + event-history rules
        final List<LaunchRule> allMainRules = new ArrayList<>();
        allMainRules.addAll(collectRulesFrom(inAppRulesBySurface));
        allMainRules.addAll(collectRulesFrom(eventHistoryRulesBySurface));
        launchRulesEngine.replaceRules(allMainRules);

        // Disk-hydrated surfaces are intentionally NOT added to networkRefreshedSurfaces, so any
        // card
        // served for them reports servedFromPersistentCache = true until a live network response
        // refreshes the surface this session.
    }

    /**
     * Hydrates all persisted content cards from disk. Called during boot-time setup before the
     * initial network fetch.
     */
    void hydrateAllPersistedContentCards() {
        hydrateContentCardRulesEngineFromDisk();
    }

    /**
     * Enriches the provided proposition interaction XDM with per-item {@code
     * servedFromPersistentCache} flags for DISPLAY events only.
     *
     * <p>For each currently-qualified content card in the display event, every item receives {@code
     * data.characteristics.servedFromPersistentCache} at {@code
     * _experience.decisioning.propositions[].items[].data.characteristics.servedFromPersistentCache}.
     * The value is {@code true} when the card's surface has NOT been refreshed from a live network
     * response this session (i.e. it is being served from the persisted disk cache), and {@code
     * false} once a network response has refreshed that surface.
     *
     * <p>Only qualified content card propositions are annotated; IAM, CBE, and inbox propositions,
     * and non-DISPLAY events (interact, dismiss, trigger), are left untouched.
     *
     * @param propositionInteractionXdm the XDM map to enrich
     * @return the enriched XDM map (same reference, mutated in place)
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> enrichWithContentCardOrigin(
            final Map<String, Object> propositionInteractionXdm) {
        if (MapUtils.isNullOrEmpty(propositionInteractionXdm)) {
            return propositionInteractionXdm;
        }

        try {
            final Map<String, Object> experience =
                    (Map<String, Object>)
                            propositionInteractionXdm.get(
                                    MessagingConstants.TrackingKeys.EXPERIENCE);
            if (experience == null) return propositionInteractionXdm;

            final Map<String, Object> decisioning =
                    (Map<String, Object>)
                            experience.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .DECISIONING);
            if (decisioning == null) return propositionInteractionXdm;

            // only enrich DISPLAY events (matching iOS behavior)
            final Map<String, Object> propositionEventType =
                    (Map<String, Object>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITION_EVENT_TYPE);
            if (propositionEventType == null
                    || !propositionEventType.containsKey(MessagingConstants.TrackingKeys.DISPLAY)) {
                return propositionInteractionXdm;
            }

            final List<Map<String, Object>> propositions =
                    (List<Map<String, Object>>)
                            decisioning.get(
                                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                            .PROPOSITIONS);
            if (propositions == null || propositions.isEmpty()) return propositionInteractionXdm;

            // Map each qualified content card proposition id to its surface so provenance can be
            // derived from networkRefreshedSurfaces. Presence in this map also scopes enrichment to
            // content cards only (IAM / CBE / inbox propositions are absent and left untouched).
            final Map<String, Surface> surfaceByPropositionId = new HashMap<>();
            for (final Map.Entry<Surface, List<Proposition>> entry :
                    contentCardsBySurface.entrySet()) {
                for (final Proposition proposition : entry.getValue()) {
                    if (!surfaceByPropositionId.containsKey(proposition.getUniqueId())) {
                        surfaceByPropositionId.put(proposition.getUniqueId(), entry.getKey());
                    }
                }
            }

            for (final Map<String, Object> propositionMap : propositions) {
                final String propositionId =
                        DataReader.optString(
                                propositionMap,
                                MessagingConstants.EventDataKeys.Messaging.Inbound.Key.ID,
                                null);
                if (propositionId == null) continue;

                // Only annotate propositions that are currently qualified content cards.
                final Surface surface = surfaceByPropositionId.get(propositionId);
                if (surface == null) continue;

                // A content card is served from the persisted cache when its surface has NOT been
                // refreshed from a live network response this session.
                final boolean servedFromCache = !networkRefreshedSurfaces.contains(surface);

                // enrich each item with servedFromPersistentCache per-item
                final List<Map<String, Object>> items =
                        (List<Map<String, Object>>)
                                propositionMap.get(
                                        MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                                .ITEMS);
                if (items == null) continue;

                for (final Map<String, Object> item : items) {
                    Map<String, Object> data =
                            (Map<String, Object>)
                                    item.get(
                                            MessagingConstants.EventDataKeys.Messaging.Data.Key
                                                    .DATA);
                    if (data == null) {
                        data = new HashMap<>();
                        item.put(MessagingConstants.EventDataKeys.Messaging.Data.Key.DATA, data);
                    }

                    Map<String, Object> characteristics =
                            (Map<String, Object>)
                                    data.get(
                                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                                    .CHARACTERISTICS);
                    if (characteristics == null) {
                        characteristics = new HashMap<>();
                        data.put(
                                MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                        .CHARACTERISTICS,
                                characteristics);
                    }

                    characteristics.put(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key
                                    .SERVED_FROM_PERSISTENT_CACHE,
                            servedFromCache);
                }
            }
        } catch (final ClassCastException ignored) {
            // if the XDM structure is unexpected, skip enrichment
        }

        return propositionInteractionXdm;
    }

    /**
     * Clears all in-memory content card state (qualified cards and rules) and the persisted content
     * card cache. Used by both the public {@code clearPersistedPropositions} API and identity
     * reset.
     */
    void clearContentCards() {
        contentCardsBySurface.clear();
        contentCardRulesBySurface.clear();
        networkRefreshedSurfaces.clear();
        contentCardRulesEngine.replaceRules(new ArrayList<>());
        messagingCacheUtilities.clearPersistedContentCardCache();
        Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Content card state and persisted caches have been cleared.");
    }

    void setSerialWorkDispatcher(final SerialWorkDispatcher<Event> serialWorkDispatcher) {
        this.serialWorkDispatcher = serialWorkDispatcher;
    }

    Map<String, List<Surface>> getRequestedSurfacesForEventId() {
        return requestedSurfacesForEventId;
    }

    /**
     * Cache any asset URL's present in each {@link RuleConsequence} detail.
     *
     * @param ruleConsequences A {@link List<RuleConsequence>} containing an in-app message rule
     *     consequences.
     */
    private void cacheImageAssetsFromPayload(final List<RuleConsequence> ruleConsequences) {
        final List<String> remoteAssetsList = new ArrayList<>();
        try {
            for (final RuleConsequence consequence : ruleConsequences) {
                final Map<String, Object> details = consequence.getDetail();
                if (MapUtils.isNullOrEmpty(details)) {
                    return;
                }
                final Map<String, Object> data =
                        DataReader.getTypedMap(
                                Object.class,
                                details,
                                MessagingConstants.EventDataKeys.Messaging.Data.Key.DATA);
                final List<String> remoteAssets =
                        DataReader.getStringList(
                                data,
                                MessagingConstants.EventDataKeys.RulesEngine
                                        .MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS);
                if (!MessagingUtils.isNullOrEmpty(remoteAssets)) {
                    for (final String remoteAsset : remoteAssets) {
                        if (UrlUtils.isValidUrl(remoteAsset)
                                && !remoteAssetsList.contains(remoteAsset)) {
                            Log.debug(
                                    MessagingConstants.LOG_TAG,
                                    SELF_TAG,
                                    "Image asset to be cached (%s) ",
                                    remoteAsset);
                            remoteAssetsList.add(remoteAsset);
                        }
                    }
                }
            }
            messagingCacheUtilities.cacheImageAssets(remoteAssetsList);
        } catch (final DataReaderException exception) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to cache image asset, exception occurred %s",
                    exception.getLocalizedMessage());
        }
    }

    @VisibleForTesting
    void setMessagesRequestEventId(
            final String messagesRequestEventId, final List<Surface> surfaceList) {
        requestedSurfacesForEventId.put(messagesRequestEventId, surfaceList);
    }

    @VisibleForTesting
    Map<Surface, List<Proposition>> getInProgressPropositions() {
        return inProgressPropositions;
    }

    @VisibleForTesting
    void setQualifiedContentCardsBySurface(final Map<Surface, List<Proposition>> contentCards) {
        contentCardsBySurface = contentCards;
    }

    @VisibleForTesting
    Map<Surface, List<Proposition>> getQualifiedContentCardsBySurface() {
        return contentCardsBySurface;
    }

    @VisibleForTesting
    java.util.Set<String> getNonRecoverableErrorEventIds() {
        return nonRecoverableErrorEventIds;
    }

    @VisibleForTesting
    java.util.Set<Surface> getNetworkRefreshedSurfaces() {
        return networkRefreshedSurfaces;
    }
}
