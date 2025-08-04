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
import com.adobe.marketing.mobile.aepcomposeui.state.ImageOnlyCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.ImageOnlyTemplate

/**
 * Implementation of the [AepUI] interface used in rendering a UI for a [ImageOnlyTemplate].
 *
 * @property template The template associated with the image only UI.
 * @property _state The current state of the image only UI.
 */
class ImageOnlyUI(
    private val template: ImageOnlyTemplate,
    state: ImageOnlyCardUIState
) : AepUI<ImageOnlyTemplate, ImageOnlyCardUIState> {
    private val _state = mutableStateOf(state)

    /**
     * Updates the current state of the image only UI.
     *
     * @param newState The new state to be set.
     */
    override fun updateState(newState: ImageOnlyCardUIState) {
        _state.value = newState
    }

    /**
     * Retrieves the template associated with the image only UI.
     *
     * @return The image only template.
     */
    override fun getTemplate(): ImageOnlyTemplate {
        return template
    }

    /**
     * Retrieves the current state of the image only UI.
     *
     * @return The current UI state.
     */
    override fun getState(): ImageOnlyCardUIState {
        return _state.value
    }
}
