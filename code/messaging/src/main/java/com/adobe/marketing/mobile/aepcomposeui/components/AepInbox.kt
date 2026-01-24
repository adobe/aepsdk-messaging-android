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
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout

/**
 * AepInboxComposable that renders the inbox based on the properties of the provided [InboxUIState].
 *
 * @param uiState The [InboxUIState] model to be rendered.
 * @param inboxStyle The style to be applied to the inbox.
 * @param itemsStyle The style to be applied to the cards within the inbox.
 * @param observer An optional event listener for content card UI events.
 */
@Composable
fun AepInbox(
    uiState: InboxUIState,
    inboxStyle: InboxUIStyle = InboxUIStyle.Builder().build(),
    itemsStyle: AepUIStyle = AepUIStyle(),
    observer: AepUIEventObserver? = null
) {
    when (uiState) {
        is InboxUIState.Loading -> {
            inboxStyle.loadingView()
        }

        is InboxUIState.Error -> {
            inboxStyle.errorView()
        }

        is InboxUIState.Success -> {
            Column {

                // Wrap AepText in an invisible Surface to provide Material Theme context
                Surface(
                    color = Color.Transparent
                ) {
                    AepText(
                        model = uiState.template.heading,
                        textStyle = inboxStyle.headingStyle
                    )
                }
                if (uiState.items.isEmpty()) {
                    EmptyInbox(
                        uiState.template.emptyMessage,
                        inboxStyle.emptyMessageStyle,
                        uiState.template.emptyImage,
                        inboxStyle.emptyImageStyle
                    )
                } else {
                    // Determine unread card background color based on theme, server settings, and style overrides
                    val isDarkTheme = isSystemInDarkTheme()
                    val unreadCardColor = remember(
                        isDarkTheme,
                        inboxStyle.unreadBgColor,
                        uiState.template.unreadBgColor
                    ) {
                        if (isDarkTheme) {
                            inboxStyle.unreadBgColor?.dark
                                ?: uiState.template.unreadBgColor?.dark
                        } else {
                            inboxStyle.unreadBgColor?.light
                                ?: uiState.template.unreadBgColor?.light
                        }
                    }

                    // Create unread style variant if unread color is specified
                    val unreadCardsStyle = createUnreadCardsStyle(itemsStyle, unreadCardColor)

                    when (uiState.template.layout) {
                        AepInboxLayout.VERTICAL -> {
                            VerticalInbox(
                                ui = uiState,
                                inboxStyle = inboxStyle,
                                readCardsStyle = itemsStyle,
                                unreadCardsStyle = unreadCardsStyle,
                                observer = observer
                            )
                        }

                        AepInboxLayout.HORIZONTAL -> {
                            HorizontalInbox(
                                ui = uiState,
                                inboxStyle = inboxStyle,
                                readCardsStyle = itemsStyle,
                                unreadCardsStyle = unreadCardsStyle,
                                observer = observer
                            )
                        }
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
