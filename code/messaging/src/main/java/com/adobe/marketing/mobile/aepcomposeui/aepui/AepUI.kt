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

import com.adobe.marketing.mobile.aepcomposeui.aepui.state.AepCardUIState
import com.adobe.marketing.mobile.aepuitemplates.AepUITemplate

/**
 * Represents a UI component that can be rendered using AEP UI templates.
 * This interface can be implemented by various UI components like [SmallImageCardAepUi], [LargeImageCardAepUi], etc.
 * It associates a specific template and state to ensure consistency when rendering and managing UI components.
 *
 * @param T The type of the template associated with the UI component.
 * @param S The type of the state associated with the UI component.
 */

sealed interface AepUI<T : AepUITemplate, S : AepCardUIState> {
    /**
     * Retrieves the template associated with this UI component.
     * The template defines the structure and appearance of the UI element.
     *
     * @return The template of type [T] associated with the UI component.
     */
    fun getTemplate(): T

    /**
     * Retrieves the current state of the UI component.
     * The state defines the dynamic behaviors or status of the UI component.
     *
     * @return The current state of type [S] associated with the UI component.
     */
    fun getState(): S

    /**
     * Updates the state of the UI component with a new state.
     * Updates to the state will trigger the UI to be recomposed, reflecting the changes in the rendered UI.
     *
     * @param newState The new state of type [S] to update the UI component.
     */
    fun updateState(newState: S)
}
