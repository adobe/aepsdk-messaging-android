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

import com.adobe.marketing.mobile.aepcomposeui.state.AepContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepContainerUITemplate

/**
 * Represents a container the holds a list of [AepUI]s and a template that defines how the UI for the list is rendered.
 * This interface can be implemented by different container types, such as lists or carousels.
 *
 * @param T The type that defines the layout and style for the container.
 * @param S The type that defines the state for the container.
 */
sealed interface AepContainerUI<T : AepContainerUITemplate, S : AepContainerUIState> {
    /**
     * Retrieves the template associated with this container UI.
     * The template defines the structure and appearance of the container.
     *
     * @return The template of type [T] associated with the container UI.
     */
    fun getAepContainerTemplate(): T

    /**
     * Retrieves the list of [AepUI] elements contained within this container.
     *
     * @return A list of [AepUI] elements.
     */
    fun getAepUIList(): List<AepUI<*, *>>

    /**
     * Retrieves the current state of the container UI.
     * The state defines the dynamic behaviors or status of the container.
     *
     * @return The current state of type [S] associated with the container UI.
     */
    fun getAepContainerState(): S

    /**
     * Updates the state of the container UI with a new state.
     * Updates to the state will trigger the UI to be recomposed, reflecting the changes in the rendered UI.
     *
     * @param newState The new state of type [S] to update the container UI.
     */
    fun updateAepContainerState(newState: S)
}
