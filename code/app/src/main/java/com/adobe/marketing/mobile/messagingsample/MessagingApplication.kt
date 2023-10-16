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

package com.adobe.marketing.mobile.messagingsample.push

import android.app.Application
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.edge.identity.Identity

import com.google.firebase.messaging.FirebaseMessaging

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.configureWithAppID("staging/1b50a869c4a2/eabbaa346d96/launch-d66dc409a75e-development")
        // add your assurance session url
        Assurance.startSession("")

        val extensions = listOf(
            Edge.EXTENSION,
            Messaging.EXTENSION,
            Assurance.EXTENSION,
            Identity.EXTENSION);
        MobileCore.registerExtensions(extensions) {
            // use cjm stage
            val cjmStageConfig: HashMap<String, Any> = hashMapOf(
                "edge.environment" to "int"
            )
            MobileCore.updateConfiguration(cjmStageConfig)
        }

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