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

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AepLazyColumnStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = true
        )
        val overridingStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(16.dp),
            contentPadding = PaddingValues(8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = false
        )

        val result = AepLazyColumnStyle.merge(defaultStyle, overridingStyle)

        AepStyleValidator.validateLazyColumnStyle(overridingStyle, result)
    }

    @Test
    fun `merge with overriding style with some parameters overriden`() {
        val defaultStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = true
        )
        val overridingStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(16.dp),
            contentPadding = PaddingValues(8.dp),
            reverseLayout = null,
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = null,
            flingBehavior = null,
            userScrollEnabled = false
        )

        val result = AepLazyColumnStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.contentPadding, result.contentPadding)
        assertEquals(defaultStyle.reverseLayout, result.reverseLayout)
        assertEquals(overridingStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(defaultStyle.horizontalAlignment, result.horizontalAlignment)
        assertEquals(defaultStyle.flingBehavior, result.flingBehavior)
        assertEquals(overridingStyle.userScrollEnabled, result.userScrollEnabled)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = true
        )

        val result = AepLazyColumnStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentPadding, result.contentPadding)
        assertEquals(defaultStyle.reverseLayout, result.reverseLayout)
        assertEquals(defaultStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(defaultStyle.horizontalAlignment, result.horizontalAlignment)
        assertEquals(defaultStyle.flingBehavior, result.flingBehavior)
        assertEquals(defaultStyle.userScrollEnabled, result.userScrollEnabled)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepLazyColumnStyle(
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            flingBehavior = mock(FlingBehavior::class.java),
            userScrollEnabled = true
        )
        val overridingStyle = AepLazyColumnStyle(
            modifier = null,
            contentPadding = null,
            reverseLayout = null,
            verticalArrangement = null,
            horizontalAlignment = null,
            flingBehavior = null,
            userScrollEnabled = null
        )

        val result = AepLazyColumnStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentPadding, result.contentPadding)
        assertEquals(defaultStyle.reverseLayout, result.reverseLayout)
        assertEquals(defaultStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(defaultStyle.horizontalAlignment, result.horizontalAlignment)
        assertEquals(defaultStyle.flingBehavior, result.flingBehavior)
        assertEquals(defaultStyle.userScrollEnabled, result.userScrollEnabled)
    }
}
