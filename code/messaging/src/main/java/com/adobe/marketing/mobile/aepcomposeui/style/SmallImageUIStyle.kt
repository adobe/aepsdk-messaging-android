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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants

/**
 * Class representing the style for a small image AEP UI.
 *
 * @property titleAepTextStyle The custom text style for the title.
 * @property bodyAepTextStyle The custom text style for the body.
 * @property buttonStyle The custom style for the buttons.
 */
class SmallImageUIStyle private constructor(
    val smallImageCardStyle: AepCardStyle,
    val rootRowStyle: AepRowStyle,
    val textColumnStyle: AepColumnStyle,
    val titleAepTextStyle: AepTextStyle,
    val bodyAepTextStyle: AepTextStyle,
    val buttonRowStyle: AepRowStyle,
    val buttonStyle: Array<Pair<AepButtonStyle, AepTextStyle>>
) {
    companion object {
        private val defaultSmallImageCardStyle = AepCardStyle(
            modifier = Modifier.padding(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp)
        )
        private val defaultRootRowStyle = AepRowStyle(
            modifier = Modifier.padding(AepUIConstants.SmallImageCard.DefaultStyle.SPACING.dp)
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
        private val defaultButtonStyle = AepButtonStyle()
    }

    class Builder {
        private var smallImageCardStyle: AepCardStyle? = null
        private var rootRowStyle: AepRowStyle? = null
        private var textColumnStyle: AepColumnStyle? = null
        private var titleAepTextStyle: AepTextStyle? = null
        private var bodyAepTextStyle: AepTextStyle? = null
        private var buttonRowStyle: AepRowStyle? = null
        private var buttonStyle: Array<Pair<AepButtonStyle?, AepTextStyle?>?>? = arrayOfNulls(3)

        fun smallImageCardStyle(style: AepCardStyle) = apply { this.smallImageCardStyle = style }
        fun rootRowStyle(style: AepRowStyle) = apply { this.rootRowStyle = style }
        fun textColumnStyle(style: AepColumnStyle) = apply { this.textColumnStyle = style }
        fun titleAepTextStyle(style: AepTextStyle) = apply { this.titleAepTextStyle = style }
        fun bodyAepTextStyle(style: AepTextStyle) = apply { this.bodyAepTextStyle = style }
        fun buttonRowStyle(style: AepRowStyle) = apply { this.buttonRowStyle = style }
        fun buttonStyle(style: Array<Pair<AepButtonStyle?, AepTextStyle?>?>) = apply { this.buttonStyle = style }

        fun build() = SmallImageUIStyle(
            smallImageCardStyle = AepCardStyle.merge(defaultSmallImageCardStyle, smallImageCardStyle),
            rootRowStyle = AepRowStyle.merge(defaultRootRowStyle, rootRowStyle),
            textColumnStyle = AepColumnStyle.merge(defaultTextColumnStyle, textColumnStyle),
            titleAepTextStyle = AepTextStyle.merge(defaultTitleAepTextStyle, titleAepTextStyle),
            bodyAepTextStyle = AepTextStyle.merge(defaultBodyAepTextStyle, bodyAepTextStyle),
            buttonRowStyle = AepRowStyle.merge(defaultButtonRowStyle, buttonRowStyle),
            buttonStyle = (buttonStyle ?: arrayOfNulls(3)).map { pair ->
                Pair(
                    AepButtonStyle.merge(defaultButtonStyle, pair?.first),
                    AepTextStyle.merge(defaultButtonTextStyle, pair?.second)
                )
            }.toTypedArray()
        )
    }
}
