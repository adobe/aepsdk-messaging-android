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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Class representing the style for a row AEP UI.
 *
 * @param modifier The modifier for the row.
 * @param horizontalArrangement The horizontal arrangement for the row.
 * @param verticalAlignment The vertical alignment for the row.
 */
class AepRowStyle(
    var modifier: Modifier? = null,
    var horizontalArrangement: Arrangement.Horizontal? = null,
    var verticalAlignment: Alignment.Vertical? = null,
) {
    companion object {
        /**
         * Returns a new [AepRowStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepRowStyle] to be applied to the text element.
         * @param overridingStyle The [AepRowStyle] provided by the app that overrides the default text style.
         *
         * @return The merged [AepRowStyle]
         */
        internal fun merge(
            defaultStyle: AepRowStyle = AepRowStyle(),
            overridingStyle: AepRowStyle? = null
        ): AepRowStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }

            return AepRowStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                horizontalArrangement = overridingStyle.horizontalArrangement ?: defaultStyle.horizontalArrangement,
                verticalAlignment = overridingStyle.verticalAlignment ?: defaultStyle.verticalAlignment
            )
        }
    }
}
