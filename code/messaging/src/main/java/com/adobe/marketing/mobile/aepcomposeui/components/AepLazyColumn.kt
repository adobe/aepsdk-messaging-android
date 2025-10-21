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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.style.AepLazyColumnStyle

/**
 * A composable function that displays a lazy column element with customizable properties.
 *
 * @param lazyColumnStyle The [AepLazyColumnStyle] to be applied to the lazy column element.
 * @param content The content of the lazy column.
 */
@Composable
internal fun AepLazyColumn(
    lazyColumnStyle: AepLazyColumnStyle = AepLazyColumnStyle(),
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = lazyColumnStyle.modifier ?: Modifier,
        contentPadding = lazyColumnStyle.contentPadding ?: PaddingValues(0.dp),
        reverseLayout = lazyColumnStyle.reverseLayout ?: false,
        verticalArrangement = lazyColumnStyle.verticalArrangement ?: getDefaultVerticalArrangement(lazyColumnStyle.reverseLayout ?: false),
        horizontalAlignment = lazyColumnStyle.horizontalAlignment ?: Alignment.Start,
        flingBehavior = lazyColumnStyle.flingBehavior ?: ScrollableDefaults.flingBehavior(),
        userScrollEnabled = lazyColumnStyle.userScrollEnabled ?: true,
        content = content
    )
}

/**
 * Helper function to get the default vertical arrangement based on reverse layout.
 */
private fun getDefaultVerticalArrangement(reverseLayout: Boolean): Arrangement.Vertical =
    if (reverseLayout) Arrangement.Bottom else Arrangement.Top
