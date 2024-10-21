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

package com.adobe.marketing.mobile.aepcomposeui.aepui.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Class representing the style of an AEPText Composable.
 *
 * @property modifier The modifier to be applied to the text.
 * @property textStyle The style configuration for the text such as color, font, line height etc.
 * @property overflow The overflow strategy for the text.
 * @property softWrap Whether the text should break at soft line breaks.
 * @property maxLines The maximum number of lines to display.
 * @property minLines The minimum number of lines to display.
 */
class AepTextStyle(
    var modifier: Modifier? = null,
    var textStyle: TextStyle? = null,
    var overflow: TextOverflow? = null,
    var softWrap: Boolean? = null,
    var maxLines: Int? = null,
    var minLines: Int? = null
)

/**
 * Returns a new [AepTextStyle] that is a combination of this style and the other style.
 *
 * other AepTextStyle's null or inherit properties are replaced with the non-null properties of this text style.
 * Another way to think of it is that the "missing" properties of the other style are filled by the properties of this style.
 * If this AepTextStyle is null, returns the other AepTextStyle.
 * If the given AepTextStyle is null, returns this AepTextStyle.
 *
 * @param other The AepTextStyle to merge with this AepTextStyle
 */
@Composable
fun AepTextStyle?.merge(other: AepTextStyle? = null): AepTextStyle? {
    if (this == null) {
        return other
    }
    if (other == null) {
        return this
    }
    return AepTextStyle(
        modifier = (modifier ?: Modifier).then(other.modifier ?: Modifier),
        textStyle = (textStyle ?: TextStyle()).merge(other.textStyle),
        overflow = other.overflow ?: overflow,
        softWrap = other.softWrap ?: softWrap,
        maxLines = other.maxLines ?: maxLines,
        minLines = other.minLines ?: minLines
    )
}
