/*
  Copyright 2020 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_NAME;
import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.CJM;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.EXPERIENCE;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.MESSAGE_PROFILE_JSON;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.META;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.MIXINS;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.XDM;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys._XDM;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessagingInternal extends Extension implements EventsHandler {

    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
    private PlatformServices platformServices = new AndroidPlatformServices();
    private MessagingState messagingState;
    private ExecutorService executorService;
    private final Object executorMutex = new Object();

    /**
     * Constructor.
     *
     * <p>
     * Called during messaging extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> {@link ConfigurationResponseContentListener} listening to event with eventType {@link EventType#CONFIGURATION}
     *     and EventSource {@link EventSource#RESPONSE_CONTENT}</li>
     *     <li> {@link MessagingRequestContentListener} listening to event with eventType {@link MessagingConstant.EventType#MESSAGING}
     *     and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> {@link IdentityRequestContentListener} listening to event with eventType {@link EventType#GENERIC_IDENTITY}
     * 	 *  and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    protected MessagingInternal(final ExtensionApi extensionApi) {
        super(extensionApi);
        registerEventListeners(extensionApi);

        // Init the messaging state
        messagingState = new MessagingState();
    }

    /**
     * Overridden method of {@link Extension} class to provide a valid extension name to register with eventHub.
     *
     * @return A {@link String} extension name for Messaging
     */
    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide the extension version.
     *
     * @return A {@link String} representing the extension version
     */
    @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Overridden method of {@link Extension} class called when extension is unregistered by the core.
     *
     * <p>
     * On unregister of messaging extension, the shared states are cleared.
     */
    @Override
    protected void onUnregistered() {
        super.onUnregistered();
        getApi().clearSharedEventStates(null);
    }

    /**
     * Overridden method of {@link Extension} class to handle error occurred during registration of the module.
     *
     * @param extensionUnexpectedError {@link ExtensionUnexpectedError} occurred exception
     */
    @Override
    protected void onUnexpectedError(ExtensionUnexpectedError extensionUnexpectedError) {
        super.onUnexpectedError(extensionUnexpectedError);
        this.onUnregistered();
    }

    private void registerEventListeners(final ExtensionApi extensionApi) {
        extensionApi.registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, ConfigurationResponseContentListener.class);
        extensionApi.registerEventListener(MessagingConstant.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName(), MessagingRequestContentListener.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.debug(MessagingConstant.LOG_TAG, "Error in registering %s event : Extension version - %s : Error %s",
                        MessagingConstant.EventType.MESSAGING, MessagingConstant.EXTENSION_VERSION, extensionError.toString());
            }
        });
        extensionApi.registerListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, IdentityRequestContentListener.class);

        Log.debug(MessagingConstant.LOG_TAG, "Registering Messaging extension - version %s",
                MessagingConstant.EXTENSION_VERSION);
    }

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
     * Suspends processing of the events in the queue if the configuration shared state is not ready.
     * Processed events are polled out of the {@link #eventQueue}.
     */
    void processEvents() {
        while (!eventQueue.isEmpty()) {
            Event eventToProcess = eventQueue.peek();

            if (eventToProcess == null) {
                Log.debug(MessagingConstant.LOG_TAG, "Unable to process event, Event received is null.");
                return;
            }

            ExtensionErrorCallback<ExtensionError> configurationErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null) {
                        Log.warning(MessagingConstant.LOG_TAG,
                                String.format("MessagingInternal : Could not process event, an error occurred while retrieving configuration shared state: %s",
                                        extensionError.getErrorName()));
                    }
                }
            };

            ExtensionErrorCallback<ExtensionError> identityErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null) {
                        Log.warning(MessagingConstant.LOG_TAG,
                                String.format("MessagingInternal : Could not process event, an error occurred while retrieving configuration shared state: %s",
                                        extensionError.getErrorName()));
                    }
                }
            };

            final Map<String, Object> configSharedState = getApi().getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME,
                    eventToProcess, configurationErrorCallback);

            final Map<String, Object> identitySharedState = getApi().getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME,
                    eventToProcess, identityErrorCallback);

            // NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (configSharedState == null) {
                Log.warning(MessagingConstant.LOG_TAG,
                        "MessagingInternal : Could not process event, configuration shared state is pending");
                return;
            }

            // NOTE: identity is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (identitySharedState == null) {
                Log.warning(MessagingConstant.LOG_TAG,
                        "MessagingInternal : Could not process event, identity shared state is pending");
                return;
            }

            if (EventType.GENERIC_IDENTITY.getName().equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(eventToProcess.getSource())) {

                // Temp : Check if the config is valid.
                if (!isConfigValid(configSharedState)) {
                    return;
                }

                // handle the push token from generic identity request content event
                handlePushToken(eventToProcess);
            } else if (MessagingConstant.EventType.MESSAGING.equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(eventToProcess.getSource())) {

                // Need experience event dataset id for sending the push token
                if (!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID)) {
                    Log.warning(LOG_TAG, "Unable to sync push token, experience event dataset id is empty. Check the messaging launch extension to add the experience event dataset.");
                    return;
                }

                // handle the push tracking information from messaging request content event
                handleTrackingInfo(eventToProcess);
            }

            // event processed, remove it from the queue
            eventQueue.poll();
        }
    }

    @Override
    public void processConfigurationResponse(final Event event) {
        //TODO Handle privacy preference changes.
        if (event == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Unable to handle configuration response. Event received is null.");
            return;
        }

        final EventData configData = event.getData();
        final EventData identityData = getApi().getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME, event);

        messagingState.setState(configData, identityData);

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!MobilePrivacyStatus.OPT_IN.equals(messagingState.getPrivacyStatus())) {
                    optOut();
                    return;
                }

                processEvents();
            }
        });
    }

    @Override
    public void handlePushToken(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, "Unable to sync push token. Event data received is null");
            return;
        }

        if (event.getEventType() == EventType.GENERIC_IDENTITY) {
            final String pushToken = (String) event.getEventData().get(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER);

            if (!MobilePrivacyStatus.OPT_OUT.equals(messagingState.getPrivacyStatus())) {
                new PushTokenStorage(platformServices.getLocalStorageService()).storeToken(pushToken);
            }
            if (MobilePrivacyStatus.OPT_IN.equals(messagingState.getPrivacyStatus())) {
                new PushTokenSyncer(platformServices.getNetworkService()).syncPushToken(pushToken, messagingState.getEcid(), messagingState.getDccsURL(), messagingState.getExperienceCloudOrg(), messagingState.getProfileDatasetId());
            }
        }
    }

    @Override
    public void handleTrackingInfo(Event event) {
        final EventData eventData = event.getData();
        if (eventData == null) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "handleTrackingInfo - Cannot track information, eventData is null.");
            return;
        }
        final String eventType = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
        final String messageId = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
        final boolean isApplicationOpened = eventData.optBoolean(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, false);
        final String actionId = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, null);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.");
            return;
        }

        String datasetId = messagingState.getExperienceEventDatasetId();
        if (datasetId == null) {
            Log.warning(LOG_TAG, "Failed to track push notification interaction, experience event datasetId is null");
            return;
        }

        // Creating the Meta Map
        Map<String, Object> metaMap = new HashMap<>();
        Map<String, Object> collectMap = new HashMap<>();
        collectMap.put("datasetId", datasetId);
        metaMap.put("collect", collectMap);

        // Create XDM data with tracking data
        final Map<String, Object> xdmMap = getXdmSchema(eventType, messageId, actionId);

        // Adding application data
        addApplicationData(isApplicationOpened, xdmMap);

        // Adding xdm data
        addXDMData(eventData, xdmMap);

        EventData xdmData = new EventData();
        xdmData.putTypedMap(XDM, xdmMap, PermissiveVariantSerializer.DEFAULT_INSTANCE);
        xdmData.putTypedMap(META, metaMap, PermissiveVariantSerializer.DEFAULT_INSTANCE);

        Event trackEvent = new Event.Builder("Push Tracking event", MessagingConstant.EventType.EDGE, EventSource.REQUEST_CONTENT.getName())
                .setData(xdmData)
                .build();
        MobileCore.dispatchEvent(trackEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.error(LOG_TAG, "Error in dispatching event for tracking");
            }
        });
    }

    /**
     * Checks whether all the configuration parameters which are required by push notification exists.
     *
     * @param configSharedState Configuration state in a map format.
     * @return boolean value explaining whether the config is valid or not.
     */
    private boolean isConfigValid(Map<String, Object> configSharedState) {
        // Need profile dataset id for sending the push token
        if (!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.PROFILE_DATASET_ID)) {
            Log.warning(LOG_TAG, "Unable to sync push token, profile dataset id is empty. Check the messaging launch extension to add the profile dataset.");
            return false;
        }

        // Temp : Need the dccs url from the customer through the updateConfiguration API
        if (!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.DCCS_URL)) {
            Log.warning(LOG_TAG, "Unable to sync push token, DCCS url is empty. Check the updateConfiguration API to send the DCCS url.");
            return false;
        }

        // Temp : Need the experience cloud org.
        if (!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.EXPERIENCE_CLOUD_ORG)) {
            Log.warning(LOG_TAG, "Unable to sync push token, Experience cloud org is empty.");
            return false;
        }

        return true;
    }

    private void optOut() {
        eventQueue.clear();
        new PushTokenStorage(platformServices.getLocalStorageService()).removeToken();
    }

    /**
     * Builds the xdmMap with the tracking information provided by the customer in eventData.
     *
     * @param eventType String eventType can be either applicationOpened or customAction
     * @param messageId String messageId for the push notification provided by the customer
     * @param actionId  String indicating the actionId of the action taken by the user on the push notification
     * @return {@link Map} object containing the xdm formatted data
     */
    private static Map<String, Object> getXdmSchema(final String eventType, final String messageId, final String actionId) {
        final Map<String, Object> xdmMap = new HashMap<>();
        final Map<String, Object> pushNotificationTrackingMap = new HashMap<>();
        final Map<String, Object> customActionMap = new HashMap<>();

        if (actionId != null) {
            customActionMap.put("actionID", actionId);
            pushNotificationTrackingMap.put("customAction", customActionMap);
            pushNotificationTrackingMap.put("pushProviderMessageID", messageId);
            pushNotificationTrackingMap.put("pushProvider", MessagingConstant.JSON_VALUES.FCM);
        }
        xdmMap.put("eventType", eventType);
        xdmMap.put("pushNotificationTracking", pushNotificationTrackingMap);
        return xdmMap;
    }

    private static void addApplicationData(final boolean applicationOpened, final Map<String, Object> xdmMap) {
        final Map<String, Object> applicationMap = new HashMap<>();
        final Map<String, Object> launchesMap = new HashMap<>();
        launchesMap.put(MessagingConstant.TrackingKeys.LAUNCHES_VALUE, applicationOpened ? 1 : 0);
        applicationMap.put(MessagingConstant.TrackingKeys.LAUNCHES, launchesMap);
        xdmMap.put(MessagingConstant.TrackingKeys.APPLICATION, applicationMap);
    }

    /**
     * Adding XDM specific data to tracking information.
     *
     * @param eventData eventData which contains the xdm data forwarded by the customer.
     * @param xdmMap    xdmMap map which is updated.
     */
    private static void addXDMData(final EventData eventData, final Map<String, Object> xdmMap) {
        // Extract the xdm adobe data string from the event data.
        final String adobe = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE, null);
        if (adobe == null) {
            Log.warning(LOG_TAG, "Failed to send adobe data with the tracking data, adobe xdm data is null.");
            return;
        }

        try {
            // Convert the adobe string to json object
            JSONObject xdmJson = new JSONObject(adobe);

            // Check for if the json has the required keys
            if (xdmJson.has(CJM) || xdmJson.has(MIXINS)) {
                final JSONObject mixins = xdmJson.has(MIXINS) ? xdmJson.getJSONObject(MIXINS) : xdmJson.getJSONObject(CJM);
                Iterator<String> keys = mixins.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    xdmMap.put(key, mixins.get(key));
                }

                // Check if the xdm data provided by the customer is using cjm for tracking
                // Check if both {@link MessagingConstant#EXPERIENCE} and {@link MessagingConstant#CUSTOMER_JOURNEY_MANAGEMENT} exists
                if (mixins.has(EXPERIENCE)) {
                    JSONObject experience = mixins.getJSONObject(EXPERIENCE);
                    if (experience.has(CUSTOMER_JOURNEY_MANAGEMENT)) {
                        JSONObject cjm = experience.getJSONObject(CUSTOMER_JOURNEY_MANAGEMENT);
                        // Adding Message profile and push channel context to CUSTOMER_JOURNEY_MANAGEMENT
                        final JSONObject jObject = new JSONObject(MESSAGE_PROFILE_JSON);
                        final Iterator<String> jObjectKeys = jObject.keys();

                        while (jObjectKeys.hasNext()) {
                            final String key = jObjectKeys.next();
                            final Object value = jObject.get(key);
                            cjm.put(key, value);
                        }
                        experience.put(CUSTOMER_JOURNEY_MANAGEMENT, cjm);
                        xdmMap.put(EXPERIENCE, experience);
                    }
                } else {
                    Log.warning(LOG_TAG, "Failed to send cjm xdm data with the tracking, required keys are missing.");
                }
            } else {
                Log.warning(LOG_TAG, "Failed to send xdm data with the tracking, required keys are missing.");
            }
        } catch (JSONException e) {
            Log.warning(LOG_TAG, "Failed to send adobe data with the tracking data, adobe data is malformed : %s", e.getMessage());
        }
    }

    // ========================================================================================
    // Getters for private members
    // ========================================================================================

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
}
