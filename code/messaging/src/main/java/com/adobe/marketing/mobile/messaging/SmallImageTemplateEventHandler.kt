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
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Small Image Template Event Handler for content card interact events.
 *
 * @property callback the callback to be invoked when the interact event occurs
 */
internal class SmallImageTemplateEventHandler(private val callback: ContentCardCallback?) :
    MessagingEventHandler(callback) {
    companion object {
        private const val SELF_TAG = "SmallImageTemplateEventHandler"
    }

    override fun handleEvent(event: UIEvent<*, *>, propositionId: String) {
        val smallImageUI = event.aepUi as SmallImageUI
        val currentUiState = event.aepUi.getState() as SmallImageCardUIState
        when (event) {
            // For dismiss event, change the dismissed state of the UI to true
            // so that the UI can be removed from the screen
            is UIEvent.Dismiss -> {
                smallImageUI.updateState(currentUiState.copy(dismissed = true))
            }

            // For display event, change the displayed state of the UI to true
            // after the tracking event is sent for the initial composition
            is UIEvent.Display -> {
                smallImageUI.updateState(currentUiState.copy(displayed = true))
            }

            is UIEvent.Interact -> {
                onInteractEvent(event, propositionId)
            }
        }
    }

    private fun onInteractEvent(event: UIEvent.Interact<*, *>, propositionId: String) {
        when (event.action) {
            is UIAction.Click -> {
                val urlHandled = callback?.onInteract(event.aepUi, event.action.id, event.action.actionUrl)

                // Open the URL if available and not handled by the listener
                if (urlHandled != true && !event.action.actionUrl.isNullOrEmpty()) {
                    Log.trace(
                        MessagingConstants.LOG_TAG,
                        SELF_TAG,
                        "SmallImageUI opening URL: ${event.action.actionUrl}"
                    )
                    ServiceProvider.getInstance().uriService.openUri(event.action.actionUrl)
                }

                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "SmallImageUI ${event.action.id} clicked"
                )
                track(propositionId, event.action.id, MessagingEdgeEventType.INTERACT)
            }
        }
    }
}
