package com.adobe.marketing.mobile.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adobe.marketing.mobile.Messaging

class NotificationDeleteReceiver : BroadcastReceiver() {
    private val dismissAction: String = "Dismiss"
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            Messaging.handleNotificationResponse(
                intent,
                false,
                dismissAction
            )
        }
    }
}