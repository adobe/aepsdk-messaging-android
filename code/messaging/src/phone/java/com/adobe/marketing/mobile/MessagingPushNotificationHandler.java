package com.adobe.marketing.mobile;

import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationManagerCompat;

class MessagingPushNotificationHandler {

    static void handlePushNotification(Context applicationContext, int notificationId, String messageId, MessagingPushPayload payload, IMessagingPushNotificationFactory factory) {
        // If its a silent push message
        if (payload.isSilentPushMessage()) {
            handleSilentPushNotification(applicationContext, messageId, payload);
        } else {
            handleNormalPushNotification(applicationContext, notificationId, messageId, payload, factory);
        }
    }

    private static void handleNormalPushNotification(Context applicationContext, int notificationId, String messageId, MessagingPushPayload payload, IMessagingPushNotificationFactory factory) {
        if (factory == null) {
            // Log that register extension is not called or provide a custom notification factory
            return;
        }
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(applicationContext);
        // Use the payload data to create notification
        Notification notification = factory.create(applicationContext, payload, messageId);
        if (notification == null) {
            // Log error
            return;
        }
        notificationManager.notify(notificationId, notification);

        // Broadcast the customer that the notification is created with this payload
    }


    private static void handleSilentPushNotification(Context applicationContext, String messageId, MessagingPushPayload payload) {
        // Broadcast the customer with the payload
    }
}
