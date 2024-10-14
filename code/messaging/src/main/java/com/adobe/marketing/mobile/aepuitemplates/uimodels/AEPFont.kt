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

import com.adobe.marketing.mobile.aepuitemplates.utils.Constants

/**
 * Data class representing font styling in the UI.
 *
 * @property name The name of the font.
 * @property size The size of the font.
 * @property weight The weight of the font (e.g., bold, regular).
 * @property style A list of styles for the font (e.g., italic, underline).
 *
 * @param fontMap A map containing key-value pairs to initialize the AEPFont properties.
 */
data class AEPFont(
    val name: String? = null,
    val size: Int? = null,
    val weight: String? = null,
    val style: List<String>? = null
) {
    constructor(fontMap: Map<String, Any>) : this(
        name = fontMap[Constants.CardTemplate.UIElement.Font.NAME] as? String,
        size = (fontMap[Constants.CardTemplate.UIElement.Font.SIZE] as? Number)?.toInt(),
        weight = fontMap[Constants.CardTemplate.UIElement.Font.WEIGHT] as? String,
        style = (fontMap[Constants.CardTemplate.UIElement.Font.STYLE] as? List<*>)?.filterIsInstance<String>()
    )
}
