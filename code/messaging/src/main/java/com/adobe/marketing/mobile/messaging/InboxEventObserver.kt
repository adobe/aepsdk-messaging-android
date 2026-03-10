/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.MessagingEdgeEventType
import com.adobe.marketing.mobile.aepcomposeui.InboxEvent

/**
 * Handles tracking for inbox-level events. Used internally by [MessagingInboxEventObserver].
 */
internal class InboxEventObserver {

    /**
     * Performs tracking for inbox events.
     */
    internal fun onInboxEvent(event: InboxEvent) {
        when (event) {
            is InboxEvent.Display -> {
                ContentCardMapper.instance.getInboxPropositionItem(event.inboxUIState.template.id)
                    ?.track(MessagingEdgeEventType.DISPLAY)
            }
        }
    }
}
