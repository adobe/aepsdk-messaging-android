/*
 Copyright 2026 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.messagingsample

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.adobe.marketing.mobile.messaging.MessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.Executors

/**
 * Debug helper to simulate AJO push notifications without FCM.
 * Uses the same path as production: [MessagingService.handleRemoteMessage].
 */
object PushSimulationHelper {
    private const val LOG_TAG = "PushSimulationHelper"
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Mac LAN IP serving `/tmp/push-test-assets/beard.jpg` — true non-loopback cleartext HTTP.
     * Update when your network changes: `ipconfig getifaddr en0`.
     *
     * Server: `cd /tmp/push-test-assets && python3 -m http.server 8080 --bind 0.0.0.0`
     *
     * **Variant B:** `android:usesCleartextTraffic="true"` in AndroidManifest → image loads.
     * **Variant A:** set `usesCleartextTraffic="false"` (or remove) → text-only on API 28+.
     */
    const val HTTP_LAN_HOST = "10.41.113.178"

    const val HTTP_EMULATOR_IMAGE_URL = "http://$HTTP_LAN_HOST:8080/beard.jpg"

    /** Loopback only — API 37 exempts localhost; not used for Variant A/B cleartext tests. */
    const val HTTP_LOOPBACK_IMAGE_URL = "http://127.0.0.1:8080/beard.jpg"

    const val HTTPS_CONTROL_IMAGE_URL = "https://picsum.photos/640/480"

    /**
     * Runs on a background thread so [MessagingService.handleRemoteMessage] can download
     * [adb_image] the same way FCM does (network I/O must not run on the main thread).
     */
    fun simulateAjoPush(context: Context, imageUrl: String, title: String, body: String) {
        val appContext = context.applicationContext
        worker.execute {
            val message = RemoteMessage.Builder("local-test-sender")
                .setMessageId("local-test-${System.currentTimeMillis()}")
                .addData("_xdm", "{}")
                .addData("adb_title", title)
                .addData("adb_body", body)
                .addData("adb_image", imageUrl)
                .build()

            Log.d(LOG_TAG, "Simulating AJO push — adb_image=$imageUrl")
            val handled = MessagingService.handleRemoteMessage(appContext, message)
            val result =
                if (handled) "Push displayed — swipe down from top to open notification shade"
                else "Not handled (not AJO payload?)"
            mainHandler.post { Toast.makeText(appContext, result, Toast.LENGTH_LONG).show() }
        }
    }
}
