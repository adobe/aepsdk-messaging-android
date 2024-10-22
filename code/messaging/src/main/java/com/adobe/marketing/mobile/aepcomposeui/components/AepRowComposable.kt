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

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.style.AepRowStyle

/**
 * A composable function that displays a row element with customizable properties.
 *
 * @param defaultRowStyle The default [AepRowStyle] to be applied to the row element.
 * @param overriddenRowStyle The [AepRowStyle] provided by the app that overrides the default row style.
 */
@Composable
internal fun AepRowComposable(
    defaultRowStyle: AepRowStyle? = null,
    overriddenRowStyle: AepRowStyle? = null,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {
    val mergedStyle = AepRowStyle.merge(defaultRowStyle, overriddenRowStyle)
    Row(
        modifier = (mergedStyle.modifier ?: Modifier).then(Modifier.clickable { onClick() }),
        horizontalArrangement = mergedStyle.horizontalArrangement ?: Arrangement.Start,
        verticalAlignment = mergedStyle.verticalAlignment ?: defaultRowStyle?.verticalAlignment ?: Alignment.Top,
        content = content
    )
}
