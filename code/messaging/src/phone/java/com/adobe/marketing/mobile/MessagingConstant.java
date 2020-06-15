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

public final class MessagingConstant {

    public static final String LOG_TAG = "Messaging";

    private MessagingConstant() {
    }

    public static final class EventDataKeys {

        public static final class Identity {
            public static final String PUSH_IDENTIFIER = "pushidentifier";

            private Identity() {
            }
        }
    }

    public static final class EventType {
        public static final String HUB = "com.adobe.eventType.hub";
        public static final String GENERIC_IDENTITY = "com.adobe.eventType.generic.identity";
        public static final String GENERIC_DATA = "com.adobe.eventType.generic.data";

        private EventType() {
        }
    }

    public static final class EventSource {
        public static final String SHARED_STATE = "com.adobe.eventSource.sharedState";
        public static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        public static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        public static final String OS = "com.adobe.eventSource.os";

        private EventSource() {
        }
    }

    public static final class SharedState {

        public static final String STATE_OWNER = "stateowner";

        private SharedState() {
        }

        public static final class Configuration {
            public static final String NAME = "com.adobe.module.configuration";
            public static final String PRIVACY_STATUS = "global.privacy";

            private Configuration() {
            }
        }

        public static final class Identity {
            public static final String NAME = "com.adobe.module.identity";

            private Identity() {
            }
        }
    }
}
