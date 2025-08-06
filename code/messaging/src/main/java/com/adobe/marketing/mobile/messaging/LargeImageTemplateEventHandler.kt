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

package com.adobe.marketing.mobile.messaging

import com.adobe.marketing.mobile.aepcomposeui.UIEvent
import com.adobe.marketing.mobile.aepcomposeui.state.LargeImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.LargeImageTemplate

/**
 * Large Image Template Event Handler for content card events.
 *
 * @property callback An optional callback to invoke when a content card event occurs.
 */
internal class LargeImageTemplateEventHandler(private val callback: ContentCardUIEventListener?) :
    MessagingEventHandler<LargeImageTemplate, LargeImageCardUIState>(
        callback
    ) {
    override fun getNewState(event: UIEvent<LargeImageTemplate, LargeImageCardUIState>): LargeImageCardUIState {
        val currentState = event.aepUi.getState()
        return when (event) {
            is UIEvent.Dismiss -> currentState.copy(dismissed = true)
            is UIEvent.Display -> currentState.copy(displayed = true)
            else -> currentState
        }
    }
}
