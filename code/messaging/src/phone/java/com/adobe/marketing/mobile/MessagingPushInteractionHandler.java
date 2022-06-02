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

import static com.adobe.marketing.mobile.MessagingConstants.PushNotificationPayload.ActionButtonType;
import static com.adobe.marketing.mobile.MessagingConstants.PushNotificationPayload.NOTIFICATION_ID;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.NotificationManagerCompat;

/**
 * This class is used to handle push notification interactions.
 */
public class MessagingPushInteractionHandler extends BroadcastReceiver {
    private static final String LOG_TAG = "MessagingPushInteractionHandler";
    private static final String ACTION_BUTTON_TYPE_KEY = "adb_action_type";
    private static final String ACTION_BUTTON_LINK_KEY = "adb_action_link";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (context == null || intent == null) {
            Log.debug(LOG_TAG, "onReceive() - Intent or Context null, ignoring this broadcast message");
            return;
        }
        PushActionHandlingRunnable runnable = new PushActionHandlingRunnable(context, intent);
        new Thread(runnable).start();
    }

    /**
     * {@link Runnable} to handle the processing of push notification interactions.
     */
    static class PushActionHandlingRunnable implements Runnable {
        final Context context;
        final Intent intent;

        PushActionHandlingRunnable(final Context context, final Intent intent) {
            this.context = context;
            this.intent = intent;
        }

        @Override
        public void run() {
            handleAction();
        }

        /**
         * Handles any actions present in the received {@link Intent}.
         * If a notification button press occurred, the specified action is performed within {@link #handleNotificationButtonPress()}.
         * If a notification interaction occurred (for example notification clicked or deleted), this function will broadcast the
         * notification interaction information to the app.
         * <p>
         * Notification interactions may contain an additional boolean extra named HANDLE_NOTIFICATION_TRACKING_KEY.
         * If this flag set to true, the push interaction tracking will be handled by {@link #handlePushInteraction()}.
         */
        void handleAction() {
            final String action = intent.getAction();

            if (StringUtils.isNullOrEmpty(action)) {
                Log.debug(LOG_TAG, "handleAction() - Empty/Null action. Not processing this broadcast message.");
                return;
            }

            MessagingUtils.sendBroadcasts(context, intent, action);
            // if shouldHandleTracking is true in the intent extras then handle the push notification interaction tracking (clicked, deleted, button clicked)
            if (intent.getExtras().getBoolean(MessagingConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)) {
                Log.trace(LOG_TAG, "handleAction() - Handling notification interaction tracking.");
                handlePushInteraction();
            }
            // handle button presses
            if (action.equals(MessagingPushPayload.ACTION_KEY.ACTION_BUTTON_CLICKED)) {
                Log.trace(LOG_TAG, "handleAction() - Notification button pressed with action (%s).", action);
                handleNotificationButtonPress();
            }
        }

        /**
         * Handles the specified button press action.
         */
        private void handleNotificationButtonPress() {
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            final Bundle extras = intent.getExtras();
            final String actionType = extras.getString(ACTION_BUTTON_TYPE_KEY);

            // dismiss the message
            final int notificationId = extras.getInt(NOTIFICATION_ID);
            Log.trace(LOG_TAG, "handleNotificationButtonPress() - Dismissing the message.");
            notificationManager.cancel(notificationId);

            // broadcast an intent with an action to collapse the notification drawer
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

            // handle the button press
            final String url = extras.getString(ACTION_BUTTON_LINK_KEY);
            final Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            switch (actionType) {
                case ActionButtonType.WEBURL:
                    Log.trace(LOG_TAG, "handleNotificationButtonPress() - showing url (%s) using UIService.", url);
                    if (!StringUtils.isNullOrEmpty(url)) {
                        MobileCore.getCore().eventHub.getPlatformServices().getUIService().showUrl(url);
                    }
                    break;
                case ActionButtonType.DEEPLINK:
                    Log.trace(LOG_TAG, "handleNotificationButtonPress() - opening the app with deeplink (%s).", url);
                    launchIntent.setData(Uri.parse(url));
                    context.startActivity(launchIntent);
                    break;
                case ActionButtonType.OPENAPP:
                default:
                    Log.trace(LOG_TAG, "handleNotificationButtonPress() - opening the app.", url);
                    context.startActivity(launchIntent);
                    break;
            }
        }

        /**
         * Sends the push interaction tracking information via the {@link Messaging#handleNotificationResponse(Intent, boolean, String)} API.
         */
        private void handlePushInteraction() {
            // if the push notification was deleted just send the interaction tracking request
            if (intent.getAction().equals(MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED)) {
                Messaging.handleNotificationResponse(intent, false, intent.getAction());
            } else { // otherwise do push tracking and open the app
                Messaging.handleNotificationResponse(intent, true, intent.getAction());
                final PackageManager pm = context.getPackageManager();
                final Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                try {
                    pendingIntent.send();
                } catch (final PendingIntent.CanceledException e) {
                    Log.warning(LOG_TAG, "handlePushInteraction() - exception occurred when sending pending intent: %s", e.getMessage());
                }
            }
        }
    }
}
