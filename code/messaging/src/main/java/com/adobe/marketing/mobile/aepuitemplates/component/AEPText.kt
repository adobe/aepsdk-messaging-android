package com.adobe.marketing.mobile.aepuitemplates.component

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
