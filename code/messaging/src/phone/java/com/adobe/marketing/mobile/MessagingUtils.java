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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    static String getChannelId(Context context, MessagingPushPayload payload) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (payload == null || payload.getChannelId() == null || payload.getChannelId().isEmpty()) {
            createDefaultNotificationChannel(context, notificationManager, payload);
            return MessagingConstant.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID;
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

        return MessagingConstant.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID;
    }

    static PendingIntent getPendingIntentForAction(Context context, MessagingPushPayload payload, String messageId, String action) {
        Bundle extras = getBundleFromMap(payload.getData());
        Intent intent = new Intent(context, MessagingPushReceiver.class);
        intent.setAction(action);
        intent.putExtras(extras);
        // Adding CJM specific details
        Messaging.addPushTrackingDetails(intent, messageId, payload.getData());
        return PendingIntent.getBroadcast(context, MessagingConstant.PushNotificationPayload.REQUEST_CODES.PUSH_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    static int getDefaultSmallIconRes(Context context) {
        String packageName = context.getPackageName();
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(MessagingConstant.LOG_TAG, e.getMessage());
        }
        return MessagingPushNotificationFactory.INVALID_SMALL_ICON_RES_ID;
    }

    static Bitmap getLargeIcon(Context context, MessagingPushPayload payload) {
        String iconUrl = payload.getIcon();
        IMessagingImageDownloader downloader = Messaging.getImageDownloader();
        if (downloader == null) {
            // log that messaging extension is not registered
            return null;
        }
        return downloader.getBitmapFromUrl(context, iconUrl);
    }

    static Uri getSoundUri(Context context, String fileName) {
        int resID = context.getResources().getIdentifier(fileName, "raw", context.getPackageName());
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + resID);
    }

    static void addAction(Context context, MessagingPushPayload.ActionButton button, MessagingPushPayload payload, Notification.Builder notificationBuilder, String messageId) {
        Bundle extras = getBundleFromMap(payload.getData());
        extras.putString(MessagingPushPayload.ACTION_BUTTON_KEY.LABEL, button.getLabel());
        extras.putString(MessagingPushPayload.ACTION_BUTTON_KEY.LINK, button.getLink());
        extras.putString(MessagingPushPayload.ACTION_BUTTON_KEY.TYPE, button.getType().name());
        Intent intent = new Intent(context, MessagingPushReceiver.class);
        intent.setAction(MessagingPushPayload.ACTION_KEY.ACTION_BUTTON_CLICKED);
        intent.putExtras(extras);
        // Adding CJM specific details
        Messaging.addPushTrackingDetails(intent, messageId, payload.getData());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, MessagingConstant.PushNotificationPayload.REQUEST_CODES.PUSH_INTENT_REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT);
        Notification.Action action = new Notification.Action(0, button.getLabel(), pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.addAction(action);
        }
    }

    private static void createDefaultNotificationChannel(Context context, NotificationManager notificationManager, MessagingPushPayload payload) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(MessagingConstant.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID) == null) {
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(MessagingConstant.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_ID,
                        MessagingConstant.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_NAME,
                        importance);
                channel.setDescription(MessagingConstant.PushNotificationPayload.DEFAULTS.DEFAULT_CHANNEL_DESCRIPTION);
                setSound(context, channel, payload.getSound());
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Need to be implemented - to map priority to importance for version >= O
    private static void setImportance(Context context, NotificationChannel channel, String priority) {
        // todo
    }

    private static void setSound(Context context, NotificationChannel channel, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(getSoundUri(context, fileName), audioAttributes);
        }
    }

    private static Bundle getBundleFromMap(Map<String, String> dataMap) {
        Bundle bundle = new Bundle();
        if (dataMap == null || dataMap.isEmpty()) {
            return bundle;
        }
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
        return bundle;
    }

    private static Object fromJson(Object json) throws JSONException {
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
