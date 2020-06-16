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


public class MessagingModule extends Module implements EventsHandler {

    private static final String MODULE_NAME = "com.adobe.aepsdk.module.messaging";
    private PlatformServices platformServices = new AndroidPlatformServices();
    private PushTokenSyncer pushTokenSyncer = new PushTokenSyncer(platformServices.getNetworkService(), platformServices.getJsonUtilityService());
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
            Log.debug(MessagingConstant.LOG_TAG, "Unable to sync push token. Event data received is null");
        }

        pushTokenSyncer.syncPushToken((String) event.getEventData().get(MessagingConstant.EventDataKeys.Identity.PUSH_IDENTIFIER), ecid);
    }

    @Override
    public void handleTrackingInfo(final Event event) {
        //TODO Send tracking info.
    }

    @Override
    public void handlePrivacyPreferenceChange(final Event event) {
        if (event == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Unable to handle configuration response. Event received is null.");
        }
        final EventData eventData = getSharedEventState(MessagingConstant.SharedState.Identity.NAME, event);
        try {
            ecid = eventData.getString2("mid");
        } catch (VariantException e) {
            e.printStackTrace();
        }

    }
}
