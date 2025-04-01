/*
  Copyright 2023 Adobe. All rights reserved.
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
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.caching.CacheResult;
import com.adobe.marketing.mobile.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class for building push notification.
 *
 * <p>The build method in this class takes {@link MessagingPushPayload} received from the push
 * notification and builds the notification. This class is used internally by MessagingService to
 * build the push notification.
 */
class MessagingPushBuilder {

    private static final String SELF_TAG = "MessagingPushBuilder";
    private static final String DEFAULT_CHANNEL_ID = "AJOPushChannel";
    // When no channel name is received from the push notification, this default channel name is
    // used.
    // This will appear in the notification settings for the app.
    private static final String DEFAULT_CHANNEL_NAME = "General Notifications";

    /**
     * Builds a notification for the received payload.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @return the notification
     */
    @NonNull static Notification build(final MessagingPushPayload payload, final Context context) {
        final String channelId = createChannelAndGetChannelID(payload, context);

        // Create the notification
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId);
        builder.setContentTitle(payload.getTitle());
        builder.setContentText(payload.getBody());
        builder.setNumber(payload.getBadgeCount());
        builder.setPriority(payload.getNotificationPriority());
        builder.setAutoCancel(true);

        setSmallIcon(
                builder, payload,
                context); // Small Icon must be present, otherwise the notification will not be
        // displayed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setVisibility(builder, payload);
        }
        addActionButtons(builder, payload, context); // Add action buttons if any
        setSound(builder, payload, context);
        setNotificationClickAction(builder, payload, context);
        setNotificationDeleteAction(builder, payload, context);

        return buildNotification(builder, payload);
    }

    /**
     * Creates a channel if it does not exist and returns the channel ID. If a channel ID is
     * received from the payload and if channel exists for the channel ID, the same channel ID is
     * returned. If a channel ID is received from the payload and if channel does not exist for the
     * channel ID, Messaging extension's default channel is used. If no channel ID is received from
     * the payload, Messaging extension's default channel is used. For Android versions below O, no
     * channel is created. Just return the obtained channel ID.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @return the channel ID
     */
    @NonNull private static String createChannelAndGetChannelID(
            final MessagingPushPayload payload, final Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // For Android versions below O, no channel is created. Just return the obtained channel
            // ID.
            return payload.getChannelId() == null ? DEFAULT_CHANNEL_ID : payload.getChannelId();
        } else {
            // For Android versions O and above, create a channel if it does not exist and return
            // the channel ID.
            final NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            final String channelIdFromPayload = payload.getChannelId();

            // if a channel from the payload is not null and if a channel exists for the channel ID
            // from the payload, use the same channel ID.
            if (channelIdFromPayload != null
                    && notificationManager.getNotificationChannel(channelIdFromPayload) != null) {
                Log.debug(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "Channel exists for channel ID: "
                                + channelIdFromPayload
                                + ". Using the same for push notification.");
                return channelIdFromPayload;
            } else {
                Log.debug(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "Channel does not exist for channel ID obtained from payload ( "
                                + channelIdFromPayload
                                + "). Using the Messaging Extension's default channel.");
            }

            // Use the default channel ID if the channel ID from the payload is null or if a channel
            // does not exist for the channel ID from the payload.
            final String channelId = DEFAULT_CHANNEL_ID;
            if (notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID) != null) {
                Log.debug(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "Channel already exists for the default channel ID: " + channelId);
                return DEFAULT_CHANNEL_ID;
            } else {
                Log.debug(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "Creating a new channel for the default channel ID: " + channelId + ".");
                final NotificationChannel channel =
                        new NotificationChannel(
                                channelId,
                                DEFAULT_CHANNEL_NAME,
                                NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }
            return channelId;
        }
    }

    /**
     * Sets the small icon for the notification. If a small icon is received from the payload, the
     * same is used. If a small icon is not received from the payload, we use the icon set using
     * MobileCore.setSmallIcon(). If a small icon is not set using MobileCore.setSmallIcon(), we use
     * the default small icon of the application.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @param builder the notification builder
     */
    private static void setSmallIcon(
            final NotificationCompat.Builder builder,
            final MessagingPushPayload payload,
            final Context context) {
        final int iconFromPayload =
                MessagingPushUtils.getSmallIconWithResourceName(payload.getIcon(), context);
        final int iconFromMobileCore = MobileCore.getSmallIconResourceID();

        if (isValidIcon(iconFromPayload)) {
            builder.setSmallIcon(iconFromPayload);
        } else if (isValidIcon(iconFromMobileCore)) {
            builder.setSmallIcon(iconFromMobileCore);
        } else {
            final int iconFromApp = MessagingPushUtils.getDefaultAppIcon(context);
            if (isValidIcon(iconFromApp)) {
                builder.setSmallIcon(iconFromApp);
            } else {
                Log.warning(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "No valid small icon found. Notification will not be displayed.");
            }
        }
    }

    /**
     * Sets the sound for the notification. If a sound is received from the payload, the same is
     * used. If a sound is not received from the payload, the default sound is used The sound name
     * from the payload should also include the format of the sound file. eg: sound.mp3
     *
     * @param notificationBuilder the notification builder
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     */
    private static void setSound(
            final NotificationCompat.Builder notificationBuilder,
            final MessagingPushPayload payload,
            final Context context) {
        if (!StringUtils.isNullOrEmpty(payload.getSound())) {
            notificationBuilder.setSound(
                    MessagingPushUtils.getSoundUriForResourceName(payload.getSound(), context));
            return;
        }
        notificationBuilder.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }

    /**
     * Sets the image for the notification then builds the notification. If a large icon url is
     * received from the payload, the media is downloaded and the notification style is set to
     * BigPictureStyle. If large icon url is not received from the payload, default style is used
     * for the notification.
     *
     * @param notificationBuilder the notification builder
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @return the built {@link Notification} object
     */
    private static Notification buildNotification(
            final NotificationCompat.Builder notificationBuilder,
            final MessagingPushPayload payload) {
        // Quick bail out if there is no image url
        if (StringUtils.isNullOrEmpty(payload.getImageUrl())) return notificationBuilder.build();

        // If the device is running on Android 34 or above and the image is a gif, download the gif
        // and build the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && MessagingPushUtils.isGifContent(payload.getImageUrl())) {
            return downloadGifThenBuildNotification(notificationBuilder, payload);
        } else {
            final Bitmap bitmap = MessagingPushUtils.download(payload.getImageUrl());
            // Bail out if the download fails
            if (bitmap == null) return notificationBuilder.build();

            final NotificationCompat.BigPictureStyle bigPictureStyle =
                    new NotificationCompat.BigPictureStyle();
            bigPictureStyle.bigPicture(bitmap);
            notificationBuilder.setStyle(bigPictureStyle);
        }
        return notificationBuilder.build();
    }

    /**
     * Downloads and caches the gif content using the {@link MessageAssetDownloader} then applies
     * the gif using a {@link androidx.core.app.NotificationCompat.BigPictureStyle}. The prepared
     * {@code NotificationCompat.Builder} is then used to build the notification.
     *
     * @param notificationBuilder the {@link NotificationCompat.Builder} to be used to build the
     *     notification
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @return the built {@link Notification} object
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @NonNull private static Notification downloadGifThenBuildNotification(
            final NotificationCompat.Builder notificationBuilder,
            final MessagingPushPayload payload) {
        CompletableFuture<NotificationCompat.Builder> builderFuture =
                CompletableFuture.supplyAsync(
                        () -> {
                            final MessageAssetDownloader assetDownloader =
                                    new MessageAssetDownloader(
                                            new ArrayList<String>() {
                                                {
                                                    add(payload.getImageUrl());
                                                }
                                            });
                            assetDownloader.downloadAssetCollection();

                            final ExecutorService singleThreadScheduledExecutor =
                                    Executors.newSingleThreadScheduledExecutor();
                            final CacheResult cacheResult =
                                    MessagingPushUtils.getCachedAsset(
                                                    singleThreadScheduledExecutor,
                                                    payload.getImageUrl(),
                                                    MessagingConstants.DOWNLOAD_ASSET_TIMEOUT)
                                            .join();
                            singleThreadScheduledExecutor.shutdown();

                            if (cacheResult == null) {
                                Log.debug(
                                        MessagingPushConstants.LOG_TAG,
                                        SELF_TAG,
                                        "Failed to download the rich media. A text only"
                                                + " notification will be displayed.");
                                return notificationBuilder;
                            }
                            setNotificationBigPicture(notificationBuilder, cacheResult);
                            return notificationBuilder;
                        });

        return builderFuture.thenApply(NotificationCompat.Builder::build).join();
    }

    /**
     * Sets the gif media for the notification using a {@link
     * androidx.core.app.NotificationCompat.BigPictureStyle}.
     *
     * @param notificationBuilder the {@link NotificationCompat.Builder} to be used to build the
     *     notification
     * @param cacheResult the {@link CacheResult} containing the cached gif media
     */
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static void setNotificationBigPicture(
            final NotificationCompat.Builder notificationBuilder, final CacheResult cacheResult) {
        final Uri uri = MessagingPushUtils.getCachedRichMediaFileUri(cacheResult);
        if (uri == null) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to find cached rich media, uri not built.");
            return;
        }

        final NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        style.bigPicture(Icon.createWithContentUri(uri));
        notificationBuilder.setStyle(style);
    }

    /**
     * Sets the click action for the notification. If an action type is received from the payload,
     * the same is used. If an action type is not received from the payload, the default action type
     * is used. If an action type is received from the payload, but the action type is not
     * supported, the default action type is used.
     *
     * @param notificationBuilder the {@link NotificationCompat.Builder} to be used to build the
     *     notification
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     */
    private static void setNotificationClickAction(
            final NotificationCompat.Builder notificationBuilder,
            final MessagingPushPayload payload,
            final Context context) {
        final PendingIntent pendingIntent;
        if (payload.getActionType() == MessagingPushPayload.ActionType.DEEPLINK
                || payload.getActionType() == MessagingPushPayload.ActionType.WEBURL) {
            pendingIntent =
                    createPendingIntent(
                            payload,
                            context,
                            MessagingPushConstants.NotificationAction.OPENED,
                            payload.getActionUri(),
                            null);
        } else {
            pendingIntent =
                    createPendingIntent(
                            payload,
                            context,
                            MessagingPushConstants.NotificationAction.OPENED,
                            null,
                            null);
        }
        notificationBuilder.setContentIntent(pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void setVisibility(
            final NotificationCompat.Builder notificationBuilder,
            final MessagingPushPayload payload) {
        final int visibility = payload.getNotificationVisibility();
        switch (visibility) {
            case NotificationCompat.VISIBILITY_PUBLIC:
                notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                break;
            case NotificationCompat.VISIBILITY_PRIVATE:
                notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                break;
            case NotificationCompat.VISIBILITY_SECRET:
                notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
                break;
            default:
                notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
                Log.debug(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "Invalid visibility value received from the payload. Using the default"
                                + " visibility value.");
                break;
        }
    }

    /**
     * Adds action buttons for the notification.
     *
     * @param builder the notification builder
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     */
    private static void addActionButtons(
            final NotificationCompat.Builder builder,
            final MessagingPushPayload payload,
            final Context context) {
        final List<MessagingPushPayload.ActionButton> actionButtons = payload.getActionButtons();
        if (actionButtons == null || actionButtons.isEmpty()) {
            return;
        }

        for (final MessagingPushPayload.ActionButton eachButton : actionButtons) {

            final PendingIntent pendingIntent;
            if (eachButton.getType() == MessagingPushPayload.ActionType.DEEPLINK
                    || eachButton.getType() == MessagingPushPayload.ActionType.WEBURL) {
                pendingIntent =
                        createPendingIntent(
                                payload,
                                context,
                                MessagingPushConstants.NotificationAction.BUTTON_CLICKED,
                                eachButton.getLink(),
                                eachButton.getLabel());
            } else {
                pendingIntent =
                        createPendingIntent(
                                payload,
                                context,
                                MessagingPushConstants.NotificationAction.BUTTON_CLICKED,
                                null,
                                eachButton.getLabel());
            }
            builder.addAction(0, eachButton.getLabel(), pendingIntent);
        }
    }

    /**
     * Creates a pending intent for the notification.
     *
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     * @param notificationAction the notification action
     * @param actionUri the action uri
     * @param actionID the action ID
     * @return the pending intent
     */
    private static PendingIntent createPendingIntent(
            final MessagingPushPayload payload,
            final Context context,
            final String notificationAction,
            final String actionUri,
            final String actionID) {
        final Intent intent = new Intent(notificationAction);
        intent.setClass(context.getApplicationContext(), MessagingPushTrackerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        addActionDetailsToIntent(intent, actionUri, actionID);
        addPushPayloadToIntent(intent, payload);
        Messaging.addPushTrackingDetails(intent, payload.getMessageId(), payload.getData());
        // adding tracking details
        PendingIntent resultIntent =
                TaskStackBuilder.create(context)
                        .addNextIntentWithParentStack(intent)
                        .getPendingIntent(
                                new Random().nextInt(),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return resultIntent;
    }

    /**
     * Sets the delete action for the notification.
     *
     * @param builder the notification builder
     * @param payload {@link MessagingPushPayload} the payload received from the push notification
     * @param context the application {@link Context}
     */
    private static void setNotificationDeleteAction(
            final NotificationCompat.Builder builder,
            final MessagingPushPayload payload,
            final Context context) {
        final Intent deleteIntent = new Intent(MessagingPushConstants.NotificationAction.DISMISSED);
        deleteIntent.setClass(context, MessagingPushTrackerActivity.class);
        Messaging.addPushTrackingDetails(deleteIntent, payload.getMessageId(), payload.getData());
        final PendingIntent intent =
                PendingIntent.getActivity(
                        context,
                        new Random().nextInt(),
                        deleteIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setDeleteIntent(intent);
    }

    /**
     * Adds action details to the intent.
     *
     * @param intent the intent
     * @param actionUri the action uri
     * @param actionId the action ID
     */
    private static void addActionDetailsToIntent(
            final Intent intent, final String actionUri, final String actionId) {
        if (!StringUtils.isNullOrEmpty(actionUri)) {
            intent.putExtra(MessagingPushConstants.Tracking.Keys.ACTION_URI, actionUri);
        }

        if (!StringUtils.isNullOrEmpty(actionId)) {
            intent.putExtra(MessagingPushConstants.Tracking.Keys.ACTION_ID, actionId);
        }
    }

    /**
     * Adds the data in {@code payload} to the extras of the provided {@link Intent}.
     *
     * @param intent {@code Intent} to be modified
     * @param payload {@link MessagingPushPayload} containing the push notification data
     */
    private static void addPushPayloadToIntent(
            final Intent intent, final MessagingPushPayload payload) {
        if (intent != null && payload != null) {
            payload.putDataInExtras(intent);
        }
    }

    /**
     * Checks if the icon is valid.
     *
     * @param icon the icon to be checked
     * @return true if the icon is valid, false otherwise
     */
    private static boolean isValidIcon(final int icon) {
        return icon > 0;
    }
}
