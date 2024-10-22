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

package com.adobe.marketing.mobile.aepcomposeui.aepui

import com.adobe.marketing.mobile.aepcomposeui.aepui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepuitemplates.SmallImageTemplate
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepText
import org.junit.Assert.assertEquals
import org.junit.Test

class SmallImageUiTests {

    @Test
    fun test_SmallImageUi_initialState() {
        val template = SmallImageTemplate(
            "testId",
            AepText("Card Title")
        )
        val initialState = SmallImageCardUIState()
        val ui = SmallImageUI(template, initialState)
        assertEquals(initialState, ui.getState())
    }

    @Test
    fun test_SmallImageUi_updateState() {
        val template = SmallImageTemplate(
            "testId",
            AepText("Card Title")
        )
        val initialState = SmallImageCardUIState()
        val ui = SmallImageUI(template, initialState)
        val newState = SmallImageCardUIState(dismissed = true, selected = true)
        ui.updateState(newState)
        assertEquals(newState, ui.getState())
    }

    @Test
    fun test_SmallImageUi_getTemplate() {
        val template = SmallImageTemplate(
            "testId",
            AepText("Card Title")
        )
        val initialState = SmallImageCardUIState()
        val ui = SmallImageUI(template, initialState)
        assertEquals(template, ui.getTemplate())
    }
}
