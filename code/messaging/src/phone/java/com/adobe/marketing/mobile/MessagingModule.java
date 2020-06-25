/*
  Copyright 2020 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/
package com.adobe.marketing.mobile;
import static com.adobe.marketing.mobile.MessagingConstant.LOG_TAG;

import com.adobe.marketing.mobile.xdm.*;

import java.util.Map;

public class MessagingModule extends Module implements EventsHandler {

    private static final String MODULE_NAME = "com.adobe.aepsdk.module.messaging";
    private PlatformServices platformServices = new AndroidPlatformServices();
    private PushTokenSyncer pushTokenSyncer = new PushTokenSyncer(platformServices.getNetworkService());
    private String ecid;

    protected MessagingModule(EventHub hub) {
        super(MODULE_NAME, hub);
        registerEventListeners();
    }

    private void registerEventListeners() {
        registerListener(EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT, ConfigurationResponseContentListener.class);
        registerListener(EventType.GENERIC_DATA, EventSource.OS, GenericDataOSListener.class);
        registerListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_CONTENT, IdentityRequestContentListener.class);
    }


    @Override
    public void handlePushToken(final Event event) {
        if (event == null) {
            Log.debug(LOG_TAG, "Unable to sync push token. Event data received is null");
        }

        if (ecid == null) {
            final EventData eventData = getSharedEventState(MessagingConstant.SharedState.Identity.NAME, event);
            try {
                ecid = eventData.getString2("mid");
            } catch (VariantException e) {
                Log.debug(LOG_TAG, "handlePushToken :: Error in getting identity shared state. Can not sync push token.");
            }
        }

        pushTokenSyncer.syncPushToken((String) event.getEventData().get(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER), ecid);
    }

    @Override
    public void handleTrackingInfo(Event event) {
        final EventData eventData = event.getData();
        if (eventData == null) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "handleTrackingInfo - Cannot track information, eventData is null.");
            return;
        }
        final String eventType = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_EVENT_TYPE, null);
        final String messageId = eventData.optString(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_MESSAGE_ID, null);
        final Boolean isApplicationOpened = eventData.optBoolean(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_APPLICATION_OPENED, false);
        final int actionId = eventData.optInteger(MessagingConstant.EventDataKeys.Messaging.TRACK_INFO_KEY_ACTION_ID, -1);

        if (StringUtils.isNullOrEmpty(eventType) || StringUtils.isNullOrEmpty(messageId)) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "handleTrackingInfo - Cannot track information, eventType or messageId is either null or empty.");
            return;
        }

        // Create XDM data with tracking data
        MobilePushTrackingSchemaTest schema = getXdmSchema(eventType, messageId, isApplicationOpened, actionId);
        ExperiencePlatformEvent experiencePlatformEvent = new ExperiencePlatformEvent.Builder()
                .setXdmSchema(schema)
                .build();

        ExperiencePlatform.sendEvent(experiencePlatformEvent, new ExperiencePlatformCallback() {
            @Override
            public void onResponse(Map<String, Object> map) {

                //todo
            }
        });
    }

    @Override
    public void handlePrivacyPreferenceChange(final Event event) {
        //TODO Handle privacy preference changes.
        if (event == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Unable to handle configuration response. Event received is null.");
        }
    }

    private MobilePushTrackingSchemaTest getXdmSchema(String eventType, String messageId, Boolean isApplicationOpened, int actionId) {
        final MobilePushTrackingSchemaTest schema = new MobilePushTrackingSchemaTest();
        final Acopprod3 acopprod3 = new Acopprod3();
        final Track track = new Track();
        final CustomAction customAction = new CustomAction();

        if (isApplicationOpened) {
            track.setApplicationOpened(true);
        } else {
            customAction.setValue(actionId);
            track.setCustomAction(customAction);
        }

        schema.setEventType(eventType);
        track.setId(messageId);
        acopprod3.setTrack(track);
        schema.setAcopprod3(acopprod3);
        return schema;
    }
}
