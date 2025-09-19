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
 * Sealed class representing different events related to the lifecycle of an AEP container UI.
 *
 * @param T The type of [AepContainerUITemplate] associated with the event.
 * @property aepContainerUI The AEP container UI associated with the event.
 */
sealed class ContainerEvent<T : AepContainerUITemplate, S : AepContainerUIState>(open val aepContainerUI: AepContainerUI<T, S>) {
    // todo: This is a temporary list subject to change
    /**
     * Event indicating that the container UI contents are refreshed.
     *
     * @param aepContainerUI The AEP container UI associated with the loading event.
     */
    data class Refreshed<T : AepContainerUITemplate, S : AepContainerUIState>(override val aepContainerUI: AepContainerUI<T, S>) : ContainerEvent<T, S>(aepContainerUI)
}
