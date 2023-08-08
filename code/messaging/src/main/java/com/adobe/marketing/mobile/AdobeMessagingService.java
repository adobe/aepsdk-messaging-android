/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.adobe.marketing.mobile.services.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;

public class AdobeMessagingService extends FirebaseMessagingService {
    private static final String SELF_TAG = "AdobeMessagingService";
    private static final String XDM_KEY  = "_xdm";

    @Override
    public void onNewToken(final @NonNull String token) {
        super.onNewToken(token);
        MobileCore.setPushIdentifier(token);
    }

    @Override
    public void onMessageReceived(final @NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        handleRemoteMessage(this, remoteMessage);
    }

    public static boolean handleRemoteMessage(final @NonNull Context context, final @NonNull RemoteMessage remoteMessage) {
        if (!isAdobePushNotification(remoteMessage)) {
            Log.debug(MessagingPushConstants.LOG_TAG, SELF_TAG, "The received push message is not generated from Adobe Journey Optimizer, Messaging extension is ignoring to display the push notification.");
            return false;
        }

        final MessagingPushPayload payload = new MessagingPushPayload(remoteMessage);

        // build notification with payload
        final MessagingPushBuilder builder = new MessagingPushBuilder(payload, context);

        // display notification
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(remoteMessage.getMessageId().hashCode(), builder.build());

        // dispatch Push Notification Displayed event
        final HashMap<String,Object> notificationData = new HashMap<>(remoteMessage.getData());
        final Event pushNotificationReceivedEvent = new Event.Builder("Push Notification Displayed", EventType.MESSAGING, EventSource.REQUEST_CONTENT).setEventData(notificationData).build();
        MobileCore.dispatchEvent(pushNotificationReceivedEvent);
        return true;
    }


    // This method looks at the remote message and determines if it is an Adobe push notification
    // by checking if it contains the title and body keys.
    // This is a temporary solution until we have a better way to identify Adobe push notifications.
    // @param remoteMessage the remote message to check
    // @return true if the remote message is an Adobe push notification, false otherwise.
    private static boolean isAdobePushNotification(final @NonNull RemoteMessage remoteMessage) {
        return remoteMessage.getData().containsKey(XDM_KEY) || remoteMessage.getData().containsKey(MessagingPushConstants.PayloadKeys.TITLE);
    }

}