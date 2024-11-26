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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AepRowStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepRowStyle(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        )
        val overridingStyle = AepRowStyle(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        )

        val result = AepRowStyle.merge(defaultStyle, overridingStyle)

        AepStyleValidator.validateRowStyle(overridingStyle, result)
    }

    @Test
    fun `merge with overriding style with some parameters overriden`() {
        val defaultStyle = AepRowStyle(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        )
        val overridingStyle = AepRowStyle(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = null
        )

        val result = AepRowStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.horizontalArrangement, result.horizontalArrangement)
        assertEquals(defaultStyle.verticalAlignment, result.verticalAlignment)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepRowStyle(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        )

        val result = AepRowStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.horizontalArrangement, result.horizontalArrangement)
        assertEquals(defaultStyle.verticalAlignment, result.verticalAlignment)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepRowStyle(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        )
        val overridingStyle = AepRowStyle(
            modifier = null,
            horizontalArrangement = null,
            verticalAlignment = null
        )

        val result = AepRowStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.horizontalArrangement, result.horizontalArrangement)
        assertEquals(defaultStyle.verticalAlignment, result.verticalAlignment)
    }
}
