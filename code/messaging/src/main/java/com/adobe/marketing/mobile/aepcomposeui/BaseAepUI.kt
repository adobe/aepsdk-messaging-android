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
import com.adobe.marketing.mobile.aepcomposeui.state.AepCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate

/**
 * Base class for AEP UI components that implements the [AepUI] interface.
 *
 * @param T The type of the template associated with the AepUI.
 * @param S The type of the state associated with the AepUI.
 */
sealed class BaseAepUI<T : AepUITemplate, S : AepCardUIState>(
    private val template: T,
    state: S
) : AepUI<T, S> {
    private val _state = mutableStateOf(state)

    /**
     * Updates the current state of the AepUI.
     *
     * @param newState The new state to be set.
     */
    override fun updateState(newState: S) {
        _state.value = newState
    }

    /**
     * Retrieves the template associated with the AepUI.
     *
     * @return The template associated with the AepUI.
     */
    override fun getTemplate(): T {
        return template
    }

    /**
     * Retrieves the current state of the AepUI.
     *
     * @return The current state of the AepUI.
     */
    override fun getState(): S {
        return _state.value
    }
}
