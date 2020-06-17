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


public class GenericDataOSListener extends ModuleEventListener<com.adobe.marketing.mobile.MessagingModule> {

    private final EventsHandler eventsHandler;

    GenericDataOSListener(com.adobe.marketing.mobile.MessagingModule module, EventType type, EventSource source) {
        super(module, type, source);
        eventsHandler = module;
    }

    @Override
    public void hear(Event event) {
        if(event == null && event.getEventData() == null && eventsHandler == null){
            Log.debug(com.adobe.marketing.mobile.MessagingConstant.LOG_TAG, "Event or Event data is null.");
            return;
        }
        eventsHandler.handleTrackingInfo(event);
    }
}
