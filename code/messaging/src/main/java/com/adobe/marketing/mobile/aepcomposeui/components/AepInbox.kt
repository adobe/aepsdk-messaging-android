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

package com.adobe.marketing.mobile.aepcomposeui.components

import androidx.compose.runtime.Composable
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle
import com.adobe.marketing.mobile.aepcomposeui.uimodels.AepInboxLayout

/**
 * AepInboxComposable that renders the inbox based on the properties of the provided [InboxUIState].
 *
 * @param uiState The [InboxUIState] model to be rendered.
 * @param inboxStyle The style to be applied to the inbox.
 * @param itemsStyle The style to be applied to the cards within the inbox.
 * @param observer An optional event listener for content card UI events.
 */
@Composable
fun AepInbox(
    uiState: InboxUIState,
    inboxStyle: InboxUIStyle = InboxUIStyle.Builder().build(),
    itemsStyle: AepUIStyle = AepUIStyle(),
    // todo: implement inbox observer with specific inbox events
    observer: AepUIEventObserver? = null
) {
    when (uiState) {
        is InboxUIState.Loading -> {
            inboxStyle.loadingView()
        }

        is InboxUIState.Error -> {
            inboxStyle.errorView()
        }

        is InboxUIState.Success -> {
            when (uiState.template.layout) {
                AepInboxLayout.VERTICAL -> {
                    VerticalInbox(
                        ui = uiState,
                        inboxStyle = inboxStyle,
                        itemsStyle = itemsStyle,
                        observer = observer
                    )
                }
                AepInboxLayout.HORIZONTAL -> {
                    HorizontalInbox(
                        ui = uiState,
                        inboxStyle = inboxStyle,
                        itemsStyle = itemsStyle,
                        observer = observer
                    )
                }
            }
        }
    }
}
