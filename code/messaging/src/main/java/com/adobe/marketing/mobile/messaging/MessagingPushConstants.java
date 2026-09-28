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
        // Silent, host-tracked interaction (input replies, unknown interaction types). Delivered to
        // the NotificationInteractionReceiver and tracked by actionId without launching the tracker
        // Activity. Not used by AJO Basic / Big Text today.
        static final String INTERACTION = "Notification Interaction";
        // Re-render is disabled until a template uses it; uncomment to enable.
        // // Gesture that rebuilds a push-template notification (for example a carousel arrow).
        // // Delivered to the NotificationInteractionReceiver, which rebuilds via the UI template
        // // plugin and re-posts. Not used by AJO Basic / Big Text today.
        // static final String RERENDER = "Notification Rerender";

        private NotificationAction() {}
    }

    // Plugin contract with the UI template add-on (see Core IUiTemplatePlugin / PushInteraction).
    // These values must match the add-on's constants exactly; PluginContractTest pins them.
    static final class PushInteractionType {
        static final String CONTENT_CLICK = "content_click";
        static final String BUTTON_CLICK = "button_click";
        static final String DISMISS = "dismiss";
        static final String INPUT_SUBMIT = "input_submit";
        static final String RERENDER = "rerender";

        private PushInteractionType() {}
    }

    // Messaging-internal intent extras and identity for push-template PendingIntents.
    static final class TemplateIntent {
        // Re-render is disabled until a template uses it; uncomment to enable.
        // // nested Bundle holding the original message data (incl. reserved keys) on re-render
        // // intents
        // static final String PUSH_PAYLOAD = "adb_rerender_payload";
        // // nested Bundle holding the UI template state on re-render intents
        // static final String TEMPLATE_STATE = "adb_rerender_state";
        // data-URI scheme giving each template PendingIntent a deterministic identity
        static final String IDENTITY_SCHEME = "adbpush";
        // fixed request code; identity comes from the data URI
        static final int REQUEST_CODE = 0;

        private TemplateIntent() {}
    }

    class Tracking {
        class Keys {
            static final String ACTION_ID = "actionId";
            static final String ACTION_URI = "actionUri";
            static final String MESSAGE_ID = "messageId";
            // Reserved message-data key: the id Messaging posts the notification with.
            static final String NOTIFICATION_ID = "notificationId";
            static final String ACTION_DISMISS =
                    MessagingConstants.Push.TrackingKeys.ACTION_DISMISS;

            private Keys() {}
        }

        private Tracking() {}
    }
}
