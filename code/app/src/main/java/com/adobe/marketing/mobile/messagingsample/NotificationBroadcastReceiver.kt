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
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification? = intent?.getParcelableExtra(NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel("10001", "NOTIFICATION_CHANNEL_NAME", importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        notification?.contentIntent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
            Messaging.addPushTrackingDetails(this, "messageId", XDM_DATA)
        }, 0)
        val id = intent?.getIntExtra(NOTIFICATION_ID, 0)
        id?.let { notificationManager.notify(it, notification) }
    }

    companion object {
        const val NOTIFICATION_ID = "notification-id"
        const val NOTIFICATION = "notification"
        val XDM_DATA = mapOf("_xdm" to "{\n        \"cjm\": {\n          \"_experience\": {\n            \"customerJourneyManagement\": {\n              \"messageExecution\": {\n                \"messageExecutionID\": \"16-Sept-postman\",\n                \"messageID\": \"567111\",\n                \"journeyVersionID\": \"some-journeyVersionId\",\n                \"journeyVersionInstanceID\": \"someJourneyVersionInstanceID\"\n              }\n            }\n          }\n        }\n      }")
    }
}