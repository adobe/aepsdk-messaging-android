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
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.Lifecycle
import com.adobe.marketing.mobile.edge.identity.Identity

class MessagingApplication : Application() {
    private val ENVIRONMENT_FILE_ID = "staging/1b50a869c4a2/8d83ca76a48a/launch-98809790b968-development"

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        val extensions = listOf(Messaging.EXTENSION, Identity.EXTENSION, Lifecycle.EXTENSION, Edge.EXTENSION, Assurance.EXTENSION)
        MobileCore.registerExtensions(extensions) {
            MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)
            MobileCore.updateConfiguration(
                hashMapOf("edge.environment" to "int") as Map<String, Any>)
            MobileCore.lifecycleStart(null)

            val configMap = mapOf(
                "messaging.optimizePushSync" to false
            )
            MobileCore.updateConfiguration(configMap)
        }
    }
}