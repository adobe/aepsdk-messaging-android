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
            MobileCore.configureWithAppID("3805cb8645dd/b8dec0fe156d/launch-7dfbe727ca00-development")
            MobileCore.lifecycleStart(null);

            MobileCore.updateConfiguration(mutableMapOf("messaging.dccs" to "https://dcs-stg.adobedc.net/collection/9b994747bbced2f43847d61e043d8a5c8a39e642dfdec1ddf1bb6d4d784f9cd9"
                    , "messaging.eventDataset" to "5f8623492312f418dcc6b3d9") as Map<String, Any>?)
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