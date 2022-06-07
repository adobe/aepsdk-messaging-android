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

/**
 * Interface defining a Messaging extension push notification factory object.
 */
public interface IMessagingPushNotificationFactory {
    /**
     * Creates a push notification from the given {@code MessagingPushPayload}.
     *
     * @param context              the application {@link Context}
     * @param payload              the {@link MessagingPushPayload} containing the data payload from AJO
     * @param messageId            a {@code String} containing the message id
     * @param notificationId       {@code int} id used when the notification was scheduled
     * @param shouldHandleTracking {@code boolean} if true the Messaging extension will handle notification interaction tracking
     * @return the created {@link Notification}
     */
    Notification create(final Context context, final MessagingPushPayload payload, final String messageId, final int notificationId, final boolean shouldHandleTracking);
}
