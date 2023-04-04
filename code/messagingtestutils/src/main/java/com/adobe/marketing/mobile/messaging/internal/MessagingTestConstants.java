/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging.internal;

/**
 * Class to maintain test constants.
 */
public class MessagingTestConstants {
    public static final String EXTENSION_VERSION = MessagingConstants.EXTENSION_VERSION;
    static final String CACHE_NAME = "com.adobe.messaging.test.cache";
    static final String PROPOSITIONS_CACHE_SUBDIRECTORY = "propositions";
    static final String IMAGES_CACHE_SUBDIRECTORY = "images";
    static final String CACHE_BASE_DIR = "messaging";
    static final String EXTENSION_NAME = "com.adobe.messaging";

    public static final class EventType {
        public static final String MONITOR = "com.adobe.functional.eventType.monitor";
        public static final String MESSAGING = "com.adobe.eventType.messaging";
        public static final String EDGE = "com.adobe.eventType.edge";
        public static final String OPTIMIZE = "com.adobe.eventType.optimize";

        private EventType() {
        }
    }

    public final class EventName {
        public static final String MESSAGE_INTERACTION_EVENT = "Messaging interaction event";
        public static final String PUSH_NOTIFICATION_INTERACTION_EVENT = "Push notification interaction event";
        public static final String PUSH_TRACKING_EDGE_EVENT = "Push tracking edge event";
        public static final String PUSH_PROFILE_EDGE_EVENT = "Push notification profile edge event";
        public static final String REFRESH_MESSAGES_EVENT = "Refresh in-app messages";
        public static final String UPDATE_MESSAGE_FEEDS = "Update message feeds";

        private EventName() {
        }
    }

    public static final class EventSource {
        // Used by Monitor Extension
        public static final String XDM_SHARED_STATE_REQUEST = "com.adobe.eventSource.xdmsharedStateRequest";
        public static final String XDM_SHARED_STATE_RESPONSE = "com.adobe.eventSource.xdmsharedStateResponse";
        public static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
        public static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
        public static final String UNREGISTER = "com.adobe.eventSource.unregister";
        public static final String PERSONALIZATION_DECISIONS = "personalization:decisions";
        public static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";

        private EventSource() {
        }
    }

    public static final class EventDataKey {
        public static final String STATE_OWNER = "stateowner";

        private EventDataKey() {
        }
    }

    public static final class SharedStateName {
        public static final String EVENT_HUB = "com.adobe.module.eventhub";
        public static final String EDGE_IDENTITY = "com.adobe.module.identity";

        private SharedStateName() {
        }
    }

    public static final class TrackingKeys {
        public static final String _XDM = "_xdm";
        public static final String XDM = "xdm";
        public static final String META = "meta";
        public static final String CJM = "cjm";
        public static final String MIXINS = "mixins";
        public static final String EXPERIENCE = "_experience";
        public static final String CUSTOMER_JOURNEY_MANAGEMENT = "customerJourneyManagement";
        public static final String MESSAGE_EXECUTION = "messageExecution";
        public static final String MESSAGE_EXECUTION_ID = "messageExecutionID";
        public static final String MESSAGE_PROFILE_JSON = "{\n" +
                "   \"messageProfile\":{\n" +
                "      \"channel\":{\n" +
                "         \"_id\":\"https://ns.adobe.com/xdm/channels/push\"\n" +
                "      }\n" +
                "   },\n" +
                "   \"pushChannelContext\":{\n" +
                "      \"platform\":\"fcm\"\n" +
                "   }\n" +
                "}";
        public static final String APPLICATION = "application";
        public static final String LAUNCHES = "launches";
        public static final String LAUNCHES_VALUE = "value";
        public static final String DATASET_ID = "datasetId";
        public static final String COLLECT = "collect";
    }

    public static final class EventDataKeys {
        public static final String STATE_OWNER = "stateowner";

        public static final class Identity {
            public static final String PUSH_IDENTIFIER = "pushidentifier";

            private Identity() {
            }
        }

        public static final class Messaging {
            public static final String TRACK_INFO_KEY_EVENT_TYPE = "eventType";
            public static final String TRACK_INFO_KEY_MESSAGE_ID = "messageId";
            public static final String TRACK_INFO_KEY_MESSAGE_EXECUTION_ID = "messageExecutionID";
            public static final String TRACK_INFO_KEY_APPLICATION_OPENED = "applicationOpened";
            public static final String TRACK_INFO_KEY_ACTION_ID = "actionId";
            // Google messaging id key
            public static final String TRACK_INFO_KEY_GOOGLE_MESSAGE_ID = "google.message_id";

            public static final String TRACK_INFO_KEY_ADOBE_XDM = "adobe_xdm";
            public static final String REFRESH_MESSAGES = "refreshmessages";
            public static final String UPDATE_FEEDS = "updatefeeds";
            public static final String SURFACES = "surfaces";

            private Messaging() {
            }

            public static final class XDMDataKeys {
                public static final String XDM_DATA_ACTION_ID = "actionID";
                public static final String XDM_DATA_CUSTOM_ACTION = "customAction";
                public static final String XDM_DATA_PUSH_PROVIDER_MESSAGE_ID = "pushProviderMessageID";
                public static final String XDM_DATA_PUSH_PROVIDER = "pushProvider";
                public static final String XDM_DATA_EVENT_TYPE = "eventType";
                public static final String XDM_DATA_PUSH_NOTIFICATION_TRACKING_MIXIN_NAME = "pushNotificationTracking";
                public static final String XDM_DATA_IN_APP_NOTIFICATION_TRACKING_MIXIN_NAME = "inappMessageTracking";
                public static final String ACTION = "action";

                private XDMDataKeys() {
                }
            }

            public static final class PushNotificationDetailsDataKeys {
                public static final String DATA = "data";
                public static final String PUSH_NOTIFICATION_DETAILS = "pushNotificationDetails";
                public static final String IDENTITY = "identity";
                public static final String NAMESPACE = "namespace";
                public static final String CODE = "code";
                public static final String ID = "id";
                public static final String APP_ID = "appID";
                public static final String TOKEN = "token";
                public static final String PLATFORM = "platform";
                public static final String DENY_LISTED = "denylisted";

                public static final class EventType {
                    static final String OPENED = "pushTracking.applicationOpened";
                    static final String CUSTOM_ACTION = "pushTracking.customAction";
                }

                private PushNotificationDetailsDataKeys() {
                }
            }

            final class IAMDetailsDataKeys {
                static final String IN_APP_MIXIN_NAME = "inappMessageTracking";
                static final String SURFACE_BASE = "mobileapp://";

                private IAMDetailsDataKeys() {
                }

                final class EventType {
                    static final String DISMISS = "decisioning.propositionDismiss";
                    static final String INTERACT = "decisioning.propositionInteract";
                    static final String TRIGGER = "decisioning.propositionTrigger";
                    static final String DISPLAY = "decisioning.propositionDisplay";
                    static final String PERSONALIZATION_REQUEST = "personalization.request";

                    private EventType() {
                    }
                }

                final class Key {
                    static final String PERSONALIZATION = "personalization";
                    static final String CHARACTERISTICS = "characteristics";
                    static final String DECISIONING = "decisioning";
                    static final String PAYLOAD = "payload";
                    static final String ITEMS = "items";
                    static final String ID = "id";
                    static final String SCOPE = "scope";
                    static final String SCOPE_DETAILS = "scopeDetails";
                    static final String QUERY = "query";
                    static final String SURFACES = "surfaces";
                    static final String ACTION = "action";
                    static final String IN_APP_MESSAGE_TRACKING = "inappMessageTracking";
                    static final String CJM_XDM = "cjmXdm";
                    static final String PROPOSITION_EVENT_TYPE = "propositionEventType";
                    static final String PROPOSITIONS = "propositions";

                    private Key() {
                    }
                }

                final class Value {
                    static final String TRIGGERED = "triggered";
                    static final String DISPLAYED = "displayed";
                    static final String CLICKED = "clicked";
                    static final String DISMISSED = "dismissed";
                    static final String EMPTY_CONTENT = "{}";

                    private Value() {
                    }
                }
            }
        }

        public static final class RulesEngine {
            public static final String JSON_KEY = "rules";
            public static final String JSON_CONDITION_KEY = "condition";
            public static final String JSON_CONSEQUENCES_KEY = "consequences";
            public static final String MESSAGE_CONSEQUENCE_ID = "id";
            public static final String MESSAGE_CONSEQUENCE_TYPE = "type";
            public static final String MESSAGE_CONSEQUENCE_CJM_VALUE = "cjmiam";
            public static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML = "html";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS = "remoteAssets";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE = "template";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS = "mobileParameters";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_XDM = "_xdm";
            public static final String CONSEQUENCE_TRIGGERED = "triggeredconsequence";

            private RulesEngine() {
            }
        }

        final class MobileParametersKeys {
            static final String MOBILE_PARAMETERS = "mobileParameters";
            static final String SCHEMA_VERSION = "schemaVersion";
            static final String WIDTH = "width";
            static final String HEIGHT = "height";
            static final String VERTICAL_ALIGN = "verticalAlign";
            static final String VERTICAL_INSET = "verticalInset";
            static final String HORIZONTAL_ALIGN = "horizontalAlign";
            static final String HORIZONTAL_INSET = "horizontalInset";
            static final String UI_TAKEOVER = "uiTakeover";
            static final String DISPLAY_ANIMATION = "displayAnimation";
            static final String DISMISS_ANIMATION = "dismissAnimation";
            static final String BACKDROP_COLOR = "backdropColor";
            static final String BACKDROP_OPACITY = "backdropOpacity";
            static final String CORNER_RADIUS = "cornerRadius";
            static final String GESTURES = "gestures";
            static final String BODY = "body";

            private MobileParametersKeys() {
            }
        }
    }

    public static final class SharedState {
        public static final class Configuration {
            public static final String EXTENSION_NAME = "com.adobe.module.configuration";

            // Messaging
            public static final String EXPERIENCE_EVENT_DATASET_ID = "messaging.eventDataset";

            private Configuration() {
            }
        }

        public static final class EdgeIdentity {
            public static final String EXTENSION_NAME = "com.adobe.edge.identity";
            public static final String IDENTITY_MAP = "identityMap";
            public static final String ECID = "ECID";
            public static final String ID = "id";

            private EdgeIdentity() {
            }
        }

        public static final class Messaging {
            public static final String PUSH_IDENTIFIER = "pushidentifier";
        }

        private SharedState() {
        }
    }
}
