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

package com.adobe.marketing.mobile.aepcomposeui.uimodels

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AepColorTests {

    @Test
    fun `create AepColor with both light and dark colors`() {
        val lightColor = Color.White
        val darkColor = Color.Black

        val aepColor = AepColor(light = lightColor, dark = darkColor)

        assertEquals(lightColor, aepColor.light)
        assertEquals(darkColor, aepColor.dark)
    }

    @Test
    fun `create AepColor with only light color`() {
        val lightColor = Color.Red

        val aepColor = AepColor(light = lightColor, dark = null)

        assertEquals(lightColor, aepColor.light)
        assertNull(aepColor.dark)
    }

    @Test
    fun `create AepColor with only dark color`() {
        val darkColor = Color.Blue

        val aepColor = AepColor(light = null, dark = darkColor)

        assertNull(aepColor.light)
        assertEquals(darkColor, aepColor.dark)
    }

    @Test
    fun `create AepColor with both colors null`() {
        val aepColor = AepColor(light = null, dark = null)

        assertNull(aepColor.light)
        assertNull(aepColor.dark)
    }

    @Test
    fun `AepColor data class equality with same colors`() {
        val aepColor1 = AepColor(light = Color.Red, dark = Color.DarkGray)
        val aepColor2 = AepColor(light = Color.Red, dark = Color.DarkGray)

        assertEquals(aepColor1, aepColor2)
        assertEquals(aepColor1.hashCode(), aepColor2.hashCode())
    }

    @Test
    fun `AepColor data class inequality with different light color`() {
        val aepColor1 = AepColor(light = Color.Red, dark = Color.DarkGray)
        val aepColor2 = AepColor(light = Color.Blue, dark = Color.DarkGray)

        assertNotEquals(aepColor1, aepColor2)
    }

    @Test
    fun `AepColor data class inequality with different dark color`() {
        val aepColor1 = AepColor(light = Color.Red, dark = Color.DarkGray)
        val aepColor2 = AepColor(light = Color.Red, dark = Color.Black)

        assertNotEquals(aepColor1, aepColor2)
    }

    @Test
    fun `AepColor data class inequality when one has null`() {
        val aepColor1 = AepColor(light = Color.Red, dark = Color.DarkGray)
        val aepColor2 = AepColor(light = Color.Red, dark = null)

        assertNotEquals(aepColor1, aepColor2)
    }

    @Test
    fun `AepColor copy with modified light color`() {
        val original = AepColor(light = Color.Red, dark = Color.Black)

        val modified = original.copy(light = Color.Green)

        assertEquals(Color.Green, modified.light)
        assertEquals(original.dark, modified.dark)
    }

    @Test
    fun `AepColor copy with modified dark color`() {
        val original = AepColor(light = Color.Red, dark = Color.Black)

        val modified = original.copy(dark = Color.Gray)

        assertEquals(original.light, modified.light)
        assertEquals(Color.Gray, modified.dark)
    }

    @Test
    fun `AepColor copy setting color to null`() {
        val original = AepColor(light = Color.Red, dark = Color.Black)

        val modified = original.copy(light = null)

        assertNull(modified.light)
        assertEquals(original.dark, modified.dark)
    }

    @Test
    fun `AepColor with various color values`() {
        val colors = listOf(
            Color.Red,
            Color.Green,
            Color.Blue,
            Color.Yellow,
            Color.Cyan,
            Color.Magenta,
            Color.White,
            Color.Black,
            Color.Gray,
            Color.LightGray,
            Color.DarkGray,
            Color.Transparent
        )

        colors.forEach { color ->
            val aepColor = AepColor(light = color, dark = color)
            assertEquals(color, aepColor.light)
            assertEquals(color, aepColor.dark)
        }
    }

    @Test
    fun `AepColor with custom ARGB color`() {
        val customLight = Color(0xFFFF5733)
        val customDark = Color(0xFF1A1A2E)

        val aepColor = AepColor(light = customLight, dark = customDark)

        assertEquals(customLight, aepColor.light)
        assertEquals(customDark, aepColor.dark)
    }

    @Test
    fun `AepColor with semi-transparent colors`() {
        val semiTransparentLight = Color(0x80FF0000) // 50% transparent red
        val semiTransparentDark = Color(0x800000FF) // 50% transparent blue

        val aepColor = AepColor(light = semiTransparentLight, dark = semiTransparentDark)

        assertEquals(semiTransparentLight, aepColor.light)
        assertEquals(semiTransparentDark, aepColor.dark)
    }

    @Test
    fun `AepColor equality with both null values`() {
        val aepColor1 = AepColor(light = null, dark = null)
        val aepColor2 = AepColor(light = null, dark = null)

        assertEquals(aepColor1, aepColor2)
        assertEquals(aepColor1.hashCode(), aepColor2.hashCode())
    }

    @Test
    fun `AepColor toString contains color information`() {
        val aepColor = AepColor(light = Color.Red, dark = Color.Blue)

        val string = aepColor.toString()

        assertTrue("toString should contain 'AepColor'", string.contains("AepColor"))
        assertTrue("toString should contain 'light'", string.contains("light"))
        assertTrue("toString should contain 'dark'", string.contains("dark"))
    }

    @Test
    fun `AepColor destructuring declaration`() {
        val aepColor = AepColor(light = Color.White, dark = Color.Black)

        val (light, dark) = aepColor

        assertEquals(Color.White, light)
        assertEquals(Color.Black, dark)
    }
}
