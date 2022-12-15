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

package com.adobe.marketing.mobile.messaging;

import android.app.Notification;

import static com.adobe.marketing.mobile.messaging.MessagingConstants.LOG_TAG;
import com.adobe.marketing.mobile.util.StringUtils;

import com.adobe.marketing.mobile.services.Log;
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
public class MessagingPushPayload {
    private final String SELF_TAG = "MessagingPushPayload";

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
     * <p>
     * Provides the MessagingPushPayload object
     *
     * @param message {@link RemoteMessage} object received from {@link com.google.firebase.messaging.FirebaseMessagingService}
     */
    public MessagingPushPayload(final RemoteMessage message) {
        if (message == null) {
            Log.error(LOG_TAG, SELF_TAG, "Failed to create MessagingPushPayload, remote message is null");
            return;
        }
        if (message.getData().isEmpty()) {
            Log.error(LOG_TAG, SELF_TAG, "Failed to create MessagingPushPayload, remote message data payload is null");
            return;
        }
        init(message.getData());
    }

    /**
     * Constructor
     * <p>
     * Provides the MessagingPushPayload object
     *
     * @param data {@link Map} map which indicates the data part of {@link RemoteMessage}
     */
    public MessagingPushPayload(final Map<String, String> data) {
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
     *
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
            Log.debug(LOG_TAG, SELF_TAG, "Payload extraction failed because data provided is null");
            return;
        }
        this.title = data.get(MessagingConstants.PushNotificationPayload.TITLE);
        this.body = data.get(MessagingConstants.PushNotificationPayload.BODY);
        this.sound = data.get(MessagingConstants.PushNotificationPayload.SOUND);
        this.channelId = data.get(MessagingConstants.PushNotificationPayload.CHANNEL_ID);
        this.icon = data.get(MessagingConstants.PushNotificationPayload.ICON);
        this.actionUri = data.get(MessagingConstants.PushNotificationPayload.ACTION_URI);
        this.imageUrl = data.get(MessagingConstants.PushNotificationPayload.IMAGE_URL);

        try {
            String count = data.get(MessagingConstants.PushNotificationPayload.NOTIFICATION_COUNT);
            this.badgeCount = Integer.parseInt(count);
        } catch (NumberFormatException e) {
            Log.debug(MessagingConstants.LOG_TAG, "%s - Exception in converting string %s to int", SELF_TAG);
        }

        this.notificationPriority = getNotificationPriorityFromString(data.get(MessagingConstants.PushNotificationPayload.NOTIFICATION_PRIORITY));
        this.actionType = getActionTypeFromString(data.get(MessagingConstants.PushNotificationPayload.ACTION_TYPE));
        this.actionButtons = getActionButtonsFromString(data.get(MessagingConstants.PushNotificationPayload.ACTION_BUTTONS));
    }

    private int getNotificationPriorityFromString(final String priority) {
        if (priority == null) return Notification.PRIORITY_DEFAULT;
        switch (priority) {
            case MessagingConstants.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_MIN:
                return Notification.PRIORITY_MIN;
            case MessagingConstants.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_LOW:
                return Notification.PRIORITY_LOW;
            case MessagingConstants.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_HIGH:
                return Notification.PRIORITY_HIGH;
            case MessagingConstants.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_MAX:
                return Notification.PRIORITY_MAX;
            case MessagingConstants.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_DEFAULT:
            default:
                return Notification.PRIORITY_DEFAULT;
        }
    }

    private ActionType getActionTypeFromString(final String type) {
        if (StringUtils.isNullOrEmpty(type)) {
            return ActionType.NONE;
        }

        switch (type) {
            case MessagingConstants.PushNotificationPayload.ActionButtonType.DEEPLINK: return ActionType.DEEPLINK;
            case MessagingConstants.PushNotificationPayload.ActionButtonType.WEBURL: return ActionType.WEBURL;
            case MessagingConstants.PushNotificationPayload.ActionButtonType.DISMISS: return ActionType.DISMISS;
            case MessagingConstants.PushNotificationPayload.ActionButtonType.OPENAPP: return ActionType.OPENAPP;
        }

        return ActionType.NONE;
    }

    private List<ActionButton> getActionButtonsFromString(final String actionButtons) {
        if (actionButtons == null) {
            Log.debug(LOG_TAG, SELF_TAG, "Exception in converting actionButtons json string to json object, Error : actionButtons is null");
            return null;
        }
        List<ActionButton> actionButtonList = new ArrayList<>(3);
        try {
            final JSONArray jsonArray = new JSONArray(actionButtons);
            for (int i=0; i < jsonArray.length(); i++) {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);
                final ActionButton button = getActionButton(jsonObject);
                if (button == null) continue;
                actionButtonList.add(button);
            }
        } catch (final JSONException e) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception in converting actionButtons json string to json object, Error : %s", e.getLocalizedMessage());
            return null;
        }
        return actionButtonList;
    }

    private ActionButton getActionButton(final JSONObject jsonObject) {
        try {
            final String label = jsonObject.getString(MessagingConstants.PushNotificationPayload.ActionButtons.LABEL);
            if (label.isEmpty()) {
                Log.debug(LOG_TAG, SELF_TAG, "Label is empty");
                return null;
            }
            final String uri = jsonObject.getString(MessagingConstants.PushNotificationPayload.ActionButtons.URI);
            final String type = jsonObject.getString(MessagingConstants.PushNotificationPayload.ActionButtons.TYPE);

            Log.trace(LOG_TAG, SELF_TAG, "Creating an ActionButton with label (%s), uri (%s), and type (%s)", label, uri, type);
            return new ActionButton(label, uri, type);
        } catch (final JSONException e) {
            Log.warning(LOG_TAG, SELF_TAG, "Exception in converting actionButtons json string to json object, Error : %s", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Enum to denote the type of action
     */
    public enum ActionType {
        DEEPLINK, WEBURL, DISMISS, OPENAPP, NONE
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
