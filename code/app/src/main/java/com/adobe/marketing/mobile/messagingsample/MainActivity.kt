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
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.services.ui.AEPMessage
import com.adobe.marketing.mobile.services.ui.AEPMessageSettings
import com.adobe.marketing.mobile.services.ui.FullscreenMessage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val customMessagingDelegate = CustomDelegate()
    private lateinit var spinner: Spinner
    private var triggerKey = "key"
    private var triggerValue = "value"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileCore.setFullscreenMessageDelegate(customMessagingDelegate)
        customMessagingDelegate.autoTrack = true

        spinner = findViewById(R.id.iamTypeSpinner)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.iam_types_array2,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        // spinner handling
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            // spinner handling
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                if (parent.getItemAtPosition(pos).equals("Korean character test")) {
                    triggerKey = "ryan"
                    triggerValue = "korean"
                } else if (parent.getItemAtPosition(pos).equals("Cyrillic character test")) {
                    triggerKey = "ryan"
                    triggerValue = "cyrillic"
                } else if (parent.getItemAtPosition(pos).equals("Surrogate pair character test")) {
                    triggerKey = "ryan"
                    triggerValue = "surrogate"
                } else if (parent.getItemAtPosition(pos).equals("Chinese character test")) {
                    triggerKey = "ryan"
                    triggerValue = "sctest"
                } else if (parent.getItemAtPosition(pos).equals("High ascii character test")) {
                    triggerKey = "ryan"
                    triggerValue = "highascii"
                } else if (parent.getItemAtPosition(pos)
                        .equals("Japanese 1+2 byte character test")
                ) {
                    triggerKey = "ryan"
                    triggerValue = "japanese"
                } else if (parent.getItemAtPosition(pos).equals("Chinese 4 byte character test")) {
                    triggerKey = "ryan"
                    triggerValue = "chinese"
                } else if (parent.getItemAtPosition(pos).equals("I8N 4 byte character test")) {
                    triggerKey = "ryan"
                    triggerValue = "4byte"
                } else if (parent.getItemAtPosition(pos).equals("Hebrew character test")) {
                    triggerKey = "ryan"
                    triggerValue = "hebrew"
                } else if (parent.getItemAtPosition(pos).equals("Ryan test no advanced settings")) {
                    triggerKey = "user"
                    triggerValue = "ryan"
                } else if (parent.getItemAtPosition(pos)
                        .equals("Ryan test with advanced settings")
                ) {
                    triggerKey = "ryan"
                    triggerValue = "test2"
                } else if (parent.getItemAtPosition(pos).equals("Free shot fullscreen")) {
                    triggerKey = "foo"
                    triggerValue = "bar"
                } else if (parent.getItemAtPosition(pos).equals("Banner IAM")) {
                    triggerKey = "ryan"
                    triggerValue = "banner"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // no-op
            }

        }

        btnGetLocalNotification.setOnClickListener {
            scheduleNotification(getNotification("Click on the notification for tracking"), 1000)
        }
        btnTriggerFullscreenIAM.setOnClickListener {
            val eventData = HashMap<String, Any>()
            eventData.put(triggerKey, triggerValue)

            val iamTrigger = Event.Builder(
                "test",
                "iamtest", "iamtest"
            )
                .setEventData(eventData)
                .build()
            MobileCore.dispatchEvent(iamTrigger, null)
        }

        btnHistoricalEvent1.setOnClickListener {
            val mask = arrayOf("firstEvent")
            var eventData = HashMap<String, Any>()
            eventData.put("firstEvent", "true")

            val triggerEvent1 = Event.Builder(
                "messaging event 1",
                "iamtest", "iamtest", mask
            )
                .setEventData(eventData)
                .build()
            MobileCore.dispatchEvent(triggerEvent1, null)
        }

        btnHistoricalEvent2.setOnClickListener {
            val mask = arrayOf("secondEvent")
            var eventData = HashMap<String, Any>()
            eventData.put("secondEvent", "true")

            val triggerEvent2 = Event.Builder(
                "messaging event 2",
                "iamtest", "iamtest", mask
            )
                .setEventData(eventData)
                .build()
            MobileCore.dispatchEvent(triggerEvent2, null)
        }

        btnHistoricalEvent3.setOnClickListener {
            val mask = arrayOf("thirdEvent")
            var eventData = HashMap<String, Any>()
            eventData.put("thirdEvent", "true")

            val triggerEvent3 = Event.Builder(
                "messaging event 3",
                "iamtest", "iamtest", mask
            )
                .setEventData(eventData)
                .build()
            MobileCore.dispatchEvent(triggerEvent3, null)
        }

        btnCheckSequence.setOnClickListener {
            var eventData = HashMap<String, Any>()
            eventData.put("checkSequence", "true")

            var checkSequenceEvent = Event.Builder(
                "check sequence",
                "iamtest", "iamtest"
            )
                .setEventData(eventData)
                .build()
            MobileCore.dispatchEvent(checkSequenceEvent, null)
        }

        allowIAMSwitch.setOnCheckedChangeListener { _, isChecked ->
            val message = if (isChecked) "Fullscreen IAM enabled" else "Fullscreen IAM disabled"
            Toast.makeText(
                this@MainActivity, message,
                Toast.LENGTH_SHORT
            ).show()
            customMessagingDelegate.showMessages = isChecked
        }

        btnTriggerLastIAM.setOnClickListener {
            Toast.makeText(
                this@MainActivity, "Showing last message.",
                Toast.LENGTH_SHORT
            ).show()
            customMessagingDelegate.getLastTriggeredMessage()?.show()
        }
        btnCleanEventHistory.setOnClickListener {
            Messaging.refreshInAppMessages()
        }

        intent?.extras?.apply {
            if (getString(FROM) == "action") {
                Messaging.handleNotificationResponse(intent, true, "button")
            } else {
                Messaging.handleNotificationResponse(intent, true, null)
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

    override fun onResume() {
        super.onResume()
        MobileCore.lifecycleStart(null)
    }

    override fun onPause() {
        super.onPause()
        MobileCore.lifecyclePause()
    }
}

class CustomDelegate : MessageDelegate() {
    private var currentMessage: FullscreenMessage? = null
    var showMessages = true

    override fun shouldShowMessage(fullscreenMessage: FullscreenMessage?): Boolean {
        if (!showMessages) {
            if (fullscreenMessage != null) {
                this.currentMessage = fullscreenMessage
                val message = (currentMessage?.settings as? AEPMessageSettings)?.parent as? Message
                println("message was suppressed: ${message?.messageId}")
                message?.track("message suppressed")
            }
        }
        return showMessages
    }

    override fun onShow(fullscreenMessage: FullscreenMessage?) {
        var message: AEPMessage = this.currentMessage as AEPMessage
        message
        this.currentMessage = fullscreenMessage
    }

    override fun onDismiss(fullscreenMessage: FullscreenMessage?) {
        this.currentMessage = fullscreenMessage
    }

    fun getLastTriggeredMessage(): FullscreenMessage? {
        return currentMessage
    }
}