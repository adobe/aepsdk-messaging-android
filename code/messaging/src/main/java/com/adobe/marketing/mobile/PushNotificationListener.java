/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Listener interface for push notification lifecycle events. Register via {@link
 * Messaging#setPushNotificationListener(PushNotificationListener)}.
 *
 * <p>Callbacks are invoked on background threads. Implementations must not perform long-running
 * work on the calling thread.
 */
public interface PushNotificationListener {

    /**
     * Called after a push notification has been displayed by the SDK.
     *
     * <p>This is invoked from {@code MessagingService.handleRemoteMessage()} on the FCM background
     * thread. It fires for the out-of-the-box and delegated integration paths (Paths 1 and 2).
     *
     * @param payload the push notification payload including any custom data keys
     */
    void onNotificationReceived(@NonNull MessagingPushPayload payload);

    /**
     * Called when the user taps the notification body or an action button.
     *
     * <p>This is invoked from {@code Messaging.handleNotificationResponse()} and fires across all
     * integration paths — out-of-the-box, delegated, and manual.
     *
     * @param payload the push notification payload reconstructed from the interaction intent
     * @param actionButtonId the label/ID of the tapped action button, or {@code null} if the user
     *     tapped the notification body
     */
    void onNotificationOpened(
            @NonNull MessagingPushPayload payload, @Nullable String actionButtonId);

    /**
     * Called when the user dismisses (swipes away / clears) the notification.
     *
     * <p>This is invoked from {@code Messaging.handleNotificationResponse()} and fires across all
     * integration paths — out-of-the-box, delegated, and manual.
     *
     * @param payload the push notification payload reconstructed from the interaction intent
     */
    void onNotificationDismissed(@NonNull MessagingPushPayload payload);
}
