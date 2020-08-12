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

package com.adobe.marketing.mobile;

import static com.adobe.marketing.mobile.MessagingConstant.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;


public final class Messaging {

    private Messaging() {}

    public static String extensionVersion() {
        return EXTENSION_VERSION;
    }

    public static void registerExtension() {
        if(MobileCore.getCore() == null || MobileCore.getCore().eventHub == null) {
            Log.error(LOG_TAG, "Unable to register Messaging SDK since MobileCore is not initialized properly. For more details refer to https://aep-sdks.gitbook.io/docs/using-mobile-extensions/mobile-core");
        }

        MobileCore.registerExtension(MessagingInternal.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.debug("There was an error registering Places Monitoring Extension: %s", extensionError.getErrorName());
            }
        });
    }

    public class MessagingEventType {
        public static final String MESSAGING_TRACKING_EVENT_TYPE_APPLICATION_OPENED = "track.applicationOpened";
        public static final String MESSAGING_TRACKING_EVENT_TYPE_CUSTOM_ACTION = "track.customAction";
    }
}
