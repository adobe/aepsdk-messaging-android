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
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.DataReader

/**
 * Utility object for creating instances of AEP model classes from Map data structures.
 * This utility handles parsing and transforming content card data into the relevant AEP UI models,
 * such as [AepText], [AepImage], [AepButton], and [AepIcon], as well as creating
 * the corresponding UI templates like [SmallImageTemplate], [LargeImageTemplate], [ImageOnlyTemplate].
 */
internal object ContentCardSchemaDataUtils {
    const val SELF_TAG = "ContentCardSchemaDataUtils"

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
            is ImageOnlyTemplate -> {
                ImageOnlyUI(uiTemplate, ImageOnlyCardUIState())
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
     * Parses the given [ContentCardSchemaData] to create an [AepUITemplate].
     *
     * @param contentCardSchemaData The content card schema data containing the content map.
     * @return The parsed [AepUITemplate] or null if parsing fails.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getTemplate(contentCardSchemaData: ContentCardSchemaData): AepUITemplate? {
        try {
            val contentMap =
                contentCardSchemaData.content as? Map<String, Any>
                    ?: throw IllegalArgumentException("Content map is null")
            val id = contentCardSchemaData.parent.proposition.uniqueId

            val metaAdobeObject =
                contentCardSchemaData.meta?.get(MessagingConstants.ContentCard.UIKeys.ADOBE)

            metaAdobeObject.let { mapObject ->
                val templateType =
                    (mapObject as? Map<String, Any>)?.get(MessagingConstants.ContentCard.UIKeys.TEMPLATE)

                when (templateType) {
                    AepUITemplateType.SMALL_IMAGE.typeName -> {
                        return createSmallImageTemplate(contentMap, id)
                    }

                    AepUITemplateType.LARGE_IMAGE.typeName -> {
                        return createLargeImageTemplate(contentMap, id)
                    }

                    AepUITemplateType.IMAGE_ONLY.typeName -> {
                        return createImageOnlyTemplate(contentMap, id)
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

    private fun createSmallImageTemplate(
        contentMap: Map<String, Any>,
        cardId: String
    ): SmallImageTemplate? {
        val title = createAepText(contentMap, MessagingConstants.ContentCard.UIKeys.TITLE, cardId)
        if (title == null || title.content.isEmpty()) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Title is missing for SmallImage content card with id: $cardId. Skipping the content card."
            )
            return null
        }
        return SmallImageTemplate(
            id = cardId,
            title = title,
            body = createAepText(contentMap, MessagingConstants.ContentCard.UIKeys.BODY, cardId),
            image = createAepImage(contentMap, cardId),
            actionUrl = getActionUrl(contentMap, cardId),
            buttons = createAepButtonsList(contentMap, cardId),
            dismissBtn = createAepDismissButton(contentMap, cardId)
        )
    }

    private fun createLargeImageTemplate(
        contentMap: Map<String, Any>,
        cardId: String
    ): LargeImageTemplate? {
        val title = createAepText(contentMap, MessagingConstants.ContentCard.UIKeys.TITLE, cardId)
        if (title == null || title.content.isEmpty()) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Title is missing for LargeImage content card with id: $cardId. Skipping the content card."
            )
            return null
        }
        return LargeImageTemplate(
            id = cardId,
            title = title,
            body = createAepText(contentMap, MessagingConstants.ContentCard.UIKeys.BODY, cardId),
            image = createAepImage(contentMap, cardId),
            actionUrl = getActionUrl(contentMap, cardId),
            buttons = createAepButtonsList(contentMap, cardId),
            dismissBtn = createAepDismissButton(contentMap, cardId)
        )
    }

    private fun createImageOnlyTemplate(
        contentMap: Map<String, Any>,
        cardId: String
    ): ImageOnlyTemplate? {
        val image = createAepImage(contentMap, cardId)
        if (image == null || image.url.isNullOrBlank()) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Image is missing for ImageOnly content card with id: $cardId. Skipping the content card."
            )
            return null
        }
        return ImageOnlyTemplate(
            id = cardId,
            image = image,
            actionUrl = getActionUrl(contentMap, cardId),
            dismissBtn = createAepDismissButton(contentMap, cardId)
        )
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

    /**
     * Creates an instance of [AepText] from a map of key-value pairs.
     *
     * @param contentMap The map containing the card data.
     * @param textKey The key for the text content in the map.
     * @param cardId The unique identifier for the content card.
     * @return An instance of [AepText] for the title or null if the title is not present.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepText(
        contentMap: Map<String, Any>,
        textKey: String,
        cardId: String
    ): AepText? {
        val textMap = DataReader.optTypedMap(
            Any::class.java,
            contentMap,
            textKey,
            null
        )
        if (textMap.isNullOrEmpty() || textMap[MessagingConstants.ContentCard.UIKeys.CONTENT] == null) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "$textKey is null in content card with id $cardId"
            )
            return null
        }
        return AepText(
            content = DataReader.optString(
                textMap,
                MessagingConstants.ContentCard.UIKeys.CONTENT,
                null
            )
        )
    }

    /**
     * Creates an instance of [AepImage] from a map of key-value pairs.
     *
     * @param contentMap The map containing the card data.
     * @param cardId The unique identifier for the content card.
     * @return An instance of [AepImage].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepImage(contentMap: Map<String, Any>, cardId: String): AepImage? {
        val imageMap = DataReader.optTypedMap(
            Any::class.java,
            contentMap,
            MessagingConstants.ContentCard.UIKeys.IMAGE,
            null
        )
        if (imageMap.isNullOrEmpty()) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Image is null in content card with id $cardId"
            )
            return null
        }
        return AepImage(
            url = DataReader.optString(imageMap, MessagingConstants.ContentCard.UIKeys.URL, null),
            darkUrl = DataReader.optString(
                imageMap,
                MessagingConstants.ContentCard.UIKeys.DARK_URL,
                null
            )
        )
    }

    /**
     * Creates a list of [AepButton] from a map of key-value pairs.
     *
     * @param contentMap The map containing the card data.
     * @param cardId The unique identifier for the content card.
     * @return A list of [AepButton].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepButtonsList(
        contentMap: Map<String, Any>,
        cardId: String
    ): List<AepButton>? {
        val buttons = DataReader.optTypedListOfMap(
            Any::class.java,
            contentMap,
            MessagingConstants.ContentCard.UIKeys.BUTTONS,
            emptyList()
        )
        if (buttons.isEmpty()) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Buttons are empty in content card with id $cardId"
            )
            return null
        }

        val buttonsList = mutableListOf<AepButton>()
        for (button in buttons) {
            val id = DataReader.optString(
                button,
                MessagingConstants.ContentCard.UIKeys.INTERACT_ID,
                null
            )
            val actionUrl = DataReader.optString(
                button,
                MessagingConstants.ContentCard.UIKeys.ACTION_URL,
                null
            )
            val text = createAepText(
                contentMap = button,
                textKey = MessagingConstants.ContentCard.UIKeys.TEXT,
                cardId = cardId
            )
            if (id == null || actionUrl == null || text == null) {
                Log.trace(
                    MessagingConstants.LOG_TAG,
                    SELF_TAG,
                    "Button id, actionUrl or text is null for button in content card with id $cardId"
                )
                continue
            }
            buttonsList.add(
                AepButton(
                    id = id,
                    actionUrl = actionUrl,
                    text = text
                )
            )
        }
        return buttonsList
    }

    /**
     * Creates an instance of [AepIcon] from a map of key-value pairs.
     *
     * @param contentMap The map containing the card data.
     * @param cardId The unique identifier for the content card.
     * @return An instance of [AepIcon].
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepDismissButton(contentMap: Map<String, Any>, cardId: String): AepIcon? {
        val dismissMap = DataReader.optTypedMap(
            Any::class.java,
            contentMap,
            MessagingConstants.ContentCard.UIKeys.DISMISS_BTN,
            null
        )
        if (dismissMap.isNullOrEmpty()) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Dismiss button is null in content card with id $cardId"
            )
            return null
        }
        val style = DataReader.optString(
            dismissMap,
            MessagingConstants.ContentCard.UIKeys.STYLE,
            MessagingConstants.ContentCard.UIKeys.NONE
        )
        return when (style) {
            MessagingConstants.ContentCard.UIKeys.SIMPLE -> AepIcon(R.drawable.close_filled)
            MessagingConstants.ContentCard.UIKeys.CIRCLE -> AepIcon(R.drawable.cancel_filled)
            else -> null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getActionUrl(contentMap: Map<String, Any>, cardId: String): String? {
        val actionUrl =
            DataReader.optString(contentMap, MessagingConstants.ContentCard.UIKeys.ACTION_URL, null)
        if (actionUrl.isNullOrBlank()) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Action URL is null or empty in content card with id $cardId"
            )
        }
        return actionUrl
    }
}
