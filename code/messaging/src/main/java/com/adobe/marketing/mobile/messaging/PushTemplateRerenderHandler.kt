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

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.TemplateIntent
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.Tracking.Keys.ACTION_ID
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.Tracking.Keys.MESSAGE_ID
import com.adobe.marketing.mobile.plugin.IUiTemplatePlugin
import com.adobe.marketing.mobile.services.Log

/**
 * Rebuilds a push-template notification after a re-render gesture (for example a carousel arrow).
 *
 * Tracks the gesture when the UI marked it trackable (it carries an action id), asks the UI template
 * plugin to rebuild from the merged message data, and posts the result under the notification's
 * existing id. A `null` rebuild posts nothing (there is no fallback on re-render).
 */
internal class PushTemplateRerenderHandler(
    private val tracker: PushResponseTracker = PushResponseTracker.MESSAGING,
    private val pluginProvider: () -> IUiTemplatePlugin? = {
        MobileCore.getPlugin(IUiTemplatePlugin::class.java)
    },
    private val poster: NotificationPoster = NotificationPoster.SYSTEM
) : NotificationInteractionHandler {

    override val runsInBackground: Boolean = true

    override fun handle(context: Context, intent: Intent) {
        val request = PushTemplateRerenderRequest.from(intent)
        if (request == null) {
            Log.warning(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "Re-render intent has no message data or message id; ignoring."
            )
            return
        }

        trackGesture(intent)
        val notification = rebuild(request) ?: return
        poster.post(context, request.notificationId, notification)
    }

    private fun trackGesture(intent: Intent) {
        intent.getStringExtra(ACTION_ID)
            ?.takeIf { it.isNotEmpty() }
            ?.let { tracker.track(intent, it) }
    }

    private fun rebuild(request: PushTemplateRerenderRequest): Notification? {
        val plugin = pluginProvider()
        if (plugin == null) {
            Log.warning(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "No IUiTemplatePlugin registered; cannot re-render. Register it in" +
                    " Application.onCreate so it is available when this receiver cold-starts the app."
            )
            return null
        }

        val notification = plugin.buildPushTemplateNotification(
            request.messageData(),
            MessagingPushTrackingProvider(request.messageId, request.payload)
        )
        if (notification == null) {
            Log.debug(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "Plugin returned no notification for re-render; nothing to post."
            )
        }
        return notification
    }

    private companion object {
        const val SELF_TAG = "PushTemplateRerenderHandler"
    }
}

/**
 * A re-render request read from a [MessagingPushConstants.NotificationAction.RERENDER] intent.
 *
 * @property messageId the push message id (required)
 * @property payload the original message data, including the reserved keys
 * @property templateState the UI template state attached to the gesture
 * @property inputResults text entered through RemoteInput, keyed by result key
 */
internal class PushTemplateRerenderRequest(
    val messageId: String,
    val payload: Map<String, String>,
    val templateState: Map<String, String>,
    val inputResults: Map<String, String>
) {

    /** The id the notification is posted with. */
    val notificationId: Int
        get() = MessagingPushTrackingProvider.notificationIdFor(messageId, payload)

    /** The message data for the plugin, merged in a fixed order (later entries win). */
    fun messageData(): Map<String, String> = payload + templateState + inputResults

    companion object {

        /** @return the request, or `null` if the intent has no message data or message id */
        fun from(intent: Intent): PushTemplateRerenderRequest? {
            val payload = intent.getBundleExtra(TemplateIntent.PUSH_PAYLOAD)?.toStringMap()
                ?: return null
            val messageId = payload[MESSAGE_ID]?.takeIf { it.isNotEmpty() } ?: return null
            return PushTemplateRerenderRequest(
                messageId = messageId,
                payload = payload,
                templateState = intent.getBundleExtra(TemplateIntent.TEMPLATE_STATE)?.toStringMap()
                    ?: emptyMap(),
                inputResults = RemoteInput.getResultsFromIntent(intent)?.toCharSequenceMap()
                    ?: emptyMap()
            )
        }

        private fun Bundle.toStringMap(): Map<String, String> =
            keySet().mapNotNull { key -> getString(key)?.let { key to it } }.toMap()

        private fun Bundle.toCharSequenceMap(): Map<String, String> =
            keySet().mapNotNull { key -> getCharSequence(key)?.let { key to it.toString() } }.toMap()
    }
}

/** Posts a notification. */
internal fun interface NotificationPoster {

    fun post(context: Context, notificationId: Int, notification: Notification)

    companion object {
        private const val SELF_TAG = "NotificationPoster"

        /** Posts through [NotificationManagerCompat]; a missing notification permission is logged. */
        val SYSTEM = NotificationPoster { context, notificationId, notification ->
            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (e: SecurityException) {
                Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Unable to post notification: ${e.message}"
                )
            }
        }
    }
}
