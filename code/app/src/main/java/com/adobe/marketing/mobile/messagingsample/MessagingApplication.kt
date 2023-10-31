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
import android.app.AlertDialog
import android.app.Application
import android.content.DialogInterface
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.concurrent.timer
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.identity.Identity
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Timer
import java.util.TimerTask


class MessagingApplication : Application() {
    private val ENVIRONMENT_FILE_ID = "3149c49c3910/afbd4e5232c5/launch-cf9ec24f55ed-development"
    private val ASSURANCE_SESSION_ID = "messagingdemo://foo?adb_validation_sessionid=248cbbdf-71cb-417e-9b1c-746fb4d59190"
    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        Assurance.startSession(ASSURANCE_SESSION_ID)
        MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)

        val extensions = listOf(
            Edge.EXTENSION,
            Messaging.EXTENSION,
            Assurance.EXTENSION,
            Identity.EXTENSION);
        MobileCore.registerExtensions(extensions) {
            // all the extensions are successfully registered
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            // Log and toast
            if (task.isSuccessful) {
                // Get new FCM registration token
                val token = task.result
                print("MessagingApplication Firebase token :: $token")
                MobileCore.setPushIdentifier(token)
            }
        }
    }
}