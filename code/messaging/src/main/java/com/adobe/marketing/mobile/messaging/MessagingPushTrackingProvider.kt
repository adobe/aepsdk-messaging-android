/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
// import android.os.Bundle // re-render only
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.NotificationAction
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.PushInteractionType
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.TemplateIntent
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.Tracking
import com.adobe.marketing.mobile.plugin.IPushTemplateTrackingProvider
import com.adobe.marketing.mobile.plugin.PushInteraction
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Messaging's [IPushTemplateTrackingProvider]: the single channel the UI template add-on uses to obtain
 * every [PendingIntent] for a push-template notification, so Messaging keeps ownership of tracking and
 * intent handling while the add-on owns rendering.
 *
 * Routing per [PushInteraction.type]:
 * - `content_click` -> [MessagingPushTrackerActivity], action [NotificationAction.OPENED]
 * - `button_click` -> [MessagingPushTrackerActivity], action [NotificationAction.BUTTON_CLICKED]
 * - `dismiss` -> [NotificationInteractionReceiver], action [NotificationAction.DISMISSED]
 * - `input_submit` -> currently disabled (commented out) until a template uses it, so it takes the
 *   fallback route below. When enabled: [NotificationInteractionReceiver], action
 *   [NotificationAction.INTERACTION]
 * - `rerender` -> currently disabled (commented out) until a template uses it, so it takes the
 *   fallback route below. When enabled: [NotificationInteractionReceiver], action
 *   `NotificationAction.RERENDER`; the receiver rebuilds the notification through the plugin and
 *   re-posts it
 * - any other type -> fallback: with an `actionUri`, treated as an open; otherwise a silent
 *   [NotificationAction.INTERACTION]
 *
 * Tracking intents carry `messageId` + XDM (via [Messaging.addPushTrackingDetails]) and the message
 * data, like the existing basic push flow. Re-render intents carry the message data and template state
 * as separate nested bundles so they round-trip without mixing with tracking extras.
 *
 * Every intent gets a deterministic data URI (`adbpush://{notificationId}/{type}?...`), so the same
 * interaction always resolves to the same [PendingIntent] (cancel / replace works, no collisions).
 *
 * @param messageId the push message id
 * @param data the message data (push payload plus the reserved `messageId` / `notificationId` keys)
 * @param contextProvider supplies the application [Context]; defaults to Core's `ServiceProvider`
 */
internal class MessagingPushTrackingProvider @JvmOverloads constructor(
    private val messageId: String,
    private val data: Map<String, String>,
    private val contextProvider: () -> Context? = {
        ServiceProvider.getInstance().appContextService.applicationContext
    }
) : IPushTemplateTrackingProvider {

    private val notificationId: Int = notificationIdFor(messageId, data)

    override fun getPendingIntent(interaction: PushInteraction): PendingIntent? {
        val context = contextProvider()
        if (context == null) {
            Log.warning(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "Application context is null, cannot build a PendingIntent."
            )
            return null
        }

        return when (interaction.type) {
            PushInteractionType.CONTENT_CLICK ->
                activityPendingIntent(context, NotificationAction.OPENED, interaction)

            PushInteractionType.BUTTON_CLICK ->
                activityPendingIntent(context, NotificationAction.BUTTON_CLICKED, interaction)

            PushInteractionType.DISMISS ->
                broadcastPendingIntent(context, NotificationAction.DISMISSED, interaction)

            // Input submit is disabled until a template uses it; uncomment to enable.
            // PushInteractionType.INPUT_SUBMIT ->
            //     broadcastPendingIntent(context, NotificationAction.INTERACTION, interaction)

            // Re-render is disabled until a template uses it; uncomment to enable.
            // PushInteractionType.RERENDER -> rerenderPendingIntent(context, interaction)

            else -> {
                Log.debug(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Unknown interaction type '${interaction.type}', using the fallback route."
                )
                if (!interaction.actionUri.isNullOrEmpty()) {
                    activityPendingIntent(context, NotificationAction.OPENED, interaction)
                } else {
                    broadcastPendingIntent(context, NotificationAction.INTERACTION, interaction)
                }
            }
        }
    }

    /**
     * Builds a tracking [PendingIntent] targeting [MessagingPushTrackerActivity] for a user-facing
     * interaction (content tap / action button).
     */
    private fun activityPendingIntent(
        context: Context,
        action: String,
        interaction: PushInteraction
    ): PendingIntent {
        val intent = Intent(action)
        intent.setClass(context.applicationContext, MessagingPushTrackerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        intent.data = identityUri(interaction)
        populateTrackingIntent(intent, interaction)
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(TemplateIntent.REQUEST_CODE, pendingIntentFlags(interaction))!!
    }

    /**
     * Builds a tracking [PendingIntent] delivered as a silent broadcast to
     * [NotificationInteractionReceiver] (dismiss / interaction).
     */
    private fun broadcastPendingIntent(
        context: Context,
        action: String,
        interaction: PushInteraction
    ): PendingIntent {
        val intent = Intent(context, NotificationInteractionReceiver::class.java)
        intent.action = action
        intent.data = identityUri(interaction)
        populateTrackingIntent(intent, interaction)
        return PendingIntent.getBroadcast(
            context,
            TemplateIntent.REQUEST_CODE,
            intent,
            pendingIntentFlags(interaction)
        )
    }

    // Re-render is disabled until a template uses it; uncomment to enable.
    // /**
    //  * Builds a re-render [PendingIntent] delivered to [NotificationInteractionReceiver]. The message
    //  * data and the template state travel as separate nested bundles; tracking details stay flat so the
    //  * receiver can track the gesture (only when an `actionId` is set).
    //  */
    // private fun rerenderPendingIntent(context: Context, interaction: PushInteraction): PendingIntent {
    //     val intent = Intent(context, NotificationInteractionReceiver::class.java)
    //     intent.action = NotificationAction.RERENDER
    //     intent.data = identityUri(interaction)
    //     interaction.actionId?.takeIf { it.isNotEmpty() }?.let {
    //         intent.putExtra(Tracking.Keys.ACTION_ID, it)
    //     }
    //     intent.putExtra(TemplateIntent.PUSH_PAYLOAD, data.toBundle())
    //     interaction.templateExtras?.let { intent.putExtra(TemplateIntent.TEMPLATE_STATE, it.toBundle()) }
    //     Messaging.addPushTrackingDetails(intent, messageId, data)
    //     return PendingIntent.getBroadcast(
    //         context,
    //         TemplateIntent.REQUEST_CODE,
    //         intent,
    //         pendingIntentFlags(interaction)
    //     )
    // }

    /**
     * Attaches the action details, message data, and push tracking details (messageId + XDM) to
     * [intent], mirroring the existing basic push flow so downstream tracking is identical.
     */
    private fun populateTrackingIntent(intent: Intent, interaction: PushInteraction) {
        interaction.actionUri?.takeIf { it.isNotEmpty() }?.let {
            intent.putExtra(Tracking.Keys.ACTION_URI, it)
        }
        interaction.actionId?.takeIf { it.isNotEmpty() }?.let {
            intent.putExtra(Tracking.Keys.ACTION_ID, it)
        }
        for ((key, value) in data) {
            if (value.isNotEmpty()) {
                intent.putExtra(key, value)
            }
        }
        Messaging.addPushTrackingDetails(intent, messageId, data)
    }

    /**
     * Builds a deterministic data URI that gives each [PendingIntent] a unique identity. It is not
     * used for routing and is never read by the receiving component.
     *
     * Android matches PendingIntents by request code plus [Intent.filterEquals] (action, data,
     * type, component and categories), ignoring extras. Because every PendingIntent here uses the
     * fixed [TemplateIntent.REQUEST_CODE] and [PendingIntent.FLAG_UPDATE_CURRENT], intents without
     * a distinct identity would share one PendingIntent, and the last one created would overwrite
     * the extras of the others. For example, every button would fire the last button's action,
     * or an older notification would carry a newer notification's tracking data.
     *
     * The URI is made of the notification id, the interaction type, the action id and uri, and
     * the template state (sorted so the order is stable), for example
     * `adbpush://12345/button_click?id=btn_1&uri=myapp://offer&s.index=1`. As a result:
     * - each notification and each interaction within it gets its own PendingIntent;
     * - a re-render with the same state updates the existing PendingIntent in place instead of
     *   leaking a new one (unlike the random request codes used by the legacy push flow);
     * - a different template state (for example a carousel index) produces a separate
     *   PendingIntent, so stale actions cannot fire with the wrong state.
     *
     * All intents are explicit and nothing registers [TemplateIntent.IDENTITY_SCHEME], so the URI
     * does not make any intent resolvable from outside the app.
     */
    private fun identityUri(interaction: PushInteraction): Uri {
        val builder = Uri.Builder()
            .scheme(TemplateIntent.IDENTITY_SCHEME)
            .authority(notificationId.toString())
            .appendPath(interaction.type)
        interaction.actionId?.let { builder.appendQueryParameter("id", it) }
        interaction.actionUri?.let { builder.appendQueryParameter("uri", it) }
        interaction.templateExtras?.toSortedMap()?.forEach { (key, value) ->
            builder.appendQueryParameter("s.$key", value)
        }
        return builder.build()
    }

    private fun pendingIntentFlags(interaction: PushInteraction): Int {
        val mutabilityFlag =
            if (interaction.mutablePendingIntent) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
    }

    companion object {
        private const val SELF_TAG = "MessagingPushTrackingProvider"

        /**
         * The id Messaging posts a push-template notification with: the reserved `notificationId`
         * message-data key when present, otherwise [MessagingPushUtils.getNotificationId].
         */
        @JvmStatic
        fun notificationIdFor(messageId: String, data: Map<String, String>): Int =
            data[Tracking.Keys.NOTIFICATION_ID]?.toIntOrNull()
                ?: MessagingPushUtils.getNotificationId(messageId)

        // Re-render is disabled until a template uses it; uncomment to enable.
        // private fun Map<String, String>.toBundle(): Bundle =
        //     Bundle().also { bundle -> forEach { (key, value) -> bundle.putString(key, value) } }
    }
}
