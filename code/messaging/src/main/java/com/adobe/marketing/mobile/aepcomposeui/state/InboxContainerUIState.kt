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
 * Sealed interface representing different states for Inbox Container UI.
 * Extends the base AepContainerUIState to provide inbox-specific state management.
 */
sealed interface InboxContainerUIState : AepContainerUIState {

    /**
     * Loading state for the inbox container.
     */
    object Loading : InboxContainerUIState

    /**
     * Success state for the inbox container.
     *
     * @param items List of AEP UI elements to display in the inbox
     */
    data class Success(
        override val items: List<AepUI<*, *>> = emptyList()
    ) : AepContainerUIState.Success, InboxContainerUIState

    /**
     * Error state for the inbox container.
     *
     * @param error The throwable that caused the error
     */
    data class Error(
        override val error: Throwable
    ) : AepContainerUIState.Error, InboxContainerUIState
}
