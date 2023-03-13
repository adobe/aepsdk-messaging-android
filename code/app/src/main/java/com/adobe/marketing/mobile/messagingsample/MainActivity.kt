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

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.services.MessagingDelegate
import com.adobe.marketing.mobile.services.ui.FullscreenMessage
import com.adobe.marketing.mobile.util.StringUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : ComponentActivity() {
    private val customMessagingDelegate = CustomDelegate()

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileCore.setMessagingDelegate(customMessagingDelegate)

        // setup ui interaction listeners
        setupButtonClickListeners()
        // setupSpinnerItemSelectedListener()
        setupSwitchListeners()

        // handle push notification interactions
        intent?.extras?.apply {
            if (getString(FROM) == "action") {
                Messaging.handleNotificationResponse(intent, true, "button")
            } else {
                Messaging.handleNotificationResponse(intent, true, null)
            }
        }
    }

    private fun setupButtonClickListeners() {
        btnGetLocalNotification.setOnClickListener {
            scheduleNotification(getNotification("Click on the notification for tracking"), 1000)
        }

        btnTriggerFullscreenIAM.setOnClickListener {
            val trigger = editText.text.toString()
            if (StringUtils.isNullOrEmpty(trigger) || trigger == "Trigger IAM") {
                Toast.makeText(
                    this@MainActivity,
                    "Empty or default trigger string provided. Triggering default message.",
                    Toast.LENGTH_SHORT
                ).show()
                MobileCore.trackAction("samus", null)
            } else {
                MobileCore.trackAction(trigger, null)
            }
        }

        btnTriggerLastIAM.setOnClickListener {
            Toast.makeText(
                this@MainActivity, "Showing last message.",
                Toast.LENGTH_SHORT
            ).show()
            customMessagingDelegate.getLastTriggeredMessage()?.show()
        }

        btnRefreshInAppMessages.setOnClickListener {
            Messaging.refreshInAppMessages()
        }
    }

    private fun setupSwitchListeners() {
        allowIAMSwitch.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Fullscreen IAM enabled" else "Fullscreen IAM disabled"
            Toast.makeText(
                this@MainActivity, message,
                Toast.LENGTH_SHORT
            ).show()
            customMessagingDelegate.showMessages = isChecked
        }
    }

    private fun scheduleNotification(notification: Notification?, delay: Int) {
        val notificationIntent = Intent(this, NotificationBroadcastReceiver::class.java)
        notificationIntent.putExtra(NotificationBroadcastReceiver.NOTIFICATION_ID, 1)
        notificationIntent.putExtra(NotificationBroadcastReceiver.NOTIFICATION, notification)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
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
        val pendingIntent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                this,
                System.currentTimeMillis().toInt(),
                actionReceiver,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                System.currentTimeMillis().toInt(),
                actionReceiver,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        builder.addAction(
            R.drawable.ic_launcher_background, "buttonAction",
            pendingIntent
        )
        return builder.build()
    }

    companion object {
        const val FROM = "from"
    }

    override fun onResume() {
        super.onResume()
        MobileCore.lifecycleStart(null)
    }

    override fun onPause() {
        super.onPause()
        MobileCore.lifecyclePause()
    }

    class CustomDelegate : MessagingDelegate {
        private var currentMessage: Message? = null
        private var webview: WebView? = null
        var showMessages = true

        override fun shouldShowMessage(fullscreenMessage: FullscreenMessage?): Boolean {
            // access to the whole message from the parent
            fullscreenMessage?.also {
                this.currentMessage = (fullscreenMessage.parent) as? Message
                this.webview = currentMessage?.webView

                // if we're not showing the message now, we can save it for later
                if (!showMessages) {
                    println("message was suppressed: ${currentMessage?.id}")
                    currentMessage?.track(
                        "message suppressed",
                        MessagingEdgeEventType.IN_APP_TRIGGER
                    )
                }
            }
            return showMessages
        }

        override fun onShow(fullscreenMessage: FullscreenMessage?) {
            this.currentMessage = fullscreenMessage?.parent as Message?
            this.webview = currentMessage?.webView

            // example: in-line handling of javascript calls in the AJO in-app message html
            // the content callback will contain the output of (function() { return 'inline js return value'; })();
            currentMessage?.handleJavascriptMessage("handler_name") { content ->
                if (content != null) {
                    println("magical handling of our content from js! content is: $content")
                    currentMessage?.track(content, MessagingEdgeEventType.IN_APP_INTERACT)
                }
            }

            // example: running javascript on the webview created by the Messaging extension.
            // running javascript content must be done on the ui thread
            webview?.post {
                webview?.evaluateJavascript("(function() { return 'function return value'; })();") { content ->
                    if (content != null) {
                        println("js function return content is: $content")
                    }
                }
            }
        }

        override fun onDismiss(fullscreenMessage: FullscreenMessage?) {
            this.currentMessage = fullscreenMessage?.parent as Message?
        }

        fun getLastTriggeredMessage(): Message? {
            return currentMessage
        }
    }
}