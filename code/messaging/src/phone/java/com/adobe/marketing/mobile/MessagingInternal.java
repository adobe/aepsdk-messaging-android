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

import static com.adobe.marketing.mobile.MessagingConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.MessagingConstants.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.APP_ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.CODE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DENY_LISTED;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.ID;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.IDENTITY;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.NAMESPACE;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PLATFORM;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PUSH_NOTIFICATION_DETAILS;
import static com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.TOKEN;
import static com.adobe.marketing.mobile.MessagingConstants.JSON_VALUES.ECID;
import static com.adobe.marketing.mobile.MessagingConstants.JSON_VALUES.FCM;
import static com.adobe.marketing.mobile.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.CJM;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.COLLECT;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.DATASET_ID;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.EXPERIENCE;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.MESSAGE_PROFILE_JSON;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.META;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.MIXINS;
import static com.adobe.marketing.mobile.MessagingConstants.TrackingKeys.XDM;

import com.adobe.marketing.mobile.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class MessagingInternal extends Extension {
    private final String SELF_TAG = "MessagingInternal";
    private final MessagingState messagingState;
    private final Object executorMutex = new Object();
    private final ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
    private final InAppNotificationHandler inAppNotificationHandler;
    private CacheManager cacheManager;
    private AndroidEventHistory androidEventHistory;
    private ExecutorService executorService;
    private boolean initialMessageFetchComplete = false;

    /**
     * Constructor.
     *
     * <p>
     * Called during messaging extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> {@link ListenerHubSharedState} listening to event with eventType {@link EventType#HUB}
     *           and EventSource {@link EventSource#SHARED_STATE}</li>
     *     <li> {@link ListenerMessagingRequestContent} listening to event with eventType {@link MessagingConstants.EventType#MESSAGING}
     *          and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> {@link ListenerIdentityRequestContent} listening to event with eventType {@link EventType#GENERIC_IDENTITY}
     * 	        and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> {@link ListenerOffersPersonalizationDecisions} listening to event with eventType {@link MessagingConstants.EventType#EDGE}
     * 	        and EventSource {@link MessagingConstants.EventSource#PERSONALIZATION_DECISIONS}</li>
     *      <li> {@link ListenerRulesEngineResponseContent} listening to event with eventType {@link EventType#RULES_ENGINE}
     * 	        and EventSource {@link EventSource#RESPONSE_CONTENT}</li>
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    protected MessagingInternal(final ExtensionApi extensionApi) {
        super(extensionApi);

        final PlatformServices platformServices = MessagingUtils.getPlatformServices();
        final SystemInfoService systemInfoService = platformServices.getSystemInfoService();

        // initialize the cache manager
        try {
            cacheManager = new CacheManager(systemInfoService);
        } catch (final MissingPlatformServicesException e) {
            Log.warning(LOG_TAG, "Exception occurred when creating the CacheManager: %s", e.getMessage());
        }

        // initialize the EventHistory database
        try {
            androidEventHistory = new AndroidEventHistory(systemInfoService);
            MobileCore.getCore().eventHub.setEventHistory(androidEventHistory);
        } catch (final EventHistoryDatabaseCreationException e) {
            Log.warning(LOG_TAG, "Exception occurred when creating event history: %s", e.getMessage());
        }

        // initialize the in-app notification handler and check if we have any cached messages. if we do, load them.
        inAppNotificationHandler = new InAppNotificationHandler(this, cacheManager);
        registerEventListeners(extensionApi);

        // initialize the messaging state
        messagingState = new MessagingState();
    }

    /**
     * Get profile data with token
     *
     * @param token push token which needs to be synced
     * @param ecid  experience cloud id of the device
     * @return {@link Map} of profile data in the correct format with token
     */
    private static Map<String, Object> getProfileEventData(final String token, final String ecid) {
        if (ecid == null) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "MessagingInternal - Failed to sync push token, ecid is null.");
            return null;
        }

        final Map<String, String> namespace = new HashMap<>();
        namespace.put(CODE, ECID);

        final Map<String, Object> identity = new HashMap<>();
        identity.put(NAMESPACE, namespace);
        identity.put(ID, ecid);

        final ArrayList<Map<String, Object>> pushNotificationDetailsArray = new ArrayList<>();
        final Map<String, Object> pushNotificationDetailsData = new HashMap<>();
        pushNotificationDetailsData.put(IDENTITY, identity);
        pushNotificationDetailsData.put(APP_ID, App.getApplication().getPackageName());
        pushNotificationDetailsData.put(TOKEN, token);
        pushNotificationDetailsData.put(PLATFORM, FCM);
        pushNotificationDetailsData.put(DENY_LISTED, false);

        pushNotificationDetailsArray.add(pushNotificationDetailsData);

        final Map<String, Object> data = new HashMap<>();
        data.put(PUSH_NOTIFICATION_DETAILS, pushNotificationDetailsArray);

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(DATA, data);

        return eventData;
    }

    /**
     * Builds the xdmMap with the tracking information provided by the customer in eventData.
     *
     * @param eventType String eventType can be either applicationOpened or customAction
     * @param messageId String messageId for the push notification provided by the customer
     * @param actionId  String indicating the actionId of the action taken by the user on the push notification
     * @return {@link Map} object containing the xdm formatted data
     */
    private static Map<String, Object> getXdmData(final String eventType, final String messageId, final String actionId) {
        final Map<String, Object> xdmMap = new HashMap<>();
        final Map<String, Object> trackingMap = new HashMap<>();
        final Map<String, Object> customActionMap = new HashMap<>();

        if (actionId != null) {
            customActionMap.put(XDMDataKeys.XDM_DATA_ACTION_ID, actionId);
            trackingMap.put(XDMDataKeys.XDM_DATA_CUSTOM_ACTION, customActionMap);
        }

        trackingMap.put(XDMDataKeys.XDM_DATA_PUSH_PROVIDER, FCM);
        trackingMap.put(XDMDataKeys.XDM_DATA_PUSH_PROVIDER_MESSAGE_ID, messageId);
        xdmMap.put(XDMDataKeys.XDM_DATA_EVENT_TYPE, eventType);
        xdmMap.put(XDMDataKeys.XDM_DATA_PUSH_NOTIFICATION_TRACKING, trackingMap);

        return xdmMap;
    }

    private static void addApplicationData(final boolean applicationOpened, final Map<String, Object> xdmMap) {
        final Map<String, Object> applicationMap = new HashMap<>();
        final Map<String, Object> launchesMap = new HashMap<>();
        launchesMap.put(MessagingConstants.TrackingKeys.LAUNCHES_VALUE, applicationOpened ? 1 : 0);
        applicationMap.put(MessagingConstants.TrackingKeys.LAUNCHES, launchesMap);
        xdmMap.put(MessagingConstants.TrackingKeys.APPLICATION, applicationMap);
    }

    /**
     * Adding XDM specific data to tracking information.
     *
     * @param eventData eventData which contains the xdm data forwarded by the customer.
     * @param xdmMap    xdmMap map which is updated.
     */
    private static void addXDMData(final EventData eventData, final Map<String, Object> xdmMap) {
        // Extract the xdm adobe data string from the event data.
        final String adobe = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, null);
        if (adobe == null) {
            Log.warning(LOG_TAG, "MessagingInternal - Failed to send adobe data with the tracking data, adobe xdm data is null.");
            return;
        }

        try {
            // Convert the adobe string to json object
            final JSONObject xdmJson = new JSONObject(adobe);
            final Map<String, Object> xdmMapObject = MessagingUtils.toMap(xdmJson);

            if (xdmMapObject == null) {
                Log.warning(LOG_TAG, "Failed to send adobe data with the tracking data, adobe xdm data conversion to map faileds.");
                return;
            }

            Map<String, Object> mixins = null;

            // Check for if the json has the required keys
            if (xdmMapObject.containsKey(CJM) && xdmMapObject.get(CJM) instanceof Map) {
                mixins = (Map<String, Object>) xdmMapObject.get(CJM);
            }

            if (xdmMapObject.containsKey(MIXINS) && xdmMapObject.get(MIXINS) instanceof Map) {
                mixins = (Map<String, Object>) xdmMapObject.get(MIXINS);
            }

            if (mixins == null) {
                Log.debug(LOG_TAG, "MessagingInternal - Failed to send cjm xdm data with the tracking, Missing xdm data.");
                return;
            }

            xdmMap.putAll(mixins);

            // Check if the xdm data provided by the customer is using cjm for tracking
            // Check if both {@link MessagingConstants#EXPERIENCE} and {@link MessagingConstants#CUSTOMER_JOURNEY_MANAGEMENT} exists
            if (mixins.containsKey(EXPERIENCE) && mixins.get(EXPERIENCE) instanceof Map) {
                Map<String, Object> experience = (Map<String, Object>) mixins.get(EXPERIENCE);
                if (experience.containsKey(CUSTOMER_JOURNEY_MANAGEMENT) && experience.get(CUSTOMER_JOURNEY_MANAGEMENT) instanceof Map) {
                    Map<String, Object> cjm = (Map<String, Object>) experience.get(CUSTOMER_JOURNEY_MANAGEMENT);
                    // Adding Message profile and push channel context to CUSTOMER_JOURNEY_MANAGEMENT
                    final JSONObject jObject = new JSONObject(MESSAGE_PROFILE_JSON);
                    cjm.putAll(MessagingUtils.toMap(jObject));

                    experience.put(CUSTOMER_JOURNEY_MANAGEMENT, cjm);
                    xdmMap.put(EXPERIENCE, experience);
                }
            } else {
                Log.warning(LOG_TAG, "MessagingInternal - Failed to send cjm xdm data with the tracking, required keys are missing.");
            }
        } catch (JSONException e) {
            Log.warning(LOG_TAG, "MessagingInternal - Failed to send adobe data with the tracking data, adobe data is malformed : %s", e.getMessage());
        } catch (ClassCastException e) {
            Log.warning(LOG_TAG, "MessagingInternal - Failed to send adobe data with the tracking data, adobe data is malformed : %s", e.getMessage());
        }
    }

    /**
     * Overridden method of {@link Extension} class to provide a valid extension name to register with eventHub.
     *
     * @return A {@link String} extension name for Messaging
     */
    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide the extension version.
     *
     * @return A {@link String} representing the extension version
     */
    @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Overridden method of {@link Extension} class to handle error occurred during registration of the module.
     *
     * @param extensionUnexpectedError {@link ExtensionUnexpectedError} occurred exception
     */
    protected void onUnexpectedError(final ExtensionUnexpectedError extensionUnexpectedError) {
        super.onUnexpectedError(extensionUnexpectedError);
        this.onUnregistered();
    }

    private void registerEventListeners(final ExtensionApi extensionApi) {
        ExtensionErrorCallback<ExtensionError> listenerErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Log.error(MessagingConstants.LOG_TAG, "%s - Error in registering %s event : Extension version - %s : Error %s",
                        SELF_TAG, MessagingConstants.EventType.MESSAGING, MessagingConstants.EXTENSION_VERSION, extensionError.toString());
            }
        };

        extensionApi.registerEventListener(EventType.HUB.getName(), EventSource.SHARED_STATE.getName(), ListenerHubSharedState.class, listenerErrorCallback);
        extensionApi.registerEventListener(MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT.getName(), ListenerMessagingRequestContent.class, listenerErrorCallback);
        extensionApi.registerEventListener(EventType.GENERIC_IDENTITY.getName(), EventSource.REQUEST_CONTENT.getName(), ListenerIdentityRequestContent.class, listenerErrorCallback);
        extensionApi.registerEventListener(MessagingConstants.EventType.EDGE, MessagingConstants.EventSource.PERSONALIZATION_DECISIONS, ListenerOffersPersonalizationDecisions.class, listenerErrorCallback);
        extensionApi.registerEventListener(EventType.RULES_ENGINE.getName(), EventSource.RESPONSE_CONTENT.getName(), ListenerRulesEngineResponseContent.class, listenerErrorCallback);

        Log.debug(MessagingConstants.LOG_TAG, "%s - Registering Messaging extension - version %s",
                SELF_TAG, MessagingConstants.EXTENSION_VERSION);
    }

    /**
     * This method queues the provided event in {@link #eventQueue}.
     *
     * <p>
     * The queued events are then processed in an orderly fashion.
     * No action is taken if the provided event's value is null.
     *
     * @param event The {@link Event} thats needs to be queued
     */
    void queueEvent(final Event event) {
        if (event == null) {
            return;
        }

        eventQueue.add(event);
    }

    /**
     * Processes the queued event one by one until queue is empty.
     *
     * <p>
     * Suspends processing of the events in the queue if the configuration or identity shared state is not ready.
     * Processed events are polled out of the {@link #eventQueue}.
     */
    void processEvents() {
        while (!eventQueue.isEmpty()) {
            Event eventToProcess = eventQueue.peek();

            if (eventToProcess == null) {
                Log.debug(MessagingConstants.LOG_TAG, "%s - Unable to process event, Event received is null.", SELF_TAG);
                return;
            }

            ExtensionErrorCallback<ExtensionError> configurationErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null) {
                        Log.warning(MessagingConstants.LOG_TAG,
                                String.format("MessagingInternal : Could not process event, an error occurred while retrieving configuration shared state: %s",
                                        extensionError.getErrorName()));
                    }
                }
            };

            ExtensionErrorCallback<ExtensionError> edgeIdentityErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null) {
                        Log.warning(MessagingConstants.LOG_TAG,
                                String.format("MessagingInternal : Could not process event, an error occurred while retrieving edge identity shared state: %s",
                                        extensionError.getErrorName()));
                    }
                }
            };

            final Map<String, Object> configSharedState = getApi().getSharedEventState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME,
                    eventToProcess, configurationErrorCallback);

            final Map<String, Object> edgeIdentitySharedState = getApi().getXDMSharedEventState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                    eventToProcess, edgeIdentityErrorCallback);

            // NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (configSharedState == null || configSharedState.isEmpty()) {
                Log.warning(MessagingConstants.LOG_TAG,
                        "%s : Could not process event, configuration shared state is pending", SELF_TAG);
                return;
            }

            // NOTE: identity is mandatory processing the event, so if shared state is null (pending) stop processing events
            if (edgeIdentitySharedState == null || edgeIdentitySharedState.isEmpty()) {
                Log.warning(MessagingConstants.LOG_TAG,
                        "%s : Could not process event, identity shared state is pending", SELF_TAG);
                return;
            }

            // Set the messaging state
            messagingState.setState(configSharedState, edgeIdentitySharedState);

            // fetch messages from offers on initial launch once we have configuration and identity state set
            if (messagingState.isReadyForEvents()
                    && !initialMessageFetchComplete) {
                inAppNotificationHandler.fetchMessages();
                initialMessageFetchComplete = true;
            }

            // validate fetch messages event then refresh in-app messages from offers
            if (MessagingUtils.isFetchMessagesEvent(eventToProcess)) {
                if (messagingState.isConfigStateSet()) {
                    inAppNotificationHandler.fetchMessages();
                }
            } else if (MessagingUtils.isGenericIdentityRequestEvent(eventToProcess)) {
                // handle the push token from generic identity request content event
                handlePushToken(eventToProcess);
            } else if (MessagingUtils.isMessagingRequestContentEvent(eventToProcess)) {
                // Need experience event dataset id for sending the push token
                if (!configSharedState.containsKey(MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID)) {
                    Log.warning(LOG_TAG, "%s - Unable to track push notification interaction, experience event dataset id is empty. Check the messaging launch extension to add the experience event dataset.", SELF_TAG);
                    return;
                }
                // handle the push tracking information from messaging request content event
                handleTrackingInfo(eventToProcess);
                // validate the edge response event from Optimize then load any iam rules present
            } else if (MessagingUtils.isEdgePersonalizationDecisionEvent(eventToProcess)) {
                final ArrayList<Map<String, Variant>> payload = (ArrayList<Map<String, Variant>>) eventToProcess.getEventData().get(MessagingConstants.EventDataKeys.Optimize.PAYLOAD);
                if (payload != null && payload.size() > 0) {
                    inAppNotificationHandler.handleOfferNotificationPayload(payload.get(0), eventToProcess);
                    MessagingUtils.cacheRetrievedMessages(cacheManager, payload.get(0));
                }
                // handle rules response events containing message definitions
            } else if (MessagingUtils.isMessagingConsequenceEvent(eventToProcess)) {
                inAppNotificationHandler.createInAppMessage(eventToProcess);
            }
            // event processed, remove it from the queue
            eventQueue.poll();
        }
    }

    public void handlePushToken(final Event event) {
        if (event == null || event.getEventData() == null) {
            Log.debug(LOG_TAG, "%s - Unable to sync push token. Event or event data received is null.", SELF_TAG);
            return;
        }

        final String pushToken = (String) event.getEventData().get(MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER);

        if (pushToken == null || pushToken.isEmpty()) {
            MobileCore.log(LoggingMode.ERROR, LOG_TAG, "Failed to sync push token, token is null or empty.");
            return;
        }

        Map<String, Object> eventData = getProfileEventData(pushToken, messagingState.getEcid());
        if (eventData == null) {
            return;
        }

        // Update the push token to the shared state
        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Log.warning(MessagingConstants.LOG_TAG, String.format("An error occurred while setting the push token in the shared state %s",
                        extensionError.getErrorName()));
            }
        };
        final HashMap<String, Object> map = new HashMap<>();
        map.put(MessagingConstants.SharedState.Messaging.PUSH_IDENTIFIER, pushToken);
        getApi().setSharedEventState(map, event, errorCallback);

        // Send an edge event with profile data as event data
        final Event profileEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_PUSH_PROFILE_EDGE_EVENT, MessagingConstants.EventType.EDGE, EventSource.REQUEST_CONTENT.getName())
                .setEventData(eventData)
                .build();
        MobileCore.dispatchEvent(profileEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.error(LOG_TAG, "%s - Error in dispatching event for updating the push profile details", SELF_TAG);
            }
        });
    }

    public void handleTrackingInfo(final Event event) {
        final EventData eventData = event.getData();
        if (eventData == null) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "%s - handleTrackingInfo - Cannot track information, eventData is null.", SELF_TAG);
            return;
        }
        final String eventType = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
        final String messageId = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
        final boolean isApplicationOpened = eventData.optBoolean(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, false);
        final String actionId = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, null);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(MessagingConstants.LOG_TAG,
                    "%s - handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.", SELF_TAG);
            return;
        }

        final String datasetId = messagingState.getExperienceEventDatasetId();
        if (datasetId == null || datasetId.isEmpty()) {
            Log.warning(LOG_TAG, "%s - Unable to record a message interaction, configuration information is not available.", SELF_TAG);
            return;
        }

        // Creating the Meta Map
        final Map<String, Object> metaMap = new HashMap<>();
        final Map<String, Object> collectMap = new HashMap<>();
        collectMap.put(DATASET_ID, datasetId);
        metaMap.put(COLLECT, collectMap);

        // Create XDM data with tracking data
        final Map<String, Object> xdmMap = getXdmData(eventType, messageId, actionId);

        // Adding application data to xdmMap
        addApplicationData(isApplicationOpened, xdmMap);

        // Adding xdm data to xdmMap
        addXDMData(eventData, xdmMap);

        final Map<String, Object> xdmData = new HashMap<>();
        xdmData.put(XDM, xdmMap);
        xdmData.put(META, metaMap);

        final Event trackEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_PUSH_TRACKING_EDGE_EVENT, MessagingConstants.EventType.EDGE, EventSource.REQUEST_CONTENT.getName(), null)
                .setEventData(xdmData)
                .build();

        MobileCore.dispatchEvent(trackEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.error(LOG_TAG, "%s - Error in dispatching event for tracking", SELF_TAG);
            }
        });
    }

    public void handleInAppTrackingInfo(final Event event) {
        final String datasetId = messagingState.getExperienceEventDatasetId();
        if (StringUtils.isNullOrEmpty(datasetId)) {
            Log.warning(LOG_TAG, "%s - Unable to record an in-app message interaction, configuration information is not available.", SELF_TAG);
            return;
        }

        final EventData eventData = event.getData();
        // TODO: event history POC, try to retrieve the amount of events which match the current triggered tracking event ===========
        final String eventType = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
        final String trackingType = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, null);
        final String messageId = eventData.optString(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, null);
//
//        if (trackingType.equals("triggered") || trackingType.equals("displayed") || trackingType.equals("dismissed")
//                || trackingType.equals("closed")) {
//            //messageCount++;
//            final EventHistoryRequest[] requests = new EventHistoryRequest[messageCount];
//            for (int i = 0; i < messageCount ; i++) {
//                // create request
//                final HashMap<String, Variant> eventDataMask = new HashMap<>();
//                eventDataMask.put("xdm.eventType", Variant.fromString(eventType));
//                eventDataMask.put("xdm._experience.customerJourneyManagement.messageExecution.messageExecutionID", Variant.fromString(messageId));
//                eventDataMask.put("xdm.inappMessageTracking.action", Variant.fromString(trackingType));
//                final EventHistoryRequest request = new EventHistoryRequest(eventDataMask, 0, System.currentTimeMillis());
//                requests[i] = request;
//            }
//            if (messageCount+1 != 0) {
//                // create handler
//                final EventHistoryResultHandler<ArrayList<DatabaseService.QueryResult>> resultHandler = new EventHistoryResultHandler<ArrayList<DatabaseService.QueryResult>>() {
//                    @Override
//                    public void call(final ArrayList<DatabaseService.QueryResult> results) {
//                        try {
//                            for (DatabaseService.QueryResult result: results) {
//                                result.moveToFirst();
//                                int count = result.getInt(0);
//                                long oldest = result.getLong(1);
//                                long newest = result.getLong(2);
//                                Log.debug("handleInAppTrackingInfo", "Message Id: %s, %s tracking event matched %s times in " +
//                                        "the event history database. " +
//                                        "oldest match found at %s, " +
//                                        "newest match found at %s.", messageCount, trackingType, count, oldest, newest);
//                                result.close();
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                };
//                androidEventHistory.getEvents(requests, true, resultHandler);
//            }
//        }
        // TODO: ==============================================================================================================

        // Create XDM data with tracking data
        final Map<String, Object> xdmMap = new HashMap<>();
        final Map<String, Object> experienceMap = new HashMap<>();
        final Map<String, Object> messageExecutionMap = new HashMap<>();
        final Map<String, Object> cjmMap = new HashMap<>();
        messageExecutionMap.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION_ID, messageId);
        cjmMap.put(MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_EXECUTION, messageExecutionMap);
        experienceMap.put(MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT, cjmMap);
        xdmMap.put(XDMDataKeys.XDM_DATA_EVENT_TYPE, eventType);
        xdmMap.put(MessagingConstants.TrackingKeys.EXPERIENCE, experienceMap);

        // add iam mixin information if this is an interact eventType
        if (eventType.equals(MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.EventType.INTERACT) && !StringUtils.isNullOrEmpty(trackingType)) {
            final Map<String, Object> actionMap = new HashMap<>();
            actionMap.put(XDMDataKeys.ACTION, trackingType);
            xdmMap.put(XDMDataKeys.XDM_DATA_IN_APP_NOTIFICATION_TRACKING, actionMap);
        }

        // Creating the Meta Map
        final Map<String, Object> metaMap = new HashMap<>();
        final Map<String, Object> collectMap = new HashMap<>();
        collectMap.put(DATASET_ID, datasetId);
        metaMap.put(COLLECT, collectMap);

        // Adding xdm data to xdmMap
        addXDMData(eventData, xdmMap);

        final Map<String, Object> xdmData = new HashMap<>();
        xdmData.put(XDM, xdmMap);
        xdmData.put(META, metaMap);

        // TODO: event history POC, use message id and custom action (e.g. "triggered") as a mask =============
        final String[] mask = {"xdm.eventType",
                "xdm._experience.customerJourneyManagement.messageExecution.messageExecutionID",
                "xdm.inappMessageTracking.action"};

        final Event trackEvent = new Event.Builder(MessagingConstants.EventName.MESSAGING_IAM_TRACKING_EDGE_EVENT, MessagingConstants.EventType.EDGE, EventSource.REQUEST_CONTENT.getName(), mask)
                .setEventData(xdmData)
                .build();

        MobileCore.dispatchEvent(trackEvent, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                Log.error(LOG_TAG, "%s - Error in dispatching event for tracking", SELF_TAG);
            }
        });
    }

    public void processHubSharedState(Event event) {
        if (isSharedStateUpdateFor(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event) ||
                isSharedStateUpdateFor(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
            processEvents();
        }
    }

    // ========================================================================================
    // Getters for private members
    // ========================================================================================

    /**
     * Getter for the {@link #executorService}. Access to which is mutex protected.
     *
     * @return A non-null {@link ExecutorService} instance
     */
    ExecutorService getExecutor() {
        synchronized (executorMutex) {
            if (executorService == null) {
                executorService = Executors.newSingleThreadExecutor();
            }

            return executorService;
        }
    }

    /**
     * Getter for the {@link #eventQueue}.
     *
     * @return A non-null {@link ConcurrentLinkedQueue} instance
     */
    ConcurrentLinkedQueue<Event> getEventQueue() {
        return eventQueue;
    }

    /**
     * Checks if the provided {@code event} is a shared state update event for {@code stateOwnerName}
     *
     * @param stateOwnerName the shared state owner name; should not be null
     * @param event          current event to check; should not be null
     * @return {@code boolean} indicating if it is the shared state update for the provided {@code stateOwnerName}
     */
    private boolean isSharedStateUpdateFor(final String stateOwnerName, final Event event) {
        if (stateOwnerName == null || stateOwnerName.isEmpty() || event == null) {
            return false;
        }

        String stateOwner;

        try {
            stateOwner = (String) event.getEventData().get(MessagingConstants.EventDataKeys.STATE_OWNER);
        } catch (ClassCastException e) {
            return false;
        }

        return stateOwnerName.equals(stateOwner);
    }
}