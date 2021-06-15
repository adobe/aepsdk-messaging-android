package com.adobe.marketing.mobile;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.ResolveInfo;

import java.util.Iterator;
import java.util.List;

public class MessagingPushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(context == null || intent == null) {
            Log.warning(MessagingConstant.LOG_TAG, "onReceive() - Intent or Context null, ignoring this broadcast message");
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
                Log.warning(MessagingConstant.LOG_TAG,"handleAction() - Empty/Null action. Not processing this broadcast message");
                return;
            }

            switch (action) {
                case MessagingConstant.PushNotificationPayload.ACTIONS.ACTION_NOTIFICATION_CLICKED:
                    sendBroadcasts(context, intent, MessagingConstant.PushNotificationPayload.ACTIONS.ACTION_NOTIFICATION_CLICKED);
                    break;

                case MessagingConstant.PushNotificationPayload.ACTIONS.ACTION_BUTTON_CLICKED:
                    sendBroadcasts(context, intent, MessagingConstant.PushNotificationPayload.ACTIONS.ACTION_BUTTON_CLICKED);
                    break;

                default:
                    Log.warning(MessagingConstant.LOG_TAG, "handleAction() - Ignoring broadcast. Broadcast message is not for Adobe messaging sdk.");
                    break;
            }
        }

        private void sendBroadcasts(final Context context, final Intent intent, final String action) {
            String packageName = context.getPackageName();
            String broadcastingAction = packageName + "_" + action;
            Intent sendIntent = new Intent(broadcastingAction);
            sendIntent.putExtras(intent.getExtras());
            List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(intent, 0);
            if (receivers.isEmpty()) {
                // log error no component
            } else {
                for (ResolveInfo receiver : receivers) {
                    Intent intent1 = new Intent(intent);
                    String classInfo = receiver.activityInfo.name;
                    ComponentName name = new ComponentName(packageName, classInfo);
                    intent1.setComponent(name);
                    context.sendBroadcast(intent1);
                }
            }
        }
    }
}
