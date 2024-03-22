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

package com.adobe.marketing.mobile.messaging;

class MessagingPushConstants {
    static final String LOG_TAG = "Messaging";

    class NotificationAction {
        static final String DISMISSED = "Notification Dismissed";
        static final String OPENED = "Notification Opened";
        static final String BUTTON_CLICKED = "Notification Button Clicked";

        private NotificationAction() {}
    }

    class Tracking {
        class Keys {
            static final String ACTION_ID = "actionId";
            static final String ACTION_URI = "actionUri";
            static final String MESSAGE_ID = "messageId";

            private Keys() {}
        }

        private Tracking() {}
    }
}
