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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adobe.marketing.mobile.messaging.internal.MessagingExtension;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Messaging {
    private static final String EXTENSION_VERSION = "2.1.2";
    private static final String LOG_TAG = "Messaging";
    private static final String CLASS_NAME = "Messaging";

    private static final String EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED = "pushTracking.applicationOpened";
    private static final String EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION = "pushTracking.customAction";
    private static final String PUSH_NOTIFICATION_INTERACTION_EVENT = "Push notification interaction event";
    private static final String UPDATE_MESSAGE_FEEDS = "Update message feeds";
    private static final String REFRESH_MESSAGES_EVENT = "Refresh in-app messages";
    private static final long TIMEOUT_MILLIS = 5000L;
    private static final String TRACK_INFO_KEY_ACTION_ID = "actionId";
    private static final String TRACK_INFO_KEY_ADOBE_XDM = "adobe_xdm";
    private static final String TRACK_INFO_KEY_APPLICATION_OPENED = "applicationOpened";
    private static final String TRACK_INFO_KEY_EVENT_TYPE = "eventType";
    private static final String TRACK_INFO_KEY_GOOGLE_MESSAGE_ID = "google.message_id";
    private static final String TRACK_INFO_KEY_MESSAGE_ID = "messageId";
    private static final String _XDM = "_xdm";
    private static final String UPDATE_FEEDS = "updatefeeds";
    private static final String SURFACES = "surfaces";
    private static final String REFRESH_MESSAGES = "refreshmessages";

    public static final Class<? extends Extension> EXTENSION = MessagingExtension.class;

    private Messaging() {
    }

    /**
     * Returns the current version of the Messaging extension.
     *
     * @return A {@link String} representing the Messaging extension version
     */
    @NonNull public static String extensionVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Registers the Messaging extension with the {@code MobileCore}.
     * <p>
     * This will allow the extension to send and receive events to and from the SDK.
     */
    @Deprecated
    public static void registerExtension() {
        MobileCore.registerExtension(MessagingExtension.class, extensionError -> {
            if (extensionError == null) {
                return;
            }
            Log.error(LOG_TAG, CLASS_NAME, "There was an error registering Messaging Extension: %s", extensionError.getErrorName());
        });
    }

    /**
     * Extracts and update the intent with xdm data and message id from data payload.
     * <p>
     * This method needs to be called with the intent before the notification is created.
     *
     * @param intent    Intent which needs to be updated with xdm data and messageId
     * @param messageId String : message id from RemoteMessage which is received in FirebaseMessagingService#onMessageReceived
     * @param data      Map which represents the data part of the remoteMessage which is received in FirebaseMessagingService#onMessageReceived
     * @return boolean value indicating whether the intent was update with push tracking details (messageId and xdm data).
     */
    public static boolean addPushTrackingDetails(@NonNull final Intent intent, @NonNull final String messageId, final @NonNull Map<String, String> data) {
        if (intent == null) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to add push tracking details as intent is null.");
            return false;
        }
        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to add push tracking details as MessageId is null.");
            return false;
        }
        if (MapUtils.isNullOrEmpty(data)) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to add push tracking details as data is null or empty.");
            return false;
        }


        // Adding message id as extras in intent
        intent.putExtra(TRACK_INFO_KEY_MESSAGE_ID, messageId);

        // Adding xdm data as extras in intent. If the xdm key is not present just log a warning
        final String xdmData = data.get(_XDM);
        if (xdmData != null && !xdmData.isEmpty()) {
            intent.putExtra(TRACK_INFO_KEY_ADOBE_XDM, xdmData);
        } else {
            Log.warning(LOG_TAG, CLASS_NAME, "XDM data is not added as push tracking details to the intent, XDM data is null or empty");
        }

        return true;
    }

    /**
     * Sends the push notification interactions as an experience event to Adobe Experience Edge.
     *
     * @param intent            object which contains the tracking and xdm information.
     * @param applicationOpened Boolean values denoting whether the application was opened when notification was clicked
     * @param customActionId    String value of the custom action (e.g button id on the notification) which was clicked.
     */
    public static void handleNotificationResponse(@NonNull final Intent intent, final boolean applicationOpened, @Nullable final String customActionId) {
        if (intent == null) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to track notification interactions, intent provided is null");
            return;
        }
        String messageId = intent.getStringExtra(TRACK_INFO_KEY_MESSAGE_ID);
        if (StringUtils.isNullOrEmpty(messageId)) {
            // Check if the message Id is in the intent with the key TRACK_INFO_KEY_GOOGLE_MESSAGE_ID which comes through google directly
            // This happens when FirebaseMessagingService#onMessageReceived is not called.
            messageId = intent.getStringExtra(TRACK_INFO_KEY_GOOGLE_MESSAGE_ID);
            if (StringUtils.isNullOrEmpty(messageId)) {
                Log.warning(LOG_TAG, CLASS_NAME, "Failed to track notification interactions, message id provided is null");
                return;
            }
        }

        final String xdmData = intent.getStringExtra(TRACK_INFO_KEY_ADOBE_XDM);
        if (xdmData == null) {
            Log.warning(LOG_TAG, CLASS_NAME, "XDM data provided is null");
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(TRACK_INFO_KEY_MESSAGE_ID, messageId);
        eventData.put(TRACK_INFO_KEY_APPLICATION_OPENED, applicationOpened);
        eventData.put(TRACK_INFO_KEY_ADOBE_XDM, xdmData);

        if (StringUtils.isNullOrEmpty(customActionId)) {
            eventData.put(TRACK_INFO_KEY_EVENT_TYPE, EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED);
        } else {
            eventData.put(TRACK_INFO_KEY_ACTION_ID, customActionId);
            eventData.put(TRACK_INFO_KEY_EVENT_TYPE, EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION);
        }

        final Event messagingEvent = new Event.Builder(PUSH_NOTIFICATION_INTERACTION_EVENT,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEventWithResponseCallback(messagingEvent, TIMEOUT_MILLIS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(AdobeError adobeError) {
                Log.warning(LOG_TAG, CLASS_NAME, "Failed to track notification interactions: Error: %s", adobeError.getErrorName());
            }

            @Override
            public void call(Event event) {
            }
        });
    }

    /**
     * Initiates a network call to retrieve remote In-App Message definitions from Offers.
     */
    public static void refreshInAppMessages() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(REFRESH_MESSAGES, true);

        final Event refreshMessageEvent = new Event.Builder(REFRESH_MESSAGES_EVENT,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(refreshMessageEvent);
    }

    // region Message Feed api

    /**
     *  Dispatches an event to fetch message feeds for the provided surface paths from the Adobe Journey Optimizer via the Experience Edge network.
     *  @param surfacePaths A {@code List<String>} of surface paths
     */
    public static void updateFeedsForSurfacePaths(final List<String> surfacePaths) {
        final List<String> validSurfacePaths = new ArrayList<>();
        for (final String surfacePath : surfacePaths) {
            if (!StringUtils.isNullOrEmpty(surfacePath)) {
                validSurfacePaths.add(surfacePath);
            }
        }

        if (validSurfacePaths.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME, "Cannot update feeds as the provided surface paths array has no valid items.");
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(UPDATE_FEEDS, true);
        eventData.put(SURFACES, validSurfacePaths);

        final Event updateFeedsEvent = new Event.Builder(UPDATE_MESSAGE_FEEDS,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(updateFeedsEvent);
    }
}
