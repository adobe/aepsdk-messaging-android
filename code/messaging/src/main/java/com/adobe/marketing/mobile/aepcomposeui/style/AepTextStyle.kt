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
) {
    companion object {

        /**
         * Returns a new [AepTextStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepTextStyle] to be applied to the text element.
         * @param overridingStyle The [AepTextStyle] provided by the app that overrides the default text style.
         */
        internal fun merge(
            defaultStyle: AepTextStyle? = null,
            overridingStyle: AepTextStyle? = null
            /* use when server side styling is added
            serverStyle: AepText? = null, */
        ): AepTextStyle {
            /* use when server side styling is added
            val textColor = serverStyle?.color?.getColor() ?: Color.Unspecified

            // Convert server text alignment from string to TextAlign object
            val textAlign = when (serverStyle?.align?.lowercase()) {
                "left" -> TextAlign.Left
                "center" -> TextAlign.Center
                "right" -> TextAlign.Right
                else -> TextAlign.Unspecified
            }

            // Convert server font properties from string to respective font objects
            // map from `font.name` if needed
            val fontFamily = null
            val fontSize = (serverStyle?.font?.size)?.sp ?: TextUnit.Unspecified
            val fontWeight = when (serverStyle?.font?.weight?.lowercase()) {
                "bold" -> FontWeight.Bold
                else -> null
            }
            val fontStyle = if (serverStyle?.font?.style?.contains("italic") == true) {
                FontStyle.Italic
            } else {
                null
            }

            val serverSideTextStyle = TextStyle(
                color = textColor,
                textAlign = textAlign,
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )*/

            // Merge all text styles together
            val mergedTextStyle = (defaultStyle?.textStyle ?: TextStyle())
                .merge(overridingStyle?.textStyle)

            return AepTextStyle(
                modifier = (defaultStyle?.modifier ?: Modifier).then(
                    overridingStyle?.modifier ?: Modifier
                ),
                textStyle = mergedTextStyle,
                overflow = overridingStyle?.overflow ?: defaultStyle?.overflow,
                softWrap = overridingStyle?.softWrap ?: defaultStyle?.softWrap,
                maxLines = overridingStyle?.maxLines ?: defaultStyle?.maxLines,
                minLines = overridingStyle?.minLines ?: defaultStyle?.minLines
            )
        }
    }
}
