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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adobe.marketing.mobile.messaging.MessagingExtension;
import com.adobe.marketing.mobile.messaging.MessagingUtils;
import com.adobe.marketing.mobile.messaging.Proposition;
import com.adobe.marketing.mobile.messaging.Surface;
import com.adobe.marketing.mobile.messaging.PushTrackingStatus;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.DataReaderException;
import com.adobe.marketing.mobile.util.MapUtils;
import com.adobe.marketing.mobile.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Messaging {
    private static final String EXTENSION_VERSION = "2.3.0";
    private static final String LOG_TAG = "Messaging";
    private static final String CLASS_NAME = "Messaging";

    private static final String EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED = "pushTracking.applicationOpened";
    private static final String EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION = "pushTracking.customAction";
    private static final String EVENT_SOURCE_NOTIFICATION = "com.adobe.eventSource.notification";
    private static final String PUSH_NOTIFICATION_INTERACTION_EVENT = "Push notification interaction event";
    private static final String UPDATE_PROPOSITIONS = "Update propositions";
    private static final String GET_PROPOSITIONS = "Get propositions";
    private static final String REFRESH_MESSAGES = "Refresh in-app messages";
    private static final long TIMEOUT_MILLIS = 5000L;
    private static final long GET_PROPOSITIONS_TIMEOUT_MILLIS = 10000L;
    private static final String TRACK_INFO_KEY_ACTION_ID = "actionId";
    private static final String TRACK_INFO_KEY_ADOBE_XDM = "adobe_xdm";
    private static final String TRACK_INFO_KEY_APPLICATION_OPENED = "applicationOpened";
    private static final String TRACK_INFO_KEY_EVENT_TYPE = "eventType";
    private static final String TRACK_INFO_KEY_GOOGLE_MESSAGE_ID = "google.message_id";
    private static final String TRACK_INFO_KEY_MESSAGE_ID = "messageId";
    private static final String PUSH_NOTIFICATION_TRACKING_STATUS = "pushTrackingStatus";
    private static final String _XDM = "_xdm";
    private static final String SURFACES = "surfaces";
    private static final String PROPOSITIONS = "propositions";
    private static final String UPDATE_PROPOSITIONS_EVENT = "updatepropositions";
    private static final String GET_PROPOSITIONS_EVENT = "getpropositions";
    private static final String REFRESH_MESSAGES_EVENT = "refreshmessages";
    private static final String RESPONSE_ERROR = "responseerror";
    private static final String SCOPE = "scope";

    public static final Class<? extends Extension> EXTENSION = MessagingExtension.class;
    private static boolean isPropositionsResponseListenerRegistered = false;
    private static AdobeCallback<Map<Surface, List<Proposition>>> propositionsResponseHandler;

    private Messaging() {
    }

    /**
     * Returns the current version of the Messaging extension.
     *
     * @return A {@link String} representing the Messaging extension version
     */
    @NonNull
    public static String extensionVersion() {
        return EXTENSION_VERSION;
    }

    /**
     * Registers the Messaging extension with the {@code MobileCore}.
     * <p>
     * This will allow the extension to send and receive events to and from the SDK.
     */
    @Deprecated
    public static void registerExtension() {
        MobileCore.registerExtension(MessagingExtension.class, extensionError -> {
            if (extensionError == null) {
                return;
            }
            Log.error(LOG_TAG, CLASS_NAME, "There was an error registering Messaging Extension: %s", extensionError.getErrorName());
        });
    }

    /**
     * Extracts and update the intent with xdm data and message id from data payload.
     * <p>
     * This method needs to be called with the intent before the notification is created.
     *
     * @param intent    Intent which needs to be updated with xdm data and messageId
     * @param messageId String : message id from RemoteMessage which is received in FirebaseMessagingService#onMessageReceived
     * @param data      Map which represents the data part of the remoteMessage which is received in FirebaseMessagingService#onMessageReceived
     * @return boolean value indicating whether the intent was update with push tracking details (messageId and xdm data).
     */
    public static boolean addPushTrackingDetails(@NonNull final Intent intent, @NonNull final String messageId, final @NonNull Map<String, String> data) {
        if (intent == null) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to add push tracking details as intent is null.");
            return false;
        }
        if (StringUtils.isNullOrEmpty(messageId)) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to add push tracking details as MessageId is null.");
            return false;
        }
        if (MapUtils.isNullOrEmpty(data)) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to add push tracking details as data is null or empty.");
            return false;
        }

        // Adding message id as extras in intent
        intent.putExtra(TRACK_INFO_KEY_MESSAGE_ID, messageId);

        // Adding xdm data as extras in intent. If the xdm key is not present just log a warning
        final String xdmData = data.get(_XDM);
        if (xdmData != null && !xdmData.isEmpty()) {
            intent.putExtra(TRACK_INFO_KEY_ADOBE_XDM, xdmData);
        } else {
            Log.warning(LOG_TAG, CLASS_NAME, "XDM data is not added as push tracking details to the intent, XDM data is null or empty");
        }

        return true;
    }

    /**
     * Sends the push notification interactions as an experience event to Adobe Experience Edge.
     *
     * @param intent            object which contains the tracking and xdm information.
     * @param applicationOpened Boolean values denoting whether the application was opened when notification was clicked
     * @param customActionId    String value of the custom action (e.g button id on the notification) which was clicked.
     */
    public static void handleNotificationResponse(@NonNull final Intent intent,
                                                     final boolean applicationOpened,
                                                     @Nullable final String customActionId) {
        Messaging.handleNotificationResponse(intent,applicationOpened,customActionId,null);
    }

    /**
     * Sends the push notification interactions as an experience event to Adobe Experience Edge.
     *
     * @param intent            object which contains the tracking and xdm information.
     * @param applicationOpened Boolean values denoting whether the application was opened when notification was clicked
     * @param customActionId    String value of the custom action (e.g button id on the notification) which was clicked.
     * @param callback          Callback which will be invoked with the status of push notification tracking.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static void handleNotificationResponse(@NonNull final Intent intent,
                                                  final boolean applicationOpened,
                                                  @Nullable final String customActionId,
                                                  @Nullable final AdobeCallback<PushTrackingStatus> callback) {
        if (intent == null) {
            Log.warning(LOG_TAG, CLASS_NAME, "Failed to track notification interactions, intent provided is null");
            callTrackingCallback(PushTrackingStatus.INVALID_INTENT, callback);
            return;
        }
        String messageId = intent.getStringExtra(TRACK_INFO_KEY_MESSAGE_ID);
        if (StringUtils.isNullOrEmpty(messageId)) {
            // Check if the message Id is in the intent with the key TRACK_INFO_KEY_GOOGLE_MESSAGE_ID which comes through google directly
            // This happens when FirebaseMessagingService#onMessageReceived is not called.
            messageId = intent.getStringExtra(TRACK_INFO_KEY_GOOGLE_MESSAGE_ID);
            if (StringUtils.isNullOrEmpty(messageId)) {
                Log.warning(LOG_TAG, CLASS_NAME, "Failed to track notification interactions, message id provided is null");
                callTrackingCallback(PushTrackingStatus.INVALID_MESSAGE_ID, callback);
                return;
            }
        }

        final String xdmData = intent.getStringExtra(TRACK_INFO_KEY_ADOBE_XDM);
        if (StringUtils.isNullOrEmpty(xdmData)) {
            Log.warning(LOG_TAG, CLASS_NAME, "No tracking data found in the intent, Ignoring to track AJO notification interactions.");
            callTrackingCallback(PushTrackingStatus.NO_TRACKING_DATA, callback);
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(TRACK_INFO_KEY_MESSAGE_ID, messageId);
        eventData.put(TRACK_INFO_KEY_APPLICATION_OPENED, applicationOpened);
        eventData.put(TRACK_INFO_KEY_ADOBE_XDM, xdmData);

        if (StringUtils.isNullOrEmpty(customActionId)) {
            eventData.put(TRACK_INFO_KEY_EVENT_TYPE, EVENT_TYPE_PUSH_TRACKING_APPLICATION_OPENED);
        } else {
            eventData.put(TRACK_INFO_KEY_ACTION_ID, customActionId);
            eventData.put(TRACK_INFO_KEY_EVENT_TYPE, EVENT_TYPE_PUSH_TRACKING_CUSTOM_ACTION);
        }

        final Event messagingEvent = new Event.Builder(PUSH_NOTIFICATION_INTERACTION_EVENT,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEventWithResponseCallback(messagingEvent, TIMEOUT_MILLIS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                callTrackingCallback(PushTrackingStatus.UNKNOWN_ERROR,callback);
            }

            @Override
            public void call(final Event event) {
                final Map<String,Object> responseEventData = event.getEventData();

                if (responseEventData == null) {
                    callTrackingCallback(PushTrackingStatus.UNKNOWN_ERROR,callback);
                }

                try {
                    final int resultStatusInteger = DataReader.getInt(responseEventData, PUSH_NOTIFICATION_TRACKING_STATUS);
                    final PushTrackingStatus status = PushTrackingStatus.fromInt(resultStatusInteger);
                    callTrackingCallback(status,callback);

                } catch (final DataReaderException e) {
                    callTrackingCallback(PushTrackingStatus.UNKNOWN_ERROR,callback);
                }
            }
        });
    }

    /**
     * Initiates a network call to retrieve remote In-App Message definitions from Adobe Journey Optimizer.
     */
    public static void refreshInAppMessages() {
        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(REFRESH_MESSAGES_EVENT, true);

        final Event refreshMessageEvent = new Event.Builder(REFRESH_MESSAGES,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(refreshMessageEvent);
    }

    /**
     * Dispatches an event to retrieve the previously fetched (and cached) feeds content from the SDK for the provided surfaces.
     * If the feeds content for one or more surfaces isn't previously cached in the SDK, it will not be retrieved from Adobe Journey Optimizer via the Experience Edge network.
     *
     * @param surfaces A {@link List<Surface>} containing {@link Surface}s to be used for retrieving previously fetched propositions
     * @param callback A {@link AdobeCallback} which will be invoked with a {@link Map<Surface, List<  Proposition  >>} containing previously fetched feeds content
     */
    public static void getPropositionsForSurfaces(@NonNull final List<Surface> surfaces, @NonNull final AdobeCallback<Map<Surface, List<Proposition>>> callback) {
        if (callback == null ) {
            Log.warning(LOG_TAG, CLASS_NAME, "Cannot get propositions as the provided callback is null.");
            return;
        }

        if (surfaces == null || surfaces.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME, "Cannot get propositions as the provided list of surfaces is null or empty.");
            return;
        }

        final List<Map<String, Object>> validSurfacesFlattened = new ArrayList<>();
        for (final Surface surface : surfaces) {
            if (surface.isValid()) {
                validSurfacesFlattened.add(surface.toEventData());
            }
        }

        if (validSurfacesFlattened.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME, "Cannot get propositions as the provided list of surfaces has no valid items.");
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(GET_PROPOSITIONS_EVENT, true);
        eventData.put(SURFACES, validSurfacesFlattened);

        final Event getPropositionsEvent = new Event.Builder(GET_PROPOSITIONS,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEventWithResponseCallback(getPropositionsEvent, GET_PROPOSITIONS_TIMEOUT_MILLIS, new AdobeCallbackWithError<Event>() {
            @Override
            public void fail(final AdobeError adobeError) {
                failWithError(callback, adobeError);
            }

            @Override
            public void call(final Event event) {
                try {
                    final Map<String, Object> eventData = event.getEventData();
                    if (MapUtils.isNullOrEmpty(eventData)) {
                        failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                        return;
                    }

                    if (eventData.containsKey(RESPONSE_ERROR)) {
                        final int errorCode = DataReader.getInt(eventData, RESPONSE_ERROR);
                        failWithError(callback, convertToAdobeError(errorCode));
                        return;
                    }

                    Map<Surface, List<Proposition>> requestedPropositionsMap = new HashMap<>();
                    final List<Map<String, Object>> retrievedPropositions = DataReader.optTypedListOfMap(Object.class, eventData, PROPOSITIONS, Collections.emptyList());
                    if (retrievedPropositions == null || retrievedPropositions.isEmpty()) {
                        failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                        return;
                    }

                    for (final Map<String, Object> propositionMap : retrievedPropositions) {
                        final Proposition proposition = Proposition.fromEventData(propositionMap);
                        if (proposition != null) {
                            final Surface surface = MessagingUtils.scopeToSurface(proposition.getScope());
                            requestedPropositionsMap = MessagingUtils.updatePropositionMapForSurface(surface, proposition, requestedPropositionsMap);
                        }
                    }

                    callback.call(requestedPropositionsMap);
                } catch (final DataReaderException ignored) {
                    failWithError(callback, AdobeError.UNEXPECTED_ERROR);
                }
            }
        });
    }


    /**
     * Dispatches an event to fetch propositions for the provided surfaces from Adobe Journey Optimizer via the Experience Edge network.
     *
     * @param surfaces A {@code List<Surface>} containing {@link Surface}s to be used for retrieving propositions
     */
    public static void updatePropositionsForSurfaces(@NonNull final List<Surface> surfaces) {
        if (surfaces == null || surfaces.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME, "Cannot update propositions as the provided list of surfaces is null or empty.");
            return;
        }

        final List<Map<String, Object>> validSurfacesFlattened = new ArrayList<>();
        for (final Surface surface : surfaces) {
            if (surface.isValid()) {
                validSurfacesFlattened.add(surface.toEventData());
            }
        }

        if (validSurfacesFlattened.isEmpty()) {
            Log.warning(LOG_TAG, CLASS_NAME, "Cannot update propositions as the provided list of surfaces has no valid items.");
            return;
        }

        final Map<String, Object> eventData = new HashMap<>();
        eventData.put(UPDATE_PROPOSITIONS_EVENT, true);
        eventData.put(SURFACES, validSurfacesFlattened);

        final Event updatePropositionsEvent = new Event.Builder(UPDATE_PROPOSITIONS,
                EventType.MESSAGING, EventSource.REQUEST_CONTENT)
                .setEventData(eventData)
                .build();

        MobileCore.dispatchEvent(updatePropositionsEvent);
    }

    /**
     * Invokes fail method with the provided {@code error}, if the callback is an instance of {@code AdobeCallbackWithError}.
     *
     * @param callback can be an instance of {@link AdobeCallback} or {@link AdobeCallbackWithError}.
     * @param error {@link AdobeError} indicating the error name and code.
     */
    private static void failWithError(final AdobeCallback<?> callback, final AdobeError error) {

        final AdobeCallbackWithError<?> callbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<?>) callback : null;

        if (callbackWithError != null) {
            callbackWithError.fail(error);
        }
    }

    /**
     * Determines the {@code AdobeError} provided the error code.
     *
     * @return {@link AdobeError} corresponding to the given error code, or {@link AdobeError#UNEXPECTED_ERROR} otherwise.
     */
    @SuppressWarnings("magicnumber")
    private static AdobeError convertToAdobeError(final int errorCode) {
        final AdobeError error;
        switch (errorCode) {
            case 0:
                error = AdobeError.UNEXPECTED_ERROR;
                break;
            case 1:
                error = AdobeError.CALLBACK_TIMEOUT;
                break;
            case 2:
                error = AdobeError.CALLBACK_NULL;
                break;
            case 11:
                error = AdobeError.EXTENSION_NOT_INITIALIZED;
                break;
            default:
                error = AdobeError.UNEXPECTED_ERROR;
        }
        return error;
    }

    private static void callTrackingCallback(final PushTrackingStatus trackingStatus, final AdobeCallback<PushTrackingStatus> callback) {
        if (callback != null) {
            callback.call(trackingStatus);
        }
    }
}
