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

import com.adobe.marketing.mobile.MobileCore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.adobe.marketing.mobile.messaging.MessagingService

class NotificationService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        print("MessagingApplication Firebase token :: $token")
        MobileCore.setPushIdentifier(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (MessagingService.handleRemoteMessage(this, message)) {
            // Messaging extension has handled the notification
        } else {
            // Handle notification from other sources
        }
    }

}