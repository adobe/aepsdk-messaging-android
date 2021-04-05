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

/**
 * Listens for {@link EventType#CONFIGURATION}, {@link EventSource#RESPONSE_CONTENT} events.
 * Monitor request content events consist of following events
 * @see MessagingInternal
 */
public class ConfigurationResponseContentListener extends ExtensionListener {

    /**
     * Constructor.
     *
     * @param extensionApi an instance of  {@link ExtensionApi}
     * @param type  {@link EventType} this listener is registered to handle
     * @param source {@link EventSource} this listener is registered to handle
     */
    ConfigurationResponseContentListener(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * Method that gets called when {@link EventType#CONFIGURATION},
     * {@link EventSource#RESPONSE_CONTENT} event is dispatched through eventHub.
     * <p>
     * {@link MessagingInternal} queues event and attempts to process them immediately.
     *
     * @param event configuration response event {@link Event} to be processed
     * @see MessagingInternal#processEvents()
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null) {
            Log.debug(MessagingConstant.LOG_TAG, "Event or Event data is null.");
            return;
        }

        final MessagingInternal parentExtension = (MessagingInternal) super.getParentExtension();

        if (parentExtension == null) {
            Log.warning(MessagingConstant.LOG_TAG,
                    "The parent extension, associated with the ConfigurationResponseContentListener is null, ignoring the configuration response event.");
            return;
        }

        parentExtension.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // Handle the configuration response event
                parentExtension.processConfigurationResponse(event);
            }
        });
    }
}
