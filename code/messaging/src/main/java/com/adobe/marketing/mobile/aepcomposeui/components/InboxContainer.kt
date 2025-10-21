/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.aepcomposeui.InboxContainerUI
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxContainerUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText

/**
 * Composable that renders the inbox container UI.
 *
 * @param ui The [InboxContainerUI] to be rendered.
 * @param inboxContainerStyle The [InboxContainerUIStyle] to be applied to the inbox container.
 * @param itemsStyle The [AepUIStyle] to be applied to the items within the inbox container.
 * @param observer An optional [AepUIEventObserver] to handle UI events.
 */
@Composable
internal fun InboxContainer(
    ui: InboxContainerUI,
    inboxContainerStyle: InboxContainerUIStyle,
    itemsStyle: AepUIStyle,
    observer: AepUIEventObserver?
) {
    val inboxContainerSettings = ui.getTemplate()
    val inboxContainerState = ui.getState()
    when (inboxContainerState) {
        is InboxContainerUIState.Loading -> {
            inboxContainerStyle.loadingIndicator()
        }

        is InboxContainerUIState.Error -> {
            EmptyInboxContainer(
                // todo: Replace these with server provided error message and style if we add it to the container settings UI
                AepText("Error fetching messages"),
                inboxContainerStyle.emptyMessageStyle
            )
        }

        is InboxContainerUIState.Success -> {
            // Limit the number of items to the specified capacity
            val uiList = inboxContainerState.items.take(inboxContainerSettings.capacity)

            // Determine unread card background color based on theme, server settings, and style overrides
            val isDarkTheme = isSystemInDarkTheme()
            val unreadCardColor = remember(
                isDarkTheme,
                inboxContainerStyle.unreadBgColor,
                inboxContainerSettings.unreadBgColor
            ) {
                if (isDarkTheme) {
                    inboxContainerStyle.unreadBgColor?.darkColor
                        ?: inboxContainerSettings.unreadBgColor?.darkColor
                } else {
                    inboxContainerStyle.unreadBgColor?.lightColor
                        ?: inboxContainerSettings.unreadBgColor?.lightColor
                }
            }

            Column {
                // Wrap AepText in an invisible Surface to provide Material Theme context
                Surface(
                    color = Color.Transparent
                ) {
                    AepText(
                        model = inboxContainerSettings.heading,
                        textStyle = inboxContainerStyle.headingStyle
                    )
                }

                // Create unread style variant if unread color is specified
                val unreadCardsStyle = createUnreadCardsStyle(itemsStyle, unreadCardColor)

                if (uiList.isEmpty()) {
                    EmptyInboxContainer(
                        inboxContainerSettings.emptyMessage,
                        inboxContainerStyle.emptyMessageStyle,
                        inboxContainerSettings.emptyImage,
                        inboxContainerStyle.emptyImageStyle
                    )
                } else {
                    AepLazyColumn(
                        lazyColumnStyle = inboxContainerStyle.lazyColumnStyle
                    ) {
                        renderListItems(
                            items = uiList,
                            itemsStyle = itemsStyle,
                            unreadItemsStyle = unreadCardsStyle,
                            unreadIcon = if (inboxContainerSettings.unreadIcon != null) Triple(
                                inboxContainerSettings.unreadIcon,
                                inboxContainerStyle.unreadIconStyle,
                                inboxContainerStyle.unreadIconAlignment
                                    ?: inboxContainerSettings.unreadIconAlignment ?: Alignment.TopStart
                            )
                            else null,
                            observer = observer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates unread card styles with the specified unread color, or returns the original styles if no unread color is provided.
 */
@Composable
private fun createUnreadCardsStyle(cardsStyle: AepUIStyle, unreadCardColor: Color?): AepUIStyle {
    if (unreadCardColor == null) {
        return cardsStyle
    }

    val unreadSmallImageStyle = createUnreadSmallImageStyle(cardsStyle.smallImageUIStyle, unreadCardColor)
    val unreadLargeImageStyle = createUnreadLargeImageStyle(cardsStyle.largeImageUIStyle, unreadCardColor)
    val unreadImageOnlyStyle = createUnreadImageOnlyStyle(cardsStyle.imageOnlyUIStyle, unreadCardColor)

    return AepUIStyle(
        smallImageUIStyle = unreadSmallImageStyle,
        largeImageUIStyle = unreadLargeImageStyle,
        imageOnlyUIStyle = unreadImageOnlyStyle
    )
}

@Composable
private fun createUnreadSmallImageStyle(originalStyle: SmallImageUIStyle, unreadCardColor: Color): SmallImageUIStyle {
    val unreadCardStyle = AepCardStyle(
        modifier = originalStyle.cardStyle.modifier,
        enabled = originalStyle.cardStyle.enabled,
        shape = originalStyle.cardStyle.shape,
        colors = CardDefaults.cardColors(unreadCardColor),
        elevation = originalStyle.cardStyle.elevation,
        border = originalStyle.cardStyle.border
    )
    return SmallImageUIStyle.Builder()
        .cardStyle(unreadCardStyle)
        .imageStyle(originalStyle.imageStyle)
        .rootRowStyle(originalStyle.rootRowStyle)
        .textColumnStyle(originalStyle.textColumnStyle)
        .titleAepTextStyle(originalStyle.titleTextStyle)
        .bodyAepTextStyle(originalStyle.bodyTextStyle)
        .buttonRowStyle(originalStyle.buttonRowStyle)
        .buttonStyle(originalStyle.buttonStyle.map { it }.toTypedArray())
        .dismissButtonStyle(originalStyle.dismissButtonStyle)
        .dismissButtonAlignment(originalStyle.dismissButtonAlignment)
        .build()
}

@Composable
private fun createUnreadLargeImageStyle(originalStyle: LargeImageUIStyle, unreadCardColor: Color): LargeImageUIStyle {
    val unreadCardStyle = AepCardStyle(
        modifier = originalStyle.cardStyle.modifier,
        enabled = originalStyle.cardStyle.enabled,
        shape = originalStyle.cardStyle.shape,
        colors = CardDefaults.cardColors(unreadCardColor),
        elevation = originalStyle.cardStyle.elevation,
        border = originalStyle.cardStyle.border
    )
    return LargeImageUIStyle.Builder()
        .cardStyle(unreadCardStyle)
        .imageStyle(originalStyle.imageStyle)
        .rootColumnStyle(originalStyle.rootColumnStyle)
        .textColumnStyle(originalStyle.textColumnStyle)
        .titleAepTextStyle(originalStyle.titleTextStyle)
        .bodyAepTextStyle(originalStyle.bodyTextStyle)
        .buttonRowStyle(originalStyle.buttonRowStyle)
        .buttonStyle(originalStyle.buttonStyle.map { it }.toTypedArray())
        .dismissButtonStyle(originalStyle.dismissButtonStyle)
        .dismissButtonAlignment(originalStyle.dismissButtonAlignment)
        .build()
}

@Composable
private fun createUnreadImageOnlyStyle(originalStyle: ImageOnlyUIStyle, unreadCardColor: Color): ImageOnlyUIStyle {
    val unreadCardStyle = AepCardStyle(
        modifier = originalStyle.cardStyle.modifier,
        enabled = originalStyle.cardStyle.enabled,
        shape = originalStyle.cardStyle.shape,
        colors = CardDefaults.cardColors(unreadCardColor),
        elevation = originalStyle.cardStyle.elevation,
        border = originalStyle.cardStyle.border
    )
    return ImageOnlyUIStyle.Builder()
        .cardStyle(unreadCardStyle)
        .imageStyle(originalStyle.imageStyle)
        .dismissButtonStyle(originalStyle.dismissButtonStyle)
        .dismissButtonAlignment(originalStyle.dismissButtonAlignment)
        .build()
}
