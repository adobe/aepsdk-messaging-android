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

class AepLazyRowStyle(
    var modifier: Modifier? = null,
    var contentPadding: PaddingValues? = null,
    var reverseLayout: Boolean? = null,
    var horizontalArrangement: Arrangement.Horizontal? = null,
    var verticalAlignment: Alignment.Vertical? = null,
    var flingBehavior: FlingBehavior? = null,
    var userScrollEnabled: Boolean? = null
) {
    companion object {
        internal fun merge(defaultStyle: AepLazyRowStyle, overridingStyle: AepLazyRowStyle?): AepLazyRowStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepLazyRowStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                contentPadding = overridingStyle.contentPadding ?: defaultStyle.contentPadding,
                reverseLayout = overridingStyle.reverseLayout ?: defaultStyle.reverseLayout,
                horizontalArrangement = overridingStyle.horizontalArrangement ?: defaultStyle.horizontalArrangement,
                verticalAlignment = overridingStyle.verticalAlignment ?: defaultStyle.verticalAlignment,
                flingBehavior = overridingStyle.flingBehavior ?: defaultStyle.flingBehavior,
                userScrollEnabled = overridingStyle.userScrollEnabled ?: defaultStyle.userScrollEnabled
            )
        }
    }
}
