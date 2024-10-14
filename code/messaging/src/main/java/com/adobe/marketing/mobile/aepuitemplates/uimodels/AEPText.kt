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
 * Data class representing a text element in the UI.
 *
 * @property content The content of the text.
 * @property clr The color of the text.
 * @property align The alignment of the text (e.g., left, right, center).
 * @property font The font styling of the text, represented by an [AEPFont] object.
 *
 * @param textSchemaMap A map containing key-value pairs to initialize the AEPText properties.
 */
data class AEPText(
    val content: String? = null,
    val clr: String? = null,
    val align: String? = null,
    val font: AEPFont? = null
) {
    constructor(textSchemaMap: Map<String, Any>) : this(
        content = textSchemaMap[Constants.CardTemplate.UIElement.Text.CONTENT] as? String,
        clr = textSchemaMap[Constants.CardTemplate.UIElement.Text.CLR] as? String,
        align = textSchemaMap[Constants.CardTemplate.UIElement.Text.ALIGN] as? String,
        font = (textSchemaMap[Constants.CardTemplate.UIElement.Text.FONT] as? Map<String, Any>)?.let {
            AEPFont(it)
        }
    )
}
