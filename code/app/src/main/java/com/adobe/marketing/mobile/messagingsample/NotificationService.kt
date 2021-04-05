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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.adobe.marketing.mobile.AEPMessagingFCMPushPayload
import com.adobe.marketing.mobile.Messaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    companion object {
        @JvmField
        val NOTIFICATION_ID = 0x12E45
        var channelId = "messaging_notification_channel"
        const val NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED_ACTION"
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)

        val payload = AEPMessagingFCMPushPayload(message)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val channelName = "some channel name"
            channelId = if (payload.channelId != null) payload.channelId else channelId
            val channel = NotificationChannel(channelId, channelName, payload.importance).apply {
                description = "Settings for push notification"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setContentTitle(payload.title)
            setContentTitle(payload.body)

            priority = payload.notificationPriority
            setContentIntent(PendingIntent.getActivity(this@NotificationService, 0, Intent(this@NotificationService, MainActivity::class.java).apply {
                Messaging.addPushTrackingDetails(this, message?.messageId, message?.data)
            }, 0))
            setDeleteIntent(PendingIntent.getBroadcast(this@NotificationService, 0, Intent(this@NotificationService.applicationContext, NotificationDeleteReceiver::class.java).apply {
                Messaging.addPushTrackingDetails(this, message?.messageId, message?.data)
            }, 0))
            setAutoCancel(true)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}