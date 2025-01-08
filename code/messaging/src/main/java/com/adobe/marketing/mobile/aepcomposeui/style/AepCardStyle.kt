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
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * Class representing the style for a Card Composable.
 *
 * @param modifier The modifier to be applied to the card.
 * @param shape The shape of the card.
 * @param colors The colors that will be used to resolve the colors for this card in different states
 * @param elevation The elevation of the button.
 * @param border The border to draw around the container of this button.
 */
class AepCardStyle(
    var modifier: Modifier? = null,
    var enabled: Boolean? = null,
    var shape: Shape? = null,
    var colors: CardColors? = null,
    var elevation: CardElevation? = null,
    var border: BorderStroke? = null,
) {
    companion object {

        /**
         * Returns a new [AepCardStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepCardStyle] to be applied to the text element.
         * @param overridingStyle The [AepCardStyle] provided by the app that overrides the default text style.
         *
         * @return The merged [AepCardStyle]
         *
         */
        internal fun merge(
            defaultStyle: AepCardStyle = AepCardStyle(),
            overridingStyle: AepCardStyle? = null
        ): AepCardStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepCardStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                enabled = overridingStyle.enabled ?: defaultStyle.enabled,
                shape = overridingStyle.shape ?: defaultStyle.shape,
                colors = overridingStyle.colors ?: defaultStyle.colors,
                elevation = overridingStyle.elevation ?: defaultStyle.elevation,
                border = overridingStyle.border ?: defaultStyle.border
            )
        }
    }
}
