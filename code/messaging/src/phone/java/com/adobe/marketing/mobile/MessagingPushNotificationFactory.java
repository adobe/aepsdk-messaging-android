/*
  Copyright 2022 Adobe. All rights reserved.
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
import android.content.Context;
import android.os.Build;

import java.util.List;

/**
 * The Messaging extension implementation of {@link IMessagingPushNotificationFactory}.
 */
class MessagingPushNotificationFactory implements IMessagingPushNotificationFactory {
    public static final int INVALID_SMALL_ICON_RES_ID = -1;
    private static final String SELF_TAG = "MessagingPushNotificationFactory";
    private static volatile MessagingPushNotificationFactory singletonInstance = null;

    private MessagingPushNotificationFactory() {
    }

    /**
     * Singleton method to get the {@link MessagingPushNotificationFactory} instance.
     *
     * @return the {@code MessagingPushNotificationFactory} singleton
     */
    public static MessagingPushNotificationFactory getInstance() {
        if (singletonInstance == null) {
            synchronized (MessagingPushNotificationFactory.class) {
                if (singletonInstance == null) {
                    singletonInstance = new MessagingPushNotificationFactory();
                }
            }
        }
        return singletonInstance;
    }

    /**
     * Creates a push notification from the given {@link MessagingPushPayload}.
     *
     * @param context              the application {@link Context}
     * @param payload              the {@code MessagingPushPayload} containing the data payload from AJO
     * @param messageId            a {@code String} containing the message id
     * @param shouldHandleTracking {@code boolean} if true the AEPMessaging extension will handle notification interaction tracking
     * @return the created {@link Notification}
     */
    @Override
    public Notification create(final Context context, final MessagingPushPayload payload, final String messageId, final boolean shouldHandleTracking) {
        // Setting the channel id
        final String channelId = MessagingUtils.getChannelId(context, payload);
        final Notification.Builder notificationBuilder = new Notification.Builder(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(channelId);
        }
        // Setting the default small icon
        if (!setDefaultSmallIcon(context, notificationBuilder)) {
            Log.warning(LOG_TAG, "%s - Failed to set the default small icon.", SELF_TAG);
            return null;
        }
        // Setting the title
        notificationBuilder.setContentTitle(payload.getTitle());
        // Setting the summary
        notificationBuilder.setContentText(payload.getBody());
        // Setting the large icon
        notificationBuilder.setLargeIcon(MessagingUtils.getImageAsset(context, payload.getIcon()));
        // Setting the pending content intent for the notification
        notificationBuilder.setContentIntent(MessagingUtils.getPendingIntentForAction(context, payload, messageId, MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_CLICKED, shouldHandleTracking));
        // Setting the pending delete intent for the notification
        notificationBuilder.setDeleteIntent(MessagingUtils.getPendingIntentForAction(context, payload, messageId, MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED, shouldHandleTracking));
        // Setting the priority
        notificationBuilder.setPriority(payload.getNotificationPriority());
        // Setting badge count
        notificationBuilder.setNumber(payload.getBadgeCount());
        // Setting big picture style if an image url is present
        notificationBuilder.setStyle(new Notification.BigPictureStyle().bigPicture(MessagingUtils.getImageAsset(context, payload.getImageUrl())));
        // Setting sound based on the android sdk version
        setSound(context, payload.getSound(), notificationBuilder);
        // Setting the action buttons
        final List<MessagingPushPayload.ActionButton> buttons = payload.getActionButtons();
        if (buttons != null && !buttons.isEmpty()) {
            for (final MessagingPushPayload.ActionButton button : buttons) {
                MessagingUtils.addAction(context, button, payload, notificationBuilder, messageId);
            }
        }

        return notificationBuilder.build();
    }

    /**
     * Sets the notification sound to be used. If no bundled sound filename is provided then the default system sound is used.
     *
     * @param context             the application {@link Context}
     * @param fileName            the {@code String} containing the name of the bundled sound file to use when this notification is displayed
     * @param notificationBuilder the {@link Notification.Builder} object currently being used to build the notification
     */
    private void setSound(final Context context, final String fileName, final Notification.Builder notificationBuilder) {
        if (StringUtils.isNullOrEmpty(fileName)) {
            Log.debug(LOG_TAG, "%s - No custom sound specified, using the default notification sound.", SELF_TAG);
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                notificationBuilder.setSound(MessagingUtils.getSoundUri(context, fileName));
            }
        } catch (final Exception e) {
            Log.debug(LOG_TAG, "%s - Exception occurred when setting notification sound: %s.", SELF_TAG, e.getMessage());
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        }
    }

    /**
     * Sets the small icon to be shown in the status bar and within the notification.
     *
     * @param context             the application {@link Context}
     * @param notificationBuilder the {@link Notification.Builder} object currently being used to build the notification
     * @return a {@code boolean} containing true if the icon was set, false otherwise
     */
    private boolean setDefaultSmallIcon(final Context context, final Notification.Builder notificationBuilder) {
        // Use the launcher icon as the default small icon.
        int smallIcon = MessagingUtils.getDefaultSmallIconRes(context);
        if (smallIcon == INVALID_SMALL_ICON_RES_ID) {
            Log.warning(LOG_TAG, "%s - Invalid small icon found.", SELF_TAG);
            return false;
        }
        notificationBuilder.setSmallIcon(smallIcon);
        return true;
    }
}
