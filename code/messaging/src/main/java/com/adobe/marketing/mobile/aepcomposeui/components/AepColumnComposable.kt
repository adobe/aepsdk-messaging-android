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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.style.AepColumnStyle

/**
 * A composable function that displays a column element with customizable properties.
 *
 * @param defaultColumnStyle The default [AepColumnStyle] to be applied to the column element.
 * @param overriddenColumnStyle The [AepColumnStyle] provided by the app that overrides the default column style.
 */
@Composable
internal fun AepColumnComposable(
    defaultColumnStyle: AepColumnStyle? = null,
    overriddenColumnStyle: AepColumnStyle? = null,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val mergedStyle = AepColumnStyle.merge(defaultColumnStyle, overriddenColumnStyle)
    Column(
        modifier = (mergedStyle.modifier ?: Modifier).clickable { onClick() },
        verticalArrangement = mergedStyle.verticalArrangement ?: Arrangement.Top,
        horizontalAlignment = mergedStyle.horizontalAlignment ?: Alignment.Start,
        content = content
    )
}
