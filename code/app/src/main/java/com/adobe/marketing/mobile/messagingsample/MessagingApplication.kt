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
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.consent.Consent;
import com.google.firebase.iid.FirebaseInstanceId

class MessagingApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        Messaging.registerExtension()
        Identity.registerExtension()
        Edge.registerExtension()
        Assurance.registerExtension()
        Consent.registerExtension()
        Assurance.startSession("YOUR-SESSION-ID")

        MobileCore.start {
            // Necessary property id which has the edge configuration id needed by aep sdk
            MobileCore.configureWithAppID("3149c49c3910/cf7779260cdd/launch-be72758aa82a-development")
            MobileCore.lifecycleStart(null)
            // update config to use cjmstage for int integration
            val cjmStageConfig: HashMap<String, Any> = hashMapOf(
                "edge.environment" to "int",
                //"experienceCloud.org" to "745F37C35E4B776E0A49421B@AdobeOrg",
                "edge.configId" to "15525167-fd4e-4511-b9e0-02119485784f"
                //"edge.configId" to "1f0eb783-2464-4bdd-951d-7f8afbf527f5:dev"
                //"messaging.eventDataset" to "610ae80b3cbbc718dab06208"
            )
            MobileCore.updateConfiguration(cjmStageConfig)
        }


        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result?.token ?: ""
                print("MessagingApplication Firebase token :: $token")
                // Syncing the push token with experience platform
                MobileCore.setPushIdentifier(token)
            }
        }
    }
}