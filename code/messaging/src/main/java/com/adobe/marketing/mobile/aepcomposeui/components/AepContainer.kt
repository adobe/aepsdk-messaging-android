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
import com.adobe.marketing.mobile.aepcomposeui.AepContainerUI
import com.adobe.marketing.mobile.aepcomposeui.InboxContainerUI
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.ContainerStyle

/**
 * AEP Container Composable that renders the appropriate container based on the type of [AepContainerUI] provided.
 *
 * @param containerUi The AEP container UI model to be rendered.
 * @param containerStyle The style to be applied to the container.
 * @param itemsStyle The style to be applied to the cards within the container.
 * @param cardUIEventListener An optional event listener for content card UI events.
 */
@Composable
fun AepContainer(
    containerUi: AepContainerUI<*, *>,
    containerStyle: ContainerStyle = ContainerStyle(),
    itemsStyle: AepUIStyle = AepUIStyle(),
    observer: AepUIEventObserver? = null
) {
    when (containerUi) {
        is InboxContainerUI -> {
            InboxContainer(
                ui = containerUi,
                inboxContainerStyle = containerStyle.inboxContainerUIStyle,
                itemsStyle = itemsStyle,
                observer = observer
            )
        }
    }
}
