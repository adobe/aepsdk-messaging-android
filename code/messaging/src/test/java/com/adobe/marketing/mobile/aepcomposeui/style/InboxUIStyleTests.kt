/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui.style

import android.R.attr.maxLines
import android.R.attr.minLines
import android.R.attr.textStyle
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito.mock

class InboxUIStyleTests {

    private val defaultInboxUIStyle = InboxUIStyle.Builder().build()

    @Test
    fun `create InboxUIStyle with no builder styles used`() {
        val style = InboxUIStyle.Builder().build()

        // verify heading style parameters
        AepStyleValidator.validateTextStyle(defaultInboxUIStyle.headingStyle, style.headingStyle)

        // verify lazy column style parameters
        AepStyleValidator.validateLazyColumnStyle(defaultInboxUIStyle.lazyColumnStyle, style.lazyColumnStyle)

        // verify lazy row style parameters
        AepStyleValidator.validateLazyRowStyle(defaultInboxUIStyle.lazyRowStyle, style.lazyRowStyle)

        // verify unread icon style parameters
        AepStyleValidator.validateImageStyle(defaultInboxUIStyle.unreadIconStyle, style.unreadIconStyle)

        // verify unread icon alignment
        assertEquals(defaultInboxUIStyle.unreadIconAlignment, style.unreadIconAlignment)

        // verify unread background color
        assertEquals(defaultInboxUIStyle.unreadBgColor, style.unreadBgColor)

        // verify empty message style parameters
        AepStyleValidator.validateTextStyle(defaultInboxUIStyle.emptyMessageStyle, style.emptyMessageStyle)

        // verify empty image style parameters
        AepStyleValidator.validateImageStyle(defaultInboxUIStyle.emptyImageStyle, style.emptyImageStyle)

        // verify loading view is not null
        assertNotNull(style.loadingView)

        // verify error view is not null
        assertNotNull(style.errorView)
    }

    @Test
    fun `create InboxUIStyle with all builder styles used`() {
        val headingStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            textStyle = TextStyle(color = Color.Red, fontSize = 20.sp),
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )
        val lazyColumnStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = true
        )
        val lazyRowStyle = AepLazyRowStyle(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = false,
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = true
        )
        val unreadIconStyle = AepImageStyle(
            modifier = Modifier.size(24.dp),
            contentDescription = "Unread icon",
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            alpha = 1.0f,
            colorFilter = ColorFilter.tint(Color.Blue, BlendMode.SrcIn)
        )
        val unreadIconAlignment = Alignment.TopEnd
        val unreadBgColor = AepColor(light = Color(0xFFFF0000), dark = Color(0xFF8B0000))
        val emptyMessageStyle = AepTextStyle(
            modifier = Modifier.padding(16.dp),
            textStyle = TextStyle(color = Color.Gray, fontSize = 14.sp),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 2,
            minLines = 1
        )
        val emptyImageStyle = AepImageStyle(
            modifier = Modifier.size(48.dp),
            contentDescription = "Empty inbox image",
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            alpha = 0.8f,
            colorFilter = null
        )
        val customLoadingView: @Composable () -> Unit = {}
        val customErrorView: @Composable () -> Unit = {}

        val style = InboxUIStyle.Builder()
            .headingStyle(headingStyle)
            .lazyColumnStyle(lazyColumnStyle)
            .lazyRowStyle(lazyRowStyle)
            .unreadIconStyle(unreadIconStyle)
            .unreadIconAlignment(unreadIconAlignment)
            .unreadBgColor(unreadBgColor)
            .emptyMessageStyle(emptyMessageStyle)
            .emptyImageStyle(emptyImageStyle)
            .loadingView(customLoadingView)
            .errorView(customErrorView)
            .build()

        // verify heading style parameters
        AepStyleValidator.validateTextStyle(headingStyle, style.headingStyle)

        // verify lazy column style parameters
        AepStyleValidator.validateLazyColumnStyle(lazyColumnStyle, style.lazyColumnStyle)

        // verify lazy row style parameters
        AepStyleValidator.validateLazyRowStyle(lazyRowStyle, style.lazyRowStyle)

        // verify unread icon style parameters
        AepStyleValidator.validateImageStyle(unreadIconStyle, style.unreadIconStyle)

        // verify unread icon alignment
        assertEquals(unreadIconAlignment, style.unreadIconAlignment)

        // verify unread background color
        assertEquals(unreadBgColor, style.unreadBgColor)

        // verify empty message style parameters
        AepStyleValidator.validateTextStyle(emptyMessageStyle, style.emptyMessageStyle)

        // verify empty image style parameters
        AepStyleValidator.validateImageStyle(emptyImageStyle, style.emptyImageStyle)

        // verify loading view is the custom one
        assertEquals(customLoadingView, style.loadingView)

        // verify error view is the custom one
        assertEquals(customErrorView, style.errorView)
    }

    @Test
    fun `create InboxUIStyle with custom heading style used`() {
        val headingStyle = AepTextStyle(
            modifier = Modifier.padding(8.dp),
            textStyle = TextStyle(color = Color.Blue, fontSize = 24.sp)
        )

        val style = InboxUIStyle.Builder()
            .headingStyle(headingStyle)
            .build()

        // verify heading style parameters
        assertEquals(headingStyle.modifier, style.headingStyle.modifier)
        assertEquals(headingStyle.textStyle, style.headingStyle.textStyle)
        assertEquals(defaultInboxUIStyle.headingStyle.overflow, style.headingStyle.overflow)
        assertEquals(defaultInboxUIStyle.headingStyle.softWrap, style.headingStyle.softWrap)
        assertEquals(defaultInboxUIStyle.headingStyle.maxLines, style.headingStyle.maxLines)
        assertEquals(defaultInboxUIStyle.headingStyle.minLines, style.headingStyle.minLines)
    }

    @Test
    fun `create InboxUIStyle with custom lazy column style used`() {
        val lazyColumnStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(12.dp),
            contentPadding = PaddingValues(8.dp),
            reverseLayout = true
        )

        val style = InboxUIStyle.Builder()
            .lazyColumnStyle(lazyColumnStyle)
            .build()

        // verify lazy column style parameters
        assertEquals(lazyColumnStyle.modifier, style.lazyColumnStyle.modifier)
        assertEquals(lazyColumnStyle.contentPadding, style.lazyColumnStyle.contentPadding)
        assertEquals(lazyColumnStyle.reverseLayout, style.lazyColumnStyle.reverseLayout)
        assertEquals(defaultInboxUIStyle.lazyColumnStyle.verticalArrangement, style.lazyColumnStyle.verticalArrangement)
        assertEquals(defaultInboxUIStyle.lazyColumnStyle.horizontalAlignment, style.lazyColumnStyle.horizontalAlignment)
        assertEquals(defaultInboxUIStyle.lazyColumnStyle.flingBehavior, style.lazyColumnStyle.flingBehavior)
        assertEquals(defaultInboxUIStyle.lazyColumnStyle.userScrollEnabled, style.lazyColumnStyle.userScrollEnabled)
    }

    @Test
    fun `create InboxUIStyle with custom lazy row style used`() {
        val lazyRowStyle = AepLazyRowStyle(
            modifier = Modifier.padding(12.dp),
            contentPadding = PaddingValues(8.dp),
            reverseLayout = true
        )

        val style = InboxUIStyle.Builder()
            .lazyRowStyle(lazyRowStyle)
            .build()

        // verify lazy row style parameters
        assertEquals(lazyRowStyle.modifier, style.lazyRowStyle.modifier)
        assertEquals(lazyRowStyle.contentPadding, style.lazyRowStyle.contentPadding)
        assertEquals(lazyRowStyle.reverseLayout, style.lazyRowStyle.reverseLayout)
        assertEquals(defaultInboxUIStyle.lazyRowStyle.horizontalArrangement, style.lazyRowStyle.horizontalArrangement)
        assertEquals(defaultInboxUIStyle.lazyRowStyle.verticalAlignment, style.lazyRowStyle.verticalAlignment)
        assertEquals(defaultInboxUIStyle.lazyRowStyle.flingBehavior, style.lazyRowStyle.flingBehavior)
        assertEquals(defaultInboxUIStyle.lazyRowStyle.userScrollEnabled, style.lazyRowStyle.userScrollEnabled)
    }

    @Test
    fun `create InboxUIStyle with custom unread icon style used`() {
        val unreadIconStyle = AepImageStyle(
            modifier = Modifier.size(30.dp),
            contentDescription = "Custom unread icon"
        )

        val style = InboxUIStyle.Builder()
            .unreadIconStyle(unreadIconStyle)
            .build()

        // verify unread icon style parameters
        assertEquals(unreadIconStyle.modifier, style.unreadIconStyle.modifier)
        assertEquals(unreadIconStyle.contentDescription, style.unreadIconStyle.contentDescription)
        assertEquals(defaultInboxUIStyle.unreadIconStyle.alignment, style.unreadIconStyle.alignment)
        assertEquals(defaultInboxUIStyle.unreadIconStyle.contentScale, style.unreadIconStyle.contentScale)
        assertEquals(defaultInboxUIStyle.unreadIconStyle.alpha, style.unreadIconStyle.alpha)
        assertEquals(defaultInboxUIStyle.unreadIconStyle.colorFilter, style.unreadIconStyle.colorFilter)
    }

    @Test
    fun `create InboxUIStyle with custom unread icon alignment used`() {
        val unreadIconAlignment = Alignment.BottomStart

        val style = InboxUIStyle.Builder()
            .unreadIconAlignment(unreadIconAlignment)
            .build()

        // verify unread icon alignment
        assertEquals(unreadIconAlignment, style.unreadIconAlignment)
    }

    @Test
    fun `create InboxUIStyle with custom unread background color used`() {
        val unreadBgColor = AepColor(light = Color(0xFFFF0000), dark = Color(0xFF8B0000))

        val style = InboxUIStyle.Builder()
            .unreadBgColor(unreadBgColor)
            .build()

        // verify unread background color
        assertEquals(unreadBgColor, style.unreadBgColor)
    }

    @Test
    fun `create InboxUIStyle with custom empty message style used`() {
        val emptyMessageStyle = AepTextStyle(
            modifier = Modifier.padding(24.dp),
            textStyle = TextStyle(color = Color.DarkGray, fontSize = 16.sp)
        )

        val style = InboxUIStyle.Builder()
            .emptyMessageStyle(emptyMessageStyle)
            .build()

        // verify empty message style parameters
        assertEquals(emptyMessageStyle.modifier, style.emptyMessageStyle.modifier)
        assertEquals(emptyMessageStyle.textStyle, style.emptyMessageStyle.textStyle)
        assertEquals(defaultInboxUIStyle.emptyMessageStyle.overflow, style.emptyMessageStyle.overflow)
        assertEquals(defaultInboxUIStyle.emptyMessageStyle.softWrap, style.emptyMessageStyle.softWrap)
        assertEquals(defaultInboxUIStyle.emptyMessageStyle.maxLines, style.emptyMessageStyle.maxLines)
        assertEquals(defaultInboxUIStyle.emptyMessageStyle.minLines, style.emptyMessageStyle.minLines)
    }

    @Test
    fun `create InboxUIStyle with custom empty image style used`() {
        val emptyImageStyle = AepImageStyle(
            modifier = Modifier.size(64.dp),
            contentDescription = "Custom empty image"
        )

        val style = InboxUIStyle.Builder()
            .emptyImageStyle(emptyImageStyle)
            .build()

        // verify empty image style parameters
        assertEquals(emptyImageStyle.modifier, style.emptyImageStyle.modifier)
        assertEquals(emptyImageStyle.contentDescription, style.emptyImageStyle.contentDescription)
        assertEquals(defaultInboxUIStyle.emptyImageStyle.alignment, style.emptyImageStyle.alignment)
        assertEquals(defaultInboxUIStyle.emptyImageStyle.contentScale, style.emptyImageStyle.contentScale)
        assertEquals(defaultInboxUIStyle.emptyImageStyle.alpha, style.emptyImageStyle.alpha)
        assertEquals(defaultInboxUIStyle.emptyImageStyle.colorFilter, style.emptyImageStyle.colorFilter)
    }

    @Test
    fun `create InboxUIStyle with custom loading view used`() {
        val customLoadingView: @Composable () -> Unit = {}

        val style = InboxUIStyle.Builder()
            .loadingView(customLoadingView)
            .build()

        // verify loading view is the custom one
        assertEquals(customLoadingView, style.loadingView)
    }

    @Test
    fun `create InboxUIStyle with custom error view used`() {
        val customErrorView: @Composable () -> Unit = {}

        val style = InboxUIStyle.Builder()
            .errorView(customErrorView)
            .build()

        // verify error view is the custom one
        assertEquals(customErrorView, style.errorView)
    }
}
