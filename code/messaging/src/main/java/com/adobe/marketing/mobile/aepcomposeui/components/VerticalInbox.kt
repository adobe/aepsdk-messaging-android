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

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ImageOnlyUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.LargeImageUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.SmallImageUIStyle

/**
 * Composable that renders a vertical scrolling inbox UI.
 *
 * @param ui The [InboxUIState] to be rendered.
 * @param inboxStyle The [InboxUIStyle] to be applied to the inbox.
 * @param itemsStyle The [AepUIStyle] to be applied to the items within the inbox.
 * @param observer An optional [AepUIEventObserver] to handle UI events.
 */
@Composable
internal fun VerticalInbox(
    ui: InboxUIState.Success,
    inboxStyle: InboxUIStyle,
    itemsStyle: AepUIStyle,
    observer: AepUIEventObserver?
) {
    // Limit the number of items to the specified capacity
    val uiList = ui.items.take(ui.template.capacity)

    // Determine unread card background color based on theme, server settings, and style overrides
    val isDarkTheme = isSystemInDarkTheme()
    val unreadCardColor = remember(
        isDarkTheme,
        inboxStyle.unreadBgColor,
        ui.template.unreadBgColor
    ) {
        if (isDarkTheme) {
            inboxStyle.unreadBgColor?.darkColor
                ?: ui.template.unreadBgColor?.darkColor
        } else {
            inboxStyle.unreadBgColor?.lightColor
                ?: ui.template.unreadBgColor?.lightColor
        }
    }

    Column {
        // Wrap AepText in an invisible Surface to provide Material Theme context
        Surface(
            color = Color.Transparent
        ) {
            AepText(
                model = ui.template.heading,
                textStyle = inboxStyle.headingStyle
            )
        }

        // Create unread style variant if unread color is specified
        val unreadCardsStyle = createUnreadCardsStyle(itemsStyle, unreadCardColor)

        if (uiList.isEmpty()) {
            EmptyInbox(
                ui.template.emptyMessage,
                inboxStyle.emptyMessageStyle,
                ui.template.emptyImage,
                inboxStyle.emptyImageStyle
            )
        } else {
            val lazyColumnStyle = inboxStyle.lazyColumnStyle
            LazyColumn(
                modifier = lazyColumnStyle.modifier ?: Modifier,
                contentPadding = lazyColumnStyle.contentPadding ?: PaddingValues(0.dp),
                reverseLayout = lazyColumnStyle.reverseLayout ?: false,
                verticalArrangement = lazyColumnStyle.verticalArrangement
                    ?: getDefaultVerticalArrangement(lazyColumnStyle.reverseLayout ?: false),
                horizontalAlignment = lazyColumnStyle.horizontalAlignment ?: Alignment.Start,
                flingBehavior = lazyColumnStyle.flingBehavior ?: ScrollableDefaults.flingBehavior(),
                userScrollEnabled = lazyColumnStyle.userScrollEnabled ?: true
            ) {
                renderListItems(
                    items = uiList,
                    itemsStyle = itemsStyle,
                    unreadItemsStyle = unreadCardsStyle,
                    unreadIcon = if (ui.template.unreadIcon != null) Triple(
                        ui.template.unreadIcon,
                        inboxStyle.unreadIconStyle,
                        inboxStyle.unreadIconAlignment
                            ?: ui.template.unreadIconAlignment ?: Alignment.TopStart
                    )
                    else null,
                    observer = observer
                )
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

private fun getDefaultVerticalArrangement(reverseLayout: Boolean): Arrangement.Vertical =
    if (reverseLayout) Arrangement.Bottom else Arrangement.Top
