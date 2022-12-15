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

import static com.adobe.marketing.mobile.messaging.MessagingConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.DECISIONING;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.LABEL;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITION_ACTION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SCOPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SCOPE_DETAILS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.APP_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.CODE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DENY_LISTED;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.IDENTITY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.NAMESPACE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PLATFORM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PUSH_NOTIFICATION_DETAILS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.TOKEN;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.FRIENDLY_EXTENSION_NAME;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.JsonValues.ECID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.JsonValues.FCM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.CJM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.COLLECT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.DATASET_ID;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.EXPERIENCE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.MESSAGE_PROFILE_JSON;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.META;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.MIXINS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.TrackingKeys.XDM;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITION_EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITIONS;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.IAM_HISTORY;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.TRACKING_ACTION;
import static com.adobe.marketing.mobile.messaging.MessagingConstants.EventMask.Keys.MESSAGE_ID;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.messaging.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.EventSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MessagingExtension extends Extension {
    final static String SELF_TAG = "MessagingExtension";
    private final Object executorMutex = new Object();
    private final ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
    private final InAppNotificationHandler inAppNotificationHandler;
    private ExecutorService executorService;
    private boolean initialMessageFetchComplete = false;

    /**
     * Constructor.
     *
     * <p>
     * Called during messaging extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> Listening to event with eventType {@link EventType#HUB}
     *           and EventSource {@link EventSource#SHARED_STATE}</li>
     *     <li> Listening to event with eventType {@link MessagingConstants.EventType#MESSAGING}
     *          and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> Listening to event with eventType {@link EventType#GENERIC_IDENTITY}
     * 	        and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> Listening to event with eventType {@link MessagingConstants.EventType#EDGE}
     * 	        and EventSource {@link MessagingConstants.EventSource#PERSONALIZATION_DECISIONS}</li>
     *      <li> Listening to event with eventType {@link EventType#RULES_ENGINE}
     * 	        and EventSource {@link EventSource#RESPONSE_CONTENT}</li>
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    MessagingExtension(final ExtensionApi extensionApi) {
        super(extensionApi);

        // initialize the in-app notification handler and check if we have any cached propositions. if we do, load them.
        this.inAppNotificationHandler = new InAppNotificationHandler(this);
    }

    //region Extension interface methods

    /**
     * Overridden method of {@link Extension} class to provide a valid extension name to register with eventHub.
     *
     * @return A {@link String} extension name for Messaging
     */
    @NonNull @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide a friendly extension name.
     *
     * @return A {@link String} friendly extension name for Messaging
     */
    @NonNull @Override
    protected String getFriendlyName() {
        return FRIENDLY_EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide the extension version.
     *
     * @return A {@link String} representing the extension version
     */
    @NonNull @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }

    @Override
    protected void onRegistered() {
        getApi().registerEventListener(EventType.HUB, EventSource.SHARED_STATE, this::handleSharedStateEvent);
        getApi().registerEventListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, this::queueAndProcessEvent);
        getApi().registerEventListener(MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT, this::queueAndProcessEvent);
        getApi().registerEventListener(EventType.EDGE, MessagingConstants.EventSource.PERSONALIZATION_DECISIONS, this::queueAndProcessEvent);
        getApi().registerEventListener(EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT, this::queueAndProcessEvent);
    }

    @Override
    protected void onUnregistered() {}

    @Override
    public boolean readyForEvent(@NonNull final Event event) {
        if (!hasValidSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event)) {
            Log.trace(LOG_TAG, SELF_TAG, "Event processing is paused - waiting for valid Configuration");
            return false;
        }

        if (!hasValidSharedState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
            Log.trace(LOG_TAG, SELF_TAG, "Event processing is paused - waiting for valid XDM shared state from Edge Identity extension.");
            return false;
        }

        // fetch in-app messages on initial launch once we have configuration and identity state set
        if (!initialMessageFetchComplete) {
            inAppNotificationHandler.fetchMessages();
            initialMessageFetchComplete = true;
        }

        return true;
    }

    //endregion

    //region Event listeners

    void handleSharedStateEvent(final Event event) {
        if (!eventIsValid(event)) {
            Log.debug(LOG_TAG, SELF_TAG, "Event or EventData is null, ignoring the event.");
            return;
        }

        processHubSharedState(event);
    }

    void queueAndProcessEvent(final Event event) {
        if (!eventIsValid(event)) {
            Log.debug(LOG_TAG, SELF_TAG, "Event or EventData is null, ignoring the event.");
            return;
        }

        queueEvent(event);
        processEvents();
    }

    //endregion

    //region package-protected methods

    /**
     * This method queues the provided event in {@link #eventQueue}.
     *
     * <p>
     * The queued events are then processed in an orderly fashion.
     * No action is taken if the provided event's value is null.
     *
     * @param event The {@link Event} thats needs to be queued
     */
    void queueEvent(final Event event) {
        if (event == null) {
            return;
        }

        eventQueue.add(event);
    }

    /**
     * Processes the queued event one by one until queue is empty.
     *
     * <p>
     * Suspends processing of the events in the queue if the configuration or identity shared state is not ready.
     * Processed events are polled out of the {@link #eventQueue}.
     */
    void processEvents() {
        while (!eventQueue.isEmpty()) {
            Event eventToProcess = eventQueue.peek();

            if (eventToProcess == null) {
                Log.debug(LOG_TAG, SELF_TAG, "Unable to process event, Event received is null.");
                return;
            }

            final Map<String, Object> configSharedState = getSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, eventToProcess);
            final Map<String, Object> edgeIdentitySharedState = getXDMSharedState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, eventToProcess);

            // NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (MessagingUtils.isMapNullOrEmpty(configSharedState)) {
                Log.warning(LOG_TAG, SELF_TAG, "Could not process event, configuration shared state is pending");
                return;
            }

            // NOTE: identity is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (MessagingUtils.isMapNullOrEmpty(edgeIdentitySharedState)) {
                Log.warning(LOG_TAG, SELF_TAG, "Could not process event, identity shared state is pending");
                return;
            }

            // validate fetch messages event then refresh in-app messages via an Edge extension event
            if (MessagingUtils.isFetchMessagesEvent(eventToProcess)) {
                inAppNotificationHandler.fetchMessages();
            } else if (MessagingUtils.isGenericIdentityRequestEvent(eventToProcess)) {
                // handle the push token from generic identity request content event
                handlePushToken(eventToProcess);
            } else if (MessagingUtils.isMessagingRequestContentEvent(eventToProcess)) {
                // Need experience event dataset id for sending the push token
                if (!configSharedState.containsKey(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID)) {
                    Log.warning(LOG_TAG, SELF_TAG, "Unable to track push notification interaction, experience event dataset id is empty. Check the messaging launch extension to add the experience event dataset.");
                    return;
                }
                // handle the push tracking information from messaging request content event
                handleTrackingInfo(eventToProcess);
            } else if (MessagingUtils.isEdgePersonalizationDecisionEvent(eventToProcess)) {
                // validate the edge response event then load any iam rules present
                inAppNotificationHandler.handleEdgePersonalizationNotification(eventToProcess);
            } else if (MessagingUtils.isMessagingConsequenceEvent(eventToProcess)) {
                // handle rules response events containing message definitions
                inAppNotificationHandler.createInAppMessage(eventToProcess);
            }
            // event processed, remove it from the queue
            eventQueue.poll();
        }
    }

    void handlePushToken(final Event event) {
        if (!eventIsValid(event)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to sync push token. Event or EventData is null.");
            return;
        }

        final String pushToken = (String) event.getEventData().get(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER);

        if (StringUtils.isNullOrEmpty(pushToken)) {
            Log.debug(LOG_TAG, SELF_TAG, "Failed to sync push token, token is null or empty.");
            return;
        }

        final Map<String, Object> edgeIdentitySharedState = getXDMSharedState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event);
        final String ecid = MessagingUtils.getSharedStateEcid(edgeIdentitySharedState);
        if (StringUtils.isNullOrEmpty(ecid)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to sync the push token. ECID is unavailable for the user.");
            return;
        }

        final Map<String, Object> eventData = getProfileEventData(pushToken, ecid);
        if (eventData == null) {
            return;
        }

        // Update the push token to the shared state
        final HashMap<String, Object> messagingSharedState = new HashMap<>();
        messagingSharedState.put(MessagingConstants.SharedState.Messaging.PUSH_IDENTIFIER, pushToken);
        getApi().createSharedState(messagingSharedState, event);

        // Send an edge event with profile data as event data
        MessagingUtils.sendEvent(MessagingConstants.EventName.PUSH_PROFILE_EDGE_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                eventData,
                MessagingConstants.EventDispatchErrors.PUSH_PROFILE_UPDATE_ERROR);
    }

    void handleTrackingInfo(final Event event) {
        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null) {
            Log.debug(LOG_TAG, SELF_TAG, "handleTrackingInfo - Cannot track information, eventData is null.");
            return;
        }
        final String eventType = (String) eventData.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE);
        final String messageId = (String) eventData.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        final Object appOpenedObject = eventData.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED);
        final boolean isApplicationOpened = appOpenedObject != null && (boolean) appOpenedObject;
        final String actionId = (String) eventData.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(LOG_TAG, SELF_TAG, "handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.");
            return;
        }

        final Map<String, Object> configSharedState = getSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event);
        final String datasetId = MessagingUtils.getShareStateMessagingEventDatasetId(configSharedState);
        if (StringUtils.isNullOrEmpty(datasetId)) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to record a message interaction, configuration information is not available.");
            return;
        }

        // Creating the Meta Map
        final Map<String, Object> metaMap = new HashMap<>();
        final Map<String, Object> collectMap = new HashMap<>();
        collectMap.put(DATASET_ID, datasetId);
        metaMap.put(COLLECT, collectMap);

        // Create XDM data with tracking data
        final Map<String, Object> xdmMap = getXdmData(eventType, messageId, actionId);

        // Adding application data to xdmMap
        addApplicationData(isApplicationOpened, xdmMap);

        // Adding xdm data to xdmMap
        addXDMData(eventData, xdmMap);

        final Map<String, Object> xdmData = new HashMap<>();
        xdmData.put(XDM, xdmMap);
        xdmData.put(META, metaMap);

        // dispatch push tracking event
        MessagingUtils.sendEvent(MessagingConstants.EventName.PUSH_TRACKING_EDGE_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                xdmData,
                MessagingConstants.EventDispatchErrors.PUSH_TRACKING_ERROR);
    }

    /**
     * Sends a proposition interaction to the customer's experience event dataset.
     *
     * @param interaction {@code String} containing the interaction which occurred
     * @param eventType {@link MessagingEdgeEventType} enum containing the {@link EventType} to be used for the ensuing Edge Event
     * @param message The {@link Message} which triggered the proposition interaction
     */
    void sendPropositionInteraction(final String interaction, final MessagingEdgeEventType eventType, final Message message) {
        final PropositionInfo propositionInfo = message.propositionInfo;
        if (propositionInfo == null || MessagingUtils.isMapNullOrEmpty(propositionInfo.scopeDetails)) {
            Log.trace(LOG_TAG, "%s - Unable to record an in-app message interaction, the scope details were not found for this message.", SELF_TAG);
            return;
        }
        final List<Map<String, Object>> propositions = new ArrayList<>();
        final Map<String, Object> proposition = new HashMap<>();
        proposition.put(ID, propositionInfo.id);
        proposition.put(SCOPE, propositionInfo.scope);
        proposition.put(SCOPE_DETAILS, propositionInfo.scopeDetails);
        propositions.add(proposition);

        final Map<String, Integer> propositionEventType = new HashMap<>();
        propositionEventType.put(eventType.getPropositionEventType(), 1);

        final Map<String, Object> decisioning = new HashMap<>();
        decisioning.put(PROPOSITION_EVENT_TYPE, propositionEventType);
        decisioning.put(PROPOSITIONS, propositions);

        // add propositionAction if this is an interact eventType
        if (eventType.equals(MessagingEdgeEventType.IN_APP_INTERACT)) {
            final Map<String, String> propositionAction = new HashMap<>();
            propositionAction.put(ID, interaction);
            propositionAction.put(LABEL, interaction);
            decisioning.put(PROPOSITION_ACTION, propositionAction);
        }

        // create experience map with proposition tracking data
        final Map<String, Object> experienceMap = new HashMap<>();
        experienceMap.put(DECISIONING, decisioning);

        // create XDM data with experience data
        final Map<String, Object> xdmMap = new HashMap<>();
        xdmMap.put(XDMDataKeys.EVENT_TYPE, eventType.toString());
        xdmMap.put(MessagingConstants.TrackingKeys.EXPERIENCE, experienceMap);

        // create maps for event history
        final Map<String, String> iamHistoryMap = new HashMap<>();
        iamHistoryMap.put(EVENT_TYPE, eventType.getPropositionEventType());
        iamHistoryMap.put(MESSAGE_ID, propositionInfo.activityId);
        iamHistoryMap.put(TRACKING_ACTION, (StringUtils.isNullOrEmpty(interaction) ? "" : interaction));

        // Create the mask for storing event history
        final String[] mask = {MessagingConstants.EventMask.Mask.EVENT_TYPE, MessagingConstants.EventMask.Mask.MESSAGE_ID, MessagingConstants.EventMask.Mask.TRACKING_ACTION};

        final Map<String, Object> xdmEventData = new HashMap<>();
        xdmEventData.put(XDM, xdmMap);
        xdmEventData.put(IAM_HISTORY, iamHistoryMap);

        // dispatch in-app tracking event
        MessagingUtils.sendEvent(MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                xdmEventData,
                mask,
                MessagingConstants.EventDispatchErrors.IN_APP_TRACKING_ERROR);
    }

    void processHubSharedState(Event event) {
        if (isSharedStateUpdateFor(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event) ||
                isSharedStateUpdateFor(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
            processEvents();
        }
    }

    /**
     * Getter for the {@link #executorService}. Access to which is mutex protected.
     *
     * @return A non-null {@link ExecutorService} instance
     */
    ExecutorService getExecutor() {
        synchronized (executorMutex) {
            if (executorService == null) {
                executorService = Executors.newSingleThreadExecutor();
            }

            return executorService;
        }
    }

    /**
     * Getter for the {@link #eventQueue}.
     *
     * @return A non-null {@link ConcurrentLinkedQueue} instance
     */
    ConcurrentLinkedQueue<Event> getEventQueue() {
        return eventQueue;
    }

    //endregion

    //region private methods

    /**
     * Get profile data with token
     *
     * @param token push token which needs to be synced
     * @param ecid  experience cloud id of the device
     * @return {@link Map} of profile data in the correct format with token
     */
    private static Map<String, Object> getProfileEventData(final String token, final String ecid) {
        if (ecid == null) {
            Log.error(LOG_TAG, MessagingExtension.SELF_TAG,"Failed to sync push token, ECID is null.");
            return null;
        }

        final Map<String, String> namespace = new HashMap<>();
        namespace.put(CODE, ECID);

        final Map<String, Object> identity = new HashMap<>();
        identity.put(NAMESPACE, namespace);
        identity.put(MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.ID, ecid);

        final ArrayList<Map<String, Object>> pushNotificationDetailsArray = new ArrayList<>();
        final Map<String, Object> pushNotificationDetailsData = new HashMap<>();
        pushNotificationDetailsData.put(IDENTITY, identity);
        pushNotificationDetailsData.put(APP_ID, ServiceProvider.getInstance().getDeviceInfoService().getApplicationPackageName());
        pushNotificationDetailsData.put(TOKEN, token);
        pushNotificationDetailsData.put(PLATFORM, FCM);
        pushNotificationDetailsData.put(DENY_LISTED, false);

        pushNotificationDetailsArray.add(pushNotificationDetailsData);

        final Map<String, Object> data = new HashMap<>();
        data.put(PUSH_NOTIFICATION_DETAILS, pushNotificationDetailsArray);

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(DATA, data);

        return eventData;
    }

    /**
     * Builds the xdmMap with the tracking information provided by the customer in eventData.
     *
     * @param eventType String eventType can be either applicationOpened or customAction
     * @param messageId String messageId for the push notification provided by the customer
     * @param actionId  String indicating the actionId of the action taken by the user on the push notification
     * @return {@link Map} object containing the xdm formatted data
     */
    private static Map<String, Object> getXdmData(final String eventType, final String messageId, final String actionId) {
        final Map<String, Object> xdmMap = new HashMap<>();
        final Map<String, Object> trackingMap = new HashMap<>();
        final Map<String, Object> customActionMap = new HashMap<>();

        if (actionId != null) {
            customActionMap.put(XDMDataKeys.ACTION_ID, actionId);
            trackingMap.put(XDMDataKeys.CUSTOM_ACTION, customActionMap);
        }

        trackingMap.put(XDMDataKeys.PUSH_PROVIDER, FCM);
        trackingMap.put(XDMDataKeys.PUSH_PROVIDER_MESSAGE_ID, messageId);
        xdmMap.put(XDMDataKeys.EVENT_TYPE, eventType);
        xdmMap.put(XDMDataKeys.PUSH_NOTIFICATION_TRACKING_MIXIN_NAME, trackingMap);

        return xdmMap;
    }

    private static void addApplicationData(final boolean applicationOpened, final Map<String, Object> xdmMap) {
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
     * @param xdmMap    xdmMap map which is updated.
     */
    private static void addXDMData(final Map<String, Object> eventData, final Map<String, Object> xdmMap) {
        // Extract the xdm adobe data string from the event data.
        final String adobe = (String) eventData.get(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
        if (adobe == null) {
            Log.warning(LOG_TAG, SELF_TAG, "Failed to send Adobe data with the tracking data, Adobe XDM data is null.");
            return;
        }

        try {
            // Convert the adobe string to json object
            final JSONObject xdmJson = new JSONObject(adobe);
            final Map<String, Object> xdmMapObject = MessagingUtils.toMap(xdmJson);

            if (xdmMapObject == null) {
                Log.warning(LOG_TAG, SELF_TAG, "Failed to send Adobe data with the tracking data, Adobe XDM data conversion to map failed.");
                return;
            }

            Map<String, Object> mixins = null;

            // Check for if the json has the required keys
            if (xdmMapObject.containsKey(CJM) && xdmMapObject.get(CJM) instanceof Map) {
                mixins = (Map<String, Object>) xdmMapObject.get(CJM);
            }

            if (xdmMapObject.containsKey(MIXINS) && xdmMapObject.get(MIXINS) instanceof Map) {
                mixins = (Map<String, Object>) xdmMapObject.get(MIXINS);
            }

            if (mixins == null) {
                Log.debug(LOG_TAG, SELF_TAG, "Failed to send cjm xdm data with the tracking, Missing XDM data.");
                return;
            }

            xdmMap.putAll(mixins);

            // Check if the xdm data provided by the customer is using cjm for tracking
            // Check if both {@link MessagingConstants#EXPERIENCE} and {@link MessagingConstants#CUSTOMER_JOURNEY_MANAGEMENT} exists
            if (mixins.containsKey(EXPERIENCE) && mixins.get(EXPERIENCE) instanceof Map) {
                Map<String, Object> experience = (Map<String, Object>) mixins.get(EXPERIENCE);
                if (experience.containsKey(CUSTOMER_JOURNEY_MANAGEMENT) && experience.get(CUSTOMER_JOURNEY_MANAGEMENT) instanceof Map) {
                    Map<String, Object> cjm = (Map<String, Object>) experience.get(CUSTOMER_JOURNEY_MANAGEMENT);
                    // Adding Message profile and push channel context to CUSTOMER_JOURNEY_MANAGEMENT
                    final JSONObject jObject = new JSONObject(MESSAGE_PROFILE_JSON);
                    cjm.putAll(MessagingUtils.toMap(jObject));

                    experience.put(CUSTOMER_JOURNEY_MANAGEMENT, cjm);
                    xdmMap.put(EXPERIENCE, experience);
                }
            } else {
                Log.warning(LOG_TAG, SELF_TAG, "Failed to send CJM XDM data with the tracking, required keys are missing.");
            }
        } catch (final JSONException | ClassCastException e) {
            Log.warning(LOG_TAG, SELF_TAG, "Failed to send Adobe data with the tracking data, Adobe data is malformed : %s", e.getMessage());
        }
    }

    /**
     * Checks if the provided {@code event} is a shared state update event for {@code stateOwnerName}
     *
     * @param stateOwnerName the shared state owner name; should not be null
     * @param event          current event to check; should not be null
     * @return {@code boolean} indicating if it is the shared state update for the provided {@code stateOwnerName}
     */
    private boolean isSharedStateUpdateFor(final String stateOwnerName, final Event event) {
        if (stateOwnerName == null || stateOwnerName.isEmpty() || event == null) {
            return false;
        }

        String stateOwner;

        try {
            stateOwner = (String) event.getEventData().get(MessagingConstants.EventDataKeys.STATE_OWNER);
        } catch (ClassCastException e) {
            return false;
        }

        return stateOwnerName.equals(stateOwner);
    }

    private boolean hasValidSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        if (result == null) {
            return false;
        }
        final Map<String, Object> configuration = result.getValue();
        return configuration != null && !configuration.isEmpty();
    }

    private Map<String, Object> getSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        return result == null ? null : result.getValue();
    }

    private Map<String, Object> getXDMSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getXDMSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        return result == null ? null : result.getValue();
    }

    private boolean eventIsValid(final Event event) {
        return event != null && event.getEventData() != null;
    }

    //endregion
}