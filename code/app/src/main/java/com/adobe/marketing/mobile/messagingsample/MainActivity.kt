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
import android.app.AlertDialog
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.MessagingUtils
import com.adobe.marketing.mobile.messagingsample.databinding.ActivityMainBinding
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.ui.InAppMessage
import com.adobe.marketing.mobile.services.ui.Presentable
import com.adobe.marketing.mobile.services.ui.PresentationDelegate
import com.adobe.marketing.mobile.services.ui.PresentationListener
import com.adobe.marketing.mobile.util.StringUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private val customMessagingDelegate = CustomDelegate()
    private var triggerKey = "key"
    private var triggerValue = "value"
    
    companion object {
        private const val LOG_TAG = "MainActivity"
        const val FROM = "from"
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
                Log.d(
                    LOG_TAG,
                    "Notification permission granted"
                )
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.d(
                    LOG_TAG,
                    "Notification Permission: Not granted"
                )
                showNotificationPermissionRationale()
            } else {
                // Directly ask for the permission
                Log.d(
                    LOG_TAG,
                    "Requesting notification permission"
                )
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Log.d(
                LOG_TAG,
                "Notification permission granted"
            )
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult<String, Boolean>(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            Log.d(
                LOG_TAG,
                "Notification permission granted"
            )
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= 33) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showNotificationPermissionRationale()
                } else {
                    Log.d(
                        LOG_TAG,
                        "Grant notification permission from settings"
                    )
                    Toast.makeText(
                        this,
                        "Grant notification permission from settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Grant notification permission")
            .setMessage("Notification permission is required to show notifications")
            .setPositiveButton("Ok") { dialog: DialogInterface?, which: Int ->
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private enum class generatedIAMSpinnerValues(val value: String) {
        BOTTOM_BANNER("Bottom Banner"),
        CENTER_BANNER("Center Banner"),
        CENTER_MODAL("Center Modal"),
        TOP_BANNER("Top Banner"),
        TOP_HALF("Top Half"),
        BOTTOM_HALF("Bottom Half")
    }

    private enum class generatedIAMParameters(val values: Array<Any>) {
        BOTTOM_BANNER(
                arrayOf(
                        10,
                        95,
                        "bottom",
                        0,
                        "center",
                        1,
                        "bottom",
                        "bottom",
                        "FFFFFF",
                        "116975",
                        0.10,
                        0,
                        false
                )
        ),
        CENTER_BANNER(
                arrayOf(
                        10,
                        100,
                        "center",
                        0,
                        "center",
                        10,
                        "fade",
                        "fade",
                        "FFFFFF",
                        "FFC300",
                        0.81,
                        0,
                        false
                )
        ),
        CENTER_MODAL(
                arrayOf(
                        75,
                        75,
                        "center",
                        10,
                        "center",
                        5,
                        "fade",
                        "fade",
                        "ADD8E6",
                        "3A3A3A",
                        0.8,
                        20,
                        false
                )
        ),
        TOP_BANNER(
                arrayOf(
                        15,
                        90,
                        "top",
                        2,
                        "center",
                        2,
                        "top",
                        "top",
                        "FFFFFF",
                        "913622",
                        0.75,
                        75,
                        true
                )
        ),
        TOP_HALF(
                arrayOf(
                        50,
                        100,
                        "top",
                        0,
                        "left",
                        0,
                        "left",
                        "right",
                        "FFFFFF",
                        "5E7072",
                        0.5,
                        90,
                        true
                )
        ),
        BOTTOM_HALF(
                arrayOf(
                        50,
                        100,
                        "bottom",
                        0,
                        "left",
                        0,
                        "right",
                        "left",
                        "FFFFFF",
                        "85B085",
                        0.90,
                        0,
                        false
                )
        )
    }

    var propositions = mutableListOf<Proposition>()
    private lateinit var binding: ActivityMainBinding
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

        // Request push permissions for Android 33
        askNotificationPermission()
    }

    private fun setupButtonClickListeners() {
        btnGetLocalNotification.setOnClickListener {
            scheduleNotification(getNotification("Click on the notification for tracking"), 1000)
        }

        btnTriggerFullscreenIAM.setOnClickListener {
            val trigger = editText.text.toString()
            if (StringUtils.isNullOrEmpty(trigger) || trigger == "Trigger IAM") {
                Toast.makeText(
                    this@MainActivity, "Empty or default trigger string provided. Triggering default message.",
                    Toast.LENGTH_SHORT
                ).show()
                MobileCore.trackAction("samus", null)
            } else {
                MobileCore.trackAction(trigger, null)
            }
        }
        btnCheckFeedMessages.setOnClickListener {
            startActivity(Intent(this, ScrollingFeedActivity::class.java))
        }

        btnCheckCodeBased.setOnClickListener {
            startActivity(Intent(this, CodeBasedExperienceActivity::class.java))
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

    private fun handleGeneratedIamValues(parent: AdapterView<*>, pos: Int) {
        triggerKey = "foo"
        triggerValue = "bar"
        when (parent.getItemAtPosition(pos)) {
            generatedIAMSpinnerValues.BOTTOM_BANNER.value -> generateAndDispatchEdgeResponseEvent(
                    generatedIAMParameters.BOTTOM_BANNER
            )
            generatedIAMSpinnerValues.CENTER_BANNER.value -> generateAndDispatchEdgeResponseEvent(
                    generatedIAMParameters.CENTER_BANNER
            )
            generatedIAMSpinnerValues.CENTER_MODAL.value -> generateAndDispatchEdgeResponseEvent(
                    generatedIAMParameters.CENTER_MODAL
            )
            generatedIAMSpinnerValues.TOP_BANNER.value -> generateAndDispatchEdgeResponseEvent(
                    generatedIAMParameters.TOP_BANNER
            )
            generatedIAMSpinnerValues.TOP_HALF.value -> generateAndDispatchEdgeResponseEvent(
                    generatedIAMParameters.TOP_HALF
            )
            generatedIAMSpinnerValues.BOTTOM_HALF.value -> generateAndDispatchEdgeResponseEvent(
                    generatedIAMParameters.BOTTOM_HALF
            )
        }
    }

    private fun scheduleNotification(notification: Notification?, delay: Int) {
        val notificationIntent = Intent(this, NotificationBroadcastReceiver::class.java)
        notificationIntent.putExtra(NotificationBroadcastReceiver.NOTIFICATION_ID, 1)
        notificationIntent.putExtra(NotificationBroadcastReceiver.NOTIFICATION, notification)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
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
            PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), actionReceiver, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getBroadcast(this, System.currentTimeMillis().toInt(), actionReceiver, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        builder.addAction(R.drawable.ic_launcher_background, "buttonAction",
                pendingIntent)
        return builder.build()
    }

    override fun onResume() {
        super.onResume()
        MobileCore.lifecycleStart(null)
    }

    override fun onPause() {
        super.onPause()
        MobileCore.lifecyclePause()
    }

    // local AJO proposition event generation for testing
    private fun generateAndDispatchEdgeResponseEvent(generatedIAMParameters: generatedIAMParameters) {
        // extract parameter values from generatedIAMParams enum
        val height = generatedIAMParameters.values[0]
        val width = generatedIAMParameters.values[1]
        val vAlign = generatedIAMParameters.values[2]
        val vInset = generatedIAMParameters.values[3]
        val hAlign = generatedIAMParameters.values[4]
        val hInset = generatedIAMParameters.values[5]
        val displayAnimation = generatedIAMParameters.values[6]
        val dismissAnimation = generatedIAMParameters.values[7]
        val iamColor = generatedIAMParameters.values[8]
        val bdColor = generatedIAMParameters.values[9]
        val opacity = generatedIAMParameters.values[10]
        val cornerRadius = generatedIAMParameters.values[11]
        val uiTakeover = generatedIAMParameters.values[12]
        // simulate edge response event containing an iam payload
        val payload = JSONObject(
                "{\n" +
                        "  \"scopeDetails\" : {\n" +
                        "    \"characteristics\" : {\n" +
                        "      \"cjmEvent\" : {\"messageExecution\":{\"messageExecutionID\":\"5d1ab71b-9c79-47ab-8cd0-3094867aec9a\",\"messageID\":\"e34d32f4-16ab-440a-91f6-c681dfb8c5bd\",\"messageType\":\"marketing\",\"campaignID\":\"1671fb9f-7096-4edc-8914-53ffe51bad57\",\"campaignVersionID\":\"294de099-e449-4e40-860b-a8e8d5a28c13\",\"campaignActionID\":\"01decab6-313b-4764-b7af-9f91ba2efdbf\",\"messagePublicationID\":\"c925f829-a936-4f10-b64e-ef244faebc69\"},\"messageProfile\":{\"channel\":{\"_id\":\"https://ns.adobe.com/xdm/channels/inApp\",\"_type\":\"https://ns.adobe.com/xdm/channel-types/inApp\"},\"messageProfileID\":\"efa27a43-258b-4b55-93c0-05883f3b3eee\"}}\n" +
                        "    },\n" +
                        "    \"correlationID\" : \"e34d32f4-16ab-440a-91f6-c681dfb8c5bd\",\n" +
                        "    \"decisionProvider\" : \"AJO\"\n" +
                        "  },\n" +
                        "  \"scope\" : \"mobileapp://com.adobe.marketing.mobile.messagingsample\",\n" +
                        "  \"items\": [\n" +
                        "    {\n" +
                        "      \"id\": \"8649aa56-ad0e-47d3-b0e6-8214ad032620\",\n" +
                        "      \"schema\": \"https:\\/\\/ns.adobe.com\\/personalization\\/json-content-item\",\n" +
                        "      \"data\": {\n" +
                        "        \"content\": \"{\\\"version\\\":1,\\\"rules\\\":[{\\\"condition\\\":{\\\"type\\\":\\\"group\\\",\\\"definition\\\":{\\\"conditions\\\":[{\\\"definition\\\":{\\\"key\\\":\\\"foo\\\",\\\"matcher\\\":\\\"eq\\\",\\\"values\\\":[\\\"bar\\\"]},\\\"type\\\":\\\"matcher\\\"}],\\\"logic\\\":\\\"and\\\"}},\\\"consequences\\\":[{\\\"id\\\":\\\"e56a8dbb-c5a4-4219-9563-0496e00b4083\\\",\\\"type\\\":\\\"cjmiam\\\",\\\"detail\\\":{\\\"remoteAssets\\\":[\\\"https://upload.wikimedia.org/wikipedia/en/thumb/6/6d/Seattle_Mariners_logo_%28low_res%29.svg/1200px-Seattle_Mariners_logo_%28low_res%29.svg.png\\\"],\\\"mobileParameters\\\":{\\\"verticalAlign\\\":\\\"" + vAlign + "\\\",\\\"horizontalInset\\\":" + hInset + ",\\\"dismissAnimation\\\":\\\"" + dismissAnimation + "\\\",\\\"uiTakeover\\\":" + uiTakeover + ",\\\"horizontalAlign\\\":\\\"" + hAlign + "\\\",\\\"verticalInset\\\":" + vInset + ",\\\"displayAnimation\\\":\\\"" + displayAnimation + "\\\",\\\"width\\\":" + width + ",\\\"height\\\":" + height + ",\\\"backdropOpacity\\\":" + opacity + ",\\\"backdropColor\\\":\\\"#" + bdColor + "\\\",\\\"cornerRadius\\\":" + cornerRadius + ",\\\"gestures\\\":{}},\\\"html\\\":\\\"<html>\\\\n<head>\\\\n\\\\t<style>\\\\n\\\\t\\\\thtml,\\\\n\\\\t\\\\tbody {\\\\n\\\\t\\\\t\\\\tmargin: 0;\\\\n\\\\t\\\\t\\\\tpadding: 0;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\theight: 100%;\\\\n\\\\t\\\\t\\\\tfont-family: adobe-clean, \\\\\\\"Source Sans Pro\\\\\\\", -apple-system, BlinkMacSystemFont, \\\\\\\"Segoe UI\\\\\\\", Roboto, sans-serif;\\\\n\\\\t\\\\t}\\\\n\\\\n    h3 {\\\\n\\\\t\\\\t\\\\tmargin: .1rem auto;\\\\n\\\\t\\\\t}\\\\n\\\\t\\\\tp {\\\\n\\\\t\\\\t\\\\tmargin: 0;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.body {\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tbackground-color: #" + iamColor + ";\\\\n\\\\t\\\\t\\\\tborder-radius: 5px;\\\\n\\\\t\\\\t\\\\tcolor: #333333;\\\\n\\\\t\\\\t\\\\twidth: 100vw;\\\\n\\\\t\\\\t\\\\theight: 100vh;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\talign-items: center;\\\\n\\\\t\\\\t\\\\tbackground-size: 'cover';\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.content {\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\theight: 100%;\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tjustify-content: center;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tposition: relative;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\ta {\\\\n\\\\t\\\\t\\\\ttext-decoration: none;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.image {\\\\n\\\\t\\\\t  height: 1rem;\\\\n\\\\t\\\\t  flex-grow: 4;\\\\n\\\\t\\\\t  flex-shrink: 1;\\\\n\\\\t\\\\t  display: flex;\\\\n\\\\t\\\\t  justify-content: center;\\\\n\\\\t\\\\t  width: 90%;\\\\n      flex-direction: column;\\\\n      align-items: center;\\\\n\\\\t\\\\t}\\\\n    .image img {\\\\n      max-height: 100%;\\\\n      max-width: 100%;\\\\n    }\\\\n\\\\n\\\\t\\\\t.text {\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\tline-height: 20px;\\\\n\\\\t\\\\t\\\\tfont-size: 14px;\\\\n\\\\t\\\\t\\\\tcolor: #333333;\\\\n\\\\t\\\\t\\\\tpadding: 0 25px;\\\\n\\\\t\\\\t\\\\tline-height: 1.25rem;\\\\n\\\\t\\\\t\\\\tfont-size: 0.875rem;\\\\n\\\\t\\\\t}\\\\n\\\\t\\\\t.title {\\\\n\\\\t\\\\t\\\\tline-height: 1.3125rem;\\\\n\\\\t\\\\t\\\\tfont-size: 1.025rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.buttons {\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tfont-size: 1rem;\\\\n\\\\t\\\\t\\\\tline-height: 1.3rem;\\\\n\\\\t\\\\t\\\\ttext-decoration: none;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\tbox-sizing: border-box;\\\\n\\\\t\\\\t\\\\tpadding: .8rem;\\\\n\\\\t\\\\t\\\\tpadding-top: .4rem;\\\\n\\\\t\\\\t\\\\tgap: 0.3125rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.button {\\\\n\\\\t\\\\t\\\\tflex-grow: 1;\\\\n\\\\t\\\\t\\\\tbackground-color: #1473E6;\\\\n\\\\t\\\\t\\\\tcolor: #FFFFFF;\\\\n\\\\t\\\\t\\\\tborder-radius: .25rem;\\\\n\\\\t\\\\t\\\\tcursor: pointer;\\\\n\\\\t\\\\t\\\\tpadding: .3rem;\\\\n\\\\t\\\\t\\\\tgap: .5rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.btnClose {\\\\n\\\\t\\\\t\\\\tcolor: #000000;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.closeBtn {\\\\n\\\\t\\\\t\\\\talign-self: flex-end;\\\\n\\\\t\\\\t\\\\twidth: 1.8rem;\\\\n\\\\t\\\\t\\\\theight: 1.8rem;\\\\n\\\\t\\\\t\\\\tmargin-top: 1rem;\\\\n\\\\t\\\\t\\\\tmargin-right: .3rem;\\\\n\\\\t\\\\t}\\\\n\\\\t</style>\\\\n\\\\t<style type=\\\\\\\"text/css\\\\\\\" id=\\\\\\\"editor-styles\\\\\\\">\\\\n[data-uuid=\\\\\\\"e69adbda-8cfc-4c51-bcab-f8b2fd314d8f\\\\\\\"]  {\\\\n  flex-direction: row !important;\\\\n}\\\\n</style>\\\\n</head>\\\\n\\\\n<body>\\\\n\\\\t<div class=\\\\\\\"body\\\\\\\">\\\\n    <div class=\\\\\\\"closeBtn\\\\\\\" data-btn-style=\\\\\\\"plain\\\\\\\" data-uuid=\\\\\\\"fbc580c2-94c1-43c8-a46b-f66bb8ca8554\\\\\\\">\\\\n  <a class=\\\\\\\"btnClose\\\\\\\" href=\\\\\\\"adbinapp://cancel\\\\\\\">\\\\n    <svg xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\" height=\\\\\\\"18\\\\\\\" viewbox=\\\\\\\"0 0 18 18\\\\\\\" width=\\\\\\\"18\\\\\\\" class=\\\\\\\"close\\\\\\\">\\\\n  <rect id=\\\\\\\"Canvas\\\\\\\" fill=\\\\\\\"#ffffff\\\\\\\" opacity=\\\\\\\"" + opacity + "\\\\\\\" width=\\\\\\\"18\\\\\\\" height=\\\\\\\"18\\\\\\\" />\\\\n  <path fill=\\\\\\\"currentColor\\\\\\\" xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\" d=\\\\\\\"M13.2425,3.343,9,7.586,4.7575,3.343a.5.5,0,0,0-.707,0L3.343,4.05a.5.5,0,0,0,0,.707L7.586,9,3.343,13.2425a.5.5,0,0,0,0,.707l.707.7075a.5.5,0,0,0,.707,0L9,10.414l4.2425,4.243a.5.5,0,0,0,.707,0l.7075-.707a.5.5,0,0,0,0-.707L10.414,9l4.243-4.2425a.5.5,0,0,0,0-.707L13.95,3.343a.5.5,0,0,0-.70711-.00039Z\\\\\\\" />\\\\n</svg>\\\\n  </a>\\\\n</div><div class=\\\\\\\"image\\\\\\\" data-uuid=\\\\\\\"88c0afd9-1e3d-4d52-900f-fa08a56ad5e2\\\\\\\">\\\\n<img src=\\\\\\\"https://upload.wikimedia.org/wikipedia/en/thumb/6/6d/Seattle_Mariners_logo_%28low_res%29.svg/1200px-Seattle_Mariners_logo_%28low_res%29.svg.png\\\\\\\" alt=\\\\\\\"\\\\\\\">\\\\n</div><div class=\\\\\\\"text\\\\\\\" data-uuid=\\\\\\\"e7e49562-a365-4d38-be0e-d69cba829cb4\\\\\\\">\\\\n<h3>AJO In App test</h3>\\\\n<p>Locally generated message</p>\\\\n</div><div data-uuid=\\\\\\\"e69adbda-8cfc-4c51-bcab-f8b2fd314d8f\\\\\\\" class=\\\\\\\"buttons\\\\\\\">\\\\n  <a class=\\\\\\\"button\\\\\\\" data-uuid=\\\\\\\"864bd990-fc7f-4b1a-8d45-069c58981eb2\\\\\\\" href=\\\\\\\"adbinapp://dismiss?interaction=closed&js=(function() { return 'inline js return value'; })();\\\\\\\">Dismiss</a>\\\\n</div>\\\\n\\\\t</div>\\\\n\\\\n\\\\n</body></html>\\\",\\\"_xdm\\\":{\\\"mixins\\\":{\\\"_experience\\\":{\\\"customerJourneyManagement\\\":{\\\"messageExecution\\\":{\\\"messageExecutionID\\\":\\\"dummy_message_execution_id_7a237b33-2648-4903-82d1-efa1aac7c60d-all-visitors-everytime\\\",\\\"messageID\\\":\\\"fb31245e-3382-4b3a-a15a-dcf11f463020\\\",\\\"messagePublicationID\\\":\\\"aafe8df1-9c30-496d-ba0c-4b300f8cbabb\\\",\\\"ajoCampaignID\\\":\\\"7a237b33-2648-4903-82d1-efa1aac7c60d\\\",\\\"ajoCampaignVersionID\\\":\\\"38bd427b-f48e-4b3e-a4e1-03033b830d66\\\"},\\\"messageProfile\\\":{\\\"channel\\\":{\\\"_id\\\":\\\"https://ns.adobe.com/xdm/channels/inapp\\\"}}}}}}}}]}]}\",\n" +
                        "        \"id\": \"df6cf615-c12a-42df-bdf2-42d5c8bd9218\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"id\": \"d597cdf7-204b-4645-8115-5a0a06a910e0\"\n" +
                        "}"
        )
        val convertedPayload = PayloadFormatUtils.toObjectMap(payload)
        var listPayload = listOf(convertedPayload)
        val edgeResponseEvent = Event.Builder(
                "AEP Response Event Handle",
                "com.adobe.eventType.edge",
                "personalization:decisions"
        ).let {
            val eventData: HashMap<String, Any?> = hashMapOf(
                    "payload" to listPayload,
                    "requestId" to "D158979E-0506-4968-8031-17A6A8A87DA8",
                    "type" to "personalization:decisions",
                    "requestEventId" to "TESTING_ID"
            )
            it.setEventData(eventData)
            it.build()
        }
        MobileCore.dispatchEvent(edgeResponseEvent, null)
    }
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
            if(!showMessages) {
                println("message was suppressed: ${currentMessage?.id}")
                currentMessage?.track("message suppressed", MessagingEdgeEventType.TRIGGER)
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
                currentMessage?.track(content, MessagingEdgeEventType.INTERACT)
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