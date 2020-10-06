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
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.EXPERIENCE;
import static com.adobe.marketing.mobile.MessagingConstant.TrackingKeys.MESSAGE_PROFILE_JSON;

import com.adobe.marketing.mobile.xdm.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessagingInternal extends Extension implements EventsHandler {

    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
    private static final String MODULE_NAME = "com.adobe.aepsdk.module.messaging";
    private PlatformServices platformServices = new AndroidPlatformServices();
    private String ecid;
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
     *     <li> {@link GenericDataOSListener} listening to event with eventType {@link EventType#GENERIC_DATA}
     *     and EventSource {@link EventSource#OS}</li>
     *      <li> {@link IdentityRequestContentListener} listening to event with eventType {@link EventType#GENERIC_IDENTITY}
     * 	 *  and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     * </ul>
     *
     * @param extensionApi 	{@link ExtensionApi} instance
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
     * @param extensionUnexpectedError 	{@link ExtensionUnexpectedError} occurred exception
     */
    @Override
    protected void onUnexpectedError(ExtensionUnexpectedError extensionUnexpectedError) {
        super.onUnexpectedError(extensionUnexpectedError);
        this.onUnregistered();
    }

    private void registerEventListeners(final ExtensionApi extensionApi) {
        // todo might want to registerEventListener instead registerListener
        extensionApi.registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, ConfigurationResponseContentListener.class);
        extensionApi.registerListener(EventType.GENERIC_DATA, EventSource.OS, GenericDataOSListener.class);
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
     * @param event 	The {@link Event} thats needs to be queued
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
            }

            else if (EventType.GENERIC_DATA.getName().equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.OS.getName().equalsIgnoreCase(eventToProcess.getSource())) {

                // Need experience event dataset id for sending the push token
                if(!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID)) {
                    Log.error(LOG_TAG, "Unable to sync push token, experience event dataset id is empty. Check the messaging launch extension to add the experience event dataset.");
                    return;
                }

                // handle the push tracking information from generic data os event
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
                if (MobilePrivacyStatus.OPT_OUT.equals(messagingState.getPrivacyStatus())) {
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

        // Create XDM data with tracking data
        final MobilePushTrackingSchemaTest schema = getXdmSchema(eventType, messageId, isApplicationOpened, actionId);
        Map<String, Object> schemaXml = schema.serializeToXdm();

        // Adding adobe cjm data
        addAdobeData(eventData, schemaXml);

        String datasetId = messagingState.getExperienceEventDatasetId();
        ExperiencePlatformEvent experiencePlatformEvent;
        if (datasetId != null && !datasetId.isEmpty()) {
            experiencePlatformEvent = new ExperiencePlatformEvent.Builder()
                    .setXdmSchema(schemaXml, datasetId)
                    .build();
        } else {
            experiencePlatformEvent = new ExperiencePlatformEvent.Builder()
                    .setXdmSchema(schemaXml)
                    .build();
        }

        ExperiencePlatform.sendEvent(experiencePlatformEvent, new ExperiencePlatformCallback() {
            @Override
            public void onResponse(Map<String, Object> map) { /* no-op */ }
        });
    }

    /**
     * Checks whether all the configuration parameters which are required by push notification exists.
     * @param configSharedState Configuration state in a map format.
     * @return boolean value explaining whether the config is valid or not.
     */
    private boolean isConfigValid(Map<String, Object> configSharedState) {
        // Need profile dataset id for sending the push token
        if(!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.PROFILE_DATASET_ID)) {
            Log.error(LOG_TAG, "Unable to sync push token, profile dataset id is empty. Check the messaging launch extension to add the profile dataset.");
            return false;
        }

        // Temp : Need the dccs url from the customer through the updateConfiguration API
        if(!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.DCCS_URL)) {
            Log.error(LOG_TAG, "Unable to sync push token, DCCS url is empty. Check the updateConfiguration API to send the DCCS url.");
            return false;
        }

        // Temp : Need the experience cloud org.
        if(!configSharedState.containsKey(MessagingConstant.SharedState.Configuration.EXPERIENCE_CLOUD_ORG)) {
            Log.error(LOG_TAG, "Unable to sync push token, Experience cloud org is empty.");
            return false;
        }

        return true;
    }

    private void optOut() {
        eventQueue.clear();
        new PushTokenStorage(platformServices.getLocalStorageService()).removeToken();
    }

    // Adding CJM specific data to tracking information
    @SuppressWarnings("unchecked")
    private static void addAdobeData(final EventData eventData, final Map<String, Object> schemaXml) {
        // Temp
        // Convert the adobe string to object
        final String adobe = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE, null);
        if (adobe == null) {
            Log.error(LOG_TAG, "Failed to send adobe data with the tracking data, adobe data is null");
            return;
        }
        JSONObject adobeJson;
        try {
            adobeJson = new JSONObject(adobe);
        } catch (JSONException e) {
            Log.error(LOG_TAG, "Failed to send adobe data with the tracking data, adobe data is malformed : %s", e.getMessage());
            adobeJson = null;
        }

        // Check if the required key is available
        if (adobeJson != null && adobeJson.has(CUSTOMER_JOURNEY_MANAGEMENT)) {
            try {
                final JSONObject customerJourneyManagement = adobeJson.getJSONObject(CUSTOMER_JOURNEY_MANAGEMENT);
                Iterator<String> keys  = customerJourneyManagement.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    schemaXml.put(key, jsonStringToMap(customerJourneyManagement.get(key).toString()));
                }
            } catch (JSONException e) {
                Log.error(LOG_TAG, "Failed to send adobe data with the tracking, cjm json malformed : %s", e.getMessage());
                return;
            }

            // Adding the messageProfile adobe data
            if (schemaXml.containsKey(EXPERIENCE)) {
                HashMap<String, Object> _experience = (HashMap<String, Object>) schemaXml.get(EXPERIENCE);
                try {
                    if (_experience != null) {
                        _experience.putAll(jsonStringToMap(MESSAGE_PROFILE_JSON));
                    }
                } catch (JSONException e) {
                    Log.error(LOG_TAG, "Failed to send adobe data with the tracking, messaging profile json issue : %s", e.getMessage());
                }
            }
        } else {
            Log.debug(LOG_TAG, "Ignoring adobe data with the tracking data, missing cjm keys");
        }
    }

    private static MobilePushTrackingSchemaTest getXdmSchema(final String eventType, final String messageId, boolean isApplicationOpened, final String actionId) {
        final MobilePushTrackingSchemaTest schema = new MobilePushTrackingSchemaTest();
        final Acopprod3 acopprod3 = new Acopprod3();
        final Track track = new Track();
        final CustomAction customAction = new CustomAction();

        if (isApplicationOpened) {
            track.setApplicationOpened(true);
        } else {
            customAction.setActionId(actionId);
            track.setCustomAction(customAction);
        }

        schema.setEventType(eventType);
        track.setId(messageId);
        acopprod3.setTrack(track);
        schema.setAcopprod3(acopprod3);
        return schema;
    }

    private static Map<String, Object> jsonStringToMap(final String jsonString) throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        JSONObject jObject = new JSONObject(jsonString);
        Iterator<String> keys = jObject.keys();

        while( keys.hasNext() ) {
            String key = keys.next();
            Object value = jObject.get(key);
            map.put(key, value);
        }

        return map;
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
