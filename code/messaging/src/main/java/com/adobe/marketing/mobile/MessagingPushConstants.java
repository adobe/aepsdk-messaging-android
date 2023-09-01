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

package com.adobe.marketing.mobile;

class MessagingPushConstants {
    static final String LOG_TAG = "Messaging";

    class PayloadKeys {
        static final String TITLE = "adb_title";
        static final String BODY = "adb_body";
        static final String SOUND = "adb_sound";
        static final String BADGE_NUMBER = "adb_n_count";
        static final String NOTIFICATION_PRIORITY = "adb_n_priority";
        static final String CHANNEL_ID = "adb_channel_id";
        static final String ICON = "adb_icon";
        static final String IMAGE_URL = "adb_image";
        static final String ACTION_TYPE = "adb_a_type";
        static final String ACTION_URI = "adb_uri";
        static final String ACTION_BUTTONS = "adb_act";

        private PayloadKeys() {
        }
    }
    class Tracking {
        class Keys {
            static final String ADOBE_XDM = "adobe_xdm";
            static final String APPLICATION_OPENED = "applicationOpened";
            static final String EVENT_TYPE = "eventType";
            static final String GOOGLE_MESSAGE_ID = "google.message_id";
            static final String MESSAGE_ID = "messageId";

            private Keys() {
            }
        }

        class Values {
            static final String PUSH_TRACKING_APPLICATION_OPENED = "pushTracking.applicationOpened";
            static final String PUSH_TRACKING_CUSTOM_ACTION = "pushTracking.customAction";
            private Values() {
            }
        }


        private Tracking() {
        }
    }

}
