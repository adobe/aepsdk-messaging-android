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

package com.adobe.marketing.mobile.aepcomposeui.style

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.AepUIConstants
import com.adobe.marketing.mobile.aepcomposeui.components.AepCircularProgressIndicator
import com.adobe.marketing.mobile.aepcomposeui.components.EmptyInbox
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepText

/**
 * Class representing the style for the Inbox.
 *
 * @property loadingView The composable function representing the loading indicator.
 * @property headingStyle The style for the heading text.
 * @property lazyColumnStyle The style for the lazy column displaying messages.
 * @property emptyMessageStyle The style for the empty message text.
 * @property emptyImageStyle The style for the empty state image.
 * @property unreadIconStyle The style for the unread icon.
 * @property unreadIconAlignment The alignment for the unread icon.
 * @property unreadBgColor The background color for unread messages.
 */
class InboxUIStyle private constructor(
    val headingStyle: AepTextStyle,
    val lazyColumnStyle: AepLazyColumnStyle,
    val unreadIconStyle: AepImageStyle,
    val unreadIconAlignment: Alignment?,
    val unreadBgColor: AepColor?,
    val emptyMessageStyle: AepTextStyle,
    val emptyImageStyle: AepImageStyle,
    val loadingView: @Composable () -> Unit,
    val errorView: @Composable () -> Unit
) {
    companion object {
        private val defaultHeadingStyle = AepTextStyle(
            textStyle = TextStyle(
                fontSize = AepUIConstants.DefaultAepUIStyle.TITLE_TEXT_SIZE.sp,
                fontWeight = AepUIConstants.DefaultAepUIStyle.TITLE_FONT_WEIGHT
            )
        )
        private val defaultListStyle = AepLazyColumnStyle()
        private val defaultEmptyMessageStyle = AepTextStyle()
        private val defaultEmptyImageStyle = AepImageStyle()
        private val defaultUnreadIconStyle = AepImageStyle(
            modifier = Modifier
                .padding(AepUIConstants.DefaultAepUIStyle.SPACING.dp + 5.dp)
                .size(20.dp)
        )
        private val defaultLoadingView: @Composable () -> Unit = {
            AepCircularProgressIndicator()
        }
        private val defaultErrorView: @Composable () -> Unit = {
            EmptyInbox(
                AepText(AepUIConstants.DefaultAepInboxStyle.DEFAULT_INBOX_ERROR_MESSAGE)
            )
        }
    }

    class Builder {
        private var headingStyle: AepTextStyle? = null
        private var lazyColumnStyle: AepLazyColumnStyle? = null
        private var emptyMessageStyle: AepTextStyle? = null
        private var emptyImageStyle: AepImageStyle? = null
        private var unreadIconStyle: AepImageStyle? = null
        private var unreadIconAlignment: Alignment? = null
        private var unreadBgColor: AepColor? = null
        private var loadingView: (@Composable () -> Unit)? = null
        private var errorView: (@Composable () -> Unit)? = null

        fun headingStyle(headingStyle: AepTextStyle) = apply { this.headingStyle = headingStyle }

        fun lazyColumnStyle(listStyle: AepLazyColumnStyle) = apply { this.lazyColumnStyle = listStyle }

        fun emptyMessageStyle(emptyMessageStyle: AepTextStyle) =
            apply { this.emptyMessageStyle = emptyMessageStyle }

        fun emptyImageStyle(emptyImageStyle: AepImageStyle) =
            apply { this.emptyImageStyle = emptyImageStyle }

        fun unreadIconStyle(unreadIconStyle: AepImageStyle) =
            apply { this.unreadIconStyle = unreadIconStyle }

        fun unreadIconAlignment(unreadIconAlignment: Alignment) =
            apply { this.unreadIconAlignment = unreadIconAlignment }

        fun unreadBgColor(unreadBgColor: AepColor) = apply { this.unreadBgColor = unreadBgColor }

        fun loadingView(loadingView: @Composable () -> Unit) =
            apply { this.loadingView = loadingView }

        fun errorView(errorView: @Composable () -> Unit) =
            apply { this.errorView = errorView }

        fun build() = InboxUIStyle(
            headingStyle = AepTextStyle.merge(defaultHeadingStyle, headingStyle),
            lazyColumnStyle = AepLazyColumnStyle.merge(defaultListStyle, lazyColumnStyle),
            emptyMessageStyle = AepTextStyle.merge(defaultEmptyMessageStyle, emptyMessageStyle),
            emptyImageStyle = AepImageStyle.merge(defaultEmptyImageStyle, emptyImageStyle),
            unreadIconStyle = AepImageStyle.merge(defaultUnreadIconStyle, unreadIconStyle),
            unreadIconAlignment = unreadIconAlignment,
            unreadBgColor = unreadBgColor,
            loadingView = loadingView ?: defaultLoadingView,
            errorView = errorView ?: defaultErrorView
        )
    }
}
