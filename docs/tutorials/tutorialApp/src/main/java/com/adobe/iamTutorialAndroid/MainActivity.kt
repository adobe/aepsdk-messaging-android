/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.iamTutorialAndroid

import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.services.MessagingDelegate
import com.adobe.marketing.mobile.services.ui.FullscreenMessage
import com.adobe.marketing.mobile.util.StringUtils
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : ComponentActivity() {
    private val customMessagingDelegate = CustomDelegate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileCore.setMessagingDelegate(customMessagingDelegate)

        // setup ui interaction listeners
        setupButtonClickListeners()
        setupSwitchListeners()
    }

    private fun setupButtonClickListeners() {

        btnTriggerFullscreenIAM.setOnClickListener {
            val trigger = editTextTrigger.text.toString()
            if (StringUtils.isNullOrEmpty(trigger)) {
                Toast.makeText(
                    this@MainActivity, "Empty trigger string provided.",
                    Toast.LENGTH_SHORT
                ).show()
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
            val message = if (isChecked) "IAM enabled" else "IAM disabled"
            Toast.makeText(
                    this@MainActivity, message,
                    Toast.LENGTH_SHORT
            ).show()
            customMessagingDelegate.showMessages = isChecked
        }
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
                currentMessage?.track("message suppressed", MessagingEdgeEventType.IN_APP_TRIGGER)
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