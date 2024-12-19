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
import androidx.compose.ui.graphics.Color

/**
 * Class representing the style for an AepIcon composable.
 *
 * @param modifier The modifier for the icon.
 * @param contentDescription The content description for the icon.
 * @param tint The tint color for the icon.
 *
 */
class AepIconStyle(
    var modifier: Modifier? = null,
    var contentDescription: String? = null,
    var tint: Color? = null
) {
    companion object {

        /**
         * Returns a new [AepIconStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepIconStyle] to be applied to the text element.
         * @param overridingStyle The [AepIconStyle] provided by the app that overrides the default text style.
         *
         * @return The merged [AepIconStyle]
         *
         */
        internal fun merge(
            defaultStyle: AepIconStyle = AepIconStyle(),
            overridingStyle: AepIconStyle? = null
        ): AepIconStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepIconStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                contentDescription = overridingStyle.contentDescription ?: defaultStyle.contentDescription,
                tint = overridingStyle.tint ?: defaultStyle.tint
            )
        }
    }
}
