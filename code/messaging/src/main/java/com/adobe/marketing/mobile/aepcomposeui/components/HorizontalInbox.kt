/*
  Copyright 2026 Adobe. All rights reserved.
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle

@Composable
internal fun HorizontalInbox(
    ui: InboxUIState.Success,
    inboxStyle: InboxUIStyle,
    readCardsStyle: AepUIStyle,
    unreadCardsStyle: AepUIStyle,
    observer: AepUIEventObserver?
) {
    // Limit the number of items to the specified capacity
    val uiList = ui.items.take(ui.template.capacity)

    if (uiList.isNotEmpty()) {
        val lazyRowStyle = inboxStyle.lazyRowStyle
        LazyRow(
            modifier = lazyRowStyle.modifier ?: Modifier,
            contentPadding = lazyRowStyle.contentPadding ?: PaddingValues(0.dp),
            reverseLayout = lazyRowStyle.reverseLayout ?: false,
            horizontalArrangement = lazyRowStyle.horizontalArrangement
                ?: getDefaultHorizontalArrangement(lazyRowStyle.reverseLayout ?: false),
            verticalAlignment = lazyRowStyle.verticalAlignment ?: Alignment.Top,
            flingBehavior = lazyRowStyle.flingBehavior ?: ScrollableDefaults.flingBehavior(),
            userScrollEnabled = lazyRowStyle.userScrollEnabled ?: true
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

private fun getDefaultHorizontalArrangement(reverseLayout: Boolean): Arrangement.Horizontal =
    if (reverseLayout) Arrangement.End else Arrangement.Start
