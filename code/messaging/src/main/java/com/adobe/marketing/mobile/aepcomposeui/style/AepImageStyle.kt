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

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale

/**
 * Class representing the style for an AepImage composable.
 *
 * @param modifier The modifier for the image.
 * @param contentDescription The content description for the image.
 * @param alignment The alignment for the image.
 * @param contentScale The content scale for the image.
 * @param alpha The alpha value for the image.
 * @param colorFilter The color filter for the image.
 */
class AepImageStyle(
    var modifier: Modifier? = null,
    var contentDescription: String? = null,
    var alignment: Alignment? = null,
    var contentScale: ContentScale? = null,
    var alpha: Float? = null,
    var colorFilter: ColorFilter? = null
) {
    companion object {

        /**
         * Returns a new [AepImageStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepImageStyle] to be applied to the text element.
         * @param overridingStyle The [AepImageStyle] provided by the app that overrides the default text style.
         *
         * @return The merged [AepImageStyle]
         *
         */
        internal fun merge(
            defaultStyle: AepImageStyle = AepImageStyle(),
            overridingStyle: AepImageStyle? = null
        ): AepImageStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepImageStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                contentDescription = overridingStyle.contentDescription ?: defaultStyle.contentDescription,
                alignment = overridingStyle.alignment ?: defaultStyle.alignment,
                contentScale = overridingStyle.contentScale ?: defaultStyle.contentScale,
                alpha = overridingStyle.alpha ?: defaultStyle.alpha,
                colorFilter = overridingStyle.colorFilter ?: defaultStyle.colorFilter
            )
        }
    }
}
