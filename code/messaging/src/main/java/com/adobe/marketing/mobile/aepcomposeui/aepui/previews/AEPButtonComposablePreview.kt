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

package com.adobe.marketing.mobile.aepcomposeui.aepui.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.adobe.marketing.mobile.aepcomposeui.aepui.components.Composable
import com.adobe.marketing.mobile.aepuitemplates.uimodels.AepButton
import com.adobe.marketing.mobile.aepuitemplates.utils.AepTemplateConstants

/**
 * Preview for [AepButton.Composable].
 * This function creates a sample button using predefined schema data for demonstration
 * purposes. It showcases how the button will appear with various styling options.
 */
@Preview(showBackground = true)
@Composable
fun PreviewButton() {
    // Sample button schema data
    val buttonSchemaMap = mapOf(
        AepTemplateConstants.CardTemplate.UIElement.Button.INTERACTION_ID to "button1",
        AepTemplateConstants.CardTemplate.UIElement.Button.TEXT to mapOf(
            AepTemplateConstants.CardTemplate.UIElement.Text.CONTENT to "Click Me",
            AepTemplateConstants.CardTemplate.UIElement.Text.CLR to "#FF0000CC",
            AepTemplateConstants.CardTemplate.UIElement.Text.ALIGN to "center",
            AepTemplateConstants.CardTemplate.UIElement.Text.FONT to mapOf(
                "name" to "Arial",
                "size" to 16,
                "weight" to "bold",
                "style" to listOf("italic")
            )
        ),
        AepTemplateConstants.CardTemplate.UIElement.Button.ACTION_URL to "https://www.adobe.com",
        "borWidth" to 2,
        "borColor" to "#0FE608AC"
    )

    // Create an instance of AEPButton using the sample schema data
    val aepButton = AepButton(buttonSchemaMap)

    // Render the AEPButtonComposable with the properties from aepButton
    AepButton(
        interactId = aepButton.interactId!!,
        text = aepButton.text!!,
        actionUrl = aepButton.actionUrl!!,
        borWidth = aepButton.borWidth,
        borColor = aepButton.borColor
    ).Composable(onClick = {})
}
