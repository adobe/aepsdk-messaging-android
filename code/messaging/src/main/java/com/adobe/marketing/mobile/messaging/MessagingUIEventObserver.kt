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
import com.adobe.marketing.mobile.aepuitemplates.AepUITemplate
import com.adobe.marketing.mobile.aepuitemplates.SmallImageTemplate
import com.adobe.marketing.mobile.services.Log

class MessagingUiEventObserver(private val callback: ContentCardCallback?) : AepUIEventObserver {
    companion object {
        private const val SELF_TAG = "MessagingUiEventObserver"
    }

    override fun onEvent(event: UIEvent<*, *>) {
        when (event) {
            is UIEvent.Display -> {
                Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI Displayed")
                handleSmallImageTracking(event.aepUi.getTemplate(), null, MessagingEdgeEventType.DISPLAY)
            }
            is UIEvent.Interact -> {
                if (callback?.onCardClick(event.aepUi.getTemplate()) == true) {
                    Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI Click")
                    handleSmallImageTracking(event.aepUi.getTemplate(), "clicked", MessagingEdgeEventType.INTERACT)
                }
            }
            is UIEvent.Dismiss -> {
                if (callback?.onCardDismiss(event.aepUi.getTemplate()) == true) {
                    Log.trace(MessagingConstants.LOG_TAG, SELF_TAG, "SmallImageUI Dismissed")
                    handleSmallImageTracking(event.aepUi.getTemplate(), null, MessagingEdgeEventType.DISMISS)
                }
            }
        }
    }

    private fun handleSmallImageTracking(template: AepUITemplate, interaction: String?, eventType: MessagingEdgeEventType) {
        val template = template as SmallImageTemplate
        val contentCardSchemaData = ContentCardMapper.instance.getContentCardSchemaDataForPropositionId(template.id)
        contentCardSchemaData?.track(interaction, eventType)
    }
}

interface ContentCardCallback {
    fun onCardClick(template: AepUITemplate): Boolean
    fun onCardDismiss(template: AepUITemplate): Boolean
}
