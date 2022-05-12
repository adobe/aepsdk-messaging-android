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

import android.app.Application
import android.widget.Toast
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.edge.identity.Identity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class MessagingApplication : Application() {
    val appId = "{Your App Id}"

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Identity.registerExtension()
        Edge.registerExtension()
        Assurance.registerExtension()
        Lifecycle.registerExtension()
        Signal.registerExtension()
        UserProfile.registerExtension()
        Messaging.registerExtension()

        MobileCore.start {
            MobileCore.configureWithAppID(appId)
            MobileCore.lifecycleStart(null)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get new FCM registration token
                val token = task.result
                print("MessagingApplication Firebase token :: ${token}")
                // Syncing the push token with experience platform
                MobileCore.setPushIdentifier(token)
                // Log and toast
                val msg = "FCM token received: ${token}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
        })
    }
}