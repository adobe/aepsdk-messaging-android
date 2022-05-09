/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile.messagingsample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adobe.marketing.mobile.MessagingPushPayload

/**
 * Listens for broadcasts sent from the Messaging extension
 */
class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        val packageName = context?.packageName
        // these values are broadcast when a silent push notification is handled by the Messaging extension
        var pushPayload: MessagingPushPayload?
        var messageId: String?

        action?.also { action ->
            packageName?.also { packageName ->
                when (action) {
                    "${packageName}_${MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_CLICKED}" -> {
                        Log.d(TAG, action)
                    }
                    "${packageName}_${MessagingPushPayload.ACTION_KEY.ACTION_NOTIFICATION_DELETED}" -> {
                        Log.d(TAG, "${packageName}_adb_action_notification_deleted")
                    }
                    "${packageName}_${MessagingPushPayload.ACTION_KEY.ACTION_BUTTON_CLICKED}" -> {
                        Log.d(TAG, "${packageName}_adb_action_button_clicked")
                    }
                    "${packageName}_${MessagingPushPayload.ACTION_KEY.ACTION_NORMAL_NOTIFICATION_CREATED}" -> {
                        Log.d(TAG, "${packageName}_adb_action_notification_created")
                    }
                    "${packageName}_${MessagingPushPayload.ACTION_KEY.ACTION_SILENT_NOTIFICATION_CREATED}" -> {
                        Log.d(TAG, "${packageName}_adb_action_silent_notification_created")
                        pushPayload = intent.extras?.getParcelable("pushPayload")
                        messageId = intent.extras?.getString("messageId")
                    }
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"
        val XDM_DATA =
            mapOf("_xdm" to "{\n        \"cjm\": {\n          \"_experience\": {\n            \"customerJourneyManagement\": {\n              \"messageExecution\": {\n                \"messageExecutionID\": \"16-Sept-postman\",\n                \"messageID\": \"567111\",\n                \"journeyVersionID\": \"some-journeyVersionId\",\n                \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n              }\n            }\n          }\n        }\n      }")
        private const val TAG = "NotificationBroadcast"
    }
}