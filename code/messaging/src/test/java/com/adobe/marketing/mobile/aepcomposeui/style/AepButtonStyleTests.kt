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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonElevation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AepButtonStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepButtonStyle(
            modifier = Modifier.padding(8.dp).border(BorderStroke(1.dp, Color.Blue)),
            enabled = false,
            elevation = mock(ButtonElevation::class.java),
            shape = mock(Shape::class.java),
            border = mock(BorderStroke::class.java),
            colors = mock(ButtonColors::class.java),
            contentPadding = mock(PaddingValues::class.java),
            textStyle = mock(AepTextStyle::class.java)
        )
        val overridingStyle = AepButtonStyle(
            modifier = Modifier.padding(16.dp).border(BorderStroke(2.dp, Color.Red)),
            enabled = true,
            elevation = mock(ButtonElevation::class.java),
            shape = mock(Shape::class.java),
            border = mock(BorderStroke::class.java),
            colors = mock(ButtonColors::class.java),
            contentPadding = mock(PaddingValues::class.java),
            textStyle = mock(AepTextStyle::class.java)
        )

        val result = AepButtonStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.enabled, result.enabled)
        assertEquals(overridingStyle.elevation, result.elevation)
        assertEquals(overridingStyle.shape, result.shape)
        assertEquals(overridingStyle.border, result.border)
        assertEquals(overridingStyle.colors, result.colors)
        assertEquals(overridingStyle.contentPadding, result.contentPadding)
        assertEquals(overridingStyle.textStyle?.textStyle, result.textStyle?.textStyle)
        assertEquals(overridingStyle.textStyle?.overflow, result.textStyle?.overflow)
        assertEquals(overridingStyle.textStyle?.softWrap, result.textStyle?.softWrap)
        assertEquals(overridingStyle.textStyle?.maxLines, result.textStyle?.maxLines)
        assertEquals(overridingStyle.textStyle?.minLines, result.textStyle?.minLines)
        assertEquals(overridingStyle.textStyle?.modifier, result.textStyle?.modifier)
    }

    @Test
    fun `merge with overriding style with some parameters overriden`() {
        val defaultStyle = AepButtonStyle(
            modifier = Modifier.padding(8.dp).border(BorderStroke(1.dp, Color.Blue)),
            enabled = false,
            elevation = mock(ButtonElevation::class.java),
            shape = mock(Shape::class.java),
            border = mock(BorderStroke::class.java),
            colors = mock(ButtonColors::class.java),
            contentPadding = mock(PaddingValues::class.java),
            textStyle = mock(AepTextStyle::class.java)
        )
        val overridingStyle = AepButtonStyle(
            modifier = Modifier.padding(16.dp).border(BorderStroke(2.dp, Color.Red)),
            enabled = true,
            elevation = null,
            shape = null,
            border = BorderStroke(3.dp, Color.Green),
            colors = null,
            contentPadding = null,
            textStyle = null
        )

        val result = AepButtonStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.enabled, result.enabled)
        assertEquals(defaultStyle.elevation, result.elevation)
        assertEquals(defaultStyle.shape, result.shape)
        assertEquals(overridingStyle.border, result.border)
        assertEquals(defaultStyle.colors, result.colors)
        assertEquals(defaultStyle.contentPadding, result.contentPadding)
        assertEquals(defaultStyle.textStyle, result.textStyle)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepButtonStyle(
            modifier = Modifier,
            enabled = true,
            elevation = mock(ButtonElevation::class.java),
            shape = mock(Shape::class.java),
            border = mock(BorderStroke::class.java),
            colors = mock(ButtonColors::class.java),
            contentPadding = mock(PaddingValues::class.java),
            textStyle = mock(AepTextStyle::class.java)
        )

        val result = AepButtonStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.enabled, result.enabled)
        assertEquals(defaultStyle.elevation, result.elevation)
        assertEquals(defaultStyle.shape, result.shape)
        assertEquals(defaultStyle.border, result.border)
        assertEquals(defaultStyle.colors, result.colors)
        assertEquals(defaultStyle.contentPadding, result.contentPadding)
        assertEquals(defaultStyle.textStyle, result.textStyle)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepButtonStyle(
            modifier = Modifier,
            enabled = true,
            elevation = mock(ButtonElevation::class.java),
            shape = mock(Shape::class.java),
            border = mock(BorderStroke::class.java),
            colors = mock(ButtonColors::class.java),
            contentPadding = mock(PaddingValues::class.java),
            textStyle = mock(AepTextStyle::class.java)
        )
        val overridingStyle = AepButtonStyle(
            modifier = null,
            enabled = null,
            elevation = null,
            shape = null,
            border = null,
            colors = null,
            contentPadding = null,
            textStyle = null
        )

        val result = AepButtonStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.enabled, result.enabled)
        assertEquals(defaultStyle.elevation, result.elevation)
        assertEquals(defaultStyle.shape, result.shape)
        assertEquals(defaultStyle.border, result.border)
        assertEquals(defaultStyle.colors, result.colors)
        assertEquals(defaultStyle.contentPadding, result.contentPadding)
        assertEquals(defaultStyle.textStyle, result.textStyle)
    }
}
