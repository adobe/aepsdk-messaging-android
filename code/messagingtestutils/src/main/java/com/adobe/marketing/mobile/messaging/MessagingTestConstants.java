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

package com.adobe.marketing.mobile.messaging;

/**
 * Class to maintain test constants.
 */
public class MessagingTestConstants {
    public static final String EXTENSION_VERSION = MessagingConstants.EXTENSION_VERSION;
    static final String FRIENDLY_EXTENSION_NAME = "Messaging";
    static final String CACHE_NAME = "com.adobe.messaging.test.cache";
    static final String PROPOSITIONS_CACHE_SUBDIRECTORY = "propositions";
    static final String IMAGES_CACHE_SUBDIRECTORY = "images";
    static final String CACHE_BASE_DIR = "messaging";
    static final String EXTENSION_NAME = "com.adobe.messaging";
    static final String METADATA_PATH = "pathToFile";
    static final String HTTP_HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    static final String HTTP_HEADER_IF_NONE_MATCH = "If-None-Match";
    static final String HTTP_HEADER_ETAG = "Etag";

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
        public static final String PUSH_TRACKING_STATUS_EVENT = "Push tracking status event";
        public static final String PUSH_PROFILE_EDGE_EVENT = "Push notification profile edge event";
        public static final String REFRESH_MESSAGES_EVENT = "Retrieve message definitions";
        public static final String UPDATE_PROPOSITIONS = "Update propositions";
        public static final String MESSAGE_PROPOSITIONS_NOTIFICATION = "Message propositions notification";
        public static final String MESSAGE_PROPOSITIONS_RESPONSE = "Message propositions response";
        public static final String ASSURANCE_SPOOFED_IAM_EVENT_NAME = "Rule Consequence Event (Spoof)";
        static final String TRACK_PROPOSITIONS = "Track propositions";
        static final String EVENT_HISTORY_WRITE = "Write IAM event to history";
        static final String FINALIZE_PROPOSITIONS_RESPONSE = "Finalize propositions response";

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
        public static final String NOTIFICATION = "com.adobe.eventSource.notification";
        public static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        static final String EVENT_HISTORY_WRITE = "com.adobe.eventSource.eventHistoryWrite";

        private EventSource() {
        }
    }

    final class EventMask {
        final class Keys {
            static final String EVENT_TYPE = "eventType";
            static final String MESSAGE_ID = "id";
            static final String TRACKING_ACTION = "action";

            private Keys() {
            }
        }

        final class Mask {
            static final String EVENT_TYPE = "iam.eventType";
            static final String MESSAGE_ID = "iam.id";
            static final String TRACKING_ACTION = "iam.action";

            private Mask() {
            }
        }
    }

    public static final class EventDataKey {
        static final String REQUEST_EVENT_ID = "requestEventId";

        private EventDataKey() {
        }

        final class RulesEngine {
            static final String JSON_RULES_KEY = "rules";
            static final String JSON_CONSEQUENCES_KEY = "consequences";
            static final String JSON_VERSION_KEY = "version";
            static final String MESSAGE_CONSEQUENCE_ID = "id";
            static final String MESSAGE_CONSEQUENCE_TYPE = "type";
            static final String MESSAGE_CONSEQUENCE_CJM_VALUE = "cjmiam";
            static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA = "data";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA = "schema";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML = "html";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT = "content";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS = "remoteAssets";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS = "mobileParameters";
            static final String CONSEQUENCE_TRIGGERED = "triggeredconsequence";

            private RulesEngine() {
            }
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
        public static final String IAM_HISTORY = "iam";

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
            public static final String UPDATE_PROPOSITIONS = "updatepropositions";
            public static final String SURFACES = "surfaces";
            public static final String ENDING_EVENT_ID = "endingEventId";
            public static final String GET_PROPOSITIONS = "getpropositions";
            public static final String PROPOSITIONS = "propositions";
            public static final String RESPONSE_ERROR = "responseerror";
            static final String TRACK_PROPOSITIONS = "trackpropositions";
            public static final String PROPOSITION_INTERACTION = "propositioninteraction";
            public static final String PUSH_NOTIFICATION_TRACKING_STATUS = "pushTrackingStatus";
            public static final String PUSH_NOTIFICATION_TRACKING_MESSAGE = "pushTrackingStatusMessage";


            private Messaging() {
            }

            final class XDMDataKeys {
                static final String XDM = "xdm";
                static final String ACTION_ID = "actionID";
                static final String CUSTOM_ACTION = "customAction";
                static final String PUSH_PROVIDER_MESSAGE_ID = "pushProviderMessageID";
                static final String PUSH_PROVIDER = "pushProvider";
                static final String EVENT_TYPE = "eventType";
                static final String PUSH_NOTIFICATION_TRACKING_MIXIN_NAME = "pushNotificationTracking";
                static final String REQUEST = "request";
                static final String SEND_COMPLETION = "sendCompletion";

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

            public final class Inbound {
                static final String SURFACE_BASE = "mobileapp://";

                private Inbound() {
                }

                public final class PropositionEventType {
                    public static final String PROPOSITION_EVENT_TYPE_DISMISS = "dismiss";
                    public static final String PROPOSITION_EVENT_TYPE_INTERACT = "interact";
                    public static final String PROPOSITION_EVENT_TYPE_TRIGGER = "trigger";
                    public static final String PROPOSITION_EVENT_TYPE_DISPLAY = "display";
                    public static final String PROPOSITION_EVENT_TYPE_DISQUALIFY = "disqualify";
                    public static final String PROPOSITION_EVENT_TYPE_SUPPRESSED_DISPLAY = "suppressDisplay";

                    private PropositionEventType() {
                    }
                }

                public final class EventType {
                    public static final String DISMISS = "decisioning.propositionDismiss";
                    public static final String INTERACT = "decisioning.propositionInteract";
                    public static final String TRIGGER = "decisioning.propositionTrigger";
                    public static final String DISPLAY = "decisioning.propositionDisplay";
                    public static final String DISQUALIFY = "decisioning.propositionDisqualify";
                    public static final String SUPPRESSED_DISPLAY = "decisioning.propositionSuppressDisplay";
                    public static final String PERSONALIZATION_REQUEST = "personalization.request";
                    public static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_OPENED = "pushTracking.applicationOpened";
                    public static final String PUSH_NOTIFICATION_EVENT_TYPE_STRING_CUSTOM_ACTION = "pushTracking.customAction";

                    private EventType() {
                    }
                }

                final class Key {
                    static final String PERSONALIZATION = "personalization";
                    static final String DECISIONING = "decisioning";
                    static final String PAYLOAD = "payload";
                    static final String ITEMS = "items";
                    static final String ID = "id";
                    static final String SCOPE = "scope";
                    static final String SCOPE_DETAILS = "scopeDetails";
                    static final String QUERY = "query";
                    static final String SURFACES = "surfaces";
                    static final String PROPOSITION_EVENT_TYPE = "propositionEventType";
                    static final String PROPOSITIONS = "propositions";
                    static final String PROPOSITION_ACTION = "propositionAction";
                    static final String LABEL = "label";
                    static final String SCHEMAS = "schemas";
                    static final String CHARACTERISTICS = "characteristics";
                    static final String TOKENS = "tokens";

                    private Key() {
                    }
                }
            }
        }

        public static final class RulesEngine {
            public static final String MESSAGE_CONSEQUENCE_CJM_VALUE = "cjmiam";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_DATA = "data";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_SCHEMA = "schema";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_CONTENT = "content";
            public static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS = "remoteAssets";
            public static final String MESSAGE_CONSEQUENCE_TYPE = "type";
            public static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
            public static final String CONSEQUENCE_TRIGGERED = "triggeredconsequence";


            private RulesEngine() {
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

    final class SchemaValues {
        static final String SCHEMA_HTML_CONTENT = "https://ns.adobe.com/personalization/html-content-item";
        static final String SCHEMA_JSON_CONTENT = "https://ns.adobe.com/personalization/json-content-item";
        static final String SCHEMA_RULESET_ITEM = "https://ns.adobe.com/personalization/ruleset-item";
        static final String SCHEMA_IAM = "https://ns.adobe.com/personalization/message/in-app";
        static final String SCHEMA_FEED_ITEM = "https://ns.adobe.com/personalization/message/feed-item";
        static final String SCHEMA_NATIVE_ALERT = "https://ns.adobe.com/personalization/message/native-alert";
        static final String SCHEMA_DEFAULT_CONTENT = "https://ns.adobe.com/personalization/default-content-item";

        private SchemaValues() {
        }
    }

    final class ContentTypes {
        static final String APPLICATION_JSON = "application/json";
        static final String TEXT_HTML = "text/html";
        static final String TEXT_XML = "text/xml";
        static final String TEXT_PLAIN = "text/plain";
        private ContentTypes() {
        }
    }

    final class ConsequenceDetailKeys {
        static final String ID = "id";
        static final String SCHEMA = "schema";
        static final String DATA = "data";
        static final String CONTENT = "content";

        private ConsequenceDetailKeys() {}
    }

    final class ConsequenceDetailDataKeys {
        static final String FORMAT = "format";
        static final String CONTENT = "content";
        static final String CONTENT_TYPE = "contentType";
        static final String PUBLISHED_DATE = "publishedDate";
        static final String EXPIRY_DATE = "expiryDate";
        static final String METADATA = "meta";
        static final String MOBILE_PARAMETERS = "mobileParameters";
        static final String WEB_PARAMETERS = "webParameters";
        static final String REMOTE_ASSETS = "remoteAssets";

        private ConsequenceDetailDataKeys() {
        }
    }

    final class MessageFeedKeys {
        static final String TITLE = "title";
        static final String BODY = "body";
        static final String CONTENT = "content";
        static final String IMAGE_URL = "imageUrl";
        static final String ACTION_TITLE = "actionTitle";
        static final String ACTION_URL = "actionUrl";
        static final String FEEDS = "feeds";
        static final String FEED_NAME = "feedName";
        static final String SURFACE = "surface";

        private MessageFeedKeys() {
        }
    }

    public final class Push {
        public class PayloadKeys {
            public static final String TITLE = "adb_title";
            public static final String BODY = "adb_body";
            public static final String SOUND = "adb_sound";
            public static final String BADGE_NUMBER = "adb_n_count";
            public static final String NOTIFICATION_VISIBILITY = "adb_n_visibility";
            public static final String NOTIFICATION_PRIORITY = "adb_n_priority";
            public static final String CHANNEL_ID = "adb_channel_id";
            public static final String ICON = "adb_icon";
            public static final String IMAGE_URL = "adb_image";
            public static final String ACTION_TYPE = "adb_a_type";
            public static final String ACTION_URI = "adb_uri";
            public static final String ACTION_BUTTONS = "adb_act";

            private PayloadKeys() {}
        }
    }
}
