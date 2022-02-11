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

import static com.adobe.marketing.mobile.MessagingConstants.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.REFRESH_MESSAGES;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataValues.EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataValues.EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import android.content.Intent;

import java.util.Map;


public final class Messaging {
    private static final String SELF_TAG = "Messaging";

    private Messaging() {
    }

    /**
     * Returns the current version of the Messaging extension.
     *
     * @return A {@link String} representing the Messaging extension version
     */
    public static String extensionVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Registers the Messaging extension with the {@code MobileCore}.
     * <p>
     * This will allow the extension to send and receive events to and from the SDK.
     */
    public static void registerExtension() {
        if (MobileCore.getCore() == null || MobileCore.getCore().eventHub == null) {
            Log.warning(LOG_TAG, "%s - Unable to register Messaging SDK since MobileCore is not initialized properly.", SELF_TAG);
        }

        MobileCore.registerExtension(MessagingInternal.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.debug(LOG_TAG, "%s - There was an error registering Messaging Extension: %s", SELF_TAG, extensionError.getErrorName());
            }
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
    public static boolean addPushTrackingDetails(final Intent intent, final String messageId, final Map<String, String> data) {
        if (intent == null) {
            Log.warning(LOG_TAG, "%s - Failed to add push tracking details as intent is null.", SELF_TAG);
            return false;
        }
        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.warning(LOG_TAG, "%s - Failed to add push tracking details as MessageId is null.", SELF_TAG);
            return false;
        }
        if (MessagingUtils.isMapNullOrEmpty(data)) {
            Log.warning(LOG_TAG, "%s - Failed to add push tracking details as data is null or empty.", SELF_TAG);
            return false;
        }

        // Adding message id as extras in intent
        intent.putExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, messageId);

        // Adding xdm data as extras in intent. If the xdm key is not present just log a warning
        final String xdmData = data.get(MessagingConstants.TrackingKeys._XDM);
        if (xdmData != null && !xdmData.isEmpty()) {
            intent.putExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, xdmData);
        } else {
            Log.warning(LOG_TAG, "%s - Xdm data is not added as push tracking details to the intent, Xdm data is null or empty", SELF_TAG);
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
    public static void handleNotificationResponse(final Intent intent, final boolean applicationOpened, final String customActionId) {
        if (intent == null) {
            Log.warning(LOG_TAG, "%s - Failed to track notification interactions, intent provided is null", SELF_TAG);
            return;
        }
        String messageId = intent.getStringExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID);
        if (StringUtils.isNullOrEmpty(messageId)) {
            // Check if the message Id is in the intent with the key TRACK_INFO_KEY_GOOGLE_MESSAGE_ID which comes through google directly
            // This happens when FirebaseMessagingService#onMessageReceived is not called.
            messageId = intent.getStringExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_GOOGLE_MESSAGE_ID);
            if (StringUtils.isNullOrEmpty(messageId)) {
                Log.warning(LOG_TAG, "%s - Failed to track notification interactions, message id provided is null", SELF_TAG);
                return;
            }
        }

        final String xdmData = intent.getStringExtra(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM);
        if (xdmData == null) {
            Log.warning(LOG_TAG, "%s - XDM data provided is null", SELF_TAG);
        }

        final EventData eventData = new EventData();
        eventData.putString(TRACK_INFO_KEY_MESSAGE_ID, messageId);
        eventData.putBoolean(TRACK_INFO_KEY_APPLICATION_OPENED, applicationOpened);
        eventData.putString(TRACK_INFO_KEY_ADOBE_XDM, xdmData);

        if (StringUtils.isNullOrEmpty(customActionId)) {
            eventData.putString(TRACK_INFO_KEY_EVENT_TYPE, EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED);
        } else {
            eventData.putString(TRACK_INFO_KEY_ACTION_ID, customActionId);
            eventData.putString(TRACK_INFO_KEY_EVENT_TYPE, EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION);
        }

        final Event messagingEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_PUSH_NOTIFICATION_INTERACTION_EVENT,
                MessagingConstants.EventType.MESSAGING, MessagingConstants.EventSource.REQUEST_CONTENT)
                .setData(eventData)
                .build();
        MobileCore.dispatchEvent(messagingEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.warning(LOG_TAG, "%s - Failed to track notification interactions: Error: %s", SELF_TAG, extensionError.toString());
            }
        });
    }

    /**
     * Initiates a network call to retrieve remote In-App Message definitions from Offers.
     */
    public static void refreshInAppMessages() {
        final EventData eventData = new EventData();
        eventData.putBoolean(REFRESH_MESSAGES, true);

        final Event refreshMessageEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_RETRIEVE_MESSAGE_DEFINITIONS,
                MessagingConstants.EventType.MESSAGING, MessagingConstants.EventSource.REQUEST_CONTENT)
                .setData(eventData)
                .build();
        MobileCore.dispatchEvent(refreshMessageEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.warning(LOG_TAG, "%s - Failed to refresh in-app messages: Error: %s", SELF_TAG, extensionError.toString());
            }
        });
    }
}
