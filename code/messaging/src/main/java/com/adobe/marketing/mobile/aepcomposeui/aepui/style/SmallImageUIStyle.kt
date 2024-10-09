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

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.adobe.marketing.mobile.aepuitemplates.SmallImageTemplate

/**
 * Class representing the style for a small image AEP UI.
 *
 * @property titleTextStyle The text style for the title, if provided.
 */
class SmallImageUIStyle(
    private val titleTextStyle: TextStyle? = null
) {
    private var defaultTitleColor = Color.Red

    /**
     * Returns the text style for the title, merging the default style with the provided one.
     *
     * @param template The small image template used to determine the style.
     * @return The merged text style for the title.
     */
    @Composable
    fun getTitleTextStyle(template: SmallImageTemplate): TextStyle {
        return MaterialTheme.typography.body1.copy(defaultTitleColor)
            .merge(titleTextStyle)
    }
}
