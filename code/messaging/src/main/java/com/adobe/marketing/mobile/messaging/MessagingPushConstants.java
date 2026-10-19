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
        // Silent, host-tracked interaction for unknown interaction types without an actionUri.
        // Delivered to the NotificationInteractionReceiver and tracked by actionId without
        // launching the tracker Activity. Not used by AJO Basic / Big Text today.
        static final String INTERACTION = "Notification Interaction";

        private NotificationAction() {}
    }

    // Plugin contract with the UI template add-on (see Core IUiTemplatePlugin / PushInteraction).
    // These values must match the add-on's constants exactly; PluginContractTest pins them.
    static final class PushInteractionType {
        static final String CONTENT_CLICK = "content_click";
        static final String BUTTON_CLICK = "button_click";
        static final String DISMISS = "dismiss";

        private PushInteractionType() {}
    }

    // Messaging-internal intent extras and identity for push-template PendingIntents.
    static final class TemplateIntent {
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

    /**
     * Event Hub-only error events dispatched when a push needs a Core plugin that is not
     * registered. They carry the push's {@code _xdm} unchanged and are never forwarded to Edge.
     */
    static final class PluginError {
        // Same event name as the Live Update add-on's render errors, so consumers see one stream.
        static final String EVENT_NAME_LIVE_UPDATE_ERROR = "Live Update Render Error";
        static final String EVENT_NAME_PUSH_TEMPLATE_ERROR = "Push Template Render Error";

        static final String PAYLOAD_KEY_XDM = "_xdm";

        static final String KEY_XDM = "xdm";
        static final String KEY_CATEGORY = "category";
        static final String KEY_SUBCATEGORY = "subcategory";

        static final String CATEGORY_LIVE_UPDATE_ERROR = "liveUpdateTracking.renderError";
        static final String CATEGORY_PUSH_TEMPLATE_ERROR = "pushTracking.renderError";

        static final String SUBCATEGORY_NO_PLUGIN = "no_plugin";

        private PluginError() {}
    }
}
