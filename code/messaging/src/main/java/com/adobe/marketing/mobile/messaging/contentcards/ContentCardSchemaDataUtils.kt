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

package com.adobe.marketing.mobile.messaging.contentcards

import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepDismissButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardSchemaData
import com.adobe.marketing.mobile.messaging.MessagingConstants

object ContentCardSchemaDataUtils {
    fun createAepText(map: Map<String, Any>): AepText {
        return AepText(
            content = map[MessagingConstants.AepUIKeys.CONTENT] as String
        )
    }

    fun createAepImage(map: Map<String, Any>): AepImage {
        return AepImage(
            url = map[MessagingConstants.AepUIKeys.URL] as? String,
            darkUrl = map[MessagingConstants.AepUIKeys.DARK_URL] as? String
        )
    }

    fun createAepButton(map: Map<String, Any>): AepButton {
        return AepButton(
            id = map[MessagingConstants.AepUIKeys.INTERACT_ID] as String,
            actionUrl = map[MessagingConstants.AepUIKeys.ACTION_URL] as String,
            text = (map[MessagingConstants.AepUIKeys.TEXT] as Map<String, Any>).let { createAepText(it) }
        )
    }

    fun createAepDismissButton(map: Map<String, Any>): AepDismissButton {
        return AepDismissButton(
            style = map[MessagingConstants.AepUIKeys.STYLE] as? String ?: "NONE_ICON"
        )
    }
}

// Manual JSON parsing function
fun getTemplateModelFromContentCardSchemaData(contentCardSchemaData: ContentCardSchemaData): AepUITemplate? {
    val contentMap =
        contentCardSchemaData.content as? Map<String, Any> ?: throw IllegalArgumentException("Content map is null")
    try {
        val id = contentMap[MessagingConstants.AepUIKeys.ID] as String
        val title =
            ContentCardSchemaDataUtils.createAepText(contentMap[MessagingConstants.AepUIKeys.TITLE] as Map<String, Any>)
        val body = (contentMap[MessagingConstants.AepUIKeys.BODY] as? Map<String, Any>)?.let {
            ContentCardSchemaDataUtils.createAepText(it)
        }
        val image = (contentMap[MessagingConstants.AepUIKeys.IMAGE] as? Map<String, Any>)?.let {
            ContentCardSchemaDataUtils.createAepImage(it)
        }
        val actionUrl = contentMap[MessagingConstants.AepUIKeys.ACTION_URL] as? String
        val buttons = (contentMap[MessagingConstants.AepUIKeys.BUTTONS] as? List<Map<String, Any>>)?.map {
            ContentCardSchemaDataUtils.createAepButton(it)
        }
        val dismissBtn = (contentMap[MessagingConstants.AepUIKeys.DISMISS_BTN] as? Map<String, Any>)?.let {
            ContentCardSchemaDataUtils.createAepDismissButton(it)
        }
        return SmallImageTemplate(
            id = id,
            title = title,
            body = body,
            image = image,
            actionUrl = actionUrl,
            buttons = buttons,
            dismissBtn = dismissBtn
        )
    } catch (e: Exception) {
        throw IllegalArgumentException("Error parsing content map: ${e.message}")
    }
}
