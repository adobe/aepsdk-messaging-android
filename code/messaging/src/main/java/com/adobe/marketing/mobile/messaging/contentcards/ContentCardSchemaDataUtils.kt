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

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepDismissButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.messaging.ContentCardSchemaData
import com.adobe.marketing.mobile.messaging.MessagingConstants
import com.adobe.marketing.mobile.messaging.Proposition
import com.adobe.marketing.mobile.messaging.SchemaType
import com.adobe.marketing.mobile.util.DataReader

/**
 * Utility object for creating instances of AEP model classes from Map data structures.
 * Provides functions to create [AepText], [AepImage], [AepButton], and [AepDismissButton].
 */
internal object ContentCardSchemaDataUtils {
    /**
     * Creates an instance of [AepText] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepText].
     * @return An instance of [AepText].
     */
    private fun createAepText(map: Map<String, Any>): AepText {
        return AepText(
            content = DataReader.optString(map, MessagingConstants.UIKeys.CONTENT, "")
        )
    }

    /**
     * Creates an instance of [AepImage] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepImage].
     * @return An instance of [AepImage].
     */
    private fun createAepImage(map: Map<String, Any>): AepImage {
        return AepImage(
            url = DataReader.optString(map, MessagingConstants.UIKeys.URL, null),
            darkUrl = DataReader.optString(map, MessagingConstants.UIKeys.DARK_URL, null)
        )
    }

    /**
     * Creates an instance of [AepButton] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepButton].
     * @return An instance of [AepButton].
     */
    private fun createAepButton(map: Map<String, Any>): AepButton {
        return AepButton(
            id = DataReader.optString(map, MessagingConstants.UIKeys.INTERACT_ID, null),
            actionUrl = DataReader.optString(map, MessagingConstants.UIKeys.ACTION_URL, null),
            text = DataReader.optTypedMap(Any::class.java, map, MessagingConstants.UIKeys.TEXT, null).let { createAepText(it) }
        )
    }

    /**
     * Creates an instance of [AepDismissButton] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepDismissButton].
     * @return An instance of [AepDismissButton].
     */
    private fun createAepDismissButton(map: Map<String, Any>): AepDismissButton {
        return AepDismissButton(
            style = DataReader.optString(map, MessagingConstants.UIKeys.STYLE, MessagingConstants.UIKeys.DismissButtonStyle.NONE)
        )
    }

    /**
     * Parses the given [ContentCardSchemaData] to create an [AepUITemplate].
     *
     * @param contentCardSchemaData The content card schema data containing the content map.
     * @return The parsed [AepUITemplate] or null if parsing fails.
     */
    internal fun getTemplateModel(contentCardSchemaData: ContentCardSchemaData): AepUITemplate? {
        try {
            val contentMap =
                contentCardSchemaData.content as? Map<String, Any> ?: throw IllegalArgumentException("Content map is null")
            val id = DataReader.getString(contentMap, MessagingConstants.UIKeys.ID)

            val title = DataReader.getTypedMap(Any::class.java, contentMap, MessagingConstants.UIKeys.TITLE)?.let {
                createAepText(it)
            }
            if (title == null || title.content.isEmpty()) {
                return null
            }
            val body = DataReader.optTypedMap(Any::class.java, contentMap, MessagingConstants.UIKeys.BODY, emptyMap())?.let {
                createAepText(it)
            }
            val image = DataReader.optTypedMap(Any::class.java, contentMap, MessagingConstants.UIKeys.IMAGE, emptyMap())?.let {
                createAepImage(it)
            }
            val actionUrl = DataReader.optString(contentMap, MessagingConstants.UIKeys.ACTION_URL, null)
            val buttons = DataReader.optTypedListOfMap(Any::class.java, contentMap, MessagingConstants.UIKeys.BUTTONS, emptyList()).map {
                createAepButton(it)
            }
            val dismissBtn = DataReader.optTypedMap(Any::class.java, contentMap, MessagingConstants.UIKeys.DISMISS_BTN, emptyMap())?.let {
                createAepDismissButton(it)
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
            return null
        }
    }

    /**
     * Provides an [AepUI] instance from the given [AepUITemplate].
     *
     * @param uiTemplate The template to convert into a UI component.
     * @return An instance of [AepUI] representing the given template.
     * @throws IllegalArgumentException If the provided template type is unsupported.
     */
    internal fun getAepUI(uiTemplate: AepUITemplate): AepUI<*, *> {
        return when (uiTemplate) {
            is SmallImageTemplate -> {
                SmallImageUI(uiTemplate, SmallImageCardUIState())
            }
            else -> throw IllegalArgumentException("Unsupported template type")
        }
    }

    internal fun buildTemplate(proposition: Proposition): AepUITemplate? {
        var baseTemplateModel: AepUITemplate? = null
        if (isContentCard(proposition)) {
            val propositionItem = proposition.items[0]
            baseTemplateModel = propositionItem.contentCardSchemaData?.let {
                getTemplateModel(it)
            }
        }
        return baseTemplateModel
    }

    private fun isContentCard(proposition: Proposition): Boolean {
        return proposition.items[0].schema == SchemaType.CONTENT_CARD
    }
}
