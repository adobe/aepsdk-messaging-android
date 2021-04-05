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
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create push notification payload object from remote message.
 * It provides with functions for getting attributes of push payload (title, body ...)
 */
public class AEPMessagingFCMPushPayload {
    private String title;
    private String body;
    private String sound;
    private int badgeCount;
    private int notificationPriority;
    private String channelId;
    private String icon;
    private String imageUrl;
    private ActionType actionType;
    private String actionUri;
    private List<ActionButton> actionButtons = new ArrayList<>(3);
    private Map<String, String> data;

    /**
     * Constructor
     *
     * Provides the AEPMessagingFCMPushPayload object
     * @param message {@link RemoteMessage} object received from {@link com.google.firebase.messaging.FirebaseMessagingService}
     */
    public AEPMessagingFCMPushPayload(RemoteMessage message) {
        if (message == null) {
            Log.error(MessagingConstant.LOG_TAG, "Failed to create AEPMessagingFCMPushPayload, remote message is null");
            return;
        }
        if (message.getData().isEmpty()) {
            Log.error(MessagingConstant.LOG_TAG, "Failed to create AEPMessagingFCMPushPayload, remote message data payload is null");
            return;
        }
        init(message.getData());
    }

    /**
     * Constructor
     *
     * Provides the AEPMessagingFCMPushPayload object
     * @param data {@link Map} map which indicates the data part of {@link RemoteMessage}
     */
    public AEPMessagingFCMPushPayload(Map<String, String> data) {
        init(data);
    }

    /**
     *
     * @return
     */
    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getSound() {
        return sound;
    }

    public int getBadgeCount() {
        return badgeCount;
    }

    public int getNotificationPriority() {
        return notificationPriority;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getIcon() {
        return icon;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getActionUri() {
        return actionUri;
    }

    public List<ActionButton> getActionButtons() {
        return actionButtons;
    }

    public Map<String, String> getData() {
        return data;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public int getImportance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (notificationPriority) {
                case Notification.PRIORITY_MIN: return NotificationManager.IMPORTANCE_MIN;
                case Notification.PRIORITY_LOW: return NotificationManager.IMPORTANCE_LOW;
                case Notification.PRIORITY_HIGH: return NotificationManager.IMPORTANCE_HIGH;
                case Notification.PRIORITY_MAX: return NotificationManager.IMPORTANCE_MAX;
                case Notification.PRIORITY_DEFAULT: return NotificationManager.IMPORTANCE_DEFAULT;
                default: return NotificationManager.IMPORTANCE_NONE;
            }
        }
        return NotificationManager.IMPORTANCE_DEFAULT;
    }

    private void init(Map<String, String> data) {
        this.data = data;
        this.title = data.get(MessagingConstant.PushNotificationPayload.TITLE);
        this.body = data.get(MessagingConstant.PushNotificationPayload.BODY);
        this.sound = data.get(MessagingConstant.PushNotificationPayload.SOUND);
        this.channelId = data.get(MessagingConstant.PushNotificationPayload.CHANNEL_ID);
        this.icon = data.get(MessagingConstant.PushNotificationPayload.ICON);
        this.actionUri = data.get(MessagingConstant.PushNotificationPayload.ACTION_URI);
        this.imageUrl = data.get(MessagingConstant.PushNotificationPayload.IMAGE_URL);

        try {
            String count = data.get(MessagingConstant.PushNotificationPayload.NOTIFICATION_COUNT);
            this.badgeCount = Integer.parseInt(count);
        } catch (NumberFormatException e) {
            Log.debug(MessagingConstant.LOG_TAG, "Exception in converting string %s to int");
        }

        this.notificationPriority = getNotificationPriorityFromString(data.get(MessagingConstant.PushNotificationPayload.NOTIFICATION_PRIORITY));
        this.actionType = getActionTypeFromString(data.get(MessagingConstant.PushNotificationPayload.ACTION_TYPE));
        this.actionButtons = getActionButtonsFromString(data.get(MessagingConstant.PushNotificationPayload.ACTION_BUTTONS));
    }

    private int getNotificationPriorityFromString(String priority) {
        switch (priority) {
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_MIN: return Notification.PRIORITY_MIN;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_LOW: return Notification.PRIORITY_LOW;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_HIGH: return Notification.PRIORITY_HIGH;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_MAX: return Notification.PRIORITY_MAX;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_DEFAULT:
            default: return Notification.PRIORITY_DEFAULT;
        }
    }

    private ActionType getActionTypeFromString(String type) {
        if (type == null || type.isEmpty()) {
            return ActionType.NONE;
        }

        switch (type) {
            case MessagingConstant.PushNotificationPayload.ActionButtonType.DEEPLINK: return ActionType.DEEPLINK;
            case MessagingConstant.PushNotificationPayload.ActionButtonType.WEBURL: return ActionType.WEBURL;
            case MessagingConstant.PushNotificationPayload.ActionButtonType.DISMISS: return ActionType.DISMISS;
        }

        return ActionType.NONE;
    }

    private List<ActionButton> getActionButtonsFromString(final String actionButtons) {
        if (actionButtons == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Exception in converting actionButtons json string to json object, Error : actionButtons is null");
            return null;
        }
        List<ActionButton> actionButtonList = new ArrayList<>(3);
        try {
            JSONArray jsonArray = new JSONArray(actionButtons);
            for (int i=0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ActionButton button = getActionButton(jsonObject);
                if (button == null) continue;
                actionButtonList.add(button);
            }

        } catch (JSONException e) {
            Log.debug(MessagingConstant.LOG_TAG, "Exception in converting actionButtons json string to json object, Error : ", e.getMessage());
            return null;
        }
        return actionButtonList;
    }

    private ActionButton getActionButton(JSONObject jsonObject) {
        try {
            String label = jsonObject.getString(MessagingConstant.PushNotificationPayload.ActionButtons.LABEL);
            if (label.isEmpty()) {
                Log.debug(MessagingConstant.LOG_TAG, "Label is empty");
                return null;
            }
            String uri = jsonObject.getString(MessagingConstant.PushNotificationPayload.ActionButtons.URI);
            String type = jsonObject.getString(MessagingConstant.PushNotificationPayload.ActionButtons.TYPE);

            return new ActionButton(label, uri, type);
        } catch (JSONException e) {
            Log.debug(MessagingConstant.LOG_TAG, "Exception in converting actionButtons json string to json object, Error : ", e.getMessage());
            return null;
        }
    }

    enum ActionType {
        DEEPLINK, WEBURL, DISMISS, NONE
    }

    class ActionButton {
        private final String label;
        private final String uri;
        private final ActionType type;

        public ActionButton(final String label, final String uri, final String type) {
            this.label = label;
            this.uri = uri;
            this.type = getActionTypeFromString(type);
        }

        public String getLabel() {
            return label;
        }

        public String getUri() {
            return uri;
        }

        public ActionType getType() {
            return type;
        }
    }
}
