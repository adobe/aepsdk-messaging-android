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

package com.adobe.marketing.mobile.aepcomposeui.aepui.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepColor

internal object UIUtils {

    /**
     * Converts the [Color] from the [AepColor] based on system dark or light mode.
     * If the color string is not valid, then returns [Color.Unspecified]
     *
     * @param AepColor The AepColor object
     * @return The [Color] object
     */
    @Composable
    internal fun AepColor.getColor(): Color {
        val colorString = if (isSystemInDarkTheme()) darkColour else lightColour
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: IllegalArgumentException) {
            Color.Unspecified
        }
    }
}
