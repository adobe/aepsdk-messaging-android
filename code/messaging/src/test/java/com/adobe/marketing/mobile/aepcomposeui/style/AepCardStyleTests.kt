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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardElevation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AepCardStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepCardStyle(
            modifier = Modifier.padding(8.dp).border(BorderStroke(1.dp, Color.Blue)),
            shape = mock(Shape::class.java),
            colors = mock(CardColors::class.java),
            elevation = mock(CardElevation::class.java),
            border = mock(BorderStroke::class.java)
        )
        val overridingStyle = AepCardStyle(
            modifier = Modifier.padding(16.dp).border(BorderStroke(2.dp, Color.Red)),
            shape = mock(Shape::class.java),
            colors = mock(CardColors::class.java),
            elevation = mock(CardElevation::class.java),
            border = mock(BorderStroke::class.java)
        )

        val result = AepCardStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.shape, result.shape)
        assertEquals(overridingStyle.colors, result.colors)
        assertEquals(overridingStyle.elevation, result.elevation)
        assertEquals(overridingStyle.border, result.border)
    }

    @Test
    fun `merge with overriding style containing modifier only`() {
        val defaultStyle = AepCardStyle(
            modifier = Modifier.padding(8.dp).border(BorderStroke(1.dp, Color.Blue)),
            shape = mock(Shape::class.java),
            colors = mock(CardColors::class.java),
            elevation = mock(CardElevation::class.java),
            border = mock(BorderStroke::class.java)
        )
        val overridingStyle = AepCardStyle(
            modifier = Modifier.padding(16.dp).border(BorderStroke(2.dp, Color.Red)),
            shape = null,
            colors =  null,
            elevation = null,
            border = null
        )

        val result = AepCardStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(defaultStyle.shape, result.shape)
        assertEquals(defaultStyle.colors, result.colors)
        assertEquals(defaultStyle.elevation, result.elevation)
        assertEquals(defaultStyle.border, result.border)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepCardStyle(
            modifier = Modifier.padding(8.dp).border(BorderStroke(1.dp, Color.Blue)),
            shape = mock(Shape::class.java),
            colors = mock(CardColors::class.java),
            elevation = mock(CardElevation::class.java),
            border = mock(BorderStroke::class.java)
        )

        val result = AepCardStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.shape, result.shape)
        assertEquals(defaultStyle.colors, result.colors)
        assertEquals(defaultStyle.elevation, result.elevation)
        assertEquals(defaultStyle.border, result.border)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepCardStyle(
            modifier = Modifier.padding(8.dp).border(BorderStroke(1.dp, Color.Blue)),
            shape = mock(Shape::class.java),
            colors = mock(CardColors::class.java),
            elevation = mock(CardElevation::class.java),
            border = mock(BorderStroke::class.java)
        )
        val overridingStyle = AepCardStyle(
            modifier = null,
            shape = null,
            colors =  null,
            elevation = null,
            border = null
        )

        val result = AepCardStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.shape, result.shape)
        assertEquals(defaultStyle.colors, result.colors)
        assertEquals(defaultStyle.elevation, result.elevation)
        assertEquals(defaultStyle.border, result.border)
    }
}