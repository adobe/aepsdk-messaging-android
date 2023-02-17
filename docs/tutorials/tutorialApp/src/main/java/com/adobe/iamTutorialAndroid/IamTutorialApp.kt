/*
  Copyright 2023 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.iamTutorialAndroid

import android.app.Application
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Messaging
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.edge.identity.Identity

class IamTutorialApp : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        MobileCore.registerExtensions(listOf(Messaging.EXTENSION, Identity.EXTENSION, Edge.EXTENSION, Assurance.EXTENSION)) {
            Assurance.startSession("ryan://?adb_validation_sessionid=475247d4-509c-4b84-841c-1b306d8aa56f")
            //MobileCore.configureWithAppID("3149c49c3910/ade9986818bd/launch-10fefb329b07-development")
            // NLD2
            MobileCore.configureWithAppID("bf7248f92b53/75100d4daf35/launch-4d3425399dab-development")
            MobileCore.lifecycleStart(null)
        }
    }
}