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

import com.adobe.marketing.mobile.aepuitemplates.uiproperties.AepColor
import com.adobe.marketing.mobile.aepuitemplates.utils.AepUIConstants

/**
 * Data class representing a button element in the UI.
 *
 * @property interactId The unique interaction ID for the button.
 * @property actionUrl The URL to be opened when the button is clicked.
 * @property text The text to be displayed on the button, represented by an [AepText] object.
 * @property borWidth The border width of the button.
 * @property borColor The border color of the button represented as AepColor object.
 * @property bgColour The background color of the button represented as AepColor object.
 *
 * @param buttonSchemaMap A map containing key-value pairs to initialize the AEPButton properties.
 */
internal data class AepButton(
    val interactId: String? = null,
    val actionUrl: String? = null,
    val text: AepText? = null,
    val borWidth: Float? = null,
    val borColor: AepColor? = null,
    val bgColour: AepColor? = null
) {
    constructor(buttonSchemaMap: Map<String, Any>) : this(
        interactId = buttonSchemaMap[AepUIConstants.CardTemplate.UIElement.Button.INTERACTION_ID] as? String,
        actionUrl = buttonSchemaMap[AepUIConstants.CardTemplate.UIElement.Button.ACTION_URL] as? String,
        text = (buttonSchemaMap[AepUIConstants.CardTemplate.UIElement.Button.TEXT] as? Map<String, Any>)?.let { AepText(it) },
        borWidth = (buttonSchemaMap[AepUIConstants.CardTemplate.UIElement.Button.BOR_WIDTH] as? Number)?.toFloat(),
        borColor = buttonSchemaMap[AepUIConstants.CardTemplate.UIElement.Button.BOR_COLOR] as? AepColor,
        bgColour = buttonSchemaMap[AepUIConstants.CardTemplate.UIElement.Button.BG_COLOR] as? AepColor
    )
}
