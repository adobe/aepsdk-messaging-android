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

class ListenerHubSharedState extends ExtensionListener {
    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type         the {@link String} eventType this listener is registered to handle
     * @param source       the {@link String} eventSource this listener is registered to handle
     */
    ListenerHubSharedState(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }


    /**
     * Method that gets called when event with event type {@link EventType#HUB}
     * and with event source {@link EventSource#SHARED_STATE} is dispatched through eventHub.
     * <p>
     * @param event the hub shared state change {@link Event} to be processed
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "ListenerHubSharedState - Event / EventData is null. Ignoring the event.");
            return;
        }

        final MessagingInternal parentExtension = (MessagingInternal) super.getParentExtension();

        if (parentExtension == null) {
            Log.debug(MessagingConstant.LOG_TAG,
                    "ListenerHubSharedState - The parent extension, associated with this listener is null, ignoring the event.");
            return;
        }

        parentExtension.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                parentExtension.queueEvent(event);
                parentExtension.processEvents();
            }
        });
    }
}