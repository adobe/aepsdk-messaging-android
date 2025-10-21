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

package com.adobe.marketing.mobile.aepcomposeui.state

import com.adobe.marketing.mobile.aepcomposeui.AepUI

/**
 * Sealed interface representing the state of an AEP container UI.
 *
 * This interface provides a common structure for different states (Loading, Success, Error)
 * that can be extended by specific container types to include additional state information.
 *
 * Container-specific states should extend these base states to add their own properties
 * while maintaining the common state structure.
 */
sealed interface AepContainerUIState {

    /**
     * Represents the loading state of the container UI.
     * Can be extended by specific container types to include loading-specific information.
     */
    interface Loading : AepContainerUIState

    /**
     * Represents the successful state of the container UI.
     * Can be extended by specific container types to include their own success data.
     */
    interface Success : AepContainerUIState {
        val items: List<AepUI<*, *>>
    }

    /**
     * Represents the error state of the container UI.
     * Can be extended by specific container types to include error-specific information.
     */
    interface Error : AepContainerUIState {
        val error: Throwable
    }
}
