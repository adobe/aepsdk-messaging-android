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

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.interactions.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate

class ContentCardEventObserver(private var callback: ContentCardCallback?) : AepUIEventObserver {
    companion object {
        private const val SELF_TAG = "ContentCardEventObserver"
    }

    override fun onEvent(event: UIEvent<*, *>) {
        when (event) {
            is UIEvent.Display -> {
                when (val template = event.aepUi.getTemplate()) {
                    is SmallImageTemplate -> {
                        val eventHandler = SmallImageTemplateEventHandler(callback)
                        eventHandler.onEvent(event, template.id)
                    }
                }
            }
            is UIEvent.Dismiss -> {
                when (val template = event.aepUi.getTemplate()) {
                    is SmallImageTemplate -> {
                        val eventHandler = SmallImageTemplateEventHandler(callback)
                        eventHandler.onEvent(event, template.id)
                    }
                }
            }
            is UIEvent.Interact -> {
                when (event.aepUi.getTemplate()) {
                    is SmallImageTemplate -> {
                        val eventHandler = SmallImageTemplateEventHandler(callback)
                        eventHandler.onInteractEvent(event)
                    }
                }
            }
        }
    }
}

/**
 * Interface to handle different callback events which can occur for a displayed content card.
 */
interface ContentCardCallback {

    /**
     * Callback to invoke when a content card is displayed.
     */
    fun onDisplay()

    /**
     * Callback to invoke when a content card is dismissed.
     */
    fun onDismiss()

    /**
     * Callback to invoke when a content card is interacted with.
     *
     * @param aepUI The AepUI instance that was interacted with.
     * @param interactionId An optional string identifier for the interaction event.
     * @param actionUrl An optional URL associated with the interaction.
     */
    fun onInteract(aepUI: AepUI<*, *>, interactionId: String?, actionUrl: String?)
}
