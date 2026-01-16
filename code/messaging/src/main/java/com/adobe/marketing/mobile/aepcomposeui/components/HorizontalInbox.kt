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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import com.adobe.marketing.mobile.aepcomposeui.observers.AepUIEventObserver
import com.adobe.marketing.mobile.aepcomposeui.state.InboxUIState
import com.adobe.marketing.mobile.aepcomposeui.style.AepUIStyle
import com.adobe.marketing.mobile.aepcomposeui.style.InboxUIStyle

@Composable
internal fun HorizontalInbox(
    ui: InboxUIState.Success,
    inboxStyle: InboxUIStyle,
    itemsStyle: AepUIStyle,
    observer: AepUIEventObserver?
) {
    // todo: implement horizontal inbox
}

private fun getDefaultHorizontalArrangement(reverseLayout: Boolean): Arrangement.Horizontal =
    if (reverseLayout) Arrangement.End else Arrangement.Start
