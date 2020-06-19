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

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;


public class MessagingModule extends Module implements EventsHandler {

    private ConcurrentLinkedQueue<Event> waitingEvents = new ConcurrentLinkedQueue<>();
    private static final String MODULE_NAME = "com.adobe.aepsdk.module.messaging";
    private PlatformServices platformServices = new AndroidPlatformServices();
    private String ecid;

    protected MessagingModule(EventHub hub) {
        super(MODULE_NAME, hub);
        registerEventListeners();
    }

    private void registerEventListeners() {
        registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, ConfigurationResponseContentListener.class);
        registerListener(EventType.GENERIC_DATA, EventSource.OS, GenericDataOSListener.class);
        registerListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, IdentityRequestContentListener.class);
    }


    @Override
    public void handlePushToken(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, "Unable to sync push token. Event data received is null");
        }

        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                waitingEvents.add(event);
                processQueuedEvents();
            }
        });
    }

    @Override
    public void handleTrackingInfo(final Event event) {
        //TODO Send tracking info.
        if (event == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Unable to handle handleTrackingInfo. Event received is null.");
        }
    }

    @Override
    public void processConfigurationResponse(final Event event) {
        //TODO Handle privacy preference changes.
        if (event == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Unable to handle configuration response. Event received is null.");
        }

        final EventData configData = event.getData();
        final EventData identityData = getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME, event);

        final MessagingState messagingState = new MessagingState();
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

        waitingEvents.clear();
        new PushTokenStorage(platformServices.getLocalStorageService()).removeToken();
    }

    void processQueuedEvents() {

        while (!waitingEvents.isEmpty()) {
            final Event currentEvent = waitingEvents.peek();

            if (currentEvent == null) {
                Log.debug(LOG_TAG, "processQueuedEvents -  Event queue is empty.");
                break;
            }

            final EventData configState = getSharedEventState(MessagingConstant.SharedState.Configuration.EXTENSION_NAME,
                    currentEvent);

            final EventData identityState = getSharedEventState(MessagingConstant.SharedState.Identity.EXTENSION_NAME,
                    currentEvent);

            // Check if configuration or identity is pending. We want to keep the event in the queue if we expect an update here.
            if (configState == EventHub.SHARED_STATE_PENDING || identityState == EventHub.SHARED_STATE_PENDING) {
                Log.debug(LOG_TAG,
                        "processQueuedEvents -  Pending Configuration or Identity update, so not processing queued event.");
                break;
            }

            if (currentEvent.getEventType() == EventType.GENERIC_IDENTITY) {
                final String pushToken = (String) currentEvent.getEventData().get(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER);
                new PushTokenStorage(platformServices.getLocalStorageService()).storeToken(pushToken);
                new PushTokenSyncer(platformServices.getNetworkService()).syncPushToken(pushToken, ecid);
            }
        }
    }
}
