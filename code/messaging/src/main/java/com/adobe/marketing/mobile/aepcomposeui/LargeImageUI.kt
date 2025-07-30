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

import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate

/**
 * Implementation of the [AepUI] interface used in rendering a UI for a [LargeImageTemplate].
 *
 * @property template The template associated with the large image UI.
 * @property _state The current state of the large image UI.
 */
class LargeImageUI(
    private val template: LargeImageTemplate,
    state: LargeImageCardUIState
) : AepUI<LargeImageTemplate, LargeImageCardUIState> {
    private val _state = mutableStateOf(state)

    /**
     * Updates the current state of the large image UI.
     *
     * @param newState The new state to be set.
     */
    override fun updateState(newState: LargeImageCardUIState) {
        _state.value = newState
    }

    /**
     * Retrieves the template associated with the large image UI.
     *
     * @return The large image template.
     */
    override fun getTemplate(): LargeImageTemplate {
        return template
    }

    /**
     * Retrieves the current state of the large image UI.
     *
     * @return The current UI state.
     */
    override fun getState(): LargeImageCardUIState {
        return _state.value
    }
}
