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

import com.adobe.marketing.mobile.messaging.MessagingConstants;
import com.adobe.marketing.mobile.util.StringUtils;

import com.adobe.marketing.mobile.services.Log;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create push notification payload object from remote message.
 * It provides with functions for getting attributes of push payload (title, body, actions etc ...)
 */
public class MessagingPushPayload {
    static final String SELF_TAG = "MessagingPushPayload";
    static final class NotificationPriorities {
        static final String PRIORITY_DEFAULT = "PRIORITY_DEFAULT";
        static final String PRIORITY_MIN = "PRIORITY_MIN";
        static final String PRIORITY_LOW = "PRIORITY_LOW";
        static final String PRIORITY_HIGH = "PRIORITY_HIGH";
        static final String PRIORITY_MAX = "PRIORITY_MAX";
    }

    static final class NotificationVisibility {
        static final String PUBLIC = "PUBLIC";
        static final String PRIVATE = "PRIVATE";
        static final String SECRET = "SECRET";
    }

    static final class ActionButtonType {
        static final String DEEPLINK = "DEEPLINK";
        static final String WEBURL = "WEBURL";
        static final String OPENAPP = "OPENAPP";
    }

    static final class ActionButtons {
        static final String LABEL = "label";
        static final String URI = "uri";
        static final String TYPE = "type";
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    static final Map<String,Integer> notificationImportanceMap = new HashMap<String,Integer>() {{
        put(NotificationPriorities.PRIORITY_MIN, NotificationManager.IMPORTANCE_MIN);
        put(NotificationPriorities.PRIORITY_LOW, NotificationManager.IMPORTANCE_LOW);
        put(NotificationPriorities.PRIORITY_DEFAULT, NotificationManager.IMPORTANCE_DEFAULT);
        put(NotificationPriorities.PRIORITY_HIGH, NotificationManager.IMPORTANCE_HIGH);
        put(NotificationPriorities.PRIORITY_MAX, NotificationManager.IMPORTANCE_MAX);
    }};


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static final Map<String,Integer> notificationVisibilityMap = new HashMap<String,Integer>() {{
        put(NotificationVisibility.PRIVATE, Notification.VISIBILITY_PRIVATE);
        put(NotificationVisibility.PUBLIC, Notification.VISIBILITY_PUBLIC);
        put(NotificationVisibility.SECRET, Notification.VISIBILITY_SECRET);
    }};

    static final Map<String,Integer> notificationPriorityMap = new HashMap<String,Integer>() {{
        put(NotificationPriorities.PRIORITY_MIN, Notification.PRIORITY_MIN);
        put(NotificationPriorities.PRIORITY_LOW, Notification.PRIORITY_LOW);
        put(NotificationPriorities.PRIORITY_DEFAULT, Notification.PRIORITY_DEFAULT);
        put(NotificationPriorities.PRIORITY_HIGH, Notification.PRIORITY_HIGH);
        put(NotificationPriorities.PRIORITY_MAX, Notification.PRIORITY_MAX);
    }};

    private static final int ACTION_BUTTON_CAPACITY = 3;
    private String title;
    private String body;
    private String sound;
    private int badgeCount;
    private int notificationPriority = Notification.PRIORITY_DEFAULT;
    private int notificationImportance = NotificationManager.IMPORTANCE_DEFAULT;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int notificationVisibility = Notification.VISIBILITY_PRIVATE;
    private String channelId;
    private String icon;
    private String imageUrl;
    private ActionType actionType;
    private String actionUri;
    private List<ActionButton> actionButtons = new ArrayList<>(ACTION_BUTTON_CAPACITY);
    private Map<String, String> data;
    private String messageId;

    /**
     * Constructor
     * <p>
     * Provides the MessagingPushPayload object
     *
     * @param message {@link RemoteMessage} object received from {@link com.google.firebase.messaging.FirebaseMessagingService}
     */
    public MessagingPushPayload(final RemoteMessage message) {
        if (message == null) {
            Log.error(MessagingConstants.LOG_TAG , SELF_TAG, "Failed to create MessagingPushPayload, remote message is null");
            return;
        }
        if (message.getData().isEmpty()) {
            Log.error(MessagingConstants.LOG_TAG, SELF_TAG, "Failed to create MessagingPushPayload, remote message data payload is null");
            return;
        }

        final String messageId = message.getMessageId();
        if(StringUtils.isNullOrEmpty(messageId)) {
            Log.error(MessagingConstants.LOG_TAG, SELF_TAG, "Failed to create MessagingPushPayload, message id is null or empty");
            return;
        }

        this.messageId = messageId;
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getNotificationVisibility() {
        return notificationVisibility;
    }

    public int getNotificationImportance() {
        return notificationImportance;
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
    public String getMessageId() {
        return messageId;
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
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Payload extraction failed because data provided is null");
            return;
        }
        this.title = data.get(MessagingConstants.Push.PayloadKeys.TITLE);
        this.body = data.get(MessagingConstants.Push.PayloadKeys.BODY);
        this.sound = data.get(MessagingConstants.Push.PayloadKeys.SOUND);
        this.channelId = data.get(MessagingConstants.Push.PayloadKeys.CHANNEL_ID);
        this.icon = data.get(MessagingConstants.Push.PayloadKeys.ICON);
        this.actionUri = data.get(MessagingConstants.Push.PayloadKeys.ACTION_URI);
        this.imageUrl = data.get(MessagingConstants.Push.PayloadKeys.IMAGE_URL);

        try {
            String count = data.get(MessagingConstants.Push.PayloadKeys.BADGE_NUMBER);
            if (count != null) {
                this.badgeCount = Integer.parseInt(count);
            }
        } catch (NumberFormatException e) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Exception in converting notification badge count to int - %s", e.getLocalizedMessage());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.notificationImportance = getNotificationImportanceFromString(data.get(MessagingConstants.Push.PayloadKeys.NOTIFICATION_PRIORITY));
        } else {
            this.notificationPriority = getNotificationPriorityFromString(data.get(MessagingConstants.Push.PayloadKeys.NOTIFICATION_PRIORITY));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.notificationVisibility = getNotificationVisibilityFromString(data.get(MessagingConstants.Push.PayloadKeys.NOTIFICATION_VISIBILITY));
        }

        this.actionType = getActionTypeFromString(data.get(MessagingConstants.Push.PayloadKeys.ACTION_TYPE));
        this.actionButtons = getActionButtonsFromString(data.get(MessagingConstants.Push.PayloadKeys.ACTION_BUTTONS));
    }

    private int getNotificationPriorityFromString(final String priority) {
        if (priority == null) return Notification.PRIORITY_DEFAULT;
        final Integer resolvedPriority = notificationPriorityMap.get(priority);
        if (resolvedPriority == null) return Notification.PRIORITY_DEFAULT;
        return resolvedPriority;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private int getNotificationImportanceFromString(final String priority) {
        if (StringUtils.isNullOrEmpty(priority)) return Notification.PRIORITY_DEFAULT;
        final Integer resolvedImportance = notificationImportanceMap.get(priority);
        if (resolvedImportance == null) return Notification.PRIORITY_DEFAULT;
        return resolvedImportance;
    }

    // Returns the notification visibility from the string
    // If the string is null or not a valid visibility, returns Notification.VISIBILITY_PRIVATE
    //
    // @param visibility string representing the visibility of the notification
    // @return int representing the visibility of the notification
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getNotificationVisibilityFromString(final String visibility) {
        if (StringUtils.isNullOrEmpty(visibility)) return Notification.VISIBILITY_PRIVATE;
        final Integer resolvedVisibility = notificationVisibilityMap.get(visibility);
        if (resolvedVisibility == null) return Notification.VISIBILITY_PRIVATE;
        return resolvedVisibility;
    }

    private ActionType getActionTypeFromString(final String type) {
        if (StringUtils.isNullOrEmpty(type)) {
            return ActionType.NONE;
        }

        switch (type) {
            case ActionButtonType.DEEPLINK:
                return ActionType.DEEPLINK;
            case ActionButtonType.WEBURL:
                return ActionType.WEBURL;
            case ActionButtonType.OPENAPP:
                return ActionType.OPENAPP;
        }

        return ActionType.NONE;
    }

    private List<ActionButton> getActionButtonsFromString(final String actionButtons) {
        if (actionButtons == null) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Exception in converting actionButtons json string to json object, Error : actionButtons is null");
            return null;
        }
        List<ActionButton> actionButtonList = new ArrayList<>(ACTION_BUTTON_CAPACITY);
        try {
            final JSONArray jsonArray = new JSONArray(actionButtons);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);
                final ActionButton button = getActionButton(jsonObject);
                if (button == null) continue;
                actionButtonList.add(button);
            }
        } catch (final JSONException e) {
            Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "Exception in converting actionButtons json string to json object, Error : %s", e.getLocalizedMessage());
            return null;
        }
        return actionButtonList;
    }

    private ActionButton getActionButton(final JSONObject jsonObject) {
        try {
            final String label = jsonObject.getString(ActionButtons.LABEL);
            if (label.isEmpty()) {
                Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Label is empty");
                return null;
            }
            String uri = null;
            final String type = jsonObject.getString(ActionButtons.TYPE);
            if (type.equals(ActionButtonType.WEBURL) || type.equals(ActionButtonType.DEEPLINK)) {
                uri = jsonObject.optString(ActionButtons.URI);
            }

            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "Creating an ActionButton with label (%s), uri (%s), and type (%s)", label, uri, type);
            return new ActionButton(label, uri, type);
        } catch (final JSONException e) {
            Log.warning(MessagingConstants.LOG_TAG, SELF_TAG, "Exception in converting actionButtons json string to json object, Error : %s", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Enum to denote the type of action
     */
    public enum ActionType {
        DEEPLINK,
        WEBURL,
        /**
         * @deprecated Unsupported, will be removed in future versions.
         */
        @Deprecated DISMISS,
        OPENAPP,
        NONE
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

    // Check if the push notification is silent push notification.
    // TODO: Find a better way to distinguish between silent and non-silent push notifications. (to talk with herald team)
    boolean isSilentPushMessage() {
        return data != null && title == null && body == null;
    }
}
