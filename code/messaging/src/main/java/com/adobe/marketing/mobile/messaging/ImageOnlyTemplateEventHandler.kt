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
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.UIAction
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider

/**
 * Image Only Template Event Handler for content card events.
 *
 * @property callback An optional callback to invoke when a content card event occurs.
 */
internal class ImageOnlyTemplateEventHandler(private val callback: ContentCardUIEventListener?) :
    MessagingEventHandler(callback) {
    companion object {
        private const val SELF_TAG = "ImageOnlyTemplateEventHandler"
    }

    override fun handleEvent(event: UIEvent<*, *>, propositionId: String) {
        val imageOnlyUI = event.aepUi as ImageOnlyUI
        val currentUiState = event.aepUi.getState() as ImageOnlyCardUIState
        when (event) {
            // For dismiss event, change the dismissed state of the UI to true
            // so that the UI can be removed from the screen
            is UIEvent.Dismiss -> {
                imageOnlyUI.updateState(currentUiState.copy(dismissed = true))
            }

            // For display event, change the displayed state of the UI to true
            // after the initial composition
            is UIEvent.Display -> {
                imageOnlyUI.updateState(currentUiState.copy(displayed = true))
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
                        "ImageOnlyUI opening URL: ${event.action.actionUrl}"
                    )
                    ServiceProvider.getInstance().uriService.openUri(event.action.actionUrl)
                }

                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "ImageOnlyUI ${event.action.id} clicked"
                )
                track(propositionId, event.action.id, MessagingEdgeEventType.INTERACT)
            }
        }
    }
}
