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

package com.adobe.marketing.mobile.messaging;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.adobe.marketing.mobile.PushNotificationListener;
import com.adobe.marketing.mobile.services.Log;

/**
 * Manages the {@link PushNotificationListener} registration and dispatches notification lifecycle
 * callbacks (received, opened, dismissed). Keeps all listener-related logic in one place, separate
 * from the public API facade in {@code Messaging}.
 */
public final class PushNotificationEventManager {

    private static final String LOG_TAG = "Messaging";
    private static final String CLASS_NAME = "PushNotificationEventManager";
    private static final String ACTION_ID_DISMISS = "Dismiss";

    private static volatile PushNotificationListener listener;

    private PushNotificationEventManager() {}

    public static void setListener(@Nullable final PushNotificationListener newListener) {
        listener = newListener;
    }

    @Nullable
    public static PushNotificationListener getListener() {
        return listener;
    }

    /**
     * Notifies the registered listener that a push notification was received and displayed. Called
     * from {@code MessagingService.handleRemoteMessage()} after the notification is shown.
     *
     * @param payload the original payload constructed from the {@code RemoteMessage}
     */
    public static void notifyReceived(@NonNull final MessagingPushPayload payload) {
        final PushNotificationListener current = listener;
        if (current == null) {
            return;
        }
        try {
            current.onNotificationReceived(payload);
        } catch (final Exception e) {
            Log.warning(
                    LOG_TAG,
                    CLASS_NAME,
                    "PushNotificationListener.onNotificationReceived threw an exception: %s",
                    e.getLocalizedMessage());
        }
    }

    /**
     * Notifies the registered listener of a notification interaction (opened or dismissed). Called
     * from {@code Messaging.handleNotificationResponse()} after validation passes and before the
     * tracking event is dispatched.
     *
     * @param intent the interaction intent containing push data extras
     * @param customActionId the custom action ID, or {@code null} for a body tap, or {@code
     *     "Dismiss"} for a dismissal
     */
    public static void notifyInteraction(
            @NonNull final Intent intent, @Nullable final String customActionId) {
        final PushNotificationListener current = listener;
        if (current == null) {
            return;
        }
        try {
            final MessagingPushPayload payload = new MessagingPushPayload(intent);
            if (ACTION_ID_DISMISS.equals(customActionId)) {
                current.onNotificationDismissed(payload);
            } else {
                current.onNotificationOpened(payload, customActionId);
            }
        } catch (final Exception e) {
            Log.warning(
                    LOG_TAG,
                    CLASS_NAME,
                    "PushNotificationListener callback threw an exception: %s",
                    e.getLocalizedMessage());
        }
    }
}
