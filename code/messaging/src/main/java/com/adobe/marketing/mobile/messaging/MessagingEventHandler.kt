/*
  Copyright 2024 Adobe. All rights reserved.
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
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.services.Log

/**
 * Base interaction handler for messaging events.
 * This class is responsible for handling the display and dismiss events for a content card proposition.
 *
 * @property callback An optional callback to invoke when a content card event occurs.
 */
internal open class MessagingEventHandler(private val callback: ContentCardUIEventListening?) {
    companion object {
        private const val SELF_TAG = "MessagingEventHandler"

        internal fun track(propositionId: String, interaction: String?, eventType: MessagingEdgeEventType?) {
            val contentCardSchemaData = ContentCardMapper.instance.getContentCardSchemaData(propositionId)
            contentCardSchemaData?.track(interaction, eventType)
        }
    }

    internal open fun onEvent(event: UIEvent<*, *>, propositionId: String) {
        if (event is UIEvent.Display) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "${event.aepUi.getTemplate().getType()} Displayed")
            callback?.onDisplay(event.aepUi)
            track(propositionId, null, MessagingEdgeEventType.DISPLAY)
        } else if (event is UIEvent.Dismiss) {
            Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "${event.aepUi.getTemplate().getType()} Dismissed")
            callback?.onDismiss(event.aepUi)
            track(propositionId, null, MessagingEdgeEventType.DISMISS)
        }
    }
}
