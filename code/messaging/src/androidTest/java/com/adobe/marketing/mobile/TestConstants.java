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

package com.adobe.marketing.mobile;

/**
 * Class to maintain test constants.
 */
public class TestConstants {

    public class EventType {
        static final String MONITOR = "com.adobe.functional.eventType.monitor";

        private EventType() {
        }
    }

    public class EventSource {
        // Used by Monitor Extension
        static final String XDM_SHARED_STATE_REQUEST = "com.adobe.eventSource.xdmsharedStateRequest";
        static final String XDM_SHARED_STATE_RESPONSE = "com.adobe.eventSource.xdmsharedStateResponse";
        static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
        static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
        static final String UNREGISTER = "com.adobe.eventSource.unregister";

        private EventSource() {
        }
    }

    public class EventDataKey {
        static final String STATE_OWNER = "stateowner";

        private EventDataKey() {
        }
    }

    public final class SharedStateName {
        public static final String EVENT_HUB = "com.adobe.module.eventhub";
        public static final String EDGE_IDENTITY = "com.adobe.module.identity";

        private SharedStateName() {
        }
    }
}
