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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle

/**
 * Composable that renders a vertical scrolling inbox UI.
 *
 * @param ui The [InboxUIState] to be rendered.
 * @param inboxStyle The [InboxUIStyle] to be applied to the inbox.
 * @param readCardsStyle The [AepUIStyle] to be applied to the items within the inbox.
 * @param observer An optional [AepUIEventObserver] to handle UI events.
 */
@Composable
internal fun VerticalInbox(
    ui: InboxUIState.Success,
    inboxStyle: InboxUIStyle,
    readCardsStyle: AepUIStyle,
    unreadCardsStyle: AepUIStyle,
    observer: AepUIEventObserver?
) {
    // Limit the number of items to the specified capacity
    val uiList = ui.items.take(ui.template.capacity)

    if (uiList.isNotEmpty()) {
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
                readItemsStyle = readCardsStyle,
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

private fun getDefaultVerticalArrangement(reverseLayout: Boolean): Arrangement.Vertical =
    if (reverseLayout) Arrangement.Bottom else Arrangement.Top
