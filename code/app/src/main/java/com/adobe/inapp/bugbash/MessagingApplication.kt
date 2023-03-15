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

package com.adobe.inapp.bugbash

import android.app.Application
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.edge.identity.Identity
import com.google.firebase.messaging.FirebaseMessaging


class MessagingApplication : MultiDexApplication() {
    private val ENVIRONMENT_FILE_ID = "3149c49c3910/a93ff37dae6c/launch-97cd3b98c5bc-development"
    private val ASSURANCE_SESSION_LINK = "YOUR-SESSION-LINK"

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.registerExtensions(
            listOf(Edge.EXTENSION, Identity.EXTENSION, Messaging.EXTENSION, Assurance.EXTENSION)
        ) { o: Any? ->
            Log.d(
                "MainApp",
                "Adobe Experience Platform Mobile SDK was initialized."
            )
            MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)
            MobileCore.lifecycleStart(null)
        }

        Assurance.startSession(ASSURANCE_SESSION_LINK)


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            // Log and toast
            if (task.isSuccessful) {
                // Get new FCM registration token
                val token = task.result
                print("MessagingApplication Firebase token :: $token")
                // Syncing the push token with experience platform
                MobileCore.setPushIdentifier(token)
            }
        }
    }
}