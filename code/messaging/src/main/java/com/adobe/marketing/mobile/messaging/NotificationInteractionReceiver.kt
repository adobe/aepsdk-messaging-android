/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.NotificationAction
import com.adobe.marketing.mobile.services.Log
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Entry point for the silent (broadcast) push-notification interactions Messaging owns.
 *
 * The receiver only dispatches: it runs the [NotificationInteractionHandler] registered for the intent
 * action, off the main thread (keeping the broadcast alive with [goAsync]) when the handler asks for
 * it. Supporting a new interaction means registering a new handler, not changing this class.
 *
 * - [NotificationAction.DISMISSED] -> [DismissInteractionHandler]
 * - [NotificationAction.INTERACTION] -> [SilentInteractionHandler]
 *
 * This receiver must stay `exported="false"`: it records push tracking from the intent's extras.
 */
class NotificationInteractionReceiver @VisibleForTesting internal constructor(
    private val handlers: Map<String, NotificationInteractionHandler>,
    private val backgroundExecutor: Executor
) : BroadcastReceiver() {

    constructor() : this(DEFAULT_HANDLERS, BACKGROUND_EXECUTOR)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }

        val handler = handlers[intent.action]
        if (handler == null) {
            Log.debug(
                MessagingPushConstants.LOG_TAG,
                SELF_TAG,
                "Ignoring intent with unrecognized action '${intent.action}'."
            )
            return
        }

        if (handler.runsInBackground) {
            runInBackground(handler, context.applicationContext ?: context, intent)
        } else {
            handler.handle(context, intent)
        }
    }

    private fun runInBackground(
        handler: NotificationInteractionHandler,
        context: Context,
        intent: Intent
    ) {
        val pendingResult: PendingResult? = goAsync()
        backgroundExecutor.execute {
            try {
                handler.handle(context, intent)
            } catch (t: Throwable) {
                Log.warning(
                    MessagingPushConstants.LOG_TAG,
                    SELF_TAG,
                    "Failed to handle interaction '${intent.action}': ${t.message}"
                )
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private companion object {
        const val SELF_TAG = "NotificationInteractionReceiver"

        // Single thread so rapid interactions on one notification are applied in order.
        val BACKGROUND_EXECUTOR: Executor by lazy { Executors.newSingleThreadExecutor() }

        val DEFAULT_HANDLERS: Map<String, NotificationInteractionHandler> by lazy {
            mapOf(
                NotificationAction.DISMISSED to DismissInteractionHandler(),
                NotificationAction.INTERACTION to SilentInteractionHandler()
            )
        }
    }
}
