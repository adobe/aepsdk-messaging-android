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

import android.content.Context
import android.content.Intent
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.Tracking.Keys.ACTION_DISMISS
import com.adobe.marketing.mobile.messaging.MessagingPushConstants.Tracking.Keys.ACTION_ID

/**
 * Handles one kind of interaction intent delivered to [NotificationInteractionReceiver], selected by
 * the intent action.
 */
internal interface NotificationInteractionHandler {

    /**
     * Whether [handle] may do slow work (for example image loading) and must run off the main thread.
     * The receiver then keeps the broadcast alive until [handle] returns.
     */
    val runsInBackground: Boolean
        get() = false

    fun handle(context: Context, intent: Intent)
}

/** Records a push notification response with Messaging. */
internal fun interface PushResponseTracker {

    /**
     * @param intent the interaction intent carrying the push tracking details
     * @param customActionId the action id to record, or `null`
     */
    fun track(intent: Intent, customActionId: String?)

    companion object {
        /** Tracks through [Messaging.handleNotificationResponse] without marking the app as opened. */
        val MESSAGING = PushResponseTracker { intent, customActionId ->
            Messaging.handleNotificationResponse(intent, false, customActionId)
        }
    }
}

/** Tracks a notification dismissal. */
internal class DismissInteractionHandler(
    private val tracker: PushResponseTracker = PushResponseTracker.MESSAGING
) : NotificationInteractionHandler {

    override fun handle(context: Context, intent: Intent) {
        tracker.track(intent, ACTION_DISMISS)
    }
}

/** Tracks a silent interaction by its action id, without opening the app. */
internal class SilentInteractionHandler(
    private val tracker: PushResponseTracker = PushResponseTracker.MESSAGING
) : NotificationInteractionHandler {

    override fun handle(context: Context, intent: Intent) {
        tracker.track(intent, intent.getStringExtra(ACTION_ID))
    }
}
