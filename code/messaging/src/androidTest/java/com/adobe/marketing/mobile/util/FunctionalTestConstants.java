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

package com.adobe.marketing.mobile.util;

public class FunctionalTestConstants {
    public static final String LOG_TAG = "FunctionalTestsFramework";
    public static final String EDGE_DATA_STORAGE = "EdgeDataStorage";

    // Event type and sources used by Monitor Extension
    public static class EventType {

        public static final String MONITOR = "com.adobe.functional.eventType.monitor";

        private EventType() {}
    }

    public static class EventSource {

        public static final String SHARED_STATE_REQUEST =
                "com.adobe.eventSource.sharedStateRequest";
        public static final String SHARED_STATE_RESPONSE =
                "com.adobe.eventSource.sharedStateResponse";
        public static final String UNREGISTER = "com.adobe.eventSource.unregister";

        private EventSource() {}
    }

    public static class Defaults {

        public static final int WAIT_TIMEOUT_MS = 1000;
        public static final int WAIT_NETWORK_REQUEST_TIMEOUT_MS = 2000;
        public static final int WAIT_EVENT_TIMEOUT_MS = 2000;

        private Defaults() {}
    }

    public static class EventDataKey {
        // Used by Monitor Extension
        public static final String STATE_OWNER = "stateowner";

        private EventDataKey() {}
    }

    public static class SharedState {

        public static final String STATE_OWNER = "stateowner";
        public static final String MESSAGING = "com.adobe.messaging";
        public static final String IDENTITY = "com.adobe.module.identity";
        public static final String EDGE = "com.adobe.edge";
        public static final String LIFECYCLE = "com.adobe.module.lifecycle";

        private SharedState() {}
    }

    public static class NetworkKeys {
        public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
        public static final int DEFAULT_READ_TIMEOUT_SECONDS = 5;
        public static final String HEADER_KEY_ACCEPT = "accept";
        public static final String HEADER_KEY_CONTENT_TYPE = "Content-Type";
        public static final String HEADER_VALUE_APPLICATION_JSON = "application/json";

        private NetworkKeys() {}
    }

    private FunctionalTestConstants() {}
}
