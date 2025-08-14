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

package com.adobe.marketing.mobile.aepcomposeui.aepui.state

import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageOnlyCardUIStateTests {

    @Test
    fun test_ImageOnlyCardUIState_initialState() {
        val state = ImageOnlyCardUIState()
        assertEquals(false, state.dismissed)
    }

    @Test
    fun test_ImageOnlyCardUIState_updateState() {
        val updatedState = ImageOnlyCardUIState(dismissed = true)
        assertEquals(true, updatedState.dismissed)
    }

    @Test
    fun test_ImageOnlyCardUIState_updateDismissed() {
        var state = ImageOnlyCardUIState()
        state = state.copy(dismissed = true)
        assertEquals(true, state.dismissed)
    }
}
