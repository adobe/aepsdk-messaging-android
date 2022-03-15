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

final class MessagingConstants {

    static final String LOG_TAG = "Messaging";
    static final String EXTENSION_VERSION = "1.1.0";
    static final String FRIENDLY_EXTENSION_NAME = "Messaging";
    static final String EXTENSION_NAME = "com.adobe.messaging";
    static final String CACHE_NAME = "com.adobe.messaging.cache";
    static final String MESSAGES_CACHE_SUBDIRECTORY = "messages";
    static final String IMAGES_CACHE_SUBDIRECTORY = "images";

    private MessagingConstants() {
    }

    final class TrackingKeys {
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

    final class ManifestMetadataKeys {
        static final String ACTIVITY_ID = "activityId";
        static final String PLACEMENT_ID = "placementId";
    }

    final class JsonValues {
        static final String FCM = "fcm";
        static final String ECID = "ECID";
    }

    final class MessagingScheme {
        static final String ADOBE_INAPP = "adbinapp";
        static final String PATH_CANCEL = "cancel";
        static final String PATH_CONFIRM = "confirm";
        static final String PATH_DISMISS = "dismiss";
        static final String INTERACTION = "interaction";
        static final String DEEPLINK = "deeplink";
        static final String LINK = "link";
        static final String JS = "js";
    }

    final class DefaultValues {
        final class Optimize {
            static final int MAX_ITEM_COUNT = 30;
        }
    }

    final class EventDataKeys {
        static final String STATE_OWNER = "stateowner";

        final class Identity {
            static final String PUSH_IDENTIFIER = "pushidentifier";

            private Identity() {
            }
        }

        final class Messaging {
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

            final class XDMDataKeys {
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

                final class EventType {
                    static final String OPENED = "push.opened";
                    static final String INTERACT = "push.interact";
                }

                private PushNotificationDetailsDataKeys() {
                }
            }

            final class IAMDetailsDataKeys {
                final class EventType {
                    static final String DISMISS = "inapp.dismiss";
                    static final String INTERACT = "inapp.interact";
                    static final String TRIGGER = "inapp.trigger";
                    static final String DISPLAY = "inapp.display";
                }

                private IAMDetailsDataKeys() {
                }
            }
        }

        final class Optimize {
            static final String REQUEST_TYPE = "requesttype";
            static final String DECISION_SCOPES = "decisionscopes";
            static final String NAME = "name";
            static final String ID = "id";
            static final String DATA = "data";
            static final String CONTENT = "content";
            static final String PAYLOAD = "payload";
            static final String ACTIVITY = "activity";
            static final String PLACEMENT = "placement";
            static final String ACTIVITY_ID = "activityId";
            static final String PLACEMENT_ID = "placementId";
            static final String ITEMS = "items";
            static final String XDM_NAME = "xdm:name";
            static final String SCOPE = "scope";

            private Optimize() {
            }
        }

        final class Values {
            final class Optimize {
                static final String UPDATE_PROPOSITIONS = "updatepropositions";
            }
        }

        final class RulesEngine {
            static final String JSON_KEY = "rules";
            static final String JSON_CONDITION_KEY = "condition";
            static final String JSON_CONSEQUENCES_KEY = "consequences";
            static final String MESSAGE_CONSEQUENCE_ID = "id";
            static final String MESSAGE_CONSEQUENCE_TYPE = "type";
            static final String MESSAGE_CONSEQUENCE_CJM_VALUE = "cjmiam";
            static final String MESSAGE_CONSEQUENCE_DETAIL = "detail";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_HTML = "html";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_REMOTE_ASSETS = "remoteAssets";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_TEMPLATE = "template";
            static final String MESSAGE_CONSEQUENCE_DETAIL_KEY_MOBILE_PARAMETERS = "mobileParameters";
            static final String MESSAGE_CONSEQUENCE_DETAIL_XDM = "_xdm";
            static final String CONSEQUENCE_TRIGGERED = "triggeredconsequence";

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

    final class EventType {
        static final String MESSAGING = "com.adobe.eventType.messaging";
        static final String EDGE = "com.adobe.eventType.edge";
        static final String OPTIMIZE = "com.adobe.eventType.optimize";
    }

    final class EventName {
        static final String PUSH_NOTIFICATION_INTERACTION_EVENT = "Push notification interaction event";
        static final String PUSH_TRACKING_EDGE_EVENT = "Push tracking edge event";
        static final String PUSH_PROFILE_EDGE_EVENT = "Push notification profile edge event";
        static final String RETRIEVE_MESSAGE_DEFINITIONS_EVENT = "Retrieve message definitions";
        static final String IAM_INTERACTION_EVENT = "In App tracking edge event";
    }

    final class EventSource {
        static final String PERSONALIZATION_DECISIONS = "personalization:decisions";
        static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
    }

    final class EventDispatchErrors {
        static final String OPTIMIZE_OFFER_RETRIEVAL_ERROR = "Error in dispatching event for refreshing messages from Optimize";
        static final String PUSH_PROFILE_UPDATE_ERROR = "Error in dispatching event for updating the push profile details";
        static final String PUSH_TRACKING_ERROR = "Error in dispatching event for push notification tracking";
        static final String IN_APP_TRACKING_ERROR = "Error in dispatching event for in-app notification tracking";
    }

    final class EventDataValues {
        static final String EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED = "pushTracking.applicationOpened";
        static final String EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION = "pushTracking.customAction";
    }

    final class EventMask {
        final class XDM {
            // mask values for experience event storage in event history
            static final String EVENT_TYPE = "xdm.eventType";
            static final String MESSAGE_EXECUTION_ID = "xdm._experience.customerJourneyManagement.messageExecution.messageExecutionID";
            static final String TRACKING_ACTION = "xdm.inappMessageTracking.action";
        }
    }

    final class SharedState {

        private SharedState() {
        }

        final class Configuration {
            static final String EXTENSION_NAME = "com.adobe.module.configuration";

            // Messaging
            static final String EXPERIENCE_EVENT_DATASET_ID = "messaging.eventDataset";

            private Configuration() {/* no-op */}
        }

        final class EdgeIdentity {
            static final String EXTENSION_NAME = "com.adobe.edge.identity";
            static final String IDENTITY_MAP = "identityMap";
            static final String ECID = "ECID";
            static final String ID = "id";

            private EdgeIdentity() {/* no-op */}
        }

        final class Messaging {
            static final String PUSH_IDENTIFIER = "pushidentifier";
        }
    }

    final class PushNotificationPayload {
        static final String TITLE = "adb_title";
        static final String BODY = "adb_body";
        static final String SOUND = "adb_sound";
        static final String NOTIFICATION_COUNT = "adb_n_count";
        static final String NOTIFICATION_PRIORITY = "adb_n_priority";
        static final String CHANNEL_ID = "adb_channel_id";
        static final String ICON = "adb_icon";
        static final String IMAGE_URL = "adb_image";
        static final String ACTION_TYPE = "adb_a_type";
        static final String ACTION_URI = "adb_uri";
        static final String ACTION_BUTTONS = "adb_act";

        private PushNotificationPayload() {
        }

        final class ActionButtonType {
            static final String DEEPLINK = "DEEPLINK";
            static final String WEBURL = "WEBURL";
            static final String DISMISS = "DISMISS";

            private ActionButtonType() {/* no-op */}
        }

        final class NotificationPriorities {
            static final String PRIORITY_DEFAULT = "PRIORITY_DEFAULT";
            static final String PRIORITY_MIN = "PRIORITY_MIN";
            static final String PRIORITY_LOW = "PRIORITY_LOW";
            static final String PRIORITY_HIGH = "PRIORITY_HIGH";
            static final String PRIORITY_MAX = "PRIORITY_MAX";
        }

        final class ActionButtons {
            static final String LABEL = "label";
            static final String URI = "uri";
            static final String TYPE = "type";
        }
    }
}
