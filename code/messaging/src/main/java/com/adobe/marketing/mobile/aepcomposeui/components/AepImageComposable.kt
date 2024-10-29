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

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.adobe.marketing.mobile.aepcomposeui.style.AepImageStyle

/**
 * A composable function that displays a image element with customizable properties.
 *
 * @param content [Painter] representing image to be displayed.
 * @param style The [AepImageStyle] to be applied to the image element.
 * @param onClick Method that is called when this image is clicked
 */
@Composable
internal fun AepImageComposable(
    content: Painter,
    style: AepImageStyle = AepImageStyle(),
    onClick: () -> Unit = {}
) {
    Image(
        painter = content,
        contentDescription = style.contentDescription ?: "",
        modifier = (style.modifier ?: Modifier).clickable { onClick() },
        alignment = style.alignment ?: Alignment.Center,
        contentScale = style.contentScale ?: ContentScale.Fit,
        alpha = style.alpha ?: DefaultAlpha,
        colorFilter = style.colorFilter
    )
}
