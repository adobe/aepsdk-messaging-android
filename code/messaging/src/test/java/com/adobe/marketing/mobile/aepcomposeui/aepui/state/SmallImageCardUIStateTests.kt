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

import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import org.junit.Assert.assertEquals
import org.junit.Test

class SmallImageCardUIStateTests {

    @Test
    fun test_SmallImageCardUIState_initialState() {
        val state = SmallImageCardUIState()
        assertEquals(false, state.dismissed)
    }

    @Test
    fun test_SmallImageCardUIState_updateState() {
        val updatedState = SmallImageCardUIState(dismissed = true)
        assertEquals(true, updatedState.dismissed)
    }

    @Test
    fun test_SmallImageCardUIState_updateDismissed() {
        var state = SmallImageCardUIState()
        state = state.copy(dismissed = true)
        assertEquals(true, state.dismissed)
    }
}
