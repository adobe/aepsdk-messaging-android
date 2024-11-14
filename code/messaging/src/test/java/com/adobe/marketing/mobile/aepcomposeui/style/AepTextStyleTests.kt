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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AepTextStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepTextStyle(
            modifier = Modifier.padding(8.dp),
            textStyle = mock(TextStyle::class.java),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )
        val overridingStyle = AepTextStyle(
            modifier = Modifier.padding(16.dp),
            textStyle = mock(TextStyle::class.java),
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxLines = 2,
            minLines = 2
        )

        val result = AepTextStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.textStyle, result.textStyle)
        assertEquals(overridingStyle.overflow, result.overflow)
        assertEquals(overridingStyle.softWrap, result.softWrap)
        assertEquals(overridingStyle.maxLines, result.maxLines)
        assertEquals(overridingStyle.minLines, result.minLines)
    }

    @Test
    fun `merge with overriding style containing modifier only`() {
        val defaultStyle = AepTextStyle(
            modifier = Modifier.padding(8.dp),
            textStyle = mock(TextStyle::class.java),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )
        val overridingStyle = AepTextStyle(
            modifier = Modifier.padding(16.dp),
            textStyle = null,
            overflow = null,
            softWrap = null,
            maxLines = null,
            minLines = null
        )

        val result = AepTextStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(defaultStyle.textStyle, result.textStyle)
        assertEquals(defaultStyle.overflow, result.overflow)
        assertEquals(defaultStyle.softWrap, result.softWrap)
        assertEquals(defaultStyle.maxLines, result.maxLines)
        assertEquals(defaultStyle.minLines, result.minLines)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepTextStyle(
            modifier = Modifier.padding(8.dp),
            textStyle = mock(TextStyle::class.java),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )

        val result = AepTextStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.textStyle, result.textStyle)
        assertEquals(defaultStyle.overflow, result.overflow)
        assertEquals(defaultStyle.softWrap, result.softWrap)
        assertEquals(defaultStyle.maxLines, result.maxLines)
        assertEquals(defaultStyle.minLines, result.minLines)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepTextStyle(
            modifier = Modifier.padding(8.dp),
            textStyle = mock(TextStyle::class.java),
            overflow = TextOverflow.Clip,
            softWrap = true,
            maxLines = 1,
            minLines = 1
        )
        val overridingStyle = AepTextStyle(
            modifier = null,
            textStyle = null,
            overflow = null,
            softWrap = null,
            maxLines = null,
            minLines = null
        )

        val result = AepTextStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.textStyle, result.textStyle)
        assertEquals(defaultStyle.overflow, result.overflow)
        assertEquals(defaultStyle.softWrap, result.softWrap)
        assertEquals(defaultStyle.maxLines, result.maxLines)
        assertEquals(defaultStyle.minLines, result.minLines)
    }
}
