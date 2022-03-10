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
    static final String CACHE_NAME = "com.adobe.messaging.test.cache";
    static final String MESSAGES_CACHE_SUBDIRECTORY = "messages";
    static final String IMAGES_CACHE_SUBDIRECTORY = "images";
    static final String EXTENSION_NAME = "com.adobe.messaging";

    enum MessagingEdgeEventType {
        IN_APP_DISMISS, IN_APP_INTERACT, IN_APP_TRIGGER, IN_APP_DISPLAY, PUSH_APPLICATION_OPENED, PUSH_CUSTOM_ACTION
    }

    public class EventType {
        static final String MONITOR = "com.adobe.functional.eventType.monitor";
        static final String MESSAGING = "com.adobe.eventType.messaging";
        static final String EDGE = "com.adobe.eventType.edge";
        static final String OPTIMIZE = "com.adobe.eventType.optimize";

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
        static final String PERSONALIZATION_DECISIONS = "personalization:decisions";

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

    static final class TrackingKeys {
        static final String _XDM = "_xdm";
        static final String XDM = "xdm";
        static final String META = "meta";
        static final String CJM = "cjm";
        static final String MIXINS = "mixins";
        static final String EXPERIENCE = "_experience";
        static final String CUSTOMER_JOURNEY_MANAGEMENT = "customerJourneyManagement";
        static final String MESSAGE_EXECUTION = "messageExecution";
        static final String MESSAGE_EXECUTION_ID = "messageExecutionID";
        static final String MESSAGE_PROFILE_JSON = "{\n" +
                "   \"messageProfile\":{\n" +
                "      \"channel\":{\n" +
                "         \"_id\":\"https://ns.adobe.com/xdm/channels/push\"\n" +
                "      }\n" +
                "   },\n" +
                "   \"pushChannelContext\":{\n" +
                "      \"platform\":\"fcm\"\n" +
                "   }\n" +
                "}";
        static final String APPLICATION = "application";
        static final String LAUNCHES = "launches";
        static final String LAUNCHES_VALUE = "value";
        static final String DATASET_ID = "datasetId";
        static final String COLLECT = "collect";
    }

    static final class EventDataKeys {
        static final String STATE_OWNER = "stateowner";

        static final class Identity {
            static final String PUSH_IDENTIFIER = "pushidentifier";

            private Identity() {
            }
        }

        static final class Messaging {
            static final String TRACK_INFO_KEY_EVENT_TYPE = "eventType";
            static final String TRACK_INFO_KEY_MESSAGE_ID = "messageId";
            static final String TRACK_INFO_KEY_MESSAGE_EXECUTION_ID = "messageExecutionID";
            static final String TRACK_INFO_KEY_APPLICATION_OPENED = "applicationOpened";
            static final String TRACK_INFO_KEY_ACTION_ID = "actionId";

            // Google messaging id key
            static final String TRACK_INFO_KEY_GOOGLE_MESSAGE_ID = "google.message_id";

            static final String TRACK_INFO_KEY_ADOBE_XDM = "adobe_xdm";

            static final String REFRESH_MESSAGES = "refreshmessages";

            private Messaging() {
            }

            static final class XDMDataKeys {
                static final String XDM_DATA_ACTION_ID = "actionID";
                static final String XDM_DATA_CUSTOM_ACTION = "customAction";
                static final String XDM_DATA_PUSH_PROVIDER_MESSAGE_ID = "pushProviderMessageID";
                static final String XDM_DATA_PUSH_PROVIDER = "pushProvider";
                static final String XDM_DATA_EVENT_TYPE = "eventType";
                static final String XDM_DATA_PUSH_NOTIFICATION_TRACKING_MIXIN_NAME = "pushNotificationTracking";
                static final String XDM_DATA_IN_APP_NOTIFICATION_TRACKING_MIXIN_NAME = "inappMessageTracking";
                static final String ACTION = "action";

                private XDMDataKeys() {
                }
            }

            static final class PushNotificationDetailsDataKeys {
                static final String DATA = "data";
                static final String PUSH_NOTIFICATION_DETAILS = "pushNotificationDetails";
                static final String IDENTITY = "identity";
                static final String NAMESPACE = "namespace";
                static final String CODE = "code";
                static final String ID = "id";
                static final String APP_ID = "appID";
                static final String TOKEN = "token";
                static final String PLATFORM = "platform";
                static final String DENY_LISTED = "denylisted";

                private PushNotificationDetailsDataKeys() {
                }
            }

            static final class IAMDetailsDataKeys {
                private IAMDetailsDataKeys() {
                }

                static final class EventType {
                    static final String DISMISS = "inapp.dismiss";
                    static final String INTERACT = "inapp.interact";
                    static final String TRIGGER = "inapp.trigger";
                    static final String DISPLAY = "inapp.display";
                }
            }
        }
    }

    static final class SharedState {

        private SharedState() {
        }

        static final class Configuration {
            static final String EXTENSION_NAME = "com.adobe.module.configuration";

            // Messaging
            static final String EXPERIENCE_EVENT_DATASET_ID = "messaging.eventDataset";

            private Configuration() {/* no-op */}
        }

        static final class EdgeIdentity {
            static final String EXTENSION_NAME = "com.adobe.edge.identity";
            static final String IDENTITY_MAP = "identityMap";
            static final String ECID = "ECID";
            static final String ID = "id";

            private EdgeIdentity() {/* no-op */}
        }

        static final class Messaging {
            static final String PUSH_IDENTIFIER = "pushidentifier";
        }
    }
}
