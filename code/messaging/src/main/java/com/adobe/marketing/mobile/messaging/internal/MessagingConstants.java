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

package com.adobe.marketing.mobile.messaging.internal;

public final class MessagingConstants {

    public static final String LOG_TAG = "Messaging";
    static final String EXTENSION_VERSION = "2.2.1";
    static final String FRIENDLY_EXTENSION_NAME = "Messaging";
    static final String EXTENSION_NAME = "com.adobe.messaging";
    static final String RULES_ENGINE_NAME = EXTENSION_NAME + ".rulesengine";
    static final String CACHE_BASE_DIR = "messaging";
    static final String PROPOSITIONS_CACHE_SUBDIRECTORY = "propositions";
    static final String IMAGES_CACHE_SUBDIRECTORY = "images";
    static final String HTTP_HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    static final String HTTP_HEADER_IF_NONE_MATCH = "If-None-Match";
    static final String HTTP_HEADER_ETAG = "Etag";
    static final String METADATA_PATH = "pathToFile";
    static final int DEFAULT_TIMEOUT = 5;

    private MessagingConstants() {
    }

    final class QueryParameters {
        static final String JAVASCRIPT_QUERY_KEY = "js";
        static final String ADOBE_INAPP = "adbinapp";
        static final String PATH_CANCEL = "cancel";
        static final String PATH_DISMISS = "dismiss";
        static final String INTERACTION = "interaction";
        static final String DEEPLINK = "adb_deeplink";
        static final String LINK = "link";

        private QueryParameters() {
        }
    }

    final class TrackingKeys {
        static final String XDM = "xdm";
        static final String META = "meta";
        static final String CJM = "cjm";
        static final String MIXINS = "mixins";
        static final String EXPERIENCE = "_experience";
        static final String CUSTOMER_JOURNEY_MANAGEMENT = "customerJourneyManagement";
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

        private TrackingKeys() {
        }
    }

    final class JsonValues {
        static final String FCM = "fcm";
        static final String ECID = "ECID";

        private JsonValues() {
        }
    }

    final class PayloadKeys {
        static final String DATA = "data";
        static final String CONTENT = "content";
        static final String ID = "id";
        static final String SCOPE = "scope";
        static final String SCOPE_DETAILS = "scopeDetails";
        static final String SCHEMA = "schema";
        static final String CORRELATION_ID = "correlationID";
        static final String ACTIVITY = "activity";

        private PayloadKeys() {
        }
    }

    final class EventDataKeys {
        static final String REQUEST_EVENT_ID = "requestEventId";
        static final String IAM_HISTORY = "iam";

        final class Identity {
            static final String PUSH_IDENTIFIER = "pushidentifier";

            private Identity() {
            }
        }

        final class Messaging {
            static final String TRACK_INFO_KEY_EVENT_TYPE = "eventType";
            static final String TRACK_INFO_KEY_MESSAGE_ID = "messageId";
            static final String TRACK_INFO_KEY_APPLICATION_OPENED = "applicationOpened";
            static final String TRACK_INFO_KEY_ACTION_ID = "actionId";
            static final String TRACK_INFO_KEY_ADOBE_XDM = "adobe_xdm";
            static final String REFRESH_MESSAGES = "refreshmessages";

            static final String PUSH_NOTIFICATION_TRACKING_STATUS = "pushTrackingStatus";

            static final String PUSH_NOTIFICATION_TRACKING_MESSAGE = "pushTrackingStatusMessage";
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

                private XDMDataKeys() {
                }
            }

            final class PushNotificationDetailsDataKeys {
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

                final class EventType {
                    static final String OPENED = "pushTracking.applicationOpened";
                    static final String CUSTOM_ACTION = "pushTracking.customAction";

                    private EventType() {
                    }
                }
            }

            final class IAMDetailsDataKeys {
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

                    private Key() {
                    }
                }
            }
        }

        final class RulesEngine {
            static final String JSON_KEY = "rules";
            static final String JSON_CONSEQUENCES_KEY = "consequences";
            static final String MESSAGE_CONSEQUENCE_ID = "id";
            static final String MESSAGE_CONSEQUENCE_TYPE = "type";
            static final String MESSAGE_CONSEQUENCE_CJM_VALUE = "cjmiam";
            static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML = "html";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS = "remoteAssets";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS = "mobileParameters";
            static final String CONSEQUENCE_TRIGGERED = "triggeredconsequence";

            private RulesEngine() {
            }
        }

        final class MobileParametersKeys {
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

            private MobileParametersKeys() {
            }
        }
    }

    final class EventType {
        static final String MESSAGING = "com.adobe.eventType.messaging";
        static final String EDGE = "com.adobe.eventType.edge";

        private EventType() {
        }
    }

    final class EventName {
        static final String MESSAGE_INTERACTION_EVENT = "Messaging interaction event";
        static final String PUSH_TRACKING_EDGE_EVENT = "Push tracking edge event";
        static final String PUSH_TRACKING_STATUS_EVENT = "Push tracking status event";
        static final String PUSH_PROFILE_EDGE_EVENT = "Push notification profile edge event";
        static final String REFRESH_MESSAGES_EVENT = "Retrieve message definitions";

        static final String ASSURANCE_SPOOFED_IAM_EVENT_NAME = "Rule Consequence Event (Spoof)";

        private EventName() {
        }
    }

    final class EventSource {
        static final String PERSONALIZATION_DECISIONS = "personalization:decisions";
        static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";

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

    final class SharedState {

        private SharedState() {
        }

        final class Configuration {
            static final String EXTENSION_NAME = "com.adobe.module.configuration";

            // Messaging
            static final String EXPERIENCE_EVENT_DATASET_ID = "messaging.eventDataset";

            private Configuration() {
            }
        }

        final class EdgeIdentity {
            static final String EXTENSION_NAME = "com.adobe.edge.identity";
            static final String IDENTITY_MAP = "identityMap";
            static final String ECID = "ECID";
            static final String ID = "id";

            private EdgeIdentity() {
            }
        }

        final class Messaging {
            static final String PUSH_IDENTIFIER = "pushidentifier";

            private Messaging() {
            }
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

            private PayloadKeys() {
            }
        }
    }
}