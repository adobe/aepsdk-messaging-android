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

package com.adobe.marketing.mobile.aepcomposeui.utils

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.services.ServiceProvider

internal object UIUtils {

    /**
     * Converts the [Color] from the [AepColor] based on system dark or light mode.
     * If the color string is not valid, then returns [Color.Unspecified]
     *
     * @param AepColor The AepColor object
     * @return The [Color] object
     */
    internal fun AepColor.getColor(): Color {
        val context = ServiceProvider.getInstance().appContextService.applicationContext
        context?.let {
            val colorString = if (isSystemInDarkTheme(context)) darkColour else lightColour
            return try {
                Color(android.graphics.Color.parseColor(colorString))
            } catch (e: IllegalArgumentException) {
                Color.Unspecified
            }
        } ?: return Color.Unspecified
    }

    internal fun isSystemInDarkTheme(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
