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

package com.adobe.marketing.mobile.aepuitemplates.uimodels

import com.adobe.marketing.mobile.aepuitemplates.utils.AepUIConstants

/**
 * Represents a dismiss button for a content card in the small image template.
 *
 * The `AepDismissButton` class holds the styling information for the dismiss button.
 * If no style is provided, a default value of [AepUIConstants.CardTemplate.UIElement.DismissButton.NONE_ICON] is used.
 *
 * @property style The style of the dismiss button, which can be one of the following:
 * - `none`: No icon displayed.
 * - `simple`: A simple dismiss icon.
 * - `circle`: A circular dismiss icon.
 */
data class AepDismissButton(var style: String) {
    constructor(dismissButtonMap: Map<String, Any>) : this(
        style = dismissButtonMap[AepUIConstants.CardTemplate.UIElement.DismissButton.STYLE] as? String
            ?: AepUIConstants.CardTemplate.UIElement.DismissButton.NONE_ICON
    )
}
