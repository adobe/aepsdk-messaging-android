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

import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepDismissButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardSchemaData
import org.json.JSONObject

object AepModelUtils {
    fun createAepText(map: Map<String, Any>): AepText {
        return AepText(
            content = map[AepUIConstants.Keys.CONTENT] as String
        )
    }

    fun createAepColor(map: Map<String, Any>): AepColor {
        return AepColor(
            lightColour = map[AepUIConstants.Keys.LIGHT_COLOUR] as String,
            darkColour = map[AepUIConstants.Keys.DARK_COLOUR] as? String
        )
    }

    fun createAepImage(map: Map<String, Any>): AepImage {
        return AepImage(
            url = map[AepUIConstants.Keys.URL] as? String,
            darkUrl = map[AepUIConstants.Keys.DARK_URL] as? String
        )
    }

    fun createAepButton(map: Map<String, Any>): AepButton {
        return AepButton(
            interactId = map[AepUIConstants.Keys.INTERACT_ID] as String,
            actionUrl = map[AepUIConstants.Keys.ACTION_URL] as String,
            text = (map[AepUIConstants.Keys.TEXT] as Map<String, Any>).let { createAepText(it) }
        )
    }

    fun createAepDismissButton(map: Map<String, Any>): AepDismissButton {
        return AepDismissButton(
            style = map[AepUIConstants.Keys.STYLE] as? String ?: "NONE_ICON"
        )
    }
}

// Manual JSON parsing function
fun getTemplateModelFromContentCardSchemaData(contentCardSchemaData: ContentCardSchemaData): AepUITemplate? {
    val contentMap =
        contentCardSchemaData.content as? Map<String, Any> ?: throw IllegalArgumentException("Content map is null")
    try {
        val id = contentMap[AepUIConstants.Keys.ID] as String
        val title =
            AepModelUtils.createAepText(contentMap[AepUIConstants.Keys.TITLE] as Map<String, Any>)
        val body = (contentMap[AepUIConstants.Keys.BODY] as? Map<String, Any>)?.let {
            AepModelUtils.createAepText(it)
        }
        val image = (contentMap[AepUIConstants.Keys.IMAGE] as? Map<String, Any>)?.let {
            AepModelUtils.createAepImage(it)
        }
        val actionUrl = contentMap[AepUIConstants.Keys.ACTION_URL] as? String
        val buttons = (contentMap[AepUIConstants.Keys.BUTTONS] as? List<Map<String, Any>>)?.map {
            AepModelUtils.createAepButton(it)
        }
        val dismissBtn = (contentMap[AepUIConstants.Keys.DISMISS_BTN] as? Map<String, Any>)?.let {
            AepModelUtils.createAepDismissButton(it)
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
