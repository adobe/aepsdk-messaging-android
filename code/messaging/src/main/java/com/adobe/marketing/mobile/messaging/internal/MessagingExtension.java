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

import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EXTENSION_NAME;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EXTENSION_VERSION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.DECISIONING;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.LABEL;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITION_ACTION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SCOPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.SCOPE_DETAILS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.APP_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.CODE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DATA;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.DENY_LISTED;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.IDENTITY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.NAMESPACE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PLATFORM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.PUSH_NOTIFICATION_DETAILS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.TOKEN;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.FRIENDLY_EXTENSION_NAME;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.JsonValues.ECID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.JsonValues.FCM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.LOG_TAG;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.CJM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.COLLECT;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.CUSTOMER_JOURNEY_MANAGEMENT;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.DATASET_ID;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.EXPERIENCE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.MESSAGE_PROFILE_JSON;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.META;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.MIXINS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.TrackingKeys.XDM;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITION_EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.IAMDetailsDataKeys.Key.PROPOSITIONS;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.IAM_HISTORY;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventMask.Keys.EVENT_TYPE;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventMask.Keys.TRACKING_ACTION;
import static com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventMask.Keys.MESSAGE_ID;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.adobe.marketing.mobile.launch.rulesengine.LaunchRule;
import com.adobe.marketing.mobile.launch.rulesengine.LaunchRulesEngine;
import com.adobe.marketing.mobile.launch.rulesengine.RuleConsequence;
import com.adobe.marketing.mobile.messaging.internal.MessagingConstants.EventDataKeys.Messaging.XDMDataKeys;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.*;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.EventSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessagingExtension extends Extension {
    private final static String SELF_TAG = "MessagingExtension";

    final EdgePersonalizationResponseHandler edgePersonalizationResponseHandler;
    private boolean initialMessageFetchComplete = false;
    final LaunchRulesEngine messagingRulesEngine;

    /**
     * Constructor.
     *
     * <p>
     * Called during messaging extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *      <li> Listening to event with eventType {@link EventType#GENERIC_IDENTITY}
     * 	        and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *     <li> Listening to event with eventType {@link MessagingConstants.EventType#MESSAGING}
     *          and EventSource {@link EventSource#REQUEST_CONTENT}</li>
     *      <li> Listening to event with eventType {@link MessagingConstants.EventType#EDGE}
     * 	        and EventSource {@link MessagingConstants.EventSource#PERSONALIZATION_DECISIONS}</li>
     * 	    <li> Listening to event with eventType {@link EventType#WILDCARD}
     *          and EventSource {@link EventSource#WILDCARD}</li>
     * </ul>
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    MessagingExtension(final ExtensionApi extensionApi) {
        this(extensionApi, null, null);
    }

    @VisibleForTesting
    MessagingExtension(final ExtensionApi extensionApi, final LaunchRulesEngine messagingRulesEngine, final EdgePersonalizationResponseHandler edgePersonalizationResponseHandler) {
        super(extensionApi);
        this.messagingRulesEngine = messagingRulesEngine != null ? messagingRulesEngine : new LaunchRulesEngine(extensionApi);
        this.edgePersonalizationResponseHandler = edgePersonalizationResponseHandler != null ? edgePersonalizationResponseHandler : new EdgePersonalizationResponseHandler(this, extensionApi, this.messagingRulesEngine);
    }

    //region Extension interface methods

    /**
     * Overridden method of {@link Extension} class to provide a valid extension name to register with eventHub.
     *
     * @return A {@link String} extension name for Messaging
     */
    @NonNull
    @Override
    protected String getName() {
        return EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide a friendly extension name.
     *
     * @return A {@link String} friendly extension name for Messaging
     */
    @NonNull
    @Override
    protected String getFriendlyName() {
        return FRIENDLY_EXTENSION_NAME;
    }

    /**
     * Overridden method of {@link Extension} class to provide the extension version.
     *
     * @return A {@link String} representing the extension version
     */
    @NonNull
    @Override
    protected String getVersion() {
        return EXTENSION_VERSION;
    }

    @Override
    protected void onRegistered() {
        super.onRegistered();
        getApi().registerEventListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, this::processEvent);
        getApi().registerEventListener(MessagingConstants.EventType.MESSAGING, EventSource.REQUEST_CONTENT, this::processEvent);
        getApi().registerEventListener(EventType.EDGE, MessagingConstants.EventSource.PERSONALIZATION_DECISIONS, this::processEvent);
        getApi().registerEventListener(EventType.WILDCARD, EventSource.WILDCARD, this::handleWildcardEvents);
    }

    @Override
    protected void onUnregistered() {
    }

    @Override
    public boolean readyForEvent(@NonNull final Event event) {
        if (!hasValidSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, event)) {
            Log.trace(LOG_TAG, SELF_TAG, "Event processing is paused - waiting for valid Configuration");
            return false;
        }

        if (!hasValidXdmSharedState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event)) {
            Log.trace(LOG_TAG, SELF_TAG, "Event processing is paused - waiting for valid XDM shared state from Edge Identity extension.");
            return false;
        }

        // fetch in-app messages on initial launch once we have configuration and identity state set
        if (!initialMessageFetchComplete) {
            edgePersonalizationResponseHandler.fetchMessages(null);
            initialMessageFetchComplete = true;
        }

        return true;
    }

    //endregion

    //region Event listeners

    void handleWildcardEvents(final Event event) {
        // handling mock rules delivered from the assurance ui
        final String eventName = event.getName();
        if (!StringUtils.isNullOrEmpty(eventName) && eventName.equals(MessagingConstants.EventName.ASSURANCE_SPOOFED_IAM_EVENT_NAME)) {
            final Map<String, Object> triggeredConsequenceMap = DataReader.optTypedMap(Object.class, event.getEventData(), MessagingConstants.EventDataKeys.RulesEngine.CONSEQUENCE_TRIGGERED, null);
            if (!MapUtils.isNullOrEmpty(triggeredConsequenceMap)) {
                final String id = DataReader.optString(triggeredConsequenceMap, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_ID, "");
                final String type = DataReader.optString(triggeredConsequenceMap, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_TYPE, "");
                final Map detail = DataReader.optTypedMap(Object.class, triggeredConsequenceMap, MessagingConstants.EventDataKeys.RulesEngine.MESSAGE_CONSEQUENCE_DETAIL, null);
                edgePersonalizationResponseHandler.createInAppMessage(new RuleConsequence(id, type, detail));
            }
            return;
        }

        List<LaunchRule> triggeredRules = messagingRulesEngine.process(event);
        final List<RuleConsequence> consequences = new ArrayList<>();

        if (triggeredRules == null || triggeredRules.isEmpty()) {
            return;
        }

        for (final LaunchRule rule : triggeredRules) {
            consequences.addAll(rule.getConsequenceList());
        }

        if (consequences.isEmpty()) {
            return;
        }

        edgePersonalizationResponseHandler.createInAppMessage(consequences.get(0));
    }

    //endregion

    //region package-protected methods

    /**
     * Validates the passed in event and triggers additional processing based on the event type.
     *
     * @param eventToProcess an {@link Event} from an {@link ExtensionEventListener} to be processed
     */
    void processEvent(final Event eventToProcess) {
        if (!eventIsValid(eventToProcess)) {
            Log.debug(LOG_TAG, SELF_TAG, "Event or EventData is null, ignoring the event.");
            return;
        }

        // validate refresh messages event then fetch in-app messages via an Edge extension event
        if (MessagingUtils.isRefreshMessagesEvent(eventToProcess)) {
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Processing manual request to refresh In-App Message definitions from the remote.");
            edgePersonalizationResponseHandler.fetchMessages(null);
        } else if (MessagingUtils.isUpdateFeedsEvent(eventToProcess)) {
            // validate update feeds event then retrieve feeds via an Edge extension event
            Log.debug(MessagingConstants.LOG_TAG, SELF_TAG, "Processing request to update message feed definitions from the remote.");
            edgePersonalizationResponseHandler.fetchMessages(MessagingUtils.getSurfaces(eventToProcess));
        } else if (MessagingUtils.isGenericIdentityRequestEvent(eventToProcess)) {
            // handle the push token from generic identity request content event
            handlePushToken(eventToProcess);
        } else if (MessagingUtils.isMessagingRequestContentEvent(eventToProcess)) {
            // need experience event dataset id for sending the push token
            final Map<String, Object> configSharedState = getSharedState(MessagingConstants.SharedState.Configuration.EXTENSION_NAME, eventToProcess);
            final String experienceEventDatasetId = DataReader.optString(configSharedState, MessagingConstants.SharedState.Configuration.EXPERIENCE_EVENT_DATASET_ID, "");
            if (StringUtils.isNullOrEmpty(experienceEventDatasetId)) {
                Log.warning(LOG_TAG, SELF_TAG, "Unable to track push notification interaction, experience event dataset id is empty. Check the messaging launch extension to add the experience event dataset.");
                return;
            }
            // handle the push tracking information from messaging request content event
            handleTrackingInfo(eventToProcess, experienceEventDatasetId);
        } else if (MessagingUtils.isEdgePersonalizationDecisionEvent(eventToProcess)) {
            // validate the edge response event then load any iam rules present
            edgePersonalizationResponseHandler.handleEdgePersonalizationNotification(eventToProcess);
        }
    }

    void handlePushToken(final Event event) {
        final String pushToken = DataReader.optString(event.getEventData(), MessagingConstants.EventDataKeys.Identity.PUSH_IDENTIFIER, null);

        if (StringUtils.isNullOrEmpty(pushToken)) {
            Log.debug(LOG_TAG, SELF_TAG, "Failed to sync push token, token is null or empty.");
            return;
        }

        final Map<String, Object> edgeIdentitySharedState = getXDMSharedState(MessagingConstants.SharedState.EdgeIdentity.EXTENSION_NAME, event);
        final String ecid = MessagingUtils.getSharedStateEcid(edgeIdentitySharedState);
        if (StringUtils.isNullOrEmpty(ecid)) {
            Log.debug(LOG_TAG, SELF_TAG, "Unable to sync the push token. ECID is unavailable for the user.");
            return;
        }

        final Map<String, Object> eventData = getProfileEventData(pushToken, ecid);
        if (eventData == null) {
            return;
        }

        // Update the push token to the shared state
        final HashMap<String, Object> messagingSharedState = new HashMap<>();
        messagingSharedState.put(MessagingConstants.SharedState.Messaging.PUSH_IDENTIFIER, pushToken);
        getApi().createSharedState(messagingSharedState, event);

        // Send an edge event with profile data as event data
        MessagingUtils.sendEvent(MessagingConstants.EventName.PUSH_PROFILE_EDGE_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                eventData,
                getApi());
    }

    void handleTrackingInfo(final Event event, final String datasetId) {
        final Map<String, Object> eventData = event.getEventData();
        if (eventData == null) {
            Log.debug(LOG_TAG, SELF_TAG, "handleTrackingInfo - Cannot track information, eventData is null.");
            return;
        }
        final String eventType = DataReader.optString(eventData, MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, "");
        final String messageId = DataReader.optString(eventData, MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, "");
        final boolean isApplicationOpened = DataReader.optBoolean(eventData, MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, false);
        final String actionId = DataReader.optString(eventData, MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, null);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(LOG_TAG, SELF_TAG, "handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.");
            return;
        }

        if (StringUtils.isNullOrEmpty(datasetId)) {
            Log.warning(LOG_TAG, SELF_TAG, "Unable to record a message interaction, configuration information is not available.");
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

        // dispatch push tracking event
        MessagingUtils.sendEvent(MessagingConstants.EventName.PUSH_TRACKING_EDGE_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                xdmData,
                getApi());
    }

    /**
     * Sends a proposition interaction to the customer's experience event dataset.
     *
     * @param interaction {@code String} containing the interaction which occurred
     * @param eventType   {@link MessagingEdgeEventType} enum containing the {@link EventType} to be used for the ensuing Edge Event
     * @param message     The {@link InternalMessage} which triggered the proposition interaction
     */
    public void sendPropositionInteraction(final String interaction, final MessagingEdgeEventType eventType, final InternalMessage message) {
        final PropositionInfo propositionInfo = message.propositionInfo;
        if (propositionInfo == null || MapUtils.isNullOrEmpty(propositionInfo.scopeDetails)) {
            Log.trace(LOG_TAG, MessagingExtension.SELF_TAG, "Unable to record an in-app message interaction, the scope details were not found for this message.");
            return;
        }
        final List<Map<String, Object>> propositions = new ArrayList<>();
        final Map<String, Object> proposition = new HashMap<>();
        proposition.put(ID, propositionInfo.id);
        proposition.put(SCOPE, propositionInfo.scope);
        proposition.put(SCOPE_DETAILS, propositionInfo.scopeDetails);
        propositions.add(proposition);

        final Map<String, Integer> propositionEventType = new HashMap<>();
        propositionEventType.put(eventType.getPropositionEventType(), 1);

        final Map<String, Object> decisioning = new HashMap<>();
        decisioning.put(PROPOSITION_EVENT_TYPE, propositionEventType);
        decisioning.put(PROPOSITIONS, propositions);

        // add propositionAction if this is an interact eventType
        if (eventType.equals(MessagingEdgeEventType.IN_APP_INTERACT)) {
            final Map<String, String> propositionAction = new HashMap<>();
            propositionAction.put(ID, interaction);
            propositionAction.put(LABEL, interaction);
            decisioning.put(PROPOSITION_ACTION, propositionAction);
        }

        // create experience map with proposition tracking data
        final Map<String, Object> experienceMap = new HashMap<>();
        experienceMap.put(DECISIONING, decisioning);

        // create XDM data with experience data
        final Map<String, Object> xdmMap = new HashMap<>();
        xdmMap.put(XDMDataKeys.EVENT_TYPE, eventType.toString());
        xdmMap.put(MessagingConstants.TrackingKeys.EXPERIENCE, experienceMap);

        // create maps for event history
        final Map<String, String> iamHistoryMap = new HashMap<>();
        iamHistoryMap.put(EVENT_TYPE, eventType.getPropositionEventType());
        iamHistoryMap.put(MESSAGE_ID, propositionInfo.activityId);
        iamHistoryMap.put(TRACKING_ACTION, (StringUtils.isNullOrEmpty(interaction) ? "" : interaction));

        // Create the mask for storing event history
        final String[] mask = {MessagingConstants.EventMask.Mask.EVENT_TYPE, MessagingConstants.EventMask.Mask.MESSAGE_ID, MessagingConstants.EventMask.Mask.TRACKING_ACTION};

        final Map<String, Object> xdmEventData = new HashMap<>();
        xdmEventData.put(XDM, xdmMap);
        xdmEventData.put(IAM_HISTORY, iamHistoryMap);

        // dispatch in-app tracking event
        MessagingUtils.sendEvent(MessagingConstants.EventName.MESSAGE_INTERACTION_EVENT,
                MessagingConstants.EventType.EDGE,
                MessagingConstants.EventSource.REQUEST_CONTENT,
                xdmEventData,
                mask,
                getApi());
    }
    //endregion

    //region private methods

    /**
     * Get profile data with token
     *
     * @param token push token which needs to be synced
     * @param ecid  experience cloud id of the device
     * @return {@link Map} of profile data in the correct format with token
     */
    private static Map<String, Object> getProfileEventData(final String token, final String ecid) {
        if (ecid == null) {
            Log.error(LOG_TAG, MessagingExtension.SELF_TAG, "Failed to sync push token, ECID is null.");
            return null;
        }

        final Map<String, String> namespace = new HashMap<>();
        namespace.put(CODE, ECID);

        final Map<String, Object> identity = new HashMap<>();
        identity.put(NAMESPACE, namespace);
        identity.put(MessagingConstants.EventDataKeys.Messaging.PushNotificationDetailsDataKeys.ID, ecid);

        final ArrayList<Map<String, Object>> pushNotificationDetailsArray = new ArrayList<>();
        final Map<String, Object> pushNotificationDetailsData = new HashMap<>();
        pushNotificationDetailsData.put(IDENTITY, identity);
        pushNotificationDetailsData.put(APP_ID, ServiceProvider.getInstance().getDeviceInfoService().getApplicationPackageName());
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
            customActionMap.put(XDMDataKeys.ACTION_ID, actionId);
            trackingMap.put(XDMDataKeys.CUSTOM_ACTION, customActionMap);
        }

        trackingMap.put(XDMDataKeys.PUSH_PROVIDER, FCM);
        trackingMap.put(XDMDataKeys.PUSH_PROVIDER_MESSAGE_ID, messageId);
        xdmMap.put(XDMDataKeys.EVENT_TYPE, eventType);
        xdmMap.put(XDMDataKeys.PUSH_NOTIFICATION_TRACKING_MIXIN_NAME, trackingMap);

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
     * @param eventData eventData map which contains the xdm data forwarded by the customer.
     * @param xdmMap    xdmMap map which is updated.
     */
    private static void addXDMData(final Map<String, Object> eventData, final Map<String, Object> xdmMap) {
        // Extract the xdm adobe data string from the event data.
        final String adobe = DataReader.optString(eventData, MessagingConstants.EventDataKeys.Messaging.TRACK_INFO_KEY_ADOBE_XDM, "");
        if (StringUtils.isNullOrEmpty(adobe)) {
            Log.warning(LOG_TAG, SELF_TAG, "Failed to send Adobe data with the tracking data, Adobe XDM data is null.");
            return;
        }

        try {
            // Convert the adobe string to json object
            final JSONObject xdmJson = new JSONObject(adobe);
            final Map<String, Object> xdmMapObject = JSONUtils.toMap(xdmJson);

            if (xdmMapObject == null) {
                Log.warning(LOG_TAG, SELF_TAG, "Failed to send Adobe data with the tracking data, Adobe XDM data conversion to map failed.");
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
                Log.debug(LOG_TAG, SELF_TAG, "Failed to send cjm xdm data with the tracking, Missing XDM data.");
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
                    cjm.putAll(JSONUtils.toMap(jObject));

                    experience.put(CUSTOMER_JOURNEY_MANAGEMENT, cjm);
                    xdmMap.put(EXPERIENCE, experience);
                }
            } else {
                Log.warning(LOG_TAG, SELF_TAG, "Failed to send CJM XDM data with the tracking, required keys are missing.");
            }
        } catch (final JSONException | ClassCastException e) {
            Log.warning(LOG_TAG, SELF_TAG, "Failed to send Adobe data with the tracking data, Adobe data is malformed : %s", e.getMessage());
        }
    }

    private boolean hasValidSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        if (result == null) {
            return false;
        }
        final Map<String, Object> sharedState = result.getValue();
        return sharedState != null && !sharedState.isEmpty();
    }

    private boolean hasValidXdmSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getXDMSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        if (result == null) {
            return false;
        }
        final Map<String, Object> sharedState = result.getValue();
        return sharedState != null && !sharedState.isEmpty();
    }

    private Map<String, Object> getSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        return result == null ? null : result.getValue();
    }

    private Map<String, Object> getXDMSharedState(final String extensionName, final Event event) {
        final SharedStateResult result = getApi().getXDMSharedState(extensionName, event, false, SharedStateResolution.LAST_SET);
        return result == null ? null : result.getValue();
    }

    private boolean eventIsValid(final Event event) {
        return event != null && event.getEventData() != null;
    }

    //endregion
}