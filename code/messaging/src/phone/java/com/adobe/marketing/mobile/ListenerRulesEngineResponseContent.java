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
 * Listens for {@link EventType#RULES_ENGINE}, {@link EventSource#RESPONSE_CONTENT} events.
 *
 * <p>
 * Monitor Rules response content events containing message definitions.
 *
 * @see MessagingInternal
 */
public class ListenerRulesEngineResponseContent extends ExtensionListener {
    private final static String SELF_TAG = "ListenerRulesEngineResponseContent";

    ListenerRulesEngineResponseContent(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    @Override
    public void hear(final Event event) {

        if (event == null || event.getEventData() == null) {
            Log.debug(MessagingConstants.LOG_TAG, "%s - Event or Event data is null, ignoring the event.", SELF_TAG);
            return;
        }

        final MessagingInternal parentExtension = (MessagingInternal) super.getParentExtension();
        if (parentExtension == null) {
            Log.debug(MessagingConstants.LOG_TAG, "%s - The parent extension associated with this listener is null, ignoring the event.", SELF_TAG);
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