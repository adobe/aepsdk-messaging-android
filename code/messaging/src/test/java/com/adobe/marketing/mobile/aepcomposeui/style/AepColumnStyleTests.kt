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

class AepColumnStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepColumnStyle(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        )
        val overridingStyle = AepColumnStyle(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        )

        val result = AepColumnStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(overridingStyle.horizontalAlignment, result.horizontalAlignment)
    }

    @Test
    fun `merge with overriding style containing modifier only`() {
        val defaultStyle = AepColumnStyle(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        )
        val overridingStyle = AepColumnStyle(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = null,
            horizontalAlignment = null
        )

        val result = AepColumnStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(defaultStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(defaultStyle.horizontalAlignment, result.horizontalAlignment)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepColumnStyle(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        )

        val result = AepColumnStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(defaultStyle.horizontalAlignment, result.horizontalAlignment)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepColumnStyle(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        )
        val overridingStyle = AepColumnStyle(
            modifier = null,
            verticalArrangement = null,
            horizontalAlignment = null
        )

        val result = AepColumnStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.verticalArrangement, result.verticalArrangement)
        assertEquals(defaultStyle.horizontalAlignment, result.horizontalAlignment)
    }
}