/*
 Copyright 2020 Adobe. All rights reserved.
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
import com.adobe.marketing.mobile.*
import com.google.firebase.iid.FirebaseInstanceId

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Messaging.registerExtension()
        Identity.registerExtension()
        UserProfile.registerExtension()
        Lifecycle.registerExtension()
        Signal.registerExtension()
        Edge.registerExtension();

        MobileCore.start {
            // Necessary property id for MessagingSDKTest which has the edge configuration id needed by aep sdk
            MobileCore.configureWithAppID("<appId>")
            MobileCore.lifecycleStart(null);

            MobileCore.updateConfiguration(mutableMapOf("messaging.eventDataset" to "<eventDatasetId>") as Map<String, Any>?)
        }

        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener{ task ->
            if(task.isSuccessful) {
                val token = task.result?.token ?: ""
                print("MessagingApplication Firebase token :: $token")
                MobileCore.setPushIdentifier(token)
            }
        }
    }
}