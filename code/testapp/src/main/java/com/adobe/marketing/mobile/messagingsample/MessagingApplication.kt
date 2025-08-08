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
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.LoggingMode

class MessagingApplication : Application() {
    private val ENVIRONMENT_FILE_ID = "3149c49c3910/4f6b2fbf2986/launch-7d78a5fd1de3-development"
    private val ASSURANCE_SESSION_ID = ""
    private val STAGING_APP_ID = "staging/1b50a869c4a2/bcd1a623883f/launch-e44d085fc760-development"
    private val STAGING = false

    // Use these IDs for Push Sync Optimization Bug Bash
    private val STAGING_APP_ID_WITH_PUSH_OPTIMIZATION_ENABLED = "staging/1b50a869c4a2/c8445c476ccf/launch-735af9a49790-staging"
    private val STAGING_APP_ID_WITH_PUSH_OPTIMIZATION_DISABLED = "staging/1b50a869c4a2/c8445c476ccf/launch-1e5d4da4ab99-development"
    private val STAGING_APP_ID_WITHOUT_PUSH_OPTIMIZATION_KEY = "staging/1b50a869c4a2/bcd1a623883f/launch-e44d085fc760-development"

    override fun onCreate() {
        super.onCreate()

        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.initialize(this, STAGING_APP_ID_WITHOUT_PUSH_OPTIMIZATION_KEY) {
            if (STAGING) {
                MobileCore.updateConfiguration(
                    hashMapOf("edge.environment" to "int") as Map<String, Any>)
            }
        }

        if (ASSURANCE_SESSION_ID.isEmpty()) {
            Assurance.startSession()
        } else {
            Assurance.startSession(ASSURANCE_SESSION_ID)
        }
    }
}