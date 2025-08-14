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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonElevation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class LargeImageUIStyleTests {
    val defaultLargeImageUIStyle = LargeImageUIStyle.Builder().build()

    @Test
    fun `create LargeImageUIStyle with no builder styles used`() {
        val style = LargeImageUIStyle.Builder().build()

        // verify card style parameters
        AepStyleValidator.validateCardStyle(defaultLargeImageUIStyle.cardStyle, style.cardStyle)

        // verify root column style parameters
        AepStyleValidator.validateColumnStyle(defaultLargeImageUIStyle.rootColumnStyle, style.rootColumnStyle)

        // verify image style parameters
        AepStyleValidator.validateImageStyle(defaultLargeImageUIStyle.imageStyle, style.imageStyle)

        // verify text column style parameters
        AepStyleValidator.validateColumnStyle(defaultLargeImageUIStyle.textColumnStyle, style.textColumnStyle)

        // verify title text style parameters
        AepStyleValidator.validateTextStyle(defaultLargeImageUIStyle.titleTextStyle, style.titleTextStyle)

        // verify body text style parameters
        AepStyleValidator.validateTextStyle(defaultLargeImageUIStyle.bodyTextStyle, style.bodyTextStyle)

        // verify button row style parameters
        AepStyleValidator.validateRowStyle(defaultLargeImageUIStyle.buttonRowStyle, style.buttonRowStyle)

        // verify button style parameters
        assertEquals(3, style.buttonStyle.size)
        var index = 0
        for (builtButtonStyle in style.buttonStyle) {
            AepStyleValidator.validateButtonStyle(defaultLargeImageUIStyle.buttonStyle[index], builtButtonStyle)
            index++
        }

        // verify dismiss button style parameters
        AepStyleValidator.validateIconStyle(defaultLargeImageUIStyle.dismissButtonStyle, style.dismissButtonStyle)

        // verify dismiss button alignment
        assertEquals(defaultLargeImageUIStyle.dismissButtonAlignment, style.dismissButtonAlignment)
    }

    @Test
    fun `create LargeImageUIStyle with all builder styles used`() {
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            shape = RectangleShape
        )
        val rootColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            colorFilter = ColorFilter.tint(Color.Red, BlendMode.Color)
        )
        val textColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        val titleTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            textStyle = TextStyle(color = Color.Red, fontSize = 16.sp),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )

        val bodyTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            textStyle = TextStyle(color = Color.Red, fontSize = 16.sp),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )
        val buttonRowStyle = AepRowStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        )
        val buttonStyle = AepButtonStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            enabled = true,
            elevation = mock(ButtonElevation::class.java), // using a mock as ButtonElevation requires a composable function
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.Red),
            contentPadding = null,
            textStyle = AepTextStyle(
                modifier = Modifier.size(100.dp, 100.dp),
                textStyle = TextStyle(color = Color.Red, fontSize = 16.sp),
                overflow = TextOverflow.Clip,
                softWrap = true,
                maxLines = 1,
                minLines = 1
            )
        )
        val dismissButtonStyle = AepIconStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description",
            tint = Color.Blue
        )
        val dismissButtonAlignment = Alignment.BottomEnd

        val style = LargeImageUIStyle.Builder()
            .cardStyle(cardStyle)
            .rootColumnStyle(rootColumnStyle)
            .imageStyle(imageStyle)
            .textColumnStyle(textColumnStyle)
            .titleAepTextStyle(titleTextStyle)
            .bodyAepTextStyle(bodyTextStyle)
            .buttonRowStyle(buttonRowStyle)
            .buttonStyle(arrayOf(buttonStyle, buttonStyle))
            .dismissButtonStyle(dismissButtonStyle)
            .dismissButtonAlignment(dismissButtonAlignment)
            .build()

        // verify card style parameters
        AepStyleValidator.validateCardStyle(cardStyle, style.cardStyle)

        // verify root column style parameters
        AepStyleValidator.validateColumnStyle(rootColumnStyle, style.rootColumnStyle)

        // verify image style parameters
        AepStyleValidator.validateImageStyle(imageStyle, style.imageStyle)

        // verify text column style parameters
        AepStyleValidator.validateColumnStyle(textColumnStyle, style.textColumnStyle)

        // verify title text style parameters
        AepStyleValidator.validateTextStyle(titleTextStyle, style.titleTextStyle)

        // verify body text style parameters
        AepStyleValidator.validateTextStyle(bodyTextStyle, style.bodyTextStyle)

        // verify button row style parameters
        AepStyleValidator.validateRowStyle(buttonRowStyle, style.buttonRowStyle)

        // verify button style parameters
        assertEquals(2, style.buttonStyle.size)
        for (builtButtonStyle in style.buttonStyle) {
            AepStyleValidator.validateButtonStyle(buttonStyle, builtButtonStyle)
        }

        // verify dismiss button style parameters
        AepStyleValidator.validateIconStyle(dismissButtonStyle, style.dismissButtonStyle)

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)
    }

    @Test
    fun `create LargeImageUIStyle with custom card style used`() {
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )

        val style = LargeImageUIStyle.Builder()
            .cardStyle(cardStyle)
            .build()

        // verify card style parameters
        assertEquals(defaultLargeImageUIStyle.cardStyle.border, style.cardStyle.border)
        assertEquals(defaultLargeImageUIStyle.cardStyle.colors, style.cardStyle.colors)
        assertEquals(defaultLargeImageUIStyle.cardStyle.elevation, style.cardStyle.elevation)
        assertEquals(cardStyle.modifier, style.cardStyle.modifier)
        assertEquals(defaultLargeImageUIStyle.cardStyle.shape, style.cardStyle.shape)
    }

    @Test
    fun `create LargeImageUIStyle with custom root column style used`() {
        val rootColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center
        )

        val style = LargeImageUIStyle.Builder()
            .rootColumnStyle(rootColumnStyle)
            .build()

        // verify root column style parameters
        assertEquals(rootColumnStyle.verticalArrangement, style.rootColumnStyle.verticalArrangement)
        assertEquals(defaultLargeImageUIStyle.rootColumnStyle.horizontalAlignment, style.rootColumnStyle.horizontalAlignment)
        assertEquals(rootColumnStyle.modifier, style.rootColumnStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom image style used`() {
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description"
        )

        val style = LargeImageUIStyle.Builder()
            .imageStyle(imageStyle)
            .build()

        // verify image style parameters
        assertEquals(defaultLargeImageUIStyle.imageStyle.alpha, style.imageStyle.alpha)
        assertEquals(defaultLargeImageUIStyle.imageStyle.colorFilter, style.imageStyle.colorFilter)
        assertEquals(imageStyle.contentDescription, style.imageStyle.contentDescription)
        assertEquals(defaultLargeImageUIStyle.imageStyle.alignment, style.imageStyle.alignment)
        assertEquals(defaultLargeImageUIStyle.imageStyle.contentScale, style.imageStyle.contentScale)
        assertEquals(imageStyle.modifier, style.imageStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom text column style used`() {
        val textColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center
        )

        val style = LargeImageUIStyle.Builder()
            .textColumnStyle(textColumnStyle)
            .build()

        // verify text column style parameters
        assertEquals(textColumnStyle.verticalArrangement, style.textColumnStyle.verticalArrangement)
        assertEquals(defaultLargeImageUIStyle.textColumnStyle.horizontalAlignment, style.textColumnStyle.horizontalAlignment)
        assertEquals(textColumnStyle.modifier, style.textColumnStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom title text style used`() {
        val titleTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            textStyle = TextStyle(color = Color.Red, fontSize = 16.sp)
        )

        val style = LargeImageUIStyle.Builder()
            .titleAepTextStyle(titleTextStyle)
            .build()

        // verify title text style parameters
        assertEquals(titleTextStyle.textStyle, style.titleTextStyle.textStyle)
        assertEquals(defaultLargeImageUIStyle.titleTextStyle.overflow, style.titleTextStyle.overflow)
        assertEquals(defaultLargeImageUIStyle.titleTextStyle.softWrap, style.titleTextStyle.softWrap)
        assertEquals(defaultLargeImageUIStyle.titleTextStyle.maxLines, style.titleTextStyle.maxLines)
        assertEquals(defaultLargeImageUIStyle.titleTextStyle.minLines, style.titleTextStyle.minLines)
        assertEquals(titleTextStyle.modifier, style.titleTextStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom body text style used`() {
        val bodyTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )

        val style = LargeImageUIStyle.Builder()
            .bodyAepTextStyle(bodyTextStyle)
            .build()

        // verify body text style parameters
        assertEquals(defaultLargeImageUIStyle.bodyTextStyle.textStyle, style.bodyTextStyle.textStyle)
        assertEquals(defaultLargeImageUIStyle.bodyTextStyle.overflow, style.bodyTextStyle.overflow)
        assertEquals(defaultLargeImageUIStyle.bodyTextStyle.softWrap, style.bodyTextStyle.softWrap)
        assertEquals(defaultLargeImageUIStyle.bodyTextStyle.maxLines, style.bodyTextStyle.maxLines)
        assertEquals(defaultLargeImageUIStyle.bodyTextStyle.minLines, style.bodyTextStyle.minLines)
        assertEquals(bodyTextStyle.modifier, style.bodyTextStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom button row style used`() {
        val buttonRowStyle = AepRowStyle(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        )

        val style = LargeImageUIStyle.Builder()
            .buttonRowStyle(buttonRowStyle)
            .build()

        // verify button row style parameters
        assertEquals(buttonRowStyle.verticalAlignment, style.buttonRowStyle.verticalAlignment)
        assertEquals(defaultLargeImageUIStyle.buttonRowStyle.horizontalArrangement, style.buttonRowStyle.horizontalArrangement)
        assertEquals(buttonRowStyle.modifier, style.buttonRowStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom button style used`() {
        val buttonStyle = AepButtonStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            enabled = true,
            elevation = mock(ButtonElevation::class.java), // using a mock as ButtonElevation requires a composable function
            shape = CircleShape
        )
        val buttonStyles: Array<AepButtonStyle?> = arrayOf(buttonStyle, buttonStyle, buttonStyle)

        val style = LargeImageUIStyle.Builder()
            .buttonStyle(buttonStyles)
            .build()

        // verify button style parameters
        var index = 0
        assertEquals(3, style.buttonStyle.size)
        for (builtButtonStyle in style.buttonStyle) {
            assertEquals(buttonStyles[index]?.shape, builtButtonStyle.shape)
            assertEquals(defaultLargeImageUIStyle.buttonStyle[index].border, builtButtonStyle.border)
            assertEquals(defaultLargeImageUIStyle.buttonStyle[index].colors, builtButtonStyle.colors)
            assertEquals(defaultLargeImageUIStyle.buttonStyle[index].contentPadding, builtButtonStyle.contentPadding)
            assertEquals(buttonStyles[index]?.elevation, builtButtonStyle.elevation)
            assertEquals(buttonStyles[index]?.enabled, builtButtonStyle.enabled)
            assertEquals(buttonStyles[index]?.modifier, builtButtonStyle.modifier)
            AepStyleValidator.validateTextStyle(defaultLargeImageUIStyle.buttonStyle[index].textStyle, builtButtonStyle.textStyle)
            index++
        }
    }

    @Test
    fun `create LargeImageUIStyle with custom dismiss button style used`() {
        val dismissButtonStyle = AepIconStyle(
            tint = Color.Blue
        )

        val style = LargeImageUIStyle.Builder()
            .dismissButtonStyle(dismissButtonStyle)
            .build()

        // verify dismiss button style parameters
        assertEquals(dismissButtonStyle.tint, style.dismissButtonStyle.tint)
        assertEquals(defaultLargeImageUIStyle.dismissButtonStyle.contentDescription, style.dismissButtonStyle.contentDescription)
        assertEquals(defaultLargeImageUIStyle.dismissButtonStyle.modifier, style.dismissButtonStyle.modifier)
    }

    @Test
    fun `create LargeImageUIStyle with custom dismiss button alignment used`() {
        val dismissButtonAlignment = Alignment.BottomEnd

        val style = LargeImageUIStyle.Builder()
            .dismissButtonAlignment(dismissButtonAlignment)
            .build()

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)
    }
}
