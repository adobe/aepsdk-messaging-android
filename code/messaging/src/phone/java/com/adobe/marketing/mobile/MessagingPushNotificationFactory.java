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

import android.app.Notification;
import android.content.Context;
import android.os.Build;

import java.util.List;


class MessagingPushNotificationFactory implements IMessagingPushNotificationFactory {

    public static final int INVALID_SMALL_ICON_RES_ID = -1;

    private static volatile MessagingPushNotificationFactory singletonInstance = null;

    private MessagingPushNotificationFactory() {
    }

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

    @Override
    public Notification create(final Context context, final MessagingPushPayload payload, final String messageId, final boolean shouldHandleTracking) {
        // Setting the channel id
        String channelId = MessagingUtils.getChannelId(context, payload);
        Notification.Builder notificationBuilder = new Notification.Builder(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(channelId);
        }
        // Setting the default small icon
        if (!setDefaultSmallIcon(context, notificationBuilder)) {
            // Log error failed to set the default icon
            return null;
        }
        // Setting the title
        notificationBuilder.setContentTitle(payload.getTitle());
        // Setting the summary
        notificationBuilder.setContentText(payload.getBody());
        // Setting the large icon
        notificationBuilder.setLargeIcon(MessagingUtils.getLargeIcon(context, payload));
        // Setting the pending content intent for the notification
        notificationBuilder.setContentIntent(MessagingUtils.getPendingIntentForAction(context, payload, messageId, MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_CLICKED, shouldHandleTracking));
        // Setting the pending delete intent for the notification
        notificationBuilder.setDeleteIntent(MessagingUtils.getPendingIntentForAction(context, payload, messageId, MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED, shouldHandleTracking));
        // Setting the priority
        notificationBuilder.setPriority(payload.getNotificationPriority());
        // Setting badge count
        notificationBuilder.setNumber(payload.getBadgeCount());
        // Setting big picture style if an image url is present
        notificationBuilder.setStyle(new Notification.BigPictureStyle().bigPicture(MessagingUtils.getNotificationImage(context, payload)));
        // Setting sound based on the android sdk version
        setSound(context, payload.getSound(), notificationBuilder);
        // Setting the action buttons
        List<MessagingPushPayload.ActionButton> buttons = payload.getActionButtons();
        if (buttons != null && !buttons.isEmpty()) {
            for (MessagingPushPayload.ActionButton button: buttons) {
                MessagingUtils.addAction(context, button, payload, notificationBuilder, messageId);
            }
        }

        return notificationBuilder.build();
    }

    private void setSound(final Context context, final String fileName, final Notification.Builder notificationBuilder) {
        if (fileName == null || fileName.isEmpty()) {
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                notificationBuilder.setSound(MessagingUtils.getSoundUri(context, fileName));
            }
        } catch (Exception e) {
            // log exception
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        }
    }

    private boolean setDefaultSmallIcon(final Context context, final Notification.Builder builder) {
        // Use the launcher icon as the default small icon.
        int smallIcon = MessagingUtils.getDefaultSmallIconRes(context);
        if (smallIcon == INVALID_SMALL_ICON_RES_ID) {
            // log
            return false;
        }
        builder.setSmallIcon(smallIcon);
        return true;
    }
}
