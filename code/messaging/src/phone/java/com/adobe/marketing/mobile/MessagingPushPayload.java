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
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to create a push notification payload object from a received remote message.
 * It implements {@link Parcelable} interface so the {@link MessagingPushPayload} can be broadcast back to the parent application.
 * It provides with functions for getting attributes of push payload (title, body, actions etc ...)
 */
public class MessagingPushPayload implements Parcelable {
    private static final String SELF_TAG = "MessagingPushPayload";
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
            Log.error(MessagingConstants.LOG_TAG, "%s - Failed to create MessagingPushPayload, remote message is null", SELF_TAG);
            return;
        }
        if (message.getData().isEmpty()) {
            Log.error(MessagingConstants.LOG_TAG, "%s - Failed to create MessagingPushPayload, remote message data payload is empty", SELF_TAG);
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

    // Parcelable implementation for MessagingPushPayload

    /**
     * Parcelable Constructor
     *
     * @param in {@link Parcel} retrieved from an {@link android.content.Intent}
     */
    protected MessagingPushPayload(final Parcel in) {
        title = in.readString();
        body = in.readString();
        sound = in.readString();
        badgeCount = in.readInt();
        notificationPriority = in.readInt();
        channelId = in.readString();
        icon = in.readString();
        imageUrl = in.readString();
        actionUri = in.readString();
        actionType = ActionType.valueOf(in.readString());
        in.readList(actionButtons, ActionButton.class.getClassLoader());
        data = (HashMap<String, String>) in.readSerializable();
    }

    public static final Creator<MessagingPushPayload> CREATOR = new Creator<MessagingPushPayload>() {
        @Override
        public MessagingPushPayload createFromParcel(final Parcel in) {
            return new MessagingPushPayload(in);
        }

        @Override
        public MessagingPushPayload[] newArray(final int size) {
            return new MessagingPushPayload[size];
        }
    };

    @Override
    public int describeContents() {
        // a bitmask indicating the set of special object types marshaled by this Parcelable object instance. value should be 0 or CONTENTS_FILE_DESCRIPTOR
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        // flags var is unused
        parcel.writeString(title);
        parcel.writeString(body);
        parcel.writeString(sound);
        parcel.writeInt(badgeCount);
        parcel.writeInt(notificationPriority);
        parcel.writeString(channelId);
        parcel.writeString(icon);
        parcel.writeString(imageUrl);
        parcel.writeString(actionUri);
        parcel.writeString(actionType.name());
        parcel.writeList(actionButtons);
        parcel.writeSerializable(new HashMap<>(data));
    }
    // end of Parcelable implementation for MessagingPushPayload

    /**
     * Check whether the remote message is origination from AEP
     *
     * @return boolean value indicating whether the remote message is origination from AEP
     */
    public boolean isAEPPushMessage() {
        if (data == null || data.isEmpty()) {
            Log.error(MessagingConstants.LOG_TAG, "%s - Returning false as remote message data payload is empty", SELF_TAG);
            return false;
        }
        return "true".equals(data.get(MessagingConstants.PushNotificationPayload.ADB));
    }

    public boolean isSilentPushMessage() {
        return data != null && title == null && body == null;
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
            Log.debug(MessagingConstants.LOG_TAG, "Payload extraction failed because data provided is null");
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
            final String count = data.get(MessagingConstants.PushNotificationPayload.NOTIFICATION_COUNT);
            if (!StringUtils.isNullOrEmpty(count)) {
                this.badgeCount = Integer.parseInt(count);
            }
        } catch (final NumberFormatException e) {
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
        if (type == null || type.isEmpty()) {
            return ActionType.NONE;
        }

        switch (type) {
            case MessagingConstants.PushNotificationPayload.ActionButtonType.DEEPLINK:
                return ActionType.DEEPLINK;
            case MessagingConstants.PushNotificationPayload.ActionButtonType.WEBURL:
                return ActionType.WEBURL;
            case MessagingConstants.PushNotificationPayload.ActionButtonType.DISMISS:
                return ActionType.DISMISS;
        }

        return ActionType.NONE;
    }

    private List<ActionButton> getActionButtonsFromString(final String actionButtons) {
        if (actionButtons == null) {
            Log.debug(MessagingConstants.LOG_TAG, "%s - Exception in converting actionButtons json string to json object, Error : actionButtons is null", SELF_TAG);
            return null;
        }
        final List<ActionButton> actionButtonList = new ArrayList<>(3);
        try {
            final JSONArray jsonArray = new JSONArray(actionButtons);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject jsonObject = jsonArray.getJSONObject(i);
                final ActionButton button = getActionButton(jsonObject);
                if (button == null) continue;
                actionButtonList.add(button);
            }
        } catch (final JSONException e) {
            Log.debug(MessagingConstants.LOG_TAG, "%s - Exception in converting actionButtons json string to json object, Error : %s", SELF_TAG, e.getMessage());
            return null;
        }
        return actionButtonList;
    }

    private ActionButton getActionButton(final JSONObject jsonObject) {
        try {
            final String label = jsonObject.getString(MessagingConstants.PushNotificationPayload.ActionButtons.LABEL);
            if (label.isEmpty()) {
                Log.debug(MessagingConstants.LOG_TAG, "%s - Label is empty", SELF_TAG);
                return null;
            }
            final String uri = jsonObject.getString(MessagingConstants.PushNotificationPayload.ActionButtons.URI);
            final String type = jsonObject.getString(MessagingConstants.PushNotificationPayload.ActionButtons.TYPE);

            return new ActionButton(label, uri, type);
        } catch (final JSONException e) {
            Log.debug(MessagingConstants.LOG_TAG, "%s - Exception in converting actionButtons json string to json object, Error : %s", SELF_TAG, e.getMessage());
            return null;
        }
    }

    /**
     * Enum to denote the type of action
     */
    public enum ActionType {
        DEEPLINK, WEBURL, DISMISS, NONE
    }

    public static class ACTION_BUTTON_KEY {
        public final static String LABEL = "adb_action_label";
        public final static String LINK = "adb_action_link";
        public final static String TYPE = "adb_action_type";
    }

    public static final class ACTION_KEY {
        public static final String ACTION_NOTIFICATION_CLICKED = "adb_action_notification_clicked";
        public static final String ACTION_NOTIFICATION_DELETED = "adb_action_notification_deleted";
        public static final String ACTION_BUTTON_CLICKED = "adb_action_button_clicked";
        public static final String ACTION_NORMAL_NOTIFICATION_CREATED = "adb_action_notification_created";
        public static final String ACTION_SILENT_NOTIFICATION_CREATED = "adb_action_silent_notification_created";
    }

    /**
     * Class representing the action button with label, link and type
     */
    public static class ActionButton implements Parcelable {
        private final String label;
        private final String link;
        private final ActionType type;

        public ActionButton(final String label, final String link, final String type) {
            this.label = label;
            this.link = link;
            this.type = ActionType.valueOf(type);
        }

        // Parcelable implementation for ActionButton

        /**
         * Parcelable Constructor
         *
         * @param in {@link Parcel} retrieved from an {@link android.content.Intent}
         */
        protected ActionButton(final Parcel in) {
            label = in.readString();
            link = in.readString();
            type = ActionType.valueOf(in.readString());
        }

        public static final Creator<ActionButton> CREATOR = new Creator<ActionButton>() {
            @Override
            public ActionButton createFromParcel(final Parcel in) {
                return new ActionButton(in);
            }

            @Override
            public ActionButton[] newArray(final int size) {
                return new ActionButton[size];
            }
        };

        @Override
        public int describeContents() {
            // a bitmask indicating the set of special object types marshaled by this Parcelable object instance. value should be 0 or CONTENTS_FILE_DESCRIPTOR
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel parcel, final int flags) {
            // flags var is unused
            parcel.writeString(label);
            parcel.writeString(link);
            parcel.writeString(type.name());
        }
        // end of Parcelable implementation for ActionButton

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
