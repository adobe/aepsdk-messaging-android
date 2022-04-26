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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import java.util.List;

public class MessagingPushReceiver extends BroadcastReceiver {
    private static String LOG_TAG = "MessagingPushReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if(context == null || intent == null) {
            Log.warning(LOG_TAG, "onReceive() - Intent or Context null, ignoring this broadcast message");
            return;
        }
        PushActionHandlingRunnable runnable = new PushActionHandlingRunnable(context, intent);
        new Thread(runnable).start();
    }

    static class PushActionHandlingRunnable implements Runnable {
        Context context;
        Intent intent;
        PushActionHandlingRunnable(Context context, Intent intent) {
            this.context = context;
            this.intent = intent;
        }

        @Override
        public void run() {
            handleAction();
        }

        void handleAction() {
            // Check if its a push display notification or silent notification
            String action = intent.getAction();


            if(StringUtils.isNullOrEmpty(action)) {
                Log.warning(LOG_TAG,"handleAction() - Empty/Null action. Not processing this broadcast message");
                return;
            }

            sendBroadcasts(context, intent, action);
        }

        private void sendBroadcasts(final Context context, final Intent intent, final String action) {
            final String packageName = context.getPackageName();
            final String broadcastingAction = packageName + "_" + action;
            final Intent sendIntent = new Intent();
            final Bundle extras = intent.getExtras();
            sendIntent.setAction(broadcastingAction);
            sendIntent.putExtras(extras);
            final List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(sendIntent, 0);
            if (receivers.isEmpty()) {
                // log error no component
            } else {
                for (ResolveInfo receiver : receivers) {
                    Intent broadcastIntent = new Intent(sendIntent);
                    String classInfo = receiver.activityInfo.name;
                    ComponentName name = new ComponentName(packageName, classInfo);
                    broadcastIntent.setComponent(name);
                    context.sendBroadcast(broadcastIntent);
                }
            }

            // if shouldHandleTracking is true in the extras then send the push notification tracking
            if (extras.getBoolean(MessagingConstant.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, false)) {
                handlePushInteraction(context, intent);
            }
        }

        private void handlePushInteraction(final Context context, final Intent intent) {
            // if the push notification was deleted just send the interaction tracking request
            if (intent.getAction().equals(MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED)) {
                Messaging.handleNotificationResponse(intent, false, intent.getAction());
            } else { // otherwise do push tracking and open the app
                Messaging.handleNotificationResponse(intent, true, intent.getAction());
                final PackageManager pm = context.getPackageManager();
                final Intent launchIntent = pm.getLaunchIntentForPackage(context.getPackageName());
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_ONE_SHOT);
                try {
                    pendingIntent.send();
                } catch (final PendingIntent.CanceledException e) {
                    Log.warning(LOG_TAG,"openApplication() - exception occured when sending pending intent: %s", e.getMessage());
                }
            }
        }
    }
}
