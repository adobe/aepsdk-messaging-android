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
import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate

class ContentCardEventObserver(private var callback: ContentCardCallback?) : AepUIEventObserver {
    private val smallImageEventHandler by lazy { SmallImageTemplateEventHandler(callback) }

    override fun onEvent(event: UIEvent<*, *>) {
        when (val template = event.aepUi.getTemplate()) {
            is SmallImageTemplate -> { smallImageEventHandler.onEvent(event, template.id) }
        }
    }
}

/**
 * Interface to handle different callback events which can occur for a displayed content card.
 */
interface ContentCardCallback {

    /**
     * Callback to invoke when a content card is displayed.
     *
     * @param aepUI The AepUI instance that was displayed.
     */
    fun onDisplay(aepUI: AepUI<*, *>,)

    /**
     * Callback to invoke when a content card is dismissed.
     *
     * @param aepUI The AepUI instance that was dismissed.
     */
    fun onDismiss(aepUI: AepUI<*, *>,)

    /**
     * Callback to invoke when a content card is interacted with.
     *
     * @param aepUI The AepUI instance that was interacted with.
     * @param interactionId An optional string identifier for the interaction event.
     * @param actionUrl An optional URL associated with the interaction.
     * @return true if the interaction was handled, false otherwise.
     */
    fun onInteract(aepUI: AepUI<*, *>, interactionId: String?, actionUrl: String?): Boolean
}
