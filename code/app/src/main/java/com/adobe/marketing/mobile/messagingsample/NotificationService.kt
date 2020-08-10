package com.adobe.marketing.mobile.messagingsample

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NotificationService : FirebaseMessagingService() {

    companion object {
        @JvmField
        val NOTIFICATION_ID = 0x12E45
        const val channelId = "messaging_notification_channel"
        const val NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED_ACTION"
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            val channelName = "Messaging Notifications Channel"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Settings for push notification"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            if (message?.data?.isNotEmpty() == true) {
                setContentTitle(message.data?.get("title") ?: "")
                setContentText(message.data?.get("body") ?: "")
            } else {
                setContentTitle(message?.notification?.title ?: "")
                setContentText(message?.notification?.body ?: "")
            }

            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(PendingIntent.getActivity(this@NotificationService, 0, Intent(this@NotificationService, MainActivity::class.java), 0))
            setDeleteIntent(PendingIntent.getBroadcast(this@NotificationService, 0, Intent(NOTIFICATION_DELETED_ACTION), 0))
            setAutoCancel(true)
        }

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}