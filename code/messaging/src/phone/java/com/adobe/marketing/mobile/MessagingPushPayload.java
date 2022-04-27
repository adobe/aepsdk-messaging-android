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
import java.util.List;
import java.util.Map;

/**
 * This class is used to create a push notification payload object from a received remote message.
 * It implements {@link Parcelable} interface so the {@link MessagingPushPayload} can be broadcast back to the parent application.
 * It provides with functions for getting attributes of push payload (title, body, actions etc ...)
 */
public class MessagingPushPayload implements Parcelable {
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
            Log.error(MessagingConstant.LOG_TAG, "%s - Failed to create MessagingPushPayload, remote message is null", SELF_TAG);
            return;
        }
        if (message.getData().isEmpty()) {
            Log.error(MessagingConstant.LOG_TAG, "%s - Failed to create MessagingPushPayload, remote message data payload is empty", SELF_TAG);
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
    }

    /**
     * Check whether the remote message is origination from AEP
     *
     * @return boolean value indicating whether the remote message is origination from AEP
     */
    public boolean isAEPPushMessage() {
        if (data == null || data.isEmpty()) {
            Log.error(MessagingConstant.LOG_TAG, "%s - Returning false as remote message data payload is empty", SELF_TAG);
            return false;
        }
        return "true".equals(data.get(MessagingConstant.PushNotificationPayload.ADB));
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
            if (!StringUtils.isNullOrEmpty(count)) {
                this.badgeCount = Integer.parseInt(count);
            }
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
                    .PRIORITY_MIN:
                return Notification.PRIORITY_MIN;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_LOW:
                return Notification.PRIORITY_LOW;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_HIGH:
                return Notification.PRIORITY_HIGH;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
                    .PRIORITY_MAX:
                return Notification.PRIORITY_MAX;
            case MessagingConstant.PushNotificationPayload.NotificationPriorities
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
            case MessagingConstant.PushNotificationPayload.ActionButtonType.DEEPLINK:
                return ActionType.DEEPLINK;
            case MessagingConstant.PushNotificationPayload.ActionButtonType.WEBURL:
                return ActionType.WEBURL;
            case MessagingConstant.PushNotificationPayload.ActionButtonType.DISMISS:
                return ActionType.DISMISS;
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
            for (int i = 0; i < jsonArray.length(); i++) {
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
