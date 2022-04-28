/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationManagerCompat;

/**
 * Handles the creation of a {@link Notification} from the given AJO data payload.
 */
class MessagingPushNotificationHandler {
    private static final String SELF_TAG = "MessagingPushNotificationHandler";
    private static final String MESSAGE_ID = "messageId";
    private static final String PUSH_PAYLOAD = "pushPayload";

    /**
     * Creates a silent or normal notification and schedules the created notification with the {@link android.app.NotificationManager}.
     *
     * @param applicationContext   the application {@link Context}
     * @param notificationId       {@code int} id to be used when scheduling the notification
     * @param messageId            a {@code String} containing the message id
     * @param payload              the {@link MessagingPushPayload} containing the data payload from AJO
     * @param factory              the {@link IMessagingPushNotificationFactory} instance to use for creating the notification
     * @param shouldHandleTracking {@code boolean} if true the AEPMessaging extension will handle notification interaction tracking
     */
    static void handlePushNotification(final Context applicationContext, final int notificationId, final String messageId, final MessagingPushPayload payload, final IMessagingPushNotificationFactory factory, final boolean shouldHandleTracking) {
        if (payload.isSilentPushMessage()) {
            Log.debug(LOG_TAG, "%s - Creating a silent push notification.", SELF_TAG);
            handleSilentPushNotification(applicationContext, messageId, payload);
        } else {
            Log.debug(LOG_TAG, "%s - Creating a normal push notification.", SELF_TAG);
            handleNormalPushNotification(applicationContext, notificationId, messageId, payload, factory, shouldHandleTracking);
        }
    }

    /**
     * Handle the creation of normal (non-silent) push notifications. The push notification creation information will be broadcast to the app if the notification was created successfully.
     *
     * @param applicationContext   the application {@link Context}
     * @param notificationId       {@code int} id to be used when scheduling the notification
     * @param messageId            a {@code String} containing the message id
     * @param payload              the {@link MessagingPushPayload} containing the data payload from AJO
     * @param factory              the {@link IMessagingPushNotificationFactory} instance to use for creating the notification
     * @param shouldHandleTracking {@code boolean} if true the AEPMessaging extension will handle notification interaction tracking
     */
    private static void handleNormalPushNotification(final Context applicationContext, final int notificationId, final String messageId, final MessagingPushPayload payload, final IMessagingPushNotificationFactory factory, final boolean shouldHandleTracking) {
        if (factory == null) {
            Log.warning(LOG_TAG, "%s - The MessagingPushNotificationFactory instance is null. Ensure that the Messaging extension has been registered or if using a custom MessagingPushNotificationFactory, ensure that a valid MessagingPushNotificationFactory is set.", SELF_TAG);
            return;
        }
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(applicationContext);
        // Use the payload data to create notification
        final Notification notification = factory.create(applicationContext, payload, messageId, shouldHandleTracking);
        if (notification == null) {
            Log.warning(LOG_TAG, "%s - Failed to create a notification for message with id (%s).", SELF_TAG, messageId);
            return;
        }
        notificationManager.notify(notificationId, notification);

        // Broadcast the customer that the notification is created with this payload
        sendBroadcast(applicationContext, messageId, payload, MessagingPushPayload.ACTION_KEY.ACTION_NORMAL_NOTIFICATION_CREATED);
    }


    /**
     * Broadcasts the push notification creation information to the app if a silent notification was present in the {@link MessagingPushPayload} from AJO.
     *
     * @param applicationContext the application {@link Context}
     * @param messageId          a {@code String} containing the message id
     * @param payload            the {@link MessagingPushPayload} containing the data payload from AJO
     */
    private static void handleSilentPushNotification(final Context applicationContext, final String messageId, final MessagingPushPayload payload) {
        // Broadcast the customer that the notification is created with this payload
        sendBroadcast(applicationContext, messageId, payload, MessagingPushPayload.ACTION_KEY.ACTION_SILENT_NOTIFICATION_CREATED);
    }

    /**
     * Broadcasts the push notification creation information to the app.
     *
     * @param applicationContext the application {@link Context}
     * @param messageId          a {@code String} containing the message id
     * @param payload            the {@link MessagingPushPayload} containing the data payload from AJO
     * @param notificationType   a {@code String} containing the action to be set when sending the broadcast
     */
    private static void sendBroadcast(final Context applicationContext, final String messageId, final MessagingPushPayload payload, final String notificationType) {
        final PackageManager pm = applicationContext.getPackageManager();
        final Intent launchIntent = pm.getLaunchIntentForPackage(applicationContext.getPackageName());
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // include the push payload for silent push notification broadcasts
        if (payload.isSilentPushMessage()) {
            launchIntent.putExtra(MESSAGE_ID, messageId);
            launchIntent.putExtra(PUSH_PAYLOAD, payload);
        }
        if (notificationType.equals(MessagingPushPayload.ACTION_KEY.ACTION_NORMAL_NOTIFICATION_CREATED)) {
            MessagingUtils.sendBroadcasts(applicationContext, launchIntent, MessagingPushPayload.ACTION_KEY.ACTION_NORMAL_NOTIFICATION_CREATED);
        } else if (notificationType.equals(MessagingPushPayload.ACTION_KEY.ACTION_SILENT_NOTIFICATION_CREATED)) {
            MessagingUtils.sendBroadcasts(applicationContext, launchIntent, MessagingPushPayload.ACTION_KEY.ACTION_SILENT_NOTIFICATION_CREATED);
        }
    }
}
