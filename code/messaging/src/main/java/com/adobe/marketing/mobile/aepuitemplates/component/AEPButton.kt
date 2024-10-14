package com.adobe.marketing.mobile.aepuitemplates.component

import com.adobe.marketing.mobile.aepuitemplates.utils.Constants

/**
 * Data class representing a button element in the UI.
 *
 * @property interactId The unique interaction ID for the button.
 * @property actionUrl The URL to be opened when the button is clicked.
 * @property text The text to be displayed on the button, represented by an [AEPText] object.
 * @property borWidth The border width of the button.
 * @property borColor The border color of the button.
 *
 * @param buttonSchemaMap A map containing key-value pairs to initialize the AEPButton properties.
 */
data class AEPButton(
    val interactId: String? = null,
    val actionUrl: String? = null,
    val text: AEPText? = null,
    val borWidth: Int? = null,
    val borColor: String? = null
) {
    constructor(buttonSchemaMap: Map<String, Any>) : this(
        interactId = buttonSchemaMap[Constants.CardTemplate.UIElement.Button.INTERACTION_ID] as? String,
        actionUrl = buttonSchemaMap[Constants.CardTemplate.UIElement.Button.ACTION_URL] as? String,
        text = (buttonSchemaMap[Constants.CardTemplate.UIElement.Button.TEXT] as? Map<String, Any>)?.let { AEPText(it) },
        borWidth = (buttonSchemaMap[Constants.CardTemplate.UIElement.Button.BOR_WIDTH] as? Number)?.toInt(),
        borColor = buttonSchemaMap[Constants.CardTemplate.UIElement.Button.BOR_COLOR] as? String
    )
}