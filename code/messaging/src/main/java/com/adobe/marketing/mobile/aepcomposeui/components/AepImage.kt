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
 * @param imageStyle The [AepImageStyle] to be applied to the image element.
 */
@Composable
internal fun AepImage(
    content: Painter,
    imageStyle: AepImageStyle = AepImageStyle()
) {
    Image(
        painter = content,
        contentDescription = imageStyle.contentDescription ?: "",
        modifier = imageStyle.modifier ?: Modifier,
        alignment = imageStyle.alignment ?: Alignment.Center,
        contentScale = imageStyle.contentScale ?: ContentScale.Fit,
        alpha = imageStyle.alpha ?: DefaultAlpha,
        colorFilter = imageStyle.colorFilter
    )
}
