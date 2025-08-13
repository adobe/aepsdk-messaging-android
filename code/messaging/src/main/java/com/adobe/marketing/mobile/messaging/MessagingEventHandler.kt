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
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.AepCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Base interaction handler for messaging events.
 * This class is responsible for handling the display, dismiss and interact events for a content card proposition.
 *
 * @param T The type of the template associated with the UI component.
 * @param S The type of the state associated with the UI component.
 * @property callback An optional callback to invoke when a content card event occurs.
 */
internal abstract class MessagingEventHandler<T : AepUITemplate, S : AepCardUIState>(
    private val callback: ContentCardUIEventListener?
) {
    companion object {
        private const val SELF_TAG = "MessagingEventHandler"
    }

    /**
     * Performs the logic common for all templates for display and dismiss events.
     * Delegates the logic for interact event handling to [onInteractEvent]
     * which can be overridden by subclasses to perform template specific logic.
     *
     * @param event the event to handle
     * @param propositionId the id of the proposition
     */
    internal fun onEvent(event: UIEvent<T, S>, propositionId: String) {
        val ui = event.aepUi
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

                ui.updateState(getNewState(event))

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

                ui.updateState(getNewState(event))

                track(propositionId, null, MessagingEdgeEventType.DISMISS)
                callback?.onDismiss(event.aepUi)
            }

            is UIEvent.Interact -> {
                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "${event.aepUi.getTemplate().getType()} with id $propositionId is interacted"
                )

                onInteractEvent(event, propositionId)
            }
        }
    }

    /**
     * Updates the state of the UI based on the event.
     * This method should be implemented by the subclass to provide
     * a new state specific to the template based on the event.
     *
     * @param event the event to handle
     * @return the updated state
     */
    internal abstract fun getNewState(event: UIEvent<T, S>): S

    /**
     * Performs the logic for interact events.
     * This method can be overridden by subclasses to perform template specific logic.
     *
     * @param event the interact event to handle
     * @param propositionId the id of the proposition
     */
    internal open fun onInteractEvent(event: UIEvent.Interact<T, S>, propositionId: String) {
        val templateType = event.aepUi.getTemplate().getType()
        when (event.action) {
            is UIAction.Click -> {
                val urlHandled =
                    callback?.onInteract(event.aepUi, event.action.id, event.action.actionUrl)

                // Open the URL if available and not handled by the listener
                if (urlHandled != true && !event.action.actionUrl.isNullOrEmpty()) {
                    Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "$templateType opening URL: ${event.action.actionUrl}"
                    )
                    ServiceProvider.getInstance().uriService.openUri(event.action.actionUrl)
                }

                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "$templateType ${event.action.id} clicked"
                )
                track(propositionId, event.action.id, MessagingEdgeEventType.INTERACT)
            }
        }
    }

    internal fun track(propositionId: String, interaction: String?, eventType: MessagingEdgeEventType?) {
        val contentCardSchemaData = ContentCardMapper.instance.getContentCardSchemaData(propositionId)
        contentCardSchemaData?.track(interaction, eventType)
    }
}
