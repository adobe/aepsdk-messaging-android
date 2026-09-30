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

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.plugin.ILiveupdatePlugin;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * This class is the entry point for all push notifications received from Firebase.
 *
 * <p>Once the MessagingService is registered in the AndroidManifest.xml. This class will
 * automatically handle display and tracking of Adobe Journey Optimizer push notifications.
 */
public class MessagingService extends FirebaseMessagingService {
    private static final String SELF_TAG = "MessagingService";
    private static final String XDM_KEY = "_xdm";

    // Self-init constants — mirror AppIdManager's persistence keys in Core.
    private static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
    private static final String CONFIG_KEY_APP_ID = "config.appID";
    private static volatile boolean selfInitTried = false;

    @Override
    public void onNewToken(final @NonNull String token) {
        super.onNewToken(token);
        Log.debug(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "onNewToken: received new FCM registration token; forwarding to MobileCore.");
        MobileCore.setPushIdentifier(token);
    }

    @Override
    public void onMessageReceived(final @NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.debug(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "onMessageReceived: received remote message from FCM; delegating to"
                        + " handleRemoteMessage.");

        // Build and display the notification immediately while the FCM wakelock is active.
        handleRemoteMessage(this, remoteMessage);
    }

    /**
     * Entry point for an FCM message: displays and tracks it when it was sent by Adobe Journey
     * Optimizer. Live Update messages go to the registered {@link ILiveupdatePlugin}; all other AJO
     * messages go through the standard push path.
     *
     * @param context the {@link Context} used to build and post the notification
     * @param remoteMessage the {@link RemoteMessage} received from FCM
     * @return {@code true} if Messaging handled the message, {@code false} if it was not an AJO
     *     message or has no message id, in which case the app should handle it
     */
    public static boolean handleRemoteMessage(
            final @NonNull Context context, final @NonNull RemoteMessage remoteMessage) {
        if (!isAJONotification(remoteMessage)) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "The received push message is not generated from Adobe Journey Optimizer."
                            + " Messaging extension is ignoring to display the push notification.");
            return false;
        }

        // Live Update fork — early gate. If a handler is registered, hand off the raw
        // RemoteMessage; the handler owns parsing, building, and posting end-to-end.
        // If no handler is registered, drop with a warning — Messaging cannot meaningfully
        // render a Live Update payload (its content lives inside adb_liveupdate_data which
        // is intentionally opaque to Messaging).
        if (isLiveUpdateMessage(remoteMessage)) {
            handleLiveUpdateMessage(context, remoteMessage);
            return true;
        }

        // Standard push path — UNCHANGED from pre-Live-Update state. Reads only outer adb_*
        // keys; never peeks inside adb_liveupdate_data.
        // Build and display the notification synchronously while the FCM wakelock is active.
        return handlePushMessage(context, remoteMessage);
    }

    /**
     * Standard push path: builds the notification (through the push template plugin when one is
     * registered, otherwise the basic layout), posts it under the id derived from the message id,
     * then records delivery, bootstrapping the SDK first on a cold start.
     *
     * @param context the {@link Context} used to build and post the notification
     * @param remoteMessage the AJO {@link RemoteMessage} to display
     * @return {@code true} if the notification was posted, {@code false} if the message has no
     *     message id
     */
    private static boolean handlePushMessage(
            final @NonNull Context context, final @NonNull RemoteMessage remoteMessage) {
        final String messageId = remoteMessage.getMessageId();
        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "The received push message does not have a message id. Messaging extension is"
                            + " ignoring to display the push notification.");
            return false;
        }

        // Build and display the notification synchronously while the FCM wakelock is active.
        final Notification notification =
                MessagingPushBuilder.build(remoteMessage, messageId, context);

        // display notification
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(MessagingPushUtils.getNotificationId(messageId), notification);

        // Bootstrap the SDK if this is a cold-start push, then record delivery.
        selfInit(context, () -> Messaging.trackPushReceived(remoteMessage));

        return true;
    }

    /**
     * @param remoteMessage the {@link RemoteMessage} to check
     * @return {@code true} if the message data has the {@code adb_liveupdate_data} key, marking it
     *     as a Live Update push
     */
    private static boolean isLiveUpdateMessage(final @NonNull RemoteMessage remoteMessage) {
        return remoteMessage
                .getData()
                .containsKey(MessagingConstants.Push.PayloadKeys.LIVE_UPDATE_DATA);
    }

    /**
     * Hands a Live Update message to the registered {@link ILiveupdatePlugin}, which owns parsing,
     * building and posting it. Without a registered plugin the message is dropped with a warning,
     * because Messaging cannot render the opaque {@code adb_liveupdate_data} content, and a {@code
     * no_plugin} render error event carrying the message's {@code _xdm} is dispatched.
     *
     * @param context the {@link Context} passed to the plugin
     * @param remoteMessage the Live Update {@link RemoteMessage}
     */
    private static void handleLiveUpdateMessage(
            final @NonNull Context context, final @NonNull RemoteMessage remoteMessage) {
        final ILiveupdatePlugin liveUpdatePlugin = MobileCore.getPlugin(ILiveupdatePlugin.class);
        if (liveUpdatePlugin == null) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Received a Live Update push but no ILiveupdatePlugin is registered."
                            + " Dropping. Register a plugin via MobileCore.addPlugins(...).");
            MessagingPushUtils.dispatchPluginErrorEvent(
                    MessagingPushConstants.PluginError.EVENT_NAME_LIVE_UPDATE_ERROR,
                    MessagingPushConstants.PluginError.CATEGORY_LIVE_UPDATE_ERROR,
                    MessagingPushConstants.PluginError.SUBCATEGORY_NO_PLUGIN,
                    remoteMessage.getData());
            return;
        }
        liveUpdatePlugin.handleLiveUpdatePush(context, remoteMessage);
    }

    /**
     * Bootstraps the AEP SDK from a cached {@code appId} when the SDK is not yet initialized, then
     * runs {@code onInitComplete} from the {@link MobileCore#initialize} completion callback. Used
     * when a cold-start push arrives before the host app finishes initialization.
     *
     * <p>Runs at most once per process. If no cached {@code appId} is available (first launch
     * before any {@link MobileCore#configureWithAppID(String)} call), self-init is skipped and
     * {@code onInitComplete} is not invoked.
     *
     * @param context the {@link Context} from FCM's {@code onMessageReceived}
     * @param onInitComplete callback invoked after initialization completes, or immediately if
     *     self-init was already attempted in this process
     */
    private static synchronized void selfInit(
            @NonNull final Context context, @NonNull final Runnable onInitComplete) {
        if (selfInitTried) {
            onInitComplete.run();
            return;
        }
        selfInitTried = true;

        if (!(context.getApplicationContext() instanceof Application)) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Self-init aborted: ApplicationContext is not an Application instance.");
            return;
        }
        final Application application = (Application) context.getApplicationContext();

        MobileCore.setApplication(application);

        final String cachedAppId = readCachedAppId();
        if (StringUtils.isNullOrEmpty(cachedAppId)) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Self-init aborted: no cached appId found in persistence. The host app must"
                            + " configureWithAppID at least once before the SDK can self-init.");
            return;
        }
        Log.debug(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "Self-init: cold-start detected, calling MobileCore.initialize.");

        MobileCore.initialize(
                application,
                cachedAppId,
                ignored -> {
                    Log.debug(
                            MessagingPushConstants.LOG_TAG,
                            SELF_TAG,
                            "Self-init: MobileCore.initialize completed; dispatching push-receive"
                                    + " event.");
                    onInitComplete.run();
                });
    }

    /**
     * Reads the {@code appId} previously written to the {@code AdobeMobile_ConfigState}
     * NamedCollection by Core's {@code ConfigurationExtension.configureWithAppID}.
     *
     * @return the persisted appId, or {@code null} if not present or unreadable.
     */
    private static String readCachedAppId() {
        try {
            final NamedCollection store =
                    ServiceProvider.getInstance()
                            .getDataStoreService()
                            .getNamedCollection(CONFIG_DATASTORE);
            return (store != null) ? store.getString(CONFIG_KEY_APP_ID, null) : null;
        } catch (final RuntimeException e) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Self-init: failed to read cached appId: " + e.getMessage());
            return null;
        }
    }

    /**
     * This method looks at the remote message payload and determines if it is a notification from
     * Adobe Journey Optimizer.
     *
     * @param remoteMessage the message received from Firebase
     * @return true if the remote message originated from Adobe Journey Optimizer, false otherwise
     */
    public static boolean isAJONotification(final @NonNull RemoteMessage remoteMessage) {
        // TODO: Use the newly introduced key "ajo_type" to identify Adobe push notifications.
        return remoteMessage.getData().containsKey(XDM_KEY)
                || remoteMessage.getData().containsKey(MessagingConstants.Push.PayloadKeys.TITLE);
    }
}
