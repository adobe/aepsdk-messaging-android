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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.style.AepCardStyle

/**
 * A composable function that displays a card element with customizable properties.
 *
 * @param cardStyle The [AepCardStyle] to be applied to the card element.
 * @param onClick Method that is called when this card is clicked
 * @param content The content of the card.
 */
@Composable
internal fun AepCardComposable(
    cardStyle: AepCardStyle = AepCardStyle(),
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = (cardStyle.modifier ?: Modifier).then(Modifier.clickable { onClick() }),
        shape = cardStyle.shape ?: CardDefaults.shape,
        colors = cardStyle.colors ?: CardDefaults.cardColors(),
        elevation = cardStyle.elevation ?: CardDefaults.elevatedCardElevation(),
        border = cardStyle.border,
        content = content
    )
}
