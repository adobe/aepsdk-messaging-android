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

import com.adobe.marketing.mobile.aepcomposeui.state.InboxContainerUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.InboxContainerUITemplate

/**
 * Implementation of the [AepContainerUI] interface used in rendering a UI for an [InboxContainerUITemplate].
 *
 * @param template The template associated with the inbox container UI.
 * @param state The current state of the inbox container UI.
 */
class InboxContainerUI(
    private val template: InboxContainerUITemplate,
    state: InboxContainerUIState
) : BaseContainerUI<InboxContainerUITemplate, InboxContainerUIState> (template, state)
