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
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.SharedStateResolution;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.EventUtils;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.SerialWorkDispatcher;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public final class MessagingExtension extends Extension {
    private static final String SELF_TAG = "MessagingExtension";

    final EdgePersonalizationResponseHandler edgePersonalizationResponseHandler;
    private boolean initialMessageFetchComplete = false;
    final LaunchRulesEngine messagingRulesEngine;
    final ContentCardRulesEngine contentCardRulesEngine;
    private SerialWorkDispatcher<Event> serialWorkDispatcher;

    @VisibleForTesting static final Object completionHandlersMutex = new Object();
    @VisibleForTesting static List<CompletionHandler> completionHandlers = new ArrayList<>();

    /**
     * Constructor.
     *
     * <p>Called during messaging extension's registration. The following listeners are registered
     * during this extension's registration.
     *
     * <ul>
     *   <li>Listening to event with eventType {@link EventType#GENERIC_IDENTITY} and EventSource
     *       {@link EventSource#REQUEST_CONTENT}
     *   <li>Listening to event with eventType {@link EventType#GENERIC_IDENTITY} and EventSource
     *       {@link EventSource#REQUEST_RESET}
     *   <li>Listening to event with eventType {@link MessagingConstants.EventType#MESSAGING} and
     *       EventSource {@link EventSource#REQUEST_CONTENT}
     *   <li>Listening to event with eventType {@link MessagingConstants.EventType#EDGE} and
     *       EventSource {@link MessagingConstants.EventSource#PERSONALIZATION_DECISIONS}
     *   <li>Listening to event with eventType {@link EventType#WILDCARD} and EventSource {@link
     *       EventSource#WILDCARD}
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    MessagingExtension(final ExtensionApi extensionApi) {
        this(extensionApi, null, null, null);
    }

    @VisibleForTesting
    MessagingExtension(
            final ExtensionApi extensionApi,
            final LaunchRulesEngine messagingRulesEngine,
            final ContentCardRulesEngine contentCardRulesEngine,
            final EdgePersonalizationResponseHandler edgePersonalizationResponseHandler) {
        super(extensionApi);
        this.messagingRulesEngine =
                messagingRulesEngine != null
                        ? messagingRulesEngine
                        : new LaunchRulesEngine(MessagingConstants.RULES_ENGINE_NAME, extensionApi);
        this.contentCardRulesEngine =
                contentCardRulesEngine != null
                        ? contentCardRulesEngine
                        : new ContentCardRulesEngine(
                                MessagingConstants.CONTENT_CARD_RULES_ENGINE_NAME, extensionApi);
        this.edgePersonalizationResponseHandler =
                edgePersonalizationResponseHandler != null
                        ? edgePersonalizationResponseHandler
                        : new EdgePersonalizationResponseHandler(
                                this,
                                extensionApi,
                                this.messagingRulesEngine,
                                this.contentCardRulesEngine);
    }

    // region Extension interface methods

    /**
     * Overridden method of {@link Extension} class to provide a valid extension name to register
     * with eventHub.
     *
     * @return A {@link String} extension name for Messaging
     */
    @NonNull @Override
    protected String getName() {
        return MessagingConstants.EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide a friendly extension name.
     *
     * @return A {@link String} friendly extension name for Messaging
     */
    @NonNull @Override
    protected String getFriendlyName() {
        return MessagingConstants.FRIENDLY_EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide the extension version.
     *
     * @return A {@link String} representing the extension version
     */
    @NonNull @Override
    protected String getVersion() {
        return Messaging.extensionVersion();
    }

    @Override
    protected void onRegistered() {
        super.onRegistered();
        getApi().registerEventListener(
                        EventType.GENERIC_IDENTITY,
                        EventSource.REQUEST_CONTENT,
                        this::processEvent);
        getApi().registerEventListener(
                        EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET, this::processEvent);
        getApi().registerEventListener(
                        MessagingConstants.EventType.MESSAGING,
                        EventSource.REQUEST_CONTENT,
                        this::processEvent);
        getApi().registerEventListener(
                        EventType.EDGE,
                        MessagingConstants.EventSource.PERSONALIZATION_DECISIONS,
                        this::processEvent);
        getApi().registerEventListener(
                        EventType.WILDCARD, EventSource.WILDCARD, this::handleWildcardEvents);
        getApi().registerEventListener(
                        EventType.RULES_ENGINE,
                        EventSource.RESPONSE_CONTENT,
                        this::handleRuleEngineResponseEvents);
        getApi().registerEventListener(
                        EventType.MESSAGING, EventSource.CONTENT_COMPLETE, this::processEvent);
        getApi().registerEventListener(
                        MessagingConstants.EventType.MESSAGING,
                        MessagingConstants.EventSource.EVENT_HISTORY_WRITE,
                        this::processEvent);

        // register listener for handling debug events
        getApi().registerEventListener(EventType.SYSTEM, EventSource.DEBUG, this::handleDebugEvent);

        // Handler function called for each queued event. If the queued event is a get propositions
        // event, process it
        // otherwise if it is an Edge event to update propositions, process it only if it is
        // completed.
        if (serialWorkDispatcher == null) {
            serialWorkDispatcher =
                    new SerialWorkDispatcher<>(
                            "MessagingEvents",
                            event -> {
                                if (InternalMessagingUtils.isGetPropositionsEvent(event)) {
                                    edgePersonalizationResponseHandler.retrieveInMemoryPropositions(
                                            InternalMessagingUtils.getSurfaces(event), event);
                                } else if (event.getType().equals(EventType.EDGE)) {
                                    return !edgePersonalizationResponseHandler
                                            .getRequestedSurfacesForEventId()
                                            .containsKey(event.getUniqueIdentifier());
                                }
                                return true;
                            });
        }

        edgePersonalizationResponseHandler.setSerialWorkDispatcher(serialWorkDispatcher);
        serialWorkDispatcher.start();

        // retrieve the push token from the messaging named collection and add it to the messaging
        // shared state. this must be done as its not guaranteed that the push token will be synced
        // (and the shared state updated) on an app launch.
        final String existingPushToken = InternalMessagingUtils.getPushTokenFromPersistence();
        createMessagingSharedState(existingPushToken, null);
    }

    @Override
    protected void onUnregistered() {}

    @Override
    public boolean readyForEvent(@NonNull final Event event) {
        if (!hasValidSharedState(
                MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Event processing is paused - waiting for valid Configuration");
            return false;
        }

        if (!hasValidXdmSharedState(
                MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Event processing is paused - waiting for valid XDM shared state from Edge"
                            + " Identity extension.");
            return false;
        }

        // fetch propositions on initial launch once we have configuration and identity state set
        if (!initialMessageFetchComplete) {
            edgePersonalizationResponseHandler.fetchPropositions(event, null);
            initialMessageFetchComplete = true;
        }

        return true;
    }

    // endregion

    // region Event listeners

    /**
     * Processes debug events triggered by the system. A debug event allows the messaging extension
     * to processes non-production workflows.
     *
     * @param event the debug `Event` to be handled.
     */
    void handleDebugEvent(final Event event) {
        // handle rules consequence debug events
        if (EventType.RULES_ENGINE.equals(EventUtils.getDebugEventType(event))
                && EventSource.RESPONSE_CONTENT.equals(EventUtils.getDebugEventSource(event))) {
            if (!InternalMessagingUtils.isSchemaConsequence(event)
                    || MapUtils.isNullOrEmpty(event.getEventData())) {
                Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "handleDebugEvent - Ignoring rule consequence event. Either consequence is"
                                + " not of type 'schema' or 'eventData' is nil.");
                return;
            }

            final PropositionItem tempPropositionItem =
                    PropositionItem.fromSchemaConsequenceEvent(event);
            if (tempPropositionItem == null) {
                Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "handleDebugEvent -  Ignoring rule consequence event, could not create"
                                + " propositionItem");
                return;
            }
            switch (tempPropositionItem.getSchema()) {
                case INAPP:
                    edgePersonalizationResponseHandler.createInAppMessage(tempPropositionItem);
                    break;
                default:
            }
        }
    }

    // Called on every event, used to allow processing of the Messaging rules engine
    @SuppressWarnings("NestedIfDepth")
    void handleWildcardEvents(final Event event) {
        // handling mock rules delivered from the assurance ui
        final String eventName = event.getName();
        if (!StringUtils.isNullOrEmpty(eventName)
                && eventName.equals(
                        MessagingConstants.EventName.ASSURANCE_SPOOFED_IAM_EVENT_NAME)) {
            if (!InternalMessagingUtils.isSchemaConsequence(event)) {
                Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "handleWildcardEvents - Ignoring rule consequence event(spoof),"
                                + " consequence is not of type 'schema'");
                return;
            }
            edgePersonalizationResponseHandler.createInAppMessage(
                    PropositionItem.fromSchemaConsequenceEvent(event));
            return;
        }
        messagingRulesEngine.processEvent(event);
        edgePersonalizationResponseHandler.updateQualifiedContentCardsForEvent(event);
    }

    /**
     * Handles Rule Engine Response Content events which are dispatched when a event matches a rule
     * in the Messaging {@link LaunchRulesEngine}. The {@link EdgePersonalizationResponseHandler}
     * will then attempt to show a {@link
     * com.adobe.marketing.mobile.services.ui.Presentable<com.adobe.marketing.mobile.services.ui.InAppMessage>}
     * created from the triggered rule consequence payload.
     *
     * @param event incoming {@link Event} object to be processed
     */
    void handleRuleEngineResponseEvents(final Event event) {
        if (!InternalMessagingUtils.isSchemaConsequence(event)) {
            Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "handleRuleEngineResponseEvents - Ignoring rule response event,"
                            + " consequence is not of type 'schema'");
            return;
        }
        final PropositionItem propositionItem = PropositionItem.fromSchemaConsequenceEvent(event);
        if (propositionItem == null) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "handleSchemaConsequence -  Ignoring rule consequence event, propositionItem is"
                            + " null");
            return;
        }
        switch (propositionItem.getSchema()) {
            case INAPP:
                edgePersonalizationResponseHandler.createInAppMessage(propositionItem);
                break;
            case EVENT_HISTORY_OPERATION:
                edgePersonalizationResponseHandler.handleEventHistoryRuleConsequence(
                        propositionItem);
                break;
            default:
        }
    }

    // endregion

    // region package-protected methods

    /**
     * Validates the passed in event and triggers additional processing based on the event type.
     *
     * @param eventToProcess an {@link Event} from an {@link ExtensionEventListener} to be processed
     */
    void processEvent(final Event eventToProcess) {
        if (!eventIsValid(eventToProcess)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Invalid event, ignoring.");
            return;
        }

        // validate refresh messages event then fetch in-app messages via an Edge extension event
        if (InternalMessagingUtils.isRefreshMessagesEvent(eventToProcess)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Processing manual request to refresh In-App Message definitions from the"
                            + " remote.");
            edgePersonalizationResponseHandler.fetchPropositions(eventToProcess, null);
        } else if (InternalMessagingUtils.isUpdatePropositionsEvent(eventToProcess)) {
            // validate update propositions event then retrieve propositions via an Edge extension
            // event
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Processing request to retrieve propositions from the remote.");
            edgePersonalizationResponseHandler.fetchPropositions(
                    eventToProcess, InternalMessagingUtils.getSurfaces(eventToProcess));
        } else if (InternalMessagingUtils.isGetPropositionsEvent(eventToProcess)) {
            // Queue the get propositions event in the
            // edgePersonalizationResponseHandler.serialWorkDispatcher to ensure any prior update
            // requests are completed
            // before it is processed.
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Processing request to get cached proposition content.");
            serialWorkDispatcher.offer(eventToProcess);
        } else if (InternalMessagingUtils.isTrackingPropositionsEvent(eventToProcess)) {
            // handle an event to track propositions
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Processing request to track propositions.");
            trackMessages(eventToProcess);
        } else if (InternalMessagingUtils.isGenericIdentityRequestEvent(eventToProcess)) {
            // handle the push token from generic identity request content event
            handlePushToken(eventToProcess);
        } else if (InternalMessagingUtils.isGenericIdentityResetEvent(eventToProcess)) {
            // handle the reset identities event
            handleResetIdentitiesEvent(eventToProcess);
        } else if (InternalMessagingUtils.isMessagingRequestContentEvent(eventToProcess)) {
            // need experience event dataset id for sending the push token
            final Map<String, Object> configSharedState =
                    getSharedState(
                            MessagingConstants.SharedState.Configuration.EXTENSION_NAME,
                            eventToProcess);
            final String experienceEventDatasetId =
                    DataReader.optString(
                            configSharedState,
                            MessagingConstants.SharedState.Configuration
                                    .EXPERIENCE_EVENT_DATASET_ID,
                            "");
            if (StringUtils.isNullOrEmpty(experienceEventDatasetId)) {
                InternalMessagingUtils.sendTrackingResponseEvent(
                        PushTrackingStatus.NO_DATASET_CONFIGURED, getApi(), eventToProcess);
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Unable to track push notification interaction, experience event dataset id"
                                + " is empty. Check the messaging launch extension to add the"
                                + " experience event dataset.");
                return;
            }
            // handle the push tracking information from messaging request content event
            handleTrackingInfo(eventToProcess, experienceEventDatasetId);
        } else if (InternalMessagingUtils.isEdgePersonalizationDecisionEvent(eventToProcess)) {
            // validate the edge response event then load any iam rules present
            edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(
                    eventToProcess);
        } else if (InternalMessagingUtils.isPersonalizationRequestCompleteEvent(eventToProcess)) {
            // validate the personalization request complete event then process the personalization
            // request data
            edgePersonalizationResponseHandler.handleProcessCompletedEvent(eventToProcess);
        }
    }

    /**
     * Generates and dispatches an event prompting the Edge extension to send a proposition
     * interactions tracking event.
     *
     * @param event A {@link Event} request event containing proposition interaction XDM data
     */
    void trackMessages(final Event event) {
        final Map<String, Object> propositionInteractionXdm =
                DataReader.optTypedMap(
                        Object.class,
                        event.getEventData(),
                        MessagingConstants.EventDataKeys.Messaging.PROPOSITION_INTERACTION,
                        new HashMap<>());
        if (MapUtils.isNullOrEmpty(propositionInteractionXdm)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Cannot track proposition item, proposition interaction XDM is not available.");
            return;
        }
        sendPropositionInteraction(propositionInteractionXdm);
    }

    void handlePushToken(final Event event) {
        final String pushToken =
                DataReader.optString(
                        event.getEventData(),
                        MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER,
                        null);

        if (StringUtils.isNullOrEmpty(pushToken)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to sync push token, token is null or empty.");
            return;
        }

        // if the push token hasn't changed or the push registration sync optimization flag is true
        // then there is no need to sync it via an edge network request
        final Map<String, Object> configSharedState =
                getSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event);

        if (!InternalMessagingUtils.shouldSyncPushToken(
                configSharedState, pushToken, event.getTimestamp())) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Skipping the push token sync and shared state update.");
            return;
        }

        dispatchPushTokenSyncEdgeEvent(pushToken, event);
    }

    /**
     * Handles the reset identities event by setting a null push token in the Messaging shared state
     * and by removing the persisted push token from the Messaging {@link
     * com.adobe.marketing.mobile.services.NamedCollection}
     *
     * @param resetIdentitiesEvent the reset identities {@link Event}
     */
    private void handleResetIdentitiesEvent(final Event resetIdentitiesEvent) {
        Log.debug(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Clearing the push token from persistence and the Messaging shared state.");
        // remove the push token from the shared state
        createMessagingSharedState(null, resetIdentitiesEvent);
        // remove the push token from the named collection
        InternalMessagingUtils.persistPushToken(null);
    }

    /**
     * Dispatches an edge event to sync the push token with the Edge network.
     *
     * @param pushToken {@link String} containing the push token
     * @param event {@link Event} containing the event that triggered this method
     */
    private void dispatchPushTokenSyncEdgeEvent(final String pushToken, final Event event) {
        final Map<String, Object> edgeIdentitySharedState =
                getXDMSharedState(
                        MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event);
        final String ecid = InternalMessagingUtils.getSharedStateEcid(edgeIdentitySharedState);
        if (StringUtils.isNullOrEmpty(ecid)) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to sync the push token. ECID is unavailable for the user.");
            return;
        }

        final Map<String, Object> eventData = getProfileEventData(pushToken, ecid);
        if (eventData == null) {
            return;
        }

        // Update the push token to the shared state
        final HashMap<String, Object> messagingSharedState = new HashMap<>();
        messagingSharedState.put(
                MessagingConstants.SharedState.Messaging.PUSH_IDENTIFIER, pushToken);
        getApi().createSharedState(messagingSharedState, event);

        // Send an edge event with profile data as event data
        InternalMessagingUtils.sendEvent(
                MessagingConstants.EventName.PUSH_PROFILE_EDGE_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                eventData,
                getApi(),
                event);
    }

    /**
     * Handles the push tracking information from the messaging request content event.
     *
     * <p>The push tracking information is sent to the platform via configured dataset.
     *
     * @param event {@link Event} containing the push tracking information
     * @param datasetId A valid {@link String} containing the dataset id
     */
    private void handleTrackingInfo(final Event event, final String datasetId) {
        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null) {
            InternalMessagingUtils.sendTrackingResponseEvent(
                    PushTrackingStatus.UNKNOWN_ERROR, getApi(), event);
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to track push notification interaction, eventData is null.");
            return;
        }
        final String eventType =
                DataReader.optString(
                        eventData,
                        MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE,
                        "");
        final String messageId =
                DataReader.optString(
                        eventData,
                        MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID,
                        "");
        final boolean isApplicationOpened =
                DataReader.optBoolean(
                        eventData,
                        MessagingConstants.EventDataKeys.Messaging
                                .TRACK_INFO_KEY_APPLICATION_OPENED,
                        false);
        final String actionId =
                DataReader.optString(
                        eventData,
                        MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID,
                        null);

        if (StringUtils.isNullOrEmpty(eventType)) {
            InternalMessagingUtils.sendTrackingResponseEvent(
                    PushTrackingStatus.UNKNOWN_ERROR, getApi(), event);
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to track push notification interaction, eventType is either null or"
                            + " empty.");
            return;
        }

        if (StringUtils.isNullOrEmpty(messageId)) {
            InternalMessagingUtils.sendTrackingResponseEvent(
                    PushTrackingStatus.INVALID_MESSAGE_ID, getApi(), event);
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to track push notification interaction, messageId is either null or"
                            + " empty.");
            return;
        }

        // Creating the Meta Map
        final Map<String, Object> metaMap = new HashMap<>();
        final Map<String, Object> collectMap = new HashMap<>();
        collectMap.put(MessagingConstants.TrackingKeys.DATASET_ID, datasetId);
        metaMap.put(MessagingConstants.TrackingKeys.COLLECT, collectMap);

        // Create XDM data with tracking data
        final Map<String, Object> xdmMap = getXdmData(eventType, messageId, actionId);

        // Adding application data to xdmMap
        addApplicationData(isApplicationOpened, xdmMap);

        // Adding xdm data to xdmMap
        addXDMData(eventData, xdmMap);

        // Add propositionEventType if exdRequestID exists in decisioning data
        addPropositionEventType(xdmMap, isApplicationOpened, actionId);

        final Map<String, Object> xdmData = new HashMap<>();
        xdmData.put(MessagingConstants.TrackingKeys.XDM, xdmMap);
        xdmData.put(MessagingConstants.TrackingKeys.META, metaMap);

        InternalMessagingUtils.sendTrackingResponseEvent(
                PushTrackingStatus.TRACKING_INITIATED, getApi(), event);

        // dispatch push tracking event
        InternalMessagingUtils.sendEvent(
                MessagingConstants.EventName.PUSH_TRACKING_EDGE_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                xdmData,
                getApi(),
                event);
    }

    /**
     * Sends a proposition interaction to the customer's experience event dataset.
     *
     * @param xdmMap {@code Map<String, Object>} containing the proposition interaction XDM.
     */
    public void sendPropositionInteraction(final Map<String, Object> xdmMap) {
        final Map<String, Object> xdmEventData = new HashMap<>();
        xdmEventData.put(MessagingConstants.TrackingKeys.XDM, xdmMap);

        // dispatch in-app tracking event
        InternalMessagingUtils.sendEvent(
                MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                xdmEventData,
                getApi(),
                null);
    }

    public static void addCompletionHandler(final CompletionHandler handler) {
        synchronized (completionHandlersMutex) {
            completionHandlers.add(handler);
        }
    }

    @Nullable CompletionHandler completionHandlerForOriginatingEventId(final String id) {
        synchronized (completionHandlersMutex) {
            final ArrayList<CompletionHandler> handlersCopy =
                    new ArrayList<>(MessagingExtension.completionHandlers);
            for (final CompletionHandler handler : handlersCopy) {
                if (handler.originatingEventId.equals(id)) {
                    MessagingExtension.completionHandlers.remove(handler);
                    return handler;
                }
            }
            return null;
        }
    }

    @Nullable CompletionHandler completionHandlerForEdgeRequestEventId(final String id) {
        synchronized (completionHandlersMutex) {
            final ArrayList<CompletionHandler> handlersCopy =
                    new ArrayList<>(MessagingExtension.completionHandlers);
            for (final CompletionHandler handler : handlersCopy) {
                if (handler.edgeRequestEventId.equals(id)) {
                    MessagingExtension.completionHandlers.remove(handler);
                    return handler;
                }
            }
            return null;
        }
    }

    // endregion

    // region private methods

    /**
     * Get profile data with token
     *
     * @param token push token which needs to be synced
     * @param ecid experience cloud id of the device
     * @return {@link Map} of profile data in the correct format with token
     */
    private Map<String, Object> getProfileEventData(final String token, final String ecid) {
        if (ecid == null) {
            Log.error(
                    MessagingConstants.LOG_TAG,
                    MessagingExtension.SELF_TAG,
                    "Failed to sync push token, ECID is null.");
            return null;
        }

        final Map<String, String> namespace = new HashMap<>();
        namespace.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.CODE,
                MessagingConstants.JsonValues.ECID);

        final Map<String, Object> identity = new HashMap<>();
        identity.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys
                        .NAMESPACE,
                namespace);
        identity.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.ID,
                ecid);

        final ArrayList<Map<String, Object>> pushNotificationDetailsArray = new ArrayList<>();
        final Map<String, Object> pushNotificationDetailsData = new HashMap<>();
        pushNotificationDetailsData.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.IDENTITY,
                identity);
        pushNotificationDetailsData.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.APP_ID,
                ServiceProvider.getInstance().getDeviceInfoService().getApplicationPackageName());
        pushNotificationDetailsData.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.TOKEN,
                token);
        pushNotificationDetailsData.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PLATFORM,
                MessagingConstants.JsonValues.FCM);
        pushNotificationDetailsData.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys
                        .DENY_LISTED,
                false);

        pushNotificationDetailsArray.add(pushNotificationDetailsData);

        final Map<String, Object> data = new HashMap<>();
        data.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys
                        .PUSH_NOTIFICATION_DETAILS,
                pushNotificationDetailsArray);

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(
                MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA,
                data);

        return eventData;
    }

    /**
     * Builds the xdmMap with the tracking information provided by the customer in eventData.
     *
     * @param eventType String eventType can be either applicationOpened or customAction
     * @param messageId String messageId for the push notification provided by the customer
     * @param actionId String indicating the actionId of the action taken by the user on the push
     *     notification
     * @return {@link Map} object containing the xdm formatted data
     */
    private Map<String, Object> getXdmData(
            final String eventType, final String messageId, final String actionId) {
        final Map<String, Object> xdmMap = new HashMap<>();
        final Map<String, Object> trackingMap = new HashMap<>();
        final Map<String, Object> customActionMap = new HashMap<>();

        if (actionId != null) {
            customActionMap.put(XDMDataKeys.ACTION_ID, actionId);
            trackingMap.put(XDMDataKeys.CUSTOM_ACTION, customActionMap);
        }

        trackingMap.put(XDMDataKeys.PUSH_PROVIDER, MessagingConstants.JsonValues.FCM);
        trackingMap.put(XDMDataKeys.PUSH_PROVIDER_MESSAGE_ID, messageId);
        xdmMap.put(XDMDataKeys.EVENT_TYPE, eventType);
        xdmMap.put(XDMDataKeys.PUSH_NOTIFICATION_TRACKING_MIXIN_NAME, trackingMap);

        return xdmMap;
    }

    private void addApplicationData(
            final boolean applicationOpened, final Map<String, Object> xdmMap) {
        final Map<String, Object> applicationMap = new HashMap<>();
        final Map<String, Object> launchesMap = new HashMap<>();
        launchesMap.put(MessagingConstants.TrackingKeys.LAUNCHES_VALUE, applicationOpened ? 1 : 0);
        applicationMap.put(MessagingConstants.TrackingKeys.LAUNCHES, launchesMap);
        xdmMap.put(MessagingConstants.TrackingKeys.APPLICATION, applicationMap);
    }

    /**
     * Adding XDM specific data to tracking information.
     *
     * @param eventData eventData map which contains the xdm data forwarded by the customer.
     * @param xdmMap xdmMap map which is updated.
     */
    private void addXDMData(final Map<String, Object> eventData, final Map<String, Object> xdmMap) {
        // Extract the xdm adobe data string from the event data.
        final String adobe =
                DataReader.optString(
                        eventData,
                        MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM,
                        "");
        if (StringUtils.isNullOrEmpty(adobe)) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to send Adobe data with the tracking data, Adobe XDM data is null.");
            return;
        }

        try {
            // Convert the adobe string to json object
            final JSONObject xdmJson = new JSONObject(adobe);
            final Map<String, Object> xdmMapObject = JSONUtils.toMap(xdmJson);

            if (xdmMapObject == null) {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Failed to send Adobe data with the tracking data, Adobe XDM data"
                                + " conversion to map failed.");
                return;
            }

            Map<String, Object> mixins = null;

            // Check for if the json has the required keys
            if (xdmMapObject.containsKey(MessagingConstants.TrackingKeys.CJM)
                    && xdmMapObject.get(MessagingConstants.TrackingKeys.CJM) instanceof Map) {
                mixins =
                        (Map<String, Object>) xdmMapObject.get(MessagingConstants.TrackingKeys.CJM);
            }

            if (xdmMapObject.containsKey(MessagingConstants.TrackingKeys.MIXINS)
                    && xdmMapObject.get(MessagingConstants.TrackingKeys.MIXINS) instanceof Map) {
                mixins =
                        (Map<String, Object>)
                                xdmMapObject.get(MessagingConstants.TrackingKeys.MIXINS);
            }

            if (mixins == null) {
                Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Failed to send cjm xdm data with the tracking, Missing XDM data.");
                return;
            }

            xdmMap.putAll(mixins);

            // Check if the xdm data provided by the customer is using cjm for tracking
            // Check if both {@link MessagingConstants#EXPERIENCE} and {@link
            // MessagingConstants#CUSTOMER_JOURNEY_MANAGEMENT} exists
            if (mixins.containsKey(MessagingConstants.TrackingKeys.EXPERIENCE)
                    && mixins.get(MessagingConstants.TrackingKeys.EXPERIENCE) instanceof Map) {
                Map<String, Object> experience =
                        (Map<String, Object>)
                                mixins.get(MessagingConstants.TrackingKeys.EXPERIENCE);
                if (experience.containsKey(
                                MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT)
                        && experience.get(
                                        MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT)
                                instanceof Map) {
                    Map<String, Object> cjm =
                            (Map<String, Object>)
                                    experience.get(
                                            MessagingConstants.TrackingKeys
                                                    .CUSTOMER_JOURNEY_MANAGEMENT);
                    // Adding Message profile and push channel context to
                    // CUSTOMER_JOURNEY_MANAGEMENT
                    final JSONObject jObject =
                            new JSONObject(MessagingConstants.TrackingKeys.MESSAGE_PROFILE_JSON);
                    cjm.putAll(JSONUtils.toMap(jObject));

                    experience.put(
                            MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT, cjm);
                    xdmMap.put(MessagingConstants.TrackingKeys.EXPERIENCE, experience);
                }
            } else {
                Log.warning(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "Failed to send CJM XDM data with the tracking, required keys are"
                                + " missing.");
            }
        } catch (final JSONException | ClassCastException e) {
            Log.warning(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to send Adobe data with the tracking data, Adobe data is malformed :"
                            + " %s",
                    e.getMessage());
        }
    }

    /**
     * Adds propositionEventType to the decisioning map if exdRequestID exists.
     *
     * @param xdmMap the XDM map to update
     * @param isApplicationOpened boolean indicating if the application was opened
     * @param actionId the action ID if a custom action was triggered
     */
    @SuppressWarnings("unchecked")
    private void addPropositionEventType(
            final Map<String, Object> xdmMap,
            final boolean isApplicationOpened,
            final String actionId) {
        try {
            // Check if _experience exists
            if (!xdmMap.containsKey(MessagingConstants.TrackingKeys.EXPERIENCE)) {
                return;
            }
            final Object experienceObj = xdmMap.get(MessagingConstants.TrackingKeys.EXPERIENCE);
            if (!(experienceObj instanceof Map)) {
                return;
            }
            final Map<String, Object> experience = (Map<String, Object>) experienceObj;

            // Check if decisioning exists
            if (!experience.containsKey(
                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key.DECISIONING)) {
                return;
            }
            final Object decisioningObj =
                    experience.get(
                            MessagingConstants.EventDataKeys.Messaging.Inbound.Key.DECISIONING);
            if (!(decisioningObj instanceof Map)) {
                return;
            }
            final Map<String, Object> decisioning = (Map<String, Object>) decisioningObj;

            // Check if exdRequestID exists
            if (!decisioning.containsKey(MessagingConstants.TrackingKeys.EXD_REQUEST_ID)) {
                return;
            }

            // Add propositionEventType based on the action type
            final Map<String, Object> propositionEventType = new HashMap<>();
            if (!isApplicationOpened) {
                // Dismiss action
                propositionEventType.put(MessagingConstants.TrackingKeys.DISMISS, 1);
            } else if (actionId != null) {
                // Button click action
                propositionEventType.put(MessagingConstants.TrackingKeys.INTERACT, 1);
            } else {
                // Application opened
                propositionEventType.put(MessagingConstants.TrackingKeys.OPEN, 1);
            }

            decisioning.put(
                    MessagingConstants.EventDataKeys.Messaging.Inbound.Key.PROPOSITION_EVENT_TYPE,
                    propositionEventType);
        } catch (final ClassCastException e) {
            Log.debug(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to add propositionEventType, error: %s",
                    e.getMessage());
        }
    }

    private boolean hasValidSharedState(final String extensionName, final Event event) {
        final SharedStateResult result =
                getApi().getSharedState(
                                extensionName, event, false, SharedStateResolution.LAST_SET);
        if (result == null) {
            return false;
        }
        final Map<String, Object> sharedState = result.getValue();
        return sharedState != null && !sharedState.isEmpty();
    }

    private boolean hasValidXdmSharedState(final String extensionName, final Event event) {
        final SharedStateResult result =
                getApi().getXDMSharedState(
                                extensionName, event, false, SharedStateResolution.LAST_SET);
        if (result == null) {
            return false;
        }
        final Map<String, Object> sharedState = result.getValue();
        return sharedState != null && !sharedState.isEmpty();
    }

    private Map<String, Object> getSharedState(final String extensionName, final Event event) {
        final SharedStateResult result =
                getApi().getSharedState(
                                extensionName, event, false, SharedStateResolution.LAST_SET);
        return result == null ? null : result.getValue();
    }

    private Map<String, Object> getXDMSharedState(final String extensionName, final Event event) {
        final SharedStateResult result =
                getApi().getXDMSharedState(
                                extensionName, event, false, SharedStateResolution.LAST_SET);
        return result == null ? null : result.getValue();
    }

    private boolean eventIsValid(final Event event) {
        return (event != null && event.getEventData() != null) || isRequestResetEvent(event);
    }

    private boolean isRequestResetEvent(final Event event) {
        return event != null && event.getSource().equals(EventSource.REQUEST_RESET);
    }

    private void createMessagingSharedState(final String pushToken, final Event event) {
        final Map<String, Object> newMessagingState = new HashMap<>();
        if (!StringUtils.isNullOrEmpty(pushToken)) {
            newMessagingState.put(
                    MessagingConstants.SharedState.Messaging.PUSH_IDENTIFIER, pushToken);
        }
        getApi().createSharedState(newMessagingState, event);
    }

    // endregion

    @VisibleForTesting
    SerialWorkDispatcher<Event> getSerialWorkDispatcher() {
        return serialWorkDispatcher;
    }

    @VisibleForTesting
    void setSerialWorkDispatcher(final SerialWorkDispatcher<Event> serialWorkDispatcher) {
        this.serialWorkDispatcher = serialWorkDispatcher;
    }
}
