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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class AepIconStyleTests {

    @Test
    fun `merge with overriding style`() {
        val defaultStyle = AepIconStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            tint = mock(Color::class.java)
        )
        val overridingStyle = AepIconStyle(
            modifier = Modifier.padding(16.dp),
            contentDescription = "overriding description",
            tint = mock(Color::class.java)
        )

        val result = AepIconStyle.merge(defaultStyle, overridingStyle)

        AepStyleValidator.validateIconStyle(overridingStyle, result)
    }

    @Test
    fun `merge with overriding style with some parameters overriden`() {
        val defaultStyle = AepIconStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            tint = mock(Color::class.java)
        )
        val overridingStyle = AepIconStyle(
            modifier = Modifier.padding(16.dp),
            contentDescription = "overriden description",
            tint = null
        )

        val result = AepIconStyle.merge(defaultStyle, overridingStyle)

        assertEquals(overridingStyle.modifier, result.modifier)
        assertEquals(overridingStyle.contentDescription, result.contentDescription)
        assertEquals(defaultStyle.tint, result.tint)
    }

    @Test
    fun `merge with null style`() {
        val defaultStyle = AepIconStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            tint = mock(Color::class.java)
        )

        val result = AepIconStyle.merge(defaultStyle, null)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentDescription, result.contentDescription)
        assertEquals(defaultStyle.tint, result.tint)
    }

    @Test
    fun `merge with null properties in overriding style`() {
        val defaultStyle = AepIconStyle(
            modifier = Modifier.padding(8.dp),
            contentDescription = "default description",
            tint = mock(Color::class.java)
        )
        val overridingStyle = AepIconStyle(
            modifier = null,
            contentDescription = null,
            tint = null
        )

        val result = AepIconStyle.merge(defaultStyle, overridingStyle)

        assertEquals(defaultStyle.modifier, result.modifier)
        assertEquals(defaultStyle.contentDescription, result.contentDescription)
        assertEquals(defaultStyle.tint, result.tint)
    }
}
