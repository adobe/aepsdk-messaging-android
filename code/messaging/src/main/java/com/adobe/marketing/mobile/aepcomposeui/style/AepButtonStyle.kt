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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * Class representing the style of an AEPButton Composable.
 *
 * @param modifier The modifier to be applied to the button.
 * @param enabled The enabled state of the button.
 * @param elevation The elevation of the button.
 * @param shape The shape of the button.
 * @param border The border to draw around the container of this button.
 * @param colors The colors that will be used to resolve the colors for this button in different states
 * @param contentPadding the spacing values to apply internally between the container and the text
 */
class AepButtonStyle(
    var modifier: Modifier? = null,
    var enabled: Boolean? = null,
    var elevation: ButtonElevation? = null,
    var shape: Shape? = null,
    var border: BorderStroke? = null,
    var colors: ButtonColors? = null,
    var contentPadding: PaddingValues? = null,
)

/**
 * Returns a new [AepButtonStyle] that is a combination of this style and the other style.
 *
 * other AepButtonStyle's null or inherit properties are replaced with the non-null properties of this text style.
 * Another way to think of it is that the "missing" properties of the other style are filled by the properties of this style.
 * If this AepButtonStyle is null, returns the other AepButtonStyle.
 * If the given AepButtonStyle is null, returns this AepButtonStyle.
 *
 * @param other The AepButtonStyle to merge with this AepButtonStyle
 */
@Composable
fun AepButtonStyle?.merge(other: AepButtonStyle? = null): AepButtonStyle? {
    if (this == null) {
        return other
    }
    if (other == null) {
        return this
    }
    return AepButtonStyle(
        modifier = (modifier ?: Modifier).then(other.modifier ?: Modifier),
        enabled = other.enabled ?: enabled,
        elevation = other.elevation ?: elevation,
        shape = other.shape ?: shape,
        border = other.border ?: border,
        colors = other.colors ?: colors,
        contentPadding = other.contentPadding ?: contentPadding
    )
}
