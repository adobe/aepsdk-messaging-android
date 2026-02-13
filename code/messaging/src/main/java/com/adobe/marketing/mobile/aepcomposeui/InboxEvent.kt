/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui

import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState

/**
 * Represents different types of inbox-level events triggered by lifecycle changes
 * or user interactions with the inbox container.
 *
 * Unlike [UIEvent], which represents events on individual cards (display, dismiss, interact),
 * [InboxEvent] represents events on the inbox as a whole.
 *
 * @property inboxUIState The current state of the inbox UI when the event occurred.
 */
sealed class InboxEvent(open val inboxUIState: InboxUIState) {

    /**
     * Event indicating that the inbox has been displayed to the user.
     *
     * @property inboxUIState The success state of the inbox UI when the display event occurred.
     */
    data class Display(override val inboxUIState: InboxUIState.Success) : InboxEvent(inboxUIState)
}
