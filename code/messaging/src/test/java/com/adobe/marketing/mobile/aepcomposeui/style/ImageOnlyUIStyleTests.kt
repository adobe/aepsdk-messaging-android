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

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOnlyUIStyleTests {
    val defaultImageOnlyUIStyle = ImageOnlyUIStyle.Builder().build()

    @Test
    fun `create ImageOnlyUIStyle with no builder styles used`() {
        val style = ImageOnlyUIStyle.Builder().build()

        // verify card style parameters
        AepStyleValidator.validateCardStyle(defaultImageOnlyUIStyle.cardStyle, style.cardStyle)

        // verify image style parameters
        AepStyleValidator.validateImageStyle(defaultImageOnlyUIStyle.imageStyle, style.imageStyle)

        // verify dismiss button style parameters
        AepStyleValidator.validateIconStyle(defaultImageOnlyUIStyle.dismissButtonStyle, style.dismissButtonStyle)

        // verify dismiss button alignment
        assertEquals(defaultImageOnlyUIStyle.dismissButtonAlignment, style.dismissButtonAlignment)
    }

    @Test
    fun `create ImageOnlyUIStyle with all builder styles used`() {
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            shape = RectangleShape
        )
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            colorFilter = ColorFilter.tint(Color.Red, BlendMode.Color)
        )
        val dismissButtonStyle = AepIconStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description",
            tint = Color.Blue
        )
        val dismissButtonAlignment = Alignment.BottomEnd

        val style = ImageOnlyUIStyle.Builder()
            .cardStyle(cardStyle)
            .imageStyle(imageStyle)
            .dismissButtonStyle(dismissButtonStyle)
            .dismissButtonAlignment(dismissButtonAlignment)
            .build()

        // verify card style parameters
        AepStyleValidator.validateCardStyle(cardStyle, style.cardStyle)

        // verify image style parameters
        AepStyleValidator.validateImageStyle(imageStyle, style.imageStyle)

        // verify dismiss button style parameters
        AepStyleValidator.validateIconStyle(dismissButtonStyle, style.dismissButtonStyle)

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)
    }

    @Test
    fun `create ImageOnlyUIStyle with custom card style used`() {
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )

        val style = ImageOnlyUIStyle.Builder()
            .cardStyle(cardStyle)
            .build()

        // verify card style parameters
        assertEquals(defaultImageOnlyUIStyle.cardStyle.border, style.cardStyle.border)
        assertEquals(defaultImageOnlyUIStyle.cardStyle.colors, style.cardStyle.colors)
        assertEquals(defaultImageOnlyUIStyle.cardStyle.elevation, style.cardStyle.elevation)
        assertEquals(cardStyle.modifier, style.cardStyle.modifier)
        assertEquals(defaultImageOnlyUIStyle.cardStyle.shape, style.cardStyle.shape)
    }

    @Test
    fun `create ImageOnlyUIStyle with custom image style used`() {
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description"
        )

        val style = ImageOnlyUIStyle.Builder()
            .imageStyle(imageStyle)
            .build()

        // verify image style parameters
        assertEquals(defaultImageOnlyUIStyle.imageStyle.alpha, style.imageStyle.alpha)
        assertEquals(defaultImageOnlyUIStyle.imageStyle.colorFilter, style.imageStyle.colorFilter)
        assertEquals(imageStyle.contentDescription, style.imageStyle.contentDescription)
        assertEquals(defaultImageOnlyUIStyle.imageStyle.alignment, style.imageStyle.alignment)
        assertEquals(defaultImageOnlyUIStyle.imageStyle.contentScale, style.imageStyle.contentScale)
        assertEquals(imageStyle.modifier, style.imageStyle.modifier)
    }

    @Test
    fun `create ImageOnlyUIStyle with custom dismiss button style used`() {
        val dismissButtonStyle = AepIconStyle(
            tint = Color.Blue
        )

        val style = ImageOnlyUIStyle.Builder()
            .dismissButtonStyle(dismissButtonStyle)
            .build()

        // verify dismiss button style parameters
        assertEquals(dismissButtonStyle.tint, style.dismissButtonStyle.tint)
        assertEquals(defaultImageOnlyUIStyle.dismissButtonStyle.contentDescription, style.dismissButtonStyle.contentDescription)
        assertEquals(defaultImageOnlyUIStyle.dismissButtonStyle.modifier, style.dismissButtonStyle.modifier)
    }

    @Test
    fun `create ImageOnlyUIStyle with custom dismiss button alignment used`() {
        val dismissButtonAlignment = Alignment.BottomEnd

        val style = ImageOnlyUIStyle.Builder()
            .dismissButtonAlignment(dismissButtonAlignment)
            .build()

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)
    }
}
