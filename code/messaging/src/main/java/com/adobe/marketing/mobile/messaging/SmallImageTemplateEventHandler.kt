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
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.utils.UIAction
import com.adobe.marketing.mobile.services.Log

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

    internal fun onInteractEvent(event: UIEvent.Interact<*, *>) {
        val template: SmallImageTemplate = event.aepUi.getTemplate() as SmallImageTemplate
        val contentCardSchemaData =
            ContentCardMapper.instance.getContentCardSchemaDataForPropositionId(template.id)
        when (event.action) {
            is UIAction.Click -> {
                callback?.onInteract(event.aepUi, event.action.id, event.action.actionUrl)
                when (event.action.id) {
                    "none", "simple", "circle" -> { // handle content card dismiss button click
                        Log.trace(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "SmallImageUI dismiss button clicked"
                        )
                        contentCardSchemaData?.track(
                            event.action.toString(),
                            MessagingEdgeEventType.DISMISS
                        )
                    }

                    else -> { // handle content card button click
                        Log.trace(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "SmallImageUI ${event.action.id} button clicked"
                        )
                        contentCardSchemaData?.track(
                            event.action.toString(),
                            MessagingEdgeEventType.INTERACT
                        )
                    }
                }
            }
        }
    }
}
