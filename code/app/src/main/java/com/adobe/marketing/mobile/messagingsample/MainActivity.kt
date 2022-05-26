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

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Messaging
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val assuranceSessionUrl = "{Your Session URL}"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Assurance.startSession(assuranceSessionUrl)
        btn_getLocalNotification.setOnClickListener {
            scheduleNotification(getNotification("Click on the notification for tracking"), 1000)
        }
        // Events to handle based on the contents of the received Intent:
        // 1. AEPMessaging extension push interaction tracking if the extension is not handling tracking. See #1 below.
        // 2. A deeplink triggered from a notification button press present in the intent data. See #2 below.

        intent?.extras?.apply {
            // event #1: handle tracking
            //if (getString(FROM) == "action") {
            //     Messaging.handleNotificationResponse(intent, true, "button")
            //} else {
            //     Messaging.handleNotificationResponse(intent, true, null)
            //}
        }

        intent?.data?.apply {
            //  event #2: handle deeplink
            val deeplink = intent?.data as Uri
            if (deeplink.scheme.equals("push-test")) {
                // handle deeplink
            }
        }
    }

    private fun scheduleNotification(notification: Notification?, delay: Int) {
        val notificationIntent = Intent(this, NotificationBroadcastReceiver::class.java)
        notificationIntent.putExtra(NotificationBroadcastReceiver.NOTIFICATION_ID, 1)
        notificationIntent.putExtra(NotificationBroadcastReceiver.NOTIFICATION, notification)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val futureInMillis = SystemClock.elapsedRealtime() + delay
        val alarmManager = (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis] = pendingIntent
    }

    private fun getNotification(content: String): Notification? {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "default")
        builder.setContentTitle("Scheduled Notification")
        builder.setContentText(content)
        builder.setSmallIcon(R.drawable.ic_launcher_background)
        builder.setAutoCancel(true)
        builder.setChannelId("10001")
        val actionReceiver = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(FROM, "action")
            Messaging.addPushTrackingDetails(
                this,
                "messageId",
                NotificationBroadcastReceiver.XDM_DATA
            )
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), actionReceiver, 0)
        builder.addAction(
            R.drawable.ic_launcher_background, "buttonAction",
            pendingIntent
        )
        return builder.build()
    }

    companion object {
        const val FROM = "from"
    }
}