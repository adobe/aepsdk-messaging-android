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

package com.adobe.marketing.mobile.aepcomposeui.style

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Class representing the style for an AEP LazyColumn component.
 *
 * @param modifier The modifier for the LazyColumn.
 * @param contentPadding The padding values for the content inside the LazyColumn.
 * @param reverseLayout Whether the layout should be reversed.
 * @param verticalArrangement The vertical arrangement of the items in the LazyColumn.
 * @param horizontalAlignment The horizontal alignment of the items in the LazyColumn.
 * @param flingBehavior The fling behavior for the LazyColumn.
 * @param userScrollEnabled Whether user scrolling is enabled for the LazyColumn.
 */
class AepLazyColumnStyle(
    var modifier: Modifier? = null,
    var contentPadding: PaddingValues? = null,
    var reverseLayout: Boolean? = null,
    var verticalArrangement: Arrangement.Vertical? = null,
    var horizontalAlignment: Alignment.Horizontal? = null,
    var flingBehavior: FlingBehavior? = null,
    var userScrollEnabled: Boolean? = null
) {
    companion object {
        internal fun merge(defaultStyle: AepLazyColumnStyle, overridingStyle: AepLazyColumnStyle?): AepLazyColumnStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepLazyColumnStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                contentPadding = overridingStyle.contentPadding ?: defaultStyle.contentPadding,
                reverseLayout = overridingStyle.reverseLayout ?: defaultStyle.reverseLayout,
                verticalArrangement = overridingStyle.verticalArrangement ?: defaultStyle.verticalArrangement,
                horizontalAlignment = overridingStyle.horizontalAlignment ?: defaultStyle.horizontalAlignment,
                flingBehavior = overridingStyle.flingBehavior ?: defaultStyle.flingBehavior,
                userScrollEnabled = overridingStyle.userScrollEnabled ?: defaultStyle.userScrollEnabled
            )
        }
    }
}
