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
 * @param buttonTextStyle The text style for the button.
 */
class AepButtonStyle(
    var modifier: Modifier? = null,
    var enabled: Boolean? = null,
    var elevation: ButtonElevation? = null,
    var shape: Shape? = null,
    var border: BorderStroke? = null,
    var colors: ButtonColors? = null,
    var contentPadding: PaddingValues? = null,
    var buttonTextStyle: AepTextStyle? = null
) {

    companion object {

        /**
         * Returns a new [AepButtonStyle] that is a combination of default style and overridden style.
         * If the same property is present in all the styles, the property from the overridden style is used.
         * If a property is not present in the overridden style, the property from the default style is used.
         *
         * @param defaultStyle The default [AepButtonStyle] to be applied to the button element.
         * @param overridingStyle The [AepButtonStyle] provided by the app that overrides the default button style.
         *
         * @return The merged [AepButtonStyle]
         */
        internal fun merge(
            defaultStyle: AepButtonStyle = AepButtonStyle(),
            overridingStyle: AepButtonStyle? = null
        ): AepButtonStyle {
            if (overridingStyle == null) {
                return defaultStyle
            }
            return AepButtonStyle(
                modifier = overridingStyle.modifier ?: defaultStyle.modifier,
                enabled = overridingStyle.enabled ?: defaultStyle.enabled,
                elevation = overridingStyle.elevation ?: defaultStyle.elevation,
                shape = overridingStyle.shape ?: defaultStyle.shape,
                border = overridingStyle.border ?: defaultStyle.border,
                colors = overridingStyle.colors ?: defaultStyle.colors,
                contentPadding = overridingStyle.contentPadding ?: defaultStyle.contentPadding,
                buttonTextStyle = AepTextStyle.merge(defaultStyle.buttonTextStyle ?: AepTextStyle(), overridingStyle.buttonTextStyle)
            )
        }
    }
}
