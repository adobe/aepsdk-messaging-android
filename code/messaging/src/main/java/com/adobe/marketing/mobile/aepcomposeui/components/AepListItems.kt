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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.ImageOnlyUI
import com.adobe.marketing.mobile.aepcomposeui.LargeImageUI
import com.adobe.marketing.mobile.aepcomposeui.SmallImageUI
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage

/**
 * Renders a list of AEP UI items within a LazyListScope.
 *
 * @param items The list of [AepUI] items to be rendered.
 * @param itemsStyle The style to be applied to the items.
 * @param unreadItemsStyle An optional style to be applied to unread items.
 * @param unreadIcon An optional Triple containing the unread icon [AepImage], its [AepImageStyle], and its [Alignment].
 * @param observer An optional observer that listens to UI events.
 */
fun LazyListScope.renderListItems(
    items: List<AepUI<*, *>>,
    itemsStyle: AepUIStyle,
    unreadItemsStyle: AepUIStyle = itemsStyle,
    unreadIcon: Triple<AepImage, AepImageStyle, Alignment>? = null,
    observer: AepUIEventObserver?
) {
    items(items = items) { aepUI ->
        val state = aepUI.getState()
        if (!state.dismissed) {
            Box(modifier = Modifier.padding(0.dp)) {
                val read = aepUI.getState().read
                when (aepUI) {
                    is SmallImageUI -> {
                        // Use read or unread style based on UI state
                        val style =
                            if (read == false) unreadItemsStyle.smallImageUIStyle else itemsStyle.smallImageUIStyle
                        SmallImageCard(
                            ui = aepUI,
                            style = style,
                            observer = observer
                        )
                    }

                    is LargeImageUI -> {
                        // Use read or unread style based on UI state
                        val style =
                            if (read == false) unreadItemsStyle.largeImageUIStyle else itemsStyle.largeImageUIStyle
                        LargeImageCard(
                            ui = aepUI,
                            style = style,
                            observer = observer
                        )
                    }

                    is ImageOnlyUI -> {
                        // Use read or unread style based on UI state
                        val style =
                            if (read == false) unreadItemsStyle.imageOnlyUIStyle else itemsStyle.imageOnlyUIStyle
                        ImageOnlyCard(
                            ui = aepUI,
                            style = style,
                            observer = observer
                        )
                    }
                }
                // Display unread icon one is provided and if the item is unread
                if (read != null && !read && unreadIcon != null) {
                    Box(modifier = Modifier.align(unreadIcon.third)) {
                        AepAsyncImage(
                            image = unreadIcon.first,
                            imageStyle = unreadIcon.second
                        )
                    }
                }
            }
        }
    }
}
