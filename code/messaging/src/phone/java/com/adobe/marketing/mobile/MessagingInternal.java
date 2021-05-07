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
package com.adobe.marketing.mobile;

import com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.XDMDataKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_NAME;
import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.APP_ID;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.CODE;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DENY_LISTED;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.ID;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.IDENTITY;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.NAMESPACE;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PLATFORM;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PUSH_NOTIFICATION_DETAILS;
import static com.adobe.marketing.mobile.MessagingConstant.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.TOKEN;
import static com.adobe.marketing.mobile.MessagingConstant.JSON_VALUES.ECID;
import static com.adobe.marketing.mobile.MessagingConstant.JSON_VALUES.FCM;
import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.CJM;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.COLLECT;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.DATASET_ID;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.EXPERIENCE;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.MESSAGE_PROFILE_JSON;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.META;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.MIXINS;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.XDM;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MessagingInternal extends Extension implements MessagingEventsHandler {
    private final String SELF_TAG = "MessagingInternal";
    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
    private final MessagingState messagingState;
    private ExecutorService executorService;
    private final Object executorMutex = new Object();

    /**
     * Constructor.
     *
     * <p>
     * Called during messaging extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> {@link ListenerHubSharedState} listening to event with eventType {@link EventType#HUB}
     *      *     and EventSource {@link EventSource#SHARED_STATE}</li>
     *     <li> {@link ListenerMessagingRequestContent} listening to event with eventType {@link MessagingConstant.EventType#MESSAGING}
     *     and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> {@link ListenerIdentityRequestContent} listening to event with eventType {@link EventType#GENERIC_IDENTITY}
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
     * Overridden method of {@link Extension} class to handle error occurred during registration of the module.
     *
     * @param extensionUnexpectedError {@link ExtensionUnexpectedError} occurred exception
     */
    protected void onUnexpectedError(final ExtensionUnexpectedError extensionUnexpectedError) {
        super.onUnexpectedError(extensionUnexpectedError);
        this.onUnregistered();
    }

    private void registerEventListeners(final ExtensionApi extensionApi) {
        ExtensionErrorCallback<ExtensionError> listenerErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Log.error(MessagingConstant.LOG_TAG, "%s - Error in registering %s event : Extension version - %s : Error %s",
                        SELF_TAG, MessagingConstant.EventType.MESSAGING, MessagingConstant.EXTENSION_VERSION, extensionError.toString());
            }
        };

        extensionApi.registerEventListener(EventType.HUB.getName(), EventSource.SHARED_STATE.getName(), ListenerHubSharedState.class, listenerErrorCallback);
        extensionApi.registerEventListener(MessagingConstant.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName(), ListenerMessagingRequestContent.class, listenerErrorCallback);
        extensionApi.registerEventListener(EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName(), ListenerIdentityRequestContent.class, listenerErrorCallback);

        Log.debug(MessagingConstant.LOG_TAG, "%s - Registering Messaging extension - version %s",
                SELF_TAG, MessagingConstant.EXTENSION_VERSION);
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
     * Suspends processing of the events in the queue if the configuration or identity shared state is not ready.
     * Processed events are polled out of the {@link #eventQueue}.
     */
    void processEvents() {
        while (!eventQueue.isEmpty()) {
            Event eventToProcess = eventQueue.peek();

            if (eventToProcess == null) {
                Log.debug(MessagingConstant.LOG_TAG, "%s - Unable to process event, Event received is null.", SELF_TAG);
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

            ExtensionErrorCallback<ExtensionError> edgeIdentityErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null) {
                        Log.warning(MessagingConstant.LOG_TAG,
                                String.format("MessagingInternal : Could not process event, an error occurred while retrieving edge identity shared state: %s",
                                        extensionError.getErrorName()));
                    }
                }
            };

            final Map<String, Object> configSharedState = getApi().getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME,
                    eventToProcess, configurationErrorCallback);

            final Map<String, Object> edgeIdentitySharedState = getApi().getXDMSharedEventState(MessagingConstant.SharedState.EdgeIdentity.EXTENSION_NAME,
                    eventToProcess, edgeIdentityErrorCallback);

            // NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (configSharedState == null) {
                Log.warning(MessagingConstant.LOG_TAG,
                        "%s : Could not process event, configuration shared state is pending", SELF_TAG);
                return;
            }

            // NOTE: identity is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (edgeIdentitySharedState == null) {
                Log.warning(MessagingConstant.LOG_TAG,
                        "%s : Could not process event, identity shared state is pending", SELF_TAG);
                return;
            }

            // Set the messaging state
            messagingState.setState(configSharedState, edgeIdentitySharedState);

            if (EventType.GENERIC_IDENTITY.getName().equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(eventToProcess.getSource())) {

                // handle the push token from generic identity request content event
                handlePushToken(eventToProcess);
            } else if (MessagingConstant.EventType.MESSAGING.equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(eventToProcess.getSource())) {

                // Need experience event dataset id for sending the push token
                if (!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID)) {
                    Log.warning(LOG_TAG, "%s - Unable to track push notification interaction, experience event dataset id is empty. Check the messaging launch extension to add the experience event dataset.", SELF_TAG);
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
    public void handlePushToken(final Event event) {
        if (event == null || event.getEventData() == null) {
            Log.debug(LOG_TAG, "%s - Unable to sync push token. Event or event data received is null.", SELF_TAG);
            return;
        }

        final String pushToken = (String) event.getEventData().get(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER);

        if (pushToken == null || pushToken.isEmpty()) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, token is null or empty.");
            return;
        }

        Map<String, Object> eventData = getProfileEventData(pushToken, messagingState.getEcid());
        if (eventData == null) {
            return;
        }
        // Send an edge event with profile data as event data
        final Event profileEvent = new Event.Builder(MessagingConstant.EventName.MESSAGING_PUSH_PROFILE_EDGE_EVENT, MessagingConstant.EventType.EDGE, EventSource.REQUEST_CONTENT.getName())
                .setEventData(eventData)
                .build();
        MobileCore.dispatchEvent(profileEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.error(LOG_TAG, "%s - Error in dispatching event for updating the push profile details", SELF_TAG);
            }
        });
    }

    @Override
    public void handleTrackingInfo(final Event event) {
        final EventData eventData = event.getData();
        if (eventData == null) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "%s - handleTrackingInfo - Cannot track information, eventData is null.", SELF_TAG);
            return;
        }
        final String eventType = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
        final String messageId = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
        final boolean isApplicationOpened = eventData.optBoolean(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, false);
        final String actionId = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, null);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "%s - handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.", SELF_TAG);
            return;
        }

        String datasetId = messagingState.getExperienceEventDatasetId();
        if (datasetId == null || datasetId.isEmpty()) {
            Log.warning(LOG_TAG, "%s - Failed to track push notification interaction, experience event datasetId is null or empty", SELF_TAG);
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

        final Event trackEvent = new Event.Builder(MessagingConstant.EventName.MESSAGING_PUSH_TRACKING_EDGE_EVENT, MessagingConstant.EventType.EDGE, EventSource.REQUEST_CONTENT.getName())
                .setEventData(xdmData)
                .build();

        MobileCore.dispatchEvent(trackEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.error(LOG_TAG, "%s - Error in dispatching event for tracking", SELF_TAG);
            }
        });
    }

    @Override
    public void processHubSharedState(Event event) {
        if (isSharedStateUpdateFor(MessagingConstant.SharedState.Configuration.EXTENSION_NAME, event) ||
                isSharedStateUpdateFor(MessagingConstant.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
            processEvents();
        }
    }

    /**
     * Get profile data with token
     * @param token push token which needs to be synced
     * @param ecid experience cloud id of the device
     * @return {@link Map} of profile data in the correct format with token
     */
    private static Map<String, Object> getProfileEventData(final String token, final String ecid) {
        if (ecid == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "MessagingInternal - Failed to sync push token, ecid is null.");
            return null;
        }

        final Map<String, String> namespace = new HashMap<>();
        namespace.put(CODE, ECID);

        final Map<String, Object> identity = new HashMap<>();
        identity.put(NAMESPACE, namespace);
        identity.put(ID, ecid);

        final ArrayList<Map<String, Object>> pushNotificationDetailsArray = new ArrayList<>();
        final Map<String, Object> pushNotificationDetailsData = new HashMap<>();
        pushNotificationDetailsData.put(IDENTITY, identity);
        pushNotificationDetailsData.put(APP_ID, App.getApplication().getPackageName());
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
        final Map<String, Object> pushNotificationTrackingMap = new HashMap<>();
        final Map<String, Object> customActionMap = new HashMap<>();

        if (actionId != null) {
            customActionMap.put(XDMDataKeys.XDM_DATA_ACTION_ID, actionId);
            pushNotificationTrackingMap.put(XDMDataKeys.XDM_DATA_CUSTOM_ACTION, customActionMap);
        }
        pushNotificationTrackingMap.put(XDMDataKeys.XDM_DATA_PUSH_PROVIDER_MESSAGE_ID, messageId);
        pushNotificationTrackingMap.put(XDMDataKeys.XDM_DATA_PUSH_PROVIDER, FCM);
        xdmMap.put(XDMDataKeys.XDM_DATA_EVENT_TYPE, eventType);
        xdmMap.put(XDMDataKeys.XDM_DATA_PUSH_NOTIFICATION_TRACKING, pushNotificationTrackingMap);
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
        final String adobe = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, null);
        if (adobe == null) {
            Log.warning(LOG_TAG, "MessagingInternal - Failed to send adobe data with the tracking data, adobe xdm data is null.");
            return;
        }

        try {
            // Convert the adobe string to json object
            final JSONObject xdmJson = new JSONObject(adobe);
            final Map<String, Object> xdmMapObject = MessagingUtils.toMap(xdmJson);
            
            if (xdmMapObject == null) {
                Log.warning(LOG_TAG, "Failed to send adobe data with the tracking data, adobe xdm data conversion to map faileds.");
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
                Log.debug(LOG_TAG, "MessagingInternal - Failed to send cjm xdm data with the tracking, Missing xdm data.");
                return;
            }

            xdmMap.putAll(mixins);

            // Check if the xdm data provided by the customer is using cjm for tracking
            // Check if both {@link MessagingConstant#EXPERIENCE} and {@link MessagingConstant#CUSTOMER_JOURNEY_MANAGEMENT} exists
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
                Log.warning(LOG_TAG, "MessagingInternal - Failed to send cjm xdm data with the tracking, required keys are missing.");
            }
        } catch (JSONException e) {
            Log.warning(LOG_TAG, "MessagingInternal - Failed to send adobe data with the tracking data, adobe data is malformed : %s", e.getMessage());
        } catch (ClassCastException e) {
            Log.warning(LOG_TAG, "MessagingInternal - Failed to send adobe data with the tracking data, adobe data is malformed : %s", e.getMessage());
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

    /**
     * Checks if the provided {@code event} is a shared state update event for {@code stateOwnerName}
     *
     * @param stateOwnerName the shared state owner name; should not be null
     * @param event current event to check; should not be null
     * @return {@code boolean} indicating if it is the shared state update for the provided {@code stateOwnerName}
     */
    private boolean isSharedStateUpdateFor(final String stateOwnerName, final Event event) {
        if (stateOwnerName == null || stateOwnerName.isEmpty() || event == null) {
            return false;
        }

        String stateOwner;

        try {
            stateOwner = (String) event.getEventData().get(MessagingConstant.EventDataKeys.STATE_OWNER);
        } catch (ClassCastException e) {
            return false;
        }

        return stateOwnerName.equals(stateOwner);
    }
}
