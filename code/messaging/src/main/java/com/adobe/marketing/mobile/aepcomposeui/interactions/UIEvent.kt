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

package com.adobe.marketing.mobile.aepcomposeui.interactions

import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.state.AepCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepUITemplate
import com.adobe.marketing.mobile.aepcomposeui.utils.UIAction

/**
 * Represents different types of UI events that can be triggered by the user interaction on the UI templates.
 *
 * @param T represents UI template model associated like [SmallImageTemplate], which backs the composable on which the event has occurred.
 * @param S representing the state of the AEP card composable on which the event has occurred.
 */
sealed interface UIEvent<T : AepUITemplate, S : AepCardUIState> {
    /**
     * Event that represents the display of a UI element.
     *
     * @param T represents UI template model associated like [SmallImageTemplate], which backs the composable on which the event has occurred.
     * @param S representing the state of the AEP card composable on which the event has occurred.
     * @property aepUi The [AepUI] associated with the display event.
     */
    data class Display<T : AepUITemplate, S : AepCardUIState>(val aepUi: AepUI<T, S>) :
        UIEvent<T, S>

    /**
     * Event that represents a user interaction with a UI element.
     *
     * @param T represents UI template model associated like [SmallImageTemplate], which backs the composable on which the event has occurred.
     * @param S representing the state of the AEP card composable on which the event has occurred.
     * @property aepUi The [AepUI] associated with the interaction event, providing context about the UI component on which the interaction occurred.
     * @property action The [UIAction] that occurred.
     *
     * The `Interact` event captures the different types of interactions that a user can have with a UI component,
     * like clicking a button or expanding a card. Limiting the interaction types ensures consistency in event
     * generation and handling.
     *
     * Example:
     * ```
     * observer?.onEvent(AepUiEvent.Interact(ui, interactId = "purchaseID", action = UIAction.CLICK), actionUrl = "https://www.adobe.com")
     * ```
     */
    data class Interact<T : AepUITemplate, S : AepCardUIState>(
        val aepUi: AepUI<T, S>,
        val action: UIAction
    ) : UIEvent<T, S>

    /**
     * Event that represents the dismissal of a UI element.
     *
     * @param T represents UI template model associated like [SmallImageTemplate], which backs the composable on which the event has occurred.
     * @param S representing the state of the AEP card composable on which the event has occurred.
     * @property aepUi The [AepUI] associated with the dismiss event.
     */
    data class Dismiss<T : AepUITemplate, S : AepCardUIState>(val aepUi: AepUI<T, S>) :
        UIEvent<T, S>
}
