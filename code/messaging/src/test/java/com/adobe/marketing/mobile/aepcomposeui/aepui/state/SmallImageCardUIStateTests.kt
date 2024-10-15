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

package com.adobe.marketing.mobile.aepcomposeui.aepui.state

import org.junit.Assert.assertEquals
import org.junit.Test

class SmallImageCardUIStateTests {

    @Test
    fun test_SmallImageCardUIState_initialState() {
        val state = SmallImageCardUIState()
        assertEquals(false, state.timerDisplayed)
        assertEquals(false, state.cardExpanded)
        assertEquals(false, state.dismissed)
        assertEquals(false, state.selected)
        assertEquals(false, state.read)
    }

    @Test
    fun test_SmallImageCardUIState_updateState() {
        val updatedState = SmallImageCardUIState(timerDisplayed = true, cardExpanded = true, dismissed = true, selected = true, read = true)
        assertEquals(true, updatedState.timerDisplayed)
        assertEquals(true, updatedState.cardExpanded)
        assertEquals(true, updatedState.dismissed)
        assertEquals(true, updatedState.selected)
        assertEquals(true, updatedState.read)
    }

    @Test
    fun test_SmallImageCardUIState_updateTimerDisplayed() {
        val state = SmallImageCardUIState()
        state.timerDisplayed = true
        assertEquals(true, state.timerDisplayed)
    }

    @Test
    fun test_SmallImageCardUIState_updateCardExpanded() {
        val state = SmallImageCardUIState()
        state.cardExpanded = true
        assertEquals(true, state.cardExpanded)
    }

    @Test
    fun test_SmallImageCardUIState_updateDismissed() {
        val state = SmallImageCardUIState()
        state.dismissed = true
        assertEquals(true, state.dismissed)
    }

    @Test
    fun test_SmallImageCardUIState_updateSelected() {
        val state = SmallImageCardUIState()
        state.selected = true
        assertEquals(true, state.selected)
    }

    @Test
    fun test_SmallImageCardUIState_updateRead() {
        val state = SmallImageCardUIState()
        state.read = true
        assertEquals(true, state.read)
    }
}
