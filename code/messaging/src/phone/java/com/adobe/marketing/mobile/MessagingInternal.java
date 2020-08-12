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

import java.util.concurrent.ConcurrentLinkedQueue;

import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_NAME;
import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

import com.adobe.marketing.mobile.xdm.*;

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

    protected MessagingInternal(final ExtensionApi extensionApi) {
        super(extensionApi);
        registerEventListeners(extensionApi);
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

    private void registerEventListeners(ExtensionApi extensionApi) {
        // todo might want to registerEventListener instead registerListener
        extensionApi.registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, ConfigurationResponseContentListener.class);
        extensionApi.registerListener(EventType.GENERIC_DATA, EventSource.OS, GenericDataOSListener.class);
        extensionApi.registerListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, IdentityRequestContentListener.class);
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
        processEvents();
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
                return;
            }

            ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null) {
                        Log.warning(MessagingConstant.LOG_TAG,
                                String.format("MessagingInternal : Could not process event, an error occurred while retrieving configuration shared state: %s",
                                        extensionError.getErrorName()));
                    }
                }
            };
            Map<String, Object> configSharedState = getApi().getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME,
                    eventToProcess, extensionErrorCallback);

            // NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (configSharedState == null) {
                Log.warning(MessagingConstant.LOG_TAG,
                        "MessagingInternal : Could not process event, configuration shared state is pending");
                return;
            }

            if (EventType.CONFIGURATION.getName().equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.RESPONSE_CONTENT.getName().equalsIgnoreCase(eventToProcess.getSource())) {
                // handle the places monitor request event
                processConfigurationResponse(eventToProcess);
            }

            else if (EventType.GENERIC_DATA.getName().equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.OS.getName().equalsIgnoreCase(eventToProcess.getSource())) {
                // handle the places monitor request event
                handleTrackingInfo(eventToProcess);
            }

            else if (EventType.GENERIC_IDENTITY.getName().equalsIgnoreCase(eventToProcess.getType()) &&
                    EventSource.REQUEST_CONTENT.getName().equalsIgnoreCase(eventToProcess.getSource())) {
                // handle the places monitor request event
                handlePushToken(eventToProcess);
            }

            // event processed, remove it from the queue
            eventQueue.poll();
        }
    }

    @Override
    public void handlePushToken(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, "Unable to sync push token. Event data received is null");
        }

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                eventQueue.add(event);
                processQueuedEvents();
            }
        });
    }

    @Override
    public void processConfigurationResponse(final Event event) {
        //TODO Handle privacy preference changes.
        if (event == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Unable to handle configuration response. Event received is null.");
        }

        final EventData configData = event.getData();
        final EventData identityData = getApi().getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME, event);

        messagingState = new MessagingState();
        messagingState.setState(configData, identityData);

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (MobilePrivacyStatus.OPT_OUT.equals(messagingState.getPrivacyStatus())) {
                    optOut();
                    return;
                }

                processQueuedEvents();
            }
        });
    }

    private void optOut() {
        eventQueue.clear();
        new PushTokenStorage(platformServices.getLocalStorageService()).removeToken();
    }

    void processQueuedEvents() {

        while (!eventQueue.isEmpty()) {
            final Event currentEvent = eventQueue.peek();

            if (currentEvent == null) {
                Log.debug(LOG_TAG, "processQueuedEvents -  Event queue is empty.");
                break;
            }

            final EventData configState = getApi().getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME,
                    currentEvent);

            final EventData identityState = getApi().getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME,
                    currentEvent);

            // Check if configuration or identity is pending. We want to keep the event in the queue if we expect an update here.
            if (configState == EventHub.SHARED_STATE_PENDING || identityState == EventHub.SHARED_STATE_PENDING) {
                Log.debug(LOG_TAG,
                        "processQueuedEvents -  Pending Configuration or Identity update, so not processing queued event.");
                break;
            }

            if (currentEvent.getEventType() == EventType.GENERIC_IDENTITY) {
                final String pushToken = (String) currentEvent.getEventData().get(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER);
                if (!MobilePrivacyStatus.OPT_OUT.equals(messagingState.getPrivacyStatus())) {
                    new PushTokenStorage(platformServices.getLocalStorageService()).storeToken(pushToken);
                }
                if (MobilePrivacyStatus.OPT_IN.equals(messagingState.getPrivacyStatus())) {
                    new PushTokenSyncer(platformServices.getNetworkService()).syncPushToken(pushToken, messagingState.getEcid());

                }
            }
            eventQueue.poll();
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
        final Boolean isApplicationOpened = eventData.optBoolean(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, false);
        final String actionId = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, null);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.");
            return;
        }

        // Create XDM data with tracking data
        MobilePushTrackingSchemaTest schema = getXdmSchema(eventType, messageId, isApplicationOpened, actionId);
        ExperiencePlatformEvent experiencePlatformEvent = new ExperiencePlatformEvent.Builder()
                .setXdmSchema(schema)
                .build();

        ExperiencePlatform.sendEvent(experiencePlatformEvent, new ExperiencePlatformCallback() {
            @Override
            public void onResponse(Map<String, Object> map) {
            }
        });
    }

    private MobilePushTrackingSchemaTest getXdmSchema(String eventType, String messageId, Boolean isApplicationOpened, String actionId) {
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