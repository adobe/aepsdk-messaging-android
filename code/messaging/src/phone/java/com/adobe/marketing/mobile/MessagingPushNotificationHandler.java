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

import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationManagerCompat;

class MessagingPushNotificationHandler {

    static void handlePushNotification(final Context applicationContext, final int notificationId, final String messageId, final MessagingPushPayload payload, final IMessagingPushNotificationFactory factory, final boolean shouldHandleTracking) {
        // If its a silent push message
        if (payload.isSilentPushMessage()) {
            handleSilentPushNotification(applicationContext, messageId, payload);
        } else {
            handleNormalPushNotification(applicationContext, notificationId, messageId, payload, factory, shouldHandleTracking);
        }
    }

    private static void handleNormalPushNotification(final Context applicationContext, final int notificationId, final String messageId, final MessagingPushPayload payload, final IMessagingPushNotificationFactory factory, final boolean shouldHandleTracking) {
        if (factory == null) {
            // Log that register extension is not called or provide a custom notification factory
            return;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(applicationContext);
        // Use the payload data to create notification
        Notification notification = factory.create(applicationContext, payload, messageId, shouldHandleTracking);
        if (notification == null) {
            // Log error
            return;
        }
        notificationManager.notify(notificationId, notification);

        // Broadcast the customer that the notification is created with this payload
    }


    private static void handleSilentPushNotification(final Context applicationContext, final String messageId, final MessagingPushPayload payload) {
        // Broadcast the customer with the payload
    }
}
