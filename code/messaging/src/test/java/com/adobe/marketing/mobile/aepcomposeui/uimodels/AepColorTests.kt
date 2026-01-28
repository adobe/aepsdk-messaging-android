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
import org.junit.Test

class AepColorTests {

    /**
     * Parameterized test data for AepColor creation tests.
     */
    private val creationTestCases: Map<Pair<Color?, Color?>, String> = mapOf(
        Pair(Color.White, Color.Black) to "both light and dark colors",
        Pair(Color.Red, null) to "only light color",
        Pair(null, Color.Blue) to "only dark color",
        Pair(null, null) to "both colors null",
        Pair(Color(0xFFFF5733), Color(0xFF1A1A2E)) to "custom ARGB colors",
        Pair(Color(0x80FF0000), Color(0x800000FF)) to "semi-transparent colors"
    )

    @Test
    fun `AepColor creation with parameterized test cases`() {
        creationTestCases.forEach { (colorPair, description) ->
            val (lightColor, darkColor) = colorPair
            val aepColor = AepColor(light = lightColor, dark = darkColor)

            assertEquals(
                "Failed for case: $description - light color mismatch",
                lightColor,
                aepColor.light
            )
            assertEquals(
                "Failed for case: $description - dark color mismatch",
                darkColor,
                aepColor.dark
            )
        }
    }
}
