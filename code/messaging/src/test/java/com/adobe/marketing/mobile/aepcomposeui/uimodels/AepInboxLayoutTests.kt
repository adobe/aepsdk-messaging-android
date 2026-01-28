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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AepInboxLayoutTests {

    @Test
    fun `Layout has correct typeName`() {
        assertEquals("vertical", AepInboxLayout.VERTICAL.typeName)
        assertEquals("horizontal", AepInboxLayout.HORIZONTAL.typeName)
    }

    @Test
    fun `from returns the correct typeName`() {
        assertEquals(AepInboxLayout.VERTICAL,  AepInboxLayout.from("vertical"))
        assertEquals(AepInboxLayout.HORIZONTAL,  AepInboxLayout.from("horizontal"))

    }

    @Test
    fun `from returns null for unknown typeName`() {
        val result = AepInboxLayout.from("unknown")
        assertNull(result)
    }

    @Test
    fun `from returns null for null typeName`() {
        val result = AepInboxLayout.from(null)
        assertNull(result)
    }

    @Test
    fun `from returns null for empty typeName`() {
        val result = AepInboxLayout.from("")
        assertNull(result)
    }

    @Test
    fun `from is case sensitive`() {
        val resultUppercase = AepInboxLayout.from("VERTICAL")
        val resultMixedCase = AepInboxLayout.from("Vertical")

        assertNull(resultUppercase)
        assertNull(resultMixedCase)
    }

    @Test
    fun `enum values contain all expected layouts`() {
        val values = AepInboxLayout.values()
        assertEquals(2, values.size)
        assertEquals(AepInboxLayout.VERTICAL, values[0])
        assertEquals(AepInboxLayout.HORIZONTAL, values[1])
    }
}
