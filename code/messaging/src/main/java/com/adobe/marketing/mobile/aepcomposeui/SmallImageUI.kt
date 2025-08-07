/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.aepcomposeui

import com.adobe.marketing.mobile.aepcomposeui.state.SmallImageCardUIState
import com.adobe.marketing.mobile.aepcomposeui.uimodels.SmallImageTemplate

/**
 * Implementation of the [AepUI] interface used in rendering a UI for a [SmallImageTemplate].
 *
 * @param template The template associated with the small image UI.
 * @param state The current state of the small image UI.
 */
class SmallImageUI(
    private val template: SmallImageTemplate,
    state: SmallImageCardUIState
) : BaseAepUI<SmallImageTemplate, SmallImageCardUIState>(template, state)
