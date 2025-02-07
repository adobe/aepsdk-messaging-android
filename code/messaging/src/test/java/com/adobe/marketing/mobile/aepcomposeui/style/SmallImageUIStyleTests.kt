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
import androidx.compose.foundation.layout.Arrangement
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

class SmallImageUIStyleTests {
    val defaultSmallImageUIStyle = SmallImageUIStyle.Builder().build()

    @Test
    fun `create SmallImageUIStyle with no builder styles used`() {
        val style = SmallImageUIStyle.Builder().build()

        // verify image style parameters
        AepStyleValidator.validateImageStyle(defaultSmallImageUIStyle.imageStyle, style.imageStyle)

        // verify title text style parameters
        AepStyleValidator.validateTextStyle(defaultSmallImageUIStyle.titleTextStyle, style.titleTextStyle)

        // verify body text style parameters
        AepStyleValidator.validateTextStyle(defaultSmallImageUIStyle.bodyTextStyle, style.bodyTextStyle)

        // verify button row style parameters
        AepStyleValidator.validateRowStyle(defaultSmallImageUIStyle.buttonRowStyle, style.buttonRowStyle)

        // verify button style parameters
        assertEquals(3, style.buttonStyle.size)
        var index = 0
        for (builtButtonStyle in style.buttonStyle) {
            AepStyleValidator.validateButtonStyle(defaultSmallImageUIStyle.buttonStyle[index], builtButtonStyle)
            index++
        }

        // verify dismiss button style parameters
        AepStyleValidator.validateIconStyle(defaultSmallImageUIStyle.dismissButtonStyle, style.dismissButtonStyle)

        // verify dismiss button alignment
        assertEquals(defaultSmallImageUIStyle.dismissButtonAlignment, style.dismissButtonAlignment)

        // verify card style parameters
        AepStyleValidator.validateCardStyle(defaultSmallImageUIStyle.cardStyle, style.cardStyle)

        // verify root row style parameters
        AepStyleValidator.validateRowStyle(defaultSmallImageUIStyle.rootRowStyle, style.rootRowStyle)

        // verify text column style parameters
        AepStyleValidator.validateColumnStyle(defaultSmallImageUIStyle.textColumnStyle, style.textColumnStyle)
    }

    @Test
    fun `create SmallImageUIStyle with all builder styles used`() {
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description",
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            alpha = 0.5f,
            colorFilter = ColorFilter.tint(Color.Red, BlendMode.Color)
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
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            shape = RectangleShape
        )
        val rootRowStyle = AepRowStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        )
        val textColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )

        val style = SmallImageUIStyle.Builder()
            .imageStyle(imageStyle)
            .titleAepTextStyle(titleTextStyle)
            .bodyAepTextStyle(bodyTextStyle)
            .buttonRowStyle(buttonRowStyle)
            .buttonStyle(arrayOf(buttonStyle, buttonStyle))
            .dismissButtonStyle(dismissButtonStyle)
            .dismissButtonAlignment(dismissButtonAlignment)
            .cardStyle(cardStyle)
            .rootRowStyle(rootRowStyle)
            .textColumnStyle(textColumnStyle)
            .build()

        // verify image style parameters
        AepStyleValidator.validateImageStyle(imageStyle, style.imageStyle)

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

        // verify card style parameters
        AepStyleValidator.validateCardStyle(cardStyle, style.cardStyle)

        // verify root row style parameters
        AepStyleValidator.validateRowStyle(rootRowStyle, style.rootRowStyle)

        // verify text column style parameters
        AepStyleValidator.validateColumnStyle(textColumnStyle, style.textColumnStyle)
    }

    @Test
    fun `create SmallImageUIStyle with custom image style used`() {
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description"
        )

        val style = SmallImageUIStyle.Builder()
            .imageStyle(imageStyle)
            .build()

        // verify image style parameters
        assertEquals(defaultSmallImageUIStyle.imageStyle.alpha, style.imageStyle.alpha)
        assertEquals(defaultSmallImageUIStyle.imageStyle.colorFilter, style.imageStyle.colorFilter)
        assertEquals(imageStyle.contentDescription, style.imageStyle.contentDescription)
        assertEquals(defaultSmallImageUIStyle.imageStyle.alignment, style.imageStyle.alignment)
        assertEquals(defaultSmallImageUIStyle.imageStyle.contentScale, style.imageStyle.contentScale)
        assertEquals(imageStyle.modifier, style.imageStyle.modifier)
    }

    @Test
    fun `create SmallImageUIStyle with custom title text style used`() {
        val titleTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            textStyle = TextStyle(color = Color.Red, fontSize = 16.sp)
        )

        val style = SmallImageUIStyle.Builder()
            .titleAepTextStyle(titleTextStyle)
            .build()

        // verify title text style parameters
        assertEquals(titleTextStyle.textStyle, style.titleTextStyle.textStyle)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.overflow, style.titleTextStyle.overflow)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.softWrap, style.titleTextStyle.softWrap)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.maxLines, style.titleTextStyle.maxLines)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.minLines, style.titleTextStyle.minLines)
        assertEquals(titleTextStyle.modifier, style.titleTextStyle.modifier)
    }

    @Test
    fun `create SmallImageUIStyle with custom body text style used`() {
        val bodyTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )

        val style = SmallImageUIStyle.Builder()
            .bodyAepTextStyle(bodyTextStyle)
            .build()

        // verify body text style parameters
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.textStyle, style.bodyTextStyle.textStyle)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.overflow, style.bodyTextStyle.overflow)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.softWrap, style.bodyTextStyle.softWrap)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.maxLines, style.bodyTextStyle.maxLines)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.minLines, style.bodyTextStyle.minLines)
        assertEquals(bodyTextStyle.modifier, style.bodyTextStyle.modifier)
    }

    @Test
    fun `create SmallImageUIStyle with custom button row style used`() {
        val buttonRowStyle = AepRowStyle(
            verticalAlignment = Alignment.CenterVertically
        )

        val style = SmallImageUIStyle.Builder()
            .buttonRowStyle(buttonRowStyle)
            .build()

        // verify button row style parameters
        assertEquals(buttonRowStyle.verticalAlignment, style.buttonRowStyle.verticalAlignment)
        assertEquals(defaultSmallImageUIStyle.buttonRowStyle.horizontalArrangement, style.buttonRowStyle.horizontalArrangement)
        assertEquals(buttonRowStyle.modifier, style.buttonRowStyle.modifier)
    }

    @Test
    fun `create SmallImageUIStyle with custom button style used`() {
        val buttonStyle = AepButtonStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            enabled = true,
            elevation = mock(ButtonElevation::class.java), // using a mock as ButtonElevation requires a composable function
            shape = CircleShape
        )
        val buttonStyles: Array<AepButtonStyle?> = arrayOf(buttonStyle, buttonStyle, buttonStyle)

        val style = SmallImageUIStyle.Builder()
            .buttonStyle(buttonStyles)
            .build()

        // verify button style parameters
        var index = 0
        assertEquals(3, style.buttonStyle.size)
        for (builtButtonStyle in style.buttonStyle) {
            assertEquals(buttonStyles[index]?.shape, builtButtonStyle.shape)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].border, builtButtonStyle.border)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].colors, builtButtonStyle.colors)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].contentPadding, builtButtonStyle.contentPadding)
            assertEquals(buttonStyles[index]?.elevation, builtButtonStyle.elevation)
            assertEquals(buttonStyles[index]?.enabled, builtButtonStyle.enabled)
            assertEquals(buttonStyles[index]?.modifier, builtButtonStyle.modifier)
            AepStyleValidator.validateTextStyle(defaultSmallImageUIStyle.buttonStyle[index].textStyle, builtButtonStyle.textStyle)
            index++
        }
    }

    @Test
    fun `create SmallImageUIStyle with custom dismiss button style used`() {
        val dismissButtonStyle = AepIconStyle(
            tint = Color.Blue
        )

        val style = SmallImageUIStyle.Builder()
            .dismissButtonStyle(dismissButtonStyle)
            .build()

        // verify dismiss button style parameters
        assertEquals(dismissButtonStyle.tint, style.dismissButtonStyle.tint)
        assertEquals(defaultSmallImageUIStyle.dismissButtonStyle.contentDescription, style.dismissButtonStyle.contentDescription)
        assertEquals(defaultSmallImageUIStyle.dismissButtonStyle.modifier, style.dismissButtonStyle.modifier)
    }

    @Test
    fun `create SmallImageUIStyle with custom dismiss button alignment used`() {
        val dismissButtonAlignment = Alignment.BottomEnd

        val style = SmallImageUIStyle.Builder()
            .dismissButtonAlignment(dismissButtonAlignment)
            .build()

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)
    }

    @Test
    fun `create SmallImageUIStyle with custom card style used`() {
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )

        val style = SmallImageUIStyle.Builder()
            .cardStyle(cardStyle)
            .build()

        // verify card style parameters
        assertEquals(defaultSmallImageUIStyle.cardStyle.border, style.cardStyle.border)
        assertEquals(defaultSmallImageUIStyle.cardStyle.colors, style.cardStyle.colors)
        assertEquals(defaultSmallImageUIStyle.cardStyle.elevation, style.cardStyle.elevation)
        assertEquals(cardStyle.modifier, style.cardStyle.modifier)
        assertEquals(defaultSmallImageUIStyle.cardStyle.shape, style.cardStyle.shape)
    }

    @Test
    fun `create SmallImageUIStyle with custom root row style used`() {
        val rootRowStyle = AepRowStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalAlignment = Alignment.CenterVertically
        )

        val style = SmallImageUIStyle.Builder()
            .rootRowStyle(rootRowStyle)
            .build()

        // verify root row style parameters
        assertEquals(rootRowStyle.verticalAlignment, style.rootRowStyle.verticalAlignment)
        assertEquals(defaultSmallImageUIStyle.rootRowStyle.horizontalArrangement, style.rootRowStyle.horizontalArrangement)
        assertEquals(rootRowStyle.modifier, style.rootRowStyle.modifier)
    }

    @Test
    fun `create SmallImageUIStyle with custom text column style used`() {
        val textColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center,
        )

        val style = SmallImageUIStyle.Builder()
            .textColumnStyle(textColumnStyle)
            .build()

        // verify text column style parameters
        assertEquals(textColumnStyle.modifier, style.textColumnStyle.modifier)
        assertEquals(defaultSmallImageUIStyle.textColumnStyle.horizontalAlignment, style.textColumnStyle.horizontalAlignment)
        assertEquals(textColumnStyle.verticalArrangement, style.textColumnStyle.verticalArrangement)
    }
}
