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
import com.adobe.marketing.mobile.InitOptions;
import com.adobe.marketing.mobile.Messaging;
import com.adobe.marketing.mobile.MessagingPushPayload;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class is the entry point for all push notifications received from Firebase.
 *
 * <p>Once the MessagingService is registered in the AndroidManifest.xml. This class will
 * automatically handle display and tracking of Adobe Journey Optimizer push notifications.
 */
public class MessagingService extends FirebaseMessagingService {
    private static final String SELF_TAG = "MessagingService";
    private static final String XDM_KEY = "_xdm";

    // Self-init constants. Mirror AppIdManager's persistence in Core (NamedCollection +
    // key name written by ConfigurationExtension.configureWithAppID).
    private static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
    private static final String CONFIG_KEY_APP_ID = "config.appID";
    private static final long SELF_INIT_TIMEOUT_SECONDS = 5;
    private static volatile boolean selfInitTried = false;

    @Override
    public void onNewToken(final @NonNull String token) {
        super.onNewToken(token);
        android.util.Log.d("Akhil", "New Token is  " + token);

        MobileCore.setPushIdentifier(token);
    }

    @Override
    public void onMessageReceived(final @NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        android.util.Log.d(
                "Akhil", "Akhil onMessageReceived in getting triggered (SDK MessagingService)");
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

        // If the host app has not yet registered the Messaging extension (e.g., the customer
        // initialized in MainActivity rather than Application.onCreate, or a killed-state push
        // arrived before customer init could complete), attempt to bootstrap the SDK from the
        // cached appId in persistence so the push receive event isn't lost.
        if (!MessagingExtension.isRegistered()) {
            Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Messaging extension is not registered. Attempting self-init from cached"
                            + " configuration.");
            selfInit(context);
        }

        final MessagingPushPayload payload = new MessagingPushPayload(remoteMessage);
        final Notification notification = MessagingPushBuilder.build(payload, context);

        // display notification
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(remoteMessage.getMessageId().hashCode(), notification);

        // track push notification received
        Messaging.handlePushReceived(remoteMessage.getMessageId(), remoteMessage.getData());
        return true;
    }

    /**
     * Bootstraps the AEP SDK from the persisted {@code appId} when the host app has not yet
     * registered the Messaging extension. This protects push receive tracking against the
     * cold-start race where {@link FirebaseMessagingService#onMessageReceived} fires before
     * customer-code initialization (in {@code Application.onCreate} or {@code
     * MainActivity.onCreate}) has completed.
     *
     * <p>Behavior:
     *
     * <ul>
     *   <li>Reads the {@code appId} cached by a prior successful {@link
     *       MobileCore#configureWithAppID(String)} call, via {@link
     *       ServiceProvider#getDataStoreService()}.
     *   <li>Delegates to {@link MobileCore#initialize(Application, InitOptions,
     *       com.adobe.marketing.mobile.AdobeCallback)}, which auto-discovers all AEP extensions on
     *       the classpath and applies the cached configuration.
     *   <li>Blocks the FCM service thread on a {@link CountDownLatch} bounded by {@value
     *       #SELF_INIT_TIMEOUT_SECONDS} seconds so the call cannot run beyond the FCM wakelock
     *       budget.
     *   <li>Runs at most once per process.
     * </ul>
     *
     * <p>Lifecycle auto-tracking is disabled for this code path. We are running on the FCM service
     * thread with no Activity context; firing lifecycle events from a non-foreground bootstrap is
     * not desired.
     *
     * <p>Returns silently (with a warning log) if no cached {@code appId} is found — that is the
     * first-ever-launch case where the host app has never configured the SDK, and there is nothing
     * to bootstrap from.
     *
     * <p>Idempotency: {@link MobileCore#initialize} and its underlying primitives ({@code
     * setApplication}, per-extension registration) are individually idempotent. It is safe for the
     * host app to also call {@code MobileCore.initialize} later from {@code Application.onCreate};
     * redundant registrations are short-circuited by the Core EventHub with no side effects.
     *
     * @param context the {@link Context} from FCM's {@code onMessageReceived}. Must have a non-null
     *     {@link Application} as its application context.
     */
    private static synchronized void selfInit(final @NonNull Context context) {
        if (selfInitTried) {
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

        // Read the appId persisted by a previous successful MobileCore.configureWithAppID call.
        // If none exists, the host app has never configured the SDK and there's nothing to
        // bootstrap from. configureWithAppID writes to the same NamedCollection / key via
        // ConfigurationExtension's internal AppIdManager.
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
                "Self-init: cached appId found, calling MobileCore.initialize.");

        // Disable automatic lifecycle tracking — we're on the FCM service thread, not in an
        // Activity context. Lifecycle events should be driven by the host app's foreground init.
        final InitOptions options = InitOptions.configureWithAppID(cachedAppId);
        options.setLifecycleAutomaticTrackingEnabled(false);

        final CountDownLatch latch = new CountDownLatch(1);
        try {
            MobileCore.initialize(application, options, ignored -> latch.countDown());
        } catch (final RuntimeException e) {
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Self-init: MobileCore.initialize threw an exception: " + e.getMessage());
            return;
        }

        try {
            final boolean completed = latch.await(SELF_INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                Log.warning(
                        MessagingPushConstants.LOG_TAG,
                        SELF_TAG,
                        "Self-init: MobileCore.initialize did not complete within "
                                + SELF_INIT_TIMEOUT_SECONDS
                                + "s; proceeding without confirmed registration.");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Self-init: interrupted while waiting for initialize callback.");
        }
    }

    /**
     * Reads the {@code appId} previously written to {@value #CONFIG_DATASTORE} / {@value
     * #CONFIG_KEY_APP_ID} by Core's {@code ConfigurationExtension.configureWithAppID}.
     *
     * @return the persisted appId, or {@code null} if not present or unreadable.
     */
    @androidx.annotation.Nullable private static String readCachedAppId() {
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
