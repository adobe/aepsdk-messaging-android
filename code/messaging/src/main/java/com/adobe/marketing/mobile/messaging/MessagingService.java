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
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.adobe.marketing.mobile.MobileCore;
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

        // Build and display the notification synchronously while the FCM wakelock is active.
        final MessagingPushPayload payload = new MessagingPushPayload(remoteMessage);
        final Notification notification = MessagingPushBuilder.build(payload, context);
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(remoteMessage.getMessageId().hashCode(), notification);

        // Bootstrap the SDK if this is a cold-start push, then record delivery.
        selfInit(context, () -> Messaging.trackPushReceived(remoteMessage));
        return true;
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
    private static boolean isAJONotification(final @NonNull RemoteMessage remoteMessage) {
        // TODO: Use the newly introduced key "ajo_type" to identify Adobe push notifications.
        return remoteMessage.getData().containsKey(XDM_KEY)
                || remoteMessage.getData().containsKey(MessagingConstants.Push.PayloadKeys.TITLE);
    }
}
