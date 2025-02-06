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

package com.adobe.marketing.mobile.aepcomposeui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import kotlin.test.BeforeTest

class UIEventTests {

    @Mock
    private lateinit var mockAepUI: AepUI<*, *>

    @BeforeTest
    fun setup() {
        mockAepUI = mock(AepUI::class.java)
    }

    @Test
    fun `Create a UIEvent Display object`() {
        val event = UIEvent.Display(mockAepUI)
        assertEquals(mockAepUI, event.aepUi)
    }

    @Test
    fun `Create a UIEvent Interact object and verify the specified UIAction`() {
        val action = UIAction.Click(id = "button1", actionUrl = "http://example.com")
        val event = UIEvent.Interact(mockAepUI, action)
        assertEquals(mockAepUI, event.aepUi)
        assertEquals(action, event.action)
    }

    @Test
    fun `Create a UIEvent Dismiss object`() {
        val event = UIEvent.Dismiss(mockAepUI)
        assertEquals(mockAepUI, event.aepUi)
    }
}
