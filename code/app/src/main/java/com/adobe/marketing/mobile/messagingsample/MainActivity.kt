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
import android.webkit.ValueCallback
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.ui.FullscreenMessage
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private val customMessagingDelegate = CustomDelegate()
    private lateinit var spinner: Spinner
    private var triggerKey = "key"
    private var triggerValue = "value"

    private enum class i18NSpinnerValues(val value: String) {
        KOREAN("Korean character test"),
        CYRILLIC("Cyrillic character test"),
        SURROGATE("Surrogate pair character test"),
        CHINESE("Chinese character test"),
        HIGH_ASCII("High ascii character test"),
        JAPANESE("Japanese 1+2 byte character test"),
        CHINESE_FOUR_BYTE("Chinese 4 byte character test"),
        FOUR_BYTE("I8N 4 byte character test"),
        HEBREW("Hebrew character test")
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
                0.0,
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
                0.0,
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
                20.0,
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
                75.0,
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
                90.0,
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
                0.0,
                false
            )
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ServiceProvider.getInstance().messageDelegate = customMessagingDelegate
        customMessagingDelegate.autoTrack = true

        // setup ui interaction listeners
        setupButtonClickListeners()
        setupSpinnerItemSelectedListener()
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

        // triggerKey and triggerValue are set from the spinner item selected
        btnTriggerFullscreenIAM.setOnClickListener {
            val iamTrigger = Event.Builder("test", "iamtest", "iamtest").let {
                val eventData: HashMap<String, Any?> = hashMapOf(triggerKey to triggerValue)
                it.setEventData(eventData)
                it.build()
            }
            MobileCore.dispatchEvent(iamTrigger, null)
        }

        btnHistoricalEvent1.setOnClickListener {
            val triggerEvent1 =
                Event.Builder("messaging event 1", "iamtest", "iamtest", arrayOf("firstEvent"))
                    .let {
                        val eventData: HashMap<String, Any?> = hashMapOf("firstEvent" to "true")
                        it.setEventData(eventData)
                        it.build()
                    }
            MobileCore.dispatchEvent(triggerEvent1, null)
        }

        btnHistoricalEvent2.setOnClickListener {
            val triggerEvent2 =
                Event.Builder("messaging event 2", "iamtest", "iamtest", arrayOf("secondEvent"))
                    .let {
                        val eventData: HashMap<String, Any?> = hashMapOf("secondEvent" to "true")
                        it.setEventData(eventData)
                        it.build()
                    }
            MobileCore.dispatchEvent(triggerEvent2, null)
        }

        btnHistoricalEvent3.setOnClickListener {
            val triggerEvent3 =
                Event.Builder("messaging event 3", "iamtest", "iamtest", arrayOf("thirdEvent"))
                    .let {
                        val eventData: HashMap<String, Any?> = hashMapOf("thirdEvent" to "true")
                        it.setEventData(eventData)
                        it.build()
                    }
            MobileCore.dispatchEvent(triggerEvent3, null)
        }

        btnCheckSequence.setOnClickListener {
            val checkSequenceEvent = Event.Builder("check sequence", "iamtest", "iamtest").let {
                val eventData: HashMap<String, Any?> = hashMapOf("checkSequence" to "true")
                it.setEventData(eventData)
                it.build()
            }
            MobileCore.dispatchEvent(checkSequenceEvent, null)
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

    private fun setupSpinnerItemSelectedListener() {
        spinner = findViewById(R.id.iamTypeSpinner)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.iam_types_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
        // spinner selection handling
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                // first check for i18n selections which are created then downloaded from AJO/Optimize
                handleI18nSpinnerValues(parent, pos)
                if (triggerValue.isNotEmpty()) { // we found an i18n iam, quick out
                    return
                }
                // otherwise check if a locally generated IAM was selected
                handleGeneratedIamValues(parent, pos)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
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

    private fun handleI18nSpinnerValues(parent: AdapterView<*>, pos: Int) {
        triggerKey = "ryan"
        triggerValue = ""
        when (parent.getItemAtPosition(pos)) {
            i18NSpinnerValues.KOREAN.value -> triggerValue = "korean"
            i18NSpinnerValues.CYRILLIC.value -> triggerValue = "cyrillic"
            i18NSpinnerValues.SURROGATE.value -> triggerValue = "surrogate"
            i18NSpinnerValues.CHINESE.value -> triggerValue = "sctest"
            i18NSpinnerValues.HIGH_ASCII.value -> triggerValue = "highascii"
            i18NSpinnerValues.JAPANESE.value -> triggerValue = "japanese"
            i18NSpinnerValues.CHINESE_FOUR_BYTE.value -> triggerValue = "chinese"
            i18NSpinnerValues.FOUR_BYTE.value -> triggerValue = "4byte"
            i18NSpinnerValues.HEBREW.value -> triggerValue = "hebrew"
            else -> print("Testing locally generated IAM")
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

    // local optimize event generation for testing
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
        // simulate edge response event containing offers
        val payload = JSONObject(
            "{\n" +
                    "  \"activity\": {\n" +
                    "    \"id\": \"906E3A095DC834230A495FD6@AdobeOrg\",\n" +
                    "    \"etag\": \"9\"\n" +
                    "  },\n" +
                    "  \"id\": \"57d8f990-734a-4d7b-a7e7-739b03753226\",\n" +
                    "  \"scope\": \"eyJ4ZG06bmFtZSI6ImNvbS5hZG9iZS5tYXJrZXRpbmcubW9iaWxlLm1lc3NhZ2luZ3NhbXBsZSJ9\",\n" +
                    "  \"items\": [\n" +
                    "    {\n" +
                    "      \"id\": \"xcore:personalized-offer:140f858533497db9\",\n" +
                    "      \"data\": {\n" +
                    "        \"characteristics\": {\n" +
                    "          \"inappmessageExecutionId\": \"dummy_message_execution_id_7a237b33-2648-4903-82d1-efa1aac7c60d-all-visitors-everytime\"\n" +
                    "        },\n" +
                    "        \"id\": \"xcore:personalized-offer:140f858533497db9\",\n" +
                    "        \"content\": \"{\\\"version\\\":1,\\\"rules\\\":[{\\\"condition\\\":{\\\"type\\\":\\\"group\\\",\\\"definition\\\":{\\\"conditions\\\":[{\\\"definition\\\":{\\\"key\\\":\\\"foo\\\",\\\"matcher\\\":\\\"eq\\\",\\\"values\\\":[\\\"bar\\\"]},\\\"type\\\":\\\"matcher\\\"}],\\\"logic\\\":\\\"and\\\"}},\\\"consequences\\\":[{\\\"id\\\":\\\"e56a8dbb-c5a4-4219-9563-0496e00b4083\\\",\\\"type\\\":\\\"cjmiam\\\",\\\"detail\\\":{\\\"mobileParameters\\\":{\\\"verticalAlign\\\":\\\"" + vAlign + "\\\",\\\"horizontalInset\\\":" + hInset + ",\\\"dismissAnimation\\\":\\\"" + dismissAnimation + "\\\",\\\"uiTakeover\\\":" + uiTakeover + ",\\\"horizontalAlign\\\":\\\"" + hAlign + "\\\",\\\"verticalInset\\\":" + vInset + ",\\\"displayAnimation\\\":\\\"" + displayAnimation + "\\\",\\\"width\\\":" + width + ",\\\"height\\\":" + height + ",\\\"backdropOpacity\\\":" + opacity + ",\\\"backdropColor\\\":\\\"#" + bdColor + "\\\",\\\"gestures\\\":{}},\\\"html\\\":\\\"<html>\\\\n<head>\\\\n\\\\t<style>\\\\n\\\\t\\\\thtml,\\\\n\\\\t\\\\tbody {\\\\n\\\\t\\\\t\\\\tmargin: 0;\\\\n\\\\t\\\\t\\\\tpadding: 0;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\theight: 100%;\\\\n\\\\t\\\\t\\\\tfont-family: adobe-clean, \\\\\\\"Source Sans Pro\\\\\\\", -apple-system, BlinkMacSystemFont, \\\\\\\"Segoe UI\\\\\\\", Roboto, sans-serif;\\\\n\\\\t\\\\t}\\\\n\\\\n    h3 {\\\\n\\\\t\\\\t\\\\tmargin: .1rem auto;\\\\n\\\\t\\\\t}\\\\n\\\\t\\\\tp {\\\\n\\\\t\\\\t\\\\tmargin: 0;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.body {\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tbackground-color: #" + iamColor + ";\\\\n\\\\t\\\\t\\\\tborder-radius: " + cornerRadius + "px;\\\\n\\\\t\\\\t\\\\tcolor: #333333;\\\\n\\\\t\\\\t\\\\twidth: 100vw;\\\\n\\\\t\\\\t\\\\theight: 100vh;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\talign-items: center;\\\\n\\\\t\\\\t\\\\tbackground-size: 'cover';\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.content {\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\theight: 100%;\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tjustify-content: center;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tposition: relative;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\ta {\\\\n\\\\t\\\\t\\\\ttext-decoration: none;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.image {\\\\n\\\\t\\\\t  height: 1rem;\\\\n\\\\t\\\\t  flex-grow: 4;\\\\n\\\\t\\\\t  flex-shrink: 1;\\\\n\\\\t\\\\t  display: flex;\\\\n\\\\t\\\\t  justify-content: center;\\\\n\\\\t\\\\t  width: 90%;\\\\n      flex-direction: column;\\\\n      align-items: center;\\\\n\\\\t\\\\t}\\\\n    .image img {\\\\n      max-height: 100%;\\\\n      max-width: 100%;\\\\n    }\\\\n\\\\n\\\\t\\\\t.text {\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\tline-height: 20px;\\\\n\\\\t\\\\t\\\\tfont-size: 14px;\\\\n\\\\t\\\\t\\\\tcolor: #333333;\\\\n\\\\t\\\\t\\\\tpadding: 0 25px;\\\\n\\\\t\\\\t\\\\tline-height: 1.25rem;\\\\n\\\\t\\\\t\\\\tfont-size: 0.875rem;\\\\n\\\\t\\\\t}\\\\n\\\\t\\\\t.title {\\\\n\\\\t\\\\t\\\\tline-height: 1.3125rem;\\\\n\\\\t\\\\t\\\\tfont-size: 1.025rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.buttons {\\\\n\\\\t\\\\t\\\\twidth: 100%;\\\\n\\\\t\\\\t\\\\tdisplay: flex;\\\\n\\\\t\\\\t\\\\tflex-direction: column;\\\\n\\\\t\\\\t\\\\tfont-size: 1rem;\\\\n\\\\t\\\\t\\\\tline-height: 1.3rem;\\\\n\\\\t\\\\t\\\\ttext-decoration: none;\\\\n\\\\t\\\\t\\\\ttext-align: center;\\\\n\\\\t\\\\t\\\\tbox-sizing: border-box;\\\\n\\\\t\\\\t\\\\tpadding: .8rem;\\\\n\\\\t\\\\t\\\\tpadding-top: .4rem;\\\\n\\\\t\\\\t\\\\tgap: 0.3125rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.button {\\\\n\\\\t\\\\t\\\\tflex-grow: 1;\\\\n\\\\t\\\\t\\\\tbackground-color: #1473E6;\\\\n\\\\t\\\\t\\\\tcolor: #FFFFFF;\\\\n\\\\t\\\\t\\\\tborder-radius: " + cornerRadius + "rem;\\\\n\\\\t\\\\t\\\\tcursor: pointer;\\\\n\\\\t\\\\t\\\\tpadding: .3rem;\\\\n\\\\t\\\\t\\\\tgap: .5rem;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.btnClose {\\\\n\\\\t\\\\t\\\\tcolor: #000000;\\\\n\\\\t\\\\t}\\\\n\\\\n\\\\t\\\\t.closeBtn {\\\\n\\\\t\\\\t\\\\talign-self: flex-end;\\\\n\\\\t\\\\t\\\\twidth: 1.8rem;\\\\n\\\\t\\\\t\\\\theight: 1.8rem;\\\\n\\\\t\\\\t\\\\tmargin-top: 1rem;\\\\n\\\\t\\\\t\\\\tmargin-right: .3rem;\\\\n\\\\t\\\\t}\\\\n\\\\t</style>\\\\n\\\\t<style type=\\\\\\\"text/css\\\\\\\" id=\\\\\\\"editor-styles\\\\\\\">\\\\n[data-uuid=\\\\\\\"e69adbda-8cfc-4c51-bcab-f8b2fd314d8f\\\\\\\"]  {\\\\n  flex-direction: row !important;\\\\n}\\\\n</style>\\\\n</head>\\\\n\\\\n<body>\\\\n\\\\t<div class=\\\\\\\"body\\\\\\\">\\\\n    <div class=\\\\\\\"closeBtn\\\\\\\" data-btn-style=\\\\\\\"plain\\\\\\\" data-uuid=\\\\\\\"fbc580c2-94c1-43c8-a46b-f66bb8ca8554\\\\\\\">\\\\n  <a class=\\\\\\\"btnClose\\\\\\\" href=\\\\\\\"adbinapp://cancel\\\\\\\">\\\\n    <svg xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\" height=\\\\\\\"18\\\\\\\" viewbox=\\\\\\\"0 0 18 18\\\\\\\" width=\\\\\\\"18\\\\\\\" class=\\\\\\\"close\\\\\\\">\\\\n  <rect id=\\\\\\\"Canvas\\\\\\\" fill=\\\\\\\"#ffffff\\\\\\\" opacity=\\\\\\\"" + opacity + "\\\\\\\" width=\\\\\\\"18\\\\\\\" height=\\\\\\\"18\\\\\\\" />\\\\n  <path fill=\\\\\\\"currentColor\\\\\\\" xmlns=\\\\\\\"http://www.w3.org/2000/svg\\\\\\\" d=\\\\\\\"M13.2425,3.343,9,7.586,4.7575,3.343a.5.5,0,0,0-.707,0L3.343,4.05a.5.5,0,0,0,0,.707L7.586,9,3.343,13.2425a.5.5,0,0,0,0,.707l.707.7075a.5.5,0,0,0,.707,0L9,10.414l4.2425,4.243a.5.5,0,0,0,.707,0l.7075-.707a.5.5,0,0,0,0-.707L10.414,9l4.243-4.2425a.5.5,0,0,0,0-.707L13.95,3.343a.5.5,0,0,0-.70711-.00039Z\\\\\\\" />\\\\n</svg>\\\\n  </a>\\\\n</div><div class=\\\\\\\"image\\\\\\\" data-uuid=\\\\\\\"88c0afd9-1e3d-4d52-900f-fa08a56ad5e2\\\\\\\">\\\\n<img src=\\\\\\\"https://i.ibb.co/0X8R3TG/Messages-24.png\\\\\\\" alt=\\\\\\\"\\\\\\\">\\\\n</div><div class=\\\\\\\"text\\\\\\\" data-uuid=\\\\\\\"e7e49562-a365-4d38-be0e-d69cba829cb4\\\\\\\">\\\\n<h3>AJO In App test</h3>\\\\n<p>Locally generated message</p>\\\\n</div><div data-uuid=\\\\\\\"e69adbda-8cfc-4c51-bcab-f8b2fd314d8f\\\\\\\" class=\\\\\\\"buttons\\\\\\\">\\\\n  <a class=\\\\\\\"button\\\\\\\" data-uuid=\\\\\\\"864bd990-fc7f-4b1a-8d45-069c58981eb2\\\\\\\" href=\\\\\\\"adbinapp://dismiss\\\\\\\">Dismiss</a>\\\\n</div>\\\\n\\\\t</div>\\\\n\\\\n\\\\n</body></html>\\\",\\\"_xdm\\\":{\\\"mixins\\\":{\\\"_experience\\\":{\\\"customerJourneyManagement\\\":{\\\"messageExecution\\\":{\\\"messageExecutionID\\\":\\\"dummy_message_execution_id_7a237b33-2648-4903-82d1-efa1aac7c60d-all-visitors-everytime\\\",\\\"messageID\\\":\\\"fb31245e-3382-4b3a-a15a-dcf11f463020\\\",\\\"messagePublicationID\\\":\\\"aafe8df1-9c30-496d-ba0c-4b300f8cbabb\\\",\\\"ajoCampaignID\\\":\\\"7a237b33-2648-4903-82d1-efa1aac7c60d\\\",\\\"ajoCampaignVersionID\\\":\\\"38bd427b-f48e-4b3e-a4e1-03033b830d66\\\"},\\\"messageProfile\\\":{\\\"channel\\\":{\\\"_id\\\":\\\"https://ns.adobe.com/xdm/channels/inapp\\\"}}}}}}}}]}]}\",\n" +
                    "        \"format\": \"application/json\"\n" +
                    "      },\n" +
                    "      \"etag\": \"1\",\n" +
                    "      \"schema\": \"https://ns.adobe.com/experience/offer-management/content-component-json\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"placement\": {\n" +
                    "    \"id\": \"com.adobe.marketing.mobile.messagingsample\",\n" +
                    "    \"etag\": \"1\"\n" +
                    "  }\n" +
                    "}"
        )
        val convertedPayload = PayloadFormatUtils.toObjectMap(payload)
        var listPayload: ArrayList<Map<String, Any>> = ArrayList(1)
        listPayload.add(convertedPayload as Map<String, Any>)
        val edgeResponseEvent = Event.Builder(
            "AEP Response Event Handle",
            "com.adobe.eventType.edge",
            "personalization:decisions"
        ).let {
            val eventData: HashMap<String, Any?> = hashMapOf(
                "payload" to listPayload,
                "requestId" to "D158979E-0506-4968-8031-17A6A8A87DA8",
                "type" to "personalization:decisions",
                "requestEventId" to "2E964037-E319-4D14-98B8-0682374E547B"
            )
            it.setEventData(eventData)
            it.build()
        }
        MobileCore.dispatchEvent(edgeResponseEvent, null)
    }
}

class CustomDelegate : MessageDelegate() {
    private var currentMessage: FullscreenMessage? = null
    var showMessages = true

    override fun shouldShowMessage(fullscreenMessage: FullscreenMessage?): Boolean {
        fullscreenMessage?.also {
            this.currentMessage = fullscreenMessage
            val message = (currentMessage?.parent) as? Message

            // in-line handling of javascript calls
            message?.handleJavascriptMessage("magic") { content ->
                print("magical handling of our content from js! content is: $content")
                message?.track(content, MessagingEdgeEventType.IN_APP_INTERACT)
            }

            // if we're not showing the message now, we can save it for later
            showMessages.let {
                println("message was suppressed: ${message?.messageId}")
                message?.track("message suppressed", MessagingEdgeEventType.IN_APP_TRIGGER)
            }
        }
        return showMessages
    }

    override fun onShow(fullscreenMessage: FullscreenMessage?) {
        this.currentMessage = fullscreenMessage
    }

    override fun onDismiss(fullscreenMessage: FullscreenMessage?) {
        this.currentMessage = fullscreenMessage
    }

    fun getLastTriggeredMessage(): FullscreenMessage? {
        return currentMessage
    }
}