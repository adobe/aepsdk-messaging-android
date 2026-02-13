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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adobe.marketing.mobile.aepcomposeui.AepUI
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxTemplate

/**
 * Sealed interface representing different states for Inbox UI.
 */
sealed interface InboxUIState {

    /**
     * Loading state for the inbox.
     */
    object Loading : InboxUIState

    /**
     * Success state for the inbox.
     *
     * @param template The properties to be used for rendering the inbox
     * @param items List of AEP UI elements to display in the inbox
     */
    data class Success(
        val template: InboxTemplate,
        val items: List<AepUI<*, *>>
    ) : InboxUIState {
        /**
         * Tracks whether the inbox display event has already been fired for this state instance.
         * Backed by Compose [mutableStateOf] so that reads during composition are observable.
         */
        internal var displayed by mutableStateOf(false)
    }

    /**
     * Error state for the inbox.
     *
     * @param error The throwable that caused the error
     */
    data class Error(
        val error: Throwable
    ) : InboxUIState
}
