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
 * Class representing the style for a column AEP UI.
 *
 * @param modifier The modifier for the column.
 * @param verticalArrangement The vertical arrangement for the column.
 * @param horizontalAlignment The horizontal alignment for the column.
 */
class AepColumnStyle(
    var modifier: Modifier? = null,
    var verticalArrangement: Arrangement.Vertical? = null,
    var horizontalAlignment: Alignment.Horizontal? = null
) {
    companion object {
        /**
         * Returns a new [AepColumnStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepColumnStyle] to be applied to the text element.
         * @param overridingStyle The [AepColumnStyle] provided by the app that overrides the default text style.
         *
         * @return The merged [AepColumnStyle]
         */
        internal fun merge(
            defaultStyle: AepColumnStyle = AepColumnStyle(),
            overridingStyle: AepColumnStyle? = null
        ): AepColumnStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepColumnStyle(
                modifier = (defaultStyle.modifier ?: Modifier).then(
                    overridingStyle.modifier ?: Modifier
                ),
                verticalArrangement = overridingStyle.verticalArrangement ?: defaultStyle.verticalArrangement,
                horizontalAlignment = overridingStyle.horizontalAlignment ?: defaultStyle.horizontalAlignment
            )
        }
    }
}
