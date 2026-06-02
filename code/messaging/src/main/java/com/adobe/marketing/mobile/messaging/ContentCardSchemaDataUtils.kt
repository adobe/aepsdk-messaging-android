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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepButton
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplateType
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NamedCollection
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.util.DataReader

/**
 * Utility object for creating instances of AEP model classes from Map data structures.
 * This utility handles parsing and transforming content card data into the relevant AEP UI models,
 * such as [AepText], [AepImage], [AepButton], and [AepIcon], as well as creating
 * the corresponding UI templates like [SmallImageTemplate], [LargeImageTemplate], [ImageOnlyTemplate].
 */
internal object ContentCardSchemaDataUtils {
    private const val SELF_TAG = "ContentCardSchemaDataUtils"
    private const val READ_STATE_NAMED_COLLECTION = "MessagingReadStateCollection"
    private val readStateStoreCollection: NamedCollection?
        get() = ServiceProvider.getInstance().dataStoreService?.getNamedCollection(READ_STATE_NAMED_COLLECTION)

    /**
     * Provides an [AepUI] instance from the given [AepUITemplate].
     *
     * @param uiTemplate The template to convert into a UI component.
     * @return An instance of [AepUI] representing the given template or null in case of unsupported template type.
     */
    internal fun getAepUI(uiTemplate: AepUITemplate, isRead: Boolean? = null): AepUI<*, *>? {
        return when (uiTemplate) {
            is SmallImageTemplate -> {
                SmallImageUI(
                    uiTemplate,
                    SmallImageCardUIState(
                        read = isRead
                    )
                )
            }
            is LargeImageTemplate -> {
                LargeImageUI(
                    uiTemplate,
                    LargeImageCardUIState(
                        read = isRead
                    )
                )
            }
            is ImageOnlyTemplate -> {
                ImageOnlyUI(
                    uiTemplate,
                    ImageOnlyCardUIState(
                        read = isRead
                    )
                )
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
     * Returns a new [AepUI] instance with the same template and state as [ui].
     * Used so that [kotlinx.coroutines.flow.StateFlow] sees a distinct value and emits when state was updated in place.
     */
    internal fun copyAepUI(ui: AepUI<*, *>): AepUI<*, *> = when (ui) {
        is SmallImageUI -> SmallImageUI(ui.getTemplate(), ui.getState())
        is LargeImageUI -> LargeImageUI(ui.getTemplate(), ui.getState())
        is ImageOnlyUI -> ImageOnlyUI(ui.getTemplate(), ui.getState())
    }

    /**
     * Builds an [AepUITemplate] from a given [Proposition].
     *
     * @param proposition The proposition containing the content card data.
     * @return The built [AepUITemplate] or null if the proposition is not a content card or parsing fails.
     */
    internal fun buildTemplate(proposition: Proposition): AepUITemplate? {
        if (!isContentCard(proposition)) return null

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
            val id = contentCardSchemaData.parent.proposition.getActivityId()

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

    /**
     * Retrieves the action URL from the content map of a content card.
     *
     * @param contentMap The map containing the content card data.
     * @param cardId The unique identifier for the content card.
     * @return The action URL as a [String] or null if it is not present or empty.
     */
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

    /**
     * Creates an instance of [InboxTemplate] from a map of key-value pairs.
     *
     * @param proposition The proposition containing the inbox data.
     * @return An instance of [InboxTemplate] or null if required fields are missing or invalid.
     */
    internal fun createInboxTemplate(proposition: Proposition): InboxTemplate? {
        if (!isInbox(proposition)) {
            return null
        }

        val contentMap = proposition.items[0].inboxSchemaData?.content
        val inboxId = proposition.activityId
        if (contentMap.isNullOrEmpty()) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to create inbox template for id: $inboxId, content map is null or empty for Inbox."
            )
            return null
        }
        val heading = createAepText(contentMap, MessagingConstants.Inbox.UIKeys.HEADING, inboxId)
        val layout = createAepInboxLayout(contentMap, inboxId)
        val capacity = DataReader.optInt(
            contentMap,
            MessagingConstants.Inbox.UIKeys.CAPACITY,
            -1
        )
        if (heading == null || heading.content.isEmpty()) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to create inbox template for id: $inboxId, heading is missing or empty."
            )
            return null
        }
        if (layout == null) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to create inbox template for id: $inboxId, layout is missing or invalid."
            )
            return null
        }
        if (capacity <= 0) {
            Log.error(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Failed to create inbox template for id: $inboxId, capacity is missing or invalid."
            )
            return null
        }
        val emptyStateSettings = DataReader.optTypedMap(
            Any::class.java,
            contentMap,
            MessagingConstants.Inbox.UIKeys.EMPTY_STATE_SETTINGS,
            emptyMap()
        )
        val unreadIndicator = DataReader.optTypedMap(
            Any::class.java,
            contentMap,
            MessagingConstants.Inbox.UIKeys.UNREAD_INDICATOR,
            emptyMap()
        )
        val unreadBg = DataReader.optTypedMap(
            Any::class.java,
            unreadIndicator,
            MessagingConstants.Inbox.UIKeys.UNREAD_BG,
            emptyMap()
        )
        return InboxTemplate(
            id = inboxId,
            heading = heading,
            layout = layout,
            capacity = capacity,
            emptyMessage = createAepText(
                emptyStateSettings,
                MessagingConstants.Inbox.UIKeys.MESSAGE,
                inboxId
            ),
            emptyImage = createAepImage(emptyStateSettings, inboxId),
            isUnreadEnabled = DataReader.optBoolean(
                contentMap,
                MessagingConstants.Inbox.UIKeys.IS_UNREAD_ENABLED,
                false
            ),
            unreadBgColor = createAepColor(
                DataReader.optStringMap(
                    unreadBg,
                    MessagingConstants.Inbox.UIKeys.CLR,
                    emptyMap()
                ),
                inboxId
            ),
            unreadIcon = createAepImage(
                DataReader.optTypedMap(
                    Any::class.java,
                    unreadIndicator,
                    MessagingConstants.Inbox.UIKeys.UNREAD_ICON,
                    emptyMap()
                ),
                inboxId
            ),
            unreadIconAlignment = createAlignment(
                DataReader.optString(
                    unreadIndicator,
                    MessagingConstants.Inbox.UIKeys.PLACEMENT,
                    null
                )
            )
        )
    }

    /**
     * Creates an instance of [AepInboxLayout] from a map of key-value pairs.
     *
     * @param contentMap The map containing the inbox data.
     * @param inboxId The unique identifier for the inbox.
     * @return An instance of [AepInboxLayout] or null if the layout is missing or invalid.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepInboxLayout(
        contentMap: Map<String, Any>,
        inboxId: String
    ): AepInboxLayout? {
        val layoutMap = DataReader.optTypedMap(
            Any::class.java,
            contentMap,
            MessagingConstants.Inbox.UIKeys.LAYOUT,
            null
        )
        if (layoutMap.isNullOrEmpty() || layoutMap[MessagingConstants.Inbox.UIKeys.ORIENTATION] == null) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Layout is null in inbox with id $inboxId"
            )
            return null
        }
        return AepInboxLayout.from(
            DataReader.optString(
                layoutMap,
                MessagingConstants.Inbox.UIKeys.ORIENTATION,
                null
            )
        )
    }

    /**
     * Creates an instance of [AepColor] from a map of key-value pairs.
     *
     * @param contentMap The map containing the inbox data.
     * @param colorKey The key for the color in the map.
     * @param inboxId The unique identifier for the inbox.
     * @return An instance of [AepColor] or null if the light color is missing or invalid.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAepColor(
        contentMap: Map<String, Any>,
        inboxId: String
    ): AepColor? {
        val lightColor =
            DataReader.optString(contentMap, MessagingConstants.Inbox.UIKeys.LIGHT, null)
                ?.toComposeColor()
        if (lightColor == null) {
            Log.trace(
                MessagingConstants.LOG_TAG,
                SELF_TAG,
                "Light color is null or invalid in inbox with id $inboxId"
            )
            return null
        }
        val darkColor = DataReader.optString(contentMap, MessagingConstants.Inbox.UIKeys.DARK, null)
            ?.toComposeColor()
        return AepColor(lightColor, darkColor)
    }

    /**
     * Checks if the given [Proposition] is represents inbox.
     *
     * @param proposition The proposition to check.
     * @return `true` if the proposition represents an inbox, `false` otherwise.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun isInbox(proposition: Proposition): Boolean {
        return proposition.items.isNotEmpty() &&
            proposition.items[0]?.schema == SchemaType.INBOX
    }

    /**
     * Creates an instance of [Alignment] from a string value.
     *
     * @param alignment The string representation of the alignment.
     * @return An instance of [Alignment] or null if the alignment is invalid.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createAlignment(alignment: String?): Alignment? {
        return when (alignment?.lowercase()) {
            MessagingConstants.Inbox.UIKeys.TOP_LEFT -> Alignment.TopStart
            MessagingConstants.Inbox.UIKeys.TOP_RIGHT -> Alignment.TopEnd
            MessagingConstants.Inbox.UIKeys.BOTTOM_LEFT -> Alignment.BottomStart
            MessagingConstants.Inbox.UIKeys.BOTTOM_RIGHT -> Alignment.BottomEnd
            else -> null
        }
    }

    /**
     * Sets the read status for a content card activity ID.
     *
     * @param activityId The activity ID of the content card proposition.
     * @param isRead The read status to set (true for read, false for unread).
     */
    internal fun setReadStatus(activityId: String, isRead: Boolean) {
        readStateStoreCollection?.setBoolean(activityId, isRead)
    }

    /**
     * Retrieves the read status for a content card based on the proposition's activity ID.
     *
     * @param activityId The activity ID of the content card proposition.
     * @return The read status (true for read, false for unread) or null if not set.
     */
    internal fun getReadStatus(activityId: String): Boolean? {
        return readStateStoreCollection?.getBoolean(activityId, false)
    }
}

/**
 * Parses a hex color string in `#RRGGBBAA` format into a Compose [Color].
 *
 * @return The parsed [Color], or null if the string is not a valid 6- or 8-digit hex color.
 */
internal fun String.toComposeColor(): Color? {
    val hex = removePrefix("#")
    return try {
        when (hex.length) {
            6 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                val a = hex.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> null
        }
    } catch (e: NumberFormatException) {
        null
    }
}
