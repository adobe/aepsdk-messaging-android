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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants

/**
 * Class representing the style for a small image AEP UI.
 *
 * @property titleTextStyle The custom text style for the title, if provided.
 */

class SmallImageUIStyle(
    var titleAepTextStyle: AepTextStyle? = null,
    var bodyAepTextStyle: AepTextStyle? = null,
    var buttonAepButtonStyle: Array<AepButtonStyle?> = arrayOfNulls(3),
    var buttonAepTextStyle: Array<AepTextStyle?> = arrayOfNulls(3),
) {
    val defaultTitleTextStyle: AepTextStyle
        @Composable
        @ReadOnlyComposable
        get() = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.SmallImageCard.DefaultStyle.TITLE_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.SmallImageCard.DefaultStyle.TITLE_FONT_WEIGHT
            )
        )

    val defaultBodyTextStyle: AepTextStyle
        @Composable
        @ReadOnlyComposable
        get() = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.SmallImageCard.DefaultStyle.BODY_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.SmallImageCard.DefaultStyle.BODY_FONT_WEIGHT,
            )
        )

    val defaultButtonTextStyle: AepTextStyle
        @Composable
        @ReadOnlyComposable
        get() = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.SmallImageCard.DefaultStyle.BUTTON_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.SmallImageCard.DefaultStyle.BUTTON_FONT_WEIGHT,
            )
        )
}
