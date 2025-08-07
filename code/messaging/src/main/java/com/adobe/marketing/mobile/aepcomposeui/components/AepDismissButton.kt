/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.aepcomposeui.style.AepIconStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepIcon

/**
 * A composable function to render a dismiss button with an icon.
 *
 * @param modifier Modifier to be applied to the dismiss button.
 * @param dismissIcon The icon to be displayed on the dismiss button.
 * @param style The style to be applied to the icon.
 * @param onClick Callback function to be invoked when the button is clicked.
 */
@Composable
internal fun AepDismissButton(
    modifier: Modifier,
    dismissIcon: AepIcon?,
    style: AepIconStyle = AepIconStyle(),
    onClick: () -> Unit = {},
) {
    dismissIcon?.let {
        AepIconComposable(
            model = dismissIcon,
            iconStyle = style.apply {
                this.modifier = (this.modifier ?: Modifier).then(modifier)
                    .clickable { onClick() }
            }
        )
    }
}
