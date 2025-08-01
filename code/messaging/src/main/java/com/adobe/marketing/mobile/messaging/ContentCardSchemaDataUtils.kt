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

import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.DataReader

/**
 * Utility object for creating instances of AEP model classes from Map data structures.
 * This utility handles parsing and transforming content card data into the relevant AEP UI models,
 * such as [AepText], [AepImage], [AepButton], and [AepDismissButton], as well as creating
 * the corresponding UI templates like [SmallImageTemplate].
 */
internal object ContentCardSchemaDataUtils {
    const val SELF_TAG = "ContentCardSchemaDataUtils"
    /**
     * Creates an instance of [AepText] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepText].
     * @return An instance of [AepText].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepText(map: Map<String, Any>): AepText {
        return AepText(
            content = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.CONTENT, null)
        )
    }

    /**
     * Creates an instance of [AepImage] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepImage].
     * @return An instance of [AepImage].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepImage(map: Map<String, Any>): AepImage {
        return AepImage(
            url = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.URL, null),
            darkUrl = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.DARK_URL, null)
        )
    }

    /**
     * Creates an instance of [AepButton] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepButton].
     * @return An instance of [AepButton].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepButton(map: Map<String, Any>): AepButton {
        return AepButton(
            id = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.INTERACT_ID, null),
            actionUrl = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.ACTION_URL, null),
            interactId = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.INTERACT_ID, null),
            text = createAepText(DataReader.optTypedMap(Any::class.java, map, MessagingConstants.ContentCard.UIKeys.TEXT, null))
        )
    }

    /**
     * Creates an instance of [AepDismissButton] from a map of key-value pairs.
     *
     * @param map The map containing the data for [AepDismissButton].
     * @return An instance of [AepDismissButton].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepDismissButton(map: Map<String, Any>): AepIcon? {
        val style = DataReader.optString(map, MessagingConstants.ContentCard.UIKeys.STYLE, MessagingConstants.ContentCard.UIKeys.NONE)
        return when (style) {
            MessagingConstants.ContentCard.UIKeys.SIMPLE -> AepIcon(R.drawable.close_filled)
            MessagingConstants.ContentCard.UIKeys.CIRCLE -> AepIcon(R.drawable.cancel_filled)
            else -> null
        }
    }

    /**
     * Parses the given [ContentCardSchemaData] to create an [AepUITemplate].
     *
     * @param contentCardSchemaData The content card schema data containing the content map.
     * @return The parsed [AepUITemplate] or null if parsing fails.
     */
    internal fun getTemplate(contentCardSchemaData: ContentCardSchemaData): AepUITemplate? {
        try {
            val contentMap =
                contentCardSchemaData.content as? Map<String, Any> ?: throw IllegalArgumentException("Content map is null")
            val id = contentCardSchemaData.parent.proposition.uniqueId

            val metaAdobeObject =
                contentCardSchemaData.meta?.get(MessagingConstants.ContentCard.UIKeys.ADOBE)

            metaAdobeObject.let { mapObject ->
                val templateType =
                    (mapObject as? Map<String, Any>)?.get(MessagingConstants.ContentCard.UIKeys.TEMPLATE)

                when (templateType) {
                    AepUITemplateType.SMALL_IMAGE.typeName -> {
                        return createAepUITemplate(AepUITemplateType.SMALL_IMAGE, contentMap, id)
                    }
                    AepUITemplateType.LARGE_IMAGE.typeName -> {
                        return createAepUITemplate(
                            templateType = AepUITemplateType.LARGE_IMAGE,
                            contentMap = contentMap,
                            id = id
                        )
                    }
                    else -> {
                        Log.error(
                            MessagingConstants.LOG_TAG,
                            SELF_TAG,
                            "Unsupported template type: $templateType. Skipping the content card."
                        )
                        return null
                    }
                }
            }
        } catch (e: Exception) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to parse content card schema data: ${e.message}"
            )
            return null
        }
    }

    private fun createAepUITemplate(templateType: AepUITemplateType, contentMap: Map<String, Any>, id: String): AepUITemplate? {
        val image = DataReader.optTypedMap(Any::class.java, contentMap, MessagingConstants.ContentCard.UIKeys.IMAGE, null)?.let {
            createAepImage(it)
        }
        val actionUrl = DataReader.optString(contentMap, MessagingConstants.ContentCard.UIKeys.ACTION_URL, null)
        val dismissBtn = DataReader.optTypedMap(Any::class.java, contentMap, MessagingConstants.ContentCard.UIKeys.DISMISS_BTN, null)?.let {
            createAepDismissButton(it)
        }
        if (templateType == AepUITemplateType.SMALL_IMAGE || templateType == AepUITemplateType.LARGE_IMAGE) {
            val title = DataReader.getTypedMap(
                Any::class.java,
                contentMap,
                MessagingConstants.ContentCard.UIKeys.TITLE
            )?.let {
                createAepText(it)
            }
            if (title == null || title.content.isEmpty()) {
                Log.error(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Title is missing in the content card with id: $id. Skipping the content card."
                )
                return null
            }
            val body = DataReader.optTypedMap(
                Any::class.java,
                contentMap,
                MessagingConstants.ContentCard.UIKeys.BODY,
                null
            )?.let {
                createAepText(it)
            }
            val buttons = DataReader.optTypedListOfMap(
                Any::class.java,
                contentMap,
                MessagingConstants.ContentCard.UIKeys.BUTTONS,
                emptyList()
            ).map {
                createAepButton(it)
            }

            if (templateType == AepUITemplateType.SMALL_IMAGE) {
                return SmallImageTemplate(
                    id = id,
                    title = title,
                    body = body,
                    image = image,
                    actionUrl = actionUrl,
                    buttons = buttons,
                    dismissBtn = dismissBtn
                )
            } else {
                return LargeImageTemplate(
                    id = id,
                    title = title,
                    body = body,
                    image = image,
                    actionUrl = actionUrl,
                    buttons = buttons,
                    dismissBtn = dismissBtn
                )
            }
        }
        return null
    }

    /**
     * Provides an [AepUI] instance from the given [AepUITemplate].
     *
     * @param uiTemplate The template to convert into a UI component.
     * @return An instance of [AepUI] representing the given template or null in case of unsupported template type.
     */
    internal fun getAepUI(uiTemplate: AepUITemplate): AepUI<*, *>? {
        return when (uiTemplate) {
            is SmallImageTemplate -> {
                SmallImageUI(uiTemplate, SmallImageCardUIState())
            }
            is LargeImageTemplate -> {
                LargeImageUI(uiTemplate, LargeImageCardUIState())
            }
            else -> {
                Log.error(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Unsupported template type: ${uiTemplate::class.java.simpleName}"
                )
                return null
            }
        }
    }

    /**
     * Builds an [AepUITemplate] from a given [Proposition].
     *
     * @param proposition The proposition containing the content card data.
     * @return The built [AepUITemplate] or null if the proposition is not a content card or parsing fails.
     */
    internal fun buildTemplate(proposition: Proposition): AepUITemplate? {

        if (!isContentCard(proposition)) return null

        if (proposition.items.size <= 0) return null

        val propositionItem = proposition.items[0]

        val baseTemplateModel: AepUITemplate? = propositionItem.contentCardSchemaData?.let {
            getTemplate(it)
        }
        return baseTemplateModel
    }

    /**
     * Checks if the given [Proposition] is a content card.
     *
     * @param proposition The proposition to check.
     * @return `true` if the proposition is a content card, `false` otherwise.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun isContentCard(proposition: Proposition): Boolean {
        return proposition.items.isNotEmpty() &&
            proposition.items[0]?.schema == SchemaType.CONTENT_CARD
    }
}
