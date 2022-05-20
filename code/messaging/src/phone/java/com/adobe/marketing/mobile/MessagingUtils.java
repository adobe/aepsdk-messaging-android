/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

class MessagingUtils {
    /* JSON - Map conversion helpers */

    /**
     * Converts provided {@link org.json.JSONObject} into {@link java.util.Map} for any number of levels, which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonObject to be converted
     * @return {@link java.util.Map} containing the elements from the provided json, null if {@code jsonObject} is null
     */
    static Map<String, Object> toMap(final JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        Map<String, Object> jsonAsMap = new HashMap<>();
        Iterator<String> keysIterator = jsonObject.keys();

        if (keysIterator == null) return null;

        while (keysIterator.hasNext()) {
            String nextKey = keysIterator.next();
            jsonAsMap.put(nextKey, fromJson(jsonObject.get(nextKey)));
        }

        return jsonAsMap;
    }

    /**
     * Converts provided {@link JSONArray} into {@link List} for any number of levels which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonArray to be converted
     * @return {@link List} containing the elements from the provided json, null if {@code jsonArray} is null
     */
    static List<Object> toList(final JSONArray jsonArray) throws JSONException {
        if (jsonArray == null) {
            return null;
        }

        List<Object> jsonArrayAsList = new ArrayList<>();
        int size = jsonArray.length();

        for (int i = 0; i < size; i++) {
            jsonArrayAsList.add(fromJson(jsonArray.get(i)));
        }

        return jsonArrayAsList;
    }

    /**
     * Returns the notification channel id if present in the {@link MessagingPushPayload}.
     * A default channel id is returned if no channel id is present in the {@code MessagingPushPayload}.
     *
     * @param context the application {@link Context}
     * @param payload the {@code MessagingPushPayload} containing the data payload from AJO
     * @return {@code String} containing the notification channel id to use for a {@link Notification}
     */
    static String getChannelId(final Context context, final MessagingPushPayload payload) {
        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (payload == null || payload.getChannelId() == null || payload.getChannelId().isEmpty()) {
            createDefaultNotificationChannel(context, notificationManager, payload);
            return MessagingConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return payload.getChannelId();
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(payload.getChannelId()) != null) {
                return payload.getChannelId();
            } else {
                createDefaultNotificationChannel(context, notificationManager, payload);
            }
        }

        return MessagingConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID;
    }

    /**
     * Returns a {@link PendingIntent} containing push tracking details.
     *
     * @param context              the application {@link Context}
     * @param payload              the {@code MessagingPushPayload} containing the data payload from AJO
     * @param messageId            the {@code String} message id
     * @param action               a {@code String} containing the notification interaction which occurred
     * @param shouldHandleTracking {@code boolean} if true the AEPMessaging extension will handle notification interaction tracking
     * @return {@code PendingIntent} for recording {@link Notification} interactions
     */
    static PendingIntent getPendingIntentForAction(final Context context, final MessagingPushPayload payload, final String messageId, final String action, final boolean shouldHandleTracking) {
        final Bundle extras = getBundleFromMap(payload.getData());
        extras.putBoolean(MessagingConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, shouldHandleTracking);
        final Intent intent = new Intent(context, MessagingPushReceiver.class);
        intent.setAction(action);
        intent.putExtras(extras);
        // Adding CJM specific details
        Messaging.addPushTrackingDetails(intent, messageId, payload.getData());
        return PendingIntent.getBroadcast(context, MessagingConstants.PushNotificationPayload.REQUEST_CODES.PUSH_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    /**
     * Returns the small icon resource id.
     *
     * @param context the application {@link Context}
     * @return {@code int} containing the small icon resource id
     */
    static int getDefaultSmallIconRes(final Context context) {
        final String packageName = context.getPackageName();
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).icon;
        } catch (final PackageManager.NameNotFoundException e) {
            Log.debug(LOG_TAG, "Exception occurred when retrieving the small icon resource id: %s", e.getMessage());
        }
        return MessagingPushNotificationFactory.INVALID_SMALL_ICON_RES_ID;
    }

    /**
     * Returns a {@link Bitmap} created from a remote asset.
     *
     * @param context  the application {@link Context}
     * @param assetUrl the {@code String} containing the asset url to be downloaded
     * @return the created {@code Bitmap}
     */
    static Bitmap getImageAsset(final Context context, final String assetUrl) {
        final IMessagingImageDownloader downloader = Messaging.getImageDownloader();
        if (downloader == null) {
            Log.warning(LOG_TAG, "The MessagingImageDownloader instance is null. Ensure that the Messaging extension has been registered.");
            return null;
        }
        return downloader.getBitmapFromUrl(context, assetUrl);
    }

    /**
     * Returns a {@link Uri} containing the location of a local audio file to use when displaying a push {@link Notification}.
     *
     * @param context  the application {@link Context}
     * @param fileName the {@code String} containing the name of an audio file to use for a displayed notification
     * @return {@code Uri} containing the local location of an audio file o use for a displayed {@code Notification}
     */
    static Uri getSoundUri(final Context context, final String fileName) {
        final int resID = context.getResources().getIdentifier(fileName, "raw", context.getPackageName());
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + resID);
    }

    /**
     * Prepares a {@link Notification.Action} to be performed when a {@link Notification} button is pressed.
     *
     * @param context              the application {@link Context}
     * @param button               the {@link MessagingPushPayload.ActionButton} which triggers the {@code Notification.Action}
     * @param payload              the {@code MessagingPushPayload} containing the data payload from AJO
     * @param notificationBuilder  the {@link Notification.Builder} object currently being used to build the notification
     * @param messageId            the {@code String} message id
     * @param notificationId       {@code int} used when scheduling the notification
     * @param shouldHandleTracking {@code boolean} if true the AEPMessaging extension will handle notification interaction tracking
     */
    static void addAction(final Context context, final MessagingPushPayload.ActionButton button, final MessagingPushPayload payload, final Notification.Builder notificationBuilder, final String messageId, final int notificationId, final boolean shouldHandleTracking) {
        // setup bundle extras to be sent with the intent
        final Bundle extras = getBundleFromMap(payload.getData());
        extras.putString(MessagingPushPayload.ACTION_BUTTON_KEY.LABEL, button.getLabel());
        extras.putString(MessagingPushPayload.ACTION_BUTTON_KEY.LINK, button.getLink());
        extras.putString(MessagingPushPayload.ACTION_BUTTON_KEY.TYPE, button.getType().name());
        extras.putInt(MessagingConstants.PushNotificationPayload.NOTIFICATION_ID, notificationId);
        extras.putBoolean(MessagingConstants.PushNotificationPayload.HANDLE_NOTIFICATION_TRACKING_KEY, shouldHandleTracking);

        // create the intent and add the bundle
        final Intent intent = new Intent(context, MessagingPushReceiver.class);
        intent.setAction(MessagingPushPayload.ACTION_KEY.ACTION_BUTTON_CLICKED);
        intent.putExtras(extras);

        // Adding CJM specific details
        // Use a random request code when creating the Pending Intent to make each one unique
        final int requestCode = new Random().nextInt();
        Messaging.addPushTrackingDetails(intent, messageId, payload.getData());
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
        final Notification.Action action = new Notification.Action(0, button.getLabel(), pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.addAction(action);
        }
    }

    /**
     * Broadcasts a {@link Intent} containing a notification event (creation, deletion, or interaction). The receivers must be defined
     * in the app manifest to correctly receive the broadcast e.g.:
     * <receiver
     *      android:name=".NotificationBroadcastReceiver"
     *      android:exported="false">
     *          <intent-filter>
     *              <action android:name="${applicationId}_adb_action_notification_clicked" />
     *              <action android:name="${applicationId}_adb_action_button_clicked" />
     *              <action android:name="${applicationId}_adb_action_notification_deleted" />
     *              <action android:name="${applicationId}_adb_action_notification_created" />
     *              <action android:name="${applicationId}_adb_action_silent_notification_created" />
     *          </intent-filter>
     * </receiver>
     *
     * @param context the application {@link Context}
     * @param intent  the {@code Intent} to be performed by the {@link android.content.BroadcastReceiver}
     * @param action  a {@code String} containing the action to be broadcast
     */
    static void sendBroadcasts(final Context context, final Intent intent, final String action) {
        final String packageName = context.getPackageName();
        final String broadcastingAction = packageName + "_" + action;
        final Intent sendIntent = new Intent();
        sendIntent.setAction(broadcastingAction);
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            sendIntent.putExtras(intent.getExtras());
        }
        final List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(sendIntent, 0);
        if (receivers.isEmpty()) {
            Log.warning(LOG_TAG, "Will not broadcast an intent for action (%s), no BroadcastReceivers were found.", broadcastingAction);
        } else {
            for (final ResolveInfo receiver : receivers) {
                final Intent broadcastIntent = new Intent(sendIntent);
                final String classInfo = receiver.activityInfo.name;
                final ComponentName name = new ComponentName(packageName, classInfo);
                broadcastIntent.setComponent(name);
                context.sendBroadcast(broadcastIntent);
            }
        }
    }

    /**
     * Creates a default notification channel if the {@link MessagingPushPayload} does not specify one.
     *
     * @param context             the application {@link Context}
     * @param notificationManager the {@link NotificationManager} instance
     * @param payload             the {@code MessagingPushPayload} containing the data payload from AJO
     */
    private static void createDefaultNotificationChannel(final Context context, final NotificationManager notificationManager, final MessagingPushPayload payload) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(MessagingConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID) == null) {
                final int importance = getImportance(payload.getNotificationPriority(), notificationManager);
                final NotificationChannel channel = new NotificationChannel(MessagingConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID,
                        MessagingConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_NAME,
                        importance);
                channel.setDescription(MessagingConstants.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_DESCRIPTION);
                setSound(context, channel, payload.getSound());
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Converts the {@link Notification} priority to a {@link NotificationManager} importance.
     * Notification priority must be converted to a Notification Manager importance for Android version >= O.
     *
     * @param priority            the {@code int} priority to be converted
     * @param notificationManager the {@link NotificationManager} instance
     * @return the equivalent {@code NotificationManager} importance
     */
    private static int getImportance(final int priority, final NotificationManager notificationManager) {
        switch (priority) {
            case Notification.PRIORITY_DEFAULT:
                return NotificationManager.IMPORTANCE_DEFAULT;
            case Notification.PRIORITY_MIN:
                return NotificationManager.IMPORTANCE_MIN;
            case Notification.PRIORITY_LOW:
                return NotificationManager.IMPORTANCE_LOW;
            case Notification.PRIORITY_HIGH:
                return NotificationManager.IMPORTANCE_HIGH;
            case Notification.PRIORITY_MAX:
                return NotificationManager.IMPORTANCE_MAX;
            default:
                return NotificationManager.IMPORTANCE_UNSPECIFIED;
        }
    }

    /**
     * Sets the local file as the {@link Notification} displayed sound for the specified {@link NotificationChannel}.
     *
     * @param context  the application {@link Context}
     * @param channel  the {@code NotificationChannel} to be assigned a sound
     * @param fileName the {@code String} containing the name of an audio file to use for the {@code NotificationChannel}
     */
    private static void setSound(final Context context, final NotificationChannel channel, final String fileName) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (StringUtils.isNullOrEmpty(fileName)) {
                Log.debug(LOG_TAG, "Will not set a custom sound for notification channel {%S). The provided file name is invalid.", channel.getName());
                return;
            }
            final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(getSoundUri(context, fileName), audioAttributes);
        }
    }

    /**
     * Converts provided {@link Map<String, String>} to a {@link Bundle} object.
     *
     * @param dataMap the {@code Map<String, String>} to be converted
     * @return a {@code Bundle} containing the map data
     */
    private static Bundle getBundleFromMap(final Map<String, String> dataMap) {
        final Bundle bundle = new Bundle();
        if (dataMap == null || dataMap.isEmpty()) {
            return bundle;
        }
        for (final Map.Entry<String, String> entry : dataMap.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    /**
     * Converts provided {@link JSONObject} to a {@link Map} or {@link JSONArray} into a {@link List}.
     *
     * @param json to be converted
     * @return {@link Object} converted from the provided json object.
     */
    private static Object fromJson(final Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else if (json instanceof JSONArray) {
            return toList((JSONArray) json);
        } else {
            return json;
        }
    }
}
