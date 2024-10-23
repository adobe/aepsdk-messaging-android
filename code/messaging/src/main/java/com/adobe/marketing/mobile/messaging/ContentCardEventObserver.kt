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
import com.adobe.marketing.mobile.aepcomposeui.interactions.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.utils.UIAction
import com.adobe.marketing.mobile.services.Log

class ContentCardEventObserver(private var callback: ContentCardCallback?) : AepUIEventObserver {
    companion object {
        private const val SELF_TAG = "ContentCardEventObserver"
    }

    override fun onEvent(event: UIEvent<*, *>) {
        if (callback == null) {
            callback = DefaultContentCardCallback()
        }

        when (event) {
            is UIEvent.Display -> {
                if (callback?.onCardDisplayed(event) == true) {
                    if (event.aepUi.getTemplate() is SmallImageTemplate) {
                        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI Displayed")
                        handleSmallImageTracking(
                            event.aepUi.getTemplate() as SmallImageTemplate,
                            null,
                            MessagingEdgeEventType.DISPLAY
                        )
                    }
                }
            }
            is UIEvent.Interact -> {
                handleInteractEvent(event)
            }
            is UIEvent.Dismiss -> {
                if (callback?.onCardDismiss(event) == true) {
                    if (event.aepUi.getTemplate() is SmallImageTemplate) {
                        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI Dismissed")
                        handleSmallImageTracking(event.aepUi.getTemplate() as SmallImageTemplate, null, MessagingEdgeEventType.DISMISS)
                    }
                }
            }
        }
    }

    private fun handleInteractEvent(event: UIEvent.Interact<*, *>) {
        when (event.action) {
            UIAction.CLICK -> {
                if (event.aepUi.getTemplate() is SmallImageTemplate) {
                    handleSmallImageClickInteraction(event)
                }
            }
            UIAction.DOUBLE_CLICK -> {
                // TODO: handle double click action
            }
            UIAction.DRAG -> {
                // TODO: handle drag action
            }
            UIAction.SWIPE -> {
                // TODO: handle swipe action
            }
            UIAction.LONG_PRESS -> {
                // TODO: handle long press action
            }
            UIAction.EXPAND -> {
                // TODO: handle expand action
            }
        }
    }

    private fun handleSmallImageClickInteraction(event: UIEvent.Interact<*, *>) {
        val smallImageTemplate = (event.aepUi.getTemplate() as SmallImageTemplate)
        // content card buttons have an interact id, dismiss buttons will have the style as the interact id
        if (!event.interactId.isNullOrEmpty()) {
            val contentCardSchemaData = ContentCardMapper.instance.getContentCardSchemaDataForPropositionId(smallImageTemplate.id)
            when (event.interactId) {
                "none", "simple", "circle" -> { // handle content card dismiss button click
                    if (callback?.onDismissButtonClick(event) == true) {
                        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI dismiss button click")
                        contentCardSchemaData?.track(event.action.toString(), MessagingEdgeEventType.INTERACT)
                    }
                }
                else -> { // handle content card button click
                    if (callback?.onButtonClick(event) == true) {
                        Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI ${event.interactId} button click")
                        contentCardSchemaData?.track(event.action.toString(), MessagingEdgeEventType.INTERACT)
                    }
                }
            }
        } else { // handle content card click
            if (callback?.onCardClick(event) == true) {
                if (event.aepUi.getTemplate() is SmallImageTemplate) {
                    Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI click")
                    handleSmallImageTracking(smallImageTemplate, event.action, MessagingEdgeEventType.INTERACT)
                }
            }
        }
    }

    private fun handleSmallImageTracking(template: SmallImageTemplate, interaction: UIAction?, eventType: MessagingEdgeEventType) {
        val contentCardSchemaData = ContentCardMapper.instance.getContentCardSchemaDataForPropositionId(template.id)
        contentCardSchemaData?.track(interaction?.toString(), eventType)
    }
}

class DefaultContentCardCallback : ContentCardCallback {
    override fun onCardDisplayed(event: UIEvent.Display<*, *>): Boolean {
        return true
    }

    override fun onCardClick(event: UIEvent.Interact<*, *>): Boolean {
        return true
    }

    override fun onCardDismiss(event: UIEvent.Dismiss<*, *>): Boolean {
        return true
    }

    override fun onButtonClick(event: UIEvent.Interact<*, *>): Boolean {
        return true
    }

    override fun onDismissButtonClick(event: UIEvent.Interact<*, *>): Boolean {
        return true
    }
}

/**
 * Interface to handle different callback events which can occur for a displayed content card.
 */
interface ContentCardCallback {

    /**
     * Callback to invoke when a content card is displayed.
     *
     * @param event The [UIEvent.Display] which occurred.
     * @return true if tracking should be handled by the Messaging extension, false otherwise.
     */
    fun onCardDisplayed(event: UIEvent.Display<*, *>): Boolean

    /**
     * Callback to invoke when a content card is clicked.
     *
     * @param event The [UIEvent.Interact] which occurred.
     * @return true if tracking should be handled by the Messaging extension, false otherwise.
     */
    fun onCardClick(event: UIEvent.Interact<*, *>): Boolean

    /**
     * Callback to invoke when a content card is dismissed.
     *
     * @param event The [UIEvent.Dismiss] which occurred.
     * @return true if tracking should be handled by the Messaging extension, false otherwise.
     */
    fun onCardDismiss(event: UIEvent.Dismiss<*, *>): Boolean

    /**
     * Callback to invoke when a content card button is clicked.
     *
     * @param event The [UIEvent.Interact] which occurred.
     * @return true if tracking should be handled by the Messaging extension, false otherwise.
     */
    fun onButtonClick(event: UIEvent.Interact<*, *>): Boolean

    /**
     * Callback to invoke when a content card dismiss button is clicked.
     *
     * @param event The [UIEvent.Interact] which occurred.
     * @return true if tracking should be handled by the Messaging extension, false otherwise.
     */
    fun onDismissButtonClick(event: UIEvent.Interact<*, *>): Boolean
}
