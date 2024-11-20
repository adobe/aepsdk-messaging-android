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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AepImageStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepImageStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 1.0f,
            colorFilter = mock(ColorFilter::class.java)
        )
        val overridingStyle = AepImageStyle(
            modifier = Modifier.padding(16.dp),
            contentDescription = "overriding description",
            alignment = Alignment.BottomEnd,
            contentScale = ContentScale.FillBounds,
            alpha = 0.5f,
            colorFilter = mock(ColorFilter::class.java)
        )

        val result = AepImageStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.contentDescription, result.contentDescription)
        assertEquals(overridingStyle.alignment, result.alignment)
        assertEquals(overridingStyle.contentScale, result.contentScale)
        assertEquals(overridingStyle.alpha, result.alpha)
        assertEquals(overridingStyle.colorFilter, result.colorFilter)
    }

    @Test
    fun `merge with overriding style with some parameters overriden`() {
        val defaultStyle = AepImageStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 1.0f,
            colorFilter = mock(ColorFilter::class.java)
        )
        val overridingStyle = AepImageStyle(
            modifier = Modifier.padding(16.dp),
            contentDescription = null,
            alignment = Alignment.BottomEnd,
            contentScale = ContentScale.FillBounds,
            alpha = null,
            colorFilter = null
        )

        val result = AepImageStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentDescription, result.contentDescription)
        assertEquals(overridingStyle.alignment, result.alignment)
        assertEquals(overridingStyle.contentScale, result.contentScale)
        assertEquals(defaultStyle.alpha, result.alpha)
        assertEquals(defaultStyle.colorFilter, result.colorFilter)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepImageStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 1.0f,
            colorFilter = mock(ColorFilter::class.java)
        )

        val result = AepImageStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentDescription, result.contentDescription)
        assertEquals(defaultStyle.alignment, result.alignment)
        assertEquals(defaultStyle.contentScale, result.contentScale)
        assertEquals(defaultStyle.alpha, result.alpha)
        assertEquals(defaultStyle.colorFilter, result.colorFilter)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepImageStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 1.0f,
            colorFilter = mock(ColorFilter::class.java)
        )
        val overridingStyle = AepImageStyle(
            modifier = null,
            contentDescription = null,
            alignment = null,
            contentScale = null,
            alpha = null,
            colorFilter = null
        )

        val result = AepImageStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentDescription, result.contentDescription)
        assertEquals(defaultStyle.alignment, result.alignment)
        assertEquals(defaultStyle.contentScale, result.contentScale)
        assertEquals(defaultStyle.alpha, result.alpha)
        assertEquals(defaultStyle.colorFilter, result.colorFilter)
    }
}
