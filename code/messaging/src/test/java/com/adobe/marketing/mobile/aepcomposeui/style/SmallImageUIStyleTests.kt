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
            buttonTextStyle = AepTextStyle(
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
            tint = { Color.Blue }
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
        assertEquals(imageStyle.alpha, style.imageStyle.alpha)
        assertEquals(imageStyle.colorFilter, style.imageStyle.colorFilter)
        assertEquals(imageStyle.contentDescription, style.imageStyle.contentDescription)
        assertEquals(imageStyle.alignment, style.imageStyle.alignment)
        assertEquals(imageStyle.contentScale, style.imageStyle.contentScale)
        assertEquals(imageStyle.modifier, style.imageStyle.modifier)

        // verify title text style parameters
        assertEquals(titleTextStyle.textStyle, style.titleTextStyle.textStyle)
        assertEquals(titleTextStyle.overflow, style.titleTextStyle.overflow)
        assertEquals(titleTextStyle.softWrap, style.titleTextStyle.softWrap)
        assertEquals(titleTextStyle.maxLines, style.titleTextStyle.maxLines)
        assertEquals(titleTextStyle.minLines, style.titleTextStyle.minLines)
        assertEquals(titleTextStyle.modifier, style.titleTextStyle.modifier)

        // verify body text style parameters
        assertEquals(bodyTextStyle.textStyle, style.bodyTextStyle.textStyle)
        assertEquals(bodyTextStyle.overflow, style.bodyTextStyle.overflow)
        assertEquals(bodyTextStyle.softWrap, style.bodyTextStyle.softWrap)
        assertEquals(bodyTextStyle.maxLines, style.bodyTextStyle.maxLines)
        assertEquals(bodyTextStyle.minLines, style.bodyTextStyle.minLines)
        assertEquals(bodyTextStyle.modifier, style.bodyTextStyle.modifier)

        // verify button row style parameters
        assertEquals(buttonRowStyle.verticalAlignment, style.buttonRowStyle.verticalAlignment)
        assertEquals(buttonRowStyle.horizontalArrangement, style.buttonRowStyle.horizontalArrangement)
        assertEquals(buttonRowStyle.modifier, style.buttonRowStyle.modifier)

        // verify button style parameters
        assertEquals(2, style.buttonStyle.size)
        for (builtButtonStyle in style.buttonStyle) {
            assertEquals(buttonStyle.shape, builtButtonStyle.shape)
            assertEquals(buttonStyle.border, builtButtonStyle.border)
            assertEquals(buttonStyle.colors, builtButtonStyle.colors)
            assertEquals(buttonStyle.contentPadding, builtButtonStyle.contentPadding)
            assertEquals(buttonStyle.elevation, builtButtonStyle.elevation)
            assertEquals(buttonStyle.enabled, builtButtonStyle.enabled)
            assertEquals(buttonStyle.modifier, builtButtonStyle.modifier)
            assertEquals(buttonStyle.buttonTextStyle?.textStyle, builtButtonStyle.buttonTextStyle?.textStyle)
            assertEquals(buttonStyle.buttonTextStyle?.overflow, builtButtonStyle.buttonTextStyle?.overflow)
            assertEquals(buttonStyle.buttonTextStyle?.softWrap, builtButtonStyle.buttonTextStyle?.softWrap)
            assertEquals(buttonStyle.buttonTextStyle?.maxLines, builtButtonStyle.buttonTextStyle?.maxLines)
            assertEquals(buttonStyle.buttonTextStyle?.minLines, builtButtonStyle.buttonTextStyle?.minLines)
            assertEquals(buttonStyle.buttonTextStyle?.modifier, builtButtonStyle.modifier)
        }

        // verify dismiss button style parameters
        assertEquals(dismissButtonStyle.tint, style.dismissButtonStyle.tint)
        assertEquals(dismissButtonStyle.contentDescription, style.dismissButtonStyle.contentDescription)
        assertEquals(dismissButtonStyle.modifier, style.dismissButtonStyle.modifier)

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)

        // verify card style parameters
        assertEquals(cardStyle.border, style.cardStyle.border)
        assertEquals(cardStyle.colors, style.cardStyle.colors)
        assertEquals(cardStyle.elevation, style.cardStyle.elevation)
        assertEquals(cardStyle.modifier, style.cardStyle.modifier)
        assertEquals(cardStyle.shape, style.cardStyle.shape)

        // verify root row style parameters
        assertEquals(rootRowStyle.verticalAlignment, style.rootRowStyle.verticalAlignment)
        assertEquals(rootRowStyle.horizontalArrangement, style.rootRowStyle.horizontalArrangement)
        assertEquals(rootRowStyle.modifier, style.rootRowStyle.modifier)

        // verify text column style parameters
        assertEquals(textColumnStyle.modifier, style.textColumnStyle.modifier)
        assertEquals(textColumnStyle.horizontalAlignment, style.textColumnStyle.horizontalAlignment)
        assertEquals(textColumnStyle.verticalArrangement, style.textColumnStyle.verticalArrangement)
    }

    @Test
    fun `create SmallImageUIStyle with some custom builder styles used`() {
        val imageStyle = AepImageStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            contentDescription = "content description"
        )
        val titleTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            textStyle = TextStyle(color = Color.Red, fontSize = 16.sp)
        )
        val bodyTextStyle = AepTextStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )
        val buttonRowStyle = AepRowStyle(
            verticalAlignment = Alignment.CenterVertically
        )
        val dismissButtonStyle = AepIconStyle(
            tint = { Color.Blue }
        )
        val dismissButtonAlignment = Alignment.BottomEnd
        val cardStyle = AepCardStyle(
            modifier = Modifier.size(100.dp, 100.dp)
        )
        val rootRowStyle = AepRowStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalAlignment = Alignment.CenterVertically
        )
        val textColumnStyle = AepColumnStyle(
            modifier = Modifier.size(100.dp, 100.dp),
            verticalArrangement = Arrangement.Center,
        )

        val style = SmallImageUIStyle.Builder()
            .imageStyle(imageStyle)
            .titleAepTextStyle(titleTextStyle)
            .bodyAepTextStyle(bodyTextStyle)
            .buttonRowStyle(buttonRowStyle)
            .dismissButtonStyle(dismissButtonStyle)
            .dismissButtonAlignment(dismissButtonAlignment)
            .cardStyle(cardStyle)
            .rootRowStyle(rootRowStyle)
            .textColumnStyle(textColumnStyle)
            .build()

        // verify image style parameters
        assertEquals(defaultSmallImageUIStyle.imageStyle.alpha, style.imageStyle.alpha)
        assertEquals(defaultSmallImageUIStyle.imageStyle.colorFilter, style.imageStyle.colorFilter)
        assertEquals(imageStyle.contentDescription, style.imageStyle.contentDescription)
        assertEquals(defaultSmallImageUIStyle.imageStyle.alignment, style.imageStyle.alignment)
        assertEquals(defaultSmallImageUIStyle.imageStyle.contentScale, style.imageStyle.contentScale)
        assertEquals(imageStyle.modifier, style.imageStyle.modifier)

        // verify title text style parameters
        assertEquals(titleTextStyle.textStyle, style.titleTextStyle.textStyle)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.overflow, style.titleTextStyle.overflow)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.softWrap, style.titleTextStyle.softWrap)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.maxLines, style.titleTextStyle.maxLines)
        assertEquals(defaultSmallImageUIStyle.titleTextStyle.minLines, style.titleTextStyle.minLines)
        assertEquals(titleTextStyle.modifier, style.titleTextStyle.modifier)

        // verify body text style parameters
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.textStyle, style.bodyTextStyle.textStyle)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.overflow, style.bodyTextStyle.overflow)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.softWrap, style.bodyTextStyle.softWrap)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.maxLines, style.bodyTextStyle.maxLines)
        assertEquals(defaultSmallImageUIStyle.bodyTextStyle.minLines, style.bodyTextStyle.minLines)
        assertEquals(bodyTextStyle.modifier, style.bodyTextStyle.modifier)

        // verify button row style parameters
        assertEquals(buttonRowStyle.verticalAlignment, style.buttonRowStyle.verticalAlignment)
        assertEquals(defaultSmallImageUIStyle.buttonRowStyle.horizontalArrangement, style.buttonRowStyle.horizontalArrangement)
        assertEquals(buttonRowStyle.modifier, style.buttonRowStyle.modifier)

        // verify button style parameters, should be all default values as no button style is set on the builder
        var index = 0
        assertEquals(3, style.buttonStyle.size)
        for (builtButtonStyle in style.buttonStyle) {
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].shape, builtButtonStyle.shape)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].border, builtButtonStyle.border)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].colors, builtButtonStyle.colors)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].contentPadding, builtButtonStyle.contentPadding)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].elevation, builtButtonStyle.elevation)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].enabled, builtButtonStyle.enabled)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].modifier, builtButtonStyle.modifier)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].buttonTextStyle?.textStyle, builtButtonStyle.buttonTextStyle?.textStyle)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].buttonTextStyle?.overflow, builtButtonStyle.buttonTextStyle?.overflow)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].buttonTextStyle?.softWrap, builtButtonStyle.buttonTextStyle?.softWrap)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].buttonTextStyle?.maxLines, builtButtonStyle.buttonTextStyle?.maxLines)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].buttonTextStyle?.minLines, builtButtonStyle.buttonTextStyle?.minLines)
            assertEquals(defaultSmallImageUIStyle.buttonStyle[index].buttonTextStyle?.modifier, builtButtonStyle.buttonTextStyle?.modifier)
            index++
        }

        // verify dismiss button style parameters
        assertEquals(dismissButtonStyle.tint, style.dismissButtonStyle.tint)
        assertEquals(defaultSmallImageUIStyle.dismissButtonStyle.contentDescription, style.dismissButtonStyle.contentDescription)
        assertEquals(defaultSmallImageUIStyle.dismissButtonStyle.modifier, style.dismissButtonStyle.modifier)

        // verify dismiss button alignment
        assertEquals(dismissButtonAlignment, style.dismissButtonAlignment)

        // verify card style parameters
        assertEquals(defaultSmallImageUIStyle.cardStyle.border, style.cardStyle.border)
        assertEquals(defaultSmallImageUIStyle.cardStyle.colors, style.cardStyle.colors)
        assertEquals(defaultSmallImageUIStyle.cardStyle.elevation, style.cardStyle.elevation)
        assertEquals(cardStyle.modifier, style.cardStyle.modifier)
        assertEquals(defaultSmallImageUIStyle.cardStyle.shape, style.cardStyle.shape)

        // verify root row style parameters
        assertEquals(rootRowStyle.verticalAlignment, style.rootRowStyle.verticalAlignment)
        assertEquals(defaultSmallImageUIStyle.rootRowStyle.horizontalArrangement, style.rootRowStyle.horizontalArrangement)
        assertEquals(rootRowStyle.modifier, style.rootRowStyle.modifier)

        // verify text column style parameters
        assertEquals(textColumnStyle.modifier, style.textColumnStyle.modifier)
        assertEquals(defaultSmallImageUIStyle.textColumnStyle.horizontalAlignment, style.textColumnStyle.horizontalAlignment)
        assertEquals(textColumnStyle.verticalArrangement, style.textColumnStyle.verticalArrangement)
    }
}
