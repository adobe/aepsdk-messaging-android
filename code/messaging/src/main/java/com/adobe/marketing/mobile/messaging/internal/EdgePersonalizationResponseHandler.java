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
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_CJM_VALUE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventName.MESSAGE_FEEDS_NOTIFICATION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.SchemaValues.MESSAGE_FEED_SCHEMA_VALUE;

import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.Feed;
import com.adobe.marketing.mobile.FeedItem;
import com.adobe.marketing.mobile.Proposition;
import com.adobe.marketing.mobile.PropositionItem;
import com.adobe.marketing.mobile.Surface;
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
    private final LaunchRulesEngine feedRulesEngine;
    private final Map<String, List<String>> requestedSurfacesForEventId = new HashMap<>();
    private List<Proposition> inMemoryPropositions = new ArrayList<>();
    private Map<String, PropositionInfo> propositionInfo = new HashMap<>();
    private final Map<String, Object> inMemoryFeeds = new HashMap<>();
    private Map<String, PropositionInfo> feedInfo = new HashMap<>();
    private String messagesRequestEventId;
    private String lastProcessedRequestEventId;

    /**
     * Constructor
     *
     * @param parent       {@link MessagingExtension} instance that is the parent of this {@code EdgePersonalizationResponseHandler}
     * @param extensionApi {@link ExtensionApi} instance
     * @param rulesEngine  {@link LaunchRulesEngine} instance to use for loading in-app message rule payloads
     * @param rulesEngine  {@link LaunchRulesEngine} instance to use for loading message feed rule payloads
     */
    EdgePersonalizationResponseHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final LaunchRulesEngine feedRulesEngine) {
        this(parent, extensionApi, rulesEngine, feedRulesEngine, null, null);
    }

    @VisibleForTesting
    EdgePersonalizationResponseHandler(final MessagingExtension parent, final ExtensionApi extensionApi, final LaunchRulesEngine rulesEngine, final LaunchRulesEngine feedRulesEngine, final MessagingCacheUtilities messagingCacheUtilities, final String messagesRequestEventId) {
        this.parent = parent;
        this.extensionApi = extensionApi;
        this.launchRulesEngine = rulesEngine;
        this.feedRulesEngine = feedRulesEngine;
        this.messagesRequestEventId = messagesRequestEventId;

        // load cached propositions (if any) when EdgePersonalizationResponseHandler is instantiated
        this.messagingCacheUtilities = messagingCacheUtilities != null ? messagingCacheUtilities : new MessagingCacheUtilities();
        if (this.messagingCacheUtilities.arePropositionsCached()) {
            final List<Proposition> cachedPropositions = this.messagingCacheUtilities.getCachedPropositions();
            if (cachedPropositions != null && !cachedPropositions.isEmpty()) {
                Log.trace(LOG_TAG, SELF_TAG, "Retrieved cached propositions, attempting to load the propositions into the rules engine.");
                inMemoryPropositions = cachedPropositions;
                final List<LaunchRule> parsedRules = processPropositions(cachedPropositions, Collections.singletonList(getAppSurface()), false, false);
                launchRulesEngine.replaceRules(parsedRules);
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
            final String appSurface = getAppSurface();
            if (appSurface.equals("unknown")) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to update messages, cannot read the application package name.");
                return;
            }
            requestedSurfaceUris.add(appSurface);
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
        messagesRequestEventId = event.getUniqueIdentifier();
        requestedSurfacesForEventId.put(messagesRequestEventId, requestedSurfaceUris);

        // send event
        Log.debug(LOG_TAG, SELF_TAG, "Dispatching an edge event to retrieve in-app or feed message definitions.");
        extensionApi.dispatch(event);
    }

    /**
     * Validates that the edge response event is a response that we are waiting for. If the returned payload is empty then the Messaging cache
     * and any loaded rules in the Messaging extension's {@link LaunchRulesEngine} are cleared.
     * Non-empty payloads are converted into rules within {@link #processPropositions(List, List, boolean, boolean)}.
     *
     * @param edgeResponseEvent A {@link Event} containing the in-app message definitions retrieved via the Edge extension.
     */
    void handleEdgePersonalizationNotification(final Event edgeResponseEvent) {
        final String requestEventId = MessagingUtils.getRequestEventId(edgeResponseEvent);

        // "TESTING_ID" used in unit and functional testing
        if (!messagesRequestEventId.equals(requestEventId) && !"TESTING_ID".equals(requestEventId)) {
            return;
        }

        if ("TESTING_ID".equals(requestEventId)) {
            requestedSurfacesForEventId.put("TESTING_ID", Collections.singletonList(getAppSurface()));
        }

        // if this is an event for a new request, purge cache and update lastProcessedRequestEventId
        boolean clearExistingRules = false;
        if (lastProcessedRequestEventId != requestEventId) {
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

        final List<LaunchRule> parsedRules = processPropositions(propositions, requestedSurfacesForEventId.get(messagesRequestEventId), clearExistingRules, true);
        if (!parsedRules.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG, "Loading %d rule(s) into the rules engine for scope %s.", parsedRules.size(), requestedSurfacesForEventId.get(messagesRequestEventId));
        }

        final List<RuleConsequence> consequenceList = !parsedRules.isEmpty() ? parsedRules.get(0).getConsequenceList() : new ArrayList<>();
        if (!consequenceList.isEmpty() && MessagingUtils.isFeedItem(consequenceList.get(0))) {
            if (clearExistingRules) {
                feedRulesEngine.replaceRules(parsedRules);
            } else {
                feedRulesEngine.addRules(parsedRules);
            }

            final Map<String, Feed> feeds = processFeedConsequences(feedRulesEngine.evaluateEvent(edgeResponseEvent));
            mergeFeedsInMemory(feeds, Objects.requireNonNull(requestedSurfacesForEventId.get(lastProcessedRequestEventId)));
            // dispatch an event with the feeds received from the remote
            final Event event = new Event.Builder(MESSAGE_FEEDS_NOTIFICATION,
                    EventType.MESSAGING, MessagingConstants.EventSource.NOTIFICATION)
                    .setEventData(inMemoryFeeds)
                    .build();

            extensionApi.dispatch(event);
        } else {
            launchRulesEngine.replaceRules(parsedRules);
        }
    }

    private void mergeFeedsInMemory(final Map<String, Feed> feeds, final List<String> requestedSurfaces) {
        for (final String surface : requestedSurfaces) {
            final Feed feed = feeds.get(surface);
            if (feed != null) {
                // convert the feed to a map so it can be sent in event data
                inMemoryFeeds.put(surface, feed.toEventData());
            } else {
                inMemoryFeeds.remove(surface);
            }
        }
    }

    private Map<String, Feed> processFeedConsequences(final List<RuleConsequence> feedConsequences) {
        if (feedConsequences.isEmpty()) {
            Log.debug(LOG_TAG, SELF_TAG, "Will not process feed consequences as none were found in the rules engine.");
            return null;
        }

        final Map<String, Feed> processedFeeds = new HashMap<>();
        for (final RuleConsequence consequence : feedConsequences) {
            final Map details = consequence.getDetail();
            final FeedItem feedItem = createFeedItemFromConsequenceDetail(details);
            final String appSurface = getAppSurface();
            Feed feed = processedFeeds.get(appSurface);
            final List<FeedItem> feedItems = feed == null ? new ArrayList<>() : feed.getItems();
            feedItems.add(feedItem);
            feed = new Feed(appSurface, feedItems);
            processedFeeds.put(appSurface, feed);
        }

        return processedFeeds;
    }

    /**
     * Attempts to parse any in-app message or message feed rules contained in the provided {@code List<Proposition>}. Any valid rule {@code JSONObject}s
     * found will be returned in a {code List<LaunchRule>}.
     *
     * @param propositions     A {@link List<Proposition>} propositions to be processed
     * @param expectedSurfaces A {@link List<String>} containing the expected app surfaces
     * @param clearExisting    {@code boolean} if true the existing cached propositions are cleared and new message rules are replaced in the {@code LaunchRulesEngine}
     * @param persistChanges   {@code boolean} if true the passed in {@code List<Proposition>} are added to the cache
     * @return {@link List<LaunchRule>} containing {@link LaunchRule}s parsed from the passed in propositions
     */
    private List<LaunchRule> processPropositions(final List<Proposition> propositions, final List<String> expectedSurfaces, final boolean clearExisting, final boolean persistChanges) {
        final List<LaunchRule> parsedRules = new ArrayList<>();
        final List<Proposition> tempPropositions = new ArrayList<>();
        final Map<String, PropositionInfo> tempPropositionInfo = new HashMap<>();
        final Map<String, PropositionInfo> tempFeedInfo = new HashMap<>();
        final String appSurface = getAppSurface();
        boolean isFeedConsequence = false;

        if (propositions == null || propositions.isEmpty()) {
            if (clearExisting) {
                if (expectedSurfaces.equals(Collections.singletonList(appSurface))) {
                    inMemoryPropositions.clear();
                    propositionInfo.clear();
                    messagingCacheUtilities.cachePropositions(null);
                } else {
                    inMemoryFeeds.clear();
                    feedInfo.clear();
                }
            }
            return parsedRules;
        }

        for (final Proposition proposition : propositions) {
            final String scope = proposition.getScope();
            if (StringUtils.isNullOrEmpty(scope) || !expectedSurfaces.contains(scope)) {
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

                // store reporting data for this payload for later use
                final PropositionInfo propositionInfo = PropositionInfo.createFromProposition(proposition);
                if (propositionInfo == null) {
                    Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Skipping proposition with invalid tracking data.");
                    continue;
                }

                final Map<String, PropositionInfo> localPropositionInfo = new HashMap<>();
                localPropositionInfo.put(MessagingUtils.getMessageId(ruleJson), propositionInfo);

                final String schemaValue = MessagingUtils.getConsequenceSchema(ruleJson);
                final boolean isMessageFeedConsequence = !StringUtils.isNullOrEmpty(schemaValue) && schemaValue.equals(MESSAGE_FEED_SCHEMA_VALUE);

                if (!isMessageFeedConsequence) {
                    // cache any image assets present in the current rule json's image assets array
                    cacheImageAssetsFromPayload(ruleJson);

                    // store reporting info for this payload
                    tempPropositionInfo.putAll(localPropositionInfo);
                } else { // we have a message feed consequence, persist the feed in memory
                    isFeedConsequence = true;
                    tempFeedInfo.putAll(localPropositionInfo);
                }
                tempPropositions.add(proposition);
                parsedRules.add(parsedRule.get(0));
            }
        }

        // update proposition scope details for in-app messages and cache the messages if needed
        if (!isFeedConsequence && !parsedRules.isEmpty()) {
            if (clearExisting) {
                propositionInfo = tempPropositionInfo;
                inMemoryPropositions = tempPropositions;
            } else {
                propositionInfo.putAll(tempPropositionInfo);
                inMemoryPropositions.addAll(tempPropositions);
            }

            if (persistChanges) {
                messagingCacheUtilities.cachePropositions(inMemoryPropositions);
            }
        } else { // update proposition info for message feed items
            if (clearExisting) {
                inMemoryFeeds.clear();
                feedInfo = tempFeedInfo;
            } else {
                feedInfo.putAll(tempFeedInfo);
            }
        }

        return parsedRules;
    }

    private FeedItem createFeedItemFromConsequenceDetail(final Map<String, Object> consequenceDetailMap) {
        if (MapUtils.isNullOrEmpty(consequenceDetailMap)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create feed item from the consequence details, invalid details map found.");
            return null;
        }

        final Map<String, Object> dataMap = DataReader.optTypedMap(Object.class, consequenceDetailMap, MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA, null);
        if (MapUtils.isNullOrEmpty(dataMap)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create feed item from the consequence details, invalid data map found.");
            return null;
        }

        final Map<String, Object> contentMap = DataReader.optTypedMap(Object.class, dataMap, MessagingConstants.MessageFeedKeys.CONTENT, null);
        if (MapUtils.isNullOrEmpty(contentMap)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to create feed item from the consequence details, invalid content map found.");
            return null;
        }

        final String title = DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.TITLE, "");
        final String body = DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.BODY, "");
        final String imageUrl = DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.IMAGE_URL, "");
        final String actionTitle = DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.ACTION_TITLE, "");
        final String actionUrl = DataReader.optString(contentMap, MessagingConstants.MessageFeedKeys.ACTION_URL, "");

        // cache feed image if one is present
        if (!StringUtils.isNullOrEmpty(imageUrl)) {
            messagingCacheUtilities.cacheImageAssets(Collections.singletonList(imageUrl));
        }

        return new FeedItem.Builder(title, body)
                .setImageUrl(imageUrl)
                .setActionTitle(actionTitle)
                .setActionUrl(actionUrl)
                .build();
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
            final String messageFeedImageUrl = details.optString(MessagingConstants.MessageFeedKeys.IMAGE_URL);
            if (!StringUtils.isNullOrEmpty(messageFeedImageUrl)) {
                remoteAssetsList.add(messageFeedImageUrl);
            } else {
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