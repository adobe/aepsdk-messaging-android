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
internal abstract class MessagingEventHandler(private val callback: ContentCardUIEventListener?) {
    companion object {
        private const val SELF_TAG = "MessagingEventHandler"
    }

    /**
     * Method to be implemented by the subclass to perform the template specific logic on events.
     *
     * @param event the event to handle
     * @param propositionId the id of the proposition
     */
    abstract fun handleEvent(event: UIEvent<*, *>, propositionId: String)

    /**
     * Performs the logic common for all templates for display and dismiss events.
     * Delegates the template specific logic and interact event handling to the subclass by calling [handleEvent].
     *
     * @param event the event to handle
     * @param propositionId the id of the proposition
     */
    internal fun onEvent(event: UIEvent<*, *>, propositionId: String) {

        when (event) {
            is UIEvent.Display -> {
                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "${event.aepUi.getTemplate().getType()} with id $propositionId is displayed"
                )

                // UIEvent.Display can be called multiple times on configuration changes
                // We only need to send tracking events for initial composition
                if (event.aepUi.getState().displayed) {
                    Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "UI already displayed, skipping handling display event"
                    )
                    return
                }

                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "UI displayed for the first time, sending display tracking event"
                )

                handleEvent(event, propositionId)

                track(propositionId, null, MessagingEdgeEventType.DISPLAY)
                callback?.onDisplay(event.aepUi)
            }
            is UIEvent.Dismiss -> {
                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "${event.aepUi.getTemplate().getType()} with id $propositionId is dismissed"
                )

                // UIEvent.Dismiss can be called multiple times on multiple dismiss actions
                // We only need to send tracking events for initial dismissal
                if (event.aepUi.getState().dismissed) {
                    Log.debug(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "UI already dismissed, skipping handling dismiss tracking event"
                    )
                    return
                }
                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "UI dismissed for the first time, sending dismiss tracking event"
                )

                handleEvent(event, propositionId)

                track(propositionId, null, MessagingEdgeEventType.DISMISS)
                callback?.onDismiss(event.aepUi)
            }

            is UIEvent.Interact -> {
                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "${event.aepUi.getTemplate().getType()} with id $propositionId is interacted"
                )

                handleEvent(event, propositionId)
            }
        }
    }

    internal fun track(propositionId: String, interaction: String?, eventType: MessagingEdgeEventType?) {
        val contentCardSchemaData = ContentCardMapper.instance.getContentCardSchemaData(propositionId)
        contentCardSchemaData?.track(interaction, eventType)
    }
}
