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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants

/**
 * Class representing the style for a small image AEP UI.
 *
 * @param cardStyle The style for the card.
 * @param rootRowStyle The style for the root row.
 * @param imageStyle The style for the image.
 * @param textColumnStyle The style for the column containing the title, body and buttons.
 * @property titleTextStyle The text style for the title.
 * @property bodyTextStyle The text style for the body.
 * @property buttonRowStyle The style for the row containing the buttons.
 * @property buttonStyle The style for the buttons.
 * @property dismissButtonStyle The style for the dismiss button.
 * @property dismissButtonAlignment The alignment for the dismiss button.
 */
class ImageOnlyUIStyle private constructor(
    val imageStyle: AepImageStyle,
    val dismissButtonStyle: AepIconStyle,
    val dismissButtonAlignment: Alignment
) {
    companion object {
        private val defaultImageStyle = AepImageStyle(
            modifier = Modifier.width(AepUIConstants.DefaultStyle.IMAGE_WIDTH.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
        private val defaultDismissButtonStyle = AepIconStyle(
            modifier = Modifier
                .padding(AepUIConstants.DefaultStyle.SPACING.dp)
                .size(AepUIConstants.DISMISS_BUTTON_SIZE.dp)
        )
        private val defaultDismissButtonAlignment = Alignment.TopEnd
    }

    class Builder {
        private var imageStyle: AepImageStyle? = null
        private var dismissButtonStyle: AepIconStyle? = null
        private var dismissButtonAlignment: Alignment? = null

        fun imageStyle(style: AepImageStyle) = apply { this.imageStyle = style }
        fun dismissButtonStyle(style: AepIconStyle) = apply { this.dismissButtonStyle = style }
        fun dismissButtonAlignment(alignment: Alignment) =
            apply { this.dismissButtonAlignment = alignment }

        fun build() = ImageOnlyUIStyle(
            imageStyle = AepImageStyle.merge(defaultImageStyle, imageStyle),
            dismissButtonStyle = AepIconStyle.merge(defaultDismissButtonStyle, dismissButtonStyle),
            dismissButtonAlignment = dismissButtonAlignment ?: defaultDismissButtonAlignment
        )
    }
}
