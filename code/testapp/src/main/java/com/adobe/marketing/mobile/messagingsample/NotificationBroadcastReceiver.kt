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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adobe.marketing.mobile.Messaging


class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification? = intent?.getParcelableExtra(NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel("10001", "Messaging Sample Notification Channel", importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notification?.contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
                Messaging.addPushTrackingDetails(this, "messageId", XDM_DATA)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
                Messaging.addPushTrackingDetails(this, "messageId", XDM_DATA)
            }, PendingIntent.FLAG_IMMUTABLE)
        }
        val id = intent?.getIntExtra(NOTIFICATION_ID, 0)
        id?.let { notificationManager.notify(it, notification) }
    }

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"
        val XDM_DATA =
            mapOf("_xdm" to "{\n        \"cjm\": {\n          \"_experience\": {\n            \"customerJourneyManagement\": {\n              \"messageExecution\": {\n                \"messageExecutionID\": \"16-Sept-postman\",\n                \"messageID\": \"567111\",\n                \"journeyVersionID\": \"some-journeyVersionId\",\n                \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n              }\n            }\n          }\n        }\n      }")
    }
}