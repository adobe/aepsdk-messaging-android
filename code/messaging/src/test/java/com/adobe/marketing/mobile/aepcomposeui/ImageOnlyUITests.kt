/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui

import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepImage
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOnlyUITests {

    @Test
    fun test_ImageOnlyUI_initialState() {
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://example.com/image.png")
        )
        val initialState = ImageOnlyCardUIState()
        val ui = ImageOnlyUI(template, initialState)
        assertEquals(initialState, ui.getState())
    }

    @Test
    fun test_ImageOnlyUI_updateState() {
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://example.com/image.png")
        )
        val initialState = ImageOnlyCardUIState()
        val ui = ImageOnlyUI(template, initialState)
        val newState = ImageOnlyCardUIState(dismissed = true)
        ui.updateState(newState)
        assertEquals(newState, ui.getState())
    }

    @Test
    fun test_ImageOnlyUI_getTemplate() {
        val template = ImageOnlyTemplate(
            "testId",
            AepImage("https://example.com/image.png")
        )
        val initialState = ImageOnlyCardUIState()
        val ui = ImageOnlyUI(template, initialState)
        assertEquals(template, ui.getTemplate())
    }
}
