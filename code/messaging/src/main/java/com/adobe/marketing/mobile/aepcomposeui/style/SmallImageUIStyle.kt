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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
class SmallImageUIStyle private constructor(
    val cardStyle: AepCardStyle,
    val rootRowStyle: AepRowStyle,
    val imageStyle: AepImageStyle,
    val textColumnStyle: AepColumnStyle,
    val titleTextStyle: AepTextStyle,
    val bodyTextStyle: AepTextStyle,
    val buttonRowStyle: AepRowStyle,
    val buttonStyle: Array<AepButtonStyle>,
    val dismissButtonStyle: AepIconStyle,
    val dismissButtonAlignment: Alignment
) {
    companion object {
        private val defaultCardStyle = AepCardStyle(
            modifier = Modifier.padding(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp)
        )
        private val defaultRootRowStyle = AepRowStyle(
            modifier = Modifier.padding(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp),
            horizontalArrangement = Arrangement.spacedBy(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        private val defaultImageStyle = AepImageStyle(
            modifier = Modifier.width(AepUIConstants.SmallImageCard.DefaultStyle.IMAGE_WIDTH.dp),
            alignment = Alignment.Center
        )
        private val defaultTextColumnStyle = AepColumnStyle(
            verticalArrangement = Arrangement.spacedBy(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp)
        )
        private val defaultTitleAepTextStyle = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.SmallImageCard.DefaultStyle.TITLE_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.SmallImageCard.DefaultStyle.TITLE_FONT_WEIGHT
            )
        )
        private val defaultBodyAepTextStyle = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.SmallImageCard.DefaultStyle.BODY_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.SmallImageCard.DefaultStyle.BODY_FONT_WEIGHT,
            )
        )
        private val defaultButtonRowStyle = AepRowStyle(
            horizontalArrangement = Arrangement.spacedBy(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp)
        )
        private val defaultButtonTextStyle = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.SmallImageCard.DefaultStyle.BUTTON_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.SmallImageCard.DefaultStyle.BUTTON_FONT_WEIGHT,
            )
        )
        private val defaultButtonStyle = AepButtonStyle(
            textStyle = defaultButtonTextStyle
        )
        private val defaultDismissButtonStyle = AepIconStyle(
            modifier = Modifier
                .padding(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp)
                .size(AepUIConstants.DISMISS_BUTTON_SIZE.dp)
        )
        private val defaultDismissButtonAlignment = Alignment.TopEnd
    }

    class Builder {
        private var cardStyle: AepCardStyle? = null
        private var rootRowStyle: AepRowStyle? = null
        private var imageStyle: AepImageStyle? = null
        private var textColumnStyle: AepColumnStyle? = null
        private var titleAepTextStyle: AepTextStyle? = null
        private var bodyAepTextStyle: AepTextStyle? = null
        private var buttonRowStyle: AepRowStyle? = null
        private var buttonStyle: Array<AepButtonStyle?> = arrayOfNulls(3)
        private var dismissButtonStyle: AepIconStyle? = null
        private var dismissButtonAlignment: Alignment? = null

        fun cardStyle(style: AepCardStyle) = apply { this.cardStyle = style }
        fun rootRowStyle(style: AepRowStyle) = apply { this.rootRowStyle = style }
        fun imageStyle(style: AepImageStyle) = apply { this.imageStyle = style }
        fun textColumnStyle(style: AepColumnStyle) = apply { this.textColumnStyle = style }
        fun titleAepTextStyle(style: AepTextStyle) = apply { this.titleAepTextStyle = style }
        fun bodyAepTextStyle(style: AepTextStyle) = apply { this.bodyAepTextStyle = style }
        fun buttonRowStyle(style: AepRowStyle) = apply { this.buttonRowStyle = style }
        fun buttonStyle(style: Array<AepButtonStyle?>) = apply { this.buttonStyle = style }
        fun dismissButtonStyle(style: AepIconStyle) = apply { this.dismissButtonStyle = style }
        fun dismissButtonAlignment(alignment: Alignment) =
            apply { this.dismissButtonAlignment = alignment }

        fun build() = SmallImageUIStyle(
            cardStyle = AepCardStyle.merge(defaultCardStyle, cardStyle),
            rootRowStyle = AepRowStyle.merge(defaultRootRowStyle, rootRowStyle),
            imageStyle = AepImageStyle.merge(defaultImageStyle, imageStyle),
            textColumnStyle = AepColumnStyle.merge(defaultTextColumnStyle, textColumnStyle),
            titleTextStyle = AepTextStyle.merge(defaultTitleAepTextStyle, titleAepTextStyle),
            bodyTextStyle = AepTextStyle.merge(defaultBodyAepTextStyle, bodyAepTextStyle),
            buttonRowStyle = AepRowStyle.merge(defaultButtonRowStyle, buttonRowStyle),
            buttonStyle = buttonStyle.map {
                AepButtonStyle.merge(defaultButtonStyle, it)
            }.toTypedArray(),
            dismissButtonStyle = AepIconStyle.merge(defaultDismissButtonStyle, dismissButtonStyle),
            dismissButtonAlignment = dismissButtonAlignment ?: defaultDismissButtonAlignment
        )
    }
}
