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
    static final String LOG_TAG = "Messaging";
    static final String SELF_TAG = "MessagingPushPayload";
    static final String TITLE = "adb_title";
    static final String BODY = "adb_body";
    static final String SOUND = "adb_sound";
    static final String NOTIFICATION_COUNT = "adb_n_count";
    static final String NOTIFICATION_PRIORITY = "adb_n_priority";
    static final String CHANNEL_ID = "adb_channel_id";
    static final String ICON = "adb_icon";
    static final String IMAGE_URL = "adb_image";
    static final String ACTION_TYPE = "adb_a_type";
    static final String ACTION_URI = "adb_uri";
    static final String ACTION_BUTTONS = "adb_act";

    static final class NotificationPriorities {
        static final String PRIORITY_DEFAULT = "PRIORITY_DEFAULT";
        static final String PRIORITY_MIN = "PRIORITY_MIN";
        static final String PRIORITY_LOW = "PRIORITY_LOW";
        static final String PRIORITY_HIGH = "PRIORITY_HIGH";
        static final String PRIORITY_MAX = "PRIORITY_MAX";
    }

    static final class ActionButtonType {
        static final String DEEPLINK = "DEEPLINK";
        static final String WEBURL = "WEBURL";
        static final String DISMISS = "DISMISS";
        static final String OPENAPP = "OPENAPP";
    }

    static final class ActionButtons {
        static final String LABEL = "label";
        static final String URI = "uri";
        static final String TYPE = "type";
    }

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
        this.title = data.get(TITLE);
        this.body = data.get(BODY);
        this.sound = data.get(SOUND);
        this.channelId = data.get(CHANNEL_ID);
        this.icon = data.get(ICON);
        this.actionUri = data.get(ACTION_URI);
        this.imageUrl = data.get(IMAGE_URL);

        try {
            String count = data.get(NOTIFICATION_COUNT);
            if (count != null) {
                this.badgeCount = Integer.parseInt(count);
            }
        } catch (NumberFormatException e) {
            Log.debug(LOG_TAG, SELF_TAG, "Exception in converting notification count to int - %s", e.getLocalizedMessage());
        }

        this.notificationPriority = getNotificationPriorityFromString(data.get(NOTIFICATION_PRIORITY));
        this.actionType = getActionTypeFromString(data.get(ACTION_TYPE));
        this.actionButtons = getActionButtonsFromString(data.get(ACTION_BUTTONS));
    }

    private int getNotificationPriorityFromString(final String priority) {
        if (priority == null) return Notification.PRIORITY_DEFAULT;
        switch (priority) {
            case NotificationPriorities.PRIORITY_MIN:
                return Notification.PRIORITY_MIN;
            case NotificationPriorities.PRIORITY_LOW:
                return Notification.PRIORITY_LOW;
            case NotificationPriorities.PRIORITY_HIGH:
                return Notification.PRIORITY_HIGH;
            case NotificationPriorities.PRIORITY_MAX:
                return Notification.PRIORITY_MAX;
            case NotificationPriorities.PRIORITY_DEFAULT:
            default:
                return Notification.PRIORITY_DEFAULT;
        }
    }

    private ActionType getActionTypeFromString(final String type) {
        if (StringUtils.isNullOrEmpty(type)) {
            return ActionType.NONE;
        }

        switch (type) {
            case ActionButtonType.DEEPLINK: return ActionType.DEEPLINK;
            case ActionButtonType.WEBURL: return ActionType.WEBURL;
            case ActionButtonType.DISMISS: return ActionType.DISMISS;
            case ActionButtonType.OPENAPP: return ActionType.OPENAPP;
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
            final String label = jsonObject.getString(ActionButtons.LABEL);
            if (label.isEmpty()) {
                Log.debug(LOG_TAG, SELF_TAG, "Label is empty");
                return null;
            }
            final String uri = jsonObject.getString(ActionButtons.URI);
            final String type = jsonObject.getString(ActionButtons.TYPE);

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
