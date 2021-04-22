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

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create push notification payload object from remote message.
 * It provides with functions for getting attributes of push payload (title, body, actions etc ...)
 */
public class AEPMessagingFCMPushPayload {
    private final String  SELF_TAG = "AEPMessagingFCMPushPayload";

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
    public AEPMessagingFCMPushPayload(final RemoteMessage message) {
        if (message == null) {
            Log.error(MessagingConstant.LOG_TAG, "%s - Failed to create AEPMessagingFCMPushPayload, remote message is null", SELF_TAG);
            return;
        }
        if (message.getData().isEmpty()) {
            Log.error(MessagingConstant.LOG_TAG, "%s - Failed to create AEPMessagingFCMPushPayload, remote message data payload is null", SELF_TAG);
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
    public AEPMessagingFCMPushPayload(final Map<String, String> data) {
        init(data);
    }

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

    /**
     * @return an {@link ActionType}
     */
    public ActionType getActionType() {
        return actionType;
    }

    public String getActionUri() {
        return actionUri;
    }

    /**
     * Returns list of action buttons which provides label, action type and action link
     * @return List of {@link ActionButton}
     */
    public List<ActionButton> getActionButtons() {
        return actionButtons;
    }

    public Map<String, String> getData() {
        return data;
    }

    private void init(final Map<String, String> data) {
        this.data = data;
        if (data == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Payload extraction failed because data provided is null");
            return;
        }
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
            Log.debug(MessagingConstant.LOG_TAG, "%s - Exception in converting string %s to int", SELF_TAG);
        }

        this.notificationPriority = getNotificationPriorityFromString(data.get(MessagingConstant.PushNotificationPayload.NOTIFICATION_PRIORITY));
        this.actionType = getActionTypeFromString(data.get(MessagingConstant.PushNotificationPayload.ACTION_TYPE));
        this.actionButtons = getActionButtonsFromString(data.get(MessagingConstant.PushNotificationPayload.ACTION_BUTTONS));
    }

    private int getNotificationPriorityFromString(final String priority) {
        if (priority == null) return Notification.PRIORITY_DEFAULT;
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

    private ActionType getActionTypeFromString(final String type) {
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
            Log.debug(MessagingConstant.LOG_TAG, "%s - Exception in converting actionButtons json string to json object, Error : actionButtons is null", SELF_TAG);
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
            Log.debug(MessagingConstant.LOG_TAG, "%s - Exception in converting actionButtons json string to json object, Error : %s", SELF_TAG, e.getMessage());
            return null;
        }
        return actionButtonList;
    }

    private ActionButton getActionButton(final JSONObject jsonObject) {
        try {
            String label = jsonObject.getString(MessagingConstant.PushNotificationPayload.ActionButtons.LABEL);
            if (label.isEmpty()) {
                Log.debug(MessagingConstant.LOG_TAG, "%s - Label is empty", SELF_TAG);
                return null;
            }
            String uri = jsonObject.getString(MessagingConstant.PushNotificationPayload.ActionButtons.URI);
            String type = jsonObject.getString(MessagingConstant.PushNotificationPayload.ActionButtons.TYPE);

            return new ActionButton(label, uri, type);
        } catch (JSONException e) {
            Log.debug(MessagingConstant.LOG_TAG, "%s - Exception in converting actionButtons json string to json object, Error : %s", SELF_TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Enum to denote the type of action
     */
    public enum ActionType {
        DEEPLINK, WEBURL, DISMISS, NONE
    }

    /**
     * Class representing the action button with label, link and type
     */
    public class ActionButton {
        private final String label;
        private final String link;
        private final ActionType type;

        public ActionButton(final String label, final String link, final String type) {
            this.label = label;
            this.link = link;
            this.type = getActionTypeFromString(type);
        }

        public String getLabel() {
            return label;
        }

        public String getLink() {
            return link;
        }

        public ActionType getType() {
            return type;
        }
    }
}
