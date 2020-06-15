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


import java.util.Map;

public class MessagingModule extends Module implements EventsHandler {

    private static final String MODULE_NAME = "com.adobe.aepsdk.module.messaging";

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
    public void handlePushToken(final Map<String, Object> eventData) {
        //TODO Cache and send new push token.
    }

    @Override
    public void handleTrackingInfo(Map<String, Object> eventData) {
        //TODO Send tracking info.
    }

    @Override
    public void handlePrivacyPreferenceChange(Map<String, Object> eventData) {
        //TODO Handle privacy preference change.

    }
}
